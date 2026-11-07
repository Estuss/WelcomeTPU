import { Link } from 'react-router-dom'
import { ArrowRight } from './Icons'

function SchoolCard({ school }) {
  return (
    <article className="school">
      <h3 className="school__title">{school.name}</h3>
      <a className="school__site" href={school.siteUrl} target="_blank" rel="noreferrer">
        Сайт школы
      </a>
      <img className="school__logo" src={school.logo} alt="" />
      <Link className="school__btn" to={`/cards/${school.id}`}>
        <span>К карточкам школы</span>
        <ArrowRight size={28} />
      </Link>
    </article>
  )
}

export default SchoolCard
