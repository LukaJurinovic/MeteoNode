import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { getStationOverview, getNodesByStation, getUnassignedNodes, assignNodeToStation, setNodeInterval, issueNodeCommand, updateNodeStatus } from '../services/stationService'
import { useAuth } from '../contexts/AuthContext'
import StatusBadge from '../components/StatusBadge'
import Spinner from '../components/Spinner'
import MeasurementChart from '../components/MeasurementChart'
import { formatInterval, secondsToDHMS, dhmsToSeconds } from '../utils/formatInterval'
import type { Metric, SensorReading, Status } from '../types'

const STATUSES: Status[] = ['ONLINE', 'OFFLINE']

type Period = '24h' | '7d' | '30d' | 'custom'

function timeAgo(iso: string | null): string {
  if (!iso) return '—'
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60_000)
  if (m < 1) return 'just now'
  if (m < 60) return `${m}m ago`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h ago`
  return `${Math.floor(h / 24)}d ago`
}

function periodBounds(period: Period, customFrom: string, customTo: string): { from: string; to: string } {
  const now = new Date()
  const to = period === 'custom' && customTo ? new Date(customTo).toISOString() : now.toISOString()
  if (period === '24h') return { from: new Date(now.getTime() - 86_400_000).toISOString(), to }
  if (period === '7d')  return { from: new Date(now.getTime() - 7 * 86_400_000).toISOString(), to }
  if (period === 'custom' && customFrom) return { from: new Date(customFrom).toISOString(), to }
  return { from: new Date(now.getTime() - 30 * 86_400_000).toISOString(), to }
}


export default function StationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const stationId = Number(id)
  const { auth } = useAuth()
  const queryClient = useQueryClient()

  const canEdit = auth?.role === 'ADMIN' || auth?.role === 'OPERATOR'

  const [editingInterval, setEditingInterval] = useState<number | null>(null)
  const [intervalInput, setIntervalInput] = useState('')

  const [period, setPeriod] = useState<Period>('30d')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo, setCustomTo] = useState('')

  const { data: overview, isLoading: loadingOverview } = useQuery({
    queryKey: ['station-overview', stationId],
    queryFn: () => getStationOverview(stationId),
    refetchInterval: 30_000,
  })

  const { data: nodes, isLoading: loadingNodes } = useQuery({
    queryKey: ['nodes', stationId],
    queryFn: () => getNodesByStation(stationId),
    refetchInterval: 30_000,
  })

  const { data: unassigned } = useQuery({
    queryKey: ['nodes', 'unassigned'],
    queryFn: getUnassignedNodes,
    enabled: canEdit,
    refetchInterval: 30_000,
  })

  const claimMutation = useMutation({
    mutationFn: (nodeId: number) => assignNodeToStation(nodeId, stationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['nodes', stationId] })
      queryClient.invalidateQueries({ queryKey: ['nodes', 'unassigned'] })
      queryClient.invalidateQueries({ queryKey: ['station-overview', stationId] })
    },
  })

  const intervalMutation = useMutation({
    mutationFn: ({ nodeId, seconds }: { nodeId: number; seconds: number }) =>
      setNodeInterval(nodeId, seconds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['nodes', stationId] })
      setEditingInterval(null)
      setIntervalInput('')
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ nodeId, status }: { nodeId: number; status: Status }) =>
      updateNodeStatus(nodeId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['nodes', stationId] })
      queryClient.invalidateQueries({ queryKey: ['station-overview', stationId] })
    },
  })

  const commandMutation = useMutation({
    mutationFn: ({ nodeId, command }: { nodeId: number; command: 'REQUEST_READINGS' | 'REBOOT' }) =>
      issueNodeCommand(nodeId, command),
  })

  const { from: chartFrom, to: chartTo } = useMemo(
    () => periodBounds(period, customFrom, customTo),
    [period, customFrom, customTo]
  )

  const nodeMetrics = useMemo(() => {
    if (!nodes) return []
    const byNode = new Map<number, { metric: Metric; sensorId: number }[]>()
    if (overview) {
      const seen = new Set<string>()
      for (const r of overview.readings) {
        const key = `${r.sensorId}-${r.metric}`
        if (!seen.has(key)) {
          seen.add(key)
          if (!byNode.has(r.nodeId)) byNode.set(r.nodeId, [])
          byNode.get(r.nodeId)!.push({ metric: r.metric, sensorId: r.sensorId })
        }
      }
    }
    return nodes.map(node => ({ node, metrics: byNode.get(node.id) ?? [] }))
  }, [overview, nodes])

  // Latest live reading per metric, keyed by node for lookup inside each node card.
  const liveByNode = useMemo(() => {
    const map = new Map<number, { readings: SensorReading[]; latestAt: string | null }>()
    if (!overview) return map
    const byNode = new Map<number, Map<string, SensorReading>>()
    for (const r of overview.readings) {
      let byMetric = byNode.get(r.nodeId)
      if (!byMetric) { byMetric = new Map(); byNode.set(r.nodeId, byMetric) }
      const existing = byMetric.get(r.metric)
      if (!existing || new Date(r.measuredAt).getTime() > new Date(existing.measuredAt).getTime()) {
        byMetric.set(r.metric, r)
      }
    }
    for (const [nodeId, byMetric] of byNode) {
      const readings = Array.from(byMetric.values()).sort((a, b) => a.metric.localeCompare(b.metric))
      const latestAt = readings.reduce<string | null>(
        (acc, r) => (!acc || new Date(r.measuredAt) > new Date(acc) ? r.measuredAt : acc),
        null
      )
      map.set(nodeId, { readings, latestAt })
    }
    return map
  }, [overview])

  const startEdit = (nodeId: number, current: number) => {
    setEditingInterval(nodeId)
    setIntervalInput(secondsToDHMS(current))
  }

  const submitInterval = (nodeId: number) => {
    const seconds = dhmsToSeconds(intervalInput)
    if (seconds === null || seconds < 10 || seconds > 86400) return
    intervalMutation.mutate({ nodeId, seconds })
  }

  return (
    <div className="px-8 py-7">
      <Link to="/" className="mb-5 inline-flex items-center gap-1 text-xs text-zinc-400 hover:text-zinc-700 transition-colors">
        ← Dashboard
      </Link>

      {(loadingOverview || loadingNodes) && (
        <div className="flex items-center gap-2 text-xs text-zinc-500 mt-4">
          <Spinner /> Loading...
        </div>
      )}

      {overview && (
        <>
          <div className="mb-6 border-b border-zinc-200 pb-4">
            <div className="flex items-start justify-between">
              <div>
                <h1 className="text-sm font-semibold uppercase tracking-widest text-zinc-900">
                  {overview.stationName}
                </h1>
                <p className="mt-0.5 text-xs text-zinc-500">Weather station</p>
              </div>
              <StatusBadge status={overview.status} />
            </div>

            <div className="mt-4 grid grid-cols-3 gap-px bg-zinc-200">
              {[
                { label: 'Nodes',  value: String(overview.nodeCount) },
                { label: 'Online', value: String(overview.onlineNodes) },
                { label: 'Status', value: overview.status },
              ].map(item => (
                <div key={item.label} className="bg-white px-4 py-3">
                  <p className="text-xs text-zinc-400">{item.label}</p>
                  <p className="mt-0.5 font-mono text-xs text-zinc-700">{item.value}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Unassigned nodes — at top so operators can claim immediately */}
          {canEdit && unassigned && unassigned.length > 0 && (
            <div className="mb-6">
              <h2 className="mb-3 text-xs font-medium uppercase tracking-widest text-zinc-500">
                Unassigned Nodes
              </h2>
              <div className="border border-zinc-200">
                <div className="grid grid-cols-3 border-b border-zinc-200 bg-zinc-50 px-4 py-2">
                  {['Serial', 'Last Seen', ''].map(h => (
                    <span key={h} className="text-xs font-medium uppercase tracking-widest text-zinc-400">{h}</span>
                  ))}
                </div>
                {unassigned.map((node, i) => (
                  <div
                    key={node.id}
                    className={`grid grid-cols-3 items-center px-4 py-3 ${i !== unassigned.length - 1 ? 'border-b border-zinc-200' : ''}`}
                  >
                    <span className="font-mono text-xs text-zinc-800">{node.serialNumber}</span>
                    <span className="font-mono text-xs text-zinc-500">{node.lastSeen ? new Date(node.lastSeen).toLocaleString() : '—'}</span>
                    <button
                      onClick={() => claimMutation.mutate(node.id)}
                      disabled={claimMutation.isPending}
                      className="justify-self-end font-mono text-[10px] text-emerald-600 hover:text-emerald-500 disabled:opacity-40 transition-colors"
                    >
                      claim →
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Nodes + their charts */}
          <div>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-xs font-medium uppercase tracking-widest text-zinc-500">Nodes</h2>
              <div className="flex items-center gap-1">
                {(['24h', '7d', '30d'] as Period[]).map(p => (
                  <button
                    key={p}
                    onClick={() => setPeriod(p)}
                    className={`px-2 py-1 font-mono text-[10px] border transition-colors ${
                      period === p
                        ? 'border-emerald-500/50 text-emerald-600'
                        : 'border-zinc-300 bg-zinc-50 text-zinc-600 hover:bg-zinc-100 hover:border-zinc-400 hover:text-zinc-700'
                    }`}
                  >
                    {p}
                  </button>
                ))}
                <button
                  onClick={() => setPeriod('custom')}
                  className={`px-2 py-1 font-mono text-[10px] border transition-colors ${
                    period === 'custom'
                      ? 'border-emerald-500/50 text-emerald-600'
                      : 'border-zinc-300 bg-zinc-50 text-zinc-600 hover:bg-zinc-100 hover:border-zinc-400 hover:text-zinc-700'
                  }`}
                >
                  custom
                </button>
              </div>
            </div>

            {period === 'custom' && (
              <div className="mb-4 flex gap-2">
                <input
                  type="datetime-local"
                  value={customFrom}
                  onChange={e => setCustomFrom(e.target.value)}
                  className="bg-white border border-zinc-300 text-xs text-zinc-700 px-2 py-1.5 font-mono focus:outline-none focus:border-emerald-500"
                />
                <span className="text-xs text-zinc-400 self-center">→</span>
                <input
                  type="datetime-local"
                  value={customTo}
                  onChange={e => setCustomTo(e.target.value)}
                  className="bg-white border border-zinc-300 text-xs text-zinc-700 px-2 py-1.5 font-mono focus:outline-none focus:border-emerald-500"
                />
              </div>
            )}

            {nodes && nodes.length === 0 && (
              <p className="text-xs text-zinc-500">No nodes registered yet. Power on the hardware to auto-register.</p>
            )}

            <div className="space-y-6">
              {nodeMetrics.map(({ node, metrics }) => {
                const live = liveByNode.get(node.id)
                return (
                <div key={node.id} className="border border-zinc-200">
                  {/* Node info row */}
                  <div className={`grid items-center bg-zinc-50 px-4 py-3 ${canEdit ? 'grid-cols-4' : 'grid-cols-3'}`}>
                    <span className="font-mono text-xs text-zinc-800">{node.displayName ?? node.serialNumber}</span>
                    {canEdit ? (
                      <select
                        value={node.status}
                        onChange={e => statusMutation.mutate({ nodeId: node.id, status: e.target.value as Status })}
                        disabled={statusMutation.isPending && statusMutation.variables?.nodeId === node.id}
                        className={`justify-self-start border px-2 py-1 font-mono text-xs focus:outline-none transition-colors disabled:opacity-50 ${
                          node.status === 'ONLINE'
                            ? 'border-emerald-500/50 bg-emerald-50 text-emerald-700'
                            : 'border-zinc-300 bg-zinc-50 text-zinc-500'
                        }`}
                      >
                        {STATUSES.map(s => (
                          <option key={s} value={s}>{s === 'ONLINE' ? '● ONLINE' : '○ OFFLINE'}</option>
                        ))}
                      </select>
                    ) : (
                      <StatusBadge status={node.status} />
                    )}
                    <span className="font-mono text-xs text-zinc-500">{timeAgo(node.lastSeen)}</span>
                    {canEdit && (
                      <div className="flex items-center gap-2">
                        {editingInterval === node.id ? (
                          <>
                            <input
                              type="text"
                              value={intervalInput}
                              onChange={e => setIntervalInput(e.target.value)}
                              placeholder="d:h:m:s"
                              title="days:hours:minutes:seconds (10s – 1d)"
                              className="w-24 bg-white border border-zinc-300 px-1.5 py-0.5 font-mono text-xs text-zinc-800 focus:outline-none focus:border-emerald-500"
                              onKeyDown={e => { if (e.key === 'Enter') submitInterval(node.id) }}
                              autoFocus
                            />
                            <button
                              onClick={() => submitInterval(node.id)}
                              disabled={intervalMutation.isPending}
                              className="font-mono text-[10px] text-emerald-600 hover:text-emerald-500 disabled:opacity-40"
                            >
                              set
                            </button>
                            <button
                              onClick={() => setEditingInterval(null)}
                              className="font-mono text-[10px] text-zinc-500 hover:text-zinc-800"
                            >
                              ✕
                            </button>
                          </>
                        ) : (
                          <>
                            <span className="font-mono text-xs text-zinc-500">
                              {formatInterval(node.reportingIntervalSeconds)}
                            </span>
                            <button
                              onClick={() => startEdit(node.id, node.reportingIntervalSeconds)}
                              className="border border-zinc-300 bg-zinc-50 px-2 py-0.5 font-mono text-[10px] text-zinc-600 hover:bg-zinc-100 hover:border-zinc-400 hover:text-zinc-800 transition-colors"
                            >
                              edit
                            </button>
                          </>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Live readings — latest value per metric */}
                  {live && live.readings.length > 0 && (
                    <div className="border-t border-zinc-200 px-4 py-3">
                      <div className="mb-2 flex items-center justify-between">
                        <p className="text-[10px] font-medium uppercase tracking-widest text-zinc-400">Latest</p>
                        <span className="font-mono text-[10px] text-zinc-400">{timeAgo(live.latestAt)}</span>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {live.readings.map(r => (
                          <div key={`${r.sensorId}-${r.metric}`} className="border border-zinc-200 bg-zinc-50 px-3 py-2">
                            <p className="text-[10px] font-medium uppercase tracking-widest text-zinc-400">{r.metric}</p>
                            <p className="mt-0.5 font-mono text-sm text-zinc-800">{Number(r.value).toFixed(1)}</p>
                            <p className="mt-0.5 font-mono text-[10px] text-zinc-400">{timeAgo(r.measuredAt)}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Node commands */}
                  {canEdit && (
                    <div className="border-t border-zinc-200 px-4 py-3">
                      <p className="mb-2 text-[10px] font-medium uppercase tracking-widest text-zinc-400">Commands</p>
                      <div className="flex items-center gap-2">
                        {(['REQUEST_READINGS', 'REBOOT'] as const).map(cmd => (
                          <button
                            key={cmd}
                            onClick={() => commandMutation.mutate({ nodeId: node.id, command: cmd })}
                            disabled={commandMutation.isPending && commandMutation.variables?.nodeId === node.id}
                            className="border border-zinc-300 bg-zinc-50 px-2 py-1 font-mono text-[10px] text-zinc-600 hover:bg-zinc-100 hover:border-zinc-400 hover:text-zinc-800 transition-colors disabled:opacity-40"
                          >
                            {cmd === 'REQUEST_READINGS' ? 'get readings' : 'reboot'}
                          </button>
                        ))}
                        {commandMutation.isSuccess && commandMutation.variables?.nodeId === node.id && (
                          <span className="font-mono text-[10px] text-emerald-600">queued</span>
                        )}
                      </div>
                    </div>
                  )}

                  {/* Historical charts */}
                  {metrics.length > 0 ? (
                    <div className="border-t border-zinc-200">
                      <p className="px-4 pt-3 text-[10px] font-medium uppercase tracking-widest text-zinc-400">History</p>
                      <div className="divide-y divide-zinc-200">
                        {metrics.map(({ metric, sensorId }) => (
                          <div key={`${sensorId}-${metric}`} className="p-4">
                            <p className="mb-3 text-[10px] font-medium uppercase tracking-widest text-zinc-500">
                              {metric}
                            </p>
                            <MeasurementChart
                              sensorId={sensorId}
                              metric={metric}
                              from={chartFrom}
                              to={chartTo}
                            />
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="border-t border-zinc-200 px-4 py-3 text-xs text-zinc-400">No readings yet.</p>
                  )}
                </div>
                )
              })}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
