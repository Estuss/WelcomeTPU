import { Link, NavLink } from 'react-router-dom'

function Header() {
  return (
    <header className="header">
      <div className="container header-inner">
        <Link to="/" className="logo">Welcome TPU</Link>
        <nav className="nav">
          <NavLink to="/">Главная</NavLink>
          <NavLink to="/cards">Карточки</NavLink>
          <NavLink to="/announcements">Объявления</NavLink>
        </nav>
      </div>
    </header>
  )
}

export default Header
