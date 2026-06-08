import api from './api'
import type { WeatherStationDTO, StationOverviewDTO, NodeDTO, NodeCommandDTO, SensorDTO, MeasurementDTO, Metric, AlarmNotificationDTO, Status } from '../types'

export async function getStations(): Promise<WeatherStationDTO[]> {
  const { data } = await api.get<WeatherStationDTO[]>('/stations')
  return data
}

export async function getStationOverview(id: number): Promise<StationOverviewDTO> {
  const { data } = await api.get<StationOverviewDTO>(`/stations/${id}/overview`)
  return data
}

export async function createStation(payload: {
  name: string
  locationLat?: number | null
  locationLon?: number | null
}): Promise<WeatherStationDTO> {
  const { data } = await api.post<WeatherStationDTO>('/stations', payload)
  return data
}

export async function updateStation(id: number, payload: {
  name: string
  locationLat?: number | null
  locationLon?: number | null
}): Promise<WeatherStationDTO> {
  const { data } = await api.put<WeatherStationDTO>(`/stations/${id}`, payload)
  return data
}

export async function getNodesByStation(stationId: number): Promise<NodeDTO[]> {
  const { data } = await api.get<NodeDTO[]>(`/nodes/station/${stationId}`)
  return data
}

export async function getUnassignedNodes(): Promise<NodeDTO[]> {
  const { data } = await api.get<NodeDTO[]>('/nodes/unassigned')
  return data
}

export async function assignNodeToStation(nodeId: number, stationId: number): Promise<NodeDTO> {
  const { data } = await api.patch<NodeDTO>(`/nodes/${nodeId}/station`, { stationId })
  return data
}

export async function getSensorsByNode(nodeId: number): Promise<SensorDTO[]> {
  const { data } = await api.get<SensorDTO[]>(`/sensors/node/${nodeId}`)
  return data
}

export async function getMeasurementHistory(sensorId: number, metric: Metric, from: string, to: string): Promise<MeasurementDTO[]> {
  const { data } = await api.get<{ content: MeasurementDTO[] }>(
    `/measurements/history?sensorId=${sensorId}&metric=${metric}&from=${from}&to=${to}&size=1440`
  )
  return data.content
}

export async function updateStationStatus(id: number, status: Status): Promise<WeatherStationDTO> {
  const { data } = await api.patch<WeatherStationDTO>(`/stations/${id}/status`, { status })
  return data
}

export async function getNotifications(): Promise<AlarmNotificationDTO[]> {
  const { data } = await api.get<AlarmNotificationDTO[]>('/notifications')
  return data
}

export async function markNotificationRead(id: number): Promise<void> {
  await api.post(`/notifications/${id}/read`)
}

export async function setNodeInterval(nodeId: number, intervalSeconds: number): Promise<NodeCommandDTO> {
  const { data } = await api.post<NodeCommandDTO>(`/nodes/${nodeId}/interval`, { intervalSeconds })
  return data
}

export async function getAllNodes(): Promise<NodeDTO[]> {
  const { data } = await api.get<NodeDTO[]>('/nodes')
  return data
}

export async function updateNode(id: number, displayName: string): Promise<NodeDTO> {
  const { data } = await api.put<NodeDTO>(`/nodes/${id}`, { displayName })
  return data
}

export async function updateNodeStatus(id: number, status: Status): Promise<NodeDTO> {
  const { data } = await api.patch<NodeDTO>(`/nodes/${id}/status`, { status })
  return data
}

export async function issueNodeCommand(nodeId: number, command: 'REQUEST_READINGS' | 'REBOOT'): Promise<NodeCommandDTO> {
  const { data } = await api.post<NodeCommandDTO>(`/nodes/${nodeId}/commands`, { command })
  return data
}

export async function deleteNode(id: number): Promise<void> {
  await api.delete(`/nodes/${id}`)
}

export async function activateSensor(id: number): Promise<void> {
  await api.patch(`/sensors/${id}/activate`)
}

export async function deactivateSensor(id: number): Promise<void> {
  await api.patch(`/sensors/${id}/deactivate`)
}

export async function deleteSensor(id: number): Promise<void> {
  await api.delete(`/sensors/${id}`)
}

export async function deleteStation(id: number): Promise<void> {
  await api.delete(`/stations/${id}`)
}

export async function unassignNodeFromStation(nodeId: number): Promise<void> {
  await api.delete(`/nodes/${nodeId}/station`)
}
