import { useEffect, useState } from 'react'
import { Icon } from './ui'
import { api } from '../api'

type Reading = { id: number; type: string; value: number; recordedAt: string }

export default function SensorPanel({ fieldId }: { fieldId: string }) {
  const [readings, setReadings] = useState<Reading[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    api.sensorReadings(fieldId)
      .then(page => setReadings(page.readings || []))
      .catch(e => setError(String(e.message || e)))
      .finally(() => setLoading(false))
  }, [fieldId])

  const header = (
    <div className="property-section-title">
      <strong>SENSOR HISTORY</strong>
      <Icon name="sensor" size={14} />
    </div>
  )

  if (loading) return <div className="property-section">{header}<p className="muted-note">Loading...</p></div>
  if (error) return <div className="property-section">{header}<p className="muted-note" style={{ color: 'var(--red)' }}>Unavailable</p></div>
  if (readings.length === 0) return <div className="property-section">{header}<p className="muted-note">No readings for this field yet</p></div>

  const byType: Record<string, Reading[]> = {}
  for (const r of readings) {
    if (!byType[r.type]) byType[r.type] = []
    byType[r.type].push(r)
  }

  return (
    <div className="property-section">
      {header}
      <p className="muted-note" style={{ fontSize: 10, marginBottom: 8 }}>{readings.length} real readings from backend</p>
      {Object.entries(byType).map(([type, list]) => (
        <div key={type} style={{ marginBottom: 10 }}>
          <div style={{ fontSize: 10, color: 'var(--muted)', marginBottom: 4 }}>{type}</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2 }}>
            {list.slice(0, 7).map(r => (
              <div key={r.id} title={r.recordedAt} style={{ fontSize: 9, color: 'var(--green)', textAlign: 'center', fontFamily: 'JetBrains Mono' }}>
                {Number(r.value).toFixed(0)}
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}