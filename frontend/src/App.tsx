import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { HomePage }        from '@/pages/HomePage'
import { LoginPage }       from '@/pages/LoginPage'
import { LobbyPage }       from '@/pages/LobbyPage'
import { GamePage }        from '@/pages/GamePage'
import { DeckBuilderPage } from '@/pages/DeckBuilderPage'
import { CollectionPage }  from '@/pages/CollectionPage'
import { RequireAuth }     from '@/components/shared/RequireAuth'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/"           element={<HomePage />} />
        <Route path="/login"      element={<LoginPage />} />
        <Route element={<RequireAuth />}>
          <Route path="/lobby"         element={<LobbyPage />} />
          <Route path="/game/:roomId"  element={<GamePage />} />
          <Route path="/decks"         element={<DeckBuilderPage />} />
          <Route path="/collection"    element={<CollectionPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
