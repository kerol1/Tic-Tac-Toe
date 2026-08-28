import type { ReactElement } from 'react'
import type { Cell } from '../api/types'

interface BoardProps {
  board: Cell[]
  winningLine: number[] | null
  lastStampedCell: number | null
  live: boolean
}

const PLAYER_CLASS = { X: 'text-player-x', O: 'text-player-o' } as const

/** A winning cell fills with the winner's colour; the sweep only plays when motion is welcome. */
const WINNING_CLASS = {
  X: '[--burn-color:var(--color-player-x)] bg-(--burn-color) motion-safe:animate-burn',
  O: '[--burn-color:var(--color-player-o)] bg-(--burn-color) motion-safe:animate-burn',
} as const

export function Board({ board, winningLine, lastStampedCell, live }: BoardProps): ReactElement {
  return (
    <ul
      aria-label="Tic Tac Toe board"
      aria-busy={live}
      className="grid aspect-square w-full max-w-[26rem] grid-cols-3 grid-rows-3 gap-2 rounded-2xl bg-ink p-2 shadow-[0_24px_60px_-30px_rgba(29,53,87,0.6)]"
    >
      {board.map((cell, index) => {
        const winning = winningLine?.includes(index) ?? false
        const stamped = lastStampedCell === index
        return (
          <li
            key={index}
            aria-label={cell ? `cell ${index + 1}: ${cell}` : `cell ${index + 1}: empty`}
            className={`relative flex min-h-0 items-center justify-center rounded-xl bg-paper ${winning && cell ? WINNING_CLASS[cell] : ''}`}
          >
            {cell === null ? (
              <span aria-hidden="true" className="font-data text-sm text-ink/25 select-none">
                {index + 1}
              </span>
            ) : (
              <span
                className={[
                  'font-display text-6xl font-semibold leading-none sm:text-7xl',
                  winning ? 'text-paper' : PLAYER_CLASS[cell],
                  stamped ? 'motion-safe:animate-stamp' : '',
                ].join(' ')}
              >
                {cell}
              </span>
            )}
          </li>
        )
      })}
    </ul>
  )
}
