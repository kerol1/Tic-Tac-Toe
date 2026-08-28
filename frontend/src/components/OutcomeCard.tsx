import { useState, type ReactElement } from 'react'
import type { GameStatus, Player } from '../api/types'

const WIN_GIFS = [
  'https://media.giphy.com/media/KzKFAvaM1RBoRU5dcl/giphy.gif',
  'https://media.giphy.com/media/3ohryhNgUwwZyxgktq/giphy.gif',
  'https://media.giphy.com/media/Jt4y4zi519V6asgGhA/giphy.gif',
  'https://media.giphy.com/media/14rk56liuv7mQo/giphy.gif',
  'https://media.giphy.com/media/GS9pfaxQj5hPKFGGp8/giphy.gif',
]

const DRAW_GIFS = [
  'https://media.giphy.com/media/h9vTTmoa7sOJ2/giphy.gif',
  'https://media.giphy.com/media/uLS4QUBJ4MpOcW9xq1/giphy.gif',
]

function pick(pool: readonly string[]): string {
  return pool[Math.floor(Math.random() * pool.length)]!
}

interface OutcomeCardProps {
  status: GameStatus
}

interface Outcome {
  title: string
  line: string
  gif: string
  alt: string
  tone: string
}

function describe(status: GameStatus): Outcome | null {
  switch (status) {
    case 'X_WON':
      return { ...winner('X'), tone: 'border-player-x/40 text-player-x' }
    case 'O_WON':
      return { ...winner('O'), tone: 'border-player-o/40 text-player-o' }
    case 'DRAW':
      return {
        title: 'A draw',
        line: 'Neither machine blinked. Turn the blunders up if you want a winner.',
        gif: pick(DRAW_GIFS),
        alt: 'A reaction to a stalemate',
        tone: 'border-ink/30 text-ink',
      }
    case 'IN_PROGRESS':
      return null
  }
}

function winner(player: Player): Omit<Outcome, 'tone'> {
  return {
    title: `${player} wins the match`,
    line: 'Both players are machines, so the machine wins either way.',
    gif: pick(WIN_GIFS),
    alt: 'A victory celebration',
  }
}

/** Mount it with a key per match so the GIF is picked once and stays put. */
export function OutcomeCard({ status }: OutcomeCardProps): ReactElement | null {
  const [outcome] = useState(() => describe(status))
  const [gifAvailable, setGifAvailable] = useState(true)

  function handleGifError(): void {
    setGifAvailable(false)
  }

  if (outcome === null) {
    return null
  }
  return (
    <div className={`flex flex-col gap-3 rounded-2xl border bg-white/60 p-4 sm:flex-row sm:items-center ${outcome.tone}`}>
      {gifAvailable && (
        <img
          src={outcome.gif}
          alt={outcome.alt}
          loading="lazy"
          onError={handleGifError}
          className="aspect-square w-full shrink-0 rounded-xl object-cover motion-reduce:hidden sm:size-44"
        />
      )}
      <div className="flex-1">
        <p className="font-display text-2xl font-semibold">{outcome.title}</p>
        <p className="mt-1 text-ink/70">{outcome.line}</p>
      </div>
    </div>
  )
}
