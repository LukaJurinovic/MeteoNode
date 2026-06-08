import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
import { login as apiLogin, logout as apiLogout, getStoredAuth } from '../services/authService'
import type { Role } from '../types'

interface AuthState {
  username: string
  role: Role
}

interface AuthContextValue {
  auth: AuthState | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(() => {
    const stored = getStoredAuth()
    return stored ? { username: stored.username, role: stored.role as Role } : null
  })

  const login = useCallback(async (username: string, password: string) => {
    const data = await apiLogin(username, password)
    setAuth({ username: data.username, role: data.role })
  }, [])

  const logout = useCallback(async () => {
    await apiLogout()
    setAuth(null)
  }, [])

  return (
    <AuthContext.Provider value={{ auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
