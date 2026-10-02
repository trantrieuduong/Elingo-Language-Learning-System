import { useEffect } from 'react'
import './Modal.css'

/**
 * Modal component dùng chung toàn project.
 * Dùng cho confirm/alert dialogs. Đóng khi click overlay.
 *
 * @param {boolean} isOpen - Hiển thị modal hay không
 * @param {string} title - Tiêu đề modal
 * @param {string} message - Nội dung mô tả
 * @param {string} confirmText - Text nút confirm (default: 'Xác nhận')
 * @param {string} cancelText - Text nút cancel (default: 'Hủy')
 * @param {Function} onConfirm - Callback khi nhấn confirm
 * @param {Function} onCancel - Callback khi nhấn cancel / click overlay
 * @param {boolean} isDanger - Nếu true → nút confirm dùng màu danger
 * @param {boolean} isLoading - Disable nút khi đang xử lý
 */
function Modal({
  isOpen,
  title,
  message,
  confirmText = 'Xác nhận',
  cancelText = 'Hủy',
  onConfirm,
  onCancel,
  isDanger = false,
  isLoading = false,
}) {
  // ── Đóng modal khi nhấn Escape ──
  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !isLoading) onCancel?.()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, isLoading, onCancel])

  // ── Khóa scroll body khi modal mở ──
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [isOpen])

  if (!isOpen) return null

  return (
    <div
      className="modal-overlay"
      onClick={() => !isLoading && onCancel?.()}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      {/* stopPropagation ngăn click bên trong đóng modal */}
      <div className="modal-container glass-card" onClick={(e) => e.stopPropagation()}>
        <h2 id="modal-title" className="modal-title text-headline-sm">
          {title}
        </h2>

        {message && <p className="modal-message text-body-md">{message}</p>}

        <div className="modal-actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={onCancel}
            disabled={isLoading}
          >
            {cancelText}
          </button>

          <button
            type="button"
            className={`modal-confirm-btn ${isDanger ? 'modal-confirm-btn--danger' : 'btn-primary'}`}
            onClick={onConfirm}
            disabled={isLoading}
          >
            {isLoading ? (
              <span className="modal-spinner" aria-hidden="true" />
            ) : (
              confirmText
            )}
          </button>
        </div>
      </div>
    </div>
  )
}

export default Modal
