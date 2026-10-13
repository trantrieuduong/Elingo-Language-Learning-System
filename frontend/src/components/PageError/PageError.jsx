import './PageError.css'

/**
 * Lỗi toàn trang (do server sập)
 */
export default function PageError({ error, onAction, actionLabel = 'Quay lại trang chủ' }) {
  if (!error) return null

  return (
    <div className="page-loading">
      <div className="page-error-container">
        <span className="material-symbols-outlined page-error-icon">error</span>
        <p style={{ margin: 0 }}>{error}</p>
        <button
          className="btn-primary page-error-btn"
          onClick={onAction}
        >
          {actionLabel}
        </button>
      </div>
    </div>
  )
}
