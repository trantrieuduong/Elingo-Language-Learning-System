import './SlugRegenerateButton.css'

function SlugRegenerateButton({ label, onClick, disabled = false }) {
  return (
    <button
      type="button"
      className="btn-icon slug-regenerate-button"
      onClick={onClick}
      title={label}
      aria-label={label}
      disabled={disabled}
    >
      <span className="material-symbols-outlined" aria-hidden="true">
        sync
      </span>
    </button>
  )
}

export default SlugRegenerateButton
