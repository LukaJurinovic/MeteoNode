import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateStation } from '../services/stationService'
import type { WeatherStationDTO } from '../types'
import Spinner from './Spinner'

interface Props {
  station: WeatherStationDTO
  onClose: () => void
}

export default function EditStationModal({ station, onClose }: Props) {
  const queryClient = useQueryClient()

  const [name, setName] = useState(station.name)
  const [locationLat, setLocationLat] = useState(station.locationLat != null ? String(station.locationLat) : '')
  const [locationLon, setLocationLon] = useState(station.locationLon != null ? String(station.locationLon) : '')
  const [error, setError] = useState('')

  const mutation = useMutation({
    mutationFn: () => updateStation(station.id, {
      name,
      locationLat: locationLat !== '' ? parseFloat(locationLat) : null,
      locationLon: locationLon !== '' ? parseFloat(locationLon) : null,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] })
      onClose()
    },
    onError: () => setError('Failed to update station.'),
  })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    mutation.mutate()
  }

  const inputClass = 'w-full border border-zinc-300 bg-zinc-50 px-3 py-2 text-sm text-zinc-900 outline-none placeholder-zinc-400 focus:border-emerald-500/50 transition-colors font-mono'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
      <div className="w-full max-w-sm border border-zinc-200 bg-white p-6">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-xs font-medium uppercase tracking-widest text-zinc-600">Edit Station</h2>
          <button onClick={onClose} className="text-zinc-400 hover:text-zinc-700 transition-colors text-lg leading-none">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1.5 block text-xs text-zinc-500">Name</label>
            <input
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              required
              className={inputClass}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1.5 block text-xs text-zinc-500">Latitude</label>
              <input
                type="number"
                step="any"
                min={-90}
                max={90}
                value={locationLat}
                onChange={e => setLocationLat(e.target.value)}
                placeholder="—"
                className={inputClass}
              />
            </div>
            <div>
              <label className="mb-1.5 block text-xs text-zinc-500">Longitude</label>
              <input
                type="number"
                step="any"
                min={-180}
                max={180}
                value={locationLon}
                onChange={e => setLocationLon(e.target.value)}
                placeholder="—"
                className={inputClass}
              />
            </div>
          </div>

          {error && <p className="text-xs text-red-500">{error}</p>}

          <div className="flex justify-end gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-1.5 text-xs text-zinc-600 bg-zinc-100 hover:bg-zinc-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={mutation.isPending}
              className="flex items-center gap-2 bg-emerald-500 px-4 py-1.5 text-xs font-medium text-white hover:bg-emerald-600 transition-colors disabled:opacity-40"
            >
              {mutation.isPending && <Spinner />}
              {mutation.isPending ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
