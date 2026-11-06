import { useMemo } from 'react'
import { Button, Icon, Input, Select, Textarea } from '../../components/ui'
import { scenarioCategories, variableOptions, type AdjustableVariable, type PersistedFarm, type ScenarioCategory, type ScenarioRecord } from '../../scenarioTypes'

export function blankScenario(farm?: PersistedFarm, category: ScenarioCategory = scenarioCategories[0]): ScenarioRecord {
  return {
    id: '',
    name: '',
    description: '',
    category,
    farmId: farm?.id ?? '',
    participatingFields: farm?.fields.map(field => field.id) ?? [],
    crops: farm?.fields.map(field => ({ fieldId: field.id, crop: field.crop })) ?? [],
    startingConditions: { soilMoisture: 55, temperature: farm?.temperature ?? 24, rainfall: 8, budget: 25000, pestPressure: 5 },
    adjustableVariables: ['Irrigation volume', 'Fertiliser rate'],
    duration: 120,
    difficulty: 'Intermediate',
    learningObjectives: ['Evaluate how management decisions affect crop health and resource efficiency.'],
    source: 'mine',
  }
}

function NumberControl({ label, value, unit, min, max, step = 1, onChange }: { label: string; value: number; unit: string; min: number; max: number; step?: number; onChange: (value: number) => void }) {
  return (
    <div className="scenario-number">
      <span>{label}</span>
      <div><Button onClick={() => onChange(Math.max(min, value - step))}>−</Button><strong>{value}{unit}</strong><Button onClick={() => onChange(Math.min(max, value + step))}>+</Button></div>
    </div>
  )
}


