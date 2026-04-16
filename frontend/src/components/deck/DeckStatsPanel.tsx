import { DeckStatsDTO } from '@/types/deck'

interface DeckStatsPanelProps {
  stats: DeckStatsDTO
}

const COLOR_CLASSES: Record<string, { bg: string; border: string; label: string }> = {
  W: { bg: 'bg-yellow-50',  border: 'border-yellow-300', label: 'text-yellow-900' },
  U: { bg: 'bg-blue-600',   border: 'border-blue-400',   label: 'text-white' },
  B: { bg: 'bg-zinc-800',   border: 'border-zinc-500',   label: 'text-zinc-100' },
  R: { bg: 'bg-red-600',    border: 'border-red-400',    label: 'text-white' },
  G: { bg: 'bg-green-600',  border: 'border-green-400',  label: 'text-white' },
  C: { bg: 'bg-zinc-500',   border: 'border-zinc-400',   label: 'text-white' },
}

const TYPE_COLORS: Record<string, string> = {
  Creature:     'bg-green-700',
  Instant:      'bg-blue-700',
  Sorcery:      'bg-purple-700',
  Enchantment:  'bg-pink-700',
  Artifact:     'bg-zinc-500',
  Planeswalker: 'bg-orange-700',
  Land:         'bg-amber-900',
  Other:        'bg-zinc-700',
}

const CMC_LABELS = ['0', '1', '2', '3', '4', '5', '6', '7+']

function SectionHeader({ title }: { title: string }) {
  return (
    <h3 className="font-display text-xs text-yellow-400 uppercase tracking-wider mb-2">{title}</h3>
  )
}

export function DeckStatsPanel({ stats }: DeckStatsPanelProps) {
  // Mana curve data
  const curveData = CMC_LABELS.map((label) => ({
    label,
    count: stats.manaCurve[label] ?? 0,
  }))
  const maxCurve = Math.max(...curveData.map((d) => d.count), 1)

  // Type breakdown total
  const typeTotal = Object.values(stats.typeBreakdown).reduce((a, b) => a + b, 0) || 1

  return (
    <div className="flex flex-col h-full bg-zinc-950 border-l border-zinc-800 overflow-y-auto">
      <div className="sticky top-0 bg-zinc-950/95 border-b border-zinc-800 px-3 py-3 z-10">
        <h2 className="font-display text-sm text-yellow-400 uppercase tracking-wider">
          Deck Stats
        </h2>
      </div>

      <div className="p-3 space-y-5">
        {/* 1. Card Counts */}
        <div>
          <SectionHeader title="Card Counts" />
          <div className="space-y-1.5">
            <div className="flex justify-between text-sm">
              <span className="text-zinc-400">Total cards</span>
              <span className="text-zinc-100 font-semibold">{stats.totalCards}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-zinc-400">Unique cards</span>
              <span className="text-zinc-100 font-semibold">{stats.uniqueCards}</span>
            </div>
            {stats.sideboardCount > 0 && (
              <div className="flex justify-between text-sm">
                <span className="text-zinc-400">Sideboard</span>
                <span className="text-zinc-100 font-semibold">{stats.sideboardCount}</span>
              </div>
            )}
          </div>
        </div>

        <div className="border-t border-zinc-800" />

        {/* 2. Average CMC */}
        <div>
          <SectionHeader title="Average CMC" />
          <div className="text-4xl font-display text-yellow-400 font-bold">
            {stats.averageCmc.toFixed(2)}
          </div>
        </div>

        <div className="border-t border-zinc-800" />

        {/* 3. Colors */}
        <div>
          <SectionHeader title="Colors" />
          <div className="flex flex-wrap gap-2">
            {['W', 'U', 'B', 'R', 'G', 'C'].map((color) => {
              const count = stats.colorDistribution[color]
              if (!count) return null
              const cls = COLOR_CLASSES[color]
              return (
                <div key={color} className="flex items-center gap-1.5">
                  <span
                    className={`inline-flex items-center justify-center w-6 h-6 rounded-full border font-bold text-xs ${cls.bg} ${cls.border} ${cls.label}`}
                  >
                    {color}
                  </span>
                  <span className="text-zinc-300 text-sm">{count}</span>
                </div>
              )
            })}
            {Object.keys(stats.colorDistribution).length === 0 && (
              <span className="text-zinc-600 text-sm">No colored cards</span>
            )}
          </div>
        </div>

        <div className="border-t border-zinc-800" />

        {/* 4. Mana Curve */}
        <div>
          <SectionHeader title="Mana Curve" />
          <div className="flex items-end gap-1 h-20">
            {curveData.map(({ label, count }) => {
              const heightPct = maxCurve > 0 ? (count / maxCurve) * 100 : 0
              // Map to tailwind height classes
              const heightPx = Math.max(4, Math.round((heightPct / 100) * 64))
              return (
                <div key={label} className="flex flex-col items-center flex-1">
                  <span className="text-zinc-500 text-[9px] mb-0.5">{count || ''}</span>
                  <div
                    className="w-full bg-yellow-600 rounded-t-sm transition-all"
                    style={{ height: `${heightPx}px` }}
                  />
                  <span className="text-zinc-600 text-[9px] mt-0.5">{label}</span>
                </div>
              )
            })}
          </div>
        </div>

        <div className="border-t border-zinc-800" />

        {/* 5. Type Breakdown */}
        <div>
          <SectionHeader title="Type Breakdown" />
          <div className="h-4 rounded overflow-hidden flex">
            {Object.entries(stats.typeBreakdown).map(([type, count]) => {
              const pct = (count / typeTotal) * 100
              const color = TYPE_COLORS[type] ?? TYPE_COLORS.Other
              return (
                <div
                  key={type}
                  className={`${color} transition-all`}
                  style={{ width: `${pct}%` }}
                  title={`${type}: ${count}`}
                />
              )
            })}
          </div>
          {/* Legend */}
          <div className="mt-2 flex flex-wrap gap-x-2 gap-y-1">
            {Object.entries(stats.typeBreakdown).map(([type, count]) => {
              const color = TYPE_COLORS[type] ?? TYPE_COLORS.Other
              return (
                <div key={type} className="flex items-center gap-1">
                  <div className={`w-2 h-2 rounded-sm ${color}`} />
                  <span className="text-zinc-400 text-[10px]">{type} {count}</span>
                </div>
              )
            })}
          </div>
        </div>

        <div className="border-t border-zinc-800" />

        {/* 6. Legality */}
        <div>
          <SectionHeader title="Legality" />
          <div className="flex items-center gap-2 mb-2">
            <span
              className={`text-lg font-bold ${stats.legality.isLegal ? 'text-green-400' : 'text-red-400'}`}
            >
              {stats.legality.isLegal ? '✓' : '✗'}
            </span>
            <span className="text-zinc-200 text-sm capitalize">{stats.legality.format}</span>
            <span
              className={`text-xs font-semibold px-1.5 py-0.5 rounded ${
                stats.legality.isLegal
                  ? 'bg-green-900/60 text-green-400'
                  : 'bg-red-900/60 text-red-400'
              }`}
            >
              {stats.legality.isLegal ? 'Legal' : 'Illegal'}
            </span>
          </div>

          {!stats.legality.isLegal && stats.legality.violations.length > 0 && (
            <div className="bg-red-900/30 border border-red-800 rounded-lg p-2 space-y-1">
              {stats.legality.violations.map((v, i) => (
                <p key={i} className="text-red-300 text-xs leading-snug">
                  • {v}
                </p>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
