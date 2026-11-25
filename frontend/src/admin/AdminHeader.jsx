import { Link } from 'react-router-dom'
import logo from '../assets/logo-tpu.png'

function AdminHeader({ children }) {
  return (
    <header className="admin-header">
      <Link to="/admin" className="admin-header__logo" aria-label="Админ-панель">
        <img src={logo} alt="Томский политехнический университет" />
      </Link>
      <div className="admin-header__actions">{children}</div>
    </header>
  )
}

export default AdminHeader
