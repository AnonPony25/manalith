import { create } from 'zustand'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface AuthState {
  userId: string | null
  displayName: string | null
  accessToken: string | null
  isAuthenticated: boolean

  login: (accessToken: string, refreshToken: string) => Promise<void>
  logout: () => Promise<void>
  refreshToken: () => Promise<boolean>
  _setFromToken: (accessToken: string) => void
}

// ---------------------------------------------------------------------------
// JWT helpers
// ---------------------------------------------------------------------------

/**
 * Decodes the payload section of a JWT (no signature verification — that is
 * the responsibility of the server).
 */
function decodeJwtPayload(token: string): Record<string, unknown> {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) throw new Error('Invalid JWT structure')
    // base64url → base64 → JSON
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = atob(base64)
    return JSON.parse(json)
  } catch {
    return {}
  }
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const REFRESH_TOKEN_KEY = 'manalith_refresh_token'

// ---------------------------------------------------------------------------
// Store
// ---------------------------------------------------------------------------

export const useAuthStore = create<AuthState>((set, get) => ({
  userId: null,
  displayName: null,
  accessToken: null,
  isAuthenticated: false,

  /**
   * Called after OAuth2 callback — stores tokens and decodes the access token.
   * The access token lives only in memory; the refresh token is persisted to
   * localStorage so sessions survive page refreshes.
   */
  async login(accessToken: string, refreshToken: string): Promise<void> {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    get()._setFromToken(accessToken)
  },

  /**
   * Calls the server logout endpoint to revoke all refresh tokens, then
   * wipes local state and storage.
   */
  async logout(): Promise<void> {
    const { accessToken } = get()
    if (accessToken) {
      try {
        // Lazy import to avoid circular dependency with authApi
        const { authApi } = await import('@/api/authApi')
        await authApi.logout()
      } catch {
        // Best-effort — always clear local state regardless
      }
    }
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    set({ userId: null, displayName: null, accessToken: null, isAuthenticated: false })
  },

  /**
   * Attempts to obtain a new access token using the stored refresh token.
   * Returns true on success, false if the refresh token is absent or rejected.
   */
  async refreshToken(): Promise<boolean> {
    const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!storedRefreshToken) return false

    try {
      const { authApi } = await import('@/api/authApi')
      const data = await authApi.refresh(storedRefreshToken)
      // Rotate: store the new refresh token
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
      get()._setFromToken(data.accessToken)
      return true
    } catch {
      // Refresh failed — clear stale local state
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      set({ userId: null, displayName: null, accessToken: null, isAuthenticated: false })
      return false
    }
  },

  /**
   * Parses a JWT access token and updates the in-memory auth state.
   * Does NOT persist anything; refresh token handling is separate.
   */
  _setFromToken(accessToken: string): void {
    const payload = decodeJwtPayload(accessToken)
    set({
      accessToken,
      userId: typeof payload.sub === 'string' ? payload.sub : null,
      displayName: typeof payload.name === 'string' ? payload.name : null,
      isAuthenticated: true,
    })
  },
}))

// ---------------------------------------------------------------------------
// Session restoration on module load
// ---------------------------------------------------------------------------
// Attempt to restore the session from a persisted refresh token.
// This runs once when the module is first imported (i.e., at app startup).
;(async () => {
  const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (storedRefreshToken) {
    await useAuthStore.getState().refreshToken()
  }
})()
