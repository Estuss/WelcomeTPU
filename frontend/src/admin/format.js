export const TYPE_NAMES = {
  FACT: 'Факт',
  EVENT: 'Мероприятие',
  STATISTIC: 'Статистика',
  PERSON: 'Персона',
  MEDIA: 'Медиа',
  QR: 'QR-код',
}

// Типы, которые можно создавать в админке (у остальных в форме пока нет полей)
export const EDITABLE_TYPES = ['FACT', 'EVENT', 'STATISTIC']

// "2026-11-25T10:00:00Z" -> "25.11.26"
export function shortDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const dd = String(d.getDate()).padStart(2, '0')
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const yy = String(d.getFullYear()).slice(-2)
  return `${dd}.${mm}.${yy}`
}
