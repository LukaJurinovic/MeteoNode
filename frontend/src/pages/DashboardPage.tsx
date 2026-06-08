import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getStations, updateStationStatus, deleteStation } from '../services/stationService'
import { useAuth } from '../contexts/AuthContext'
import type { WeatherStationDTO } from '../types'
import StatusBadge from '../components/StatusBadge'
import Spinner from '../components/Spinner'
import LiveClock from '../components/LiveClock'
import CreateStationModal from '../components/CreateStationModal'
import EditStationModal from '../components/EditStationModal'

export default function DashboardPage() {
  const { auth } = useAuth()
  const canEdit = auth?.role === 'ADMIN' || auth?.role === 'OPERATOR'
  const [showModal, setShowModal] = useState(false)
  const [editStation, setEditStation] = useState<WeatherStationDTO | null>(null)

  const queryClient = useQueryClient()

  const { data: stations, isLoading, isError } = useQuery({
    queryKey: ['stations'],
    queryFn: getStations,
    refetchInterval: 30_000,
  })

  const toggleStatus = useMutation({
    mutationFn: (station: WeatherStationDTO) =>
      updateStationStatus(station.id, station.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['stations'] }),
  })

  const deleteStationMutation = useMutation({
    mutationFn: (id: number) => deleteStation(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['stations'] }),
  })

  const online = stations?.filter(s => s.status === 'ONLINE').length ?? 0
  const total  = stations?.length ?? 0

  return (
    <div className="px-8 py-7">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between border-b border-zinc-200 pb-4">
        <div>
          <h1 className="text-sm font-semibold uppercase tracking-widest text-zinc-900">Dashboard</h1>
          <p className="mt-0.5 text-xs text-zinc-500">Weather station overview</p>
        </div>
        <div className="flex items-center gap-4">
          <LiveClock />
          {canEdit && (
            <button
              onClick={() => setShowModal(true)}
              className="bg-emerald-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-600 transition-colors"
            >
              + Add Station
            </button>
          )}
        </div>
      </div>

      {/* Stat strip */}
      <div className="mb-6 grid grid-cols-3 gap-px bg-zinc-200">
        {[
          { label: 'Total Stations', value: total },
          { label: 'Online',         value: online,         accent: online > 0 },
          { label: 'Offline',        value: total - online, danger: total - online > 0 },
        ].map(stat => (
          <div key={stat.label} className="bg-white px-5 py-4">
            <p className="text-xs text-zinc-500">{stat.label}</p>
            <p className={`mt-1 font-mono text-2xl font-medium ${
              stat.accent ? 'text-emerald-500' : stat.danger ? 'text-red-500' : 'text-zinc-900'
            }`}>
              {stat.value}
            </p>
          </div>
        ))}
      </div>

      {/* Stations */}
      {isLoading && (
        <div className="flex items-center gap-2 text-xs text-zinc-500">
          <Spinner /> Loading stations...
        </div>
      )}

      {isError && (
        <p className="text-xs text-red-500">Failed to load stations. Is the backend running?</p>
      )}

      {stations && stations.length === 0 && (
        <p className="text-xs text-zinc-500">No weather stations found. Add one to get started.</p>
      )}

      {stations && stations.length > 0 && (
        <div className="grid gap-px bg-zinc-200" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))' }}>
          {stations.map(station => (
            <div key={station.id} className="flex flex-col bg-white">
              {/* Header: name + status + action buttons */}
              <div className="flex items-start justify-between p-5 pb-3">
                <div className="min-w-0 flex-1 pr-2">
                  <p className="text-sm font-medium text-zinc-900 truncate">{station.name}</p>
                  <div className="mt-1.5">
                    <StatusBadge status={station.status} />
                  </div>
                </div>
                {canEdit && (
                  <div className="flex flex-col items-end gap-1 shrink-0">
                    <button
                      onClick={() => toggleStatus.mutate(station)}
                      className="border border-zinc-300 bg-zinc-50 px-2 py-1 font-mono text-[10px] text-zinc-600 hover:border-emerald-500/50 hover:bg-emerald-50 hover:text-emerald-600 transition-colors whitespace-nowrap"
                    >
                      {station.status === 'ONLINE' ? 'set offline' : 'set online'}
                    </button>
                    <div className="flex gap-1">
                      <button
                        onClick={() => setEditStation(station)}
                        className="border border-zinc-300 bg-zinc-50 px-2 py-1 font-mono text-[10px] text-zinc-600 hover:bg-zinc-100 hover:border-zinc-400 hover:text-zinc-800 transition-colors"
                      >
                        edit
                      </button>
                      <button
                        onClick={() => {
                          if (window.confirm(`Delete station "${station.name}"? All nodes, sensors and measurements will also be deleted.`)) {
                            deleteStationMutation.mutate(station.id)
                          }
                        }}
                        className="border border-red-300 bg-red-50 px-2 py-1 font-mono text-[10px] text-red-500 hover:bg-red-100 hover:border-red-400 transition-colors"
                      >
                        delete
                      </button>
                    </div>
                  </div>
                )}
              </div>
              {/* Clickable footer */}
              <Link
                to={`/stations/${station.id}`}
                className="group block px-5 pb-5 border-t border-zinc-200 hover:bg-zinc-50 transition-colors"
              >
                <div className="pt-3">
                  <span className="text-xs text-zinc-400 group-hover:text-zinc-600 transition-colors">
                    View details →
                  </span>
                </div>
              </Link>
            </div>
          ))}
        </div>
      )}

      {showModal && <CreateStationModal onClose={() => setShowModal(false)} />}
      {editStation && <EditStationModal station={editStation} onClose={() => setEditStation(null)} />}
    </div>
  )
}
