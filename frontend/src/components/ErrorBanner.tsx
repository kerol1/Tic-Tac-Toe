import { useState, type ReactElement } from 'react'
import type { Failure } from '../state/simulationReducer'

const CONSOLATION_GIF = 'https://media.giphy.com/media/d2ZaChASUxF6xqWk/giphy.gif'

interface ErrorBannerProps {
  failure: Failure
  onRestart: () => void
  onReload: () => void
}

const BY_CODE: Record<string, string> = {
  ENGINE_UNAVAILABLE: 'The game engine stopped answering, so the match could not continue.',
  ENGINE_REJECTED: 'The game engine refused a move it should have accepted. Start a new match.',
  INTERNAL_ERROR: 'Something went wrong on the server while the match was running.',
  SESSION_NOT_FOUND: 'This match no longer exists on the server.',
  SIMULATION_ALREADY_STARTED: 'This match has already been started.',
  NETWORK: 'The server could not be reached. Check your connection and try again.',
}

/** A sentence a person can act on; the raw code stays visible underneath for reference. */
function explain(failure: Failure): string {
  const known = BY_CODE[failure.code]
  if (known) {
    return known
  }
  if (/^HTTP_5\d\d$/.test(failure.code)) {
    return 'The server is not available right now. Give it a few seconds and try again.'
  }
  if (/^HTTP_4\d\d$/.test(failure.code)) {
    return 'The server did not accept the request.'
  }
  return failure.message
}

export function ErrorBanner({ failure, onRestart, onReload }: ErrorBannerProps): ReactElement {
  const reload = failure.recovery === 'reload'
  const [gifAvailable, setGifAvailable] = useState(true)

  function handleGifError(): void {
    setGifAvailable(false)
  }

  return (
    <div role="alert" className="flex flex-col gap-3 rounded-2xl border border-player-x/40 bg-white/60 p-4 sm:flex-row sm:items-center">
      {gifAvailable && (
        <img
          src={CONSOLATION_GIF}
          alt="Will Ferrell crying out no"
          loading="lazy"
          onError={handleGifError}
          className="aspect-square w-full shrink-0 rounded-xl object-cover motion-reduce:hidden sm:size-44"
        />
      )}
      <div className="flex-1">
        <p className="font-display font-medium text-ink">{explain(failure)}</p>
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
