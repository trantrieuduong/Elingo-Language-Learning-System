import FlashcardCardContainer from './FlashcardCardContainer'
import FlashcardRatingActions from './FlashcardRatingActions'

/**
 * Khu vực tương tác thẻ flashcard gồm thẻ 3D (kèm nút star/hide) và 4 nút đánh giá SRS SM-2.
 *
 * @param {Object} card - Thẻ flashcard hiện tại
 * @param {string} cardKey - Key định danh thẻ để reset hiệu ứng lật mặt
 * @param {boolean} isProcessing - Trạng thái xử lý API (disable các nút thao tác)
 * @param {boolean} isStarred - Thẻ có đang gắn sao hay không
 * @param {Function} onToggleStar - Callback khi bấm nút sao
 * @param {Function} onToggleHide - Callback khi bấm nút ẩn thẻ
 * @param {Function} onReviewCard - Callback khi bấm 1 trong 4 nút đánh giá SRS (grade, label)
 */
export default function FlashcardStudyArea({
  card,
  cardKey,
  isProcessing = false,
  isStarred,
  onToggleStar,
  onToggleHide,
  onReviewCard
}) {
  if (!card) return null

  return (
    <div className="flashcard-study-area">
      <FlashcardCardContainer
        card={card}
        cardKey={cardKey}
        isProcessing={isProcessing}
        isStarred={isStarred}
        onToggleStar={onToggleStar}
        onToggleHide={onToggleHide}
      />

      <FlashcardRatingActions
        onReviewCard={onReviewCard}
        isProcessing={isProcessing}
      />
    </div>
  )
}