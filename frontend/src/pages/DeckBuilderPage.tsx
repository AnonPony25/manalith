import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { deckApi } from '@/api/deckApi'
import { DeckSummaryDTO } from '@/types/deck'
import { DeckCard, CreateDeckModal } from '@/components/deck'

function SkeletonDeckCard() {
  return (
    <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-4 animate-pulse">
      <div className="flex items-start justify-between mb-3">
        <div className="h-5 bg-zinc-700 rounded w-2/3" />
        <div className="h-5 bg-zinc-700 rounded w-16" />
      </div>
      <div className="h-3 bg-zinc-800 rounded w-full mb-2" />
      <div className="h-3 bg-zinc-800 rounded w-3/4 mb-4" />
      <div className="flex justify-between pt-2 border-t border-zinc-800">
        <div className="h-3 bg-zinc-700 rounded w-16" />
        <div className="h-3 bg-zinc-700 rounded w-12" />
      </div>
    </div>
  )
}

export function DeckBuilderPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [isCreateOpen, setIsCreateOpen] = useState(false)

  const { data: decks, isLoading, isError } = useQuery({
    queryKey: ['my-decks'],
    queryFn: () => deckApi.getMyDecks(),
  })

  function handleCreated(deck: DeckSummaryDTO) {
    queryClient.invalidateQueries({ queryKey: ['my-decks'] })
    setIsCreateOpen(false)
    navigate(`/decks/${deck.id}`)
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      {/* Top bar */}
      <div className="border-b border-zinc-800 bg-zinc-900 px-6 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <h1 className="font-display text-3xl text-yellow-400 tracking-wide">My Decks</h1>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="bg-yellow-600 hover:bg-yellow-500 text-black font-semibold px-4 py-2 rounded-lg transition-colors"
          >
            + New Deck
          </button>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-8">
        {/* Error state */}
        {isError && (
          <div className="bg-red-900/40 border border-red-700 text-red-300 rounded-lg px-4 py-3 mb-6">
            Failed to load decks. Please try again.
          </div>
        )}

        {/* Loading skeleton */}
        {isLoading && (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
            <SkeletonDeckCard />
            <SkeletonDeckCard />
            <SkeletonDeckCard />
          </div>
        )}

        {/* Empty state */}
        {!isLoading && !isError && decks && decks.length === 0 && (
          <div className="flex flex-col items-center justify-center py-24 text-center">
            <div className="text-6xl mb-4">🃏</div>
            <h2 className="font-display text-2xl text-zinc-300 mb-2">No decks yet</h2>
            <p className="text-zinc-500 mb-6">Create your first deck to get started.</p>
            <button
              onClick={() => setIsCreateOpen(true)}
              className="bg-yellow-600 hover:bg-yellow-500 text-black font-semibold px-6 py-3 rounded-lg transition-colors"
            >
              Create your first deck
            </button>
          </div>
        )}

        {/* Deck grid */}
        {!isLoading && !isError && decks && decks.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
            {decks.map((deck) => (
              <DeckCard key={deck.id} deck={deck} />
            ))}
          </div>
        )}
      </div>

      <CreateDeckModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onCreated={handleCreated}
      />
    </div>
  )
}
