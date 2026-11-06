import type { ScenarioCategory } from '../../scenarioTypes'

export const categoryIcons: Record<ScenarioCategory, string> = {
  'Drought Management': 'sun',
  'Irrigation Scheduling': 'droplet',
  'Soil Type Comparison': 'field',
  'Changing Weather': 'wind',
  'Fertiliser Management': 'flask',
  'Planting Date Optimisation': 'history',
  'Over-irrigation': 'rain',
  'Pest Outbreak': 'sensor',
  'Crop Rotation': 'reset',
  'Harvest Timing': 'leaf',
  'Farm Budget Management': 'chart',
  'Maximum Yield Challenge': 'spark',
}

export const categoryBlurbs: Record<ScenarioCategory, string> = {
  'Drought Management': 'Keep crops alive when water is scarce.',
  'Irrigation Scheduling': 'Decide when and how much to irrigate.',
  'Soil Type Comparison': 'See how soil texture changes outcomes.',
  'Changing Weather': 'Adapt to heat, frost and unexpected rain.',
  'Fertiliser Management': 'Find the right nutrient rate and timing.',
  'Planting Date Optimisation': 'Choose the best window to plant.',
  'Over-irrigation': 'Learn the cost of too much water.',
  'Pest Outbreak': 'Respond to pests without overspending.',
  'Crop Rotation': 'Plan rotations that rebuild soil health.',
  'Harvest Timing': 'Balance maturity, quality and weather risk.',
  'Farm Budget Management': 'Make the most of a limited budget.',
  'Maximum Yield Challenge': 'Push every decision for the best yield.',
}
