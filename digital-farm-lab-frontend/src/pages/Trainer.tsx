import { useEffect, useState } from 'react'
import { Button, Icon } from '../components/ui'
import { api, type TrainerExercise, type TrainerReport } from '../api'

export default function TrainerPage() {
  const [exercises, setExercises] = useState<TrainerExercise[]>([])
  const [selectedId, setSelectedId] = useState<string>('')
  const [report, setReport] = useState<TrainerReport | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.trainerListExercises()
      .then(list => {
        setExercises(list)
        if (list.length > 0) setSelectedId(list[0].exerciseId)
      })
      .catch(err => setError(String(err.message || err)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!selectedId) return
    setReport(null)
    api.trainerExerciseReport(selectedId)
      .then(setReport)
      .catch(err => setError(String(err.message || err)))
  }, [selectedId])

  if (loading) return <div className="page center-page"><div className="scenario-empty-card"><strong>Loading…</strong></div></div>
  if (error) return (
    <div className="page center-page">
      <div className="scenario-empty-card">
        <div><Icon name="chart" size={29} /></div>
        <span>TRAINER · ERROR</span>
        <strong>Could not load cohort data</strong>
        <p>{error}</p>
      </div>
    </div>
  )
  if (exercises.length === 0) return (
    <div className="page center-page">
      <div className="scenario-empty-card">
        <div><Icon name="book" size={29} /></div>
        <span>TRAINER · NO DATA</span>
        <strong>No exercises yet</strong>
        <p>Exercises appear here once learners submit runs.</p>
      </div>
    </div>
  )

  const maxCount = report ? Math.max(1, ...report.distribution.map(b => b.count)) : 1

  return (
    <div className="page">
      <div className="page-title">
        <div>
          <span>TRAINER · COHORT ANALYSIS</span>
          <strong>Cohort Results</strong>
          <p>Exercise performance, score distribution and per-criterion breakdown.</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '300px 1fr', gap: 16 }}>
        <aside className="panel-card">
          <div className="panel-card-title"><span>EXERCISES</span><strong>{exercises.length} total</strong></div>
          <div style={{ display: 'grid', gap: 6 }}>
            {exercises.map(ex => (
              <Button
                key={ex.exerciseId}
                className="saved-farm"
                onClick={() => setSelectedId(ex.exerciseId)}
                style={{ borderColor: selectedId === ex.exerciseId ? 'var(--green)' : undefined }}
              >
                <div><Icon name="book" size={15} /></div>
                <span>
                  <strong>{ex.title}</strong>
                  <small>{ex.moduleCode} · {ex.submissionCount} subs · avg {ex.averageScore}</small>
                </span>
              </Button>
            ))}
          </div>
        </aside>

        <main>
          {!report && <div className="panel-card"><p className="muted-note">Loading report…</p></div>}
          {report && (
            <>
              <div className="kpi-row">
                <div className="kpi"><span>SUBMISSIONS</span><strong>{report.statistics.submissionCount}</strong><em>{report.statistics.uniqueLearners} learners</em></div>
                <div className="kpi"><span>AVERAGE SCORE</span><strong>{report.statistics.averageScore}</strong><em>median {report.statistics.medianScore}</em></div>
                <div className="kpi"><span>LEARNER PASS</span><strong>{report.statistics.learnerPassRate}%</strong><em>{report.statistics.passedLearners}/{report.statistics.uniqueLearners}</em></div>
                <div className="kpi"><span>RANGE</span><strong>{report.statistics.minScore}–{report.statistics.maxScore}</strong><em>of 100</em></div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 16 }}>
                <section className="panel-card">
                  <div className="panel-card-title"><span>DISTRIBUTION</span><strong>Score buckets</strong></div>
                  {report.distribution.map(b => (
                    <div key={b.range} style={{ display: 'grid', gridTemplateColumns: '70px 1fr 32px', gap: 8, alignItems: 'center', marginBottom: 6 }}>
                      <span style={{ fontSize: 11, color: b.meetsPassThreshold ? 'var(--green)' : 'var(--muted)' }}>{b.range}</span>
                      <div style={{ height: 8, background: 'var(--panel)', borderRadius: 4, overflow: 'hidden' }}>
                        <div style={{ height: '100%', width: `${(b.count / maxCount) * 100}%`, background: b.meetsPassThreshold ? 'var(--green)' : 'var(--amber)' }} />
                      </div>
                      <span style={{ fontSize: 11, textAlign: 'right' }}>{b.count}</span>
                    </div>
                  ))}
                </section>

                <section className="panel-card">
                  <div className="panel-card-title"><span>CRITERIA</span><strong>Satisfaction rates</strong></div>
                  {report.criteria.map(c => (
                    <div key={c.key} style={{ marginBottom: 8 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, marginBottom: 2 }}>
                        <span>{c.label}</span>
                        <strong style={{ color: 'var(--green)' }}>{c.satisfactionRate}%</strong>
                      </div>
                      <div style={{ height: 4, background: 'var(--panel)', borderRadius: 2, overflow: 'hidden' }}>
                        <div style={{ height: '100%', width: `${c.satisfactionRate}%`, background: 'var(--green)' }} />
                      </div>
                    </div>
                  ))}
                </section>
              </div>

              <section className="panel-card" style={{ marginTop: 16 }}>
                <div className="panel-card-title"><span>RECENT SUBMISSIONS</span><strong>{report.recentSubmissions.length} latest</strong></div>
                <table className="stage-table">
                  <thead><tr><th>Learner</th><th>Score</th><th>Result</th><th>Submitted</th></tr></thead>
                  <tbody>
                    {report.recentSubmissions.map(s => (
                      <tr key={s.submissionId}>
                        <td>{s.displayName ?? 'Anonymous'}</td>
                        <td><strong>{s.score}</strong>/100</td>
                        <td style={{ color: s.passed ? 'var(--green)' : 'var(--red)' }}>{s.passed ? 'PASS' : 'FAIL'}</td>
                        <td style={{ fontSize: 10 }}>{new Date(s.createdAt).toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
            </>
          )}
        </main>
      </div>
    </div>
  )
}