import { OUTCOME_LABEL, type GameStatus, type Player } from '../api/types'
import type { Phase } from '../state/simulationReducer'

interface StatusBadgeProps {
  phase: Phase
  nextPlayer: Player | null
  status: GameStatus | null
}

function label(phase: Phase, nextPlayer: Player | null, status: GameStatus | null): string {
  switch (phase) {
    case 'idle':
      return 'Ready'
    case 'creating':
      return 'Setting up the match'
    case 'running':
      return nextPlayer ? `${nextPlayer} to move` : 'In progress'
    case 'finished':
      return (status && OUTCOME_LABEL[status]) || 'Finished'
    case 'failed':
      return 'Match stopped'
  }
}

const TONE: Record<Phase, string> = {
  idle: 'bg-frost/40 text-ink',
  creating: 'bg-frost/40 text-ink',
  running: 'bg-ink text-paper',
  finished: 'bg-ink text-paper',
  failed: 'bg-player-x text-paper',
}

export function StatusBadge({ phase, nextPlayer, status }: StatusBadgeProps) {
  return (
    <span
      role="status"
      aria-live="polite"
      className={`inline-flex items-center gap-2 rounded-full px-4 py-1.5 font-display text-base font-medium ${TONE[phase]}`}
    >
      {phase === 'running' && (
        <span aria-hidden="true" className="inline-block size-2 animate-pulse rounded-full bg-frost" />
      )}
      {label(phase, nextPlayer, status)}
    </span>
  )
}
