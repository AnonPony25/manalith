import React from 'react'
import { Navbar } from './Navbar'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface AppShellProps {
  children: React.ReactNode
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export const AppShell: React.FC<AppShellProps> = ({ children }) => {
  return (
    <div className="flex min-h-screen flex-col bg-zinc-950 text-zinc-100">
      <Navbar />
      <main className="flex-1 overflow-y-auto">{children}</main>
    </div>
  )
}
