// src/auth/RequireAuth.tsx
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'

// Wrap protected routes with this. No user -> /Login.
// This is a UX guard only; the backend is what actually enforces access.
export default function RequireAuth() {
  const { user, isLoading } = useAuth()

  // On a page refresh the OIDC library takes a moment to restore the session.
  // Without this, a logged-in user would be bounced to /Login for a split second.
  if (isLoading) {
    return (
      <div className="min-h-svh bg-page text-white/60 flex items-center justify-center">
        Loading…
      </div>
    )
  }

  return user ? <Outlet /> : <Navigate to="/Login" replace />
}
