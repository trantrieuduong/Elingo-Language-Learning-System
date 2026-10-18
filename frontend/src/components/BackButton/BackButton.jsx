function BackButton({ label, onClick }) {
  return (
    <button type="button" className="btn-secondary" onClick={onClick}>
      <span className="material-symbols-outlined" aria-hidden="true">
        arrow_back
      </span>
      {label}
    </button>
  )
}

export default BackButton
