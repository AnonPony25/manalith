import { useNavigate } from 'react-router-dom'
import { DeckSummaryDTO } from '@/types/deck'

interface DeckCardProps {
  deck: DeckSummaryDTO
  onClick?: () => void
}

const FORMAT_COLORS: Record<string, string> = {
  standard:    'bg-green-700 text-green-100',
  pioneer:     'bg-blue-700 text-blue-100',
  modern:      'bg-purple-700 text-purple-100',
  legacy:      'bg-orange-700 text-orange-100',
  vintage:     'bg-yellow-600 text-yellow-100',
  commander:   'bg-red-700 text-red-100',
  pauper:      'bg-gray-600 text-gray-100',
  draft:       'bg-zinc-600 text-zinc-100',
  sealed:      'bg-zinc-600 text-zinc-100',
  custom:      'bg-zinc-600 text-zinc-100',
}

export function DeckCard({ deck, onClick }: DeckCardProps) {
  const navigate = useNavigate()

  function handleClick() {
    if (onClick) onClick()
    navigate(`/decks/${deck.id}`)
  }

  const formatColor = FORMAT_COLORS[deck.format.toLowerCase()] ?? 'bg-zinc-600 text-zinc-100'
  const totalCards = deck.mainboardCount + deck.sideboardCount

  return (
    <div
      onClick={handleClick}
      className="
        bg-zinc-900 border border-zinc-700 rounded-lg p-4 cursor-pointer
        transition-all duration-200
        hover:border-yellow-500 hover:shadow-lg hover:shadow-yellow-500/10
        group
      "
    >
      {/* Header row */}
      <div className="flex items-start justify-between gap-2 mb-3">
        <h3 className="font-display text-lg text-zinc-100 leading-tight line-clamp-2 group-hover:text-yellow-400 transition-colors">
          {deck.name}
        </h3>
        <span className={`shrink-0 text-xs font-semibold px-2 py-0.5 rounded-full capitalize ${formatColor}`}>
          {deck.format}
        </span>
      </div>

      {/* Description */}
      {deck.description && (
        <p className="text-zinc-400 text-sm mb-3 line-clamp-2">{deck.description}</p>
      )}

      {/* Footer row */}
      <div className="flex items-center justify-between text-xs text-zinc-400 mt-auto pt-2 border-t border-zinc-800">
        <span>{totalCards} cards</span>
        <div className="flex items-center gap-2">
          {deck.sideboardCount > 0 && (
            <span className="text-zinc-500">SB: {deck.sideboardCount}</span>
          )}
          <span
            className={`px-1.5 py-0.5 rounded text-xs font-medium ${
              deck.isPublic
                ? 'bg-green-900/60 text-green-400'
                : 'bg-zinc-800 text-zinc-500'
            }`}
          >
            {deck.isPublic ? 'Public' : 'Private'}
          </span>
        </div>
      </div>
    </div>
  )
}
