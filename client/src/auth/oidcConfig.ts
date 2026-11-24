// src/auth/oidcConfig.ts
import type { AuthProviderProps } from 'react-oidc-context'
import { WebStorageStateStore } from 'oidc-client-ts'

// 'mock' until the trainer's provider exists, then switch to 'oidc' in .env.local.
export const AUTH_MODE: 'mock' | 'oidc' =
  import.meta.env.VITE_AUTH_MODE === 'oidc' ? 'oidc' : 'mock'

export const oidcConfig: AuthProviderProps = {
  // The provider's base URL. Keycloak example: http://<host>:<port>/realms/<realm>
  authority: import.meta.env.VITE_OIDC_AUTHORITY ?? '',
  // The ID the provider assigned to this React app.
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID ?? '',
  // Where the provider sends the browser back after sign-in. Must be registered on the provider.
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: `${window.location.origin}/Login`,
  // Authorization code flow. oidc-client-ts adds PKCE automatically.
  response_type: 'code',
  scope: 'openid profile',
  // Keep tokens for this tab only.
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  // Quietly refresh the token before it expires.
  automaticSilentRenew: true,
  // Remove ?code=...&state=... from the address bar after the callback is processed.
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname)
  },
}
