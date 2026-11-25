import { apiDelete, apiGet, apiPost, apiPut, apiUpload } from './client'

// ---------- вход ----------
// GET /auth/me — 401, если не вошли
export const getMe = () => apiGet('/auth/me')
// GET /auth/login — адрес страницы входа oauth.tpu.ru
export const getLoginUrl = () => apiGet('/auth/login').then((r) => r.authorization_url)
export const logout = () => apiPost('/auth/logout')

// ---------- карточки ----------
export const PAGE_SIZE = 9

// GET /admin/cards — список для админки (вместе с черновиками)
export function getAdminCards({ page = 0, search, type, schoolId }) {
  return apiGet('/admin/cards', { page, size: PAGE_SIZE, search, type, school_id: schoolId })
}

// В админском API нет GET /admin/cards/{id}, поэтому для редактирования
// берём публичную карточку. Черновики так не откроются (будет 404).
export const getCard = (id) => apiGet(`/cards/${id}`)

export const createCard = (body) => apiPost('/admin/cards', body)
export const updateCard = (id, body) => apiPut(`/admin/cards/${id}`, body)
export const deleteCard = (id) => apiDelete(`/admin/cards/${id}`)

// POST /admin/cards/{id}/images — картинка привязывается к уже созданной карточке
export function uploadCardImage(cardId, file, altText) {
  const form = new FormData()
  form.append('file', file)
  if (altText) form.append('alt_text', altText)
  return apiUpload(`/admin/cards/${cardId}/images`, form)
}
export const deleteCardImage = (cardId, assetId) => apiDelete(`/admin/cards/${cardId}/images/${assetId}`)
