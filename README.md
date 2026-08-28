# Distributed Tic Tac Toe

A Tic Tac Toe game that plays itself: two Spring Boot services split the work — one knows
the rules, the other picks the moves — and a small React page shows the match as it happens.

## Services

- [Game Engine](./game-engine/README.md) — owns the rules of Tic Tac Toe: board state, move legality, game outcome
- [Game Session](./game-session/README.md) — orchestrates automated play: sessions, move generation, simulation lifecycle, live progress feed

## How they relate

- **Game Session → Game Engine**: synchronous REST, one direction only. Session creates a
  Game (`sessionId == gameId`), submits Moves, and treats the Engine's response as the
  authoritative game state. The Engine knows nothing about Sessions.
- **Frontend → Game Session**: the UI talks only to the Game Session service (REST + SSE).
  The Game Engine is internal and never exposed publicly.
- **Shared identity**: `sessionId` doubles as `gameId`. No shared code/DTO module — each
  service owns its own types.
