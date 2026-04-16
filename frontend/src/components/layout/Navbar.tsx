import React, { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface NavLink {
  label: string
  href: string
}

const NAV_LINKS: NavLink[] = [
  { label: 'Home', href: '/' },
  { label: 'Decks', href: '/decks' },
  { label: 'Collection', href: '/collection' },
  { label: 'Lobby', href: '/lobby' },
]

/** Routes where we show a minimal navbar (logo only, no nav links or user section). */
const MINIMAL_ROUTES = ['/login', '/auth/callback']

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function getInitials(name: string | null): string {
  if (!name) return '?'
  return name
    .split(/\s+/)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export const Navbar: React.FC = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, displayName, logout } = useAuth()
  const [mobileOpen, setMobileOpen] = useState(false)

  const isMinimal = MINIMAL_ROUTES.includes(location.pathname)

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <nav className="sticky top-0 z-40 bg-zinc-900/95 backdrop-blur border-b border-zinc-800">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-14 items-center justify-between">
          {/* ---- Brand ---- */}
          <Link
            to="/"
            className="font-display text-xl text-yellow-400 tracking-wide hover:text-yellow-300 transition-colors duration-150"
          >
            Manalith
          </Link>

          {!isMinimal && (
            <>
              {/* ---- Desktop nav links ---- */}
              <div className="hidden md:flex items-center gap-1">
                {NAV_LINKS.map((link) => {
                  const isActive = location.pathname === link.href
                  return (
                    <Link
                      key={link.href}
                      to={link.href}
                      className={`px-3 py-2 text-sm font-medium transition-colors duration-150 ${
                        isActive
                          ? 'text-yellow-400 border-b-2 border-yellow-500'
                          : 'text-zinc-400 hover:text-zinc-100'
                      }`}
                    >
                      {link.label}
                    </Link>
                  )
                })}
              </div>

              {/* ---- User section (desktop) ---- */}
              <div className="hidden md:flex items-center gap-3">
                {isAuthenticated ? (
                  <>
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 rounded-full bg-yellow-700 text-black font-bold text-sm flex items-center justify-center">
                        {getInitials(displayName)}
                      </div>
                      <span className="text-sm text-zinc-300">{displayName}</span>
                    </div>
                    <button
                      onClick={handleLogout}
                      className="px-3 py-1.5 text-sm rounded-md bg-transparent hover:bg-zinc-800 text-zinc-400 hover:text-zinc-100 transition-colors duration-150"
                    >
                      Logout
                    </button>
                  </>
                ) : (
                  <Link
                    to="/login"
                    className="px-4 py-2 text-sm rounded-md bg-yellow-600 hover:bg-yellow-500 text-black font-semibold transition-colors duration-150"
                  >
                    Sign In
                  </Link>
                )}
              </div>

              {/* ---- Mobile hamburger ---- */}
              <button
                className="md:hidden p-2 text-zinc-400 hover:text-zinc-100 transition-colors duration-150 focus:ring-2 focus:ring-yellow-500 focus:outline-none rounded-md"
                onClick={() => setMobileOpen((v) => !v)}
                aria-label="Toggle menu"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  {mobileOpen ? (
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  ) : (
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                  )}
                </svg>
              </button>
            </>
          )}
        </div>
      </div>

      {/* ---- Mobile slide-down panel ---- */}
      {!isMinimal && mobileOpen && (
        <div className="md:hidden border-t border-zinc-800 bg-zinc-900/95 backdrop-blur">
          <div className="px-4 py-3 space-y-1">
            {NAV_LINKS.map((link) => {
              const isActive = location.pathname === link.href
              return (
                <Link
                  key={link.href}
                  to={link.href}
                  onClick={() => setMobileOpen(false)}
                  className={`block px-3 py-2 rounded-md text-sm font-medium transition-colors duration-150 ${
                    isActive
                      ? 'text-yellow-400 bg-zinc-800'
                      : 'text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800'
                  }`}
                >
                  {link.label}
                </Link>
              )
            })}

            <div className="pt-3 border-t border-zinc-800">
              {isAuthenticated ? (
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-full bg-yellow-700 text-black font-bold text-sm flex items-center justify-center">
                      {getInitials(displayName)}
                    </div>
                    <span className="text-sm text-zinc-300">{displayName}</span>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="px-3 py-1.5 text-sm rounded-md bg-transparent hover:bg-zinc-800 text-zinc-400 hover:text-zinc-100 transition-colors duration-150"
                  >
                    Logout
                  </button>
                </div>
              ) : (
                <Link
                  to="/login"
                  onClick={() => setMobileOpen(false)}
                  className="block w-full text-center px-4 py-2 text-sm rounded-md bg-yellow-600 hover:bg-yellow-500 text-black font-semibold transition-colors duration-150"
                >
                  Sign In
                </Link>
              )}
            </div>
          </div>
        </div>
      )}
    </nav>
  )
}
