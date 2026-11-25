// src/pages/Callback.tsx
// The provider sends the browser here after sign-in (?code=...&state=...).
// react-oidc-context exchanges the code for tokens on its own; this page just
// waits for that to finish, then moves on.
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Callback() {
  const { user, isLoading, error } = useAuth()

  if (error) {
    return (
      <div className="min-h-svh bg-page text-ink flex flex-col items-center justify-center gap-4">
        <div className="text-rose-300">Sign-in failed: {error}</div>
        <Link to="/Login" className="text-accent underline hover:no-underline">
          Try again
        </Link>
      </div>
    )
  }

  if (!isLoading && user) return <Navigate to="/TradeOrder" replace />

  return (
    <div className="min-h-svh bg-page text-white/60 flex items-center justify-center">
      Signing you in…
    </div>
  )
}
