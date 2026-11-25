import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import AdminHeader from './AdminHeader'
import { ArrowRight } from '../components/Icons'
import { getLoginUrl } from '../api/admin'
import './admin.css'

function AdminLoginPage() {
  const [params] = useSearchParams()
  const [error, setError] = useState(params.get('error') ? 'Недостаточно прав для входа' : '')
  const [loading, setLoading] = useState(false)

  // Просим у бэкенда адрес oauth.tpu.ru и уходим на него
  const onLogin = async () => {
    setLoading(true)
    setError('')
    try {
      window.location.href = await getLoginUrl()
    } catch (e) {
      setError(e.message)
      setLoading(false)
    }
  }

  return (
    <div className="admin">
      <AdminHeader />
      <main className="login">
        <h1 className="login__title">Авторизация</h1>
        <p className="login__text">Войдите с помощью корпоративной почты ТПУ</p>
        <button className="login__btn" onClick={onLogin} disabled={loading}>
          <span>Войти через аккаунт ТПУ (OAuth)</span>
          <ArrowRight size={20} />
        </button>
        {error && <p className="login__error">{error}</p>}
      </main>
    </div>
  )
}

export default AdminLoginPage
