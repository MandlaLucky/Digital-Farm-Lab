import { useEffect, useMemo, useRef, useState } from 'react'
import { Button, Icon, Select } from '../components/ui'
import type { FarmConfig } from '../farmTypes'
import type { RunRecord, ScenarioRecord } from '../scenarioTypes'
import { uid } from '../store'
import EngineModal from '../components/EngineModal'

type SimStatus = 'idle' | 'running' | 'paused' | 'stopped' | 'complete'
type ComponentKind = 'field' | 'crop' | 'irrigation' | 'water' | 'sensor'

type SelectedComponent = {
  id: string
  kind: ComponentKind
  name: string
}

type LibraryItem = { id: string; name: string; icon: string; kind: ComponentKind; note: string }
export type PlacedItem = { uid: string; libId: string; name: string; kind: ComponentKind; icon: string; x: number; y: number }

const library: { title: string; items: LibraryItem[] }[] = [
  { title: 'LAND & CROPS', items: [
    { id: 'field', name: 'Field plot', icon: 'field', kind: 'field', note: 'Extra plot: adds water demand' },
    { id: 'maize', name: 'Maize crop', icon: 'leaf', kind: 'crop', note: 'Extra maize plot: adds water demand' },
    { id: 'wheat', name: 'Wheat crop', icon: 'leaf', kind: 'crop', note: 'Extra wheat plot: adds water demand' },
    { id: 'orchard', name: 'Orchard', icon: 'grid', kind: 'crop', note: 'Perennial block: adds water demand' },
  ] },
  { title: 'WATER SYSTEMS', items: [
    { id: 'drip', name: 'Drip line', icon: 'droplet', kind: 'irrigation', note: '+3 L/min, +2% moisture' },
    { id: 'pivot', name: 'Center pivot', icon: 'irrigation', kind: 'irrigation', note: '+5 L/min, +3% moisture' },
    { id: 'pump', name: 'Water pump', icon: 'settings', kind: 'water', note: 'Improves pumping efficiency' },
    { id: 'reservoir', name: 'Reservoir', icon: 'water', kind: 'water', note: '+800 m³ storage' },
  ] },
  { title: 'INSTRUMENTS', items: [
    { id: 'soil', name: 'Soil probe', icon: 'sensor', kind: 'sensor', note: '+1 data channel, better control' },
    { id: 'weather', name: 'Weather station', icon: 'sun', kind: 'sensor', note: '+1 data channel, better control' },
    { id: 'flow', name: 'Flow meter', icon: 'chart', kind: 'sensor', note: '+1 data channel, better control' },
  ] },
]
const allLibraryItems = library.flatMap(g => g.items)

const makeInitialEvents = (farm: FarmConfig, scenario?: ScenarioRecord) => [
  { time: '06:00:00', tone: 'info', text: `Model initialized with ${farm.fields.length} field zone${farm.fields.length === 1 ? '' : 's'}` },
  { time: '06:00:01', tone: 'ok', text: scenario ? `${scenario.category} scenario loaded` : 'All sensor nodes connected' },
]

function MiniChart({ values, color = 'green', label }: { values: number[]; color?: 'green' | 'blue' | 'amber'; label: string }) {
  const points = values.map((value, index) => `${(index / Math.max(1, values.length - 1)) * 240},${54 - value * .42}`).join(' ')
  return (
    <div className={`mini-chart ${color}`}>
      <svg viewBox="0 0 240 58" preserveAspectRatio="none" aria-label={label}>
        <path d="M0 14H240M0 34H240M0 54H240" className="chart-grid-lines" />
        <polyline points={points} fill="none" vectorEffect="non-scaling-stroke" />
      </svg>
    </div>
  )
}

function ValueStepper({ label, value, unit, onChange, step = 1 }: { label: string; value: number; unit: string; onChange: (value: number) => void; step?: number }) {
  return (
    <div className="property-row">
      <span>{label}</span>
      <div className="stepper">
        <Button onClick={() => onChange(Math.max(0, value - step))} aria-label={`Decrease ${label}`}>−</Button>
        <strong>{value}{unit}</strong>
        <Button onClick={() => onChange(value + step)} aria-label={`Increase ${label}`}>+</Button>
      </div>
    </div>
  )
}

