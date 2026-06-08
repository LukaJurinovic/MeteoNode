import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from './contexts/AuthContext'
import PrivateRoute from './components/PrivateRoute'
import ErrorBoundary from './components/ErrorBoundary'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import StationDetailPage from './pages/StationDetailPage'
import NotificationsPage from './pages/NotificationsPage'
import AdminPage from './pages/AdminPage'
import AlarmRulesPage from './pages/AlarmRulesPage'
import DeviceManagementPage from './pages/DeviceManagementPage'
import SystemPage from './pages/SystemPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 10_000 },
  },
})

export default function App() {
  return (
    <ErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />

            <Route element={<PrivateRoute />}>
              <Route element={<Layout />}>
                <Route path="/"                element={<DashboardPage />} />
                <Route path="/stations/:id"    element={<StationDetailPage />} />
                <Route path="/notifications"   element={<NotificationsPage />} />
              </Route>
            </Route>

            <Route element={<PrivateRoute allowedRoles={['ADMIN', 'OPERATOR']} />}>
              <Route element={<Layout />}>
                <Route path="/alarm-rules" element={<AlarmRulesPage />} />
                <Route path="/devices"     element={<DeviceManagementPage />} />
                <Route path="/system"      element={<SystemPage />} />
              </Route>
            </Route>

            <Route element={<PrivateRoute requiredRole="ADMIN" />}>
              <Route element={<Layout />}>
                <Route path="/admin" element={<AdminPage />} />
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
    </ErrorBoundary>
  )
}
