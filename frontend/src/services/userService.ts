import api from './api'
import type { UserDTO, Role } from '../types'

export async function getUsers(): Promise<UserDTO[]> {
  const { data } = await api.get<UserDTO[]>('/users')
  return data
}

export async function updateUserRole(username: string, role: Role): Promise<void> {
  await api.patch('/users/role', { username, role })
}

export async function deleteUser(username: string): Promise<void> {
  await api.delete(`/users/${username}`)
}
