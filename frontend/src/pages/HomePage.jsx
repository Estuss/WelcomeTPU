import { Link } from 'react-router-dom'
import SchoolCard from '../components/SchoolCard'
import { ArrowRight } from '../components/Icons'
import { schools } from '../data/schools'
import photoBlob from '../assets/photo-blob.webp'
import photo from '../assets/photo.jpg'
import swirls from '../assets/swirls.svg'
import swirlsMobile from '../assets/swirls-mobile.svg'

function HomePage() {
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
            просмотр карточек. (текст поменяется)
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
        <div className="schools__grid">
          {schools.map((s) => (
            <SchoolCard key={s.id} school={s} />
          ))}
        </div>
      </section>
    </>
  )
}

export default HomePage
