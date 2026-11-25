import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { createCard, deleteCard, getCard, updateCard, uploadCardImage } from '../api/admin'
import { getSchools } from '../api/schools'
import { EDITABLE_TYPES, TYPE_NAMES } from './format'

const MAX_IMAGE_MB = 10
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'] // что принимает бэкенд

const EMPTY = {
  school: '', // '' не выбрано, 'tpu' — "ТПУ в целом", иначе id школы
  type: '',
  title: '',
  content: '',
  source: '',
  date: '',
  time: '',
  place: '',
  value: '',
  unit: '',
  period: '',
}

// "10:00 - 14:00" -> { start: "10:00", end: "14:00" }
function parseTime(text) {
  const m = text.trim().match(/^(\d{1,2}:\d{2})\s*(?:[-–—]\s*(\d{1,2}:\d{2}))?$/)
  if (!m) return null
  const pad = (t) => (t.length === 4 ? '0' + t : t) + ':00'
  return { start: pad(m[1]), end: m[2] ? pad(m[2]) : null }
}

// Карточка с сервера -> значения полей формы
function toForm(card) {
  const f = {
    ...EMPTY,
    school: card.school_id == null ? 'tpu' : String(card.school_id),
    type: card.type,
    title: card.title,
    content: card.content,
    source: card.sources?.[0]?.url || '',
  }
  if (card.event_details) {
    const e = card.event_details
    f.date = e.start_date || ''
    f.time = [e.start_time, e.end_time].filter(Boolean).map((t) => t.slice(0, 5)).join(' - ')
    f.place = e.location || ''
    f.source = e.details_url || f.source
  }
  if (card.statistic_details) {
    const s = card.statistic_details
    f.value = s.value != null ? String(s.value) : ''
    f.unit = s.unit || ''
    f.period = s.period || ''
  }
  return f
}

function ChevronDown() {
  return (
    <svg className="field__chevron" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M5 9l7 7 7-7" />
    </svg>
  )
}

function UploadIcon() {
  return (
    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M12 16V4M7 8l5-5 5 5M4 15v4a1 1 0 001 1h14a1 1 0 001-1v-4" />
    </svg>
  )
}

function ImageIcon() {
  return (
    <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <circle cx="9" cy="9" r="1.5" />
      <path d="M21 16l-5-5-9 9" />
    </svg>
  )
}

function Field({ label, required, error, children, className = '' }) {
  return (
    <div className={`field ${error ? 'field--error' : ''} ${className}`}>
      <label className="field__label">
        {label}
        {required && <span className="field__req"> *</span>}
      </label>
      {children}
      {error && <div className="field__error">{error}</div>}
    </div>
  )
}

// Блок "Медиа": загрузка картинки или превью уже загруженной
function Media({ className, previewUrl, hasImage, onPick, error }) {
  const inputRef = useRef(null)
  const [drag, setDrag] = useState(false)

  const open = () => inputRef.current?.click()
  const onDrop = (e) => {
    e.preventDefault()
    setDrag(false)
    if (e.dataTransfer.files?.[0]) onPick(e.dataTransfer.files[0])
  }

  return (
    <section className={`panel media ${className}`}>
      <h2 className="panel__title">Медиа</h2>
      <input
        ref={inputRef}
        type="file"
        accept={IMAGE_TYPES.join(',')}
        hidden
        onChange={(e) => {
          if (e.target.files?.[0]) onPick(e.target.files[0])
          e.target.value = ''
        }}
      />
      {hasImage ? (
        <>
          <div className="media__preview">
            {previewUrl ? <img src={previewUrl} alt="" /> : <ImageIcon />}
          </div>
          <button type="button" className="btn btn--outline btn--block" onClick={open}>Изменить изображение</button>
        </>
      ) : (
        <button
          type="button"
          className={`media__drop${drag ? ' media__drop--drag' : ''}`}
          onClick={open}
          onDragOver={(e) => { e.preventDefault(); setDrag(true) }}
          onDragLeave={() => setDrag(false)}
          onDrop={onDrop}
        >
          <UploadIcon />
          <strong>Загрузить изображение</strong>
          <span>PNG, JPG или WEBP (макс. {MAX_IMAGE_MB} МБ)</span>
        </button>
      )}
      {error && <div className="field__error">{error}</div>}
    </section>
  )
}

