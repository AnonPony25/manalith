import type { ReactNode } from 'react'

/**
 * AppShell
 * Top-level layout wrapper providing persistent nav, sidebar, and footer.
 * Placeholder — will be fleshed out in a follow-up.
 */
export function AppShell({ children }: { children: ReactNode }) {
  return <div className="app-shell min-h-screen flex flex-col">{children}</div>
}
