import { useTranslation } from 'react-i18next'
import Flashcard from './FlashCard/FlashCard'

/**
 * Vùng hiển thị thẻ Flashcard 3D kèm 2 nút thao tác Star và Hidden ở góc thẻ.
 * @param {Object} card - Dữ liệu thẻ flashcard hiện tại
 * @param {string} cardKey - Key định danh để reset trạng thái lật của thẻ
 * @param {boolean} isProcessing - Trạng thái đang gửi API
 * @param {boolean} isStarred - Thẻ có được gắn sao không
 * @param {Function} onToggleStar - Hàm xử lý bật/tắt gắn sao
 * @param {Function} onToggleHide - Hàm xử lý ẩn thẻ
 */
export default function FlashcardCardContainer({
  card,
  cardKey,
  isProcessing = false,
  isStarred,
  onToggleStar,
  onToggleHide
}) {
  const { t } = useTranslation('learning')

  if (!card) return null

  return (
    <div className="flashcard-card-container">
      <div className="flashcard-card-actions">
        <button
          type="button"
          className={`btn-icon flashcard-card-btn flashcard-card-btn-star ${
            isStarred ? 'active-star' : ''
          }`}
          onClick={(e) => {
            e.stopPropagation()
            e.preventDefault()
            if (onToggleStar) onToggleStar()
          }}
          onPointerDown={(e) => e.stopPropagation()}
          disabled={isProcessing}
          title={isStarred ? t('flashcard.actions.unstar') : t('flashcard.actions.star')}
          aria-label={isStarred ? t('flashcard.actions.unstar') : t('flashcard.actions.star')}
        >
          <span
            className="material-symbols-outlined"
            style={isStarred ? { fontVariationSettings: "'FILL' 1" } : {}}
          >
            star
          </span>
        </button>

        <button
          type="button"
          className="btn-icon flashcard-card-btn flashcard-card-btn-hidden"
          onClick={(e) => {
            e.stopPropagation()
            e.preventDefault()
            if (onToggleHide) onToggleHide()
          }}
          onPointerDown={(e) => e.stopPropagation()}
          disabled={isProcessing}
          title={t('flashcard.actions.hide')}
          aria-label={t('flashcard.actions.hide')}
        >
          <span className="material-symbols-outlined">visibility_off</span>
        </button>
      </div>

      {/* Thẻ Flashcard 3D */}
      <Flashcard
        key={cardKey !== undefined ? cardKey : (card.id ?? 0)}
        card={card}
      />
    </div>
  )
}
