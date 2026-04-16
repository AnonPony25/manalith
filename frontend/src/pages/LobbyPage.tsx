import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Badge } from '@/components/ui/Badge'
import { Spinner } from '@/components/ui/Spinner'
import { useToast } from '@/components/ui/useToast'

// ---------------------------------------------------------------------------
// Mock data — lobby backend not built yet
// ---------------------------------------------------------------------------

interface Room {
  id: string
  name: string
  format: string
  players: number
  maxPlayers: number
  host: string
  isPrivate: boolean
}

const MOCK_ROOMS: Room[] = [
  { id: 'abc123', name: 'Aggro Warriors', format: 'Modern', players: 1, maxPlayers: 2, host: 'SpellSlinger42', isPrivate: false },
  { id: 'def456', name: 'Commander Night', format: 'Commander', players: 3, maxPlayers: 4, host: 'Archmage_Blu', isPrivate: false },
  { id: 'ghi789', name: 'Draft Chaos', format: 'Limited', players: 2, maxPlayers: 2, host: 'CardShark', isPrivate: true },
  { id: 'jkl012', name: 'Pauper League', format: 'Pauper', players: 1, maxPlayers: 2, host: 'BudgetBrew', isPrivate: false },
  { id: 'mno345', name: 'Legacy Vault', format: 'Legacy', players: 0, maxPlayers: 2, host: 'OldSchoolMTG', isPrivate: false },
  { id: 'pqr678', name: 'Standard Grind', format: 'Standard', players: 2, maxPlayers: 4, host: 'MetaDragon', isPrivate: false },
]

const FORMATS = ['Standard', 'Modern', 'Legacy', 'Commander', 'Limited', 'Pauper'] as const
const MAX_PLAYER_OPTIONS = [2, 3, 4] as const

// ---------------------------------------------------------------------------
// Skeleton loader for room cards
// ---------------------------------------------------------------------------

function RoomSkeleton() {
  return (
    <div className="bg-zinc-900 border border-zinc-700 rounded-lg p-4 animate-pulse">
      <div className="h-5 bg-zinc-700 rounded w-2/3 mb-3" />
      <div className="h-4 bg-zinc-700 rounded w-1/3 mb-2" />
      <div className="h-4 bg-zinc-700 rounded w-1/2 mb-4" />
      <div className="h-9 bg-zinc-700 rounded w-full" />
    </div>
  )
}

// ---------------------------------------------------------------------------
// LobbyPage
// ---------------------------------------------------------------------------

