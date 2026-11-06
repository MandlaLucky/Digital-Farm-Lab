import { useState } from 'react'
import { Button, Icon } from '../../components/ui'
import { scenarioCategories, type PersistedFarm, type ScenarioCategory, type ScenarioRecord } from '../../scenarioTypes'
import { categoryBlurbs, categoryIcons } from './categories'
import ScenarioEditor, { blankScenario } from './ScenarioEditor'

export type ScenarioTab = 'browse' | 'create' | 'edit' | 'mine' | 'categories'

const tabs: { id: ScenarioTab; label: string }[] = [
  { id: 'browse', label: 'Browse Scenarios' },
  { id: 'create', label: 'Create Scenario' },
  { id: 'edit', label: 'Edit Scenario' },
  { id: 'mine', label: 'My Scenarios' },
  { id: 'categories', label: 'Scenario Categories' },
]

function ScenarioCard({ scenario, farm, actions }: { scenario: ScenarioRecord; farm?: PersistedFarm; actions: React.ReactNode }) {
  return (
    <article className="scenario-card">
      <div className="scenario-card-top"><div><Icon name={categoryIcons[scenario.category]} size={20} /></div><span className={`difficulty-tag ${scenario.difficulty.toLowerCase()}`}>{scenario.difficulty}</span></div>
      <span className="scenario-category">{scenario.category}</span><strong>{scenario.name}</strong><p>{scenario.description}</p>
      <div className="scenario-card-meta"><span><Icon name="field" size={13} /> {farm?.name ?? 'Farm unavailable'}</span><span><Icon name="history" size={13} /> {scenario.duration} days</span><span><Icon name="grid" size={13} /> {scenario.participatingFields.length} fields</span></div>
      <div className="scenario-card-actions">{actions}</div>
    </article>
  )
}

