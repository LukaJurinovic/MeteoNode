import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getNotifications, markNotificationRead } from '../services/stationService'
import Spinner from '../components/Spinner'
import type { Severity } from '../types'

const severityBorderStyle: Record<Severity, string> = {
  INFO:     'border-zinc-400',
  WARNING:  'border-amber-500',
  CRITICAL: 'border-red-500',
}

const severityTextStyle: Record<Severity, string> = {
  INFO:     'text-zinc-500',
  WARNING:  'text-amber-600',
  CRITICAL: 'text-red-600',
}

export default function NotificationsPage() {
  const queryClient = useQueryClient()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['notifications'],
    queryFn: getNotifications,
    refetchInterval: 30_000,
  })

  const readMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  return (
    <div className="px-8 py-7">
      <div className="mb-6 border-b border-zinc-200 pb-4">
        <h1 className="text-sm font-semibold uppercase tracking-widest text-zinc-900">Notifications</h1>
        <p className="mt-0.5 text-xs text-zinc-500">Alarm events from sensor threshold breaches</p>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 text-xs text-zinc-500">
          <Spinner /> Loading...
        </div>
      )}

      {isError && (
        <p className="text-xs text-red-500">Failed to load notifications.</p>
      )}

      {data && data.length === 0 && (
        <p className="text-xs text-zinc-500">No alarm notifications. All sensors within thresholds.</p>
      )}

      {data && data.length > 0 && (
        <div className="space-y-px">
          {data.map(n => (
            <div
              key={n.id}
              className={`border-l-2 px-4 py-3 transition-colors ${severityBorderStyle[n.severity]} ${
                n.read ? 'bg-zinc-50' : 'bg-white'
              }`}
            >
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <p className={`text-xs font-medium ${n.read ? 'text-zinc-500' : 'text-zinc-900'}`}>
                    {n.message}
                  </p>
                  <p className="mt-0.5 font-mono text-xs text-zinc-500">
                    {n.metric} · {n.value} · sensor #{n.sensorId}
                  </p>
                </div>
                <div className="shrink-0 text-right">
                  <span className={`font-mono text-[10px] uppercase ${severityTextStyle[n.severity]}`}>
                    {n.severity}
                  </span>
                  <p className="mt-0.5 font-mono text-[10px] text-zinc-400">
                    {new Date(n.triggeredAt).toLocaleString()}
                  </p>
                  {!n.read && (
                    <button
                      onClick={() => readMutation.mutate(n.id)}
                      className="mt-1 text-[10px] text-zinc-600 hover:text-zinc-900 transition-colors"
                    >
                      mark read
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
