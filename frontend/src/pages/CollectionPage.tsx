import { useState, useEffect, useCallback, type ChangeEvent } from 'react'
import { cardApi } from '@/api/cardApi'
import { CardPrinting } from '@/types/card'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'
import Badge from '@/components/ui/Badge'
import Spinner from '@/components/ui/Spinner'

// ---------------------------------------------------------------------------
// Color filter config
// ---------------------------------------------------------------------------

interface ManaColor {
  code: string
  label: string
  scryfallKey: string
  cssColor: string
}

const MANA_COLORS: ManaColor[] = [
  { code: 'W', label: 'White', scryfallKey: 'W', cssColor: 'bg-yellow-100' },
  { code: 'U', label: 'Blue', scryfallKey: 'U', cssColor: 'bg-blue-500' },
  { code: 'B', label: 'Black', scryfallKey: 'B', cssColor: 'bg-zinc-800 border border-zinc-500' },
  { code: 'R', label: 'Red', scryfallKey: 'R', cssColor: 'bg-red-600' },
  { code: 'G', label: 'Green', scryfallKey: 'G', cssColor: 'bg-green-600' },
  { code: 'C', label: 'Colorless', scryfallKey: 'C', cssColor: 'bg-zinc-500' },
]

const CARD_TYPES = [
  'All',
  'Creature',
  'Instant',
  'Sorcery',
  'Enchantment',
  'Artifact',
  'Planeswalker',
  'Land',
] as const

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function getCardImage(card: CardPrinting): string | null {
  if (card.imageUris?.normal) return card.imageUris.normal
  if (card.cardFaces?.[0]?.imageUris?.normal) return card.cardFaces[0].imageUris.normal
  return null
}

function buildQuery(name: string, colors: string[], type: string): string {
  const parts: string[] = []
  if (name.trim()) parts.push(name.trim())
  for (const c of colors) {
    parts.push(`color:${c}`)
  }
  if (type !== 'All') parts.push(`type:${type.toLowerCase()}`)
  return parts.join(' ') || 'lightning bolt'
}

// ---------------------------------------------------------------------------
// CollectionPage
// ---------------------------------------------------------------------------

