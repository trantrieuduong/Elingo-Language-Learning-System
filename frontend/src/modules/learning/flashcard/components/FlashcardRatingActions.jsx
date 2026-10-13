const RATING_BUTTONS = [
  {
    grade: 0,
    label: 'Học lại',
    icon: 'replay',
    btnClass: 'btn-secondary flashcard-btn-rating flashcard-btn-rating--again'
  },
  {
    grade: 1,
    label: 'Khó',
    icon: 'psychology',
    btnClass: 'btn-secondary flashcard-btn-rating flashcard-btn-rating--hard'
  },
  {
    grade: 2,
    label: 'Dễ',
    icon: 'sentiment_satisfied',
    btnClass: 'btn-secondary flashcard-btn-rating flashcard-btn-rating--good'
  },
  {
    grade: 3,
    label: 'Quá dễ',
    icon: 'sentiment_very_satisfied',
    btnClass: 'btn-primary flashcard-btn-rating flashcard-btn-rating--easy'
  }
]

/**
 * 4 nút đánh giá mức độ ghi nhớ SM-2 (Học lại, Khó, Dễ, Quá dễ).
 *
 * @param {Function} onReviewCard - Callback khi người dùng chọn mức đánh giá (grade, label)
 * @param {boolean} isProcessing - Trạng thái đang gửi API đánh giá (disable nút)
 */
export default function FlashcardRatingActions({ onReviewCard, isProcessing = false }) {
  return (
    <div className="flashcard-actions">
      {RATING_BUTTONS.map((btn) => (
        <button
          key={btn.grade}
          type="button"
          className={btn.btnClass}
          onClick={() => onReviewCard && onReviewCard(btn.grade, btn.label)}
          disabled={isProcessing}
        >
          <span className="material-symbols-outlined">{btn.icon}</span>
          <span>{btn.label}</span>
        </button>
      ))}
    </div>
  )
}
