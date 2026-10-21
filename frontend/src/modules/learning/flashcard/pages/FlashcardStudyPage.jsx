import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { getTopicsByDeckIdApi, getUnlearnedCardsByTopicIdApi, submitSrsReviewApi, toggleStarApi, toggleHideApi } from '../flashcardApi'
import FlashcardStudyArea from '../components/FlashcardStudyArea'
import FlashcardCompletePlaceholder from '../components/FlashcardCompletePlaceholder'
import PageError from '../../../../components/PageError/PageError'
import ProgressBar from '../../../../components/ProgressBar/ProgressBar'
import BackButton from '../../../../components/BackButton/BackButton'
import './FlashcardStudyPage.css'

/**
 * @param {string} [deckId] - ID của deck (fallback nếu có)
 * @param {Function} onNavigate - navigate function từ App.jsx
 */
function FlashcardStudyPage({ deckId, onNavigate }) {
  const { t } = useTranslation(['learning', 'api', 'common'])
  const [topics, setTopics] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [selectedTopicId, setSelectedTopicId] = useState(null)

  const [topicProgress, setTopicProgress] = useState({})
  const [unlearnedCardsByTopic, setUnlearnedCardsByTopic] = useState({})
  const [topicCardsError, setTopicCardsError] = useState({})

  // State quản lý trạng thái xử lý tương tác thẻ
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    const fetchTopics = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await getTopicsByDeckIdApi(deckId)
        if (res.success && res.data) {
          setTopics(res.data)
          
          const progressMap = {}
          res.data.forEach((t) => {
            progressMap[t.id] = {
              unlearned: t.unlearnedCardCount ?? 0,
              total: t.cardCount ?? 0
            }
          })
          setTopicProgress(progressMap)

          // Tự động chọn topic đầu tiên nếu có
          if (res.data.length > 0) {
            setSelectedTopicId(res.data[0].id)
          }
        } else {
          setError({
            message: t('flashcard.loadTopicsError'),
            actionLabel: t('flashcard.backToDecks'),
            actionPath: '/decks'
          })
        }
      } catch (err) {
        console.error('Lỗi khi tải topics:', err)
        const errorCode = err.response?.data?.code
        const errorMessage = err.response?.data?.message || t('api:common.UNKNOWN_ERROR')

        if (errorCode === 'DECK_PREMIUM_REQUIRED') {
          setError({
            message: errorMessage,
            actionLabel: t('flashcard.upgradePremium'),
            actionPath: '/premium'
          })
        } else {
          setError({
            message: errorMessage,
            actionLabel: t('flashcard.backToDecks'),
            actionPath: '/decks'
          })
        }
      } finally {
        setLoading(false)
      }
    }

    if (deckId) {
      fetchTopics()
    }
  }, [deckId, t])

  useEffect(() => {
    if (!selectedTopicId) return

    // Nếu đã load thẻ của topic này rồi thì thôi
    if (unlearnedCardsByTopic[selectedTopicId] !== undefined) return

    const fetchCards = async () => {
      setTopicCardsError(prev => ({ ...prev, [selectedTopicId]: false }))
      try {
        const res = await getUnlearnedCardsByTopicIdApi(selectedTopicId)
        if (res && res.success) {
          setUnlearnedCardsByTopic(prev => ({
            ...prev,
            [selectedTopicId]: res.data || []
          }))
        } else {
          setTopicCardsError(prev => ({ ...prev, [selectedTopicId]: true }))
        }
      } catch (err) {
        console.error('Lỗi khi tải danh sách thẻ cho topic:', err)
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
        } else {
          setTopicCardsError(prev => ({ ...prev, [selectedTopicId]: true }))
        }
      }
    }

    fetchCards()
  }, [selectedTopicId, unlearnedCardsByTopic, t])

  const removeCurrentCardAndUpdateProgress = () => {
    setUnlearnedCardsByTopic((prev) => {
      const list = prev[selectedTopicId] || []
      return {
        ...prev,
        [selectedTopicId]: list.slice(1)
      }
    })

    setTopicProgress((prev) => {
      const current = prev[selectedTopicId]
      if (!current) return prev
      return {
        ...prev,
        [selectedTopicId]: {
          ...current,
          unlearned: Math.max(0, current.unlearned - 1)
        }
      }
    })
  }

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
   * - Grade 0 (Học lại)
   * - Grade 1 (Khó)
   * - Grade 2 (Dễ)
   * - Grade 3 (Quá dễ)
   *
   * @param {number} grade - Điểm đánh giá (0, 1, 2, 3)
   * @param {string} gradeLabel - Nhãn tiếng Việt tương ứng
   */
  const handleReviewCard = async (grade, gradeLabel) => {
    if (!selectedTopicId || isProcessing) return

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || []
    if (currentCardList.length === 0) return

    const currentCard = currentCardList[0]
    if (!currentCard || !currentCard.id) return

    setIsProcessing(true)
    try {
      const res = await submitSrsReviewApi(currentCard.id, grade)
      if (res && res.success) {
        removeCurrentCardAndUpdateProgress()
      } else {
        console.error('Đánh giá thẻ không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi gửi đánh giá SRS thẻ ID ${currentCard.id} (${gradeLabel}):`, err)
      handleCardActionError(err)
    } finally {
      setIsProcessing(false)
    }
  }

  const handleToggleStar = async () => {
    if (!selectedTopicId || isProcessing) return

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || []
    if (currentCardList.length === 0) return

    const currentCard = currentCardList[0]
    if (!currentCard || !currentCard.id) return

    const isStarred = Boolean(currentCard.flagsStarred)
    const nextStarredState = !isStarred

    setIsProcessing(true)
    try {
      const res = await toggleStarApi(currentCard.id)
      if (res && res.success) {
        // Cập nhật trạng thái star trong danh sách thẻ của topic
        setUnlearnedCardsByTopic((prev) => {
          const list = prev[selectedTopicId] || []
          const updatedList = list.map((card, idx) => {
            if (idx === 0) {
              return {
                ...card,
                flagsStarred: nextStarredState
              }
            }
            return card
          })
          return {
            ...prev,
            [selectedTopicId]: updatedList
          }
        })
      } else {
        console.error('Đổi trạng thái gắn sao không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi đổi trạng thái gắn sao thẻ ID ${currentCard.id}:`, err)
      handleCardActionError(err)
    } finally {
      setIsProcessing(false)
    }
  }

  const handleToggleHide = async () => {
    if (!selectedTopicId || isProcessing) return

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || []
    if (currentCardList.length === 0) return

    const currentCard = currentCardList[0]
    if (!currentCard || !currentCard.id) return

    setIsProcessing(true)
    try {
      const res = await toggleHideApi(currentCard.id)
      if (res && res.success) {
        removeCurrentCardAndUpdateProgress()
      } else {
        console.error('Ẩn thẻ không thành công:', res)
      }
    } catch (err) {
      console.error(`Lỗi khi ẩn thẻ ID ${currentCard.id}:`, err)
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

  const currentCardList = selectedTopicId ? (unlearnedCardsByTopic[selectedTopicId] || []) : []
  const currentCard = currentCardList[0] || null
  const isCurrentCardStarred = Boolean(currentCard?.flagsStarred)

  return (
    <div className="flashcard-page-container">
      <div className="flashcard-layout">
        
        {/* Sẽ tự động chia 2 cột khớp với grid bên dưới */}
        <div className="flashcard-header-bar">
          <BackButton 
            label={t('common:back')}
            onClick={() => onNavigate?.('/decks')}
          />
        </div>

        <div>
          {/* Thanh tiến độ tổng quan */}
          {(() => {
            const progressValues = Object.values(topicProgress)
            if (progressValues.length === 0) return null
            const globalTotal = progressValues.reduce((acc, curr) => acc + curr.total, 0)
            if (globalTotal === 0) return null
            const globalUnlearned = progressValues.reduce((acc, curr) => acc + curr.unlearned, 0)
            const globalLearned = globalTotal - globalUnlearned
            return (
              <div className="glass-card flashcard-global-progress">
                <ProgressBar
                  title={t('flashcard.overallProgress')}
                  current={globalLearned}
                  total={globalTotal}
                />
              </div>
            )
          })()}
        </div>

        {/* Cột trái: Danh sách Topics */}
        <aside className="flashcard-sidebar">
          {topics.length === 0 && !loading && (
            <div className="empty-state glass-card flashcard-empty-topic">
              <span className="material-symbols-outlined">inbox</span>
              <p>{t('flashcard.noTopicsInDeck')}</p>
            </div>
          )}

          {topics.map((topic) => {
            const progress = topicProgress[topic.id]
            return (
              <div
                key={topic.id}
                className={`glass-card flashcard-topic-card ${selectedTopicId === (topic.id) ? 'active' : ''}`}
                onClick={() => setSelectedTopicId(topic.id)}
              >
                <div className="flashcard-topic-header">
                  <h3 className="flashcard-topic-title">{topic.name}</h3>
                  {progress && (
                    <span className="flashcard-progress-stats">
                      {progress.total - progress.unlearned} / {progress.total}
                    </span>
                  )}
                </div>
                {topic.description && (
                  <p className="flashcard-topic-desc">{topic.description}</p>
                )}
                {progress && progress.total > 0 && (
                  <div className="flashcard-progress-track">
                    <div 
                      className="flashcard-progress-bar"
                      style={{ width: `${Math.round(((progress.total - progress.unlearned) / progress.total) * 100)}%` }} 
                    />
                  </div>
                )}
              </div>
            )
          })}
        </aside>

        {/* Cột phải: Không gian Flashcard */}
        <main className="flashcard-main-content glass-well">
          {!selectedTopicId ? (
            <div className="flashcard-empty-placeholder">
              <span className="material-symbols-outlined flashcard-empty-placeholder-icon">
                style
              </span>
              <p className="text-body-md flashcard-placeholder-text">
                {t('flashcard.noCardsToStudy')}
              </p>
            </div>
          ) : topicCardsError[selectedTopicId] ? (
            <div className="flashcard-empty-placeholder">
              <span className="material-symbols-outlined flashcard-empty-placeholder-icon flashcard-placeholder-icon--error">
                wifi_off
              </span>
              <h2 className="text-title-md">{t('flashcard.connectionError')}</h2>
              <p className="text-body-md flashcard-placeholder-text">
                {t('flashcard.loadCardsNetworkError')}
              </p>
              <button 
                className="btn-primary flashcard-placeholder-btn"
                onClick={() => {
                  setTopicCardsError(prev => ({ ...prev, [selectedTopicId]: false }))
                  setUnlearnedCardsByTopic(prev => ({ ...prev, [selectedTopicId]: undefined }))
                }}
              >
                {t('flashcard.retry')}
              </button>
            </div>
          ) : unlearnedCardsByTopic[selectedTopicId] === undefined ? (
            <div className="flashcard-empty-placeholder">
              <div className="app-loading-spinner" />
              <p className="text-body-md">
                {t('flashcard.loadingCards')}
              </p>
            </div>
          ) : currentCard ? (
            <FlashcardStudyArea
              card={currentCard}
              cardKey={`${selectedTopicId}-${currentCard.id ?? 0}`}
              isProcessing={isProcessing}
              isStarred={isCurrentCardStarred}
              onToggleStar={handleToggleStar}
              onToggleHide={handleToggleHide}
              onReviewCard={handleReviewCard}
            />
          ) : (
            <FlashcardCompletePlaceholder
              message={t('flashcard.studyCompleted')}
            />
          )}
        </main>

      </div>
    </div>
  )
}

export default FlashcardStudyPage
