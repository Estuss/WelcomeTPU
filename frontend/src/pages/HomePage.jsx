import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import SchoolCard from '../components/SchoolCard'
import { ArrowRight } from '../components/Icons'
import { getSchools } from '../api/schools'
import photoBlob from '../assets/photo-blob.webp'
import photo from '../assets/photo.jpg'
import swirls from '../assets/swirls.svg'
import swirlsMobile from '../assets/swirls-mobile.svg'

function HomePage() {
  const [schools, setSchools] = useState([])
  const [status, setStatus] = useState('loading') // loading | error | ready
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setStatus('loading')
    getSchools()
      .then((data) => {
        setSchools(data)
        setStatus('ready')
      })
      .catch((e) => {
        setError(e.message)
        setStatus('error')
      })
  }, [])

  useEffect(() => {
    load()
  }, [load])

  return (
    <>
      <section className="hero">
        <picture>
          <source media="(max-width: 767px)" srcSet={swirlsMobile} />
          <img className="hero__swirls" src={swirls} alt="" />
        </picture>
        <div className="hero__photo-mobile" style={{ backgroundImage: `url(${photo})` }} />
        <img className="hero__photo" src={photoBlob} alt="Главный корпус ТПУ" />
        <div className="hero__content">
          <h1 className="hero__title">
            Добро пожаловать в <br className="br-desktop" />
            Томский Политехнический Университет
          </h1>
          <p className="hero__text">
            Изучайте интересные факты, истории студентов, статистику и предстоящие мероприятия.
            <br />
            Выберите категорию «ТПУ в целом» или интересующую вас инженерную школу, чтобы начать
            просмотр карточек.
          </p>
          <Link to="/cards" className="about">
            <span className="about__title">О Политехе</span>
            <span className="about__more">
              Читайте в нашей подборке
              <ArrowRight size={28} />
            </span>
          </Link>
        </div>
      </section>

      <section className="schools">
        <h2 className="schools__title">Наши школы</h2>
        {status === 'loading' && <div className="schools__state">Загружаем школы…</div>}
        {status === 'error' && (
          <div className="schools__state">
            <p>Не удалось загрузить школы. {error}</p>
            <button className="state__btn" onClick={load}>Попробовать ещё раз</button>
          </div>
        )}
        {status === 'ready' && schools.length === 0 && (
          <div className="schools__state">Школ пока нет</div>
        )}
        {status === 'ready' && schools.length > 0 && (
          <div className="schools__grid">
            {schools.map((s, i) => (
              <SchoolCard key={s.id} school={s} index={i} />
            ))}
          </div>
        )}
      </section>
    </>
  )
}

export default HomePage
