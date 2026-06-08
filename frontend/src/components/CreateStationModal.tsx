import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createStation } from '../services/stationService'

interface Props {
  onClose: () => void
}

export default function CreateStationModal({ onClose }: Props) {
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [lat, setLat] = useState('')
  const [lon, setLon] = useState('')
  const [error, setError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => createStation({
      name,
      locationLat: lat ? parseFloat(lat) : null,
      locationLon: lon ? parseFloat(lon) : null,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] })
      onClose()
    },
    onError: () => setError('Failed to create station.'),
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md border border-zinc-200 bg-white p-6">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-widest text-zinc-900">New Station</h2>
          <button onClick={onClose} className="text-zinc-400 hover:text-zinc-700 transition-colors text-xs">✕</button>
        </div>

        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs text-zinc-500">Name *</label>
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Main Station"
              className="w-full bg-zinc-50 border border-zinc-300 px-3 py-2 text-xs text-zinc-900 font-mono placeholder-zinc-400 focus:outline-none focus:border-emerald-500"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs text-zinc-500">Latitude</label>
              <input
                value={lat}
                onChange={e => setLat(e.target.value)}
                placeholder="45.8150"
                type="number"
                step="any"
                className="w-full bg-zinc-50 border border-zinc-300 px-3 py-2 text-xs text-zinc-900 font-mono placeholder-zinc-400 focus:outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-zinc-500">Longitude</label>
              <input
                value={lon}
                onChange={e => setLon(e.target.value)}
                placeholder="15.9820"
                type="number"
                step="any"
                className="w-full bg-zinc-50 border border-zinc-300 px-3 py-2 text-xs text-zinc-900 font-mono placeholder-zinc-400 focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>
        </div>

        {error && <p className="mt-3 text-xs text-red-500">{error}</p>}

        <div className="mt-5 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs text-zinc-600 bg-zinc-100 hover:bg-zinc-200 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => mutation.mutate()}
            disabled={!name || mutation.isPending}
            className="bg-emerald-500 px-4 py-2 text-xs font-medium text-white hover:bg-emerald-600 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {mutation.isPending ? 'Creating…' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  )
}
