import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getAllNodes, getStations } from '../services/stationService'
import StatusBadge from '../components/StatusBadge'
import Spinner from '../components/Spinner'

export default function SystemPage() {
  const { data: nodes, isLoading: loadingNodes } = useQuery({
    queryKey: ['nodes'],
    queryFn: getAllNodes,
    refetchInterval: 30_000,
  })

  const { data: stations, isLoading: loadingStations } = useQuery({
    queryKey: ['stations'],
    queryFn: getStations,
    refetchInterval: 30_000,
  })

  const totalNodes = nodes?.length ?? 0
  const onlineNodes = nodes?.filter(n => n.status === 'ONLINE').length ?? 0
  const offlineNodes = totalNodes - onlineNodes
  const unassignedNodes = nodes?.filter(n => !n.stationId) ?? []
  const totalStations = stations?.length ?? 0

  const stationStats = useMemo(() => {
    if (!stations || !nodes) return []
    return stations.map(s => {
      const stNodes = nodes.filter(n => n.stationId === s.id)
      const online = stNodes.filter(n => n.status === 'ONLINE').length
      return { station: s, total: stNodes.length, online, offline: stNodes.length - online }
    })
  }, [stations, nodes])

  const isLoading = loadingNodes || loadingStations

  return (
    <div className="px-8 py-7">
      <div className="mb-6 border-b border-zinc-200 pb-4">
        <h1 className="text-sm font-semibold uppercase tracking-widest text-zinc-900">System</h1>
        <p className="mt-0.5 text-xs text-zinc-500">Infrastructure overview</p>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 text-xs text-zinc-500">
          <Spinner /> Loading…
        </div>
      )}

      {nodes && stations && (
        <>
          {/* Stat strip */}
          <div className="mb-8 grid grid-cols-3 gap-px bg-zinc-200">
            <div className="bg-white px-5 py-4">
              <p className="text-2xl font-semibold text-zinc-900">{totalStations}</p>
              <p className="mt-1 text-xs text-zinc-500 uppercase tracking-widest">Stations</p>
            </div>
            <div className="bg-white px-5 py-4">
              <p className="text-2xl font-semibold text-zinc-900">{totalNodes}</p>
              <p className="mt-1 text-xs text-zinc-500 uppercase tracking-widest">
                Nodes
                {onlineNodes > 0 && <span className="ml-2 text-emerald-500">{onlineNodes} online</span>}
                {offlineNodes > 0 && <span className="ml-1 text-zinc-400">{offlineNodes} offline</span>}
              </p>
            </div>
            <div className="bg-white px-5 py-4">
              <p className={`text-2xl font-semibold ${unassignedNodes.length > 0 ? 'text-amber-500' : 'text-zinc-900'}`}>
                {unassignedNodes.length}
              </p>
              <p className="mt-1 text-xs text-zinc-500 uppercase tracking-widest">Unassigned</p>
            </div>
          </div>

          {/* Per-station breakdown */}
          {stationStats.length > 0 && (
            <div className="mb-8">
              <h2 className="mb-3 text-xs font-medium uppercase tracking-widest text-zinc-500">Stations</h2>
              <div className="border border-zinc-200">
                <div className="grid grid-cols-5 border-b border-zinc-200 bg-zinc-50 px-4 py-2">
                  {['Station', 'Status', 'Total Nodes', 'Online', 'Offline'].map(h => (
                    <span key={h} className="text-xs font-medium uppercase tracking-widest text-zinc-400">{h}</span>
                  ))}
                </div>
                {stationStats.map(({ station, total, online, offline }, i) => (
                  <div
                    key={station.id}
                    className={`grid grid-cols-5 items-center px-4 py-3 ${
                      i !== stationStats.length - 1 ? 'border-b border-zinc-200' : ''
                    }`}
                  >
                    <span className="font-mono text-xs text-zinc-800">{station.name}</span>
                    <StatusBadge status={station.status} />
                    <span className="font-mono text-xs text-zinc-600">{total}</span>
                    <span className={`font-mono text-xs ${online > 0 ? 'text-emerald-500' : 'text-zinc-400'}`}>{online}</span>
                    <span className={`font-mono text-xs ${offline > 0 ? 'text-red-500' : 'text-zinc-400'}`}>{offline}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Unassigned nodes */}
          {unassignedNodes.length > 0 && (
            <div>
              <h2 className="mb-3 text-xs font-medium uppercase tracking-widest text-zinc-500">
                Unassigned Nodes
              </h2>
              <div className="border border-amber-300 bg-amber-50">
                <div className="grid grid-cols-3 border-b border-zinc-200 bg-zinc-50 px-4 py-2">
                  {['Serial / Name', 'Status', 'Last Seen'].map(h => (
                    <span key={h} className="text-xs font-medium uppercase tracking-widest text-zinc-400">{h}</span>
                  ))}
                </div>
                {unassignedNodes.map((node, i) => (
                  <div
                    key={node.id}
                    className={`grid grid-cols-3 items-center px-4 py-3 ${
                      i !== unassignedNodes.length - 1 ? 'border-b border-zinc-200' : ''
                    }`}
                  >
                    <span className="font-mono text-xs text-amber-600">{node.displayName ?? node.serialNumber}</span>
                    <StatusBadge status={node.status} />
                    <span className="font-mono text-xs text-zinc-500">
                      {node.lastSeen ? new Date(node.lastSeen).toLocaleString() : '—'}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
