// ============================================================
// Manalith Game Protocol Types
// Mirror the server-side DTOs from manalith-forge-adapter
// ============================================================

export type Phase =
  | 'UNTAP' | 'UPKEEP' | 'DRAW'
  | 'MAIN1' | 'BEGIN_COMBAT' | 'DECLARE_ATTACKERS'
  | 'DECLARE_BLOCKERS' | 'COMBAT_DAMAGE' | 'END_COMBAT'
  | 'MAIN2' | 'END' | 'CLEANUP'

export type Zone =
  | 'HAND' | 'LIBRARY' | 'BATTLEFIELD' | 'GRAVEYARD'
  | 'EXILE' | 'COMMAND' | 'STACK'

export interface CardRef {
  instanceId: string    // unique object ID within the game
  cardId:     string    // Scryfall UUID
  name:       string
  imageUri:   string | null
  zone:       Zone
  isFaceDown: boolean
  isKnown:    boolean   // false = hidden (opponent hand / top of library)
}

export interface PlayerPublicState {
  playerId:        string
  displayName:     string
  avatarUrl:       string | null
  life:            number
  poisonCounters:  number
  commanderDamage: Record<string, number>  // sourcePlayerId → damage
  handCount:       number
  libraryCount:    number
  battlefield:     CardRef[]
  graveyard:       CardRef[]
  exile:           CardRef[]
  manaPool:        ManaPool
  hasPriority:     boolean
  isActivePlayer:  boolean
}

export interface PlayerPrivateState {
  hand:         CardRef[]
  libraryTop:   CardRef[]   // only populated when allowed to see (e.g., Scry)
}

export interface ManaPool {
  W: number; U: number; B: number; R: number; G: number; C: number
}

export interface StackItem {
  stackId:     string
  type:        'SPELL' | 'ABILITY' | 'TRIGGERED' | 'ACTIVATED'
  description: string
  controllerId: string
  cardRef:     CardRef | null
}

export interface GameStateDTO {
  roomId:             string
  gameId:             string
  revision:           number
  perspectivePlayerId: string
  isCommander:        boolean
  format:             string
  turnNumber:         number
  phase:              Phase
  priorityPlayerId:   string | null
  activePlayerId:     string
  stack:              StackItem[]
  players:            PlayerPublicState[]
  self:               PlayerPrivateState
  prompts:            DecisionRequest[]
  gameLog:            LogEntry[]
  outcome:            GameOutcome | null
}

export interface DecisionRequest {
  requestId:   string
  kind:        string
  description: string
  choices:     Choice[]
  timeoutMs:   number
}

export interface Choice {
  id:    string
  label: string
}

export interface LogEntry {
  timestamp: string
  message:   string
  playerId:  string | null
}

export interface GameOutcome {
  winnerId:  string | null
  reason:    string
  endedAt:   string
}

// ── Client → Server action envelope ──────────────────────────────────
export interface GameActionEnvelope {
  type:      'GAME_ACTION'
  version:   1
  roomId:    string
  gameId:    string
  seatId:    string
  actionId:  string       // client-generated UUID for idempotency
  requestId: string | null
  payload:   GameActionPayload
}

export type GameActionKind =
  | 'PASS_PRIORITY'
  | 'PLAY_LAND'
  | 'CAST_SPELL'
  | 'ACTIVATE_ABILITY'
  | 'CHOOSE_TARGETS'
  | 'DECLARE_ATTACKERS'
  | 'DECLARE_BLOCKERS'
  | 'MULLIGAN_DECISION'
  | 'CONCEDE'
  | 'CHAT'

export interface GameActionPayload {
  kind:       GameActionKind
  objectRef?: string
  targets?:   string[]
  choices?:   string[]
  xValue?:    number
  message?:   string
}
