import './Pagination.css'

/**
 * Pagination component dùng chung toàn project.
 * Tự render null nếu totalPages <= 1.
 * Có jump-to-page input.
 *
 * @param {number} currentPage - Trang hiện tại (1-indexed)
 * @param {number} totalPages - Tổng số trang
 * @param {Function} onPageChange - Callback (page: number) => void
 */
function Pagination({ currentPage, totalPages, onPageChange }) {
  // Không render nếu chỉ có 1 trang
  if (!totalPages || totalPages <= 1) return null

  const handleJump = (e) => {
    if (e.key === 'Enter') {
      const page = parseInt(e.target.value, 10)
      if (page >= 1 && page <= totalPages) {
        onPageChange(page)
        e.target.value = ''
      }
    }
  }

  // Tạo danh sách page numbers với ellipsis
  const getPages = () => {
    const pages = []
    const delta = 1 // Số trang hiển thị xung quanh currentPage

    for (let i = 1; i <= totalPages; i++) {
      if (
        i === 1 ||
        i === totalPages ||
        (i >= currentPage - delta && i <= currentPage + delta)
      ) {
        pages.push(i)
      } else if (pages[pages.length - 1] !== '...') {
        pages.push('...')
      }
    }
    return pages
  }

  return (
    <nav className="pagination" aria-label="Phân trang">
      {/* Nút Previous */}
      <button
        type="button"
        className="pagination__btn btn-icon"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage <= 1}
        aria-label="Trang trước"
      >
        <span className="material-symbols-outlined">chevron_left</span>
      </button>

      {/* Page numbers */}
      <div className="pagination__pages">
        {getPages().map((page, idx) =>
          page === '...' ? (
            <span key={`ellipsis-${idx}`} className="pagination__ellipsis">
              …
            </span>
          ) : (
            <button
              key={page}
              type="button"
              className={`pagination__page ${page === currentPage ? 'pagination__page--active' : ''}`}
              onClick={() => onPageChange(page)}
              aria-label={`Trang ${page}`}
              aria-current={page === currentPage ? 'page' : undefined}
            >
              {page}
            </button>
          )
        )}
      </div>

      {/* Nút Next */}
      <button
        type="button"
        className="pagination__btn btn-icon"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages}
        aria-label="Trang tiếp"
      >
        <span className="material-symbols-outlined">chevron_right</span>
      </button>

      {/* Jump to page */}
      {totalPages > 5 && (
        <div className="pagination__jump">
          <input
            type="number"
            className="pagination__jump-input"
            min={1}
            max={totalPages}
            placeholder="Đi đến"
            onKeyDown={handleJump}
            aria-label="Nhảy đến trang"
          />
        </div>
      )}
    </nav>
  )
}

export default Pagination
