import { useState, useEffect, useCallback } from 'react'
import {
  getCardsForReviewApi,
  submitSrsReviewApi,
  toggleStarApi,
  toggleHideApi
} from '../flashcardApi'
import Flashcard from '../components/FlashCard/FlashCard'
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

  /**
   * Tải danh sách flashcard đến hạn ôn tập từ mock API
   */
  const fetchDueReviewCards = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await getCardsForReviewApi({ limit: 100 })
      if (res && res.success && Array.isArray(res.data)) {
        setReviewCards(res.data)
      } else {
        setError(res?.message || 'Không thể tải danh sách thẻ đến hạn ôn tập.')
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
    if (!currentCard || !currentCard.id) return

    setIsProcessing(true)
    try {
      const res = await submitSrsReviewApi(currentCard.id, grade)
      if (res && res.success) {
        if (grade === 0) {
          // Học lại: đưa thẻ về cuối hàng đợi ôn tập trong phiên
          setReviewCards((prev) => {
            if (prev.length <= 1) return prev
            const [first, ...rest] = prev
            return [...rest, first]
          })
        } else {
          // Hoàn thành: loại bỏ thẻ khỏi queue
          setReviewCards((prev) => prev.slice(1))
        }
      } else {
        console.error('Đánh giá thẻ không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi gửi đánh giá SRS thẻ ID ${currentCard.id} (${gradeLabel}):`, err)
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
    if (!currentCard || !currentCard.id) return

    const nextStarredState = !currentCard.flagsStarred

    setIsProcessing(true)
    try {
      const res = await toggleStarApi(currentCard.id)
      if (res && res.success) {
        setReviewCards((prev) => {
          if (prev.length === 0) return prev
          const updated = [...prev]
          updated[0] = { ...updated[0], flagsStarred: nextStarredState }
          return updated
        })
      } else {
        console.error('Đổi trạng thái gắn sao không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi đổi trạng thái gắn sao thẻ ID ${currentCard.id}:`, err)
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
    if (!currentCard || !currentCard.id) return

    setIsProcessing(true)
    try {
      const res = await toggleHideApi(currentCard.id)
      if (res && res.success) {
        setReviewCards((prev) => prev.slice(1))
      } else {
        console.error('Ẩn thẻ không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi ẩn thẻ ID ${currentCard.id}:`, err)
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
            Quay lại kho từ vựng
          </button>
        </div>
      </div>
    )
  }

  const currentCard = reviewCards[0] || null
  const isCurrentCardStarred = Boolean(currentCard?.flagsStarred)

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
            <div className="flashcard-study-area">
              {/* Thẻ Flashcard kèm 2 nút thao tác Star và Hidden trên góc thẻ */}
              <div className="flashcard-card-container">
                <div className="flashcard-card-actions">
                  <button
                    type="button"
                    className={`btn-icon flashcard-card-btn flashcard-card-btn-star ${
                      isCurrentCardStarred ? 'active-star' : ''
                    }`}
                    onClick={(e) => {
                      e.stopPropagation()
                      handleToggleStar()
                    }}
                    disabled={isProcessing}
                    title={isCurrentCardStarred ? 'Bỏ đánh dấu sao (Unstar)' : 'Đánh dấu sao (Star)'}
                    aria-label="Đánh / bỏ dấu sao"
                  >
                    <span
                      className="material-symbols-outlined"
                      style={isCurrentCardStarred ? { fontVariationSettings: "'FILL' 1" } : {}}
                    >
                      star
                    </span>
                  </button>

                  <button
                    type="button"
                    className="btn-icon flashcard-card-btn flashcard-card-btn-hidden"
                    onClick={(e) => {
                      e.stopPropagation()
                      handleToggleHide()
                    }}
                    disabled={isProcessing}
                    title="Ẩn thẻ này khỏi phiên ôn tập (Hidden)"
                    aria-label="Ẩn thẻ này"
                  >
                    <span className="material-symbols-outlined">visibility_off</span>
                  </button>
                </div>

                {/* Thẻ Flashcard 3D tái sử dụng từ FlashcardStudyPage */}
                <Flashcard key={`review-card-${currentCard.id ?? 0}`} card={currentCard} />
              </div>

              {/* 4 Nút đánh giá mức độ ghi nhớ SM-2 */}
              <div className="flashcard-actions">
                <button
                  type="button"
                  className="btn-secondary flashcard-btn-rating flashcard-btn-rating--again"
                  onClick={() => handleReviewCard(0, 'Học lại')}
                  disabled={isProcessing}
                  title="Đánh giá: Học lại (Khoảng cách ôn tiếp theo: 10 phút, SRS Grade 0)"
                >
                  <span className="material-symbols-outlined">replay</span>
                  <span>Học lại</span>
                </button>

                <button
                  type="button"
                  className="btn-secondary flashcard-btn-rating flashcard-btn-rating--hard"
                  onClick={() => handleReviewCard(1, 'Khó')}
                  disabled={isProcessing}
                  title="Đánh giá: Khó (Khoảng cách ôn tiếp theo: 1 ngày, SRS Grade 1)"
                >
                  <span className="material-symbols-outlined">psychology</span>
                  <span>Khó</span>
                </button>

                <button
                  type="button"
                  className="btn-secondary flashcard-btn-rating flashcard-btn-rating--good"
                  onClick={() => handleReviewCard(2, 'Dễ')}
                  disabled={isProcessing}
                  title="Đánh giá: Dễ (Khoảng cách ôn tiếp theo: 3 ngày, SRS Grade 2)"
                >
                  <span className="material-symbols-outlined">sentiment_satisfied</span>
                  <span>Dễ</span>
                </button>

                <button
                  type="button"
                  className="btn-primary flashcard-btn-rating flashcard-btn-rating--easy"
                  onClick={() => handleReviewCard(3, 'Quá dễ')}
                  disabled={isProcessing}
                  title="Đánh giá: Quá dễ (Khoảng cách ôn tiếp theo: 5 ngày, SRS Grade 3)"
                >
                  <span className="material-symbols-outlined">sentiment_very_satisfied</span>
                  <span>Quá dễ</span>
                </button>
              </div>
            </div>
          ) : (
            <div className="flashcard-empty-placeholder">
              <span
                className="material-symbols-outlined flashcard-empty-placeholder-icon"
                style={{ color: 'var(--color-success, #22c55e)' }}
              >
                task_alt
              </span>
              <h2 className="text-title-md">Tuyệt vời!</h2>
              <p className="text-body-md" style={{ marginTop: 'var(--spacing-sm)' }}>
                Bạn đã hoàn thành toàn bộ thẻ flashcard đến hạn ôn tập hôm nay.
              </p>
              <button
                type="button"
                className="btn-secondary"
                style={{ marginTop: 'var(--spacing-md)' }}
                onClick={fetchDueReviewCards}
              >
                <span className="material-symbols-outlined">refresh</span>
                Ôn lại từ đầu
              </button>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}

export default FlashcardReviewPage
