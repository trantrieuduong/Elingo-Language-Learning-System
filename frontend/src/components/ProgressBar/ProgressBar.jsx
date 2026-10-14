import './ProgressBar.css';

/**
 * ProgressBar — Thanh tiến độ chuẩn Elingo (Liquid Glass Minimalism).
 *
 * @param {string} [title] - Tiêu đề tiến độ (ví dụ: 'Tiến độ' hoặc 'Tiến độ tổng quan')
 * @param {number} [current=0] - Số lượng mục đã học / hoàn thành
 * @param {number} [total=0] - Tổng số lượng mục
 * @param {number} [percent] - Phần trăm tùy chọn (nếu không truyền sẽ tự tính từ current & total)
 */
function ProgressBar({
  title,
  current = 0,
  total = 0,
  percent,
}) {
  const calculatedPercent =
    percent !== undefined ? percent : total > 0 ? Math.round((current / total) * 100) : 0;

  const safePercent = Math.min(100, Math.max(0, calculatedPercent));

  const hasHeader = Boolean(title) || total > 0;

  return (
    <div className="progress-bar-container">
      {hasHeader && (
        <div className="progress-bar-header">
          {title && <span className="progress-bar-title">{title}</span>}
          {total > 0 && (
            <span className="progress-bar-stats">
              {current} / {total}  ({safePercent}%)
            </span>
          )}
        </div>
      )}
      <div className="progress-bar-track">
        <div className="progress-bar-fill" style={{ width: `${safePercent}%` }} />
      </div>
    </div>
  );
}

export default ProgressBar;