// на телефоне подсказки в списках короче, иначе не помещаются
function useIsMobile() {
  const query = '(max-width: 767px)'
  const [mobile, setMobile] = useState(() => window.matchMedia(query).matches)
  useEffect(() => {
    const mq = window.matchMedia(query)
    const onChange = () => setMobile(mq.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])
  return mobile
}

function AdminCardFormPage() {
  const isMobile = useIsMobile()
  const { id } = useParams()
  const isNew = !id
  const navigate = useNavigate()
  const location = useLocation()

  const [schools, setSchools] = useState([])
  const [card, setCard] = useState(location.state?.card ?? null) // исходная карточка при редактировании
  const [loadStatus, setLoadStatus] = useState(isNew || location.state?.card ? 'ready' : 'loading')
  const [loadError, setLoadError] = useState('')
  const [form, setForm] = useState(() => (location.state?.card ? toForm(location.state.card) : EMPTY))
  const [file, setFile] = useState(null)
  const [filePreview, setFilePreview] = useState('')
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    getSchools().then(setSchools).catch(() => {})
  }, [])

  // открыли ссылку на редактирование напрямую: грузим карточку с сервера
  useEffect(() => {
    if (isNew || card) return
    getCard(id)
      .then((c) => {
        setCard(c)
        setForm(toForm(c))
        setLoadStatus('ready')
      })
      .catch((e) => {
        setLoadError(e.status === 404 ? 'Карточка не найдена (черновики здесь пока не открываются)' : e.message)
        setLoadStatus('error')
      })
  }, [id, isNew, card])

  // превью выбранного файла
  useEffect(() => {
    if (!file) {
      setFilePreview('')
      return undefined
    }
    const url = URL.createObjectURL(file)
    setFilePreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  const set = (name) => (e) => {
    setForm((f) => ({ ...f, [name]: e.target.value }))
    setErrors((er) => ({ ...er, [name]: undefined }))
  }

  const pickFile = (f) => {
    if (!IMAGE_TYPES.includes(f.type)) {
      setErrors((er) => ({ ...er, file: 'Подходят только PNG, JPG и WEBP' }))
      return
    }
    if (f.size > MAX_IMAGE_MB * 1024 * 1024) {
      setErrors((er) => ({ ...er, file: `Файл больше ${MAX_IMAGE_MB} МБ` }))
      return
    }
    setErrors((er) => ({ ...er, file: undefined }))
    setFile(f)
  }

  const existingImage = card?.images?.[0] || null
  const hasImage = Boolean(file || existingImage)
  const previewUrl = filePreview || existingImage?.url || ''

  const unsupported = card && !EDITABLE_TYPES.includes(card.type)

  const validate = () => {
    const er = {}
    if (!form.school) er.school = 'Выберите подразделение'
    if (!form.type) er.type = 'Выберите тип'
    if (!form.title.trim()) er.title = 'Введите заголовок'
    if (!form.content.trim()) er.content = 'Введите текст карточки'
    if (form.type === 'EVENT') {
      if (!form.date) er.date = 'Укажите дату'
      if (form.time.trim() && !parseTime(form.time)) er.time = 'Формат: 10:00 - 14:00'
    }
    if (form.type === 'STATISTIC') {
      if (form.value === '' || Number.isNaN(Number(form.value))) er.value = 'Введите число'
    }
    if (form.source.trim() && !/^https?:\/\/\S+$/i.test(form.source.trim())) er.source = 'Ссылка должна начинаться с http:// или https://'
    setErrors(er)
    return Object.keys(er).length === 0
  }

  // Тело запроса POST / PUT /admin/cards
  const buildBody = () => {
    const source = form.source.trim()
    const extraSources = card?.sources?.slice(1).map((s) => ({ title: s.title, url: s.url })) ?? []
    const body = {
      type: form.type,
      school_id: form.school === 'tpu' ? null : Number(form.school),
      title: form.title.trim(),
      content: form.content,
      content_format: card?.content_format || 'PLAIN',
      // «Сохранить и опубликовать»: у новой карточки дата публикации — сейчас
      published_at: card?.published_at || new Date().toISOString(),
      // PUT заменяет карточку целиком, поэтому картинки и источники отправляем снова
      image_ids: file ? [] : (card?.images ?? []).map((i) => i.id),
      sources: [],
    }

    if (form.type === 'EVENT') {
      const t = form.time.trim() ? parseTime(form.time) : null
      const details = {
        start_date: form.date,
        end_date: card?.event_details?.end_date ?? null,
        start_time: t?.start ?? null,
        end_time: t?.end ?? null,
        location: form.place.trim() || null,
        details_url: source || null,
      }
      body.event_details = details
      body.eventDetails = details // бэкенд пока читает это поле без snake_case
      body.sources = card?.sources?.map((s) => ({ title: s.title, url: s.url })) ?? []
    } else {
      if (source) body.sources = [{ title: card?.sources?.[0]?.title ?? null, url: source }, ...extraSources]
      else body.sources = extraSources
    }

    if (form.type === 'STATISTIC') {
      const details = {
        value: Number(form.value),
        unit: form.unit.trim() || null,
        period: form.period.trim() || null,
        source: card?.statistic_details?.source ?? null,
      }
      body.statistic_details = details
      body.statisticDetails = details
    }
    return body
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setServerError('')
    if (!validate()) return

    setSaving(true)
    let saved = null
    try {
      saved = isNew ? await createCard(buildBody()) : await updateCard(id, buildBody())
      if (file) await uploadCardImage(saved.id, file)
      navigate('/admin')
    } catch (err) {
      if (saved && file) {
        // сама карточка сохранилась, не загрузилась только картинка
        setServerError(`Карточка сохранена, но картинку загрузить не удалось: ${err.message}`)
        if (isNew) navigate(`/admin/cards/${saved.id}`, { replace: true })
      } else {
        const fields = err.fields?.length ? ` (${err.fields.map((f) => `${f.field}: ${f.message}`).join('; ')})` : ''
        setServerError(`${err.message}${fields}`)
      }
      setSaving(false)
    }
  }

  const onDelete = async () => {
    if (!window.confirm(`Удалить карточку «${card.title}»?`)) return
    setSaving(true)
    try {
      await deleteCard(id)
      navigate('/admin')
    } catch (err) {
      setServerError(`Не удалось удалить: ${err.message}`)
      setSaving(false)
    }
  }

  if (loadStatus === 'loading') return <div className="admin-state">Загружаем карточку…</div>
  if (loadStatus === 'error') {
    return (
      <div className="admin-state">
        <p>{loadError}</p>
        <Link to="/admin" className="btn btn--outline">К списку</Link>
      </div>
    )
  }

  return (
    <div className="admin-page">
      <h1 className="admin-title">
        <span className="admin-title__prefix">Админ-панель: </span>
        {isNew ? 'Создание новой карточки' : 'Редактирование'}
      </h1>

      {unsupported && (
        <div className="notice">
          Карточки типа «{TYPE_NAMES[card.type]}» пока нельзя редактировать в админке: для них ещё нет полей в форме.
        </div>
      )}

      <form className="form" onSubmit={onSubmit} noValidate>
        <fieldset className="panel form__fields" disabled={saving || unsupported}>
          <Field label="Привязка к школе / категории" required error={errors.school}>
            <div className="field__select">
              <select value={form.school} onChange={set('school')} className={form.school ? '' : 'is-empty'}>
                <option value="">{isMobile ? 'Выберите подразделение...' : 'Выберите подразделение (например, О ТПУ, ИШИТР, ИШЭ...)'}</option>
                <option value="tpu">О ТПУ</option>
                {schools.map((s) => (
                  <option key={s.id} value={s.id}>{s.shortName} - {s.name}</option>
                ))}
              </select>
              <ChevronDown />
            </div>
          </Field>

          <Field label="Тип заголовка" required error={errors.type}>
            <div className="field__select">
              <select value={form.type} onChange={set('type')} className={form.type ? '' : 'is-empty'}>
                <option value="">{isMobile ? 'Выберите тип...' : 'Выберите тип (Факт, Мероприятие, Статистика)'}</option>
                {EDITABLE_TYPES.map((t) => (
                  <option key={t} value={t}>{TYPE_NAMES[t]}</option>
                ))}
                {card && !EDITABLE_TYPES.includes(card.type) && <option value={card.type}>{TYPE_NAMES[card.type]}</option>}
              </select>
              <ChevronDown />
            </div>
          </Field>

          <Field label="Заголовок" required error={errors.title}>
            <input type="text" maxLength={1024} placeholder="Введите короткий и емкий заголовок" value={form.title} onChange={set('title')} />
          </Field>

          <Field label="Основной текст" required error={errors.content}>
            <textarea rows={5} placeholder="Основной текст, который будет отображаться на карточке..." value={form.content} onChange={set('content')} />
          </Field>

          <Media className="media--inline" previewUrl={previewUrl} hasImage={hasImage} onPick={pickFile} error={errors.file} />

          {form.type === 'STATISTIC' && (
            <>
              <Field label="Значение" required error={errors.value}>
                <input type="text" inputMode="decimal" placeholder="Например, 13500" value={form.value} onChange={set('value')} />
              </Field>
              <div className="field-row">
                <Field label="Единица измерения">
                  <input type="text" placeholder="человек" value={form.unit} onChange={set('unit')} />
                </Field>
                <Field label="Период">
                  <input type="text" placeholder="2024/2025" value={form.period} onChange={set('period')} />
                </Field>
              </div>
            </>
          )}

          <Field label={form.type === 'EVENT' ? 'Ссылка на регистрацию / подробности' : 'Источник'} error={errors.source}>
            <input type="url" placeholder="Вставьте ссылку" value={form.source} onChange={set('source')} />
          </Field>

          {form.type === 'EVENT' && (
            <>
              <div className="field-row">
                <Field label="Дата" required error={errors.date}>
                  <input type="date" value={form.date} onChange={set('date')} className={form.date ? '' : 'is-empty'} />
                </Field>
                <Field label="Время" error={errors.time}>
                  <input type="text" placeholder="00:00 - 23:59" value={form.time} onChange={set('time')} />
                </Field>
              </div>
              <Field label="Место проведения">
                <input type="text" placeholder="Главный корпус, ауд. 227" value={form.place} onChange={set('place')} />
              </Field>
            </>
          )}
        </fieldset>

        <div className="form__side">
          <Media className="media--side" previewUrl={previewUrl} hasImage={hasImage} onPick={pickFile} error={errors.file} />

          <section className="panel publish">
            <h2 className="panel__title">Публикация</h2>
            {serverError && <div className="notice notice--error" role="alert">{serverError}</div>}
            <button type="submit" className="btn btn--green btn--block btn--lg" disabled={saving || unsupported}>
              {saving ? 'Сохраняем…' : 'Сохранить и опубликовать'}
            </button>
            <div className={`publish__row${isNew ? '' : ' publish__row--two'}`}>
              {isNew ? (
                <Link to="/admin" className="btn btn--danger btn--block">Отменить</Link>
              ) : (
                <>
                  <Link to="/admin" className="btn btn--outline btn--block">Отменить</Link>
                  <button type="button" className="btn btn--danger btn--block" onClick={onDelete} disabled={saving}>Удалить</button>
                </>
              )}
            </div>
          </section>
        </div>
      </form>
    </div>
  )
}

export default AdminCardFormPage
