import { useState, type ReactElement } from 'react'
import type { Player } from '../api/types'

const CELEBRATION_GIF = 'https://media.giphy.com/media/KzKFAvaM1RBoRU5dcl/giphy.gif'

interface WinnerCardProps {
  winner: Player
}

const WINNER_CLASS = { X: 'border-player-x/40 text-player-x', O: 'border-player-o/40 text-player-o' } as const

export function WinnerCard({ winner }: WinnerCardProps): ReactElement {
  const [gifAvailable, setGifAvailable] = useState(true)

  function handleGifError(): void {
    setGifAvailable(false)
  }

  return (
    <div className={`flex flex-col gap-3 rounded-2xl border bg-white/60 p-4 sm:flex-row sm:items-center ${WINNER_CLASS[winner]}`}>
      {gifAvailable && (
        <img
          src={CELEBRATION_GIF}
          alt="An engineer at a 1950s tic-tac-toe machine whose sign reads: machine wins"
          loading="lazy"
          onError={handleGifError}
          className="aspect-[3/4] w-full shrink-0 rounded-xl object-cover motion-reduce:hidden sm:h-44 sm:w-auto"
        />
      )}
      <div className="flex-1">
        <p className="font-display text-2xl font-semibold">{winner} wins the match</p>
        <p className="mt-1 text-ink/70">Both players are machines, so the machine wins either way.</p>
      </div>
    </div>
  )
}
