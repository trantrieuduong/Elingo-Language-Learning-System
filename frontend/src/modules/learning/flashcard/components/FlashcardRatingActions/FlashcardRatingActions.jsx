import { useTranslation } from 'react-i18next';
import './FlashcardRatingActions.css';

const RATING_CONFIGS = [
  {
    grade: 0,
    key: 'again',
    icon: 'replay',
    btnClass: 'btn-secondary flashcard-btn-rating flashcard-btn-rating--again',
  },
  {
    grade: 1,
    key: 'hard',
    icon: 'psychology',
    btnClass: 'btn-secondary flashcard-btn-rating flashcard-btn-rating--hard',
  },
  {
    grade: 2,
    key: 'good',
    icon: 'sentiment_satisfied',
    btnClass: 'btn-secondary flashcard-btn-rating flashcard-btn-rating--good',
  },
  {
    grade: 3,
    key: 'easy',
    icon: 'sentiment_very_satisfied',
    btnClass: 'btn-primary flashcard-btn-rating flashcard-btn-rating--easy',
  },
];

/**
 * 4 nút đánh giá mức độ ghi nhớ SM-2 (Học lại, Khó, Dễ, Quá dễ).
 *
 * @param {Function} onReviewCard - Callback khi người dùng chọn mức đánh giá (grade, label)
 * @param {boolean} isProcessing - Trạng thái đang gửi API đánh giá (disable nút)
 */
export default function FlashcardRatingActions({ onReviewCard, isProcessing = false }) {
  const { t } = useTranslation('learning');

  return (
    <div className="flashcard-actions">
      {RATING_CONFIGS.map((btn) => {
        const label = t(`flashcard.ratings.${btn.key}`);
        return (
          <button
            key={btn.grade}
            type="button"
            className={btn.btnClass}
            onClick={() => onReviewCard && onReviewCard(btn.grade, label)}
            disabled={isProcessing}
          >
            <span className="material-symbols-outlined">{btn.icon}</span>
            <span>{label}</span>
          </button>
        );
      })}
    </div>
  );
}
