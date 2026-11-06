import type { FarmConfig } from './farmTypes'

export const scenarioCategories = [
  'Drought Management',
  'Irrigation Scheduling',
  'Soil Type Comparison',
  'Changing Weather',
  'Fertiliser Management',
  'Planting Date Optimisation',
  'Over-irrigation',
  'Pest Outbreak',
  'Crop Rotation',
  'Harvest Timing',
  'Farm Budget Management',
  'Maximum Yield Challenge',
] as const

export const variableOptions = [
  'Irrigation volume',
  'Irrigation interval',
  'Fertiliser rate',
  'Planting date',
  'Crop variety',
  'Pesticide use',
  'Harvest date',
  'Labour budget',
] as const

export type ScenarioCategory = (typeof scenarioCategories)[number]
export type AdjustableVariable = (typeof variableOptions)[number]

export type PersistedFarm = FarmConfig & { id: string }

export type ScenarioRecord = {
  id: string
  name: string
  description: string
  category: ScenarioCategory
  farmId: string
  participatingFields: string[]
  crops: { fieldId: string; crop: string }[]
  startingConditions: {
    soilMoisture: number
    temperature: number
    rainfall: number
    budget: number
    pestPressure: number
  }
  adjustableVariables: AdjustableVariable[]
  duration: number
  difficulty: 'Beginner' | 'Intermediate' | 'Advanced'
  learningObjectives: string[]
  revisionOf?: string
  updatedAt?: string
  source: 'preset' | 'mine'
}

export type EngineOutput = {
  yieldTha: number
  waterUseM3Ha: number
  costPerHa: number
  grossMarginPerHa: number
  disclaimer: string
}

export type RunRecord = {
  id: string
  scenarioId?: string
  serverId?: string
  finishedAt: string
  complete: boolean
  farmName: string
  scenarioName: string
  category: string
  difficulty: string
  duration: number
  dayReached: number
  series: { day: number; moisture: number; growth: number; water: number }[]
  decisions: { targetMoisture: number; flowRate: number; nitrogen: number }
  metrics: { moisture: number; growth: number; waterUsed: number; yieldTHa: number; stress: number }
  scores: { moisture: number; water: number; nutrients: number; overall: number }
  objectives: { text: string; achieved: boolean }[]
  engineResults?: EngineOutput
  engineBaseline?: EngineOutput
  details?: {
    start: { soilMoisture: number; temperature: number; rainfall: number; budget: number; pestPressure: number; humidity: number; weather: string }
    avgMoistureGap: number
    effectiveFlow: number
    yieldFactor: number
    bonuses: { monitoring: number; moisture: number; pump: number; flow: number; storage: number; landPlots: number }
    components: { name: string; kind: string }[]
    events: { time: string; tone: string; text: string }[]
    fields: { name: string; crop: string; soil: string; irrigation: string; size: number }[]
    waterSources: string[]
    adjustable: string[]
  }
}