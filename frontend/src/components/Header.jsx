import { Link } from 'react-router-dom'
import logo from '../assets/logo-cube.png'

function Header() {
  return (
    <header className="header">
      <Link to="/" className="header__logo">
        <img src={logo} alt="" className="header__cube" />
        <span>Welcome TPU</span>
      </Link>
    </header>
  )
}

export default Header
