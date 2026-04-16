import React from 'react'

// ---------------------------------------------------------------------------
// SVG icon components — inline to avoid extra dependencies
// ---------------------------------------------------------------------------

function DiscordIcon() {
  return (
    <svg
      className="h-5 w-5 shrink-0"
      viewBox="0 0 24 24"
      fill="currentColor"
      aria-hidden="true"
    >
      <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03Z" />
    </svg>
  )
}

function GitHubIcon() {
  return (
    <svg
      className="h-5 w-5 shrink-0"
      viewBox="0 0 24 24"
      fill="currentColor"
      aria-hidden="true"
    >
      <path
        fillRule="evenodd"
        d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0 1 12 6.844a9.59 9.59 0 0 1 2.504.337c1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.02 10.02 0 0 0 22 12.017C22 6.484 17.522 2 12 2Z"
        clipRule="evenodd"
      />
    </svg>
  )
}

// ---------------------------------------------------------------------------
// Provider button
// ---------------------------------------------------------------------------

interface ProviderButtonProps {
  href: string
  icon: React.ReactNode
  label: string
  className?: string
}

function ProviderButton({ href, icon, label, className = '' }: ProviderButtonProps) {
  return (
    <a
      href={href}
      className={[
        'flex items-center justify-center gap-3',
        'w-full rounded-lg px-4 py-3',
        'text-sm font-semibold text-white',
        'transition-all duration-200',
        'focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-zinc-900',
        className,
      ].join(' ')}
    >
      {icon}
      {label}
    </a>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

/**
 * Login page with OAuth2 provider buttons for Discord and GitHub.
 * Each button redirects to the Spring Security OAuth2 authorization endpoint.
 */
export function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-950 px-4">
      {/* Subtle radial glow behind the card */}
      <div
        className="pointer-events-none fixed inset-0 z-0"
        style={{
          background:
            'radial-gradient(ellipse 60% 50% at 50% 40%, rgba(180,140,80,0.10) 0%, transparent 70%)',
        }}
      />

      <div className="relative z-10 w-full max-w-sm">
        {/* Card */}
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/90 p-8 shadow-2xl backdrop-blur-sm">
          {/* Title */}
          <div className="mb-8 text-center">
            <h1 className="font-display text-4xl font-bold tracking-widest text-mana-gold drop-shadow-[0_0_12px_rgba(180,140,80,0.5)]">
              Manalith
            </h1>
            <p className="mt-2 text-sm text-zinc-400">Sign in to start playing</p>
          </div>

          {/* Provider buttons */}
          <div className="flex flex-col gap-3">
            <ProviderButton
              href="/oauth2/authorization/discord"
              icon={<DiscordIcon />}
              label="Continue with Discord"
              className={[
                'bg-[#5865F2]',
                'hover:bg-[#4752C4]',
                'focus:ring-[#5865F2]',
                'active:scale-[0.98]',
              ].join(' ')}
            />

            <ProviderButton
              href="/oauth2/authorization/github"
              icon={<GitHubIcon />}
              label="Continue with GitHub"
              className={[
                'bg-zinc-800 border border-zinc-700',
                'hover:bg-zinc-700 hover:border-mana-gold/50',
                'focus:ring-mana-gold',
                'active:scale-[0.98]',
              ].join(' ')}
            />
          </div>

          {/* Footer */}
          <p className="mt-8 text-center text-xs text-zinc-600">
            By signing in you agree to the{' '}
            <a
              href="https://github.com/manalith/manalith/blob/main/LICENSE"
              target="_blank"
              rel="noreferrer"
              className="text-zinc-500 underline hover:text-mana-gold transition-colors"
            >
              GPL-3.0 license terms
            </a>
            .
          </p>
        </div>

        {/* Mana pip decoration */}
        <div className="mt-6 flex justify-center gap-2 opacity-30">
          {['⚪', '🔵', '⚫', '🔴', '🟢'].map((pip, i) => (
            <span key={i} className="text-xs" aria-hidden="true">
              {pip}
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}
