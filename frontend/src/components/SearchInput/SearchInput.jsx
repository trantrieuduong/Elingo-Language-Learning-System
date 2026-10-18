import './SearchInput.css'

/**
 * SearchInput — Ô tìm kiếm dùng chung với nút xóa nội dung.
 *
 * @param {string} id - ID duy nhất của input
 * @param {string} placeholder - Gợi ý nội dung tìm kiếm
 * @param {string} value - Giá trị tìm kiếm hiện tại
 * @param {Function} onChange - Callback khi giá trị thay đổi
 * @param {Function} onClear - Callback xóa nội dung tìm kiếm
 * @param {string} clearLabel - Nhãn truy cập cho nút xóa
 * @param {string} className - Class bổ sung cho wrapper
 */
function SearchInput({ id, placeholder, value, onChange, onClear, clearLabel, className = '' }) {
  return (
    <div className={`search-input ${className}`.trim()}>
      <input
        type="search"
        id={id}
        className="search-input__field"
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        aria-label={placeholder}
      />
      {value && onClear && (
        <button
          type="button"
          className="btn-icon search-input__clear"
          onClick={onClear}
          aria-label={clearLabel}
          title={clearLabel}
        >
          <span className="material-symbols-outlined" aria-hidden="true">
            close
          </span>
        </button>
      )}
    </div>
  )
}

export default SearchInput
