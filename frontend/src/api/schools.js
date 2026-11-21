import { apiGet } from './client'
import business from '../assets/logos/business.png'
import ies from '../assets/logos/ies.png'
import itr from '../assets/logos/itr.png'
import ndt from '../assets/logos/ndt.png'
import npt from '../assets/logos/npt.png'
import resources from '../assets/logos/resources.png'
import energy from '../assets/logos/energy.png'
import nuclear from '../assets/logos/nuclear.png'
import physics from '../assets/logos/physics.png'
import chem from '../assets/logos/chem.png'
import social from '../assets/logos/social.png'

// Если у школы на сервере ещё не загружен логотип, берём локальный по названию.
const LOCAL_LOGOS = [
  ['Бизнес', business],
  ['Интеллектуальные энергетические', ies],
  ['информационных технологий', itr],
  ['неразрушающего', ndt],
  ['новых производственных', npt],
  ['природных ресурсов', resources],
  ['энергетики', energy],
  ['ядерных', nuclear],
  ['физики', physics],
  ['химических', chem],
  ['общественных', social],
]

function logoFor(dto) {
  if (dto.icon?.url) return dto.icon.url
  const found = LOCAL_LOGOS.find(([word]) => dto.name.includes(word))
  return found ? found[1] : null
}

function toSchool(dto) {
  return {
    id: dto.id,
    name: dto.name,
    description: dto.short_description,
    siteUrl: dto.official_site_url,
    logo: logoFor(dto),
  }
}

// GET /api/v1/schools — список школ для главной
export async function getSchools() {
  const page = await apiGet('/schools', { size: 100, sort: 'name,asc' })
  return page.content.map(toSchool)
}

// GET /api/v1/schools/{id}
export async function getSchool(id) {
  return toSchool(await apiGet(`/schools/${id}`))
}
