// src/auth/AuthContext.tsx
import { createContext, useContext, useState, type ReactNode } from 'react'
import {
  AuthProvider as OidcProvider,
  useAuth as useOidc,
} from 'react-oidc-context'
import { jwtDecode } from 'jwt-decode'
import { AUTH_MODE, oidcConfig } from './oidcConfig'
import { setToken } from './tokenStore'

export type Role = 'TRADER' | 'OPERATIONS'
export type MockUsername = 'team-1-trader1' | 'team-1-trader2' | 'team-1-ops1'

export interface AuthUser {
  username: string
  role: Role
}

// What every page sees through useAuth(), whichever mode is running.
interface AuthContextValue {
  user: AuthUser | null
  isLoading: boolean
  error: string | null
  signIn: (mockUser?: MockUsername) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

// ---------------------------------------------------------------------------
// Reading the role from a real token.
// The claim path must match what the backend's JwtAuthenticationConverter reads.
//   Keycloak: realm_access.roles   Cognito: cognito:groups   Custom: roles / scope
// ---------------------------------------------------------------------------
type Claims = {
  sub: string
  preferred_username?: string
  realm_access?: { roles: string[] }
}

function userFromToken(accessToken: string): AuthUser {
  const claims = jwtDecode<Claims>(accessToken)
  const roles = claims.realm_access?.roles ?? []
  return {
    username: claims.preferred_username ?? claims.sub,
    role: roles.includes('OPERATIONS') ? 'OPERATIONS' : 'TRADER',
  }
}

// ---------------------------------------------------------------------------
// REAL MODE: redirect to the provider (authorization code + PKCE).
// ---------------------------------------------------------------------------
function OidcBridge({ children }: { children: ReactNode }) {
  const oidc = useOidc()
  const accessToken = oidc.user?.access_token ?? null

  // Set during render, not in an effect: child effects run before parent effects,
  // so the token must already be there when a page makes its first API call.
  setToken(accessToken)

  const value: AuthContextValue = {
    user: accessToken ? userFromToken(accessToken) : null,
    isLoading: oidc.isLoading,
    error: oidc.error?.message ?? null,
    signIn: () => oidc.signinRedirect(),
    logout: async () => {
      setToken(null)
      await oidc.signoutRedirect()
    },
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// ---------------------------------------------------------------------------
// MOCK MODE: no provider needed. For building the UI only.
// The backend will reject these fake tokens with 401, by design.
// ---------------------------------------------------------------------------
const MOCK_KEY = 'mock-auth'

function loadMockUser(): AuthUser | null {
  try {
    const raw = sessionStorage.getItem(MOCK_KEY)
    return raw ? (JSON.parse(raw) as AuthUser) : null
  } catch {
    return null
  }
}

function MockBridge({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadMockUser)

  setToken(user ? `mock-${user.username}` : null)

  const value: AuthContextValue = {
    user,
    isLoading: false,
    error: null,
    signIn: async (mockUser = 'team-1-trader1') => {
      const next: AuthUser = {
        username: mockUser,
        role: mockUser === 'team-1-ops1' ? 'OPERATIONS' : 'TRADER',
      }
      sessionStorage.setItem(MOCK_KEY, JSON.stringify(next))
      setUser(next)
    },
    logout: async () => {
      sessionStorage.removeItem(MOCK_KEY)
      setUser(null)
    },
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// ---------------------------------------------------------------------------
// Wrap <App /> with this in main.tsx.
// ---------------------------------------------------------------------------
export function AuthProvider({ children }: { children: ReactNode }) {
  if (AUTH_MODE === 'oidc') {
    return (
      <OidcProvider {...oidcConfig}>
        <OidcBridge>{children}</OidcBridge>
      </OidcProvider>
    )
  }
  return <MockBridge>{children}</MockBridge>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
