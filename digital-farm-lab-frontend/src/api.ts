// Typed wrappers around the Spring Boot workspace API. All requests go
// through /api/v1/workspace/* — the Vite proxy injects the JWT and strips
// the Origin header, so the browser never holds the token.

const BASE = '/api/v1/workspace'

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options?.headers || {}) },
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`${res.status} ${res.statusText}: ${text.slice(0, 200) || '(no body)'}`)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export type EngineOutput = {
  yieldTha: number
  waterUseM3Ha: number
  costPerHa: number
  grossMarginPerHa: number
  disclaimer: string
}

export type BackendFarm = Record<string, unknown> & { id: string }
export type BackendScenario = Record<string, unknown> & { id: string }
export type BackendRun = {
  id: string
  scenarioId: string
  results: EngineOutput
  baseline: EngineOutput
  createdAt: string
}



// ── Trainer types (mirror the backend records) ────────────────────────
export type TrainerExercise = {
  exerciseId: string
  moduleCode: string
  title: string
  passScore: number
  submissionCount: number
  uniqueLearners: number
  averageScore: number
  learnerPassRate: number
}

export type TrainerReport = {
  exerciseId: string
  moduleCode: string
  title: string
  prompt: string
  passScore: number
  statistics: {
    submissionCount: number
    uniqueLearners: number
    passedSubmissions: number
    passedLearners: number
    averageScore: number
    minScore: number
    medianScore: number
    maxScore: number
    submissionPassRate: number
    learnerPassRate: number
    rubricDriftCount: number
  }
  distribution: Array<{
    range: string
    lowerBound: number
    upperBound: number
    meetsPassThreshold: boolean
    count: number
  }>
  criteria: Array<{
    key: string
    label: string
    weight: number
    totalEvaluations: number
    satisfied: number
    satisfactionRate: number
  }>
  recentSubmissions: Array<{
    submissionId: string
    userId: string
    displayName: string | null
    score: number
    passed: boolean
    createdAt: string
  }>
}
export const api = {
  listFarms: () => request<BackendFarm[]>(`${BASE}/farms`),
  createFarm: (data: Record<string, unknown>) =>
    request<BackendFarm>(`${BASE}/farms`, { method: 'POST', body: JSON.stringify(data) }),
  updateFarm: (id: string, data: Record<string, unknown>) =>
    request<BackendFarm>(`${BASE}/farms/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteFarm: (id: string) =>
    request<void>(`${BASE}/farms/${id}`, { method: 'DELETE' }),

  listScenarios: () => request<BackendScenario[]>(`${BASE}/scenarios`),
  createScenario: (data: Record<string, unknown>) =>
    request<BackendScenario>(`${BASE}/scenarios`, { method: 'POST', body: JSON.stringify(data) }),
  updateScenario: (id: string, data: Record<string, unknown>) =>
    request<BackendScenario>(`${BASE}/scenarios/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteScenario: (id: string) =>
    request<void>(`${BASE}/scenarios/${id}`, { method: 'DELETE' }),

  listRuns: () => request<BackendRun[]>(`${BASE}/runs`),
  createRun: (data: Record<string, unknown>) =>
    request<BackendRun>(`${BASE}/runs`, { method: 'POST', body: JSON.stringify(data) }),

  trainerListExercises: () => request<TrainerExercise[]>(`/api/v1/trainer/exercises`),
  trainerExerciseReport: (id: string) => request<TrainerReport>(`/api/v1/trainer/exercises/${id}/report`),
}
