import { useState, useEffect } from 'react'
import { getTopicsByDeckIdApi, getUnlearnedCardsByTopicIdApi } from '../flashcardApi'
import Flashcard from '../components/FlashCard/FlashCard'
import './FlashcardStudyPage.css'

/**
 * Cấu hình khoảng cách học tập SRS (SM-2) theo từng mức grade:
 * - Grade 0 (Học lại): Khoảng cách ôn tiếp theo < 10 phút. //
 * - Grade 1 (Khó): Khoảng cách ôn tiếp theo 1 ngày.
 * - Grade 2 (Dễ): Khoảng cách ôn tiếp theo 3 ngày.
 * - Grade 3 (Quá dễ): Khoảng cách ôn tiếp theo 5 ngày.
 */
const SRS_INTERVAL_MAP = {
  0: { label: 'Học lại', interval: '< 10 phút' },
  1: { label: 'Khó', interval: '1 ngày' },
  2: { label: 'Dễ', interval: '3 ngày' },
  3: { label: 'Quá dễ', interval: '5 ngày' }
}

/**
 * @param {string} deckId - ID của deck lấy từ URL param
 * @param {Function} onNavigate - navigate function từ App.jsx
 */
function FlashcardStudyPage({ deckId, onNavigate }) {
  const [topics, setTopics] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [selectedTopicId, setSelectedTopicId] = useState(null)

  const [topicProgress, setTopicProgress] = useState({})
  const [unlearnedCardsByTopic, setUnlearnedCardsByTopic] = useState({})

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
          // Tự động chọn topic đầu tiên nếu có
          if (res.data.length > 0) {
            setSelectedTopicId(res.data[0].id)
          }
        } else {
          setError('Không thể tải danh sách topic. Vui lòng thử lại sau.')
        }
      } catch (err) {
        console.error('Lỗi khi tải topics:', err)
        const errorMsg = 'Đã có lỗi mạng xảy ra khi tải topics.'
        setError(errorMsg)
      } finally {
        setLoading(false)
      }
    }

    if (deckId) {
      fetchTopics()
    }
  }, [deckId])

  useEffect(() => {
    if (!topics.length) {
      return
    }

    const fetchProgress = async () => {
      try {
        const unlearnedResponses = await Promise.all(
          topics.map((t) => getUnlearnedCardsByTopicIdApi(t.id))
        )

        const progressMap = {}

        const cardsMap = {}

        topics.forEach((t, index) => {
          const ulRes = unlearnedResponses[index]
          const unlearnedCards = (ulRes && ulRes.success) ? ulRes.data : []
          const unlearned = unlearnedCards.length
          const total = t.cardCount ?? 0

          progressMap[t.id] = { unlearned, total }
          cardsMap[t.id] = unlearnedCards
        })

        setTopicProgress(progressMap)
        setUnlearnedCardsByTopic(cardsMap)
      } catch (err) {
        console.error('Lỗi khi tải tiến độ:', err)
      }
    }

    fetchProgress()
  }, [topics])

  /**
   * Mock hàm xử lý đánh giá mức độ ghi nhớ flashcard theo 4 cấp độ SRS (SM-2):
   * Tất cả các cấp độ đều mock ghi nhận khoảng cách học tập, hoàn thành lượt học của thẻ hiện tại
   *  và chuyển sang thẻ tiếp theo.
   * - Grade 0 (Học lại): Khoảng cách ôn tiếp theo < 10 phút. //
   * - Grade 1 (Khó): Khoảng cách ôn tiếp theo 1 ngày.
   * - Grade 2 (Dễ): Khoảng cách ôn tiếp theo 3 ngày.
   * - Grade 3 (Quá dễ): Khoảng cách ôn tiếp theo 5 ngày.
   *
   * @param {number} grade - Điểm đánh giá (0, 1, 2, 3)
   * @param {string} gradeLabel - Nhãn tiếng Việt tương ứng
   */
  const handleReviewCard = (grade, gradeLabel) => {
    if (!selectedTopicId || isProcessing) return

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || []
    if (currentCardList.length === 0) return

    const currentCard = currentCardList[0]
    if (!currentCard || !currentCard.id) return

    const srsConfig = SRS_INTERVAL_MAP[grade] || {
      label: gradeLabel,
      interval: '1 ngày'
    }

    setIsProcessing(true)
    try {
      console.log(
        `[MOCK SRS] Đánh giá thẻ ID ${currentCard.id} ("${currentCard.term || ''}"): ` +
        `${gradeLabel} (Grade: ${grade}, Khoảng cách ôn tiếp theo: ${srsConfig.interval})`
      )

      // Cả 4 cấp độ (kể cả Grade 0 - Học lại) đều ghi nhận hoàn thành lượt học thẻ này và chuyển sang thẻ tiếp theo
      setUnlearnedCardsByTopic((prev) => {
        const list = prev[selectedTopicId] || []
        return {
          ...prev,
          [selectedTopicId]: list.slice(1) // Cắt từ index 1 (phần tử thứ hai) trở đi
        }
      })

      // Cập nhật tiến độ của topic (giảm số thẻ chưa học)
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
    } catch (err) {
      console.error('Lỗi khi đánh giá thẻ:', err)
    } finally {
      setIsProcessing(false)
    }
  }

  /**
   * Mock hàm xử lý đánh dấu sao / bỏ đánh dấu sao thẻ flashcard hiện tại (Star).
   */
  const handleToggleStar = () => {
    if (!selectedTopicId || isProcessing) return

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || []
    if (currentCardList.length === 0) return

    const currentCard = currentCardList[0]
    if (!currentCard || !currentCard.id) return

    const isStarred = Boolean(currentCard.isStarred ?? currentCard.flagsStarred)//
    const nextStarredState = !isStarred

    setIsProcessing(true)
    try {
      console.log(`[MOCK STAR] Thẻ ID ${currentCard.id} ("${currentCard.term || ''}") -> Đổi trạng thái star: ${nextStarredState}`)

      // Cập nhật trạng thái star trong danh sách thẻ của topic
      setUnlearnedCardsByTopic((prev) => {
        const list = prev[selectedTopicId] || []
        const updatedList = list.map((card, idx) => {
          if (idx === 0) {
            return {
              ...card,
              isStarred: nextStarredState,//
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

    } catch (err) {
      console.error('Lỗi khi gắn sao thẻ:', err)
    } finally {
      setIsProcessing(false)
    }
  }

  /**
   * Mock hàm xử lý ẩn thẻ flashcard hiện tại khỏi danh sách học (Hidden).
   */
  const handleToggleHide = () => {
    if (!selectedTopicId || isProcessing) return

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || []
    if (currentCardList.length === 0) return

    const currentCard = currentCardList[0]
    if (!currentCard || !currentCard.id) return

    setIsProcessing(true)
    try {
      console.log(`[MOCK HIDDEN] Ẩn thẻ ID ${currentCard.id} ("${currentCard.term || ''}") khỏi danh sách học`)

      // Loại bỏ thẻ bị ẩn khỏi danh sách học
      setUnlearnedCardsByTopic((prev) => {
        const list = prev[selectedTopicId] || []
        return {
          ...prev,
          [selectedTopicId]: list.slice(1)
        }
      })

      // Giảm unlearned & total trong tiến độ của topic
      setTopicProgress((prev) => {
        const current = prev[selectedTopicId]
        if (!current) return prev
        return {
          ...prev,
          [selectedTopicId]: {
            ...current,
            unlearned: Math.max(0, current.unlearned - 1),
            total: Math.max(0, current.total - 1)
          }
        }
      })

    } catch (err) {
      console.error('Lỗi khi ẩn thẻ:', err)
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
        <div className="auth-alert auth-alert--error" style={{ maxWidth: '600px', width: '100%', margin: '0 auto', textAlign: 'center' }}>
          <span className="material-symbols-outlined" style={{ fontSize: '48px', marginBottom: '16px' }}>error</span>
          <p>{error}</p>
          <button
            className="btn-primary"
            style={{ marginTop: '24px' }}
            onClick={() => {
              if (onNavigate) onNavigate('/')
            }}
          >
            Quay lại trang chủ
          </button>
        </div>
      </div>
    )
  }

  const currentCardList = selectedTopicId ? (unlearnedCardsByTopic[selectedTopicId] || []) : []
  const currentCard = currentCardList[0] || null
  const isCurrentCardStarred = Boolean(currentCard?.isStarred ?? currentCard?.flagsStarred)

  return (
    <div className="flashcard-page-container">
      <div className="flashcard-layout">
        
        {/* DÒNG 1 (HEADER): Sẽ tự động chia 2 cột khớp với grid bên dưới */}
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <button 
            className="btn-secondary" 
            onClick={() => { if (onNavigate) onNavigate('/vocabulary') }}
            style={{ fontWeight: 600 }}
          >
            <span className="material-symbols-outlined">arrow_back</span>
            Quay lại
          </button>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', padding: '0 var(--spacing-sm)' }}>
          {/* Thanh tiến độ tổng quan */}
          {(() => {
            const progressValues = Object.values(topicProgress)
            if (progressValues.length === 0) return null
            const globalTotal = progressValues.reduce((acc, curr) => acc + curr.total, 0)
            if (globalTotal === 0) return null
            const globalUnlearned = progressValues.reduce((acc, curr) => acc + curr.unlearned, 0)
            const globalLearned = globalTotal - globalUnlearned
            const globalProgressPercent = Math.round((globalLearned / globalTotal) * 100)

            return (
              <div className="glass-card" style={{ padding: 'var(--spacing-sm) var(--spacing-lg)', borderRadius: 'var(--radius-full)', width: '100%' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                  <span className="text-title-sm" style={{ margin: 0, color: 'var(--color-text-high)' }}>Tiến độ toàn bộ</span>
                  <span style={{ fontSize: '0.85rem', fontWeight: 'bold', color: 'var(--color-primary)' }}>
                    {globalLearned} / {globalTotal}
                  </span>
                </div>
                <div 
                  style={{ 
                    width: '100%', 
                    height: '6px', 
                    backgroundColor: 'var(--color-surface-variant)', 
                    borderRadius: 'var(--radius-full)', 
                    overflow: 'hidden'
                  }}
                >
                  <div 
                    style={{ 
                      width: `${globalProgressPercent}%`, 
                      height: '100%', 
                      backgroundColor: 'var(--color-primary)', 
                      transition: 'width 0.4s ease' 
                    }} 
                  />
                </div>
              </div>
            )
          })()}
        </div>

        {/* Cột trái: Danh sách Topics */}
        <aside className="flashcard-sidebar">
          {topics.length === 0 && !loading && (
            <div className="empty-state glass-card" style={{ padding: 'var(--spacing-xl)', textAlign: 'center' }}>
              <span className="material-symbols-outlined">inbox</span>
              <p>Deck này chưa có topic nào.</p>
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
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <h3 className="flashcard-topic-title">{topic.name}</h3>
                  {progress && (
                    <span style={{ fontSize: '0.85rem', fontWeight: 'bold', color: 'var(--color-primary)' }}>
                      {progress.total - progress.unlearned} / {progress.total}
                    </span>
                  )}
                </div>
                {topic.description && (
                  <p className="flashcard-topic-desc">{topic.description}</p>
                )}
                {progress && progress.total > 0 && (
                  <div 
                    style={{ 
                      width: '100%', 
                      height: '6px', 
                      backgroundColor: 'var(--color-surface-variant)', 
                      borderRadius: 'var(--radius-full)', 
                      marginTop: 'var(--spacing-xs)',
                      overflow: 'hidden'
                    }}
                  >
                    <div 
                      style={{ 
                        width: `${Math.round(((progress.total - progress.unlearned) / progress.total) * 100)}%`, 
                        height: '100%', 
                        backgroundColor: 'var(--color-primary)', 
                        transition: 'width 0.4s ease' 
                      }} 
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
              <h2 className="text-title-md">Vùng học Flashcard</h2>
              <p className="text-body-md" style={{ marginTop: 'var(--spacing-sm)' }}>
                Chọn một topic bên trái để bắt đầu học flashcard.
              </p>
            </div>
          ) : currentCard ? (
            <div className="flashcard-study-area">


              {/* Vùng hiển thị thẻ Flashcard kèm 2 nút thao tác Star và Hidden hiển thị trực tiếp trên góc thẻ */}
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
                    title={
                      isCurrentCardStarred
                        ? 'Bỏ đánh dấu sao (Star)'
                        : 'Đánh dấu sao (Star)'
                    }
                    aria-label="Đánh dấu sao"
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
                    title="Ẩn thẻ này khỏi phiên học (Hidden)"
                    aria-label="Ẩn thẻ này"
                  >
                    <span className="material-symbols-outlined">visibility_off</span>
                  </button>
                </div>

                {/* Thẻ Flashcard 3D */}
                <Flashcard
                  key={`${selectedTopicId}-${currentCard.id ?? 0}`}
                  card={currentCard}
                />
              </div>

              {/* 4 Nút đánh giá mức độ ghi nhớ SM-2 */}
              <div className="flashcard-actions">
                <button
                  type="button"
                  className="btn-secondary flashcard-btn-rating flashcard-btn-rating--again"
                  onClick={() => handleReviewCard(0, 'Học lại')}
                  disabled={isProcessing}
                  title="Đánh giá: Học lại (Khoảng cách ôn tiếp theo: < 10 phút, SRS Grade 0)"//
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
              <span className="material-symbols-outlined flashcard-empty-placeholder-icon" style={{ color: 'var(--color-success, #22c55e)' }}>
                task_alt
              </span>
              <h2 className="text-title-md">Tuyệt vời!</h2>
              <p className="text-body-md" style={{ marginTop: 'var(--spacing-sm)' }}>
                Bạn đã học xong toàn bộ thẻ trong chủ đề này.
              </p>
            </div>
          )}
        </main>

      </div>
    </div>
  )
}

export default FlashcardStudyPage
