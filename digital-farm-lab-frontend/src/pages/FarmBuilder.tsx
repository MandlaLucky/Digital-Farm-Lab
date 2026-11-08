import { useRef, useState } from 'react'
import { Button, Icon, Input, Select, Stepper } from '../components/ui'
import { waterLayoutSlots, type FarmConfig, type FarmField, type IrrigationType, type SoilType, type WaterSource, type WaterSourceType } from '../farmTypes'
import type { PersistedFarm } from '../scenarioTypes'

type Selection = { type: 'field'; id: string } | { type: 'water'; id: string } | null

const soils: SoilType[] = ['sandy', 'clay', 'loamy', 'silt']
const irrigation: IrrigationType[] = ['drip', 'sprinkler', 'flood', 'none']
const crops = ['maize', 'wheat', 'soybean', 'sunflower', 'vegetables', 'none'] as const
const waterSources = ['dam', 'borehole', 'river', 'reservoir'] as const

export default function FarmBuilder({ farm, setFarm, onSave, onBack, onSimulate, farms, onLoad }: {
  farm: FarmConfig
  setFarm: (update: (current: FarmConfig) => FarmConfig) => void
  onSave: () => void
  onBack: () => void
  onSimulate: () => void
  farms: PersistedFarm[]
  onLoad: (farm: PersistedFarm) => void
}) {
  const [selection, setSelection] = useState<Selection>(null)
  const [saved, setSaved] = useState(false)
  const canvasRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<{ type: 'field' | 'water'; id?: string; offsetX: number; offsetY: number } | null>(null)
  const selectedField = selection?.type === 'field' ? farm.fields.find(field => field.id === selection.id) : undefined
  const selectedWater = selection?.type === 'water' ? farm.waterSources.find(w => w.id === selection.id) : undefined
  const usedSize = farm.fields.reduce((sum, field) => sum + field.size, 0)
  const canEnter = farm.fields.length > 0 && farm.waterSources.length > 0

  const updateFarm = (patch: Partial<FarmConfig>) => {
    setFarm(current => ({ ...current, ...patch }))
    setSaved(false)
  }
  const updateField = (id: string, patch: Partial<FarmField>) => {
    setFarm(current => ({ ...current, fields: current.fields.map(field => field.id === id ? { ...field, ...patch } : field) }))
    setSaved(false)
  }
  const updateWater = (id: string, patch: Partial<WaterSource>) => {
    setFarm(current => ({ ...current, waterSources: current.waterSources.map(w => w.id === id ? { ...w, ...patch } : w) }))
    setSaved(false)
  }
  const addWater = (type: WaterSourceType) => {
    const id = `water-${Date.now()}`
    const [x, y] = waterLayoutSlots[farm.waterSources.length % waterLayoutSlots.length]
    setFarm(current => ({ ...current, waterSources: [...current.waterSources, { id, type, x, y }] }))
    setSelection({ type: 'water', id })
    setSaved(false)
  }
  const removeWater = (id: string) => {
    setFarm(current => ({ ...current, waterSources: current.waterSources.filter(w => w.id !== id) }))
    setSelection(null)
    setSaved(false)
  }
  const addField = () => {
    if (farm.fields.length >= 6) return
    const index = farm.fields.length
    const id = `field-${Date.now()}`
    const field: FarmField = {
      id,
      name: `Field ${String.fromCharCode(65 + index)}`,
      size: 5,
      soil: 'loamy',
      crop: 'none',
      irrigation: 'none',
      x: 8 + (index % 3) * 28,
      y: 10 + Math.floor(index / 3) * 39,
      width: 24,
      height: 30,
    }
    setFarm(current => ({ ...current, fields: [...current.fields, field] }))
    setSelection({ type: 'field', id })
    setSaved(false)
  }
  const removeField = (id: string) => {
    setFarm(current => ({ ...current, fields: current.fields.filter(field => field.id !== id) }))
    setSelection(null)
    setSaved(false)
  }
  const save = () => {
    onSave()
    setSaved(true)
  }
  const reset = () => {
    setFarm(current => ({ ...current, fields: [], waterSources: [] }))
    setSelection(null)
    setSaved(false)
  }
  const beginDrag = (event: React.PointerEvent, type: 'field' | 'water', id?: string) => {
    const bounds = canvasRef.current?.getBoundingClientRect()
    if (!bounds) return
    const item = type === 'field' ? farm.fields.find(field => field.id === id) : null
    const water = type === 'water' ? farm.waterSources.find(w => w.id === id) : null
    const x = type === 'field' ? item?.x ?? 0 : water?.x ?? 0
    const y = type === 'field' ? item?.y ?? 0 : water?.y ?? 0
    dragRef.current = {
      type,
      id,
      offsetX: ((event.clientX - bounds.left) / bounds.width) * 100 - x,
      offsetY: ((event.clientY - bounds.top) / bounds.height) * 100 - y,
    }
    event.currentTarget.setPointerCapture(event.pointerId)
  }
  const moveDrag = (event: React.PointerEvent) => {
    const drag = dragRef.current
    const bounds = canvasRef.current?.getBoundingClientRect()
    if (!drag || !bounds) return
    const x = Math.max(1, Math.min(88, ((event.clientX - bounds.left) / bounds.width) * 100 - drag.offsetX))
    const y = Math.max(2, Math.min(84, ((event.clientY - bounds.top) / bounds.height) * 100 - drag.offsetY))
    if (drag.type === 'field' && drag.id) updateField(drag.id, { x, y })
    else if (drag.id) updateWater(drag.id, { x, y })
  }

  return (
    <div className="builder-shell">
      <div className="builder-titlebar">
        <div><span>2 · FARM BUILDER</span><strong>Lay out {farm.name || 'your farm'}</strong><p>Add fields, assign soil, crops and irrigation, and place the water source.</p></div>
        <div className="builder-actions">
          <Button onClick={onBack}><Icon name="settings" size={15} /> Farm setup</Button>
          <Button onClick={reset}><Icon name="reset" size={15} /> Clear layout</Button>
          <Button onClick={save} disabled={!canEnter}><Icon name="save" size={15} /> {saved ? 'Farm saved' : 'Save farm'}</Button>
          <Button className="enter-button" onClick={onSimulate} disabled={!canEnter}><Icon name="play" size={15} /> Open simulation</Button>
        </div>
        {saved && <span className="farm-save-message success">Saved to farm library</span>}
      </div>

      <div className="builder-workspace">
        <aside className="builder-config">
          <div className="builder-panel-title"><span>COMPONENTS</span><strong>Build your farm</strong></div>
          <div className="builder-scroll">
            <div className="builder-summary"><strong>{farm.name || 'Unnamed farm'}</strong><small>{farm.location || 'Location not set'} · {farm.totalSize} ha</small></div>
            <div className="capacity-meter">
              <div><span>FIELD ALLOCATION</span><strong>{usedSize} / {farm.totalSize} ha</strong></div>
              <i><b style={{ width: `${Math.min(100, usedSize / farm.totalSize * 100)}%` }} /></i>
            </div>

            <div className="builder-section-heading"><span>BUILD COMPONENTS</span><strong>{farm.fields.length} fields · {farm.waterSources.length} water sources</strong></div>
            <div className="builder-component-actions">
              <Button onClick={addField} disabled={farm.fields.length >= 6 || usedSize >= farm.totalSize}><Icon name="field" size={19} /><span><strong>Add field</strong><small>Draw a crop zone</small></span><b>+</b></Button>
              {waterSources.map(source => <Button key={source} onClick={() => addWater(source)}><Icon name="water" size={18} /><span><strong>{source}</strong><small>{farm.waterSources.filter(w => w.type === source).length} placed · add another</small></span><b>+</b></Button>)}
            </div>

            {farms.length > 0 && (
              <>
                <div className="builder-section-heading"><span>FARM LIBRARY</span><strong>{farms.length} saved</strong></div>
                <div className="saved-farms" style={{ marginBottom: 13 }}>
                  {farms.map(f => (
                    <Button key={f.id} className="saved-farm" onClick={() => onLoad(f)} style={{ minHeight: 44 }}>
                      <div><Icon name="field" size={15} /></div>
                      <span><strong>{f.name || 'Unnamed'}</strong><small>{f.fields.length} fields · {f.totalSize} ha</small></span>
                      <em style={{ fontSize: 7 }}>Load</em>
                    </Button>
                  ))}
                </div>
              </>
            )}

          </div>
        </aside>

        <main className="builder-canvas-wrap">
          <div className="builder-canvas-toolbar">
            <div><Button className="active"><Icon name="cursor" size={15} /> Select</Button><Button><Icon name="hand" size={15} /> Pan</Button></div>
            <span><i /> Drag fields to arrange · configure selection in the inspector</span>
            <div><Button>−</Button><strong>100%</strong><Button>+</Button></div>
          </div>
          <div className="builder-canvas" ref={canvasRef} onPointerMove={moveDrag} onPointerUp={() => { dragRef.current = null }} onPointerLeave={() => { dragRef.current = null }}>
            <div className="builder-grid" />
            {farm.fields.length === 0 && (
              <div className="empty-farm">
                <div><Icon name="field" size={31} /></div><strong>Your farm is empty</strong><p>Add your first field from the setup panel, then drag it into position.</p><Button onClick={addField}><Icon name="field" size={15} /> Add first field</Button>
              </div>
            )}
            {farm.fields.map(field => (
              <Button
                key={field.id}
                className={`builder-field soil-${field.soil} crop-${field.crop} ${selection?.type === 'field' && selection.id === field.id ? 'selected' : ''}`}
                style={{ left: `${field.x}%`, top: `${field.y}%`, width: `${field.width}%`, height: `${field.height}%` }}
                onClick={() => setSelection({ type: 'field', id: field.id })}
                onPointerDown={event => beginDrag(event, 'field', field.id)}
              >
                <CropPlants crop={field.crop} /><span className="builder-field-label"><strong>{field.name}</strong><small>{field.crop === 'none' ? 'UNPLANTED' : field.crop.toUpperCase()} · {field.size} ha</small></span>
                <span className="builder-field-meta">{field.soil}<i>{field.irrigation}</i></span>
                <span className="resize-corner" />
              </Button>
            ))}
            {farm.waterSources.map(w => (
              <Button
                key={w.id}
                className={`builder-water water-${w.type} ${selection?.type === 'water' && selection.id === w.id ? 'selected' : ''}`}
                style={{ left: `${w.x}%`, top: `${w.y}%` }}
                onClick={() => setSelection({ type: 'water', id: w.id })}
                onPointerDown={event => beginDrag(event, 'water', w.id)}
              >
                <span className="builder-water-ripple" /><Icon name="water" size={23} /><strong>{w.type.toUpperCase()}</strong><small>Water source</small>
              </Button>
            ))}
            {farm.waterSources.flatMap(w => farm.fields.filter(field => field.irrigation !== 'none').map(field => <svg className="builder-connection" key={`${w.id}-${field.id}`} viewBox="0 0 100 100" preserveAspectRatio="none"><line x1={w.x + 5} y1={w.y + 5} x2={field.x + field.width / 2} y2={field.y + field.height / 2} /></svg>))}
            <div className="builder-compass">N<span>↑</span></div>
            <div className="builder-weather"><Icon name={farm.weather === 'rain' ? 'rain' : 'sun'} size={15} /><span>{farm.temperature}°C</span><span>{farm.humidity}% RH</span></div>
            <div className="builder-scale"><span />100 m</div>
          </div>
          <div className="builder-statusbar"><span><i /> FARM BOUNDS · {farm.totalSize} ha</span><span>{farm.fields.length} fields · {farm.waterSources.length ? `${farm.waterSources.length} water source${farm.waterSources.length > 1 ? 's' : ''}` : 'No water source'} · {farm.location || 'Location not set'}</span></div>
        </main>

        <aside className="builder-inspector">
          <div className="builder-panel-title"><span>INSPECTOR</span><strong>{selectedField ? 'Field properties' : selection?.type === 'water' ? 'Water source' : 'Nothing selected'}</strong></div>
          {!selection && <div className="inspector-empty"><Icon name="cursor" size={27} /><strong>Select a component</strong><p>Choose a field or water source on the canvas to configure it.</p></div>}
          {selectedField && (
            <div className="inspector-content">
              <div className="selected-component"><div><Icon name="field" size={21} /></div><span><small>FIELD COMPONENT</small><strong>{selectedField.name}</strong></span><i /></div>
              <label className="builder-control"><span>Field name</span><Input value={selectedField.name} onChange={event => updateField(selectedField.id, { name: event.target.value })} /></label>
              <div className="builder-control inline"><span>Field size</span><Stepper value={selectedField.size} unit=" ha" min={1} max={Math.max(1, farm.totalSize - usedSize + selectedField.size)} onChange={size => updateField(selectedField.id, { size, width: Math.min(40, 18 + size * .7), height: Math.min(42, 22 + size * .65) })} /></div>
              <label className="builder-control"><span>Soil type</span><Select value={selectedField.soil} onChange={event => updateField(selectedField.id, { soil: event.target.value as SoilType })}>{soils.map(soil => <option key={soil}>{soil}</option>)}</Select></label>
              <label className="builder-control"><span>Crop</span><Select value={selectedField.crop} onChange={event => updateField(selectedField.id, { crop: event.target.value as FarmField['crop'] })}>{crops.map(crop => <option key={crop}>{crop}</option>)}</Select></label>
              <label className="builder-control"><span>Irrigation</span><Select value={selectedField.irrigation} onChange={event => updateField(selectedField.id, { irrigation: event.target.value as IrrigationType })}>{irrigation.map(item => <option key={item}>{item}</option>)}</Select></label>
              <div className="resize-controls"><span>VISUAL SIZE</span><div><Button onClick={() => updateField(selectedField.id, { width: Math.max(14, selectedField.width - 3) })}>Narrower</Button><Button onClick={() => updateField(selectedField.id, { width: Math.min(45, selectedField.width + 3) })}>Wider</Button></div><div><Button onClick={() => updateField(selectedField.id, { height: Math.max(17, selectedField.height - 3) })}>Shorter</Button><Button onClick={() => updateField(selectedField.id, { height: Math.min(48, selectedField.height + 3) })}>Taller</Button></div></div>
              <div className="field-summary"><span><Icon name="leaf" size={15} />{selectedField.crop === 'none' ? 'No crop assigned' : `${selectedField.crop} crop assigned`}</span><span><Icon name="droplet" size={15} />{selectedField.irrigation === 'none' ? 'No irrigation' : `${selectedField.irrigation} irrigation`}</span></div>
              <Button className="remove-component" onClick={() => removeField(selectedField.id)}><Icon name="trash" size={15} /> Remove field</Button>
            </div>
          )}
          {selectedWater && (
            <div className="inspector-content">
              <div className="selected-component water"><div><Icon name="water" size={21} /></div><span><small>WATER COMPONENT</small><strong>{selectedWater.type}</strong></span><i /></div>
              <label className="builder-control"><span>Source type</span><Select value={selectedWater.type} onChange={event => updateWater(selectedWater.id, { type: event.target.value as WaterSourceType })}>{waterSources.map(source => <option key={source}>{source}</option>)}</Select></label>
              <div className="water-capacity"><span>AVAILABLE CAPACITY</span><strong>2,480 m³</strong><i><b /></i><small>84% available · {farm.waterSources.length} source{farm.waterSources.length > 1 ? 's' : ''} supplying {farm.fields.length} fields</small></div>
              <Button className="remove-component" onClick={() => removeWater(selectedWater.id)}><Icon name="trash" size={15} /> Remove source</Button>
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}

function CropPlants({ crop }: { crop: string }) {
  if (crop === 'none') return null
  return (
    <div className={`crop-plants crop-${crop}`} aria-hidden="true">
      {Array.from({ length: 20 }).map((_, i) => (
        <span key={i} className="plant" />
      ))}
    </div>
  )
}
