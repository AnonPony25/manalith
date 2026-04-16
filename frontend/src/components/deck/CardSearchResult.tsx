import { useState } from 'react'
import { CardPrinting } from '@/types/card'

interface CardSearchResultProps {
  card: CardPrinting
  onAdd: (cardId: string, sideboard: boolean) => void
}

const MANA_SYMBOL_CLASSES: Record<string, string> = {
  W: 'bg-yellow-50 border-yellow-300 text-yellow-900',
  U: 'bg-blue-600 border-blue-400 text-white',
  B: 'bg-zinc-800 border-zinc-500 text-zinc-100',
  R: 'bg-red-600 border-red-400 text-white',
  G: 'bg-green-600 border-green-400 text-white',
  C: 'bg-zinc-500 border-zinc-400 text-white',
}

const RARITY_CLASSES: Record<string, string> = {
  mythic:   'bg-orange-700 text-orange-100',
  rare:     'bg-yellow-600 text-yellow-100',
  uncommon: 'bg-zinc-400 text-zinc-900',
  common:   'bg-zinc-600 text-zinc-200',
  special:  'bg-purple-700 text-purple-100',
  bonus:    'bg-pink-700 text-pink-100',
}

function ManaSymbol({ symbol }: { symbol: string }) {
  const classes = MANA_SYMBOL_CLASSES[symbol]
  if (classes) {
    return (
      <span
        className={`inline-flex items-center justify-center w-4 h-4 rounded-full border text-[9px] font-bold ${classes}`}
      >
        {symbol}
      </span>
    )
  }
  // Numeric / X / other
  return (
    <span className="inline-flex items-center justify-center w-4 h-4 rounded-full border border-zinc-500 bg-zinc-700 text-zinc-200 text-[9px] font-bold">
      {symbol}
    </span>
  )
}

function ManaCost({ cost }: { cost: string | null }) {
  if (!cost) return null
  const symbols = cost.match(/\{([^}]+)\}/g)?.map((m) => m.slice(1, -1)) ?? []
  return (
    <span className="flex items-center gap-0.5 flex-wrap">
      {symbols.map((sym, i) => (
        <ManaSymbol key={i} symbol={sym} />
      ))}
    </span>
  )
}

export function CardSearchResult({ card, onAdd }: CardSearchResultProps) {
  const [hovered, setHovered] = useState(false)

  const imageUri =
    card.imageUris?.normal ??
    card.cardFaces?.[0]?.imageUris?.normal ??
    null

  const rarityClass = RARITY_CLASSES[card.rarity] ?? 'bg-zinc-600 text-zinc-200'

  return (
    <div
      className="relative bg-zinc-900 border border-zinc-700 rounded-lg overflow-hidden group"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {/* Card image */}
      <div className="aspect-[5/7] bg-zinc-800 relative overflow-hidden">
        {imageUri ? (
          <img
            src={imageUri}
            alt={card.name}
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-zinc-600 text-xs text-center p-2">
            No image
          </div>
        )}

        {/* Hover overlay with add buttons */}
        {hovered && (
          <div className="absolute inset-0 bg-black/70 flex flex-col items-center justify-center gap-2 p-2">
            <button
              onClick={() => onAdd(card.id, false)}
              className="w-full bg-yellow-600 hover:bg-yellow-500 text-black text-xs font-semibold px-2 py-1.5 rounded transition-colors"
            >
              + Add to Deck
            </button>
            <button
              onClick={() => onAdd(card.id, true)}
              className="w-full bg-zinc-700 hover:bg-zinc-600 text-zinc-100 text-xs font-semibold px-2 py-1.5 rounded transition-colors"
            >
              + Sideboard
            </button>
          </div>
        )}
      </div>

      {/* Card info */}
      <div className="p-2 space-y-1">
        <div className="flex items-start justify-between gap-1">
          <p className="text-zinc-100 text-xs font-medium leading-tight line-clamp-1 flex-1">
            {card.name}
          </p>
          <ManaCost cost={card.manaCost} />
        </div>
        <p className="text-zinc-500 text-[10px] leading-tight line-clamp-1">{card.typeLine}</p>
        <div className="flex items-center justify-between">
          <span className="text-zinc-600 text-[10px] uppercase">{card.setCode}</span>
          <span className={`text-[10px] font-semibold px-1 py-0.5 rounded capitalize ${rarityClass}`}>
            {card.rarity.charAt(0).toUpperCase()}
          </span>
        </div>
      </div>
    </div>
  )
}
