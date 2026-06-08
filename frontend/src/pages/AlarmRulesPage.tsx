import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getStations, getNodesByStation, getSensorsByNode, getAllNodes } from '../services/stationService'
import { getAllAlarmRules, createRule, toggleRule, deleteRule } from '../services/alarmRuleService'
import type { Metric, Severity, SensorType } from '../types'
import { SENSOR_METRICS } from '../utils/sensorMetrics'
import Spinner from '../components/Spinner'

const ALL_METRICS: Metric[] = ['TEMPERATURE', 'PRESSURE', 'HUMIDITY', 'UV_INDEX', 'ILLUMINANCE']
const SEVERITIES: Severity[] = ['INFO', 'WARNING', 'CRITICAL']

const severityColor: Record<Severity, string> = {
  INFO:     'text-zinc-500',
  WARNING:  'text-amber-600',
  CRITICAL: 'text-red-600',
}

export default function AlarmRulesPage() {
  const queryClient = useQueryClient()

  const [stationId, setStationId] = useState<number | null>(null)
  const [nodeId, setNodeId] = useState<number | null>(null)
  const [sensorId, setSensorId] = useState<number | null>(null)

  const [form, setForm] = useState({
    name: '',
    metric: 'TEMPERATURE' as Metric,
    thresholdMin: '',
    thresholdMax: '',
    severity: 'WARNING' as Severity,
    cooldownSeconds: '',
  })
  const [formError, setFormError] = useState<string | null>(null)

  const { data: stations } = useQuery({ queryKey: ['stations'], queryFn: getStations })
  const { data: allNodes } = useQuery({ queryKey: ['nodes'], queryFn: getAllNodes })
  const { data: nodes } = useQuery({
    queryKey: ['nodes', stationId],
    queryFn: () => getNodesByStation(stationId!),
    enabled: stationId != null,
  })
  const { data: sensors } = useQuery({
    queryKey: ['sensors', nodeId],
    queryFn: () => getSensorsByNode(nodeId!),
    enabled: nodeId != null,
  })
  const { data: allRules, isLoading: loadingAllRules } = useQuery({
    queryKey: ['alarm-rules'],
    queryFn: getAllAlarmRules,
  })

  const nodeMap = useMemo(() => new Map((allNodes ?? []).map(n => [n.id, n])), [allNodes])
  const stationMap = useMemo(() => new Map((stations ?? []).map(s => [s.id, s])), [stations])

  type RuleGroup = { stationId: number | null; stationName: string; rules: typeof allRules }
  const ruleGroups = useMemo((): RuleGroup[] => {
    if (!allRules) return []
    const groups = new Map<string, RuleGroup>()
    for (const rule of allRules) {
      const node = rule.nodeId != null ? nodeMap.get(rule.nodeId) : null
      const stId = node?.stationId ?? null
      const key = stId != null ? String(stId) : '__unassigned__'
      if (!groups.has(key)) {
        groups.set(key, {
          stationId: stId,
          stationName: stId != null ? (stationMap.get(stId)?.name ?? `Station #${stId}`) : 'Unassigned nodes',
          rules: [],
        })
      }
      groups.get(key)!.rules!.push(rule)
    }
    return [...groups.values()].sort((a, b) => {
      if (a.stationId === null) return 1
      if (b.stationId === null) return -1
      return a.stationName.localeCompare(b.stationName)
    })
  }, [allRules, nodeMap, stationMap])

  const selectedSensor = sensors?.find(s => s.id === sensorId)
  const validMetrics: Metric[] = selectedSensor
    ? (SENSOR_METRICS[selectedSensor.sensorType as SensorType] ?? ALL_METRICS)
    : ALL_METRICS

  const createMutation = useMutation({
    mutationFn: (currentSensorId: number) => createRule({
      name: form.name || undefined,
      sensorId: currentSensorId,
      metric: form.metric,
      thresholdMin: form.thresholdMin ? parseFloat(form.thresholdMin) : null,
      thresholdMax: form.thresholdMax ? parseFloat(form.thresholdMax) : null,
      severity: form.severity,
      cooldownSeconds: form.cooldownSeconds ? parseInt(form.cooldownSeconds, 10) : 0,
    }),
    onSuccess: (_, currentSensorId) => {
      queryClient.invalidateQueries({ queryKey: ['alarm-rules', currentSensorId] })
      queryClient.invalidateQueries({ queryKey: ['alarm-rules'] })
      setForm({ name: '', metric: validMetrics[0] ?? 'TEMPERATURE', thresholdMin: '', thresholdMax: '', severity: 'WARNING', cooldownSeconds: '' })
      setFormError(null)
    },
    onError: () => setFormError('Failed to create rule.'),
  })

  const toggleMutation = useMutation({
    mutationFn: ({ ruleId }: { ruleId: number; targetSensorId: number }) => toggleRule(ruleId),
    onSuccess: (_, { targetSensorId }) => {
      queryClient.invalidateQueries({ queryKey: ['alarm-rules', targetSensorId] })
      queryClient.invalidateQueries({ queryKey: ['alarm-rules'] })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: ({ ruleId }: { ruleId: number; targetSensorId: number }) => deleteRule(ruleId),
    onSuccess: (_, { targetSensorId }) => {
      queryClient.invalidateQueries({ queryKey: ['alarm-rules', targetSensorId] })
      queryClient.invalidateQueries({ queryKey: ['alarm-rules'] })
    },
  })

  const selectClass = 'bg-white border border-zinc-300 text-xs text-zinc-700 px-2 py-1.5 font-mono focus:outline-none focus:border-emerald-500'

  return (
    <div className="px-8 py-7">
      <div className="mb-6 border-b border-zinc-200 pb-4">
        <h1 className="text-sm font-semibold uppercase tracking-widest text-zinc-900">Alarm Rules</h1>
        <p className="mt-0.5 text-xs text-zinc-500">Threshold-based sensor alerts</p>
      </div>

      {/* All rules grouped by station */}
      <div className="mb-8">
        <h2 className="mb-4 text-xs font-medium uppercase tracking-widest text-zinc-500">All Rules</h2>
        {loadingAllRules && (
          <div className="flex items-center gap-2 text-xs text-zinc-500 mb-4">
            <Spinner /> Loading…
          </div>
        )}
        {allRules && allRules.length === 0 && (
          <p className="text-xs text-zinc-400">No alarm rules configured yet.</p>
        )}
        {ruleGroups.map(group => (
          <div key={String(group.stationId)} className="mb-5">
            <p className="mb-2 text-[10px] font-medium uppercase tracking-widest text-zinc-500">
              {group.stationName}
            </p>
            <div className="border border-zinc-200">
              <div className="grid grid-cols-7 border-b border-zinc-200 bg-zinc-50 px-4 py-2">
                {['Sensor', 'Node', 'Metric', 'Min / Max', 'Severity', 'Cooldown', ''].map((h, i) => (
                  <span key={i} className="text-xs font-medium uppercase tracking-widest text-zinc-400">{h}</span>
                ))}
              </div>
              {group.rules!.map((rule, i) => {
                const node = rule.nodeId != null ? nodeMap.get(rule.nodeId) : null
                const nodeName = node ? (node.displayName ?? node.serialNumber) : '—'
                return (
                  <div
                    key={rule.id}
                    className={`grid grid-cols-7 items-center px-4 py-3 ${
                      i !== group.rules!.length - 1 ? 'border-b border-zinc-200' : ''
                    } ${!rule.isActive ? 'opacity-40' : ''}`}
                  >
                    <span className="font-mono text-xs text-zinc-600">{rule.sensorType ?? `#${rule.sensorId}`}</span>
                    <span className="font-mono text-xs text-zinc-500">{nodeName}</span>
                    <span className="font-mono text-xs text-zinc-600">{rule.metric}</span>
                    <span className="font-mono text-xs text-zinc-500">
                      {rule.thresholdMin ?? '—'} / {rule.thresholdMax ?? '—'}
                    </span>
                    <span className={`font-mono text-xs ${severityColor[rule.severity]}`}>{rule.severity}</span>
                    <span className="font-mono text-xs text-zinc-500">
                      {rule.cooldownSeconds > 0 ? `${rule.cooldownSeconds}s` : '—'}
                    </span>
                    <div className="flex gap-2">
                      <button
                        onClick={() => toggleMutation.mutate({ ruleId: rule.id, targetSensorId: rule.sensorId })}
                        className="font-mono text-[10px] text-zinc-600 hover:text-zinc-900 transition-colors"
                      >
                        {rule.isActive ? 'disable' : 'enable'}
                      </button>
                      <button
                        onClick={() => {
                          if (window.confirm('Delete this alarm rule?')) deleteMutation.mutate({ ruleId: rule.id, targetSensorId: rule.sensorId })
                        }}
                        className="font-mono text-[10px] text-red-500 hover:text-red-600 transition-colors"
                      >delete</button>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Cascading selectors */}
      <h2 className="mb-3 text-xs font-medium uppercase tracking-widest text-zinc-500">Create Rule</h2>
      <div className="mb-6 flex flex-wrap gap-3">
        <select
          value={stationId ?? ''}
          onChange={e => { setStationId(Number(e.target.value) || null); setNodeId(null); setSensorId(null) }}
          className={selectClass}
        >
          <option value="">Select station…</option>
          {stations?.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>

        <select
          value={nodeId ?? ''}
          onChange={e => { setNodeId(Number(e.target.value) || null); setSensorId(null) }}
          disabled={!stationId}
          className={`${selectClass} disabled:opacity-40`}
        >
          <option value="">Select node…</option>
          {nodes?.map(n => <option key={n.id} value={n.id}>{n.displayName ?? n.serialNumber}</option>)}
        </select>

        <select
          value={sensorId ?? ''}
          onChange={e => {
            const id = Number(e.target.value) || null
            setSensorId(id)
            if (id) {
              const s = sensors?.find(s => s.id === id)
              if (s) {
                const metrics = SENSOR_METRICS[s.sensorType as SensorType] ?? ALL_METRICS
                setForm(f => ({ ...f, metric: metrics[0] }))
              }
            }
          }}
          disabled={!nodeId}
          className={`${selectClass} disabled:opacity-40`}
        >
          <option value="">Select sensor…</option>
          {sensors?.map(s => <option key={s.id} value={s.id}>{s.sensorType} #{s.id}</option>)}
        </select>
      </div>

      {sensorId && (
        <>
          {/* Create rule form */}
          <div className="border border-zinc-200 bg-zinc-50 p-4">
            <h3 className="mb-3 text-xs font-medium uppercase tracking-widest text-zinc-500">New Rule</h3>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              <div>
                <label className="mb-1 block text-xs text-zinc-400">Name (optional)</label>
                <input
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="High temp alert"
                  className="w-full bg-white border border-zinc-300 px-2 py-1.5 text-xs text-zinc-800 font-mono placeholder-zinc-400 focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs text-zinc-400">Metric</label>
                <select
                  value={form.metric}
                  onChange={e => setForm(f => ({ ...f, metric: e.target.value as Metric }))}
                  className={selectClass + ' w-full'}
                >
                  {validMetrics.map(m => <option key={m} value={m}>{m}</option>)}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs text-zinc-400">Severity</label>
                <select
                  value={form.severity}
                  onChange={e => setForm(f => ({ ...f, severity: e.target.value as Severity }))}
                  className={selectClass + ' w-full'}
                >
                  {SEVERITIES.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs text-zinc-400">Min threshold</label>
                <input
                  value={form.thresholdMin}
                  onChange={e => setForm(f => ({ ...f, thresholdMin: e.target.value }))}
                  type="number"
                  step="any"
                  placeholder="—"
                  className="w-full bg-white border border-zinc-300 px-2 py-1.5 text-xs text-zinc-800 font-mono placeholder-zinc-400 focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs text-zinc-400">Max threshold</label>
                <input
                  value={form.thresholdMax}
                  onChange={e => setForm(f => ({ ...f, thresholdMax: e.target.value }))}
                  type="number"
                  step="any"
                  placeholder="—"
                  className="w-full bg-white border border-zinc-300 px-2 py-1.5 text-xs text-zinc-800 font-mono placeholder-zinc-400 focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs text-zinc-400">Cooldown (s)</label>
                <input
                  value={form.cooldownSeconds}
                  onChange={e => setForm(f => ({ ...f, cooldownSeconds: e.target.value }))}
                  type="number"
                  min={0}
                  max={86400}
                  placeholder="0"
                  className="w-full bg-white border border-zinc-300 px-2 py-1.5 text-xs text-zinc-800 font-mono placeholder-zinc-400 focus:outline-none focus:border-emerald-500"
                />
              </div>
            </div>
            {formError && <p className="mt-2 text-xs text-red-500">{formError}</p>}
            <div className="mt-3 flex justify-end">
              <button
                onClick={() => createMutation.mutate(sensorId!)}
                disabled={createMutation.isPending}
                className="bg-emerald-500 px-4 py-1.5 text-xs font-medium text-white hover:bg-emerald-600 transition-colors disabled:opacity-40"
              >
                {createMutation.isPending ? 'Creating…' : 'Create Rule'}
              </button>
            </div>
          </div>
        </>
      )}

      {!sensorId && (
        <p className="text-xs text-zinc-400">Select a station, node, and sensor above to create a rule.</p>
      )}
    </div>
  )
}
