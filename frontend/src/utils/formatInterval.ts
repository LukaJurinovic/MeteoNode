export function formatInterval(seconds: number): string {
  if (seconds >= 86400 && seconds % 86400 === 0) return `${seconds / 86400}d`
  if (seconds >= 3600  && seconds % 3600  === 0) return `${seconds / 3600}h`
  if (seconds >= 60    && seconds % 60    === 0) return `${seconds / 60}m`
  return `${seconds}s`
}

export function secondsToDHMS(total: number): string {
  const d = Math.floor(total / 86400)
  const h = Math.floor((total % 86400) / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d}:${pad(h)}:${pad(m)}:${pad(s)}`
}

export function dhmsToSeconds(input: string): number | null {
  const parts = input.split(':').map(p => p.trim())
  if (parts.length === 0 || parts.length > 4 || parts.some(p => !/^\d+$/.test(p))) return null
  const [s = 0, m = 0, h = 0, d = 0] = parts.map(Number).reverse()
  return d * 86400 + h * 3600 + m * 60 + s
}
