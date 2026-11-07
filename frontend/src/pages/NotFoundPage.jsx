import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <div className="page">
      <h1>404</h1>
      <p>Страница не найдена</p>
      <Link to="/" className="page__link">На главную</Link>
    </div>
  )
}

export default NotFoundPage