export default function ScenarioManagement({ tab, onTab, farms, presets, mine, onSave, onDelete, onRun }: {
  tab: ScenarioTab
  onTab: (tab: ScenarioTab) => void
  farms: PersistedFarm[]
  presets: ScenarioRecord[]
  mine: ScenarioRecord[]
  onSave: (scenario: ScenarioRecord) => ScenarioRecord
  onDelete: (id: string) => void
  onRun: (farm: PersistedFarm, scenario: ScenarioRecord) => void
}) {
  const [category, setCategory] = useState<ScenarioCategory | 'All'>('All')
  const [query, setQuery] = useState('')
  const [difficulty, setDifficulty] = useState<'All' | ScenarioRecord['difficulty']>('All')
  const [draft, setDraft] = useState<ScenarioRecord>(() => blankScenario(farms[0]))
  const [editing, setEditing] = useState<ScenarioRecord | null>(null)
  const [message, setMessage] = useState('')

  const farmFor = (s: ScenarioRecord) => farms.find(f => f.id === s.farmId)
  const run = (s: ScenarioRecord) => { const f = farmFor(s); if (f) onRun(f, s) }

  const startCreate = (from?: Partial<ScenarioRecord>) => {
    setDraft({ ...blankScenario(farms[0], from?.category), ...from, id: '', source: 'mine' })
    setMessage('')
    onTab('create')
  }
  const startEdit = (s: ScenarioRecord) => { setEditing({ ...s }); setMessage(''); onTab('edit') }
  const duplicate = (s: ScenarioRecord) => onSave({ ...s, id: '', name: `${s.name} (copy)`, source: 'mine' })

  const matches = (s: ScenarioRecord) =>
    (category === 'All' || s.category === category) &&
    (difficulty === 'All' || s.difficulty === difficulty) &&
    (!query.trim() || `${s.name} ${s.description} ${s.category}`.toLowerCase().includes(query.trim().toLowerCase()))
  const browseList = presets.filter(matches)
  const countFor = (c: ScenarioCategory, list: ScenarioRecord[]) => list.filter(s => s.category === c).length

  return (
    <div className="scenario-shell">
      <div className="subnav">
        <span className="subnav-label">3 · SCENARIO MANAGEMENT</span>
        <nav>{tabs.map(t => <Button key={t.id} className={tab === t.id ? 'active' : ''} onClick={() => onTab(t.id)}>{t.label}{t.id === 'mine' && <b>{mine.length}</b>}</Button>)}</nav>
      </div>

      {tab === 'browse' && (
        <div className="scenario-library-page">
          <div className="scenario-library-hero">
            <div><span>BROWSE SCENARIOS</span><strong>Ready-made learning challenges</strong><p>Run a curated scenario as it is, or use it as a template for your own.</p></div>
            <div className="scenario-library-stats"><div><strong>{presets.length}</strong><span>AVAILABLE SCENARIOS</span></div><div><strong>{scenarioCategories.length}</strong><span>CATEGORIES</span></div><div><strong>{mine.length}</strong><span>YOUR SCENARIOS</span></div></div>
          </div>
          <div className="scenario-library-body">
            <aside className="category-browser">
              <div className="category-title"><span>FILTER</span><strong>Categories</strong></div>
              <Button className={category === 'All' ? 'active' : ''} onClick={() => setCategory('All')}><Icon name="grid" size={16} /><span>All scenarios</span><b>{presets.length}</b></Button>
              {scenarioCategories.map(c => <Button key={c} className={category === c ? 'active' : ''} onClick={() => setCategory(c)}><Icon name={categoryIcons[c]} size={16} /><span>{c}</span><b>{countFor(c, presets)}</b></Button>)}
            </aside>
            <main className="scenario-catalog">
              <div className="catalog-head">
                <div><span>{category === 'All' ? 'ALL SCENARIOS' : category.toUpperCase()}</span><strong>{browseList.length} scenario{browseList.length === 1 ? '' : 's'}</strong></div>
                <div className="catalog-filters">
                  <input className="input" placeholder="Search scenarios…" value={query} onChange={e => setQuery(e.target.value)} aria-label="Search scenarios" />
                  {(['All', 'Beginner', 'Intermediate', 'Advanced'] as const).map(d => <Button key={d} className={difficulty === d ? 'active' : ''} onClick={() => setDifficulty(d)}>{d}</Button>)}
                </div>
              </div>
              {browseList.length === 0 ? (
                <div className="catalog-empty"><Icon name="flask" size={29} /><strong>No scenarios match</strong><p>Try another category or filter, or create your own.</p><Button onClick={() => startCreate(category === 'All' ? undefined : { category })}>Create scenario</Button></div>
              ) : (
                <div className="scenario-card-grid">
                  {browseList.map(s => <ScenarioCard key={s.id} scenario={s} farm={farmFor(s)} actions={<>
                    <Button onClick={() => startCreate({ ...s, name: `${s.name} (my version)` })}><Icon name="copy" size={14} /> Use as template</Button>
                    <Button className="run" onClick={() => run(s)} disabled={!farmFor(s)}><Icon name="play" size={14} /> Run</Button>
                  </>} />)}
                </div>
              )}
            </main>
          </div>
        </div>
      )}

      {tab === 'create' && (
        <ScenarioEditor mode="create" draft={draft} setDraft={u => setDraft(u)} farms={farms} message={message}
          onCancel={() => onTab('browse')}
          onSave={d => { const saved = onSave({ ...d, id: '' }); setDraft(blankScenario(farms[0])); setMessage(''); setEditing(null); onTab('mine'); return saved }}
          onRun={onRun} />
      )}

      {tab === 'edit' && (editing ? (
        <ScenarioEditor mode="edit" draft={editing} setDraft={u => setEditing(c => c && u(c))} farms={farms} message={message}
          onCancel={() => { setEditing(null); onTab('mine') }}
          onSave={d => { onSave(d); setMessage('Changes saved.') }}
          onRun={onRun} />
      ) : (
        <div className="scenario-picker">
          <div className="picker-head"><span>EDIT SCENARIO</span><strong>Choose a scenario to edit</strong><p>Only scenarios you created can be edited. Use a browsed scenario as a template to make your own version.</p></div>
          {mine.length === 0 ? (
            <div className="catalog-empty"><Icon name="flask" size={29} /><strong>Nothing to edit yet</strong><p>Create a scenario first, then come back here to refine it.</p><Button onClick={() => startCreate()}>Create scenario</Button></div>
          ) : (
            <div className="picker-list">{mine.map(s => <Button key={s.id} className="picker-row" onClick={() => startEdit(s)}><div><Icon name={categoryIcons[s.category]} size={17} /></div><span><strong>{s.name}</strong><small>{s.category} · {s.duration} days · {s.difficulty}</small></span><em>Edit</em></Button>)}</div>
          )}
        </div>
      ))}

      {tab === 'mine' && (
        <div className="scenario-picker wide">
          <div className="picker-head row">
            <div><span>MY SCENARIOS</span><strong>{mine.length} saved scenario{mine.length === 1 ? '' : 's'}</strong><p>Scenarios you created or adapted. They are saved in this browser.</p></div>
            <Button className="primary-action" onClick={() => startCreate()}><span>+</span> New scenario</Button>
          </div>
          {mine.length === 0 ? (
            <div className="catalog-empty"><Icon name="flask" size={29} /><strong>You have no scenarios yet</strong><p>Browse the ready-made scenarios for inspiration, or create one from scratch.</p><div className="empty-actions"><Button onClick={() => onTab('browse')}>Browse scenarios</Button><Button onClick={() => startCreate()}>Create scenario</Button></div></div>
          ) : (
            <div className="scenario-card-grid">
              {mine.map(s => <ScenarioCard key={s.id} scenario={s} farm={farmFor(s)} actions={<>
                <Button onClick={() => startEdit(s)}><Icon name="settings" size={14} /> Edit</Button>
                <Button onClick={() => duplicate(s)} aria-label="Duplicate scenario"><Icon name="copy" size={14} /></Button>
                <Button onClick={() => onDelete(s.id)} aria-label="Delete scenario"><Icon name="trash" size={14} /></Button>
                <Button className="run" onClick={() => run(s)} disabled={!farmFor(s)}><Icon name="play" size={14} /> Run</Button>
              </>} />)}
            </div>
          )}
        </div>
      )}

      {tab === 'categories' && (
        <div className="scenario-picker wide">
          <div className="picker-head"><span>SCENARIO CATEGORIES</span><strong>{scenarioCategories.length} learning themes</strong><p>Each category focuses on one farm-management skill.</p></div>
          <div className="category-grid">
            {scenarioCategories.map(c => (
              <div className="category-card" key={c}>
                <div><Icon name={categoryIcons[c]} size={20} /></div>
                <strong>{c}</strong><p>{categoryBlurbs[c]}</p>
                <small>{countFor(c, presets)} ready-made · {countFor(c, mine)} yours</small>
                <div className="category-actions"><Button onClick={() => { setCategory(c); onTab('browse') }}>Browse</Button><Button onClick={() => startCreate({ category: c })}>Create</Button></div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
