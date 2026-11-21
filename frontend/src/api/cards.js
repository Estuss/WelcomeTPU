import { apiGet } from './client'

const dateFmt = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })

function formatDate(iso) {
  return iso ? dateFmt.format(new Date(iso)) : ''
}

function formatTime(t) {
  return t ? t.slice(0, 5) : '' // "10:00:00" -> "10:00"
}

// Превращаем карточку из API в то, что рисуют компоненты
function toCard(dto) {
  const card = {
    id: dto.id,
    type: dto.type,
    title: dto.title,
    text: dto.content,
    imageUrl: dto.images?.[0]?.url || null,
    sourceUrl: dto.sources?.[0]?.url || null,
  }

  if (dto.type === 'EVENT' && dto.event_details) {
    const e = dto.event_details
    const start = formatDate(e.start_date)
    const end = formatDate(e.end_date)
    const times = [formatTime(e.start_time), formatTime(e.end_time)].filter(Boolean).join('–')
    card.date = end && end !== start ? `${start} – ${end}` : start
    card.time = times
    card.place = e.location
    card.linkUrl = e.details_url
  }

  if (dto.type === 'STATISTIC' && dto.statistic_details) {
    const s = dto.statistic_details
    card.value = Number(s.value).toLocaleString('ru-RU')
    card.label = [s.unit, s.period].filter(Boolean).join(', ')
  }

  if (dto.type === 'QR' && dto.qr_details) {
    card.sourceUrl = dto.qr_details.target_url
  }

  return card
}

// GET /api/v1/cards — без schoolId приходят карточки «ТПУ в целом»
export async function getCards(schoolId) {
  const page = await apiGet('/cards', { school_id: schoolId, size: 1000 })
  return page.content.map(toCard)
}
