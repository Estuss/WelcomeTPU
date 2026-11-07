import { Link } from 'react-router-dom'
import Placeholder from './Placeholder'
import { ArrowUpRight } from './Icons'

const LABELS = {
  FACT: 'Факт',
  HISTORY: 'История',
  EVENT: 'Мероприятие',
  STATISTIC: 'Статистика',
}

function ExtLink({ href, children }) {
  return (
    <a className="card__link" href={href} target="_blank" rel="noreferrer">
      {children} <ArrowUpRight />
    </a>
  )
}

function Card({ card }) {
  const label = LABELS[card.type] || ''

  if (card.type === 'STATISTIC') {
    return (
      <div className="card card--stat">
        <div className="card__label">{label}</div>
        <h2 className="card__title">{card.title}</h2>
        <div className="stat">
          <div className="stat__value">{card.value}</div>
          <div className="stat__label">{card.label}</div>
        </div>
        {card.sourceUrl && <ExtLink href={card.sourceUrl}>Источник</ExtLink>}
      </div>
    )
  }

  if (card.type === 'EVENT') {
    return (
      <div className="card card--event">
        <div className="card__mobile-img">
          {card.imageUrl ? <img src={card.imageUrl} alt="" /> : <Placeholder />}
        </div>
        <div className="card__body">
          <div className="card__label">{label}</div>
          <h2 className="card__title">{card.title}</h2>
          <div className="event__content">
            <div className="event__main">
              <p className="card__text">{card.text}</p>
              <ul className="event__info">
                {card.date && <li>{card.date}</li>}
                {card.time && <li>{card.time}</li>}
                {card.place && <li>{card.place}</li>}
              </ul>
            </div>
            <div className="event__img">
              {card.imageUrl ? <img src={card.imageUrl} alt="" /> : <Placeholder />}
            </div>
          </div>
          {card.linkUrl && <ExtLink href={card.linkUrl}>Подробнее</ExtLink>}
        </div>
      </div>
    )
  }

  // FACT и HISTORY: текст слева, картинка справа (на телефоне сверху)
  return (
    <div className="card card--media">
      <div className="card__mobile-img">
        {card.imageUrl ? <img src={card.imageUrl} alt="" /> : <Placeholder />}
      </div>
      <div className="card__body">
        <div className="card__label">{label}</div>
        <h2 className="card__title">{card.title}</h2>
        <p className="card__text">{card.text}</p>
        {card.sourceUrl && <ExtLink href={card.sourceUrl}>Источник</ExtLink>}
      </div>
      <div className="card__side">
        {card.imageUrl ? <img src={card.imageUrl} alt="" /> : <Placeholder />}
      </div>
    </div>
  )
}

export function FinishCard() {
  return (
    <div className="card card--finish">
      <h2 className="finish__title">Спасибо за просмотр</h2>
      <Link className="finish__link" to="/">Вернуться на главную</Link>
    </div>
  )
}

export default Card
