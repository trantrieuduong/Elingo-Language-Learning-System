/**
 * Hiển thị trạng thái hoàn thành học / ôn tập thẻ.
 *
 * @param {string} title - Tiêu đề thông báo (mặc định: 'Tuyệt vời!')
 * @param {string} message - Nội dung thông báo hoàn thành
 */
export default function FlashcardCompletePlaceholder({
  title = 'Tuyệt vời!',
  message
}) {
  return (
    <div className="flashcard-empty-placeholder">
      <span
        className="material-symbols-outlined flashcard-empty-placeholder-icon"
        style={{ color: 'var(--color-success)' }}
      >
        task_alt
      </span>
      <h2 className="text-title-md">{title}</h2>
      {message && (
        <p className="text-body-md" style={{ marginTop: 'var(--spacing-sm)' }}>
          {message}
        </p>
      )}
    </div>
  )
}
