// Базовый адрес API. В dev-режиме запросы идут через proxy из vite.config.js,
// на сервере их проксирует nginx (см. nginx.conf).
const BASE = '/api/v1'

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

export async function apiGet(path, params = {}) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value)
  })
  const qs = query.toString()

  let response
  try {
    response = await fetch(`${BASE}${path}${qs ? `?${qs}` : ''}`)
  } catch {
    throw new ApiError('Нет связи с сервером', 0)
  }

  if (!response.ok) {
    // бэкенд отдаёт ошибки в формате ProblemDetail, текст лежит в detail
    let detail = ''
    try {
      detail = (await response.json()).detail
    } catch {
      /* тела может не быть */
    }
    throw new ApiError(detail || `Ошибка сервера (${response.status})`, response.status)
  }
  return response.json()
}
