# Distributed Tic Tac Toe

[![build](https://github.com/kerol1/Tic-Tac-Toe/actions/workflows/build.yml/badge.svg)](https://github.com/kerol1/Tic-Tac-Toe/actions/workflows/build.yml)

A Tic Tac Toe game that plays itself. Two Spring Boot services split the work — one
knows the rules, the other picks the moves — and a small React page shows the match as
it happens.

```
browser ──► nginx (UI + /api) ──► gateway ──► game-session ──► game-engine
                                     │              │               │
                                     └──────── discovery (Eureka) ──┘
```

| Service | Role | Port |
|---|---|---|
| `game-engine` | Board state, move validation, win/draw detection. Internal only. | 8081 (not published) |
| `game-session` | Sessions, automated move generation, simulation loop, live progress over SSE | 8082 |
| `gateway` | Spring Cloud Gateway — the single public API entry, routes `/api/sessions/**` | 8080 (not published) |
| `discovery` | Eureka registry every Spring service registers with | 8761 |
| `frontend` | React UI served by nginx, which also proxies `/api` to the gateway | 8080 |

## Run it

The only requirement is Docker.

```bash
docker compose up --build
```

Open <http://localhost:8080>, press **Start match** and watch. The game engine is
reachable only from inside the compose network; the session API is also published on
<http://localhost:8082> for curl and Swagger UI (`/swagger-ui.html` on either service).

Running without Docker needs JDK 25 and Node 22:

```bash
./mvnw -pl game-engine spring-boot:run       # terminal 1
./mvnw -pl game-session spring-boot:run      # terminal 2
cd frontend && npm install && npm run dev    # terminal 3 → http://localhost:5173
```

In this mode the session service talks to the engine at a fixed address and no
registry is needed — discovery is opt-in through `ENGINE_DISCOVERY` / `EUREKA_ENABLED`,
which the compose file sets.

## How a match works

1. `POST /api/sessions` creates a session and, in the same call, the game in the engine.
   The session id doubles as the game id.
2. `POST /api/sessions/{id}/simulate` answers `202` immediately and starts the
   simulation on a virtual thread. The loop asks the strategy for a move, submits it to
   the engine, records it, publishes it, waits one tick (600 ms by default) and repeats
   until the engine reports a win or a draw.
3. `GET /api/sessions/{id}/events` is a server-sent event stream: `move` events as they
   happen, then a `finished` (or `failed`) event and the stream closes. Subscribing late
   replays the history first, so a page can join or reconnect at any point.
4. `GET /api/sessions/{id}` returns the state, the last known board and the full move
   history, served from the session's own snapshot without touching the engine.

The strategy that picks moves is pluggable (`simulation.strategy`): the default
heuristic wins when it can, blocks when it must, and otherwise prefers the centre and
the corners; a purely random one is also available.

## API

Game Engine (internal):

| Method | Path | Result |
|---|---|---|
| `POST` | `/games` `{ "gameId": uuid }` | `201` game state, `409 GAME_ALREADY_EXISTS` |
| `GET` | `/games/{gameId}` | `200` game state, `404 GAME_NOT_FOUND` |
| `POST` | `/games/{gameId}/moves` `{ "player": "X", "position": 4 }` | `200` game state, `409 CELL_OCCUPIED / WRONG_TURN / GAME_FINISHED`, `400 VALIDATION_ERROR` |

Game Session (public, behind `/api`):

| Method | Path | Result |
|---|---|---|
| `POST` | `/sessions` | `201` session, `503 ENGINE_UNAVAILABLE` |
| `POST` | `/sessions/{id}/simulate` | `202`, `409 SIMULATION_ALREADY_STARTED`, `404` |
| `GET` | `/sessions/{id}` | `200` session with board and move history |
| `GET` | `/sessions/{id}/events` | `text/event-stream` of `move`, `finished`, `failed` |

Every error, from either service, has the same shape:
`{ "code": "CELL_OCCUPIED", "message": "Cell 4 is already occupied", "timestamp": "…" }`.

A note on paths: the assignment names the move endpoint `/games/{gameId}/move`. It is
`/moves` here on purpose — a move is a resource added to a collection, and the plural
follows the usual REST naming.

## Design notes

**The engine owns the truth.** The session never computes a board itself; every board it
stores or streams came out of an engine response. The session keeps a snapshot so reads
and the event replay do not depend on the engine being up.

**Concurrent moves cannot corrupt a game.** The engine applies each move inside
`ConcurrentHashMap.compute`, which is atomic per key, and it tracks whose turn it is.
Twenty simultaneous requests on one game produce exactly one applied move and nineteen
`409`s — there is a test that does precisely that.

**A move is never applied twice, and never lost.** Submitting a move is not idempotent, so
it is never retried blindly. When the engine's answer is lost (timeout, reset, 5xx) the
session re-reads the game: a cell is written at most once, so `board[position]` tells
exactly whether the move landed. Creating a game is idempotent by construction — a
`GAME_ALREADY_EXISTS` on a retried create can only mean the first attempt succeeded.

**Starting a simulation is a compare-and-set.** `UPDATE sessions SET state='RUNNING'
WHERE id=? AND state='CREATED'` — of two concurrent starts exactly one gets an update
count of 1. No read-then-write, no lock.

**Nothing is ever left RUNNING.** Any failure in the loop — engine down, engine
rejection, a persistence error — ends the session as `FAILED` with a reason, and the
same reason reaches SSE subscribers as a `failed` event. Sessions still marked RUNNING
when the service starts belonged to a process that died mid-game; they are closed on
startup.

**Late subscribers never miss a move.** The SSE endpoint registers the subscriber
first and replays the history second, so a move published in between arrives twice
rather than not at all; clients de-duplicate on the move number. Together with the
browser's automatic `EventSource` reconnect this makes a dropped connection harmless.

**One place strips `/api`.** Services serve bare paths. nginx passes `/api/...` through
untouched and the gateway strips the prefix; when running without the gateway, nginx
would strip it instead. Exactly one edge, never both.

**Two storages on purpose.** The engine keeps games in a `ConcurrentHashMap` — its state
is ephemeral and the map gives it the atomicity it needs. The session uses H2 through
Spring Data JPA because its data (a session and its moves) is relational; switching to a
file-backed H2 or PostgreSQL is a configuration change. Both sit behind interfaces.

Each service has its own README with the vocabulary it uses
([engine](./game-engine/README.md), [session](./game-session/README.md)).

## Tests

```bash
./mvnw verify              # unit, API and fault-injection suites; no Docker needed
./mvnw verify -Pe2e        # end-to-end on the real compose topology (needs Docker)
cd frontend && npm test    # reducer / de-duplication logic
```

- **Engine** — a pure-JVM matrix of the rules (every winning line, draw, occupied cell,
  wrong turn, move after the end, win on the ninth move), HTTP contract tests, and the
  20-thread concurrency scenario. JaCoCo enforces 95 % line coverage on the rules package.
- **Session** — `@SpringBootTest` against a WireMock engine that behaves like the real
  one but can be told to fail before or after applying a move, reset the connection,
  stall past the read timeout, reject a move, or go down. This is where the recovery
  protocol, the atomic start, failure containment, SSE replay and the startup sweep are
  verified. Tests wait on latches, never sleep.
- **End-to-end** — Testcontainers starts `docker-compose.yml` as is, then plays a match
  through nginx and the gateway, checks that the session's board equals the engine's,
  that events stream while the game is still running, and that every service is
  registered in Eureka. The engine is reached through a Testcontainers ambassador, not a
  published port.

CI runs the default build on every push and the end-to-end suite on top of it.

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| `ENGINE_BASE_URL` | `http://localhost:8081` | Engine address, or a service id when discovery is on |
| `ENGINE_DISCOVERY` | `false` | Resolve the engine through Eureka with client-side load balancing |
| `EUREKA_ENABLED` / `EUREKA_URL` | `false` / `http://localhost:8761/eureka/` | Registration |
| `SIMULATION_TICK_DELAY` | `600ms` | Pause between moves (0 in tests) |
| `SIMULATION_STRATEGY` | `heuristic` | `heuristic` or `random` |

Engine timeouts (2 s connect, 5 s read), retry counts and the recovery-cycle bound are
in each service's `application.yml`.
