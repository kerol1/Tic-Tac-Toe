import type { ReactElement } from 'react'
import { OUTCOME_LABEL, type MoveRecord } from '../api/types'

interface MoveTickerProps {
  moves: MoveRecord[]
}

export function MoveTicker({ moves }: MoveTickerProps): ReactElement {
  return (
    <section aria-label="Move history" className="flex min-h-48 flex-col">
      <h2 className="font-display text-sm font-medium tracking-wide text-ink/60 uppercase">Moves</h2>
      {moves.length === 0 ? (
        <p className="mt-3 font-data text-sm text-ink/60">No moves yet. Start a match to watch it unfold.</p>
      ) : (
        <ol className="mt-3 flex flex-col divide-y divide-frost/70 font-data text-sm text-ink">
          {moves.map((move) => (
            <li key={move.moveNumber} className="flex items-baseline gap-4 py-1.5">
              <span className="w-6 text-right text-ink/50">{String(move.moveNumber).padStart(2, '0')}</span>
              <span className="w-4 font-medium">{move.player}</span>
              <span>cell {move.position + 1}</span>
              {OUTCOME_LABEL[move.gameStatus] && <span className="ml-auto text-ink/60">{OUTCOME_LABEL[move.gameStatus]}</span>}
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
