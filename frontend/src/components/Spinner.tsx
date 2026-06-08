export default function Spinner({ className = '' }: { className?: string }) {
  return (
    <div className={`h-4 w-4 animate-spin rounded-full border-2 border-zinc-200 border-t-emerald-500 ${className}`} />
  )
}
