import React, { useState } from 'react'
import { DeckDetailDTO, DeckEntryDTO } from '@/types/deck'

interface DeckListPanelProps {
  deck: DeckDetailDTO
  onRemoveCard: (cardId: string, sideboard: boolean) => void
  onUpdateQty: (cardId: string, qty: number, sideboard: boolean) => void
}

const TYPE_ORDER = [
  'Creature',
  'Planeswalker',
  'Instant',
  'Sorcery',
  'Enchantment',
  'Artifact',
  'Land',
]

function getTypeGroup(typeLine: string): string {
  for (const t of TYPE_ORDER) {
    if (typeLine.includes(t)) return t
  }
  return 'Other'
}

function groupByType(entries: DeckEntryDTO[]): Record<string, DeckEntryDTO[]> {
  const groups: Record<string, DeckEntryDTO[]> = {}
  for (const entry of entries) {
    const group = getTypeGroup(entry.typeLine)
    if (!groups[group]) groups[group] = []
    groups[group].push(entry)
  }
  return groups
}

function sumQty(entries: DeckEntryDTO[]): number {
  return entries.reduce((acc, e) => acc + e.quantity, 0)
}

interface CardRowProps {
  entry: DeckEntryDTO
  sideboard: boolean
  onRemove: (cardId: string, sideboard: boolean) => void
  onUpdateQty: (cardId: string, qty: number, sideboard: boolean) => void
}

function CardRow({ entry, sideboard, onRemove, onUpdateQty }: CardRowProps) {
  const [showTooltip, setShowTooltip] = useState(false)

  return (
    <div
      className="flex items-center gap-2 px-2 py-1 rounded hover:bg-zinc-800/50 group relative"
      onMouseEnter={() => setShowTooltip(true)}
      onMouseLeave={() => setShowTooltip(false)}
    >
      {/* Image tooltip */}
      {showTooltip && entry.imageUri && (
        <div className="absolute left-full top-0 z-50 ml-2 pointer-events-none">
          <img
            src={entry.imageUri}
            alt={entry.cardName}
            className="w-32 rounded-lg shadow-xl border border-zinc-600"
          />
        </div>
      )}

      {/* Quantity controls */}
      <div className="flex items-center gap-1 shrink-0">
        <button
          onClick={() => {
            if (entry.quantity > 1) {
              onUpdateQty(entry.cardId, entry.quantity - 1, sideboard)
            }
          }}
          disabled={entry.quantity <= 1}
          className="w-5 h-5 flex items-center justify-center rounded bg-zinc-700 hover:bg-zinc-600 disabled:opacity-30 disabled:cursor-not-allowed text-zinc-300 text-xs transition-colors"
          aria-label="Decrease quantity"
        >
          −
        </button>
        <span className="w-5 text-center text-sm text-zinc-200 font-mono font-semibold">
          {entry.quantity}
        </span>
        <button
          onClick={() => onUpdateQty(entry.cardId, entry.quantity + 1, sideboard)}
          className="w-5 h-5 flex items-center justify-center rounded bg-zinc-700 hover:bg-zinc-600 text-zinc-300 text-xs transition-colors"
          aria-label="Increase quantity"
        >
          +
        </button>
      </div>

      {/* Card name */}
      <span className="flex-1 text-sm text-zinc-200 truncate">{entry.cardName}</span>

      {/* Set code */}
      <span className="text-xs text-zinc-500 uppercase font-mono shrink-0">{entry.setCode}</span>

      {/* Remove button */}
      <button
        onClick={() => onRemove(entry.cardId, sideboard)}
        className="w-5 h-5 flex items-center justify-center text-zinc-600 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all text-base leading-none shrink-0"
        aria-label={`Remove ${entry.cardName}`}
      >
        ×
      </button>
    </div>
  )
}

interface SectionProps {
  label: string
  count: number
  children: React.ReactNode
  defaultOpen?: boolean
}

function Section({ label, count, children, defaultOpen = true }: SectionProps) {
  const [open, setOpen] = useState(defaultOpen)

  return (
    <div className="mb-2">
      <button
        onClick={() => setOpen((o) => !o)}
        className="w-full flex items-center justify-between px-2 py-1.5 hover:bg-zinc-800/50 rounded group"
      >
        <span className="font-display text-sm text-yellow-400 tracking-wide">{label}</span>
        <span className="text-xs text-zinc-500">
          {count} {open ? '▲' : '▼'}
        </span>
      </button>
      {open && <div>{children}</div>}
    </div>
  )
}

export function DeckListPanel({ deck, onRemoveCard, onUpdateQty }: DeckListPanelProps) {
  const mainboardGroups = groupByType(deck.mainboard)
  const mainboardTotal = sumQty(deck.mainboard)
  const sideboardTotal = sumQty(deck.sideboard)
  const commanderTotal = deck.commanders.length

  return (
    <div className="flex flex-col h-full bg-zinc-950 overflow-y-auto">
      {/* Panel header */}
      <div className="sticky top-0 bg-zinc-950/95 border-b border-zinc-800 px-3 py-3 z-10">
        <h2 className="font-display text-sm text-yellow-400 uppercase tracking-wider">
          Deck List
        </h2>
        <p className="text-xs text-zinc-500 mt-0.5">
          {mainboardTotal} mainboard
          {sideboardTotal > 0 && ` · ${sideboardTotal} sideboard`}
        </p>
      </div>

      <div className="p-3 space-y-1">
        {/* Commanders */}
        {deck.commanders.length > 0 && (
          <Section label={`Commanders`} count={commanderTotal}>
            {deck.commanders.map((entry) => (
              <CardRow
                key={entry.cardId}
                entry={entry}
                sideboard={false}
                onRemove={onRemoveCard}
                onUpdateQty={onUpdateQty}
              />
            ))}
          </Section>
        )}

        {/* Mainboard by type group */}
        {TYPE_ORDER.concat(['Other']).map((type) => {
          const entries = mainboardGroups[type]
          if (!entries || entries.length === 0) return null
          const typeCount = sumQty(entries)
          return (
            <Section key={type} label={`${type}s`} count={typeCount}>
              {entries.map((entry) => (
                <CardRow
                  key={entry.cardId}
                  entry={entry}
                  sideboard={false}
                  onRemove={onRemoveCard}
                  onUpdateQty={onUpdateQty}
                />
              ))}
            </Section>
          )
        })}

        {/* Sideboard */}
        {deck.sideboard.length > 0 && (
          <Section label="Sideboard" count={sideboardTotal}>
            {deck.sideboard.map((entry) => (
              <CardRow
                key={entry.cardId}
                entry={entry}
                sideboard={true}
                onRemove={onRemoveCard}
                onUpdateQty={onUpdateQty}
              />
            ))}
          </Section>
        )}

        {/* Empty state */}
        {deck.mainboard.length === 0 && deck.commanders.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <p className="text-zinc-500 text-sm">Your deck is empty.</p>
            <p className="text-zinc-600 text-xs mt-1">Search for cards to add them here.</p>
          </div>
        )}
      </div>
    </div>
  )
}
