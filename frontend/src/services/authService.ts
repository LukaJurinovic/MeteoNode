import axios from 'axios'
import type { AuthResponse } from '../types'

export async function login(username: string, password: string): Promise<AuthResponse> {
  const { data } = await axios.post<AuthResponse>('/api/auth/login', { username, password })
  localStorage.setItem('accessToken', data.accessToken)
  localStorage.setItem('refreshToken', data.refreshToken)
  localStorage.setItem('username', data.username)
  localStorage.setItem('role', data.role)
  return data
}

export async function register(username: string, email: string, password: string): Promise<AuthResponse> {
  const { data } = await axios.post<AuthResponse>('/api/auth/register', { username, email, password })
  return data
}

export async function logout(): Promise<void> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (refreshToken) {
    try {
      await axios.post('/api/auth/logout', { refreshToken })
    } catch {
      // best-effort; clear storage regardless
    }
  }
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
}

export function getStoredAuth(): { username: string; role: string } | null {
  const username = localStorage.getItem('username')
  const role = localStorage.getItem('role')
  if (!username || !role) return null
  return { username, role }
}
