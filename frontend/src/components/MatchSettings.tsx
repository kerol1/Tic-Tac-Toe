import type { ChangeEvent, ReactElement } from 'react'
import type { MatchSettings, StrategyKind } from '../api/types'

interface MatchSettingsProps {
  settings: MatchSettings
  onChange: (settings: MatchSettings) => void
  disabled: boolean
}

const STRATEGIES: Array<{ kind: StrategyKind; label: string; hint: string }> = [
  { kind: 'HEURISTIC', label: 'Heuristic', hint: 'Wins when it can, blocks when it must' },
  { kind: 'RANDOM', label: 'Random', hint: 'Any free cell' },
]

function blunderHint(rate: number): string {
  if (rate === 0) {
    return 'Never slips — every match is a draw'
  }
  if (rate >= 0.5) {
    return 'Slips on most moves — barely better than random'
  }
  return 'Skips its reasoning on some moves, so matches get a winner'
}

export function MatchSettings({ settings, onChange, disabled }: MatchSettingsProps): ReactElement {
  const blunderPercent = Math.round(settings.blunderRate * 100)

  function handleStrategyChange(kind: StrategyKind): void {
    onChange({ ...settings, strategy: kind })
  }

  function handleBlunderChange(event: ChangeEvent<HTMLInputElement>): void {
    onChange({ ...settings, blunderRate: Number(event.target.value) / 100 })
  }

  return (
    <fieldset disabled={disabled} className="flex flex-col gap-4 disabled:opacity-60">
      <legend className="font-display text-sm font-medium tracking-wide text-ink/60 uppercase">How both players play</legend>

      <div role="radiogroup" aria-label="Strategy" className="inline-flex self-start rounded-full bg-frost/40 p-1">
        {STRATEGIES.map(({ kind, label, hint }) => {
          const selected = settings.strategy === kind
          return (
            <label
              key={kind}
              title={hint}
              className={`cursor-pointer rounded-full px-4 py-1.5 font-display text-sm font-medium transition-colors has-[:focus-visible]:ring-4 has-[:focus-visible]:ring-frost ${
                selected ? 'bg-ink text-paper' : 'text-ink hover:bg-frost/60'
              }`}
            >
              <input
                type="radio"
                name="strategy"
                value={kind}
                checked={selected}
                onChange={() => handleStrategyChange(kind)}
                className="sr-only"
              />
              {label}
            </label>
          )
        })}
      </div>

      {settings.strategy === 'HEURISTIC' && (
        <label className="flex max-w-[26rem] flex-col gap-1">
          <span className="flex items-baseline justify-between font-data text-sm text-ink">
            <span>Blunders</span>
            <span className="tabular-nums">{blunderPercent}%</span>
          </span>
          <input
            type="range"
            min={0}
            max={100}
            step={5}
            value={blunderPercent}
            onChange={handleBlunderChange}
            aria-valuetext={`${blunderPercent} percent`}
            className="accent-ink"
          />
          <span className="font-data text-xs text-ink/60">{blunderHint(settings.blunderRate)}</span>
        </label>
      )}
    </fieldset>
  )
}
