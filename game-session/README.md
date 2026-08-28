# Game Session

Orchestrates automated play: creates sessions, generates moves for both players, drives the Simulation against the Game Engine, and feeds progress to the UI.

## Language

**Session**:
One automated playthrough as seen by the outside world, identified by `sessionId` (which doubles as the Engine's `gameId`). Holds the Session State and the Move History.
_Avoid_: game (that's the Engine's concept)

**Session State**:
`CREATED`, `RUNNING`, `FINISHED`, or `FAILED`. `FAILED` means the Simulation could not complete (e.g. the Engine became unreachable) — distinct from a game that ended in a loss or draw.
_Avoid_: status (reserved for the Engine's Game Status), phase

**Simulation**:
The asynchronous process that alternates generated Moves between `X` and `O` until the game concludes or fails. Started at most once per Session.
_Avoid_: run, playback

**Tick**:
The configurable delay between generated Moves during a Simulation. Exists purely so humans can watch the game unfold.
_Avoid_: interval, sleep

**Move Strategy**:
The pluggable algorithm that picks the next Position for a symbol given the current Board. Implementations: Heuristic (win → block → center/corner → random) and Random.
_Avoid_: AI, bot, engine (overloaded)

**Move Record**:
A historical fact in the Move History: move number, symbol, position, and the resulting Game Status. Immutable once written — unlike the Engine's Move, which is a command.
_Avoid_: move (ambiguous with the command), log entry

**Move History**:
The ordered list of Move Records for a Session, persisted with it.
_Avoid_: log, timeline

**Progress Event**:
An SSE event pushed to subscribers as the Simulation advances: `move`, `finished`, or `error`.
_Avoid_: notification, message
