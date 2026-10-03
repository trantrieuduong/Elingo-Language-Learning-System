import { useState, useEffect } from 'react'
import { getTopicsByDeckIdApi } from '../flashcardApi'
import './FlashcardPage.css'

/**
 * @param {string} deckId - ID của deck lấy từ URL param
 * @param {Function} onNavigate - navigate function từ App.jsx
 */
function FlashcardPage({ deckId, onNavigate }) {
  const [topics, setTopics] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  
  // Trạng thái chọn topic hiện tại (dùng cho phần sau này)
  const [selectedTopicId, setSelectedTopicId] = useState(null)

  useEffect(() => {
    const fetchTopics = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await getTopicsByDeckIdApi(deckId)
        if (res.success && res.data) {
          setTopics(res.data)
        } else {
          // res.message sẽ chứa thông báo chi tiết từ backend cho các trường hợp:
          // - Deck không tồn tại
          // - DeckStatus khác PUBLISHED
          // - User chưa có premium
          setError(res.message || 'Không thể tải danh sách topic. Vui lòng thử lại sau.')
        }
      } catch (err) {
        // Trong trường hợp lỗi mạng hoặc lỗi không xác định từ axios
        const errorMsg = err.response?.data?.message || 'Đã có lỗi mạng xảy ra khi tải topics.'
        setError(errorMsg)
      } finally {
        setLoading(false)
      }
    }

    if (deckId) {
      fetchTopics()
    }
  }, [deckId])

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
        
        {/* Cột trái: Danh sách Topics */}
        <aside className="flashcard-sidebar">
          <h2 className="text-headline-sm" style={{ marginBottom: 'var(--spacing-sm)' }}>
            Topics
          </h2>
          
          {topics.length === 0 && !loading && (
            <div className="empty-state glass-card" style={{ padding: 'var(--spacing-xl)', textAlign: 'center' }}>
              <span className="material-symbols-outlined">inbox</span>
              <p>Deck này chưa có topic nào.</p>
            </div>
          )}

          {topics.map((topic) => (
            <div 
              key={topic.id || topic._id} 
              className={`glass-card flashcard-topic-card ${selectedTopicId === (topic.id || topic._id) ? 'active' : ''}`}
              onClick={() => setSelectedTopicId(topic.id || topic._id)}
            >
              <h3 className="flashcard-topic-title">{topic.name}</h3>
              {topic.description && (
                <p className="flashcard-topic-desc">{topic.description}</p>
              )}
            </div>
          ))}
        </aside>

        {/* Cột phải: Không gian Flashcard */}
        <main className="flashcard-main-content glass-well">
          <div className="flashcard-empty-placeholder">
            <span className="material-symbols-outlined flashcard-empty-placeholder-icon">
              style
            </span>
            <h2 className="text-title-md">Vùng học Flashcard</h2>
            <p className="text-body-md" style={{ marginTop: 'var(--spacing-sm)' }}>
              Chọn một topic bên trái để bắt đầu học flashcard.
            </p>
          </div>
        </main>
        
      </div>
    </div>
  )
}

export default FlashcardPage
