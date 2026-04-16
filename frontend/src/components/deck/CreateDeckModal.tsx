import React, { useState, useEffect } from 'react'
import { useMutation } from '@tanstack/react-query'
import { deckApi } from '@/api/deckApi'
import { DeckSummaryDTO, DeckFormat } from '@/types/deck'

const ALL_FORMATS: DeckFormat[] = [
  'standard', 'pioneer', 'modern', 'legacy', 'vintage',
  'commander', 'pauper', 'draft', 'sealed', 'custom',
]

interface CreateDeckModalProps {
  isOpen: boolean
  onClose: () => void
  onCreated: (deck: DeckSummaryDTO) => void
}

export function CreateDeckModal({ isOpen, onClose, onCreated }: CreateDeckModalProps) {
  const [name, setName] = useState('')
  const [format, setFormat] = useState<DeckFormat>('commander')
  const [description, setDescription] = useState('')
  const [isPublic, setIsPublic] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      setName('')
      setFormat('commander')
      setDescription('')
      setIsPublic(false)
      setErrors({})
    }
  }, [isOpen])

  const createMutation = useMutation({
    mutationFn: () =>
      deckApi.createDeck({
        name: name.trim(),
        format,
        description: description.trim() || undefined,
        isPublic,
      }),
    onSuccess: (deck) => {
      onCreated(deck)
    },
  })

  function validate(): boolean {
    const errs: Record<string, string> = {}
    if (!name.trim()) errs.name = 'Deck name is required.'
    if (!format) errs.format = 'Format is required.'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return
    createMutation.mutate()
  }

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="bg-zinc-900 border border-zinc-700 rounded-xl shadow-2xl w-full max-w-md mx-4">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-700">
          <h2 className="font-display text-xl text-yellow-400">Create New Deck</h2>
          <button
            onClick={onClose}
            className="text-zinc-400 hover:text-zinc-100 text-2xl leading-none transition-colors"
            aria-label="Close"
          >
            ×
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {/* Name */}
          <div>
            <label className="block text-sm font-medium text-zinc-300 mb-1">
              Deck Name <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Esper Control"
              className="w-full bg-zinc-800 border border-zinc-600 text-zinc-100 focus:border-yellow-500 focus:outline-none rounded px-3 py-2 placeholder-zinc-500"
            />
            {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name}</p>}
          </div>

          {/* Format */}
          <div>
            <label className="block text-sm font-medium text-zinc-300 mb-1">
              Format <span className="text-red-400">*</span>
            </label>
            <select
              value={format}
              onChange={(e) => setFormat(e.target.value as DeckFormat)}
              className="w-full bg-zinc-800 border border-zinc-600 text-zinc-100 focus:border-yellow-500 focus:outline-none rounded px-3 py-2 capitalize"
            >
              {ALL_FORMATS.map((f) => (
                <option key={f} value={f} className="capitalize">
                  {f.charAt(0).toUpperCase() + f.slice(1)}
                </option>
              ))}
            </select>
            {errors.format && <p className="text-red-400 text-xs mt-1">{errors.format}</p>}
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-zinc-300 mb-1">
              Description <span className="text-zinc-500 font-normal">(optional)</span>
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe your deck strategy..."
              rows={3}
              className="w-full bg-zinc-800 border border-zinc-600 text-zinc-100 focus:border-yellow-500 focus:outline-none rounded px-3 py-2 placeholder-zinc-500 resize-none"
            />
          </div>

          {/* Public toggle */}
          <div className="flex items-center gap-3">
            <input
              id="isPublic"
              type="checkbox"
              checked={isPublic}
              onChange={(e) => setIsPublic(e.target.checked)}
              className="w-4 h-4 accent-yellow-500 rounded"
            />
            <label htmlFor="isPublic" className="text-sm text-zinc-300 cursor-pointer">
              Make this deck public
            </label>
          </div>

          {/* Error message */}
          {createMutation.isError && (
            <p className="text-red-400 text-sm">
              Failed to create deck. Please try again.
            </p>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 font-semibold px-4 py-2 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="flex-1 bg-yellow-600 hover:bg-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed text-black font-semibold px-4 py-2 rounded-lg transition-colors"
            >
              {createMutation.isPending ? 'Creating…' : 'Create Deck'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
