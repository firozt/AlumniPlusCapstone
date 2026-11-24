// src/auth/RequireAuth.tsx
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'

// Wrap protected routes with this. No user -> back to /Login.
// This is a UX guard only; the backend is what actually enforces access.
export default function RequireAuth() {
  const { user } = useAuth()
  return user ? <Outlet /> : <Navigate to="/Login" replace />
}
