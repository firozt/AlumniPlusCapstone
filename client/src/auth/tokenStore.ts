// src/auth/tokenStore.ts
// Holds the current access token outside React so the axios interceptor can read it.
// AuthContext keeps this in sync; nothing else should call setToken.
let currentToken: string | null = null

export function setToken(token: string | null) {
  currentToken = token
}

export function getToken(): string | null {
  return currentToken
}