export default function SimulationWorkspace({ farm, scenario, onBack, onScenarios, onFinish }: { farm: FarmConfig; scenario?: ScenarioRecord; onBack: () => void; onScenarios: () => void; onFinish: (run: RunRecord) => void }) {
  const firstField = farm.fields[0]
  const initialEvents = useMemo(() => makeInitialEvents(farm, scenario), [farm, scenario])
  const [status, setStatus] = useState<SimStatus>('idle')
  const [speed, setSpeed] = useState(1)
  const [progress, setProgress] = useState(0)
  const [selected, setSelected] = useState<SelectedComponent>({ id: firstField.id, kind: 'field', name: `${firstField.name} · ${firstField.crop}` })
  const [flowRate, setFlowRate] = useState(22)
  const [targetMoisture, setTargetMoisture] = useState(62)
  const [nitrogen, setNitrogen] = useState(118)
  const [events, setEvents] = useState(initialEvents)
  const [zoom, setZoom] = useState(100)
  const [placed, setPlaced] = useState<PlacedItem[]>([])
  const [past, setPast] = useState<PlacedItem[][]>([])
  const [future, setFuture] = useState<PlacedItem[][]>([])
  const [query, setQuery] = useState('')
  const [libraryOpen, setLibraryOpen] = useState(true)
  const [tool, setTool] = useState<'select' | 'pan' | 'connect'>('select')
  const [engineOpen, setEngineOpen] = useState(false)
  const canvasRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  const dragRef = useRef<{ uid: string; dx: number; dy: number } | null>(null)
  const placedCount = useRef(0)
  const firedEvents = useRef(new Set<number>())

  useEffect(() => {
    if (status !== 'running') return
    const timer = window.setInterval(() => {
      setProgress(current => {
        const next = Math.min(100, current + .055 * speed)
        if (next >= 100) setStatus('complete')
        return next
      })
    }, 100)
    return () => window.clearInterval(timer)
  }, [status, speed])

  useEffect(() => {
    const milestones = [
      { at: 6, tone: 'ok', text: 'Irrigation cycle 01 opened · Field A' },
      { at: 20, tone: 'info', text: 'Germination threshold reached' },
      { at: 38, tone: 'warn', text: 'High evapotranspiration forecast' },
      { at: 52, tone: 'ok', text: 'Moisture target reached · Zone A2' },
      { at: 72, tone: 'info', text: 'Crop stage changed to tasseling' },
      { at: 88, tone: 'ok', text: 'Irrigation cycle 04 completed' },
    ]
    milestones.forEach(item => {
      if (progress >= item.at && !firedEvents.current.has(item.at)) {
        firedEvents.current.add(item.at)
        setEvents(current => [{ time: simulationTime(progress), tone: item.tone, text: item.text }, ...current].slice(0, 40))
      }
    })
  }, [progress])

  const startingMoisture = scenario?.startingConditions.soilMoisture ?? 37
  const startingTemperature = scenario?.startingConditions.temperature ?? farm.temperature
  const count = (libId: string) => placed.filter(p => p.libId === libId).length
  const kindCount = (kind: ComponentKind) => placed.filter(p => p.kind === kind).length
  const landPlots = placed.filter(p => p.kind === 'field' || p.kind === 'crop').length
  const flowBoost = count('drip') * 3 + count('pivot') * 5
  const moistureBonus = Math.min(8, count('drip') * 2 + count('pivot') * 3)
  const monitoringBonus = Math.min(8, kindCount('sensor') * 2)
  const pumpBonus = Math.min(6, count('pump') * 3)
  const storageBonus = count('reservoir') * 800
  const effectiveFlow = flowRate + flowBoost
  const moisture = Math.round(Math.min(92, startingMoisture + moistureBonus + progress * .18 + Math.sin(progress / 5) * 5))
  const temperature = Number((startingTemperature + Math.sin(progress / 13) * (farm.weather === 'hot' ? 6.2 : 4.1)).toFixed(1))
  const humidity = Math.round(Math.max(0, Math.min(100, farm.humidity - Math.sin(progress / 11) * Math.min(10, farm.humidity, 100 - farm.humidity + 10))))
  const growth = Math.round(Math.min(100, progress * .92))
  const waterUsed = Math.round(progress * effectiveFlow * .84 * (1 + .06 * landPlots))
  const growthStage = progress < 10 ? 'Seedbed' : progress < 28 ? 'Emergence' : progress < 55 ? 'Vegetative' : progress < 78 ? 'Tasseling' : 'Maturity'
  const chartValues = useMemo(() => Array.from({ length: 30 }, (_, i) => {
    const sampleProgress = Math.max(0, progress - (29 - i) * 1.5)
    return Math.min(92, 35 + sampleProgress * .3 + Math.sin(sampleProgress / 4) * 6)
  }), [progress])

  const lastSample = useRef(-10)
  const samples = useRef<RunRecord['series']>([])
  const moistureAcc = useRef({ sum: 0, n: 0 })
  useEffect(() => {
    if (progress === 0) { samples.current = []; lastSample.current = -10; moistureAcc.current = { sum: 0, n: 0 }; return }
    moistureAcc.current.sum += Math.abs(moisture - targetMoisture)
    moistureAcc.current.n += 1
    if (progress - lastSample.current >= 2 || progress >= 100) {
      lastSample.current = progress
      samples.current.push({ day: Math.max(1, Math.ceil(progress * ((scenario?.duration ?? 120) / 100))), moisture, growth, water: waterUsed })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [progress])

  const finish = () => {
    setStatus(progress >= 100 ? 'complete' : 'stopped')
    const clamp = (n: number) => Math.max(0, Math.min(100, Math.round(n)))
    const avgGap = moistureAcc.current.n ? moistureAcc.current.sum / moistureAcc.current.n : 0
    const scores = {
      moisture: clamp(100 - avgGap * 2.2 + monitoringBonus),
      water: clamp(100 - Math.abs(flowRate - 22) * 3.5 + pumpBonus),
      nutrients: clamp(100 - Math.abs(nitrogen - 120) * 0.9),
      overall: 0,
    }
    scores.overall = Math.round((scores.moisture + scores.water + scores.nutrients) / 3)
    const factor = (scores.moisture * .5 + scores.nutrients * .3 + scores.water * .2) / 100
    const objectives = (scenario?.learningObjectives ?? ['Understand how irrigation and fertiliser decisions affect crop growth.']).map((text, i) => ({ text, achieved: progress >= 60 && scores.overall >= 55 + i * 6 }))
    onFinish({
      id: uid('run'),
      scenarioId: scenario?.id,
      finishedAt: new Date().toISOString(),
      complete: progress >= 100,
      farmName: farm.name,
      scenarioName: scenario?.name ?? 'Free simulation',
      category: scenario?.category ?? 'Free play',
      difficulty: scenario?.difficulty ?? 'Open',
      duration: scenario?.duration ?? 120,
      dayReached: Math.max(1, Math.ceil(progress * ((scenario?.duration ?? 120) / 100))),
      series: samples.current.length ? samples.current : [{ day: 1, moisture, growth, water: waterUsed }],
      decisions: { targetMoisture, flowRate, nitrogen },
      metrics: { moisture, growth, waterUsed, yieldTHa: Number((3.5 + growth / 100 * 8.5 * factor).toFixed(1)), stress: Math.max(2, 18 - Math.round(progress / 8)) },
      scores,
      objectives,
      details: {
        start: { soilMoisture: startingMoisture, temperature: startingTemperature, rainfall: scenario?.startingConditions.rainfall ?? 8, budget: scenario?.startingConditions.budget ?? 25000, pestPressure: scenario?.startingConditions.pestPressure ?? 5, humidity: farm.humidity, weather: farm.weather },
        avgMoistureGap: Number(avgGap.toFixed(1)),
        effectiveFlow,
        yieldFactor: Number(factor.toFixed(2)),
        bonuses: { monitoring: monitoringBonus, moisture: moistureBonus, pump: pumpBonus, flow: flowBoost, storage: storageBonus, landPlots },
        components: placed.map(p => ({ name: p.name, kind: p.kind })),
        events: [...events].reverse(),
        fields: farm.fields.map(f => ({ name: f.name, crop: f.crop, soil: f.soil, irrigation: f.irrigation, size: f.size })),
        waterSources: farm.waterSources.map(w => w.type),
        adjustable: scenario?.adjustableVariables ?? [],
      },
    })
  }

  const selectCanvasItem = (item: SelectedComponent) => setSelected(item)
  const commit = (next: PlacedItem[]) => { setPast(p => [...p.slice(-30), placed]); setFuture([]); setPlaced(next) }
  const logEvent = (text: string) => setEvents(current => [{ time: simulationTime(progress), tone: 'info', text }, ...current].slice(0, 40))
  const place = (item: LibraryItem, at?: { x: number; y: number }) => {
    const n = placedCount.current++
    const next: PlacedItem = { uid: `placed-${Date.now()}-${n}`, libId: item.id, name: item.name, kind: item.kind, icon: item.icon, x: at?.x ?? 8 + (n % 5) * 16, y: at?.y ?? 62 + Math.floor(n / 5 % 3) * 11 }
    commit([...placed, next])
    setSelected({ id: next.uid, kind: item.kind, name: item.name })
    logEvent(`Placed ${item.name}`)
  }
  const removePlaced = (uid: string) => {
    const item = placed.find(p => p.uid === uid)
    if (!item) return
    commit(placed.filter(p => p.uid !== uid))
    setSelected({ id: firstField.id, kind: 'field', name: `${firstField.name} · ${firstField.crop}` })
    logEvent(`Removed ${item.name}`)
  }
  const duplicatePlaced = (uid: string) => {
    const item = placed.find(p => p.uid === uid)
    if (!item) return
    const copy = { ...item, uid: `placed-${Date.now()}-${placedCount.current++}`, x: Math.min(88, item.x + 5), y: Math.min(86, item.y + 6) }
    commit([...placed, copy])
    setSelected({ id: copy.uid, kind: copy.kind, name: copy.name })
    logEvent(`Duplicated ${item.name}`)
  }
  const undo = () => { const prev = past[past.length - 1]; if (!prev) return; setFuture(f => [placed, ...f]); setPast(past.slice(0, -1)); setPlaced(prev) }
  const redo = () => { const next = future[0]; if (!next) return; setPast(p => [...p, placed]); setFuture(future.slice(1)); setPlaced(next) }
  const pointerPct = (e: { clientX: number; clientY: number }) => {
    const r = canvasRef.current?.getBoundingClientRect()
    return r ? { x: ((e.clientX - r.left) / r.width) * 100, y: ((e.clientY - r.top) / r.height) * 100 } : { x: 50, y: 50 }
  }
  const beginDrag = (e: React.PointerEvent, p: PlacedItem) => {
    setSelected({ id: p.uid, kind: p.kind, name: p.name })
    if (tool !== 'select') return
    const pt = pointerPct(e)
    dragRef.current = { uid: p.uid, dx: pt.x - p.x, dy: pt.y - p.y }
    e.currentTarget.setPointerCapture(e.pointerId)
  }
  const moveDrag = (e: React.PointerEvent) => {
    const d = dragRef.current
    if (!d) return
    const pt = pointerPct(e)
    const x = Math.max(1, Math.min(90, pt.x - d.dx)), y = Math.max(2, Math.min(88, pt.y - d.dy))
    setPlaced(list => list.map(p => p.uid === d.uid ? { ...p, x, y } : p))
  }
  const endDrag = () => { dragRef.current = null }
  const onDropLibrary = (e: React.DragEvent) => {
    e.preventDefault()
    const item = allLibraryItems.find(i => i.id === e.dataTransfer.getData('text/plain'))
    if (item) place(item, (() => { const pt = pointerPct(e); return { x: Math.max(1, Math.min(88, pt.x - 3)), y: Math.max(2, Math.min(84, pt.y - 4)) } })())
  }
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); setLibraryOpen(true); setTimeout(() => searchRef.current?.focus(), 0) } }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])
  const selectedPlaced = placed.find(p => p.uid === selected.id)
  const filteredLibrary = library.map(g => ({ ...g, items: g.items.filter(i => i.name.toLowerCase().includes(query.trim().toLowerCase())) })).filter(g => g.items.length)

  const reset = () => {
    setStatus('idle')
    setProgress(0)
    setEvents(initialEvents)
    firedEvents.current.clear()
  }
  const start = () => {
    if (progress >= 100) reset()
    setStatus('running')
  }

  return (
    <div className="simulator-shell">
      <div className="editor-toolbar">
        <div className="tool-group">
          <Button className={`tool ${tool === 'select' ? 'active' : ''}`} aria-label="Select tool" title="Select and move" onClick={() => setTool('select')}><Icon name="cursor" size={16} /></Button>
          <Button className={`tool ${tool === 'pan' ? 'active' : ''}`} aria-label="Pan tool" title="Pan (components are locked)" onClick={() => setTool('pan')}><Icon name="hand" size={16} /></Button>
          <Button className={`tool ${tool === 'connect' ? 'active' : ''}`} aria-label="Connect tool" title="Connect (components are locked)" onClick={() => setTool('connect')}><Icon name="connect" size={16} /></Button>
        </div>
        <span className="toolbar-separator" />
        <div className="tool-group">
          <Button className="tool" aria-label="Undo" title="Undo placement" onClick={undo} disabled={!past.length}><Icon name="undo" size={16} /></Button>
          <Button className="tool" aria-label="Redo" title="Redo placement" onClick={redo} disabled={!future.length}><Icon name="redo" size={16} /></Button>
        </div>
        <span className="toolbar-project"><em>{scenario ? 'SCENARIO' : 'FARM'}</em> {scenario?.name ?? farm.name}</span>
        <div className="toolbar-center">
          <span className={`status-indicator ${status}`}><i /> {status === 'running' ? 'SIMULATION RUNNING' : status.toUpperCase()}</span>
          <span>Day {Math.max(1, Math.ceil(progress * ((scenario?.duration ?? 120) / 100)))} of {scenario?.duration ?? 120}</span>
          <strong>{simulationTime(progress)}</strong>
        </div>
        <div className="tool-group toolbar-right">
          <Button className="header-button" onClick={onBack}><Icon name="folder" size={14} /> Farm builder</Button>
          <Button className="header-button" onClick={onScenarios}><Icon name="flask" size={14} /> Scenarios</Button>
          <Button className="header-button" onClick={() => setEngineOpen(true)}><Icon name="chart" size={14} /> Engine</Button>
          <span className="toolbar-separator" />
          <Button className="tool" onClick={() => setZoom(Math.max(50, zoom - 10))} aria-label="Zoom out">−</Button>
          <span className="zoom-value">{zoom}%</span>
          <Button className="tool" onClick={() => setZoom(Math.min(150, zoom + 10))} aria-label="Zoom in">+</Button>
          <Button className="tool" aria-label="Fit canvas" title="Reset zoom" onClick={() => setZoom(100)}><Icon name="fullscreen" size={16} /></Button>
        </div>
      </div>

      <div className={`workspace ${libraryOpen ? '' : 'lib-collapsed'}`}>
        <aside className="component-library">
          <div className="panel-heading"><div><span>COMPONENTS</span><strong>Farm library</strong></div><Button aria-label={libraryOpen ? 'Collapse library' : 'Expand library'} onClick={() => setLibraryOpen(o => !o)}>{libraryOpen ? '‹' : '›'}</Button></div>
          {libraryOpen && <>
          <label className="library-search"><Icon name="search" size={15} /><input ref={searchRef} value={query} onChange={e => setQuery(e.target.value)} placeholder="Search components" aria-label="Search components" /><kbd>⌘K</kbd></label>
          <div className="library-scroll">
            {filteredLibrary.length === 0 && <p className="library-empty">No components match “{query}”.</p>}
            {filteredLibrary.map(group => (
              <div className="library-group" key={group.title}>
                <span>{group.title}</span>
                <div className="component-grid">
                  {group.items.map(item => (
                    <Button key={item.id} title={item.note} draggable onDragStart={e => e.dataTransfer.setData('text/plain', item.id)} className={selectedPlaced?.libId === item.id ? 'selected' : ''} onClick={() => place(item)}>
                      <div><Icon name={item.icon} size={20} /></div><strong>{item.name}</strong>{count(item.id) > 0 && <b className="placed-count">{count(item.id)}</b>}
                    </Button>
                  ))}
                </div>
              </div>
            ))}
            {placed.length > 0 && (
              <div className="library-group">
                <span>PLACED ({placed.length})</span>
                <div className="placed-list">{placed.map(p => <Button key={p.uid} className={selected.id === p.uid ? 'selected' : ''} onClick={() => setSelected({ id: p.uid, kind: p.kind, name: p.name })}><Icon name={p.icon} size={13} /><em>{p.name}</em></Button>)}</div>
              </div>
            )}
          </div>
          <div className="library-hint"><Icon name="info" size={15} /><span>Click a component, or drag it onto the workspace, to place it. Drag placed items to move them.</span></div>
          </>}
        </aside>

        <main className="canvas-column">
          <div ref={canvasRef} onDragOver={e => e.preventDefault()} onDrop={onDropLibrary} onPointerMove={moveDrag} onPointerUp={endDrag} className={`farm-canvas tool-${tool} ${status === 'running' ? 'is-running' : ''}`} style={{ '--canvas-scale': zoom / 100 } as React.CSSProperties}>
            <div className="canvas-grid" />
            <div className="canvas-label north">N <span>↑</span></div>
            <div className="farm-road road-one" />
            <div className="farm-road road-two" />
            <div className="water-canal"><span /><span /><span /></div>

            {farm.fields.map((field, index) => (
              <Button
                key={field.id}
                className={`farm-field sim-config-field soil-${field.soil} crop-${field.crop} ${selected.id === field.id ? 'selected' : ''}`}
                style={{ left: `${field.x}%`, top: `${field.y}%`, width: `${field.width}%`, height: `${field.height}%` }}
                onClick={() => selectCanvasItem({ id: field.id, kind: 'field', name: `${field.name} · ${field.crop}` })}
              >
                <span className={`crop-rows ${field.crop}`} />
                <span className="field-tag"><strong>{field.name.toUpperCase()}</strong><small>{field.crop.toUpperCase()} · {field.size} ha</small></span>
                <span className="moisture-badge"><Icon name="droplet" size={12} /> {Math.max(25, Math.min(88, moisture + index * 3 - 4))}%</span>
              </Button>
            ))}

            {farm.waterSources.map((w, i) => (
              <Button key={w.id} className={`reservoir ${selected.id === `water-${w.id}` ? 'selected' : ''}`} style={{ left: `${Math.min(w.x, 72)}%`, top: `${Math.min(w.y, 70)}%`, right: 'auto', bottom: 'auto', width: i === 0 ? undefined : '17%', height: i === 0 ? undefined : '19%' }} onClick={() => selectCanvasItem({ id: `water-${w.id}`, kind: 'water', name: `${w.type} ${i + 1}` })}>
                <span className="water-ripple" /><Icon name="water" size={22} /><strong>{w.type.toUpperCase()}</strong><small>{Math.round(84 - progress * .12)}% · {Math.round((2480 + storageBonus) / farm.waterSources.length).toLocaleString()} m³</small>
              </Button>
            ))}
            <Button className={`pump-node ${selected.id === 'pump-01' ? 'selected' : ''}`} onClick={() => selectCanvasItem({ id: 'pump-01', kind: 'water', name: 'Pump station P-01' })}>
              <Icon name="settings" size={19} /><span><strong>P-01</strong><small>{status === 'running' ? 'ACTIVE' : 'STANDBY'}</small></span>
            </Button>
            <div className="pipe pipe-main"><i /><i /><i /><i /><i /></div>
            <div className="pipe pipe-branch"><i /><i /><i /></div>

            {farm.fields.some(field => field.irrigation !== 'none') && <Button className={`pivot pivot-a ${selected.id === 'pivot-a' ? 'selected' : ''}`} onClick={() => selectCanvasItem({ id: 'pivot-a', kind: 'irrigation', name: `${firstField.irrigation} irrigation` })}>
              <span className="pivot-arm"><i /><i /><i /><i /></span><span className="pivot-hub" />
              <span className="spray spray-one" /><span className="spray spray-two" /><span className="spray spray-three" />
            </Button>}
            <Button className={`sensor-node sensor-one ${selected.id === 'sm-01' ? 'selected' : ''}`} onClick={() => selectCanvasItem({ id: 'sm-01', kind: 'sensor', name: 'Soil sensor SM-01' })}><Icon name="sensor" size={14} /><span>SM-01</span></Button>
            <Button className="sensor-node sensor-two" onClick={() => selectCanvasItem({ id: 'sm-02', kind: 'sensor', name: 'Soil sensor SM-02' })}><Icon name="sensor" size={14} /><span>SM-02</span></Button>
            <Button className="weather-station" onClick={() => selectCanvasItem({ id: 'wx-01', kind: 'sensor', name: 'Weather station WX-01' })}><Icon name="antenna" size={20} /><span>WX-01</span></Button>

            {placed.map(p => (
              <button key={p.uid} type="button" className={`placed-node kind-${p.kind} ${selected.id === p.uid ? 'selected' : ''}`} style={{ left: `${p.x}%`, top: `${p.y}%` }} onPointerDown={e => beginDrag(e, p)} aria-label={p.name}>
                <Icon name={p.icon} size={p.kind === 'field' || p.kind === 'crop' ? 18 : 15} /><span>{p.name}</span>
              </button>
            ))}
            <div className="environment-float">
              <span><Icon name="sun" size={14} /> {temperature}°C</span>
              <span><Icon name="wind" size={14} /> {(2.4 + progress % 9 / 10).toFixed(1)} m/s</span>
              <span><Icon name="droplet" size={14} /> {humidity}% RH</span>
            </div>
            <div className="map-scale"><span />100 m</div>
          </div>

          <div className="instrument-deck">
            <div className="deck-tabs"><Button className="active">Live measurements</Button><Button>Charts</Button><Button>Event log</Button><span>{farm.fields.length * 2 + 2 + kindCount('sensor')} channels active</span></div>
            <div className="deck-content">
              <div className="measurement-strip">
                <div><span>SOIL MOISTURE</span><strong>{moisture}<small>%</small></strong><em>Target {targetMoisture}%</em></div>
                <div><span>FLOW RATE</span><strong>{status === 'running' ? flowRate : 0}<small>L/min</small></strong><em>Pump P-01</em></div>
                <div><span>CROP GROWTH</span><strong>{growth}<small>%</small></strong><em>{growthStage}</em></div>
                <div><span>WATER USED</span><strong>{waterUsed}<small>m³</small></strong><em>Efficiency {Math.min(99, 91 + pumpBonus)}%</em></div>
              </div>
              <div className="live-chart">
                <div className="chart-heading"><div><strong>{firstField.name} telemetry</strong><span><i className="green" />Moisture <i className="blue" />Flow rate</span></div><span>LAST 30 SIM. DAYS</span></div>
                <MiniChart values={chartValues} label="Soil moisture over simulation time" />
              </div>
              <div className="event-log">
                <div className="event-title"><strong>EVENT LOG</strong><Button>Clear</Button></div>
                {events.slice(0, 7).map((event, index) => <div className="event-row" key={`${event.time}-${index}`}><span>{event.time}</span><i className={event.tone} /><p>{event.text}</p></div>)}
              </div>
            </div>
          </div>
        </main>

        <aside className="properties-panel">
          <div className="panel-heading"><div><span>INSPECTOR</span><strong>Properties</strong></div><Button aria-label="More options">•••</Button></div>
          <div className="selection-summary">
            <div className={`selection-icon ${selected.kind}`}><Icon name={selected.kind === 'irrigation' ? 'irrigation' : selected.kind === 'water' ? 'water' : selected.kind === 'sensor' ? 'sensor' : selected.kind === 'crop' ? 'leaf' : 'field'} size={22} /></div>
            <div><span>{selected.kind.toUpperCase()}</span><strong>{selected.name}</strong><small>ID · {selected.id.toUpperCase()}</small></div>
            <i className="online-dot" />
          </div>
          <div className="property-section">
            <div className="property-section-title"><strong>GENERAL</strong><Icon name="chevron" size={14} /></div>
            <div className="text-property"><span>Name</span><strong>{selected.name}</strong></div>
            <div className="text-property"><span>Model state</span><strong className="state-value"><i /> Operational</strong></div>
          </div>
          <div className="property-section">
            <div className="property-section-title"><strong>AGRONOMIC PARAMETERS</strong><Icon name="chevron" size={14} /></div>
            <ValueStepper label="Target moisture" value={targetMoisture} unit="%" onChange={setTargetMoisture} />
            <ValueStepper label="Flow rate" value={flowRate} unit=" L/min" onChange={setFlowRate} />
            <ValueStepper label="Nitrogen" value={nitrogen} unit=" kg/ha" onChange={setNitrogen} step={5} />
            <div className="select-property"><span>Crop variety</span><Select defaultValue="maize"><option value="maize">Pioneer P1197</option><option value="maize2">Dekalb DKC64</option></Select></div>
          </div>
          <div className="property-section">
            <div className="property-section-title"><strong>LIVE OUTPUT</strong><Icon name="chevron" size={14} /></div>
            <div className="output-grid">
              <div><span>Moisture</span><strong>{moisture}%</strong></div><div><span>Temperature</span><strong>{temperature}°C</strong></div>
              <div><span>Growth</span><strong>{growth}%</strong></div><div><span>Stress index</span><strong>{Math.max(2, 18 - Math.round(progress / 8))}%</strong></div>
            </div>
            <MiniChart values={chartValues.slice(-16)} color="green" label="Selected component output" />
          </div>
          <div className="property-actions"><Button onClick={() => selectedPlaced && duplicatePlaced(selectedPlaced.uid)} disabled={!selectedPlaced} title={selectedPlaced ? 'Duplicate' : 'Only placed components can be duplicated'}><Icon name="copy" size={15} /> Duplicate</Button><Button className="danger" onClick={() => selectedPlaced && removePlaced(selectedPlaced.uid)} disabled={!selectedPlaced} title={selectedPlaced ? 'Remove' : 'Farm layout items are removed in the Farm Builder'}><Icon name="trash" size={15} /> Remove</Button></div>
        </aside>
      </div>

      <footer className="simulation-controls">
        <div className="transport">
          <Button className="start" onClick={start} disabled={status === 'running'}><Icon name="play" size={15} /> Start</Button>
          <Button onClick={() => setStatus('paused')} disabled={status !== 'running'}><Icon name="pause" size={15} /> Pause</Button>
          <Button onClick={() => setStatus('running')} disabled={status !== 'paused' && status !== 'stopped'}><Icon name="play" size={15} /> Resume</Button>
          <Button onClick={() => setStatus('stopped')} disabled={status === 'idle' || status === 'stopped'}><Icon name="stop" size={15} /> Stop</Button>
          <Button onClick={reset}><Icon name="reset" size={15} /> Reset</Button>
          <Button className="finish" onClick={finish} disabled={progress <= 0}><Icon name="chart" size={15} /> {status === 'complete' ? 'View results' : 'Finish & view results'}</Button>
        </div>
        <div className="timeline">
          <div className="timeline-meta"><span>DAY 1</span><strong>Day {Math.max(1, Math.ceil(progress * ((scenario?.duration ?? 120) / 100)))} · {growthStage}</strong><span>DAY {scenario?.duration ?? 120}</span></div>
          <div className="timeline-track"><span className="timeline-fill" style={{ width: `${progress}%` }} /><i className="timeline-knob" style={{ left: `${progress}%` }} /><b className="event-mark m1" /><b className="event-mark m2" /><b className="event-mark m3" /></div>
        </div>
        <div className="speed-control"><span>SPEED</span>{[1, 2, 5, 10].map(item => <Button key={item} className={speed === item ? 'active' : ''} onClick={() => setSpeed(item)}>{item}×</Button>)}</div>
      </footer>
      {engineOpen && <EngineModal farmName={farm.name} scenarioName={scenario?.name} onClose={() => setEngineOpen(false)} />}
    </div>
  )
}

function simulationTime(progress: number) {
  const totalMinutes = Math.floor(progress * 17.2)
  const hours = Math.floor(totalMinutes / 60) % 24
  const minutes = totalMinutes % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:00`
}

