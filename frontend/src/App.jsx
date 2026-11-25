import { Outlet, Route, Routes } from 'react-router-dom'
import Header from './components/Header'
import HomePage from './pages/HomePage'
import CardsPage from './pages/CardsPage'
import AnnouncementsPage from './pages/AnnouncementsPage'
import NotFoundPage from './pages/NotFoundPage'
import AdminLayout from './admin/AdminLayout'
import AdminLoginPage from './admin/AdminLoginPage'
import AdminCardsPage from './admin/AdminCardsPage'
import AdminCardFormPage from './admin/AdminCardFormPage'

// Обычный сайт: шапка Welcome TPU и страница внутри
function SiteLayout() {
  return (
    <div className="app">
      <Header />
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/admin/login" element={<AdminLoginPage />} />
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<AdminCardsPage />} />
        <Route path="cards/new" element={<AdminCardFormPage />} />
        <Route path="cards/:id" element={<AdminCardFormPage />} />
      </Route>

      <Route element={<SiteLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/cards" element={<CardsPage />} />
        <Route path="/cards/:schoolId" element={<CardsPage />} />
        <Route path="/announcements" element={<AnnouncementsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
