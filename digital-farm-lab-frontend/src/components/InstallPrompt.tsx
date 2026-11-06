import { useEffect, useState } from 'react'

type BIPEvent = Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }> }

export default function InstallPrompt() {
  const [deferred, setDeferred] = useState<BIPEvent | null>(null)
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    const handler = (e: Event) => { e.preventDefault(); setDeferred(e as BIPEvent) }
    window.addEventListener('beforeinstallprompt', handler)
    return () => window.removeEventListener('beforeinstallprompt', handler)
  }, [])

  if (!deferred || dismissed) return null

  return (
    <div style={{
      position: 'fixed', left: 16, right: 16, bottom: 16, zIndex: 2000,
      padding: 14, borderRadius: 10, background: '#0e1d19',
      border: '1px solid rgba(175,211,198,.25)', color: '#d9e6e2',
      display: 'flex', alignItems: 'center', gap: 12,
      boxShadow: '0 12px 32px rgba(0,0,0,.5)'
    }}>
      <div style={{ flex: 1 }}>
        <strong style={{ display: 'block', fontSize: 14 }}>Install Digital Farm Lab</strong>
        <small style={{ color: '#718c83', fontSize: 12 }}>Add to home screen — works offline</small>
      </div>
      <button onClick={async () => { await deferred.prompt(); setDeferred(null) }} style={{
        background: '#55d88b', color: '#07100e', border: 0, borderRadius: 6,
        padding: '8px 14px', fontWeight: 700, cursor: 'pointer', fontSize: 13
      }}>Install</button>
      <button onClick={() => setDismissed(true)} aria-label="Dismiss" style={{
        background: 'transparent', color: '#718c83', border: 0, fontSize: 20, cursor: 'pointer', lineHeight: 1
      }}>×</button>
    </div>
  )
}