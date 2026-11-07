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

// Порядок как в макете. Пока данные заглушки, потом будут приходить с бэкенда.
export const schools = [
  { id: 'business', name: 'Бизнес школа', shortName: 'Бизнес школа', logo: business, siteUrl: 'https://tpu.ru' },
  { id: 'ies', name: 'Инженерная школа «Интеллектуальные энергетические системы»', shortName: 'ИШ ИЭС', logo: ies, siteUrl: 'https://tpu.ru' },
  { id: 'itr', name: 'Инженерная школа информационных технологий и робототехники', shortName: 'ИШИТР', logo: itr, siteUrl: 'https://tpu.ru' },
  { id: 'ndt', name: 'Инженерная школа неразрушающего контроля и безопасности', shortName: 'ИШНКБ', logo: ndt, siteUrl: 'https://tpu.ru' },
  { id: 'npt', name: 'Инженерная школа новых производственных технологий', shortName: 'ИШНПТ', logo: npt, siteUrl: 'https://tpu.ru' },
  { id: 'resources', name: 'Инженерная школа природных ресурсов', shortName: 'ИШПР', logo: resources, siteUrl: 'https://tpu.ru' },
  { id: 'energy', name: 'Инженерная школа энергетики', shortName: 'ИШЭ', logo: energy, siteUrl: 'https://tpu.ru' },
  { id: 'nuclear', name: 'Инженерная школа ядерных технологий', shortName: 'ИЯТШ', logo: nuclear, siteUrl: 'https://tpu.ru' },
  { id: 'physics', name: 'Исследовательская школа физики высокоэнергетических процессов', shortName: 'ИШФВП', logo: physics, siteUrl: 'https://tpu.ru' },
  { id: 'chem', name: 'Исследовательская школа химических и биомедицинских технологий', shortName: 'ИШХБМТ', logo: chem, siteUrl: 'https://tpu.ru' },
  { id: 'social', name: 'Школа общественных наук', shortName: 'ШОН', logo: social, siteUrl: 'https://tpu.ru' },
]

export function getSchool(id) {
  return schools.find((s) => s.id === id)
}
