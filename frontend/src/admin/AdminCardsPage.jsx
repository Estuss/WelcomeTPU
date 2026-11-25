import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Pagination from './Pagination'
import { deleteCard, getAdminCards } from '../api/admin'
import { getSchools } from '../api/schools'
import { TYPE_NAMES, shortDate } from './format'

function ChevronDown() {
  return (
    <svg className="field__chevron" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M5 9l7 7 7-7" />
    </svg>
  )
}

function SearchIcon() {
  return (
    <svg className="search__icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20l-4-4" />
    </svg>
  )
}

function AdminCardsPage() {
  const [schools, setSchools] = useState([])
  const [data, setData] = useState(null) // страница от сервера
  const [status, setStatus] = useState('loading') // loading | error | ready
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [type, setType] = useState('')
  const [schoolId, setSchoolId] = useState('')

  // школы нужны для бейджей и фильтра
  useEffect(() => {
    getSchools().then(setSchools).catch(() => {})
  }, [])

  // поиск запускается, когда человек перестал печатать
  useEffect(() => {
    const t = setTimeout(() => {
      setSearch(searchInput.trim())
      setPage(0)
    }, 400)
    return () => clearTimeout(t)
  }, [searchInput])

  const load = useCallback(() => {
    setStatus('loading')
    getAdminCards({ page, search, type, schoolId })
      .then((res) => {
        setData(res)
        setStatus('ready')
      })
      .catch((e) => {
        setError(e.message)
        setStatus('error')
      })
  }, [page, search, type, schoolId])

  useEffect(() => {
    load()
  }, [load])

  const onDelete = async (card) => {
    if (!window.confirm(`Удалить карточку «${card.title}»?`)) return
    try {
      await deleteCard(card.id)
      // если удалили последнюю на странице, возвращаемся назад
      if (data.content.length === 1 && page > 0) setPage(page - 1)
      else load()
    } catch (e) {
      window.alert(`Не удалось удалить: ${e.message}`)
    }
  }

  const badgeFor = (card) => {
    if (card.school_id == null) return { text: 'ТПУ', color: '#0ee48c' }
    const s = schools.find((x) => x.id === card.school_id)
    return s ? { text: s.shortName, color: s.color } : { text: 'ИШ', color: '#3b7499' }
  }

  const cards = data?.content ?? []

  return (
    <div className="admin-page">
      <h1 className="admin-title">Управление карточками</h1>

      <Link to="/admin/cards/new" className="btn btn--green btn--create-mobile">Создать карточку</Link>

      <div className="filters">
        <label className="search">
          <SearchIcon />
          <input
            type="search"
            placeholder="Поиск"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
        </label>
        <div className="filters__selects">
          <div className="field__select">
            <select value={type} onChange={(e) => { setType(e.target.value); setPage(0) }} aria-label="Тип карточек">
              <option value="">Все типы карточек</option>
              {Object.entries(TYPE_NAMES).map(([value, name]) => (
                <option key={value} value={value}>{name}</option>
              ))}
            </select>
            <ChevronDown />
          </div>
          <div className="field__select">
            <select value={schoolId} onChange={(e) => { setSchoolId(e.target.value); setPage(0) }} aria-label="Школа">
              <option value="">Все школы</option>
              {schools.map((s) => (
                <option key={s.id} value={s.id}>{s.shortName} — {s.name}</option>
              ))}
            </select>
            <ChevronDown />
          </div>
        </div>
      </div>

      {status === 'loading' && !data && <div className="admin-state">Загружаем карточки…</div>}
      {status === 'error' && (
        <div className="admin-state">
          <p>Не удалось загрузить карточки. {error}</p>
          <button className="btn btn--outline" onClick={load}>Попробовать ещё раз</button>
        </div>
      )}
      {status !== 'error' && data && cards.length === 0 && <div className="admin-state">Карточек не найдено</div>}

      {status !== 'error' && cards.length > 0 && (
        <div className={`cards-grid${status === 'loading' ? ' cards-grid--loading' : ''}`}>
          {cards.map((card) => {
            const badge = badgeFor(card)
            return (
              <article className="acard" key={card.id}>
                <div className="acard__top">
                  <span className="acard__badge" style={{ background: badge.color }}>{badge.text}</span>
                  <span className="acard__type">{TYPE_NAMES[card.type] || card.type}</span>
                  <span className="acard__date">{shortDate(card.published_at || card.created_at)}</span>
                </div>
                <h2 className="acard__title">{card.title}</h2>
                <p className="acard__text">{card.content}</p>
                <div className="acard__actions">
                  <Link to={`/admin/cards/${card.id}`} state={{ card }} className="btn btn--outline">Редактировать</Link>
                  <button className="btn btn--danger" onClick={() => onDelete(card)}>Удалить</button>
                </div>
              </article>
            )
          })}
        </div>
      )}

      <Pagination page={page} totalPages={data?.totalPages ?? 0} onChange={setPage} />
    </div>
  )
}

export default AdminCardsPage
