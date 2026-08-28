import { describe, expect, it } from 'vitest'
import { initialState, simulationReducer, type SimulationState } from './simulationReducer'
import type { Cell, MoveEvent, SessionDetails } from '../api/types'

function boardWith(...marks: Array<[number, 'X' | 'O']>): Cell[] {
  const board: Cell[] = Array.from({ length: 9 }, () => null)
  marks.forEach(([position, player]) => (board[position] = player))
  return board
}

const move1: MoveEvent = { moveNumber: 1, player: 'X', position: 4, board: boardWith([4, 'X']), gameStatus: 'IN_PROGRESS' }
const move2: MoveEvent = { moveNumber: 2, player: 'O', position: 0, board: boardWith([4, 'X'], [0, 'O']), gameStatus: 'IN_PROGRESS' }

function running(): SimulationState {
  return simulationReducer(simulationReducer(initialState, { type: 'start' }), { type: 'sessionCreated', sessionId: 's1' })
}

describe('simulationReducer', () => {
  it('builds the board and history from move events', () => {
    const state = simulationReducer(simulationReducer(running(), { type: 'move', event: move1 }), { type: 'move', event: move2 })

    expect(state.board).toEqual(boardWith([4, 'X'], [0, 'O']))
    expect(state.moves.map((move) => move.moveNumber)).toEqual([1, 2])
    expect(state.nextPlayer).toBe('X')
    expect(state.lastStampedCell).toBe(0)
  })

  it('ignores a move it has already applied', () => {
    const once = simulationReducer(running(), { type: 'move', event: move1 })
    const twice = simulationReducer(once, { type: 'move', event: move1 })

    expect(twice).toBe(once)
    expect(twice.moves).toHaveLength(1)
  })

  it('marks the winner and the winning line on finished', () => {
    const board = boardWith([0, 'X'], [1, 'X'], [2, 'X'], [3, 'O'], [4, 'O'])
    const state = simulationReducer(running(), { type: 'finished', event: { status: 'X_WON', winningLine: [0, 1, 2], board } })

    expect(state.phase).toBe('finished')
    expect(state.status).toBe('X_WON')
    expect(state.winningLine).toEqual([0, 1, 2])
    expect(state.board).toEqual(board)
    expect(state.nextPlayer).toBeNull()
  })

  it('maps a simulation error to a restartable failure', () => {
    const state = simulationReducer(running(), { type: 'simulationFailed', event: { code: 'ENGINE_UNAVAILABLE', message: 'down' } })

    expect(state.phase).toBe('failed')
    expect(state.failure).toEqual({ code: 'ENGINE_UNAVAILABLE', message: 'down', recovery: 'restart' })
  })

  it('offers reload only when re-reading an existing session failed', () => {
    const reload = simulationReducer(running(), { type: 'requestFailed', during: 'reload', code: 'NETWORK', message: 'offline' })
    const simulate = simulationReducer(running(), { type: 'requestFailed', during: 'simulate', code: 'NETWORK', message: 'offline' })
    const create = simulationReducer(initialState, { type: 'requestFailed', during: 'create', code: 'NETWORK', message: 'offline' })

    expect(reload.failure?.recovery).toBe('reload')
    expect(simulate.failure?.recovery).toBe('restart')
    expect(create.failure?.recovery).toBe('restart')
  })

  it('hydrates from session details after a reconnect', () => {
    const details: SessionDetails = {
      sessionId: 's1',
      state: 'RUNNING',
      game: { board: boardWith([4, 'X']), nextPlayer: 'O', status: 'IN_PROGRESS', winningLine: null },
      moves: [{ moveNumber: 1, player: 'X', position: 4, gameStatus: 'IN_PROGRESS' }],
      failureReason: null,
    }
    const state = simulationReducer(running(), { type: 'hydrate', details })
    const afterReplay = simulationReducer(state, { type: 'move', event: move1 })

    expect(state.phase).toBe('running')
    expect(state.moves).toHaveLength(1)
    expect(afterReplay).toBe(state)
  })
})
