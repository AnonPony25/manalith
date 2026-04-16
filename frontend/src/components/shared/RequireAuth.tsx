import { Navigate, Outlet } from 'react-router-dom'

/**
 * RequireAuth
 * Wraps protected routes. Redirects unauthenticated users to /login.
 * TODO: read auth state from authStore (Zustand) or cookie check.
 */
export function RequireAuth() {
  const isAuthenticated = false // TODO: replace with actual auth check
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}
