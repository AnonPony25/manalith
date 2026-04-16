import { create } from 'zustand'
import { DeckDetailDTO } from '@/types/deck'

interface DeckEditorState {
  activeDeck: DeckDetailDTO | null
  isDirty: boolean
  setActiveDeck: (deck: DeckDetailDTO | null) => void
  updateActiveDeck: (deck: DeckDetailDTO) => void
}

export const useDeckStore = create<DeckEditorState>((set) => ({
  activeDeck: null,
  isDirty: false,

  setActiveDeck: (deck) =>
    set({ activeDeck: deck, isDirty: false }),

  updateActiveDeck: (deck) =>
    set({ activeDeck: deck, isDirty: true }),
}))
