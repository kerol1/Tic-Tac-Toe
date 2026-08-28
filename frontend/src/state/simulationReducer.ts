import type { Cell, FailedEvent, FinishedEvent, GameStatus, MoveEvent, MoveRecord, Player, SessionDetails } from '../api/types'

export type Phase = 'idle' | 'creating' | 'running' | 'finished' | 'failed'

/** What the person can do about a failure. Each failure class has exactly one answer. */
export type Recovery = 'restart' | 'reload'

export interface Failure {
  code: string
  message: string
  recovery: Recovery
}

export interface SimulationState {
  phase: Phase
  sessionId: string | null
  board: Cell[]
  nextPlayer: Player | null
  status: GameStatus | null
  winningLine: number[] | null
  moves: MoveRecord[]
  lastStampedCell: number | null
  failure: Failure | null
}

/** Which request failed decides what "try again" means. */
export type FailedRequest = 'create' | 'simulate' | 'reload'

export type Action =
  | { type: 'start' }
  | { type: 'sessionCreated'; sessionId: string }
  | { type: 'move'; event: MoveEvent }
  | { type: 'finished'; event: FinishedEvent }
  | { type: 'simulationFailed'; event: FailedEvent }
  | { type: 'hydrate'; details: SessionDetails }
  | { type: 'requestFailed'; during: FailedRequest; code: string; message: string }

const EMPTY_BOARD: Cell[] = Array.from({ length: 9 }, () => null)

export const initialState: SimulationState = {
  phase: 'idle',
  sessionId: null,
  board: EMPTY_BOARD,
  nextPlayer: null,
  status: null,
  winningLine: null,
  moves: [],
  lastStampedCell: null,
  failure: null,
}

function lastMoveNumber(moves: MoveRecord[]): number {
  return moves.length === 0 ? 0 : moves[moves.length - 1]!.moveNumber
}

export function simulationReducer(state: SimulationState, action: Action): SimulationState {
  switch (action.type) {
    case 'start':
      return { ...initialState, phase: 'creating' }

    case 'sessionCreated':
      return { ...state, phase: 'running', sessionId: action.sessionId, nextPlayer: 'X', status: 'IN_PROGRESS' }

    case 'move': {
      // Replay on (re)subscribe can deliver a move twice; the move number is the identity.
      if (action.event.moveNumber <= lastMoveNumber(state.moves)) {
        return state
      }
      const { board, ...move } = action.event
      return {
        ...state,
        phase: 'running',
        board,
        nextPlayer: move.gameStatus === 'IN_PROGRESS' ? (move.player === 'X' ? 'O' : 'X') : null,
        status: move.gameStatus,
        moves: [...state.moves, move],
        lastStampedCell: move.position,
      }
    }

    case 'finished':
      return {
        ...state,
        phase: 'finished',
        board: action.event.board,
        status: action.event.status,
        winningLine: action.event.winningLine,
        nextPlayer: null,
        lastStampedCell: null,
      }

    case 'simulationFailed':
      return {
        ...state,
        phase: 'failed',
        nextPlayer: null,
        failure: { code: action.event.code, message: action.event.message, recovery: 'restart' },
      }

    case 'hydrate': {
      const { details } = action
      const phase: Phase =
        details.state === 'FINISHED' ? 'finished' : details.state === 'FAILED' ? 'failed' : 'running'
      return {
        ...state,
        phase,
        sessionId: details.sessionId,
        board: details.game.board,
        nextPlayer: details.game.nextPlayer,
        status: details.game.status,
        winningLine: details.game.winningLine,
        moves: details.moves,
        lastStampedCell: null,
        failure:
          details.state === 'FAILED'
            ? { code: details.failureReason ?? 'UNKNOWN', message: 'The simulation failed', recovery: 'restart' }
            : null,
      }
    }

    case 'requestFailed':
      return {
        ...state,
        phase: 'failed',
        failure: {
          code: action.code,
          message: action.message,
          recovery: action.during === 'reload' ? 'reload' : 'restart',
        },
      }
  }
}
