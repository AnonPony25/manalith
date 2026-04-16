import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

/**
 * Handles the OAuth2 redirect from the Spring Boot backend.
 *
 * The backend's {@code OAuth2AuthenticationSuccessHandler} redirects here with:
 *   {@code /auth/callback?accessToken=<jwt>&refreshToken=<opaque>}
 *
 * This component:
 *   1. Reads the tokens from query parameters
 *   2. Calls {@code authStore.login()} to store them and decode the access token
 *   3. Navigates to {@code /lobby}
 *   4. Shows an error screen if tokens are missing
 */
export function AuthCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)

  const [error, setError] = useState<string | null>(null)
  // Guard against double-invocation in React 18 Strict Mode
  const processed = useRef(false)

  useEffect(() => {
    if (processed.current) return
    processed.current = true

    const accessToken = searchParams.get('accessToken')
    const refreshToken = searchParams.get('refreshToken')

    if (!accessToken || !refreshToken) {
      setError('Authentication failed: tokens missing from callback URL.')
      return
    }

    login(accessToken, refreshToken)
      .then(() => {
        navigate('/lobby', { replace: true })
      })
      .catch(() => {
        setError('Authentication failed: unable to process tokens.')
      })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // ---------------------------------------------------------------------------
  // Error state
  // ---------------------------------------------------------------------------
  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-zinc-950 text-zinc-100 px-4 gap-4">
        <p className="text-red-400 font-semibold">{error}</p>
        <a
          href="/login"
          className="text-sm text-zinc-400 underline hover:text-mana-gold transition-colors"
        >
          Return to login
        </a>
      </div>
    )
  }

  // ---------------------------------------------------------------------------
  // Loading state (spinner while processing)
  // ---------------------------------------------------------------------------
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-zinc-950 gap-4">
      {/* Spinning ring */}
      <div
        className="h-10 w-10 rounded-full border-4 border-zinc-700 border-t-mana-gold animate-spin"
        role="status"
        aria-label="Signing in…"
      />
      <p className="text-sm text-zinc-400">Signing you in…</p>
    </div>
  )
}
