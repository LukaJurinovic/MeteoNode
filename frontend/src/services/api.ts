import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let refreshing: Promise<string> | null = null

api.interceptors.response.use(
  res => res,
  async err => {
    const original = err.config
    if (err.response?.status !== 401 || original._retry) {
      return Promise.reject(err)
    }
    original._retry = true

    if (!refreshing) {
      refreshing = axios
        .post('/api/auth/refresh', { refreshToken: localStorage.getItem('refreshToken') })
        .then(r => {
          const token: string = r.data.accessToken
          localStorage.setItem('accessToken', token)
          return token
        })
        .catch(() => {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          localStorage.removeItem('username')
          localStorage.removeItem('role')
          window.location.href = '/login'
          return Promise.reject(err)
        })
        .finally(() => { refreshing = null })
    }

    const token = await refreshing
    original.headers.Authorization = `Bearer ${token}`
    return api(original)
  }
)

export default api
