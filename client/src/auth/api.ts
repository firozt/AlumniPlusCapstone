// src/lib/api.ts
// Use this instead of importing axios directly, so every request carries the token.
import axios from 'axios'
import { getToken } from '../auth/AuthContext'

const api = axios.create()

// Attach "Authorization: Bearer <token>" to every request.
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 401 = missing or expired token: clear the session and go to login.
// 403 is NOT handled here: it means "logged in but not allowed", so the
// calling page should show the error message instead of logging the user out.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (axios.isAxiosError(err) && err.response?.status === 401) {
      sessionStorage.removeItem('auth')
      window.location.href = '/Login'
    }
    return Promise.reject(err)
  },
)

export default api
