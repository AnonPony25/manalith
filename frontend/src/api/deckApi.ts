import { axiosInstance as authApi } from './authApi'
import {
  DeckSummaryDTO,
  DeckDetailDTO,
  CreateDeckRequest,
  AddCardRequest,
  LegalityResultDTO,
} from '@/types/deck'

interface PaginatedDecks {
  content: DeckSummaryDTO[]
  totalElements: number
  totalPages: number
}

export const deckApi = {
  async getMyDecks(): Promise<DeckSummaryDTO[]> {
    const { data } = await authApi.get<DeckSummaryDTO[]>('/decks/mine')
    return data
  },

  async getPublicDecks(page = 0, size = 20): Promise<PaginatedDecks> {
    const { data } = await authApi.get<PaginatedDecks>('/decks/public', {
      params: { page, size },
    })
    return data
  },

  async getDeck(id: string): Promise<DeckDetailDTO> {
    const { data } = await authApi.get<DeckDetailDTO>(`/decks/${id}`)
    return data
  },

  async createDeck(req: CreateDeckRequest): Promise<DeckSummaryDTO> {
    const { data } = await authApi.post<DeckSummaryDTO>('/decks', req)
    return data
  },

  async updateDeck(
    id: string,
    req: Partial<Pick<CreateDeckRequest, 'name' | 'format' | 'description' | 'isPublic'>>
  ): Promise<DeckSummaryDTO> {
    const { data } = await authApi.patch<DeckSummaryDTO>(`/decks/${id}`, req)
    return data
  },

  async deleteDeck(id: string): Promise<void> {
    await authApi.delete(`/decks/${id}`)
  },

  async addCard(deckId: string, req: AddCardRequest): Promise<DeckDetailDTO> {
    const { data } = await authApi.post<DeckDetailDTO>(`/decks/${deckId}/cards`, req)
    return data
  },

  async removeCard(deckId: string, cardId: string, sideboard = false): Promise<DeckDetailDTO> {
    const { data } = await authApi.delete<DeckDetailDTO>(`/decks/${deckId}/cards/${cardId}`, {
      params: { sideboard },
    })
    return data
  },

  async updateCardQty(
    deckId: string,
    cardId: string,
    quantity: number,
    sideboard = false
  ): Promise<DeckDetailDTO> {
    const { data } = await authApi.patch<DeckDetailDTO>(
      `/decks/${deckId}/cards/${cardId}`,
      { quantity, sideboard }
    )
    return data
  },

  async validateDeck(id: string): Promise<LegalityResultDTO> {
    const { data } = await authApi.get<LegalityResultDTO>(`/decks/${id}/validate`)
    return data
  },

  async exportDeck(id: string, format: 'text' | 'mtgo' | 'arena' | 'json'): Promise<string> {
    const { data } = await authApi.get<string>(`/decks/${id}/export`, {
      params: { format },
      responseType: format === 'json' ? 'json' : 'text',
    })
    return typeof data === 'string' ? data : JSON.stringify(data, null, 2)
  },

  async importDeck(
    name: string,
    format: string,
    deckList: string,
    importFormat: 'text' | 'mtgo' | 'arena'
  ): Promise<DeckDetailDTO> {
    const { data } = await authApi.post<DeckDetailDTO>('/decks/import', {
      name,
      format,
      deckList,
      importFormat,
    })
    return data
  },
}
