import { useEffect, useState } from 'react'
import { Button, Icon } from './ui'
import { runEngine, type EngineRunResponse } from '../engine'
import './engine-modal.css'

type Props = {
  farmName: string
  scenarioName?: string
  onClose: () => void
}

const PRESETS: { label: string; irr: number; fer: number; offset: number }[] = [
  { label: 'Baseline', irr: 550, fer: 180, offset: 0 },
  { label: '20% less water', irr: 440, fer: 180, offset: 0 },
  { label: 'Max fertiliser', irr: 550, fer: 300, offset: 0 },
  { label: 'Late planting', irr: 550, fer: 180, offset: 30 },
]

export default function EngineModal({ farmName, scenarioName, onClose }: Props) {
  const [irrigation, setIrrigation] = useState(550)
  const [fertiliser, setFertiliser] = useState(180)
  const [plantingOffset, setPlantingOffset] = useState(0)
  const [result, setResult] = useState<EngineRunResponse | null>(null)
  const [error, setError] = useState('')
  const [isPending, setIsPending] = useState(false)

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  const run = async () => {
    setError('')
    setResult(null)
    setIsPending(true)
    try {
      const base = new Date('2026-10-15T00:00:00Z')
      base.setUTCDate(base.getUTCDate() + plantingOffset)
      const plantingDate = base.toISOString().slice(0, 10)
      const res = await runEngine({ irrigationMm: irrigation, fertiliserKgHa: fertiliser, plantingDate })
      setResult(res)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Engine unreachable')
    } finally {
      setIsPending(false)
    }
  }

  return (
    <div className="engine-modal-backdrop" onClick={onClose}>
      <div className="engine-modal" onClick={e => e.stopPropagation()}>
        <header className="engine-modal-header">
          <div>
            <span>A14 ENGINE</span>
            <strong>{farmName}{scenarioName ? ` · ${scenarioName}` : ''}</strong>
          </div>
          <button className="engine-modal-close" onClick={onClose} aria-label="Close">×</button>
        </header>

        <div className="engine-presets">
          {PRESETS.map(p => {
            const active = p.irr === irrigation && p.fer === fertiliser && p.offset === plantingOffset
            return (
              <button
                key={p.label}
                className={active ? 'active' : ''}
                onClick={() => { setIrrigation(p.irr); setFertiliser(p.fer); setPlantingOffset(p.offset) }}
              >
                {p.label}
              </button>
            )
          })}
        </div>

        <div className="engine-sliders">
          <Slider label="Irrigation" value={irrigation} min={0} max={900} step={10} unit=" mm" onChange={setIrrigation} />
          <Slider label="Fertiliser" value={fertiliser} min={0} max={300} step={10} unit=" kg/ha" onChange={setFertiliser} />
          <Slider label="Planting offset" value={plantingOffset} min={-30} max={60} step={1} unit=" days" sign onChange={setPlantingOffset} />
        </div>

        <Button className="engine-run" onClick={run} disabled={isPending}>
          <Icon name="play" size={15} />
          {isPending ? 'Running…' : 'Run engine'}
        </Button>

        {error && <div className="engine-error">{error}</div>}

        {result && (
          <div className="engine-results">
            <div className="engine-results-title">
              <span>ENGINE OUTPUT</span>
              <small>compared to baseline</small>
            </div>
            <div className="engine-result-grid">
              <ResultCell label="Yield" value={result.scenario.yieldTha} baseline={result.baseline.yieldTha} unit="t/ha" precision={2} />
              <ResultCell label="Water use" value={result.scenario.waterUseM3Ha} baseline={result.baseline.waterUseM3Ha} unit="m³/ha" precision={0} />
              <ResultCell label="Cost" value={result.scenario.costPerHa} baseline={result.baseline.costPerHa} unit="R/ha" precision={0} />
              <ResultCell label="Gross margin" value={result.scenario.grossMarginPerHa} baseline={result.baseline.grossMarginPerHa} unit="R/ha" precision={0} />
            </div>
            <p className="engine-disclaimer">{result.scenario.disclaimer}</p>
          </div>
        )}
      </div>
    </div>
  )
}

function Slider({ label, value, min, max, step, unit, sign = false, onChange }: {
  label: string; value: number; min: number; max: number; step: number; unit: string; sign?: boolean; onChange: (v: number) => void
}) {
  return (
    <label className="engine-slider">
      <span>{label}</span>
      <input type="range" min={min} max={max} step={step} value={value} onChange={e => onChange(Number(e.target.value))} />
      <strong>{sign && value >= 0 ? '+' : ''}{value}{unit}</strong>
    </label>
  )
}

function ResultCell({ label, value, baseline, unit, precision }: {
  label: string; value: number; baseline: number; unit: string; precision: number
}) {
  const diff = value - baseline
  const pct = baseline !== 0 ? (diff / baseline) * 100 : 0
  const tone = diff > 0.005 ? 'up' : diff < -0.005 ? 'down' : 'flat'
  const arrow = diff > 0.005 ? '↑' : diff < -0.005 ? '↓' : '·'
  return (
    <div className="engine-result-cell">
      <span>{label}</span>
      <strong>{value.toFixed(precision)}<small> {unit}</small></strong>
      <em className={tone}>{arrow} {pct === 0 ? 'no change' : `${Math.abs(pct).toFixed(1)}%`}</em>
      <i>baseline {baseline.toFixed(precision)} {unit}</i>
    </div>
  )
}