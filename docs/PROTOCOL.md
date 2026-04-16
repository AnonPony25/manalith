# Manalith WebSocket Game Protocol

Version: **1** (unstable until Phase 2 lock-in)

## Endpoints

| Endpoint | Purpose |
|---|---|
| `ws://host/ws/game?roomId=<uuid>&token=<jwt>` | In-game real-time channel |
| `ws://host/ws/lobby?token=<jwt>` | Lobby / room discovery channel |

## Message Envelope

All messages in both directions are JSON objects with a top-level `type` field.

### Client → Server

```json
{
  "type": "GAME_ACTION",
  "version": 1,
  "roomId": "uuid",
  "gameId": "uuid",
  "seatId": "uuid",
  "actionId": "client-generated-uuid",
  "requestId": "uuid-or-null",
  "payload": { "kind": "...", ... }
}
```

**Supported `payload.kind` values:**

| Kind | Description |
|---|---|
| `PASS_PRIORITY` | Pass priority, no action |
| `PLAY_LAND` | Play a land from hand |
| `CAST_SPELL` | Cast a spell |
| `ACTIVATE_ABILITY` | Activate an ability |
| `CHOOSE_TARGETS` | Respond to target selection prompt |
| `CHOOSE_MODE` | Choose from optional modes |
| `CHOOSE_X` | Provide an X value |
| `PAY_MANA` | Submit a mana payment plan |
| `DECLARE_ATTACKERS` | Declare which creatures attack |
| `DECLARE_BLOCKERS` | Declare blocking assignments |
| `MULLIGAN_DECISION` | KEEP or MULLIGAN |
| `SIDEBOARD_SUBMIT` | Submit post-game sideboard changes |
| `CONCEDE` | Concede the game |
| `CHAT` | In-game chat message |

### Server → Client

| Event type | Description |
|---|---|
| `GAME_STATE` | Full game state snapshot |
| `GAME_DIFF` | Incremental delta since last revision |
| `DECISION_REQUEST` | Server is waiting for a specific decision |
| `DECISION_TIMEOUT_WARNING` | 15-second warning before auto-pass |
| `ACTION_REJECTED` | Client action was invalid |
| `PRIORITY_CHANGED` | Priority has moved to a different player |
| `TURN_CHANGED` | Active player changed |
| `PHASE_CHANGED` | Phase/step changed |
| `STACK_UPDATED` | Stack was modified |
| `GAME_LOG_APPEND` | New log entries |
| `PLAYER_DISCONNECTED` | A player lost connection |
| `PLAYER_RECONNECTED` | A player reconnected |
| `GAME_OVER` | Game has ended |

## Sequence: Casting a Spell

```
Client                        Server
  │  ── GAME_ACTION (CAST_SPELL) ──►  │
  │                                    │  validates priority, mana cost
  │                                    │  calls Forge PlayerController
  │  ◄── DECISION_REQUEST (targets) ── │
  │  ── GAME_ACTION (CHOOSE_TARGETS) ► │
  │                                    │  validates targets, places on stack
  │  ◄───── GAME_DIFF ─────────────── │  (all players)
  │  ◄── PRIORITY_CHANGED ──────────── │  (active player opponent now has priority)
```

## Hidden Information Rules

- Opponent hand cards: sent as `{ isKnown: false, name: "Unknown Card" }`
- Library cards: never sent, only library count
- Face-down permanents: sent as anonymous card ref unless current player controls
- Spectators: receive no hidden information
