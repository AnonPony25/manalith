import axios from 'axios'
import { CardPrinting } from '@/types/card'

const api = axios.create({ baseURL: '/api' })

export const cardApi = {
  search: (query: string, page = 1) =>
    api.get<{ cards: CardPrinting[]; total: number; page: number }>(
      '/catalog/cards/search',
      { params: { q: query, page } }
    ),

  getById: (id: string) =>
    api.get<CardPrinting>(`/catalog/cards/${id}`),

  getByName: (name: string) =>
    api.get<CardPrinting>('/catalog/cards/named', { params: { exact: name } }),
}
