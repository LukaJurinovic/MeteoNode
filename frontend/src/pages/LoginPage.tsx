import { useState, useEffect, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { register as apiRegister } from '../services/authService'
import Spinner from '../components/Spinner'
import axios from 'axios'

interface SystemInfo {
  stationCount: number
  nodeCount: number
}

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const [systemInfo, setSystemInfo] = useState<SystemInfo | null>(null)

  useEffect(() => {
    axios.get<SystemInfo>('/api/info').then(r => setSystemInfo(r.data)).catch(() => {})
  }, [])

  function switchMode(next: 'login' | 'register') {
    setMode(next)
    setError('')
    setSuccess('')
    setUsername('')
    setEmail('')
    setPassword('')
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      if (mode === 'login') {
        await login(username, password)
        navigate('/')
      } else {
        await apiRegister(username, email, password)
        setSuccess('Account created. You can now sign in.')
        switchMode('login')
      }
    } catch {
      setError(mode === 'login' ? 'Invalid credentials.' : 'Registration failed. Username or email may already be taken.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-zinc-100 px-4">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="mb-8 text-center">
          <h1 className="text-sm font-semibold tracking-widest text-zinc-900 uppercase">MeteoNode</h1>
          <p className="mt-1 text-xs text-zinc-500">Environmental Monitoring</p>
        </div>

        {/* Card */}
        <div className="border border-zinc-200 bg-white p-6">
          <h2 className="mb-5 text-xs font-medium uppercase tracking-widest text-zinc-500">
            {mode === 'login' ? 'Sign in' : 'Create account'}
          </h2>

          {success && (
            <p className="mb-4 text-xs text-emerald-600">{success}</p>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-1.5 block text-xs text-zinc-500">Username</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                className="w-full border border-zinc-300 bg-zinc-50 px-3 py-2 text-sm text-zinc-900 outline-none placeholder-zinc-400 focus:border-emerald-500/50 transition-colors font-mono"
                placeholder="username"
                required
                autoFocus
              />
            </div>

            {mode === 'register' && (
              <div>
                <label className="mb-1.5 block text-xs text-zinc-500">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  className="w-full border border-zinc-300 bg-zinc-50 px-3 py-2 text-sm text-zinc-900 outline-none placeholder-zinc-400 focus:border-emerald-500/50 transition-colors font-mono"
                  placeholder="user@example.com"
                  required
                />
              </div>
            )}

            <div>
              <label className="mb-1.5 block text-xs text-zinc-500">Password</label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                className="w-full border border-zinc-300 bg-zinc-50 px-3 py-2 text-sm text-zinc-900 outline-none placeholder-zinc-400 focus:border-emerald-500/50 transition-colors font-mono"
                placeholder="••••••••"
                required
              />
            </div>

            {error && (
              <p className="text-xs text-red-500">{error}</p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center gap-2 bg-emerald-500 px-4 py-2 text-xs font-medium text-white transition-colors hover:bg-emerald-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading && <Spinner />}
              {loading ? (mode === 'login' ? 'Signing in…' : 'Creating account…') : (mode === 'login' ? 'Sign in' : 'Create account')}
            </button>
          </form>

          <div className="mt-4 border-t border-zinc-200 pt-4 text-center">
            {mode === 'login' ? (
              <p className="text-xs text-zinc-400">
                No account?{' '}
                <button onClick={() => switchMode('register')} className="text-zinc-600 hover:text-zinc-900 transition-colors">
                  Register
                </button>
              </p>
            ) : (
              <p className="text-xs text-zinc-400">
                Already have an account?{' '}
                <button onClick={() => switchMode('login')} className="text-zinc-600 hover:text-zinc-900 transition-colors">
                  Sign in
                </button>
              </p>
            )}
          </div>
        </div>

        {/* System info strip */}
        {systemInfo && (
          <div className="mt-4 grid grid-cols-2 gap-px bg-zinc-200">
            <div className="bg-white px-4 py-3 text-center">
              <p className="font-mono text-lg font-medium text-emerald-500">{systemInfo.stationCount}</p>
              <p className="text-xs text-zinc-400">Stations</p>
            </div>
            <div className="bg-white px-4 py-3 text-center">
              <p className="font-mono text-lg font-medium text-emerald-500">{systemInfo.nodeCount}</p>
              <p className="text-xs text-zinc-400">Nodes</p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
