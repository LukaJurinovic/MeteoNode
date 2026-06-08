import type { Metric, SensorType } from '../types'

export const SENSOR_METRICS: Record<SensorType, Metric[]> = {
  BMP280:     ['TEMPERATURE', 'PRESSURE'],
  DHT11:      ['HUMIDITY'],
  GUVA_S12SD: ['UV_INDEX'],
  GY_302:     ['ILLUMINANCE'],
}
