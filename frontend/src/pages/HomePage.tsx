import React from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

// ---------------------------------------------------------------------------
// Feature card data
// ---------------------------------------------------------------------------

interface FeatureCard {
  title: string
  description: string
  icon: React.ReactNode
}

const FEATURES: FeatureCard[] = [
  {
    title: 'Play',
    description:
      'Challenge opponents in real-time with full rules enforcement via the Forge engine.',
    icon: (
      <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1.5}
          d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
        />
      </svg>
    ),
  },
  {
    title: 'Build Decks',
    description:
      'Powerful deck builder with 20+ format legality checks, import/export, and stats.',
    icon: (
      <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1.5}
          d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
        />
      </svg>
    ),
  },
  {
    title: 'Collection',
    description:
      'Track your card collection, browse 30,000+ cards from Scryfall.',
    icon: (
      <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1.5}
          d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"
        />
      </svg>
    ),
  },
]

// ---------------------------------------------------------------------------
// Stats data
// ---------------------------------------------------------------------------

interface Stat {
  label: string
  value: string
}

const STATS: Stat[] = [
  { label: 'Players', value: '5,432' },
  { label: 'Decks', value: '23,891' },
  { label: 'Cards', value: '31,247' },
]

// ---------------------------------------------------------------------------
// Mana color decoration
// ---------------------------------------------------------------------------

const MANA_COLORS = [
  { className: 'text-mana-white', symbol: 'W' },
  { className: 'text-mana-blue', symbol: 'U' },
  { className: 'text-mana-black', symbol: 'B' },
  { className: 'text-mana-red', symbol: 'R' },
  { className: 'text-mana-green', symbol: 'G' },
]

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function HomePage(): React.JSX.Element {
  const { isAuthenticated } = useAuth()

  return (
    <div className="bg-zinc-950">
      {/* ---- Hero Section ---- */}
      <section className="relative overflow-hidden">
        {/* Radial gradient background */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-zinc-900 via-zinc-950 to-zinc-950" />

        <div className="relative mx-auto max-w-5xl px-4 py-24 sm:py-32 text-center">
          {/* Mana symbol decoration */}
          <div className="flex items-center justify-center gap-4 mb-8">
            {MANA_COLORS.map((mana) => (
              <span
                key={mana.symbol}
                className={`${mana.className} text-2xl font-bold opacity-60`}
              >
                {mana.symbol}
              </span>
            ))}
          </div>

          <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl text-zinc-100 tracking-wide mb-6">
            Play Magic: The Gathering Online
          </h1>
          <p className="text-lg sm:text-xl text-zinc-400 max-w-2xl mx-auto mb-10">
            Open-source multiplayer platform. Build decks, join lobbies, and
            battle opponents with full rules enforcement.
          </p>

          {/* CTA buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            {isAuthenticated ? (
              <>
                <Link
                  to="/lobby"
                  className="inline-flex items-center px-6 py-3 rounded-md bg-yellow-600 hover:bg-yellow-500 text-black font-semibold text-lg transition-colors duration-150 focus:ring-2 focus:ring-yellow-500 focus:outline-none"
                >
                  Go to Lobby
                </Link>
                <Link
                  to="/decks"
                  className="inline-flex items-center px-6 py-3 rounded-md bg-zinc-700 hover:bg-zinc-600 text-zinc-100 font-semibold text-lg transition-colors duration-150 focus:ring-2 focus:ring-yellow-500 focus:outline-none"
                >
                  My Decks
                </Link>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="inline-flex items-center px-6 py-3 rounded-md bg-yellow-600 hover:bg-yellow-500 text-black font-semibold text-lg transition-colors duration-150 focus:ring-2 focus:ring-yellow-500 focus:outline-none"
                >
                  Get Started
                </Link>
                <Link
                  to="/decks"
                  className="inline-flex items-center px-6 py-3 rounded-md bg-zinc-700 hover:bg-zinc-600 text-zinc-100 font-semibold text-lg transition-colors duration-150 focus:ring-2 focus:ring-yellow-500 focus:outline-none"
                >
                  View Decks
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      {/* ---- Feature Cards ---- */}
      <section className="mx-auto max-w-5xl px-4 py-16">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {FEATURES.map((feature) => (
            <div
              key={feature.title}
              className="bg-zinc-900 border border-zinc-800 rounded-lg p-6 hover:border-yellow-600 transition-colors duration-150"
            >
              <div className="text-yellow-500 mb-4">{feature.icon}</div>
              <h3 className="font-display text-lg text-zinc-100 mb-2">
                {feature.title}
              </h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                {feature.description}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* ---- Stats Bar ---- */}
      <section className="border-t border-b border-zinc-800 bg-zinc-900/50">
        <div className="mx-auto max-w-5xl px-4 py-10">
          <div className="grid grid-cols-3 gap-4 text-center">
            {STATS.map((stat) => (
              <div key={stat.label}>
                <div className="text-2xl sm:text-3xl font-bold text-yellow-400 font-mono">
                  {stat.value}
                </div>
                <div className="text-sm text-zinc-400 mt-1">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ---- Footer ---- */}
      <footer className="mx-auto max-w-5xl px-4 py-10 text-center">
        <p className="text-sm text-zinc-500">
          Manalith is open source.{' '}
          <a
            href="https://github.com/AnonPony25/manalith"
            target="_blank"
            rel="noopener noreferrer"
            className="text-zinc-400 hover:text-zinc-100 underline underline-offset-2 transition-colors duration-150"
          >
            GitHub
          </a>
        </p>
        <p className="text-xs text-zinc-600 mt-2">
          Not affiliated with Wizards of the Coast.
        </p>
      </footer>
    </div>
  )
}
