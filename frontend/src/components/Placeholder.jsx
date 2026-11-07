// Серая заглушка с крестом, как в макете, когда у карточки нет картинки
function Placeholder() {
  return (
    <div className="ph" aria-hidden="true">
      <svg viewBox="0 0 100 100" preserveAspectRatio="none">
        <line x1="0" y1="0" x2="100" y2="100" vectorEffect="non-scaling-stroke" />
        <line x1="100" y1="0" x2="0" y2="100" vectorEffect="non-scaling-stroke" />
      </svg>
    </div>
  )
}

export default Placeholder
