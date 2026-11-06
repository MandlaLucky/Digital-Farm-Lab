export type SoilType = 'sandy' | 'clay' | 'loamy' | 'silt'
export type CropType = 'maize' | 'wheat' | 'soybean' | 'sunflower' | 'vegetables' | 'none'
export type WaterSourceType = 'dam' | 'borehole' | 'river' | 'reservoir'
export type WaterSource = { id: string; type: WaterSourceType; x: number; y: number }
export type IrrigationType = 'drip' | 'sprinkler' | 'flood' | 'none'
export type WeatherType = 'clear' | 'cloudy' | 'rain' | 'hot'

export type FarmField = {
  id: string
  name: string
  size: number
  soil: SoilType
  crop: CropType
  irrigation: IrrigationType
  x: number
  y: number
  width: number
  height: number
}

export type FarmConfig = {
  name: string
  location: string
  totalSize: number
  fields: FarmField[]
  waterSources: WaterSource[]
  weather: WeatherType
  temperature: number
  humidity: number
}

export const emptyFarm: FarmConfig = {
  name: '',
  location: '',
  totalSize: 40,
  fields: [],
  waterSources: [],
  weather: 'clear',
  temperature: 24,
  humidity: 62,
}

/** Accepts farms saved by earlier versions (single water source) and returns the current shape. */
export function normalizeFarm<T extends Partial<FarmConfig> & Record<string, unknown>>(raw: T): T & FarmConfig {
  const legacy = raw as Record<string, unknown>
  let waterSources = Array.isArray(raw.waterSources) ? raw.waterSources : []
  if (!waterSources.length && typeof legacy.waterSource === 'string' && legacy.waterSource) {
    waterSources = [{ id: 'water-1', type: legacy.waterSource as WaterSourceType, x: Number(legacy.waterX ?? 78), y: Number(legacy.waterY ?? 68) }]
  }
  return { ...emptyFarm, ...raw, waterSources } as T & FarmConfig
}

export const waterLayoutSlots = [[78, 68], [78, 14], [8, 72], [50, 76], [88, 40], [60, 4]]
