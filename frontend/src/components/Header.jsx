import { Link } from 'react-router-dom'
import logo from '../assets/logo-tpu.png'

function Header() {
  return (
    <header className="header">
      <Link to="/" className="header__logo">
        <img src={logo} alt="Томский политехнический университет" className="header__img" />
      </Link>
    </header>
  )
}

export default Header
