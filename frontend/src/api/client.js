// Базовый адрес API. В dev-режиме запросы идут через proxy из vite.config.js,
// на сервере их проксирует nginx (см. nginx.conf).
const BASE = '/api/v1'

export class ApiError extends Error {
  constructor(message, status, fields = []) {
    super(message)
    this.status = status
    this.fields = fields // ошибки по полям для 400: [{ field, message }]
  }
}

async function request(method, path, { params = {}, json, form } = {}) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value)
  })
  const qs = query.toString()

  const headers = {}
  let body
  if (method !== 'GET') headers['X-Requested-With'] = 'XMLHttpRequest' // для проверки на бэке
  if (json !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(json)
  } else if (form) {
    body = form // Content-Type с boundary браузер поставит сам
  }

  let response
  try {
    response = await fetch(`${BASE}${path}${qs ? `?${qs}` : ''}`, { method, headers, body })
  } catch {
    throw new ApiError('Нет связи с сервером', 0)
  }

  if (!response.ok) {
    // бэкенд отдаёт ошибки в формате ProblemDetail, текст лежит в detail
    let problem = {}
    try {
      problem = await response.json()
    } catch {
      /* тела может не быть */
    }
    throw new ApiError(problem.detail || `Ошибка сервера (${response.status})`, response.status, problem.errors || [])
  }
  if (response.status === 204) return null
  return response.json()
}

export const apiGet = (path, params) => request('GET', path, { params })
export const apiPost = (path, json) => request('POST', path, { json })
export const apiPut = (path, json) => request('PUT', path, { json })
export const apiDelete = (path) => request('DELETE', path)
export const apiUpload = (path, form) => request('POST', path, { form })
