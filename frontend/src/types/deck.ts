export interface DeckSummaryDTO {
  id: string
  ownerId: string
  ownerName: string
  name: string
  format: string
  description: string | null
  isPublic: boolean
  mainboardCount: number
  sideboardCount: number
  createdAt: string
  updatedAt: string
}

export interface DeckDetailDTO {
  id: string
  ownerId: string
  ownerName: string
  name: string
  format: string
  description: string | null
  isPublic: boolean
  createdAt: string
  updatedAt: string
  mainboard: DeckEntryDTO[]
  sideboard: DeckEntryDTO[]
  commanders: DeckEntryDTO[]
  stats: DeckStatsDTO
}

export interface DeckEntryDTO {
  cardId: string
  cardName: string
  manaCost: string | null
  cmc: number
  typeLine: string
  setCode: string
  rarity: string
  imageUri: string | null
  quantity: number
  isCommander: boolean
}

export interface DeckStatsDTO {
  totalCards: number
  uniqueCards: number
  sideboardCount: number
  colorDistribution: Record<string, number>
  manaCurve: Record<string, number>
  typeBreakdown: Record<string, number>
  averageCmc: number
  colorIdentity: string[]
  legality: LegalityResultDTO
}

export interface LegalityResultDTO {
  isLegal: boolean
  format: string
  violations: string[]
}

export interface CreateDeckRequest {
  name: string
  format: string
  description?: string
  isPublic: boolean
}

export interface AddCardRequest {
  cardId: string
  quantity: number
  isCommander: boolean
  isSideboard: boolean
}

export type DeckFormat =
  | 'standard'
  | 'pioneer'
  | 'modern'
  | 'legacy'
  | 'vintage'
  | 'commander'
  | 'pauper'
  | 'draft'
  | 'sealed'
  | 'custom'
