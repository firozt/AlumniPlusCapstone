// src/auth/AuthContext.tsx
import { createContext, useContext, useState, type ReactNode } from 'react'
import { jwtDecode } from 'jwt-decode'

export type Role = 'TRADER' | 'OPERATIONS'

export interface AuthUser {
  token: string
  username: string
  role: Role
}

interface AuthContextValue {
  user: AuthUser | null
  login: (username: string, password: string) => Promise<AuthUser>
  logout: () => void
}

const STORAGE_KEY = 'auth'
const AuthContext = createContext<AuthContextValue | null>(null)

// sessionStorage: survives a page refresh, cleared when the tab closes.
function loadStoredUser(): AuthUser | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AuthUser) : null
  } catch {
    return null
  }
}

// Used by the axios interceptor, which lives outside React.
export function getToken(): string | null {
  return loadStoredUser()?.token ?? null
}

// ---------------------------------------------------------------------------
// REAL TOKENS (for when the trainer's OAuth2 provider is available)
// Decodes a JWT into a user. Adjust the role claim path to match the provider:
//   Keycloak: realm_access.roles   Cognito: cognito:groups   Custom: roles / scope
// ---------------------------------------------------------------------------
type Claims = {
  sub: string
  preferred_username?: string
  realm_access?: { roles: string[] }
}

export function userFromToken(token: string): AuthUser {
  const claims = jwtDecode<Claims>(token)
  const roles = claims.realm_access?.roles ?? []
  return {
    token,
    username: claims.preferred_username ?? claims.sub,
    role: roles.includes('OPERATIONS') ? 'OPERATIONS' : 'TRADER',
  }
}

// ---------------------------------------------------------------------------
// MOCK LOGIN (temporary)
// Same usernames the spec says the trainer's provider will have.
// When the real provider arrives, replace mockLogin with the real flow and
// build the user with userFromToken(). Nothing outside this file changes.
// ---------------------------------------------------------------------------
const MOCK_USERS: Record<string, Role> = {
  trader1: 'TRADER',
  ops1: 'OPERATIONS',
}

async function mockLogin(
  username: string,
  _password: string,
): Promise<AuthUser> {
  const role = MOCK_USERS[username]
  if (!role) throw new Error('Unknown user. Try trader1 or ops1.')
  return { token: `mock-${username}`, username, role }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser)

  const login = async (username: string, password: string) => {
    const next = await mockLogin(username, password)
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    setUser(next)
    return next
  }

  const logout = () => {
    sessionStorage.removeItem(STORAGE_KEY)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
