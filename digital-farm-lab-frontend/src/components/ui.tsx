import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

export function Icon({ name, size = 20 }: { name: string; size?: number }) {
  const paths: Record<string, ReactNode> = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="2" /><rect x="14" y="3" width="7" height="7" rx="2" /><rect x="3" y="14" width="7" height="7" rx="2" /><rect x="14" y="14" width="7" height="7" rx="2" /></>,
    field: <><path d="M3 19c5-7 10-10 18-12" /><path d="M4 14c5-4 10-6 15-7" /><path d="M8 21c4-7 7-10 12-12" /></>,
    flask: <><path d="M9 3h6" /><path d="M10 3v6l-5 8a3 3 0 0 0 2.6 4h8.8a3 3 0 0 0 2.6-4l-5-8V3" /><path d="M7 16h10" /></>,
    chart: <><path d="M4 20V10" /><path d="M10 20V4" /><path d="M16 20v-7" /><path d="M22 20H2" /></>,
    book: <><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /></>,
    history: <><path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 3v5h5" /><path d="M12 7v5l3 2" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1a1.7 1.7 0 0 0 1.9.3A1.7 1.7 0 0 0 10 3V2.8h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z" /></>,
    sun: <><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></>,
    droplet: <path d="M12 2S5 10 5 15a7 7 0 0 0 14 0c0-5-7-13-7-13Z" />,
    rain: <><path d="M7 16a4 4 0 1 1 .9-7.9A5.5 5.5 0 0 1 18.5 10 3 3 0 0 1 18 16Z" /><path d="m8 19-1 2M13 19l-1 2M18 19l-1 2" /></>,
    leaf: <><path d="M20 4c-8 0-14 4-14 10 0 3 2 5 5 5 6 0 9-7 9-15Z" /><path d="M4 21c2-6 7-10 13-13" /></>,
    spark: <><path d="m12 3 1.4 4.2L18 9l-4.6 1.8L12 15l-1.4-4.2L6 9l4.6-1.8Z" /><path d="m19 16 .7 2.1L22 19l-2.3.9L19 22l-.7-2.1L16 19l2.3-.9Z" /></>,
    play: <path d="m8 5 11 7-11 7Z" />,
    pause: <><path d="M9 5v14M15 5v14" /></>,
    stop: <rect x="6" y="6" width="12" height="12" rx="1" />,
    reset: <><path d="M4 4v6h6" /><path d="M5.4 15a8 8 0 1 0 .8-7.8L4 10" /></>,
    irrigation: <><circle cx="6" cy="17" r="2" /><circle cx="18" cy="17" r="2" /><path d="M6 15V9h12v6M4 9h16M12 9V5" /></>,
    water: <><path d="M3 15c3-2 5 2 8 0s5 2 10 0M3 19c3-2 5 2 8 0s5 2 10 0" /><path d="M12 3s-4 4-4 7a4 4 0 0 0 8 0c0-3-4-7-4-7Z" /></>,
    sensor: <><circle cx="12" cy="12" r="2" /><path d="M8.5 8.5a5 5 0 0 0 0 7M15.5 8.5a5 5 0 0 1 0 7M5.7 5.7a9 9 0 0 0 0 12.6M18.3 5.7a9 9 0 0 1 0 12.6" /></>,
    cursor: <path d="m5 3 13 9-6 1-3 6Z" />,
    hand: <path d="M7 11V7a2 2 0 0 1 4 0v3-5a2 2 0 0 1 4 0v5-3a2 2 0 0 1 4 0v7c0 5-3 7-7 7-3 0-5-2-7-5l-2-3a2 2 0 0 1 4-2Z" />,
    connect: <><circle cx="6" cy="6" r="2" /><circle cx="18" cy="18" r="2" /><path d="m8 8 8 8" /></>,
    undo: <><path d="M9 7 4 12l5 5" /><path d="M4 12h10a6 6 0 0 1 6 6" /></>,
    redo: <><path d="m15 7 5 5-5 5" /><path d="M20 12H10a6 6 0 0 0-6 6" /></>,
    folder: <path d="M3 6h7l2 2h9v11H3Z" />,
    save: <><path d="M4 4h14l2 2v14H4Z" /><path d="M8 4v6h8V4M8 20v-6h8v6" /></>,
    fullscreen: <><path d="M8 3H3v5M16 3h5v5M8 21H3v-5M16 21h5v-5" /></>,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 8h.01" /></>,
    wind: <><path d="M3 8h11c3 0 3-4 0-4M3 12h16c3 0 3 4 0 4M3 16h8" /></>,
    antenna: <><path d="M12 12v9M8 21h8M9 8a4 4 0 0 1 6 0M6 5a8 8 0 0 1 12 0" /><circle cx="12" cy="10" r="1" /></>,
    chevron: <path d="m8 10 4 4 4-4" />,
    copy: <><rect x="8" y="8" width="11" height="11" rx="2" /><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3" /></>,
    trash: <><path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14M10 11v6M14 11v6" /></>,
    arrow: <><path d="M5 12h14" /><path d="m14 7 5 5-5 5" /></>,
    menu: <><path d="M4 7h16M4 12h16M4 17h16" /></>,
    close: <><path d="m6 6 12 12M18 6 6 18" /></>,
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>
}

export function Button({ children, className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button className={`btn ${className}`} {...props}>{children}</button>
}

export function Select({ children, className = '', ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={`select ${className}`} {...props}>{children}</select>
}

export function Input({ className = '', ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={`input ${className}`} {...props} />
}

export function Textarea({ className = '', ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={`textarea ${className}`} {...props} />
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`card ${className}`}>{children}</section>
}

export function SectionTitle({ eyebrow, title, action }: { eyebrow?: string; title: string; action?: ReactNode }) {
  return <div className="section-head"><div>{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h2>{title}</h2></div>{action}</div>
}

export function Progress({ value, tone = 'green' }: { value: number; tone?: 'green' | 'blue' | 'amber' }) {
  return <div className={`progress ${tone}`}><span style={{ width: `${Math.min(value, 100)}%` }} /></div>
}

export function Stepper({ value, unit = '', min, max, step = 1, onChange }: { value: number; unit?: string; min: number; max: number; step?: number; onChange: (value: number) => void }) {
  return (
    <div className="builder-stepper">
      <Button onClick={() => onChange(Math.max(min, value - step))} aria-label="Decrease">−</Button>
      <strong>{value.toLocaleString()}{unit}</strong>
      <Button onClick={() => onChange(Math.min(max, value + step))} aria-label="Increase">+</Button>
    </div>
  )
}
