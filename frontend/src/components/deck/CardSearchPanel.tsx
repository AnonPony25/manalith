import { useState, useEffect, useRef } from 'react'
import { useQuery } from '@tanstack/react-query'
import { cardApi } from '@/api/cardApi'
import { CardPrinting } from '@/types/card'
import { CardSearchResult } from './CardSearchResult'

interface CardSearchPanelProps {
  onAddCard: (cardId: string, isCommander?: boolean, isSideboard?: boolean) => void
  format: string
}

function Spinner() {
  return (
    <div className="flex items-center justify-center py-8">
      <div className="w-8 h-8 rounded-full border-2 border-zinc-600 border-t-yellow-500 animate-spin" />
    </div>
  )
}

export function CardSearchPanel({ onAddCard, format: _format }: CardSearchPanelProps) {
  const [inputValue, setInputValue] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [page, setPage] = useState(1)
  const [accumulatedCards, setAccumulatedCards] = useState<CardPrinting[]>([])
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Debounce input with 300ms delay
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setDebouncedQuery(inputValue.trim())
      setPage(1)
      setAccumulatedCards([])
    }, 300)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [inputValue])

  const { data, isFetching, isError } = useQuery({
    queryKey: ['card-search', debouncedQuery, page],
    queryFn: () => cardApi.search(debouncedQuery, page),
    enabled: debouncedQuery.length >= 2,
    select: (res) => res.data,
  })

  // Accumulate cards across pages
  useEffect(() => {
    if (!data) return
    if (page === 1) {
      setAccumulatedCards(data.cards)
    } else {
      setAccumulatedCards((prev) => [...prev, ...data.cards])
    }
  }, [data, page])

  const hasMore = data ? accumulatedCards.length < data.total : false

  function handleAdd(cardId: string, sideboard: boolean) {
    onAddCard(cardId, false, sideboard)
  }

  function handleLoadMore() {
    setPage((p) => p + 1)
  }

  function handleClear() {
    setInputValue('')
    setDebouncedQuery('')
    setAccumulatedCards([])
    setPage(1)
  }

  return (
    <div className="flex flex-col h-full bg-zinc-950 border-r border-zinc-800">
      {/* Search header */}
      <div className="px-3 py-3 border-b border-zinc-800">
        <h2 className="font-display text-sm text-yellow-400 mb-2 uppercase tracking-wider">
          Card Search
        </h2>
        <div className="relative">
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Search cards…"
            className="w-full bg-zinc-800 border border-zinc-600 text-zinc-100 focus:border-yellow-500 focus:outline-none rounded px-3 py-2 placeholder-zinc-500 text-sm pr-8"
          />
          {inputValue && (
            <button
              onClick={handleClear}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 text-lg leading-none"
              aria-label="Clear search"
            >
              ×
            </button>
          )}
        </div>
        {debouncedQuery && data && (
          <p className="text-zinc-500 text-xs mt-1">
            {data.total} result{data.total !== 1 ? 's' : ''}
          </p>
        )}
      </div>

      {/* Results area */}
      <div className="flex-1 overflow-y-auto p-3">
        {/* Empty state */}
        {!debouncedQuery && (
          <div className="flex flex-col items-center justify-center h-48 text-center">
            <p className="text-zinc-500 text-sm">Search for cards to add to your deck</p>
            <p className="text-zinc-600 text-xs mt-1">Type at least 2 characters</p>
          </div>
        )}

        {/* Loading */}
        {isFetching && page === 1 && <Spinner />}

        {/* Error */}
        {isError && (
          <p className="text-red-400 text-sm text-center py-4">
            Search failed. Try again.
          </p>
        )}

        {/* Results grid */}
        {debouncedQuery && accumulatedCards.length > 0 && (
          <>
            <div className="grid grid-cols-2 gap-2">
              {accumulatedCards.map((card) => (
                <CardSearchResult key={card.id} card={card} onAdd={handleAdd} />
              ))}
            </div>

            {/* Load more */}
            {hasMore && (
              <div className="text-center mt-4">
                <button
                  onClick={handleLoadMore}
                  disabled={isFetching}
                  className="bg-zinc-800 hover:bg-zinc-700 disabled:opacity-50 text-zinc-300 text-sm px-4 py-2 rounded-lg transition-colors"
                >
                  {isFetching ? 'Loading…' : 'Load more'}
                </button>
              </div>
            )}

            {isFetching && page > 1 && (
              <div className="flex justify-center mt-4">
                <div className="w-6 h-6 rounded-full border-2 border-zinc-600 border-t-yellow-500 animate-spin" />
              </div>
            )}
          </>
        )}

        {/* No results */}
        {debouncedQuery && !isFetching && !isError && accumulatedCards.length === 0 && (
          <p className="text-zinc-500 text-sm text-center py-8">
            No cards found for "{debouncedQuery}"
          </p>
        )}
      </div>
    </div>
  )
}
