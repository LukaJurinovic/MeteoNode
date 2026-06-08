import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getUsers, updateUserRole, deleteUser } from '../services/userService'
import type { Role } from '../types'
import Spinner from '../components/Spinner'

const ROLES: Role[] = ['ADMIN', 'OPERATOR', 'USER']

export default function AdminPage() {
  const queryClient = useQueryClient()

  const { data: users, isLoading: loadingUsers, isError } = useQuery({
    queryKey: ['users'],
    queryFn: getUsers,
  })

  const roleMutation = useMutation({
    mutationFn: ({ username, role }: { username: string; role: Role }) =>
      updateUserRole(username, role),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  })

  return (
    <div className="px-8 py-7">
      <div className="mb-6 border-b border-zinc-200 pb-4">
        <h1 className="text-sm font-semibold uppercase tracking-widest text-zinc-900">User Management</h1>
        <p className="mt-0.5 text-xs text-zinc-500">Manage accounts and roles</p>
      </div>

      {loadingUsers && (
        <div className="flex items-center gap-2 text-xs text-zinc-500">
          <Spinner /> Loading users...
        </div>
      )}

      {isError && (
        <p className="text-xs text-red-500">Failed to load users.</p>
      )}

      {users && (
        <div className="border border-zinc-200">
          <div className="grid grid-cols-4 border-b border-zinc-200 bg-zinc-50 px-4 py-2">
            {['Username', 'Email', 'Role', ''].map((h, i) => (
              <span key={i} className="text-xs font-medium uppercase tracking-widest text-zinc-400">{h}</span>
            ))}
          </div>
          {users.map((user, i) => (
            <div
              key={user.id}
              className={`grid grid-cols-4 items-center px-4 py-3 ${
                i !== users.length - 1 ? 'border-b border-zinc-200' : ''
              }`}
            >
              <span className="font-mono text-xs text-zinc-800">{user.username}</span>
              <span className="font-mono text-xs text-zinc-500">{user.email}</span>
              <select
                value={user.role}
                onChange={e =>
                  roleMutation.mutate({ username: user.username, role: e.target.value as Role })
                }
                className="w-28 bg-zinc-50 border border-zinc-300 text-xs text-zinc-700 px-2 py-1 font-mono focus:outline-none focus:border-emerald-500"
              >
                {ROLES.map(r => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
              <button
                onClick={() => {
                  if (window.confirm(`Delete user "${user.username}"? This cannot be undone.`)) {
                    deleteMutation.mutate(user.username)
                  }
                }}
                className="w-fit font-mono text-[10px] text-red-500 hover:text-red-600 transition-colors"
              >
                delete
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
