import { useState, useEffect, useCallback } from 'react'
import {
  getCardsForReviewApi,
  submitSrsReviewApi,
  toggleStarApi,
  toggleHideApi
} from '../flashcardApi'
import FlashcardStudyArea from '../components/FlashcardStudyArea'
import FlashcardCompletePlaceholder from '../components/FlashcardCompletePlaceholder'
import './FlashcardReviewPage.css'

/**
 * FlashcardReviewPage — Trang ôn tập flashcard đến hạn (SM-2) tại route /flashcard/review.
 * Giao diện tối giản: chỉ gồm nút quay lại, thẻ Flashcard 3D và 4 nút đánh giá.
 *
 * @param {Function} onNavigate - Hàm điều hướng custom router từ App.jsx
 */
function FlashcardReviewPage({ onNavigate }) {
  const [reviewCards, setReviewCards] = useState([])

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isProcessing, setIsProcessing] = useState(false)

  const fetchDueReviewCards = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getCardsForReviewApi(100)
      if (res && res.success) {
        const formattedCards = res.data
          .filter((item) => item?.card)
          .map((item) => ({
            ...item.card,
            flagsStarred: item.flagsStarred
          }))
        setReviewCards(formattedCards)
      } else {
        setError('Không thể tải danh sách thẻ đến hạn ôn tập.')
      }
    } catch (err) {
      console.error('Lỗi khi tải flashcard đến hạn ôn tập:', err)
      setError('Đã có lỗi mạng xảy ra khi tải dữ liệu ôn tập.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchDueReviewCards()
  }, [fetchDueReviewCards])

  /**
   * Đánh giá mức độ ghi nhớ SM-2 (0: Học lại, 1: Khó, 2: Dễ, 3: Quá dễ)
   */
  const handleReviewCard = async (grade, gradeLabel) => {
    if (reviewCards.length === 0 || isProcessing) return

    const currentCard = reviewCards[0]
    const cardId = currentCard?.id
    if (!cardId) return

    setIsProcessing(true)
    try {
      const res = await submitSrsReviewApi(cardId, grade)
      if (res && res.success) {
        setReviewCards((prev) => prev.slice(1))
      } else {
        console.error('Đánh giá thẻ không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi gửi đánh giá SRS thẻ ID ${cardId} (${gradeLabel}):`, err)
    } finally {
      setIsProcessing(false)
    }
  }

  /**
   * Đổi trạng thái gắn sao cho thẻ hiện tại
   */
  const handleToggleStar = async () => {
    if (reviewCards.length === 0 || isProcessing) return

    const currentCard = reviewCards[0]
    const cardId = currentCard?.id
    if (!cardId) return

    const nextStarredState = !(currentCard.flagsStarred)

    setIsProcessing(true)
    try {
      const res = await toggleStarApi(cardId)
      if (res && res.success) {
        setReviewCards((prev) => {
          if (prev.length === 0) return prev
          const updated = [...prev]
          updated[0] = {
            ...updated[0],
            flagsStarred: nextStarredState,
          }
          return updated
        })
      } else {
        console.error('Đổi trạng thái gắn sao không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi đổi trạng thái gắn sao thẻ ID ${cardId}:`, err)
    } finally {
      setIsProcessing(false)
    }
  }

  /**
   * Ẩn thẻ khỏi phiên ôn tập
   */
  const handleToggleHide = async () => {
    if (reviewCards.length === 0 || isProcessing) return

    const currentCard = reviewCards[0]
    const cardId = currentCard?.id
    if (!cardId) return

    setIsProcessing(true)
    try {
      const res = await toggleHideApi(cardId)
      if (res && res.success) {
        setReviewCards((prev) => prev.slice(1))
      } else {
        console.error('Ẩn thẻ không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi ẩn thẻ ID ${cardId}:`, err)
    } finally {
      setIsProcessing(false)
    }
  }

  if (loading) {
    return (
      <div className="page-loading">
        <div className="app-loading-spinner" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="page-loading">
        <div
          className="auth-alert auth-alert--error"
          style={{ maxWidth: '600px', width: '100%', margin: '0 auto', textAlign: 'center' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: '48px', marginBottom: '16px' }}>
            error
          </span>
          <p>{error}</p>
          <button
            className="btn-primary"
            style={{ marginTop: '24px' }}
            onClick={() => {
              if (onNavigate) onNavigate('/vocabulary')
            }}
          >
            Quay lại trang chủ
          </button>
        </div>
      </div>
    )
  }

  const currentCard = reviewCards[0] || null
  const isCurrentCardStarred = Boolean(currentCard?.flagsStarred)
  const currentCardId = currentCard?.id

  return (
    <div className="flashcard-review-page-container">
      <div className="flashcard-review-layout">
        {/* HEADER: Nút quay lại */}
        <div className="flashcard-review-header-bar">
          <button
            className="btn-secondary"
            onClick={() => {
              if (onNavigate) onNavigate('/vocabulary')
            }}
          >
            <span className="material-symbols-outlined">arrow_back</span>
            Quay lại
          </button>
        </div>

        {/* KHÔNG GIAN THẺ FLASHCARD & 4 NÚT ĐÁNH GIÁ SM-2 */}
        <main className="flashcard-main-content glass-well">
          {currentCard ? (
            <FlashcardStudyArea
              card={currentCard}
              cardKey={`review-card-${currentCardId}`}
              isProcessing={isProcessing}
              isStarred={isCurrentCardStarred}
              onToggleStar={handleToggleStar}
              onToggleHide={handleToggleHide}
              onReviewCard={handleReviewCard}
            />
          ) : (
            <FlashcardCompletePlaceholder
              message="Bạn đã hoàn thành toàn bộ thẻ flashcard đến hạn ôn tập hôm nay."
            />
          )}
        </main>
      </div>
    </div>
  )
}

export default FlashcardReviewPage
