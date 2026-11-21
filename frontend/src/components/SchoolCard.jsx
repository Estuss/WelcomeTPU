import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight } from './Icons'

function SchoolCard({ school, index = 0 }) {
  const ref = useRef(null)
  const [visible, setVisible] = useState(false)

  // карточка плавно появляется, когда доезжает до экрана
  useEffect(() => {
    const el = ref.current
    if (!el || !('IntersectionObserver' in window)) {
      setVisible(true)
      return
    }
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true)
          obs.disconnect()
        }
      },
      { threshold: 0.15 },
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [])

  return (
    <article
      ref={ref}
      className={`school${visible ? ' school--visible' : ''}`}
      style={{ '--delay': `${(index % 4) * 80}ms` }}
    >
      <h3 className="school__title">{school.name}</h3>
      <a className="school__site" href={school.siteUrl} target="_blank" rel="noreferrer">
        Сайт школы
      </a>
      {school.logo && <img className="school__logo" src={school.logo} alt="" />}
      <Link className="school__btn" to={`/cards/${school.id}`}>
        <span>К карточкам школы</span>
        <ArrowRight size={28} />
      </Link>
    </article>
  )
}

export default SchoolCard
