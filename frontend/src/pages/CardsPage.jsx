import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Card, { FinishCard } from '../components/Card'
import { ArrowLeft, ArrowRight } from '../components/Icons'
import { getCards } from '../data/cards'
import { getSchool } from '../data/schools'

function CardsPage() {
  const { schoolId } = useParams()
  const navigate = useNavigate()
  const school = schoolId ? getSchool(schoolId) : null
  const title = school ? school.shortName : 'О ТПУ'

  const [cards, setCards] = useState([])
  const [status, setStatus] = useState('loading') // loading | error | ready
  const [index, setIndex] = useState(0)
  const touchStart = useRef(null)

  const load = useCallback(() => {
    setStatus('loading')
    setIndex(0)
    getCards(schoolId)
      .then((data) => {
        setCards(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [schoolId])

  useEffect(() => {
    load()
  }, [load])

  // последний экран "Спасибо за просмотр" считается отдельным шагом
  const total = cards.length + 1
  const isFinish = index === cards.length

  const next = useCallback(() => setIndex((i) => Math.min(i + 1, total - 1)), [total])
  const prev = useCallback(() => setIndex((i) => Math.max(i - 1, 0)), [])

  useEffect(() => {
    if (status !== 'ready') return
    const onKey = (e) => {
      if (e.key === 'ArrowRight') next()
      if (e.key === 'ArrowLeft') prev()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [status, next, prev])

  const onTouchStart = (e) => {
    touchStart.current = { x: e.touches[0].clientX, y: e.touches[0].clientY }
  }
  const onTouchEnd = (e) => {
    if (!touchStart.current) return
    const dx = e.changedTouches[0].clientX - touchStart.current.x
    const dy = e.changedTouches[0].clientY - touchStart.current.y
    touchStart.current = null
    if (Math.abs(dx) < 50 || Math.abs(dx) < Math.abs(dy)) return
    if (dx < 0) next()
    else prev()
  }

  const counter = `${index + 1} из ${total}`

  let content
  if (status === 'loading') {
    content = <div className="state">Загрузка…</div>
  } else if (status === 'error') {
    content = (
      <div className="state">
        <p>Не удалось загрузить карточки</p>
        <button className="state__btn" onClick={load}>Попробовать ещё раз</button>
      </div>
    )
  } else if (cards.length === 0) {
    content = (
      <div className="state">
        <p>Карточек пока нет</p>
        <Link className="state__btn" to="/">На главную</Link>
      </div>
    )
  }

  return (
    <div className="viewer">
      <div className="viewer__top">
        <Link to="/" className="back">
          <ArrowLeft size={16} />
          <span>На главную</span>
        </Link>
        <div className="viewer__title">{title}</div>
        {status === 'ready' && cards.length > 0 && <div className="viewer__count">{counter}</div>}
      </div>

      {content ? (
        <div className="viewer__stage viewer__stage--state">{content}</div>
      ) : (
        <>
          <div className="viewer__stage" onTouchStart={onTouchStart} onTouchEnd={onTouchEnd}>
            <button className="round round--prev" onClick={prev} disabled={index === 0} aria-label="Назад">
              <ArrowLeft size={28} />
            </button>
            <div className={`stack${isFinish ? ' stack--finish' : ''}`}>
              {isFinish ? <FinishCard /> : <Card key={cards[index].id} card={cards[index]} />}
            </div>
            <button
              className={`round round--next${isFinish ? ' round--filled' : ''}`}
              onClick={isFinish ? () => navigate('/') : next}
              aria-label={isFinish ? 'На главную' : 'Вперёд'}
            >
              <ArrowRight size={28} />
            </button>
          </div>
          <div className="viewer__count viewer__count--mobile">{counter}</div>
        </>
      )}
    </div>
  )
}

export default CardsPage
