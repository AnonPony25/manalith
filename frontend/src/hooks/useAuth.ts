import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

// ---------------------------------------------------------------------------
// Primary hook
// ---------------------------------------------------------------------------

/**
 * Provides a convenient interface to the Zustand auth store.
 * Mirrors the most commonly needed auth state and actions.
 *
 * @example
 * ```tsx
 * const { isAuthenticated, displayName, logout } = useAuth()
 * ```
 */
export function useAuth() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const userId = useAuthStore((s) => s.userId)
  const displayName = useAuthStore((s) => s.displayName)
  const login = useAuthStore((s) => s.login)
  const logout = useAuthStore((s) => s.logout)

  return { isAuthenticated, userId, displayName, login, logout }
}

// ---------------------------------------------------------------------------
// Guard hook
// ---------------------------------------------------------------------------

/**
 * Redirects unauthenticated users to {@code /login}.
 * Place at the top of any page component that requires authentication.
 *
 * @example
 * ```tsx
 * export function LobbyPage() {
 *   useRequireAuth()
 *   // ... rest of component
 * }
 * ```
 */
export function useRequireAuth(redirectTo = '/login') {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const navigate = useNavigate()

  useEffect(() => {
    if (!isAuthenticated) {
      navigate(redirectTo, { replace: true })
    }
  }, [isAuthenticated, navigate, redirectTo])

  return isAuthenticated
}
