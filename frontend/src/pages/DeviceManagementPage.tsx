import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getAllNodes, getStations, assignNodeToStation, updateNode,
  updateNodeStatus, deleteNode, getSensorsByNode,
  activateSensor, deactivateSensor, deleteSensor, issueNodeCommand,
  unassignNodeFromStation, setNodeInterval,
} from '../services/stationService'
import type { Status } from '../types'
import { useAuth } from '../contexts/AuthContext'
import Spinner from '../components/Spinner'
import { formatInterval, secondsToDHMS, dhmsToSeconds } from '../utils/formatInterval'

const STATUSES: Status[] = ['ONLINE', 'OFFLINE']

function formatLastSeen(ts: string | null): string {
  if (!ts) return '—'
  return new Date(ts).toLocaleString()
}

export default function DeviceManagementPage() {
  const { auth } = useAuth()
  const queryClient = useQueryClient()
  const isAdmin = auth?.role === 'ADMIN'
  const canIssueCommands = isAdmin || auth?.role === 'OPERATOR'

  const [expandedNodeId, setExpandedNodeId] = useState<number | null>(null)
  const [editingNodeId, setEditingNodeId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [assigningNodeId, setAssigningNodeId] = useState<number | null>(null)
  const [assignStationId, setAssignStationId] = useState('')
  const [editingIntervalId, setEditingIntervalId] = useState<number | null>(null)
  const [intervalInput, setIntervalInput] = useState('')

  const { data: nodes, isLoading } = useQuery({ queryKey: ['nodes'], queryFn: getAllNodes })
  const { data: stations } = useQuery({ queryKey: ['stations'], queryFn: getStations })
  const { data: sensors, isLoading: loadingSensors } = useQuery({
    queryKey: ['sensors', expandedNodeId],
    queryFn: () => getSensorsByNode(expandedNodeId!),
    enabled: expandedNodeId !== null,
  })

  const renameMutation = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => updateNode(id, name),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['nodes'] }); setEditingNodeId(null) },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: Status }) => updateNodeStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['nodes'] }),
  })

  const assignMutation = useMutation({
    mutationFn: ({ nodeId, stationId }: { nodeId: number; stationId: number }) =>
      assignNodeToStation(nodeId, stationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['nodes'] })
      setAssigningNodeId(null)
      setAssignStationId('')
    },
  })

  const deleteNodeMutation = useMutation({
    mutationFn: deleteNode,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['nodes'] }),
  })

  const unassignMutation = useMutation({
    mutationFn: (nodeId: number) => unassignNodeFromStation(nodeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['nodes'] }),
  })

  const intervalMutation = useMutation({
    mutationFn: ({ nodeId, seconds }: { nodeId: number; seconds: number }) =>
      setNodeInterval(nodeId, seconds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['nodes'] })
      setEditingIntervalId(null)
      setIntervalInput('')
    },
  })

  function submitInterval(nodeId: number) {
    const seconds = dhmsToSeconds(intervalInput)
    if (seconds === null || seconds < 10 || seconds > 86400) return
    intervalMutation.mutate({ nodeId, seconds })
  }

  const activateMutation = useMutation({
    mutationFn: ({ sensorId }: { sensorId: number; nodeId: number }) => activateSensor(sensorId),
    onSuccess: (_, { nodeId }) => queryClient.invalidateQueries({ queryKey: ['sensors', nodeId] }),
  })

  const deactivateMutation = useMutation({
    mutationFn: ({ sensorId }: { sensorId: number; nodeId: number }) => deactivateSensor(sensorId),
    onSuccess: (_, { nodeId }) => queryClient.invalidateQueries({ queryKey: ['sensors', nodeId] }),
  })

  const deleteSensorMutation = useMutation({
    mutationFn: ({ sensorId }: { sensorId: number; nodeId: number }) => deleteSensor(sensorId),
    onSuccess: (_, { nodeId }) => queryClient.invalidateQueries({ queryKey: ['sensors', nodeId] }),
  })

  const commandMutation = useMutation({
    mutationFn: ({ nodeId, command }: { nodeId: number; command: 'REQUEST_READINGS' | 'REBOOT' }) =>
      issueNodeCommand(nodeId, command),
  })

  const unassignedCount = nodes?.filter(n => !n.stationId).length ?? 0
  const stationName = (id: number | null) =>
    id != null ? (stations?.find(s => s.id === id)?.name ?? `#${id}`) : null

  const colHeader = 'text-xs font-medium uppercase tracking-widest text-zinc-400'
  const btnGhost = 'border border-zinc-300 bg-zinc-50 px-2 py-1 font-mono text-[10px] text-zinc-600 hover:bg-zinc-100 hover:border-zinc-400 hover:text-zinc-800 transition-colors'
  const btnPrimary = 'border border-emerald-500/50 bg-emerald-50 px-2 py-1 font-mono text-[10px] text-emerald-600 hover:bg-emerald-100 transition-colors'
  const btnDanger = 'border border-red-300 bg-red-50 px-2 py-1 font-mono text-[10px] text-red-500 hover:bg-red-100 hover:border-red-400 transition-colors'
  const selectClass = 'bg-white border border-zinc-300 text-xs text-zinc-700 px-2 py-1 font-mono focus:outline-none focus:border-emerald-500'

  return (
    <div className="px-8 py-7">
      <div className="mb-6 border-b border-zinc-200 pb-4">
        <h1 className="text-sm font-semibold uppercase tracking-widest text-zinc-900">Device Management</h1>
        <p className="mt-0.5 text-xs text-zinc-500">Manage nodes and sensors across all stations</p>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 text-xs text-zinc-500">
          <Spinner /> Loading devices…
        </div>
      )}

      {!isLoading && unassignedCount > 0 && (
        <div className="mb-4 border border-amber-400/50 bg-amber-50 px-4 py-2.5 text-xs text-amber-700">
          {unassignedCount} unassigned device{unassignedCount !== 1 ? 's' : ''} — assign them to a station below.
        </div>
      )}

      {nodes && nodes.length === 0 && (
        <p className="text-xs text-zinc-400">No devices registered yet.</p>
      )}

      {nodes && nodes.length > 0 && (
        <div className="border border-zinc-200">
          <div className="grid grid-cols-6 border-b border-zinc-200 bg-zinc-50 px-4 py-2">
            {['Name / Serial', 'Station', 'Status', 'Last Seen', 'Interval', ''].map((h, i) => (
              <span key={i} className={colHeader}>{h}</span>
            ))}
          </div>

          {nodes.map(node => {
            const isExpanded = expandedNodeId === node.id
            const isEditing = editingNodeId === node.id
            const isAssigning = assigningNodeId === node.id

            return (
              <div key={node.id} className="border-b border-zinc-200 last:border-b-0">
                <div className="grid grid-cols-6 items-center px-4 py-3">

                  {/* Name / Serial */}
                  <div className="flex items-center gap-2 min-w-0 pr-2">
                    {isEditing ? (
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <input
                          autoFocus
                          value={editName}
                          onChange={e => setEditName(e.target.value)}
                          className="w-28 border border-zinc-300 bg-white px-2 py-0.5 font-mono text-xs text-zinc-900 outline-none focus:border-emerald-500"
                        />
                        <button
                          onClick={() => renameMutation.mutate({ id: node.id, name: editName })}
                          disabled={renameMutation.isPending}
                          className="font-mono text-[10px] text-emerald-600 hover:text-emerald-500 transition-colors disabled:opacity-40"
                        >save</button>
                        <button onClick={() => setEditingNodeId(null)} className={btnGhost}>cancel</button>
                      </div>
                    ) : (
                      <>
                        <span className="font-mono text-xs text-zinc-700 truncate">
                          {node.displayName ?? node.serialNumber}
                        </span>
                        <button
                          onClick={() => { setEditingNodeId(node.id); setEditName(node.displayName ?? '') }}
                          className="shrink-0 font-mono text-[10px] text-zinc-500 hover:text-zinc-800 transition-colors"
                        >✎</button>
                      </>
                    )}
                  </div>

                  {/* Station */}
                  <div>
                    {node.stationId ? (
                      <div className="flex flex-col items-start gap-1.5">
                        <span className="font-mono text-xs text-zinc-600">{stationName(node.stationId)}</span>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => { setAssigningNodeId(isAssigning ? null : node.id); setAssignStationId('') }}
                            className={btnGhost}
                          >reassign</button>
                          <button
                            onClick={() => {
                              if (window.confirm(`Unassign "${node.displayName ?? node.serialNumber}" from its station?`)) {
                                unassignMutation.mutate(node.id)
                              }
                            }}
                            className={btnDanger}
                          >unassign</button>
                        </div>
                      </div>
                    ) : (
                      <button
                        onClick={() => { setAssigningNodeId(isAssigning ? null : node.id); setAssignStationId('') }}
                        className="border border-amber-400/70 bg-amber-50 px-2 py-1 font-mono text-[10px] text-amber-600 hover:bg-amber-100 transition-colors"
                      >+ assign</button>
                    )}
                  </div>

                  {/* Status */}
                  <div>
                    <select
                      value={node.status}
                      onChange={e => statusMutation.mutate({ id: node.id, status: e.target.value as Status })}
                      disabled={statusMutation.isPending}
                      className={`border px-2 py-1 font-mono text-xs focus:outline-none transition-colors disabled:opacity-50 ${
                        node.status === 'ONLINE'
                          ? 'border-emerald-500/50 bg-emerald-50 text-emerald-700'
                          : 'border-zinc-300 bg-zinc-50 text-zinc-500'
                      }`}
                    >
                      {STATUSES.map(s => (
                        <option key={s} value={s}>{s === 'ONLINE' ? '● ONLINE' : '○ OFFLINE'}</option>
                      ))}
                    </select>
                  </div>

                  {/* Last Seen */}
                  <span className="font-mono text-xs text-zinc-500">{formatLastSeen(node.lastSeen)}</span>

                  {/* Interval */}
                  <div className="flex items-center gap-2">
                    {editingIntervalId === node.id ? (
                      <>
                        <input
                          type="text"
                          value={intervalInput}
                          onChange={e => setIntervalInput(e.target.value)}
                          placeholder="d:h:m:s"
                          title="days:hours:minutes:seconds (10s – 1d)"
                          autoFocus
                          className="w-24 bg-white border border-zinc-300 px-1.5 py-0.5 font-mono text-xs text-zinc-800 focus:outline-none focus:border-emerald-500"
                          onKeyDown={e => { if (e.key === 'Enter') submitInterval(node.id) }}
                        />
                        <button
                          onClick={() => submitInterval(node.id)}
                          disabled={intervalMutation.isPending}
                          className="font-mono text-[10px] text-emerald-600 hover:text-emerald-500 disabled:opacity-40"
                        >set</button>
                        <button onClick={() => setEditingIntervalId(null)} className="font-mono text-[10px] text-zinc-500 hover:text-zinc-800">✕</button>
                      </>
                    ) : (
                      <>
                        <span className="font-mono text-xs text-zinc-500">{formatInterval(node.reportingIntervalSeconds)}</span>
                        {canIssueCommands && (
                          <button
                            onClick={() => { setEditingIntervalId(node.id); setIntervalInput(secondsToDHMS(node.reportingIntervalSeconds)) }}
                            className={btnGhost}
                            title="Edit reporting interval"
                          >⏱ edit</button>
                        )}
                      </>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setExpandedNodeId(isExpanded ? null : node.id)}
                      className={btnGhost}
                    >
                      {isExpanded ? '▾ sensors' : '▸ sensors'}
                    </button>
                    {canIssueCommands && (
                      <button
                        onClick={() => {
                          if (window.confirm(
                            `Delete node "${node.displayName ?? node.serialNumber}"? This will also delete all its sensors and measurements.`
                          )) {
                            if (isExpanded) setExpandedNodeId(null)
                            deleteNodeMutation.mutate(node.id)
                          }
                        }}
                        className={btnDanger}
                      >delete</button>
                    )}
                  </div>
                </div>

                {/* Assign/reassign sub-panel */}
                {isAssigning && (
                  <div className="border-t border-zinc-200 bg-zinc-50 px-8 py-3">
                    <p className="mb-2 text-[10px] font-medium uppercase tracking-widest text-zinc-400">
                      {node.stationId ? 'Reassign to station' : 'Assign to station'}
                    </p>
                    <div className="flex items-center gap-2">
                      <select
                        value={assignStationId}
                        onChange={e => setAssignStationId(e.target.value)}
                        className={selectClass}
                      >
                        <option value="">Select station…</option>
                        {stations?.filter(s => s.id !== node.stationId).map(s => (
                          <option key={s.id} value={s.id}>{s.name}</option>
                        ))}
                      </select>
                      <button
                        onClick={() => {
                          if (assignStationId) assignMutation.mutate({ nodeId: node.id, stationId: Number(assignStationId) })
                        }}
                        disabled={!assignStationId || assignMutation.isPending}
                        className={`${btnPrimary} disabled:opacity-40`}
                      >assign</button>
                      <button onClick={() => setAssigningNodeId(null)} className={btnGhost}>cancel</button>
                    </div>
                  </div>
                )}

                {/* Expanded sensor panel */}
                {isExpanded && (
                  <div className="border-t border-zinc-200 bg-zinc-50 px-8 py-3">
                    <p className="mb-2 text-[10px] font-medium uppercase tracking-widest text-zinc-400">Sensors</p>

                    {loadingSensors && (
                      <div className="flex items-center gap-2 text-xs text-zinc-400">
                        <Spinner /> Loading…
                      </div>
                    )}

                    {sensors && sensors.length === 0 && (
                      <p className="text-xs text-zinc-400">No sensors registered for this node.</p>
                    )}

                    {sensors && sensors.length > 0 && (
                      <div className="flex flex-col gap-2">
                        {sensors.map(sensor => (
                          <div key={sensor.id} className="flex items-center gap-3">
                            <span className="w-28 font-mono text-xs text-zinc-600">{sensor.sensorType}</span>
                            <span className={`w-14 font-mono text-[10px] ${sensor.isActive ? 'text-emerald-600' : 'text-zinc-400'}`}>
                              {sensor.isActive ? 'active' : 'inactive'}
                            </span>
                            <button
                              onClick={() =>
                                sensor.isActive
                                  ? deactivateMutation.mutate({ sensorId: sensor.id, nodeId: node.id })
                                  : activateMutation.mutate({ sensorId: sensor.id, nodeId: node.id })
                              }
                              disabled={activateMutation.isPending || deactivateMutation.isPending}
                              className={`${btnGhost} disabled:opacity-40`}
                            >
                              {sensor.isActive ? 'deactivate' : 'activate'}
                            </button>
                            {canIssueCommands && (
                              <button
                                onClick={() => {
                                  if (window.confirm(`Delete sensor ${sensor.sensorType} #${sensor.id}?`)) {
                                    deleteSensorMutation.mutate({ sensorId: sensor.id, nodeId: node.id })
                                  }
                                }}
                                className={btnDanger}
                              >delete</button>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                    {canIssueCommands && (
                      <>
                        <p className="mt-4 mb-2 text-[10px] font-medium uppercase tracking-widest text-zinc-400">Commands</p>
                        <div className="flex gap-2">
                          {(['REQUEST_READINGS', 'REBOOT'] as const).map(cmd => (
                            <button
                              key={cmd}
                              onClick={() => commandMutation.mutate({ nodeId: node.id, command: cmd })}
                              disabled={commandMutation.isPending}
                              className={`${btnGhost} disabled:opacity-40`}
                            >
                              {cmd === 'REQUEST_READINGS' ? 'get readings' : cmd.toLowerCase()}
                            </button>
                          ))}
                        </div>
                        {commandMutation.isSuccess && (
                          <p className="mt-1 text-[10px] text-emerald-600">Command queued.</p>
                        )}
                      </>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
