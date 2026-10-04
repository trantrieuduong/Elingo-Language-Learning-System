import { useState, useEffect } from 'react'
import { getTopicsByDeckIdApi, getUnlearnedCardsByTopicIdApi } from '../flashcardApi'
import Flashcard from '../components/FlashCard/FlashCard'
import './FlashcardStudyPage.css'

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
          ) : unlearnedCardsByTopic[selectedTopicId] && unlearnedCardsByTopic[selectedTopicId].length > 0 ? (
            <div className="flashcard-study-area">
              <Flashcard
                key={`${selectedTopicId}-${unlearnedCardsByTopic[selectedTopicId][0]?.id ?? 0}`}
                card={unlearnedCardsByTopic[selectedTopicId][0]}
              />
              
              <div className="flashcard-actions">
                 <button className="btn-secondary" onClick={() => alert('Sẽ implement API đánh giá khó')}>
                    <span className="material-symbols-outlined">psychology</span>
                    Khó
                 </button>
                 <button className="btn-primary" onClick={() => alert('Sẽ implement API đánh giá dễ')}>
                    <span className="material-symbols-outlined">check_circle</span>
                    Đã thuộc
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
