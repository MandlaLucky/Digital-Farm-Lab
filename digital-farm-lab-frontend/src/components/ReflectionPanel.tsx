import { useEffect, useState } from 'react'
import { Button, Icon } from './ui'
import { api } from '../api'

type Exercise = { id: string; moduleCode: string; title: string; prompt: string; passScore: number }
type Criterion = { key: string; label: string; weight: number; earned: number; satisfied: boolean }
type Result = { id: string; runId: string; score: number; passed: boolean; passScore: number; breakdown: Criterion[]; createdAt: string }

export default function ReflectionPanel({ runServerId }: { runServerId?: string }) {
  const [exercise, setExercise] = useState<Exercise | null>(null)
  const [reflection, setReflection] = useState('')
  const [result, setResult] = useState<Result | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.listExercises()
      .then(list => { if (Array.isArray(list) && list.length > 0) setExercise(list[0] as Exercise) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const wordCount = reflection.trim().split(/\s+/).filter(Boolean).length
  const canSubmit = !!runServerId && wordCount >= 5 && !submitting

  const submit = async () => {
    if (!exercise || !runServerId) return
    setSubmitting(true); setError('')
    try {
      const res = await api.submitExercise(exercise.id, { runId: runServerId, reflection })
      setResult(res as Result)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e))
    } finally { setSubmitting(false) }
  }

  if (loading) return (
    <section className="panel-card wide">
      <div className="panel-card-title"><span>SCORED EXERCISE</span><strong>Loading...</strong></div>
    </section>
  )
  if (!exercise) return (
    <section className="panel-card wide">
      <div className="panel-card-title"><span>SCORED EXERCISE</span><strong>No exercise available</strong></div>
      <p className="muted-note">The backend has no exercises seeded yet.</p>
    </section>
  )

  return (
    <section className="panel-card wide">
      <div className="panel-card-title">
        <span>SCORED EXERCISE · {exercise.moduleCode}</span>
        <strong>{exercise.title}</strong>
      </div>
      <p className="muted-note">{exercise.prompt}</p>

      {!result && (
        <>
          {!runServerId && (
            <p style={{ color: 'var(--amber)', fontSize: 11, marginBottom: 8 }}>
              Waiting for backend to persist this run... Submit will enable shortly.
            </p>
          )}
          <textarea
            value={reflection}
            onChange={e => setReflection(e.target.value)}
            placeholder="Write your reflection (at least 5 words, ideally 30+)..."
            rows={5}
            className="input"
            style={{ width: '100%', resize: 'vertical', minHeight: 100, fontFamily: 'DM Sans' }}
            disabled={submitting || !runServerId}
          />
          <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 8 }}>
            <Button onClick={submit} disabled={!canSubmit}>
              <Icon name="play" size={15} /> {submitting ? 'Submitting...' : 'Submit Reflection'}
            </Button>
            <span className="muted-note" style={{ fontSize: 10 }}>{wordCount} word(s)</span>
          </div>
          {error && <p style={{ color: 'var(--red)', fontSize: 11, marginTop: 8 }}>{error}</p>}
        </>
      )}

      {result && (
        <>
          <div className="kpi-row" style={{ marginTop: 12 }}>
            <div className="kpi"><span>SCORE</span><strong>{result.score}<small>/100</small></strong></div>
            <div className="kpi"><span>PASS MARK</span><strong>{result.passScore}<small>/100</small></strong></div>
            <div className="kpi"><span>RESULT</span><strong style={{ color: result.passed ? 'var(--green)' : 'var(--red)' }}>{result.passed ? 'PASS' : 'FAIL'}</strong></div>
          </div>
          <table className="stage-table" style={{ marginTop: 12 }}>
            <thead><tr><th>Criterion</th><th>Weight</th><th>Earned</th><th>Satisfied</th></tr></thead>
            <tbody>
              {result.breakdown.map(c => (
                <tr key={c.key}>
                  <td>{c.label}</td>
                  <td>{c.weight}</td>
                  <td>{c.earned}</td>
                  <td style={{ color: c.satisfied ? 'var(--green)' : 'var(--red)' }}>{c.satisfied ? 'Yes' : 'No'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  )
}