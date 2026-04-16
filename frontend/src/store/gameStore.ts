import { create } from 'zustand'
import { GameStateDTO, GameActionEnvelope } from '@/types/game'

interface GameStore {
  state:      GameStateDTO | null
  connected:  boolean
  socket:     WebSocket | null

  connect:    (roomId: string, token: string) => void
  disconnect: () => void
  sendAction: (action: GameActionEnvelope) => void
  applyState: (state: GameStateDTO) => void
}

export const useGameStore = create<GameStore>((set, get) => ({
  state:     null,
  connected: false,
  socket:    null,

  connect(roomId, token) {
    const ws = new WebSocket(`/ws/game?roomId=${roomId}&token=${token}`)

    ws.onopen = () => {
      set({ connected: true, socket: ws })
    }

    ws.onmessage = (event) => {
      const envelope = JSON.parse(event.data)
      if (envelope.type === 'GAME_STATE') {
        get().applyState(envelope.payload as GameStateDTO)
      }
      // TODO: handle GAME_DIFF, DECISION_REQUEST, GAME_OVER, etc.
    }

    ws.onclose = () => {
      set({ connected: false, socket: null })
      // TODO: reconnect logic with exponential backoff
    }
  },

  disconnect() {
    get().socket?.close()
    set({ connected: false, socket: null, state: null })
  },

  sendAction(action) {
    get().socket?.send(JSON.stringify(action))
  },

  applyState(state) {
    set({ state })
  },
}))
