// Client-side wrapper that calls the Spring Boot demo engine via the Vite
// dev-server proxy. The proxy (configured in vite.config.ts) injects the
// JWT from .env.local on the server side, so the token never reaches the
// browser bundle. Same security model as the previous Next.js project.

export type EngineInput = {
  irrigationMm: number
  fertiliserKgHa: number
  plantingDate: string
}

export type EngineResult = {
  yieldTha: number
  waterUseM3Ha: number
  costPerHa: number
  grossMarginPerHa: number
  disclaimer: string
}

export type EngineRunResponse = {
  scenario: EngineResult
  baseline: EngineResult
}

export async function runEngine(input: EngineInput): Promise<EngineRunResponse> {
  const res = await fetch('/api/v1/demo/run', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`Engine returned ${res.status}: ${text.slice(0, 200) || 'no body'}`)
  }

  const raw = (await res.json()) as { scenario: Record<string, unknown>; baseline: Record<string, unknown> }
  const coerce = (r: Record<string, unknown>): EngineResult => ({
    yieldTha: Number(r.yieldTha),
    waterUseM3Ha: Number(r.waterUseM3Ha),
    costPerHa: Number(r.costPerHa),
    grossMarginPerHa: Number(r.grossMarginPerHa),
    disclaimer: String(r.disclaimer ?? ''),
  })
  return { scenario: coerce(raw.scenario), baseline: coerce(raw.baseline) }
}