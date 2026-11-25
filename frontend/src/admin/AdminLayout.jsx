import { useEffect, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import AdminHeader from './AdminHeader'
import { getMe, logout } from '../api/admin'
import './admin.css'

// Обёртка для всех страниц админки: проверяет вход и рисует шапку.
function AdminLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const [status, setStatus] = useState('checking') // checking | ok | error
  const [error, setError] = useState('')

  useEffect(() => {
    getMe()
      .then(() => setStatus('ok'))
      .catch((e) => {
        if (e.status === 401 || e.status === 403) {
          navigate('/admin/login', { replace: true })
        } else {
          setError(e.message)
          setStatus('error')
        }
      })
  }, [navigate])

  const onLogout = async () => {
    try {
      await logout()
    } finally {
      navigate('/admin/login', { replace: true })
    }
  }

  if (status === 'checking') {
    return (
      <div className="admin">
        <AdminHeader />
        <div className="admin-state">Проверяем вход…</div>
      </div>
    )
  }
  if (status === 'error') {
    return (
      <div className="admin">
        <AdminHeader />
        <div className="admin-state">Не удалось проверить вход. {error}</div>
      </div>
    )
  }

  const onList = location.pathname === '/admin'
  return (
    <div className="admin">
      <AdminHeader>
        {onList && (
          <Link to="/admin/cards/new" className="btn btn--green btn--header-create">
            Создать карточку
          </Link>
        )}
        <button className="btn btn--green" onClick={onLogout}>Выйти</button>
      </AdminHeader>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  )
}

export default AdminLayout
