import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import {
  getCardsForReviewApi,
  submitSrsReviewApi,
  toggleStarApi,
  toggleHideApi
} from '../flashcardApi'
import FlashcardStudyArea from '../components/FlashcardStudyArea'
import FlashcardCompletePlaceholder from '../components/FlashcardCompletePlaceholder'
import PageError from '../../../../components/PageError/PageError'
import BackButton from '../../../../components/BackButton/BackButton'
import './FlashcardReviewPage.css'

/**
 * FlashcardReviewPage — Trang ôn tập flashcard đến hạn (SM-2) tại route /review.
 * Giao diện tối giản: chỉ gồm nút quay lại, thẻ Flashcard 3D và 4 nút đánh giá.
 *
 * @param {Function} onNavigate - Hàm điều hướng custom router từ App.jsx
 */
function FlashcardReviewPage({ onNavigate }) {
  const { t } = useTranslation(['learning', 'api', 'common'])
  const [reviewCards, setReviewCards] = useState([])

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    const fetchDueReviewCards = async () => {
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
          setError({
            message: t('flashcard.loadReviewError'),
            actionLabel: t('flashcard.backToDecks'),
            actionPath: '/decks'
          })
        }
      } catch (err) {
        console.error('Lỗi khi tải flashcard đến hạn ôn tập:', err)
        const errorMessage = err.response?.data?.message || t('api:common.UNKNOWN_ERROR')

        setError({
          message: errorMessage,
          actionLabel: t('flashcard.backToDecks'),
          actionPath: '/decks'
        })
      } finally {
        setLoading(false)
      }
    }

    fetchDueReviewCards()
  }, [t])

  /**
   * Xử lý lỗi chung khi tương tác với flashcard (Review, Star, Hide).
   */
  const handleCardActionError = (err) => {
    const errorCode = err.response?.data?.code
    const errorMessage = err.response?.data?.message || t('api:common.UNKNOWN_ERROR')

    if (errorCode === 'DECK_PREMIUM_REQUIRED') {
      setError({
        message: errorMessage,
        actionLabel: t('flashcard.upgradePremium'),
        actionPath: '/premium'
      })
    } else if (errorCode === 'DECK_NOT_FOUND') {
      setError({
        message: errorMessage,
        actionLabel: t('flashcard.backToDecks'),
        actionPath: '/decks'
      })
    }
  }

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
      handleCardActionError(err)
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
      handleCardActionError(err)
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
      handleCardActionError(err)
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
      <PageError 
        error={error.message} 
        actionLabel={error.actionLabel}
        onAction={() => {
          if (onNavigate) onNavigate(error.actionPath)
        }} 
      />
    )
  }

  const currentCard = reviewCards[0] || null
  const isCurrentCardStarred = Boolean(currentCard?.flagsStarred)
  const currentCardId = currentCard?.id

  return (
    <div className="flashcard-page-container">
      <div className="flashcard-layout">
        <div className="flashcard-header-bar">
          <BackButton label={t('common:back')} onClick={() => onNavigate?.('/decks')} />
        </div>
        
        {/*Thanh tiến độ rỗng tựa bên StudyPage */}
        <div></div>

        {/* Danh sách Topics rỗng */}
        <aside className="flashcard-sidebar"></aside>

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
              message={t('flashcard.reviewCompleted')}
            />
          )}
        </main>
      </div>
    </div>
  )
}

export default FlashcardReviewPage
