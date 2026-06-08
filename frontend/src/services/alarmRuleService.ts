import api from './api'
import type { AlarmRuleDTO, Metric, Severity } from '../types'

export async function getAllAlarmRules(): Promise<AlarmRuleDTO[]> {
  const { data } = await api.get<AlarmRuleDTO[]>('/alarm-rules')
  return data
}

export async function getRulesBySensor(sensorId: number): Promise<AlarmRuleDTO[]> {
  const { data } = await api.get<AlarmRuleDTO[]>(`/alarm-rules/sensor/${sensorId}`)
  return data
}

export async function createRule(payload: {
  name?: string
  sensorId: number
  metric: Metric
  thresholdMin?: number | null
  thresholdMax?: number | null
  severity: Severity
  cooldownSeconds?: number
}): Promise<AlarmRuleDTO> {
  const { data } = await api.post<AlarmRuleDTO>('/alarm-rules', payload)
  return data
}

export async function updateRule(id: number, payload: {
  name?: string | null
  thresholdMin?: number | null
  thresholdMax?: number | null
  severity?: Severity
  cooldownSeconds?: number
}): Promise<AlarmRuleDTO> {
  const { data } = await api.put<AlarmRuleDTO>(`/alarm-rules/${id}`, payload)
  return data
}

export async function toggleRule(id: number): Promise<AlarmRuleDTO> {
  const { data } = await api.patch<AlarmRuleDTO>(`/alarm-rules/${id}/toggle`)
  return data
}

export async function deleteRule(id: number): Promise<void> {
  await api.delete(`/alarm-rules/${id}`)
}
