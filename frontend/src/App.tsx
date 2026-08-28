import { useReducer, useState, type ReactElement } from 'react'
import { ApiRequestError, sessionApi } from './api/sessionApi'
import { Board } from './components/Board'
import { ErrorBanner } from './components/ErrorBanner'
import { MatchSettings } from './components/MatchSettings'
import { MoveTicker } from './components/MoveTicker'
import { StartButton } from './components/StartButton'
import { StatusBadge } from './components/StatusBadge'
import { useSessionEvents } from './hooks/useSessionEvents'
import { initialState, simulationReducer, type FailedRequest } from './state/simulationReducer'
import type { MatchSettings as Settings } from './api/types'

const DEFAULT_SETTINGS: Settings = { strategy: 'HEURISTIC', blunderRate: 0.25 }

function describeFailure(error: unknown): { code: string; message: string } {
  if (error instanceof ApiRequestError) {
    return { code: error.code, message: error.message }
  }
  return { code: 'UNKNOWN', message: 'Something unexpected happened' }
}

export default function App(): ReactElement {
  const [state, dispatch] = useReducer(simulationReducer, initialState)
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS)
  useSessionEvents(state.phase === 'running' ? state.sessionId : null, dispatch)
  const busy = state.phase === 'creating' || state.phase === 'running'

  async function handleStart(): Promise<void> {
    dispatch({ type: 'start' })
    let during: FailedRequest = 'create'
    try {
      const session = await sessionApi.create(settings)
      dispatch({ type: 'sessionCreated', sessionId: session.sessionId })
      during = 'simulate'
      await sessionApi.simulate(session.sessionId)
    } catch (error) {
      dispatch({ type: 'requestFailed', during, ...describeFailure(error) })
    }
  }

  async function handleReload(): Promise<void> {
    if (state.sessionId === null) {
      return
    }
    try {
      dispatch({ type: 'hydrate', details: await sessionApi.get(state.sessionId) })
    } catch (error) {
      dispatch({ type: 'requestFailed', during: 'reload', ...describeFailure(error) })
    }
  }

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-5xl flex-col gap-10 px-6 py-12 sm:py-16">
      <header className="flex flex-col gap-4">
        <p className="font-data text-xs tracking-[0.2em] text-ink/60 uppercase">Automated match · engine vs engine</p>
        <h1 className="font-display text-5xl font-semibold tracking-tight text-ink sm:text-6xl">Tic Tac Toe</h1>
        <p className="max-w-prose text-lg text-ink/80">
          Two services play each other. One knows the rules, the other picks the moves. You just watch.
        </p>
      </header>

      <div className="grid gap-10 lg:grid-cols-[minmax(0,26rem)_1fr] lg:items-start">
        <div className="flex flex-col gap-6">
          <MatchSettings settings={settings} onChange={setSettings} disabled={busy} />
          <div className="flex flex-wrap items-center gap-4">
            <StartButton phase={state.phase} onStart={handleStart} />
            <StatusBadge phase={state.phase} nextPlayer={state.nextPlayer} status={state.status} />
          </div>
          <Board
            board={state.board}
            winningLine={state.winningLine}
            lastStampedCell={state.lastStampedCell}
            live={state.phase === 'running'}
          />
        </div>

        <div className="flex flex-col gap-6">
          {state.failure && <ErrorBanner failure={state.failure} onRestart={handleStart} onReload={handleReload} />}
          <MoveTicker moves={state.moves} />
          {state.sessionId && (
            <p className="font-data text-xs text-ink/50">
              session <span className="select-all">{state.sessionId}</span>
            </p>
          )}
        </div>
      </div>
    </main>
  )
}