export function LobbyPage() {
  const navigate = useNavigate()
  const toast = useToast()

  // Loading simulation
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 1000)
    return () => clearTimeout(timer)
  }, [])

  // Room search filter
  const [searchQuery, setSearchQuery] = useState('')

  // Join by code
  const [roomCode, setRoomCode] = useState('')

  // Create Room modal
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [createForm, setCreateForm] = useState({
    name: '',
    format: 'Standard',
    maxPlayers: 2,
    isPrivate: false,
    password: '',
  })
  const [isCreating, setIsCreating] = useState(false)

  // Filtered rooms
  const filteredRooms = MOCK_ROOMS.filter((room) =>
    room.name.toLowerCase().includes(searchQuery.toLowerCase()),
  )

  // Handlers
  function handleJoinRoom(roomId: string) {
    navigate(`/game/${roomId}`)
  }

  function handleJoinByCode() {
    const code = roomCode.trim().toUpperCase()
    if (code.length !== 6) {
      toast.warning('Please enter a 6-character room code')
      return
    }
    navigate(`/game/${code}`)
  }

  function handleCreateRoom(e: React.FormEvent) {
    e.preventDefault()
    if (!createForm.name.trim()) {
      toast.error('Room name is required')
      return
    }

    setIsCreating(true)
    // Mock creation delay
    setTimeout(() => {
      setIsCreating(false)
      setIsCreateOpen(false)
      setCreateForm({ name: '', format: 'Standard', maxPlayers: 2, isPrivate: false, password: '' })
      toast.success('Room created! Waiting for players...')
    }, 800)
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 p-6 md:p-8">
      {/* Header */}
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <h1 className="font-display text-3xl md:text-4xl text-zinc-100">Game Lobby</h1>
          <Button variant="primary" onClick={() => setIsCreateOpen(true)}>
            Create Room
          </Button>
        </div>

        {/* Join by Code */}
        <div className="bg-zinc-900 border border-zinc-700 rounded-lg p-4 mb-6">
          <h2 className="font-display text-lg text-zinc-100 mb-3">Join by Code</h2>
          <div className="flex gap-3 items-end">
            <div className="flex-1 max-w-xs">
              <Input
                label="Room Code"
                placeholder="ABC123"
                value={roomCode}
                onChange={(e) => setRoomCode(e.target.value.toUpperCase().slice(0, 6))}
                className="font-mono tracking-widest uppercase"
              />
            </div>
            <Button
              variant="primary"
              onClick={handleJoinByCode}
              disabled={roomCode.trim().length !== 6}
            >
              Join
            </Button>
          </div>
        </div>

        {/* Public Rooms Browser */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display text-lg text-zinc-100">Public Rooms</h2>
          </div>
          <div className="mb-4">
            <Input
              placeholder="Search rooms..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              leftIcon={
                <svg className="w-4 h-4 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              }
            />
          </div>

          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <RoomSkeleton />
              <RoomSkeleton />
              <RoomSkeleton />
            </div>
          ) : filteredRooms.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-zinc-400 text-lg">No open rooms. Create one to get started!</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {filteredRooms.map((room) => (
                <div
                  key={room.id}
                  className="bg-zinc-900 border border-zinc-700 rounded-lg p-4 hover:border-zinc-600 transition-colors"
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold text-zinc-100">{room.name}</h3>
                      {room.isPrivate && (
                        <svg className="w-4 h-4 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                        </svg>
                      )}
                    </div>
                    <Badge variant="gold">{room.format}</Badge>
                  </div>
                  <div className="flex items-center gap-4 text-sm text-zinc-400 mb-3">
                    <span className="flex items-center gap-1">
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                      </svg>
                      {room.players}/{room.maxPlayers} Players
                    </span>
                    <span>Host: {room.host}</span>
                  </div>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => handleJoinRoom(room.id)}
                    disabled={room.players >= room.maxPlayers}
                    className="w-full"
                  >
                    {room.players >= room.maxPlayers ? 'Full' : 'Join'}
                  </Button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Create Room Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create Room" size="md">
        <form onSubmit={handleCreateRoom} className="space-y-4">
          <Input
            label="Room Name"
            placeholder="e.g. Friday Night Magic"
            value={createForm.name}
            onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
          />

          <div>
            <label className="block text-sm text-zinc-400 mb-1">Format</label>
            <select
              className="w-full bg-zinc-800 border border-zinc-700 rounded-md text-zinc-100 px-3 py-2 focus:ring-2 focus:ring-yellow-500 focus:border-yellow-500 focus:outline-none"
              value={createForm.format}
              onChange={(e) => setCreateForm((f) => ({ ...f, format: e.target.value }))}
            >
              {FORMATS.map((fmt) => (
                <option key={fmt} value={fmt}>{fmt}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm text-zinc-400 mb-1">Max Players</label>
            <select
              className="w-full bg-zinc-800 border border-zinc-700 rounded-md text-zinc-100 px-3 py-2 focus:ring-2 focus:ring-yellow-500 focus:border-yellow-500 focus:outline-none"
              value={createForm.maxPlayers}
              onChange={(e) => setCreateForm((f) => ({ ...f, maxPlayers: Number(e.target.value) }))}
            >
              {MAX_PLAYER_OPTIONS.map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="private-toggle"
              className="w-4 h-4 rounded bg-zinc-800 border-zinc-700 text-yellow-600 focus:ring-yellow-500"
              checked={createForm.isPrivate}
              onChange={(e) => setCreateForm((f) => ({ ...f, isPrivate: e.target.checked }))}
            />
            <label htmlFor="private-toggle" className="text-sm text-zinc-300">
              Private Room
            </label>
          </div>

          {createForm.isPrivate && (
            <Input
              label="Password"
              type="password"
              placeholder="Room password"
              value={createForm.password}
              onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
            />
          )}

          <div className="flex gap-3 justify-end pt-2">
            <Button variant="secondary" type="button" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" loading={isCreating}>
              Create Room
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