export default function ScenarioEditor({ mode, draft, setDraft, farms, message, onSave, onRun, onCancel }: {
  mode: 'create' | 'edit'
  draft: ScenarioRecord
  setDraft: (update: (current: ScenarioRecord) => ScenarioRecord) => void
  farms: PersistedFarm[]
  message: string
  onSave: (draft: ScenarioRecord) => void
  onRun: (farm: PersistedFarm, scenario: ScenarioRecord) => void
  onCancel: () => void
}) {
  const selectedFarm = farms.find(farm => farm.id === draft.farmId) ?? farms[0]
  const configuredFields = selectedFarm?.fields.filter(field => draft.participatingFields.includes(field.id)) ?? []
  const completion = useMemo(() => {
    const checks = [draft.name.trim(), draft.description.trim(), draft.farmId, draft.participatingFields.length, draft.adjustableVariables.length, draft.learningObjectives.filter(Boolean).length]
    return Math.round(checks.filter(Boolean).length / checks.length * 100)
  }, [draft])

  const chooseFarm = (farmId: string) => {
    const farm = farms.find(item => item.id === farmId)
    setDraft(current => ({
      ...current,
      farmId,
      participatingFields: farm?.fields.map(field => field.id) ?? [],
      crops: farm?.fields.map(field => ({ fieldId: field.id, crop: field.crop })) ?? [],
      startingConditions: { ...current.startingConditions, temperature: farm?.temperature ?? current.startingConditions.temperature },
    }))
  }
  const toggleField = (fieldId: string) => {
    setDraft(current => ({ ...current, participatingFields: current.participatingFields.includes(fieldId) ? current.participatingFields.filter(id => id !== fieldId) : [...current.participatingFields, fieldId] }))
  }
  const toggleVariable = (variable: AdjustableVariable) => {
    setDraft(current => ({ ...current, adjustableVariables: current.adjustableVariables.includes(variable) ? current.adjustableVariables.filter(item => item !== variable) : [...current.adjustableVariables, variable] }))
  }

  return (
    <div className="scenario-builder-page">
      <div className="scenario-builder-bar">
        <div><Button onClick={onCancel}>‹</Button><span><small>{mode === 'edit' ? 'EDITING SCENARIO' : 'NEW SCENARIO'}</small><strong>{draft.name || 'Untitled scenario'}</strong></span></div>
        <div className="scenario-completion"><span>CONFIGURATION {completion}%</span><i><b style={{ width: `${completion}%` }} /></i></div>
        <div>{message && <span className="scenario-message">{message}</span>}<Button onClick={() => onSave(draft)} disabled={!draft.name.trim() || !draft.description.trim() || !draft.participatingFields.length || !draft.adjustableVariables.length}><Icon name="save" size={15} /> {mode === 'edit' ? 'Save changes' : 'Save scenario'}</Button><Button className="launch" onClick={() => selectedFarm && onRun(selectedFarm, draft)} disabled={completion < 100}><Icon name="play" size={15} /> Run scenario</Button></div>
      </div>

      <div className="scenario-builder-workspace">
        <aside className="scenario-settings">
          <div className="scenario-panel-title"><span>SCENARIO SETTINGS</span><strong>Identity & context</strong></div>
          <div className="scenario-panel-scroll">
            <label><span>Scenario name</span><Input value={draft.name} placeholder="Name your learning scenario" onChange={event => setDraft(current => ({ ...current, name: event.target.value }))} /></label>
            <label><span>Description</span><Textarea value={draft.description} placeholder="Describe the challenge, context and expected decisions…" onChange={event => setDraft(current => ({ ...current, description: event.target.value }))} /></label>
            <label><span>Category</span><Select value={draft.category} onChange={event => setDraft(current => ({ ...current, category: event.target.value as ScenarioCategory }))}>{scenarioCategories.map(category => <option key={category}>{category}</option>)}</Select></label>
            <label><span>Farm environment</span><Select value={draft.farmId} onChange={event => chooseFarm(event.target.value)}>{farms.map(farm => <option key={farm.id} value={farm.id}>{farm.name} · {farm.totalSize} ha</option>)}</Select></label>
            {selectedFarm && <div className="selected-farm-summary"><div><Icon name="field" size={18} /></div><span><strong>{selectedFarm.name}</strong><small>{selectedFarm.location} · {selectedFarm.fields.length} fields</small></span><i>{selectedFarm.waterSources.map(w => w.type).join(' + ') || 'no water'}</i></div>}
            <div className="setting-divider"><span>DIFFICULTY & DURATION</span></div>
            <div className="difficulty-options">{(['Beginner', 'Intermediate', 'Advanced'] as const).map(item => <Button key={item} className={draft.difficulty === item ? 'active' : ''} onClick={() => setDraft(current => ({ ...current, difficulty: item }))}>{item}</Button>)}</div>
            <NumberControl label="Simulation duration" value={draft.duration} unit=" days" min={1} max={730} step={10} onChange={duration => setDraft(current => ({ ...current, duration }))} />
          </div>
        </aside>

        <main className="scenario-configuration">
          <div className="scenario-panel-title"><span>CONFIGURATION</span><strong>Fields, crops & starting conditions</strong></div>
          <div className="scenario-config-scroll">
            <section className="scenario-config-section">
              <div className="config-section-head"><div><span>01</span><strong>Participating fields</strong><small>Select zones from the actual farm layout</small></div><b>{draft.participatingFields.length} selected</b></div>
              <div className="scenario-farm-preview">
                <div className="scenario-preview-grid" />
                {selectedFarm?.fields.map(field => <Button key={field.id} className={`scenario-preview-field soil-${field.soil} ${draft.participatingFields.includes(field.id) ? 'active' : ''}`} style={{ left: `${field.x}%`, top: `${field.y}%`, width: `${field.width}%`, height: `${field.height}%` }} onClick={() => toggleField(field.id)}><span /><strong>{field.name}</strong><small>{field.crop} · {field.size} ha</small><i>{draft.participatingFields.includes(field.id) ? '✓' : '+'}</i></Button>)}
                {selectedFarm?.waterSources.map(w => <div key={w.id} className={`scenario-preview-water water-${w.type}`} style={{ left: `${w.x}%`, top: `${w.y}%` }}><Icon name="water" size={17} /><span>{w.type}</span></div>)}
              </div>
            </section>

            <section className="scenario-config-section">
              <div className="config-section-head"><div><span>02</span><strong>Crop configuration</strong><small>Use farm crops or override them for this experiment</small></div></div>
              <div className="scenario-crop-table">
                {configuredFields.map(field => <div key={field.id}><span><i className={`soil-dot ${field.soil}`} /><strong>{field.name}</strong><small>{field.soil} soil</small></span><Select value={draft.crops.find(item => item.fieldId === field.id)?.crop ?? field.crop} onChange={event => setDraft(current => ({ ...current, crops: [...current.crops.filter(item => item.fieldId !== field.id), { fieldId: field.id, crop: event.target.value }] }))}><option>maize</option><option>wheat</option><option>soybean</option><option>sunflower</option><option>vegetables</option><option>none</option></Select><em>{field.irrigation}</em></div>)}
              </div>
            </section>

            <section className="scenario-config-section">
              <div className="config-section-head"><div><span>03</span><strong>Starting conditions</strong><small>Set the state of the farm at simulation start</small></div></div>
              <div className="starting-condition-grid">
                <NumberControl label="Soil moisture" value={draft.startingConditions.soilMoisture} unit="%" min={0} max={100} step={5} onChange={value => setDraft(current => ({ ...current, startingConditions: { ...current.startingConditions, soilMoisture: value } }))} />
                <NumberControl label="Temperature" value={draft.startingConditions.temperature} unit="°C" min={-270} max={Infinity} onChange={value => setDraft(current => ({ ...current, startingConditions: { ...current.startingConditions, temperature: value } }))} />
                <NumberControl label="Rainfall" value={draft.startingConditions.rainfall} unit=" mm" min={0} max={1000} step={5} onChange={value => setDraft(current => ({ ...current, startingConditions: { ...current.startingConditions, rainfall: value } }))} />
                <NumberControl label="Starting budget" value={draft.startingConditions.budget} unit="" min={0} max={1000000} step={1000} onChange={value => setDraft(current => ({ ...current, startingConditions: { ...current.startingConditions, budget: value } }))} />
                <NumberControl label="Pest pressure" value={draft.startingConditions.pestPressure} unit="%" min={0} max={100} step={5} onChange={value => setDraft(current => ({ ...current, startingConditions: { ...current.startingConditions, pestPressure: value } }))} />
              </div>
            </section>
          </div>
        </main>

        <aside className="scenario-objectives">
          <div className="scenario-panel-title"><span>LEARNING DESIGN</span><strong>Variables & objectives</strong></div>
          <div className="scenario-panel-scroll">
            <div className="objectives-intro"><div><Icon name="book" size={19} /></div><span><strong>Define the learning experience</strong><p>Choose what learners can control and what successful completion should teach.</p></span></div>
            <div className="setting-divider"><span>ADJUSTABLE VARIABLES</span><b>{draft.adjustableVariables.length}</b></div>
            <div className="variable-list">{variableOptions.map(variable => <Button key={variable} className={draft.adjustableVariables.includes(variable) ? 'active' : ''} onClick={() => toggleVariable(variable)}><i>{draft.adjustableVariables.includes(variable) ? '✓' : '+'}</i><span>{variable}</span></Button>)}</div>
            <div className="setting-divider"><span>LEARNING OBJECTIVES</span><b>{draft.learningObjectives.length}</b></div>
            <div className="objective-list">
              {draft.learningObjectives.map((objective, index) => <div key={index}><span>{String(index + 1).padStart(2, '0')}</span><Input value={objective} onChange={event => setDraft(current => ({ ...current, learningObjectives: current.learningObjectives.map((item, itemIndex) => itemIndex === index ? event.target.value : item) }))} /><Button aria-label="Remove objective" onClick={() => setDraft(current => ({ ...current, learningObjectives: current.learningObjectives.filter((_, itemIndex) => itemIndex !== index) }))}>×</Button></div>)}
              {draft.learningObjectives.length < 8 && <Button className="add-objective" onClick={() => setDraft(current => ({ ...current, learningObjectives: [...current.learningObjectives, ''] }))}>+ Add learning objective</Button>}
            </div>
            <div className="scenario-readiness"><div><span>SCENARIO READINESS</span><strong>{completion}%</strong></div><i><b style={{ width: `${completion}%` }} /></i><p>{completion === 100 ? 'Ready to save and launch in the simulator.' : 'Complete the required settings to enable launch.'}</p></div>
          </div>
        </aside>
      </div>
    </div>
  )
}
