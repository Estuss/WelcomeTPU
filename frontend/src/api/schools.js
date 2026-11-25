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
// [слово в названии, логотип, сокращение, цвет бейджа в админке]
const KNOWN = [
  ['Бизнес', business, 'БШ', '#26b6d0'],
  ['Интеллектуальные энергетические', ies, 'ИШИЭС', '#3b7499'],
  ['информационных технологий', itr, 'ИШИТР', '#3b7499'],
  ['неразрушающего', ndt, 'ИШНКБ', '#1fc5e8'],
  ['новых производственных', npt, 'ИШНПТ', '#17a595'],
  ['природных ресурсов', resources, 'ИШПР', '#8cd21a'],
  ['энергетики', energy, 'ИШЭ', '#ffb400'],
  ['ядерных', nuclear, 'ИЯТШ', '#2b9bff'],
  ['физики', physics, 'ИШФВП', '#ef5350'],
  ['химических', chem, 'ИШХБМТ', '#a468d6'],
  ['общественных', social, 'ШОН', '#ff9a2e'],
]

function findKnown(name) {
  return KNOWN.find(([word]) => name.includes(word))
}

function logoFor(dto) {
  if (dto.icon?.url) return dto.icon.url
  return findKnown(dto.name)?.[1] ?? null
}

function toSchool(dto) {
  return {
    id: dto.id,
    name: dto.name,
    description: dto.short_description,
    siteUrl: dto.official_site_url,
    logo: logoFor(dto),
    shortName: findKnown(dto.name)?.[2] ?? 'ИШ',
    color: findKnown(dto.name)?.[3] ?? '#3b7499',
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
