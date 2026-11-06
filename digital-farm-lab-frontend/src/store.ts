import { useEffect, useState } from 'react'
import { emptyFarm } from './farmTypes'
import type { PersistedFarm, RunRecord, ScenarioRecord } from './scenarioTypes'

export function usePersistentState<T>(key: string, initial: T) {
  const [value, setValue] = useState<T>(() => {
    try {
      const raw = localStorage.getItem(key)
      return raw ? (JSON.parse(raw) as T) : initial
    } catch {
      return initial
    }
  })
  useEffect(() => {
    try { localStorage.setItem(key, JSON.stringify(value)) } catch { /* storage unavailable */ }
  }, [key, value])
  return [value, setValue] as const
}

export const uid = (prefix: string) => `${prefix}-${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`

export const demoFarm: PersistedFarm = {
  ...emptyFarm,
  id: 'demo-farm',
  name: 'Sunridge Demo Farm',
  location: 'Free State, South Africa',
  totalSize: 40,
  waterSources: [{ id: 'water-1', type: 'dam', x: 78, y: 68 }, { id: 'water-2', type: 'borehole', x: 78, y: 14 }],
  weather: 'clear',
  temperature: 24,
  humidity: 58,
  fields: [
    { id: 'f-a', name: 'Field A', size: 12, soil: 'loamy', crop: 'maize', irrigation: 'sprinkler', x: 6, y: 10, width: 26, height: 32 },
    { id: 'f-b', name: 'Field B', size: 10, soil: 'clay', crop: 'wheat', irrigation: 'drip', x: 36, y: 10, width: 24, height: 30 },
    { id: 'f-c', name: 'Field C', size: 8, soil: 'sandy', crop: 'soybean', irrigation: 'flood', x: 20, y: 52, width: 24, height: 30 },
  ],
}

type PresetSeed = Pick<ScenarioRecord, 'name' | 'description' | 'category' | 'difficulty' | 'duration' | 'adjustableVariables' | 'learningObjectives'> & Partial<ScenarioRecord['startingConditions']>

const seeds: PresetSeed[] = [
  { name: 'Dry Season Survival', category: 'Drought Management', difficulty: 'Intermediate', duration: 120, soilMoisture: 30, rainfall: 0, temperature: 33, adjustableVariables: ['Irrigation volume', 'Irrigation interval'], description: 'A hot, rain-free season with a shrinking dam. Keep crops alive while using as little water as possible.', learningObjectives: ['Prioritise limited water across fields.', 'Recognise early signs of crop water stress.'] },
  { name: 'Smart Irrigation Calendar', category: 'Irrigation Scheduling', difficulty: 'Beginner', duration: 90, adjustableVariables: ['Irrigation volume', 'Irrigation interval'], description: 'Design an irrigation schedule that holds soil moisture near target without wasting water.', learningObjectives: ['Match irrigation timing to crop growth stage.', 'Balance moisture stability against water use.'] },
  { name: 'Sand vs Clay', category: 'Soil Type Comparison', difficulty: 'Beginner', duration: 100, adjustableVariables: ['Irrigation volume', 'Crop variety'], description: 'Compare how sandy and clay soils hold water and respond to the same management.', learningObjectives: ['Explain how soil texture affects water retention.'] },
  { name: 'Late Frost Surprise', category: 'Changing Weather', difficulty: 'Advanced', duration: 150, temperature: 12, rainfall: 20, adjustableVariables: ['Planting date', 'Crop variety', 'Labour budget'], description: 'Unseasonal weather swings test your planting decisions and contingency planning.', learningObjectives: ['Adapt management to changing forecasts.', 'Weigh risk against yield potential.'] },
  { name: 'Nitrogen Balancing Act', category: 'Fertiliser Management', difficulty: 'Intermediate', duration: 110, adjustableVariables: ['Fertiliser rate', 'Irrigation volume'], description: 'Find the fertiliser rate that maximises yield before returns diminish and costs rise.', learningObjectives: ['Identify the point of diminishing returns for nitrogen.', 'Link fertiliser rate to input cost.'] },
  { name: 'Drowned Roots', category: 'Over-irrigation', difficulty: 'Beginner', duration: 80, soilMoisture: 80, adjustableVariables: ['Irrigation volume', 'Irrigation interval'], description: 'The farm starts over-watered. Recover the crop and understand the cost of too much water.', learningObjectives: ['Describe the effects of waterlogging on crops.'] },
  { name: 'Armyworm Alert', category: 'Pest Outbreak', difficulty: 'Intermediate', duration: 90, pestPressure: 35, adjustableVariables: ['Pesticide use', 'Labour budget'], description: 'Pest pressure is climbing. Decide when and how much to spray without wrecking your budget.', learningObjectives: ['Time interventions using pest thresholds.', 'Balance chemical use and cost.'] },
  { name: 'Three-Season Rotation', category: 'Crop Rotation', difficulty: 'Advanced', duration: 365, adjustableVariables: ['Crop variety', 'Planting date', 'Fertiliser rate'], description: 'Plan a rotation across your fields to rebuild soil and spread risk over several seasons.', learningObjectives: ['Explain how rotation improves soil health.', 'Plan crops to reduce disease and pest build-up.'] },
]

export const presetScenarios: ScenarioRecord[] = seeds.map((seed, index) => ({
  id: `preset-${index + 1}`,
  name: seed.name,
  description: seed.description,
  category: seed.category,
  farmId: demoFarm.id,
  participatingFields: demoFarm.fields.map(f => f.id),
  crops: demoFarm.fields.map(f => ({ fieldId: f.id, crop: f.crop })),
  startingConditions: {
    soilMoisture: seed.soilMoisture ?? 55,
    temperature: seed.temperature ?? 24,
    rainfall: seed.rainfall ?? 8,
    budget: seed.budget ?? 25000,
    pestPressure: seed.pestPressure ?? 5,
  },
  adjustableVariables: seed.adjustableVariables,
  duration: seed.duration,
  difficulty: seed.difficulty,
  learningObjectives: seed.learningObjectives,
  source: 'preset',
}))

export type { RunRecord }
