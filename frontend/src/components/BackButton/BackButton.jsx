import './BackButton.css';

/**
 * BackButton — Nút quay lại dùng chung
 *
 * @param {string} label - Text hiển thị trên nút
 * @param {Function} onClick - Callback khi click
 * @param {string} className - Class CSS bổ sung (optional)
 */
function BackButton({ label, onClick, className = '' }) {
  return (
    <button
      type="button"
      className={`btn-secondary back-button ${className}`.trim()}
      onClick={onClick}
    >
      <span className="material-symbols-outlined">arrow_back</span>
      {label}
    </button>
  );
}

export default BackButton;
