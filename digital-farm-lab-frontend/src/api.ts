// ═══════════════════════════════════════════════════════════════
// API client — hardcoded for production demo
// ═══════════════════════════════════════════════════════════════

const API_ORIGIN = 'https://farmlab-backend-57i8.onrender.com'
const BASE = API_ORIGIN + '/api/v1/workspace'
const TRAINER_BASE = API_ORIGIN + '/api/v1/trainer'
const AUTH: Record<string, string> = {
  Authorization: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMTExMTExMS0xMTExLTExMTEtMTExMS0xMTExMTExMTExMTEiLCJyb2xlcyI6WyJMRUFSTkVSIiwiVFJBSU5FUiJdLCJpYXQiOjE3OTM5NDU5NjMsImV4cCI6MTgyNTQ4MTk2M30.8c8bKebgyF5GmKqp6lJCE7ME8etFrgHzmQUY31Wd8Sg',
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...AUTH, ...(options?.headers || {}) },
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(String(res.status) + ' ' + res.statusText + ': ' + (text.slice(0, 200) || '(no body)'))
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
  listFarms: () => request<BackendFarm[]>(BASE + '/farms'),
  createFarm: (data: Record<string, unknown>) =>
    request<BackendFarm>(BASE + '/farms', { method: 'POST', body: JSON.stringify(data) }),
  updateFarm: (id: string, data: Record<string, unknown>) =>
    request<BackendFarm>(BASE + '/farms/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  deleteFarm: (id: string) =>
    request<void>(BASE + '/farms/' + id, { method: 'DELETE' }),

  listScenarios: () => request<BackendScenario[]>(BASE + '/scenarios'),
  createScenario: (data: Record<string, unknown>) =>
    request<BackendScenario>(BASE + '/scenarios', { method: 'POST', body: JSON.stringify(data) }),
  updateScenario: (id: string, data: Record<string, unknown>) =>
    request<BackendScenario>(BASE + '/scenarios/' + id, { method: 'PUT', body: JSON.stringify(data) }),
  deleteScenario: (id: string) =>
    request<void>(BASE + '/scenarios/' + id, { method: 'DELETE' }),

  listRuns: () => request<BackendRun[]>(BASE + '/runs'),
  createRun: (data: Record<string, unknown>) =>
    request<BackendRun>(BASE + '/runs', { method: 'POST', body: JSON.stringify(data) }),

  trainerListExercises: () => request<TrainerExercise[]>(TRAINER_BASE + '/exercises'),
  trainerExerciseReport: (id: string) => request<TrainerReport>(TRAINER_BASE + '/exercises/' + id + '/report'),
}