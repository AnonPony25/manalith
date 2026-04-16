import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore'
import { useAuthStore } from '@/store/authStore'
import { Button } from '@/components/ui'
import { Badge } from '@/components/ui'

// ── Status helpers ──────────────────────────────────────────────────────────

type ConnectionStatus = 'connected' | 'connecting' | 'disconnected'

const statusConfig: Record<ConnectionStatus, { dot: string; label: string }> = {
  connected:    { dot: 'bg-green-500',              label: 'Connected' },
  connecting:   { dot: 'bg-yellow-500 animate-pulse', label: 'Connecting...' },
  disconnected: { dot: 'bg-red-500',                label: 'Disconnected' },
}

// ── Zone button (inline) ────────────────────────────────────────────────────

function ZoneButton({ name, count, zone }: { name: string; count: number; zone: string }) {
  return (
    <button
      className="flex flex-col items-center bg-zinc-800 hover:bg-zinc-700 border border-zinc-600 rounded px-2 py-1 text-xs text-zinc-300 transition-colors"
      data-zone={zone}
    >
      <span className="font-mono text-zinc-400">{count}</span>
      <span>{name}</span>
    </button>
  )
}

// ── Player seat ─────────────────────────────────────────────────────────────

function PlayerSeat({
  name,
  life,
  poison,
  isOpponent,
}: {
  name: string
  life: number
  poison: number
  isOpponent: boolean
}) {
  return (
    <div className="flex items-center gap-3 px-4 py-2">
      <div className="w-8 h-8 rounded-full bg-zinc-700 flex items-center justify-center text-xs text-zinc-400 font-semibold shrink-0">
        {name.charAt(0).toUpperCase()}
      </div>
      <span className="text-sm text-zinc-300 font-body">{name}</span>
      <span className="font-display text-xl text-zinc-100">{life}</span>
      <span className="text-xs text-zinc-500">☠ {poison}</span>
      {!isOpponent && (
        <span className="ml-auto text-xs text-yellow-500 font-semibold">You</span>
      )}
    </div>
  )
}

// ── Main component ──────────────────────────────────────────────────────────

