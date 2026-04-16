import React, { useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { deckApi } from '@/api/deckApi'
import { useDeckStore } from '@/store/deckStore'
import {
  CardSearchPanel,
  DeckListPanel,
  DeckStatsPanel,
  DeckExportModal,
  DeckImportModal,
} from '@/components/deck'
import { useState } from 'react'
import { AddCardRequest } from '@/types/deck'

function Spinner() {
  return (
    <div className="min-h-screen bg-zinc-950 flex items-center justify-center">
      <div className="w-12 h-12 rounded-full border-2 border-zinc-600 border-t-yellow-500 animate-spin" />
    </div>
  )
}

export function DeckEditorPage() {
  const { id: deckId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { setActiveDeck, updateActiveDeck } = useDeckStore()
  const [isExportOpen, setIsExportOpen] = useState(false)
  const [isImportOpen, setIsImportOpen] = useState(false)

  // Keyboard shortcut: ESC to clear search focus (handled in CardSearchPanel)
  // We add a page-level handler for navigation hints
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        // Blur any focused input (delegates to CardSearchPanel's own state)
        const active = document.activeElement as HTMLElement | null
        active?.blur()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  const {
    data: deck,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['deck', deckId],
    queryFn: () => deckApi.getDeck(deckId!),
    enabled: Boolean(deckId),
  })

  // Sync to Zustand store when deck loads
  useEffect(() => {
    if (deck) setActiveDeck(deck)
    return () => setActiveDeck(null)
  }, [deck, setActiveDeck])

  // Add card mutation
  const addCardMutation = useMutation({
    mutationFn: (req: AddCardRequest) => deckApi.addCard(deckId!, req),
    onSuccess: (updatedDeck) => {
      updateActiveDeck(updatedDeck)
      queryClient.setQueryData(['deck', deckId], updatedDeck)
    },
  })

  // Remove card mutation
  const removeCardMutation = useMutation({
    mutationFn: ({ cardId, sideboard }: { cardId: string; sideboard: boolean }) =>
      deckApi.removeCard(deckId!, cardId, sideboard),
    onSuccess: (updatedDeck) => {
      updateActiveDeck(updatedDeck)
      queryClient.setQueryData(['deck', deckId], updatedDeck)
    },
  })

  // Update card quantity mutation
  const updateQtyMutation = useMutation({
    mutationFn: ({
      cardId,
      quantity,
      sideboard,
    }: {
      cardId: string
      quantity: number
      sideboard: boolean
    }) => deckApi.updateCardQty(deckId!, cardId, quantity, sideboard),
    onSuccess: (updatedDeck) => {
      updateActiveDeck(updatedDeck)
      queryClient.setQueryData(['deck', deckId], updatedDeck)
    },
  })

  const handleAddCard = useCallback(
    (cardId: string, isCommander = false, isSideboard = false) => {
      addCardMutation.mutate({
        cardId,
        quantity: 1,
        isCommander,
        isSideboard,
      })
    },
    [addCardMutation]
  )

  const handleRemoveCard = useCallback(
    (cardId: string, sideboard: boolean) => {
      removeCardMutation.mutate({ cardId, sideboard })
    },
    [removeCardMutation]
  )

  const handleUpdateQty = useCallback(
    (cardId: string, qty: number, sideboard: boolean) => {
      updateQtyMutation.mutate({ cardId, quantity: qty, sideboard })
    },
    [updateQtyMutation]
  )

  if (isLoading) return <Spinner />

  if (isError || !deck) {
    return (
      <div className="min-h-screen bg-zinc-950 flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-400 text-lg mb-4">Failed to load deck.</p>
          <button
            onClick={() => navigate('/decks')}
            className="bg-yellow-600 hover:bg-yellow-500 text-black font-semibold px-4 py-2 rounded-lg"
          >
            Back to My Decks
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col">
      {/* Top bar */}
      <div className="border-b border-zinc-800 bg-zinc-900 px-4 py-3 flex items-center gap-4 shrink-0">
        <button
          onClick={() => navigate('/decks')}
          className="text-zinc-400 hover:text-zinc-100 text-sm transition-colors"
          aria-label="Back to My Decks"
        >
          ← My Decks
        </button>

        <div className="flex-1 min-w-0">
          <h1 className="font-display text-xl text-yellow-400 truncate">{deck.name}</h1>
          <p className="text-xs text-zinc-500 capitalize">
            {deck.format}
            {' · '}
            {deck.isPublic ? 'Public' : 'Private'}
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {/* Mutation loading indicators */}
          {(addCardMutation.isPending || removeCardMutation.isPending || updateQtyMutation.isPending) && (
            <div className="w-4 h-4 rounded-full border-2 border-zinc-600 border-t-yellow-500 animate-spin" />
          )}

          <button
            onClick={() => setIsImportOpen(true)}
            className="bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm font-medium px-3 py-1.5 rounded-lg transition-colors"
          >
            Import
          </button>
          <button
            onClick={() => setIsExportOpen(true)}
            className="bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm font-medium px-3 py-1.5 rounded-lg transition-colors"
          >
            Export
          </button>
        </div>

        {/* Keyboard hint */}
        <span className="hidden lg:block text-xs text-zinc-600">ESC to clear search</span>
      </div>

      {/* Three-column editor layout */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left: Card Search (30%) */}
        <div className="w-[30%] min-w-[240px] overflow-hidden flex flex-col border-r border-zinc-800">
          <CardSearchPanel onAddCard={handleAddCard} format={deck.format} />
        </div>

        {/* Center: Deck List (50%) */}
        <div className="flex-1 overflow-hidden flex flex-col">
          <DeckListPanel
            deck={deck}
            onRemoveCard={handleRemoveCard}
            onUpdateQty={handleUpdateQty}
          />
        </div>

        {/* Right: Stats (20%) */}
        <div className="w-[20%] min-w-[180px] overflow-hidden flex flex-col border-l border-zinc-800">
          <DeckStatsPanel stats={deck.stats} />
        </div>
      </div>

      {/* Export modal */}
      <DeckExportModal
        isOpen={isExportOpen}
        onClose={() => setIsExportOpen(false)}
        deckId={deck.id}
      />

      {/* Import modal */}
      <DeckImportModal
        isOpen={isImportOpen}
        onClose={() => setIsImportOpen(false)}
        onImported={(importedDeck) => {
          // Navigate to the newly imported deck
          navigate(`/decks/${importedDeck.id}`)
        }}
      />
    </div>
  )
}
