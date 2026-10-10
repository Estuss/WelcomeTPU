import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <div className="container page">
      <h1>404</h1>
      <p>Страница не найдена</p>
      <Link to="/" className="link">На главную</Link>
    </div>
  )
}

export default NotFoundPage
