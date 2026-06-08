import { useQuery } from '@tanstack/react-query'
import { getMeasurementHistory } from '../services/stationService'
import type { Metric } from '../types'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import Spinner from './Spinner'

interface Props {
  sensorId: number
  metric: Metric
  from: string
  to: string
}

function formatTick(iso: string): string {
  const d = new Date(iso)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

export default function MeasurementChart({ sensorId, metric, from, to }: Props) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['history', sensorId, metric, from, to],
    queryFn: () => getMeasurementHistory(sensorId, metric, from, to),
    enabled: !!sensorId && !!from && !!to,
  })

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 py-8 text-xs text-zinc-500">
        <Spinner /> Loading chart data…
      </div>
    )
  }

  if (isError) {
    return <p className="py-4 text-xs text-red-500">Failed to load measurements.</p>
  }

  if (!data || data.length === 0) {
    return <p className="py-4 text-xs text-zinc-500">No measurements in the selected range.</p>
  }

  const chartData = data.map(m => ({ time: m.measuredAt, value: m.value }))

  return (
    <ResponsiveContainer width="100%" height={200}>
      <LineChart data={chartData} margin={{ top: 4, right: 8, left: 0, bottom: 4 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e4e4e7" />
        <XAxis
          dataKey="time"
          tickFormatter={formatTick}
          tick={{ fontSize: 10, fill: '#71717a', fontFamily: 'monospace' }}
          stroke="#d4d4d8"
          minTickGap={60}
        />
        <YAxis
          tick={{ fontSize: 10, fill: '#71717a', fontFamily: 'monospace' }}
          stroke="#d4d4d8"
          width={48}
        />
        <Tooltip
          contentStyle={{ background: '#ffffff', border: '1px solid #e4e4e7', fontSize: 11, fontFamily: 'monospace' }}
          labelStyle={{ color: '#71717a' }}
          itemStyle={{ color: '#10b981' }}
          labelFormatter={(v) => new Date(String(v)).toLocaleString()}
        />
        <Line
          type="monotone"
          dataKey="value"
          stroke="#10b981"
          strokeWidth={1.5}
          dot={false}
          activeDot={{ r: 3, fill: '#10b981' }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}
