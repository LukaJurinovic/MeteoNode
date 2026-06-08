import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const navItems = [
  { to: '/',              label: 'Dashboard',     icon: '▦' },
  { to: '/notifications', label: 'Notifications', icon: '◎' },
]

const operatorItems = [
  { to: '/devices',     label: 'Devices',     icon: '◫' },
  { to: '/alarm-rules', label: 'Alarm Rules', icon: '◈' },
  { to: '/system',      label: 'System',      icon: '◉' },
]

const adminItems = [
  { to: '/admin', label: 'Users', icon: '⬡' },
]

export default function Layout() {
  const { auth, logout } = useAuth()

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Sidebar */}
      <aside className="flex w-52 flex-col border-r border-zinc-200 bg-white">
        {/* Brand */}
        <div className="flex items-center gap-2 border-b border-zinc-200 px-4 py-4">
          <span className="text-sm font-semibold tracking-widest text-zinc-900 uppercase">MeteoNode</span>
        </div>

        {/* Nav */}
        <nav className="flex-1 px-2 py-3 space-y-0.5">
          {navItems.map(item => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-2.5 rounded-sm px-3 py-2 text-xs font-medium transition-colors ${
                  isActive
                    ? 'bg-zinc-100 text-zinc-900'
                    : 'text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700'
                }`
              }
            >
              <span className="font-mono text-sm">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}

          {(auth?.role === 'ADMIN' || auth?.role === 'OPERATOR') && (
            <>
              <div className="my-2 border-t border-zinc-200" />
              {operatorItems.map(item => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center gap-2.5 rounded-sm px-3 py-2 text-xs font-medium transition-colors ${
                      isActive
                        ? 'bg-zinc-100 text-zinc-900'
                        : 'text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700'
                    }`
                  }
                >
                  <span className="font-mono text-sm">{item.icon}</span>
                  {item.label}
                </NavLink>
              ))}
            </>
          )}

          {auth?.role === 'ADMIN' && (
            <>
              <div className="my-2 border-t border-zinc-200" />
              {adminItems.map(item => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center gap-2.5 rounded-sm px-3 py-2 text-xs font-medium transition-colors ${
                      isActive
                        ? 'bg-zinc-100 text-zinc-900'
                        : 'text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700'
                    }`
                  }
                >
                  <span className="font-mono text-sm">{item.icon}</span>
                  {item.label}
                </NavLink>
              ))}
            </>
          )}
        </nav>

        {/* User */}
        <div className="border-t border-zinc-200 px-4 py-3">
          <p className="text-xs font-medium text-zinc-800">{auth?.username}</p>
          <div className="mt-1 flex items-center justify-between">
            <span className="font-mono text-[10px] text-zinc-500">{auth?.role}</span>
            <button
              onClick={logout}
              className="text-[10px] text-zinc-400 hover:text-zinc-700 transition-colors"
            >
              logout
            </button>
          </div>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 overflow-y-auto bg-zinc-100">
        <Outlet />
      </main>
    </div>
  )
}
