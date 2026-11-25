import { ArrowLeft, ArrowRight } from '../components/Icons'

// Номера страниц: 1 2 … 6 7, вокруг текущей показываем соседей
function pageList(current, total) {
  const set = new Set([0, 1, total - 2, total - 1, current - 1, current, current + 1])
  const pages = [...set].filter((p) => p >= 0 && p < total).sort((a, b) => a - b)
  const result = []
  pages.forEach((p, i) => {
    if (i > 0 && p - pages[i - 1] > 1) result.push('gap-' + p)
    result.push(p)
  })
  return result
}

function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null
  return (
    <nav className="pager" aria-label="Страницы">
      <button className="pager__btn pager__btn--arrow" disabled={page === 0} onClick={() => onChange(page - 1)} aria-label="Назад">
        <ArrowLeft size={22} />
      </button>
      {pageList(page, totalPages).map((p) =>
        typeof p === 'string' ? (
          <span key={p} className="pager__btn pager__btn--gap">…</span>
        ) : (
          <button
            key={p}
            className={`pager__btn${p === page ? ' pager__btn--active' : ''}`}
            onClick={() => onChange(p)}
            aria-current={p === page ? 'page' : undefined}
          >
            {p + 1}
          </button>
        ),
      )}
      <button className="pager__btn pager__btn--arrow" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)} aria-label="Вперёд">
        <ArrowRight size={22} />
      </button>
    </nav>
  )
}

export default Pagination
