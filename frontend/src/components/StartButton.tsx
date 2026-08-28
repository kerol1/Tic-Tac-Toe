import type { Phase } from '../state/simulationReducer'

interface StartButtonProps {
  phase: Phase
  onStart: () => void
}

const LABEL: Record<Phase, string> = {
  idle: 'Start match',
  creating: 'Starting…',
  running: 'Match in progress',
  finished: 'Play again',
  failed: 'Start a new match',
}

export function StartButton({ phase, onStart }: StartButtonProps) {
  const busy = phase === 'creating' || phase === 'running'
  return (
    <button
      type="button"
      onClick={onStart}
      disabled={busy}
      className="rounded-full bg-ink px-6 py-3 font-display text-base font-medium text-paper transition-transform hover:-translate-y-0.5 focus-visible:ring-4 focus-visible:ring-frost focus-visible:outline-none disabled:cursor-default disabled:opacity-50 disabled:hover:translate-y-0"
    >
      {LABEL[phase]}
    </button>
  )
}
