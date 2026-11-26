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

// Порядок школ на сайте, правильные ссылки и запасные логотипы (если на сервере нет).
// [слово в названии, логотип, сокращение, цвет бейджа в админке, сайт школы]
const KNOWN = [
  ['информационных технологий', itr, 'ИШИТР', '#3b7499', 'https://itr.tpu.ru/'],
  ['Бизнес', business, 'БШ', '#26b6d0', 'https://business.tpu.ru/'],
  ['неразрушающего', ndt, 'ИШНКБ', '#1fc5e8', 'https://sndt.tpu.ru/'],
  ['новых производственных', npt, 'ИШНПТ', '#17a595', 'https://newtechnology.tpu.ru/'],
  ['природных ресурсов', resources, 'ИШПР', '#8cd21a', 'https://earth.tpu.ru/'],
  ['энергетики', energy, 'ИШЭ', '#ffb400', 'https://energy.tpu.ru/'],
  ['ядерных', nuclear, 'ИЯТШ', '#2b9bff', 'https://atom.tpu.ru/'],
  ['физики', physics, 'ИШФВП', '#ef5350', 'https://rsp.tpu.ru/'],
  ['химических', chem, 'ИШХБМТ', '#a468d6', 'https://biomed.tpu.ru/'],
  ['Интеллектуальные энергетические', ies, 'ИШИЭС', '#3b7499', 'https://engineers.tpu.ru/'],
  ['общественных', social, 'ШОН', '#ff9a2e', 'https://base.tpu.ru/'],
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
    siteUrl: findKnown(dto.name)?.[4] ?? dto.official_site_url,
    logo: logoFor(dto),
    shortName: findKnown(dto.name)?.[2] ?? 'ИШ',
    color: findKnown(dto.name)?.[3] ?? '#3b7499',
  }
}

// GET /api/v1/schools — список школ для главной
export async function getSchools() {
  const page = await apiGet('/schools', { size: 100, sort: 'name,asc' })
  const order = (name) => {
    const i = KNOWN.findIndex(([word]) => name.includes(word))
    return i === -1 ? KNOWN.length : i
  }
  return page.content.map(toSchool).sort((a, b) => order(a.name) - order(b.name))
}

// GET /api/v1/schools/{id}
export async function getSchool(id) {
  return toSchool(await apiGet(`/schools/${id}`))
}
