import React, { useState, useEffect } from 'react'
import { useMutation } from '@tanstack/react-query'
import { deckApi } from '@/api/deckApi'
import { DeckDetailDTO, DeckFormat } from '@/types/deck'

interface DeckImportModalProps {
  isOpen: boolean
  onClose: () => void
  onImported: (deck: DeckDetailDTO) => void
}

const ALL_FORMATS: DeckFormat[] = [
  'standard', 'pioneer', 'modern', 'legacy', 'vintage',
  'commander', 'pauper', 'draft', 'sealed', 'custom',
]

type ImportFormat = 'text' | 'mtgo' | 'arena'

const IMPORT_FORMAT_LABELS: Record<ImportFormat, string> = {
  text:  'Plain Text',
  mtgo:  'MTGO',
  arena: 'Arena',
}

export function DeckImportModal({ isOpen, onClose, onImported }: DeckImportModalProps) {
  const [name, setName] = useState('')
  const [format, setFormat] = useState<DeckFormat>('commander')
  const [importFormat, setImportFormat] = useState<ImportFormat>('text')
  const [deckList, setDeckList] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!isOpen) {
      setName('')
      setFormat('commander')
      setImportFormat('text')
      setDeckList('')
      setErrors({})
      importMutation.reset()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen])

  const importMutation = useMutation({
    mutationFn: () =>
      deckApi.importDeck(name.trim(), format, deckList.trim(), importFormat),
    onSuccess: (deck) => {
      onImported(deck)
      onClose()
    },
  })

  function validate(): boolean {
    const errs: Record<string, string> = {}
    if (!name.trim()) errs.name = 'Deck name is required.'
    if (!deckList.trim()) errs.deckList = 'Paste your deck list above.'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return
    importMutation.mutate()
  }

  function handleClose() {
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
      <div className="bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-700 sticky top-0 bg-zinc-900 z-10">
          <h2 className="font-display text-xl text-yellow-400">Import Deck</h2>
          <button
            onClick={handleClose}
            className="text-zinc-400 hover:text-zinc-100 text-2xl leading-none transition-colors"
            aria-label="Close"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {/* Deck name */}
          <div>
            <label className="block text-sm font-medium text-zinc-300 mb-1">
              Deck Name <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Sultai Midrange"
              className="w-full bg-zinc-800 border border-zinc-600 text-zinc-100 focus:border-yellow-500 focus:outline-none rounded px-3 py-2 placeholder-zinc-500"
            />
            {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name}</p>}
          </div>

          {/* Format */}
          <div>
            <label className="block text-sm font-medium text-zinc-300 mb-1">Format</label>
            <select
              value={format}
              onChange={(e) => setFormat(e.target.value as DeckFormat)}
              className="w-full bg-zinc-800 border border-zinc-600 text-zinc-100 focus:border-yellow-500 focus:outline-none rounded px-3 py-2"
            >
              {ALL_FORMATS.map((f) => (
                <option key={f} value={f}>
                  {f.charAt(0).toUpperCase() + f.slice(1)}
                </option>
              ))}
            </select>
          </div>

          {/* Import format radio */}
          <div>
            <label className="block text-sm font-medium text-zinc-300 mb-2">Import Format</label>
            <div className="flex gap-4">
              {(Object.keys(IMPORT_FORMAT_LABELS) as ImportFormat[]).map((fmt) => (
                <label key={fmt} className="flex items-center gap-1.5 cursor-pointer">
                  <input
                    type="radio"
                    name="importFormat"
                    value={fmt}
                    checked={importFormat === fmt}
                    onChange={() => setImportFormat(fmt)}
                    className="accent-yellow-500"
                  />
                  <span className="text-zinc-300 text-sm">{IMPORT_FORMAT_LABELS[fmt]}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Deck list textarea */}
          <div>
            <label className="block text-sm font-medium text-zinc-300 mb-1">
              Deck List <span className="text-red-400">*</span>
            </label>
            <textarea
              value={deckList}
              onChange={(e) => setDeckList(e.target.value)}
              placeholder={`Paste your deck list here…\n\nExample:\n4 Lightning Bolt\n4 Monastery Swiftspear`}
              rows={10}
              className="w-full bg-zinc-800 border border-zinc-600 text-zinc-100 focus:border-yellow-500 focus:outline-none rounded px-3 py-2 placeholder-zinc-500 font-mono text-sm resize-none"
            />
            {errors.deckList && (
              <p className="text-red-400 text-xs mt-1">{errors.deckList}</p>
            )}
          </div>

          {/* API error */}
          {importMutation.isError && (
            <div className="bg-red-900/40 border border-red-700 text-red-300 rounded-lg px-3 py-2 text-sm">
              Import failed. Check your deck list format and try again.
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 font-semibold px-4 py-2 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={importMutation.isPending}
              className="flex-1 bg-yellow-600 hover:bg-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed text-black font-semibold px-4 py-2 rounded-lg transition-colors"
            >
              {importMutation.isPending ? 'Importing…' : 'Import Deck'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