export function GamePage() {
  const { roomId } = useParams<{ roomId: string }>()
  const navigate = useNavigate()
  const accessToken = useAuthStore((s) => s.accessToken)

  const gameState = useGameStore((s) => s.state)
  const connected = useGameStore((s) => s.connected)
  const socket = useGameStore((s) => s.socket)
  const connect = useGameStore((s) => s.connect)
  const disconnect = useGameStore((s) => s.disconnect)

  const [showDev, setShowDev] = useState(true)

  // Derive tri-state connection status
  const connectionStatus: ConnectionStatus =
    connected ? 'connected' : socket ? 'connecting' : 'disconnected'

  // Connect on mount, disconnect on unmount
  useEffect(() => {
    if (roomId && accessToken) {
      connect(roomId, accessToken)
    }
    return () => {
      disconnect()
    }
  }, [roomId, accessToken, connect, disconnect])

  // Read from gameState when available
  const format = gameState?.format ?? 'Standard'
  const phase = gameState?.phase ?? 'MAIN1'
  const turnNumber = gameState?.turnNumber ?? 1
  const activePlayerId = gameState?.activePlayerId ?? null
  const selfPlayer = gameState?.players?.find((p) => p.playerId === gameState.perspectivePlayerId)
  const opponentPlayer = gameState?.players?.find((p) => p.playerId !== gameState.perspectivePlayerId)
  const handCount = gameState?.self?.hand?.length ?? 7
  const libraryCount = selfPlayer?.libraryCount ?? 40
  const graveyardCount = selfPlayer?.graveyard?.length ?? 0
  const exileCount = selfPlayer?.exile?.length ?? 0
  const oppLibraryCount = opponentPlayer?.libraryCount ?? 40
  const oppGraveyardCount = opponentPlayer?.graveyard?.length ?? 0
  const oppExileCount = opponentPlayer?.exile?.length ?? 0
  const stackItems = gameState?.stack ?? []

  const status = statusConfig[connectionStatus]

  function handleLeave() {
    if (window.confirm('Leave game? You will forfeit.')) {
      disconnect()
      navigate('/lobby')
    }
  }

  return (
    <div className="h-[calc(100vh-48px)] flex flex-col overflow-hidden bg-zinc-950 text-zinc-100">
      {/* ── Top bar ─────────────────────────────────────────────── */}
      <div className="h-12 shrink-0 bg-zinc-900 border-b border-zinc-800 flex items-center justify-between px-4">
        {/* Left: room ID */}
        <span className="font-mono text-sm text-zinc-400">Room: {roomId ?? '—'}</span>

        {/* Center: format badge */}
        <Badge variant="gold">{format}</Badge>

        {/* Right: status + phase + leave */}
        <div className="flex items-center gap-3">
          {/* WebSocket status */}
          <div className="flex items-center gap-1.5">
            <span className={`inline-block w-2 h-2 rounded-full ${status.dot}`} />
            <span className="text-xs text-zinc-400">{status.label}</span>
          </div>

          {/* Phase indicator */}
          <span className="text-xs text-zinc-500 font-mono hidden sm:inline">{phase}</span>

          {/* Leave */}
          <Button variant="ghost" size="sm" onClick={handleLeave} className="border border-red-700 text-red-400 hover:bg-red-900/30">
            Leave Game
          </Button>
        </div>
      </div>

      {/* ── Board area ──────────────────────────────────────────── */}
      <div className="flex-1 flex flex-col bg-board-felt border-x border-board-border overflow-hidden relative">
        {/* Opponent zone */}
        <div className="h-1/3 flex flex-col border-b border-board-border p-2">
          {/* Opponent info */}
          <div className="bg-black/30 rounded px-3 py-1 flex items-center gap-3">
            <PlayerSeat
              name={opponentPlayer?.displayName ?? 'Opponent'}
              life={opponentPlayer?.life ?? 20}
              poison={opponentPlayer?.poisonCounters ?? 0}
              isOpponent
            />
          </div>
          {/* Opponent zone buttons */}
          <div className="flex gap-2 mt-1 px-2">
            <ZoneButton name="Library" count={oppLibraryCount} zone="opp-library" />
            <ZoneButton name="Graveyard" count={oppGraveyardCount} zone="opp-graveyard" />
            <ZoneButton name="Exile" count={oppExileCount} zone="opp-exile" />
            <ZoneButton name="Command" count={0} zone="opp-command" />
          </div>
          {/* Opponent battlefield */}
          <div
            className="flex-1 mt-1 mx-2 border border-dashed border-zinc-600 rounded flex items-center justify-center"
            data-zone="opp-battlefield"
          >
            <span className="text-xs text-zinc-500">Battlefield</span>
          </div>
        </div>

        {/* Stack zone */}
        <div className="h-16 shrink-0 border-t border-b border-board-border bg-black/20 flex items-center justify-center">
          <span className="text-sm text-zinc-500">
            {stackItems.length > 0 ? `Stack (${stackItems.length})` : 'Stack (empty)'}
          </span>
        </div>

        {/* Player zone */}
        <div className="h-1/3 flex flex-col border-t border-board-border p-2">
          {/* Player battlefield */}
          <div
            className="flex-1 mb-1 mx-2 border border-dashed border-zinc-600 rounded flex items-center justify-center"
            data-zone="player-battlefield"
          >
            <span className="text-xs text-zinc-500">Battlefield</span>
          </div>
          {/* Player zone buttons */}
          <div className="flex gap-2 px-2">
            <ZoneButton name="Library" count={libraryCount} zone="library" />
            <ZoneButton name="Graveyard" count={graveyardCount} zone="graveyard" />
            <ZoneButton name="Exile" count={exileCount} zone="exile" />
            <ZoneButton name="Command" count={0} zone="command" />
          </div>
          {/* Player info */}
          <div className="bg-black/30 rounded px-3 py-1 mt-1">
            <PlayerSeat
              name={selfPlayer?.displayName ?? 'You'}
              life={selfPlayer?.life ?? 20}
              poison={selfPlayer?.poisonCounters ?? 0}
              isOpponent={false}
            />
          </div>
        </div>

        {/* ── Dev overlay ────────────────────────────────────────── */}
        <div className="absolute bottom-16 right-4 flex flex-col items-end gap-1">
          <button
            onClick={() => setShowDev((v) => !v)}
            className="text-[10px] text-zinc-500 hover:text-zinc-300 font-mono transition-colors"
          >
            [dev]
          </button>
          {showDev && (
            <div className="bg-zinc-900/90 border border-zinc-700 rounded p-3 text-xs text-zinc-400 space-y-1 max-w-xs">
              <div>Room: <span className="font-mono text-zinc-300">{roomId}</span></div>
              <div>Connection: <span className="font-mono text-zinc-300">{connectionStatus}</span></div>
              <div>Turn: <span className="font-mono text-zinc-300">{turnNumber}</span></div>
              <div>Active player: <span className="font-mono text-zinc-300">{activePlayerId ?? 'You'}</span></div>
              <div className="pt-1 border-t border-zinc-700 text-zinc-500">
                Development scaffold — game engine not connected
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Hand bar ────────────────────────────────────────────── */}
      <div className="h-24 shrink-0 bg-zinc-900/80 border-t border-zinc-800 flex items-center px-4 gap-4">
        {/* Hand cards */}
        <div className="flex-1 flex items-center gap-2 overflow-x-auto min-w-0">
          <div className="flex gap-1.5 shrink-0">
            {Array.from({ length: handCount }).map((_, i) => (
              <div
                key={i}
                className="w-12 h-16 bg-zinc-700 rounded border border-zinc-600 hover:border-yellow-500 transition-colors cursor-pointer shrink-0"
                data-zone="hand"
              />
            ))}
          </div>
          <span className="text-xs text-zinc-500 whitespace-nowrap ml-2">{handCount} cards</span>
        </div>

        {/* Action buttons */}
        <div className="flex items-center gap-2 shrink-0">
          <Button variant="secondary" size="sm">Pass Priority</Button>
          <Button variant="primary" size="sm">End Turn</Button>

          {/* Phase pills */}
          <div className="flex gap-1 ml-2">
            {(['Main 1', 'Combat', 'Main 2'] as const).map((label) => {
              const isActive =
                (label === 'Main 1' && phase === 'MAIN1') ||
                (label === 'Combat' && (
                  phase === 'BEGIN_COMBAT' ||
                  phase === 'DECLARE_ATTACKERS' ||
                  phase === 'DECLARE_BLOCKERS' ||
                  phase === 'COMBAT_DAMAGE' ||
                  phase === 'END_COMBAT'
                )) ||
                (label === 'Main 2' && phase === 'MAIN2')
              return (
                <span
                  key={label}
                  className={`px-2 py-0.5 rounded-full text-xs font-mono transition-colors ${
                    isActive
                      ? 'bg-yellow-600 text-black font-semibold'
                      : 'bg-zinc-800 text-zinc-500'
                  }`}
                >
                  {label}
                </span>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}
