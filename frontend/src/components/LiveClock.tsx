import { useState, useEffect } from 'react'

export default function LiveClock() {
  const [now, setNow] = useState(new Date())

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(id)
  }, [])

  return <span className="font-mono text-xs text-zinc-400">{now.toLocaleString()}</span>
}
