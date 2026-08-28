export type Player = 'X' | 'O'
export type Cell = Player | null
export type GameStatus = 'IN_PROGRESS' | 'X_WON' | 'O_WON' | 'DRAW'
export type SessionState = 'CREATED' | 'RUNNING' | 'FINISHED' | 'FAILED'

export interface MoveRecord {
  moveNumber: number
  player: Player
  position: number
  gameStatus: GameStatus
}

export interface GameSnapshot {
  board: Cell[]
  nextPlayer: Player | null
  status: GameStatus
  winningLine: number[] | null
}

export interface SessionDetails {
  sessionId: string
  state: SessionState
  game: GameSnapshot
  moves: MoveRecord[]
  failureReason: string | null
}

export interface MoveEvent extends MoveRecord {
  board: Cell[]
}

export interface FinishedEvent {
  status: GameStatus
  winningLine: number[] | null
  board: Cell[]
}

export interface FailedEvent {
  code: string
  message: string
}

export const OUTCOME_LABEL: Record<GameStatus, string> = {
  IN_PROGRESS: '',
  X_WON: 'X wins',
  O_WON: 'O wins',
  DRAW: 'Draw',
}

export interface ApiError {
  code: string
  message: string
  timestamp: string
}
