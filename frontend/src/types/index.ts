export type Role = 'ADMIN' | 'OPERATOR' | 'USER'
export type Status = 'ONLINE' | 'OFFLINE'
export type Metric = 'TEMPERATURE' | 'PRESSURE' | 'HUMIDITY' | 'UV_INDEX' | 'ILLUMINANCE'
export type SensorType = 'BMP280' | 'DHT11' | 'GUVA_S12SD' | 'GY_302'
export type Severity = 'INFO' | 'WARNING' | 'CRITICAL'

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  username: string
  role: Role
}

export interface WeatherStationDTO {
  id: number
  name: string
  ownerId: number
  locationLat: number | null
  locationLon: number | null
  status: Status
}

export interface NodeDTO {
  id: number
  serialNumber: string
  displayName: string | null
  stationId: number | null
  status: Status
  lastSeen: string | null
  reportingIntervalSeconds: number
}

export interface SensorDTO {
  id: number
  nodeId: number
  sensorType: SensorType
  isActive: boolean
}

export interface MeasurementDTO {
  id: number
  sensorId: number
  metric: Metric
  value: number
  measuredAt: string
}

export interface SensorReading {
  sensorId: number
  nodeId: number
  nodeDisplayName: string | null
  metric: Metric
  value: number
  measuredAt: string
}

export interface StationOverviewDTO {
  stationId: number
  stationName: string
  status: Status
  nodeCount: number
  onlineNodes: number
  readings: SensorReading[]
}

export interface AlarmNotificationDTO {
  id: number
  ruleId: number
  sensorId: number
  metric: Metric
  value: number
  message: string
  severity: Severity
  triggeredAt: string
  read: boolean
}

export interface AlarmRuleDTO {
  id: number
  name: string | null
  sensorId: number
  sensorType: SensorType | null
  nodeId: number | null
  metric: Metric
  thresholdMin: number | null
  thresholdMax: number | null
  severity: Severity
  isActive: boolean
  cooldownSeconds: number
  lastFiredAt: string | null
}

export interface NodeCommandDTO {
  id: number
  nodeId: number
  command: string
  status: string
  payload: string | null
  createdAt: string
  deliveredAt: string | null
}

export interface UserDTO {
  id: number
  username: string
  email: string
  role: Role
  createdAt: string
}

