// Scryfall card model (subset used by the frontend)
export interface CardPrinting {
  id:              string    // Scryfall UUID
  oracleId:        string
  name:            string
  setCode:         string
  setName:         string
  collectorNumber: string
  rarity:          'common' | 'uncommon' | 'rare' | 'mythic' | 'special' | 'bonus'
  layout:          string
  manaCost:        string | null
  cmc:             number
  typeLine:        string
  oracleText:      string | null
  colors:          string[]
  colorIdentity:   string[]
  imageUris:       ImageUris | null
  cardFaces:       CardFace[] | null
  legalities:      Record<string, string>
  prices:          CardPrices
}

export interface ImageUris {
  small:       string
  normal:      string
  large:       string
  png:         string
  art_crop:    string
  border_crop: string
}

export interface CardFace {
  name:      string
  manaCost:  string | null
  typeLine:  string
  oracleText: string | null
  imageUris: ImageUris | null
}

export interface CardPrices {
  usd:      string | null
  usd_foil: string | null
}
