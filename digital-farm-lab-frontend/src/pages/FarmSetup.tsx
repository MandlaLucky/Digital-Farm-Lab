import { useEffect, useState } from 'react'
import { Button, Icon, Input, Select, Stepper } from '../components/ui'
import { waterLayoutSlots, type FarmConfig, type WaterSourceType } from '../farmTypes'
import type { PersistedFarm } from '../scenarioTypes'

const waterSources: WaterSourceType[] = ['dam', 'borehole', 'river', 'reservoir']
const waterNotes: Record<string, string> = { dam: 'Large surface storage', borehole: 'Groundwater, steady supply', river: 'Flowing, seasonal', reservoir: 'Engineered storage' }

/** Typed number entry that tolerates in-progress input like "-" and clamps on blur. */
function NumberField({ value, min, max, unit, onChange, label }: { value: number; min: number; max: number; unit: string; onChange: (value: number) => void; label: string }) {
  const [text, setText] = useState(String(value))
  useEffect(() => { setText(String(value)) }, [value])
  const commit = (raw: string) => {
    const n = parseFloat(raw)
    if (Number.isNaN(n)) { setText(String(value)); return }
    const clamped = Math.max(min, Math.min(max, n))
    onChange(clamped); setText(String(clamped))
  }
  return (
    <span className="number-field">
      <input className="input" inputMode="decimal" aria-label={label} value={text} onChange={e => { setText(e.target.value); const n = parseFloat(e.target.value); if (!Number.isNaN(n) && n >= min && n <= max) onChange(n) }} onBlur={e => commit(e.target.value)} onKeyDown={e => e.key === 'Enter' && commit(e.currentTarget.value)} />
      <em>{unit}</em>
    </span>
  )
}

export default function FarmSetup({ farm, farms, onChange, onLoad, onNext }: {
  farm: FarmConfig
  farms: PersistedFarm[]
  onChange: (patch: Partial<FarmConfig>) => void
  onLoad: (farm: PersistedFarm) => void
  onNext: () => void
}) {
  const checks = [
    { label: 'Farm named', ok: farm.name.trim().length > 0 },
    { label: 'Location set', ok: farm.location.trim().length > 0 },
    { label: `Water source added (${farm.waterSources.length})`, ok: farm.waterSources.length > 0 },
  ]
  const ready = checks.every(c => c.ok)

  return (
    <div className="page setup-page">
      <div className="page-title">
        <div><span>1 · FARM SETUP</span><strong>Set up your farm</strong><p>Describe the farm, its water supply and the baseline weather. You will lay out the fields in the next step.</p></div>
        <Button className="primary-action" onClick={onNext} disabled={!ready}>Continue to Farm Builder <Icon name="arrow" size={15} /></Button>
      </div>

      <div className="setup-grid">
        <section className="panel-card">
          <div className="panel-card-title"><span>FARM DETAILS</span><strong>Identity & size</strong></div>
          <label className="builder-control"><span>Farm name</span><Input value={farm.name} placeholder="e.g. Green Valley Farm" onChange={e => onChange({ name: e.target.value })} /></label>
          <label className="builder-control"><span>Location</span><Input value={farm.location} placeholder="Town, region or coordinates" onChange={e => onChange({ location: e.target.value })} /></label>
          <div className="builder-control inline"><span>Total farm size</span><Stepper value={farm.totalSize} unit=" ha" min={5} max={500} step={5} onChange={totalSize => onChange({ totalSize })} /></div>

          <div className="panel-card-title spaced"><span>WATER SUPPLY</span><strong>Primary water source</strong></div>
          <p className="muted-note">Add as many sources as the farm has. You can add the same type more than once and move them in the Farm Builder.</p>
          <div className="choice-grid">
            {waterSources.map(source => (
              <Button key={source} className="choice" onClick={() => {
                const [x, y] = waterLayoutSlots[farm.waterSources.length % waterLayoutSlots.length]
                onChange({ waterSources: [...farm.waterSources, { id: `water-${Date.now()}`, type: source, x, y }] })
              }}>
                <Icon name="water" size={18} /><strong>+ {source}</strong><small>{waterNotes[source]}</small>
              </Button>
            ))}
          </div>
          {farm.waterSources.length > 0 && (
            <ul className="source-list">
              {farm.waterSources.map((w, i) => (
                <li key={w.id}><Icon name="water" size={15} /><span><strong>{w.type}</strong><small>Source {i + 1}</small></span>
                  <Button aria-label={`Remove ${w.type}`} onClick={() => onChange({ waterSources: farm.waterSources.filter(x => x.id !== w.id) })}><Icon name="trash" size={14} /></Button></li>
              ))}
            </ul>
          )}
        </section>

        <section className="panel-card">
          <div className="panel-card-title"><span>INITIAL WEATHER</span><strong>Simulation baseline</strong></div>
          <label className="builder-control"><span>Conditions</span>
            <Select value={farm.weather} onChange={e => onChange({ weather: e.target.value as FarmConfig['weather'] })}>
              <option value="clear">Clear skies</option><option value="cloudy">Cloudy</option><option value="rain">Light rain</option><option value="hot">Hot and dry</option>
            </Select>
          </label>
          <div className="builder-control"><span>Temperature (from −270 °C, no upper limit)</span>
            <div className="control-pair"><NumberField label="Temperature" value={farm.temperature} min={-270} max={Infinity} unit="°C" onChange={temperature => onChange({ temperature })} />
              <Stepper value={farm.temperature} min={-270} max={Infinity} onChange={temperature => onChange({ temperature })} /></div>
            <small className="hint">{farm.temperature <= -100 ? 'Far below any farmland temperature; useful for extreme experiments.' : farm.temperature < 0 ? 'Below freezing: crops will be frost-stressed.' : farm.temperature > 45 ? 'Extreme heat: expect severe crop stress.' : 'Within a normal growing range.'}</small></div>
          <div className="builder-control"><span>Relative humidity</span>
            <div className="control-pair"><input type="range" className="range" min={0} max={100} step={1} value={farm.humidity} aria-label="Humidity slider" onChange={e => onChange({ humidity: Number(e.target.value) })} />
              <NumberField label="Humidity" value={farm.humidity} min={0} max={100} unit="%" onChange={humidity => onChange({ humidity })} /></div>
            <small className="hint">0% is bone dry air, 100% is saturated. {farm.humidity < 30 ? 'Dry air raises evaporation.' : farm.humidity > 80 ? 'Humid air slows evaporation and favours disease.' : 'A comfortable middle range.'}</small></div>

          <div className="panel-card-title spaced"><span>READINESS</span><strong>Before you continue</strong></div>
          <ul className="check-list">
            {checks.map(c => <li key={c.label} className={c.ok ? 'ok' : ''}><i>{c.ok ? '✓' : ''}</i>{c.label}</li>)}
          </ul>
        </section>

        <section className="panel-card">
          <div className="panel-card-title"><span>FARM LIBRARY</span><strong>Start from a saved farm</strong></div>
          {farms.length === 0 && <p className="muted-note">No saved farms yet. Farms you save in the Farm Builder appear here.</p>}
          <div className="saved-farms">
            {farms.map(f => (
              <Button key={f.id} className="saved-farm" onClick={() => onLoad(f)}>
                <div><Icon name="field" size={17} /></div>
                <span><strong>{f.name}</strong><small>{f.location} · {f.fields.length} fields · {f.totalSize} ha</small></span>
                <em>Load</em>
              </Button>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
