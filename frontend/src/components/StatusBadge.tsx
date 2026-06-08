import type { Status } from '../types'

interface Props {
  status: Status
}

export default function StatusBadge({ status }: Props) {
  const online = status === 'ONLINE'
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className={`h-1.5 w-1.5 rounded-full ${online ? 'bg-emerald-500' : 'bg-zinc-300'}`} />
      <span className={`text-xs font-mono font-medium ${online ? 'text-emerald-600' : 'text-zinc-400'}`}>
        {status}
      </span>
    </span>
  )
}
