import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import type { Role } from '../types'

interface Props {
  requiredRole?: Role
  allowedRoles?: Role[]
}

export default function PrivateRoute({ requiredRole, allowedRoles }: Props) {
  const { auth } = useAuth()

  if (!auth) return <Navigate to="/login" replace />
  if (requiredRole && auth.role !== requiredRole) return <Navigate to="/" replace />
  if (allowedRoles && !allowedRoles.includes(auth.role)) return <Navigate to="/" replace />

  return <Outlet />
}