export function CollectionPage() {

  // Search state
  const [nameQuery, setNameQuery] = useState('')
  const [selectedColors, setSelectedColors] = useState<string[]>([])
  const [selectedType, setSelectedType] = useState('All')

  // Results
  const [cards, setCards] = useState<CardPrinting[]>([])
  const [totalCards, setTotalCards] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Search function
  const performSearch = useCallback(async (query: string) => {
    setLoading(true)
    setError(null)
    try {
      const response = await cardApi.search(query)
      setCards(response.data.cards)
      setTotalCards(response.data.total)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load cards'
      setError(message)
      setCards([])
      setTotalCards(0)
    } finally {
      setLoading(false)
    }
  }, [])

  // Initial load
  useEffect(() => {
    performSearch('lightning bolt')
  }, [performSearch])

  // Handle search submit
  function handleSearch(e?: React.FormEvent) {
    e?.preventDefault()
    const query = buildQuery(nameQuery, selectedColors, selectedType)
    performSearch(query)
  }

  // Toggle color filter
  function toggleColor(code: string) {
    setSelectedColors((prev) =>
      prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code],
    )
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 p-6 md:p-8">
      <div className="max-w-7xl mx-auto">
        {/* Stats Header */}
        <div className="mb-8">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
            <h1 className="font-display text-3xl md:text-4xl text-zinc-100">My Collection</h1>
            <Badge variant="info">Collection tracking coming in Phase 5</Badge>
          </div>
          <div className="flex flex-wrap gap-3">
            <div className="bg-zinc-900 border border-zinc-700 rounded-lg px-4 py-2">
              <span className="text-sm text-zinc-400">Total Cards</span>
              <p className="text-lg font-semibold text-zinc-100">0</p>
            </div>
            <div className="bg-zinc-900 border border-zinc-700 rounded-lg px-4 py-2">
              <span className="text-sm text-zinc-400">Unique Cards</span>
              <p className="text-lg font-semibold text-zinc-100">0</p>
            </div>
            <div className="bg-zinc-900 border border-zinc-700 rounded-lg px-4 py-2">
              <span className="text-sm text-zinc-400">Est. Value</span>
              <p className="text-lg font-semibold text-zinc-100">&mdash;</p>
            </div>
          </div>
        </div>

        {/* Search + Filters */}
        <form
          onSubmit={handleSearch}
          className="bg-zinc-900 border border-zinc-700 rounded-lg p-4 mb-6"
        >
          <div className="flex flex-col md:flex-row gap-4 mb-4">
            <div className="flex-1">
              <Input
                label="Card Name"
                placeholder="Search cards..."
                value={nameQuery}
                onChange={(e: ChangeEvent<HTMLInputElement>) => setNameQuery(e.target.value)}
                leftIcon={
                  <svg className="w-4 h-4 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                }
              />
            </div>
            <div className="min-w-[160px]">
              <label className="block text-sm text-zinc-400 mb-1">Type</label>
              <select
                className="w-full bg-zinc-800 border border-zinc-700 rounded-md text-zinc-100 px-3 py-2 focus:ring-2 focus:ring-yellow-500 focus:border-yellow-500 focus:outline-none"
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
              >
                {CARD_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Color Filter */}
          <div className="flex flex-wrap items-center gap-3 mb-4">
            <span className="text-sm text-zinc-400">Colors:</span>
            {MANA_COLORS.map((color) => (
              <button
                key={color.code}
                type="button"
                onClick={() => toggleColor(color.code)}
                className={`
                  w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold
                  transition-all duration-150
                  ${color.cssColor}
                  ${selectedColors.includes(color.code)
                    ? 'ring-2 ring-yellow-500 ring-offset-2 ring-offset-zinc-900 scale-110'
                    : 'opacity-50 hover:opacity-80'}
                `}
                title={color.label}
              >
                {color.code}
              </button>
            ))}
          </div>

          <Button variant="primary" type="submit" loading={loading}>
            Search
          </Button>
        </form>

        {/* Results count */}
        {!loading && !error && (
          <p className="text-sm text-zinc-400 mb-4">
            {totalCards} card{totalCards !== 1 ? 's' : ''} found
          </p>
        )}

        {/* Card Grid */}
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Spinner size="lg" />
          </div>
        ) : error ? (
          <div className="text-center py-16">
            <p className="text-red-400 text-lg mb-2">Failed to load cards</p>
            <p className="text-zinc-500 text-sm">{error}</p>
            <Button variant="secondary" className="mt-4" onClick={() => performSearch('lightning bolt')}>
              Retry
            </Button>
          </div>
        ) : cards.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-zinc-400 text-lg">No cards found. Try a different search.</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {cards.map((card) => {
              const imageUrl = getCardImage(card)
              return (
                <div key={card.id} className="group relative">
                  <div
                    className="relative rounded-lg overflow-hidden transition-transform duration-150 group-hover:scale-105 group-hover:ring-2 group-hover:ring-yellow-500"
                    style={{ aspectRatio: '2.5 / 3.5' }}
                  >
                    {imageUrl ? (
                      <img
                        src={imageUrl}
                        alt={card.name}
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                    ) : (
                      <div className="w-full h-full bg-zinc-800 flex items-center justify-center">
                        <span className="text-zinc-500 text-xs text-center px-2">{card.name}</span>
                      </div>
                    )}

                    {/* Hover overlay */}
                    <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/80 to-transparent p-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <p className="text-xs font-semibold text-zinc-100 truncate">{card.name}</p>
                      {card.manaCost && (
                        <p className="text-xs text-zinc-400 font-mono">{card.manaCost}</p>
                      )}
                    </div>
                  </div>

                  {/* Add to Collection button */}
                  <button
                    disabled
                    className="mt-1 w-full text-xs py-1 rounded-md bg-zinc-800 text-zinc-500 cursor-not-allowed"
                    title="Coming in Phase 5"
                  >
                    Add to Collection
                  </button>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
