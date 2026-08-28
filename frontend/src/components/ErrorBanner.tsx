import type { Failure } from '../state/simulationReducer'

interface ErrorBannerProps {
  failure: Failure
  onRestart: () => void
  onReload: () => void
}

const EXPLANATION: Record<string, string> = {
  ENGINE_UNAVAILABLE: 'The game engine stopped answering, so the match could not continue.',
  NETWORK: 'The server could not be reached.',
  INTERNAL_ERROR: 'Something went wrong on the server while the match was running.',
}

export function ErrorBanner({ failure, onRestart, onReload }: ErrorBannerProps) {
  const reload = failure.recovery === 'reload'
  return (
    <div role="alert" className="flex flex-col gap-3 rounded-2xl border border-player-x/40 bg-white/60 p-4 sm:flex-row sm:items-center">
      <div className="flex-1">
        <p className="font-display font-medium text-ink">{EXPLANATION[failure.code] ?? failure.message}</p>
        <p className="mt-1 font-data text-xs text-ink/60">{failure.code}</p>
      </div>
      <button
        type="button"
        onClick={reload ? onReload : onRestart}
        className="rounded-full border border-ink px-4 py-2 font-display text-sm font-medium text-ink focus-visible:ring-4 focus-visible:ring-frost focus-visible:outline-none"
      >
        {reload ? 'Reload match' : 'Try again'}
      </button>
    </div>
  )
}
