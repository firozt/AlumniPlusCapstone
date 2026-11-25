// src/lib/api.ts
// Use this instead of importing axios directly, so every request carries the token.
import axios from 'axios'
import { getToken } from '../auth/tokenStore'

const api = axios.create()

// Attach "Authorization: Bearer <token>" to every request.
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 401 = missing, expired or invalid token: send the user back to sign in.
// 403 is NOT handled here: it means "signed in but not allowed", so the
// calling page should show the error message instead.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (axios.isAxiosError(err) && err.response?.status === 401) {
      // The token was rejected, so the saved login is no good. Clear it,
      // or the Login page sees a "logged-in" user and bounces straight back here.
      sessionStorage.clear()
      if (window.location.pathname !== '/Login') {
        window.location.href = '/Login'
      }
    }
    return Promise.reject(err)
  },
)

export default api
