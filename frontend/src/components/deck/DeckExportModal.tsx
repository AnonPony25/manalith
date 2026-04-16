import React, { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { deckApi } from '@/api/deckApi'

interface DeckExportModalProps {
  isOpen: boolean
  onClose: () => void
  deckId: string
}

type ExportFormat = 'text' | 'mtgo' | 'arena' | 'json'

const FORMAT_LABELS: Record<ExportFormat, string> = {
  text:  'Plain Text',
  mtgo:  'MTGO',
  arena: 'Arena',
  json:  'JSON',
}

export function DeckExportModal({ isOpen, onClose, deckId }: DeckExportModalProps) {
  const [exportedText, setExportedText] = useState<string>('')
  const [activeFormat, setActiveFormat] = useState<ExportFormat | null>(null)
  const [copySuccess, setCopySuccess] = useState(false)

  const exportMutation = useMutation({
    mutationFn: (format: ExportFormat) => deckApi.exportDeck(deckId, format),
    onSuccess: (text) => {
      setExportedText(text)
    },
  })

  function handleFormatClick(format: ExportFormat) {
    setActiveFormat(format)
    setExportedText('')
    exportMutation.mutate(format)
  }

  async function handleCopy() {
    if (!exportedText) return
    try {
      await navigator.clipboard.writeText(exportedText)
      setCopySuccess(true)
      setTimeout(() => setCopySuccess(false), 2000)
    } catch {
      // Clipboard not available
    }
  }

  function handleClose() {
    setExportedText('')
    setActiveFormat(null)
    setCopySuccess(false)
    exportMutation.reset()
    onClose()
  }

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm"
      onClick={(e) => {
        if (e.target === e.currentTarget) handleClose()
      }}
    >
      <div className="bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl w-full max-w-lg mx-4">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-700">
          <h2 className="font-display text-xl text-yellow-400">Export Deck</h2>
          <button
            onClick={handleClose}
            className="text-zinc-400 hover:text-zinc-100 text-2xl leading-none transition-colors"
            aria-label="Close"
          >
            ×
          </button>
        </div>

        <div className="px-6 py-5 space-y-4">
          {/* Format buttons */}
          <div className="flex gap-2 flex-wrap">
            {(Object.keys(FORMAT_LABELS) as ExportFormat[]).map((fmt) => (
              <button
                key={fmt}
                onClick={() => handleFormatClick(fmt)}
                disabled={exportMutation.isPending}
                className={`px-4 py-2 rounded-lg text-sm font-semibold transition-colors disabled:opacity-50 ${
                  activeFormat === fmt
                    ? 'bg-yellow-600 text-black'
                    : 'bg-zinc-800 hover:bg-zinc-700 text-zinc-300'
                }`}
              >
                {FORMAT_LABELS[fmt]}
              </button>
            ))}
          </div>

          {/* Loading */}
          {exportMutation.isPending && (
            <div className="flex items-center justify-center py-6">
              <div className="w-8 h-8 rounded-full border-2 border-zinc-600 border-t-yellow-500 animate-spin" />
            </div>
          )}

          {/* Error */}
          {exportMutation.isError && (
            <p className="text-red-400 text-sm">Export failed. Please try again.</p>
          )}

          {/* Exported text */}
          {exportedText && !exportMutation.isPending && (
            <>
              <textarea
                readOnly
                value={exportedText}
                rows={12}
                className="w-full bg-zinc-800 border border-zinc-600 text-zinc-100 font-mono text-xs rounded px-3 py-2 resize-none focus:outline-none focus:border-yellow-500"
              />
              <div className="flex items-center justify-between">
                <button
                  onClick={handleCopy}
                  className="bg-yellow-600 hover:bg-yellow-500 text-black font-semibold px-4 py-2 rounded-lg text-sm transition-colors"
                >
                  {copySuccess ? 'Copied!' : 'Copy to Clipboard'}
                </button>
                {copySuccess && (
                  <span className="text-green-400 text-sm animate-pulse">
                    Copied to clipboard ✓
                  </span>
                )}
              </div>
            </>
          )}

          {/* Hint when nothing selected */}
          {!activeFormat && !exportMutation.isPending && !exportedText && (
            <p className="text-zinc-500 text-sm text-center py-4">
              Select a format above to export your deck.
            </p>
          )}
        </div>
      </div>
    </div>
  )
}
