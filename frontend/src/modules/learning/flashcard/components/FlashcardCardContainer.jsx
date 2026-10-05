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
  if (!card) return null

  const starred = isStarred !== undefined ? Boolean(isStarred) : Boolean(card.flagsStarred)//

  return (
    <div className="flashcard-card-container">
      <div className="flashcard-card-actions">
        <button
          type="button"
          className={`btn-icon flashcard-card-btn flashcard-card-btn-star ${
            starred ? 'active-star' : ''
          }`}
          onClick={(e) => {
            e.stopPropagation()
            if (onToggleStar) onToggleStar()
          }}
          disabled={isProcessing}
          title={starred ? 'Bỏ đánh dấu sao (Unstar)' : 'Đánh dấu sao (Star)'}
          aria-label="Đánh / bỏ dấu sao"
        >
          <span
            className="material-symbols-outlined"
            style={starred ? { fontVariationSettings: "'FILL' 1" } : {}}
          >
            star
          </span>
        </button>

        <button
          type="button"
          className="btn-icon flashcard-card-btn flashcard-card-btn-hidden"
          onClick={(e) => {
            e.stopPropagation()
            if (onToggleHide) onToggleHide()
          }}
          disabled={isProcessing}
          title="Ẩn thẻ này (Hidden)"
          aria-label="Ẩn thẻ này"
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
