# Game Engine

Owns the rules of Tic Tac Toe: what the board looks like, which moves are legal, and how a game ends. It is the single source of truth for game state.

## Language

**Game**:
One playthrough of Tic Tac Toe, identified by `gameId`. Holds the Board, the Next Player, and the Game Status.
_Avoid_: match, round, session (that's the other context)

**Board**:
The 3×3 grid, represented as a flat array of 9 Cells indexed 0–8, left-to-right, top-to-bottom.
_Avoid_: grid, field

**Cell**:
One square of the Board. Either empty or marked with a Player Symbol.
_Avoid_: square, tile, slot

**Position**:
The index (0–8) of a Cell on the Board.
_Avoid_: coordinates, row/col

**Player Symbol**:
`X` or `O`. `X` always moves first. The Engine knows players only by their symbol.
_Avoid_: player id, user

**Move**:
A command to mark one Position with one Player Symbol. Legal only if the Game is in progress, the Cell is empty, and it is that symbol's turn.
_Avoid_: turn (that's whose right it is to move), step

**Next Player**:
The Player Symbol whose turn it is. Drives Wrong-Turn rejection.
_Avoid_: current player, active player

**Game Status**:
`IN_PROGRESS`, `X_WON`, `O_WON`, or `DRAW`. Recomputed after every applied Move.
_Avoid_: state (overloaded), result

**Winning Line**:
The three Positions that produced a win. Present only when the Game Status is `X_WON` or `O_WON`.
_Avoid_: win combo
