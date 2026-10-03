import { useState, useEffect } from 'react'
import { getDecksApi } from '../../vocabularyApi'
import Pagination from '../../../../components/Pagination/Pagination'
import Filter from '../../../../components/Filter/Filter'
import Input from '../../../../components/Input/Input'
import './VocabularyListPage.css'

// Giả lập dữ liệu CEFR Levels và Tags (Thực tế có thể lấy từ API metadata)
const CEFR_LEVELS = [
  { _id: '1', name: 'A1' },
  { _id: '2', name: 'A2' },
  { _id: '3', name: 'B1' },
  { _id: '4', name: 'B2' },
  { _id: '5', name: 'C1' },
  { _id: '6', name: 'C2' },
]

const TAGS = [
  { _id: '1', name: 'Giao tiếp' },
  { _id: '2', name: 'IELTS' },
  { _id: '3', name: 'TOEIC' },
  { _id: '4', name: 'Kinh doanh' },
  { _id: '5', name: 'Công nghệ IT' },
]

const LIMIT = 9

/**
 * Trang danh sách các bộ từ vựng công khai (decks)
 * @param {Function} onNavigate - Hàm điều hướng từ App.jsx
 */
function VocabularyListPage({ onNavigate }) {
  // States dữ liệu
  const [decks, setDecks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // States filter và tìm kiếm
  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [selectedTagId, setSelectedTagId] = useState(null)
  const [selectedCefrLevelId, setSelectedCefrLevelId] = useState(null)

  // States phân trang
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  // Debounce tìm kiếm
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
      setPage(1) // Reset về trang 1 khi đổi keyword
    }, 500)
    return () => clearTimeout(timer)
    // hủy bỏ timer trước đó nếu người dùng tiếp tục nhập ký tự mới khi chưa hết 500ms
  }, [searchQuery])

  // Lọc dữ liệu khi tag/cefr thay đổi
  useEffect(() => {
    setPage(1)
  }, [selectedTagId, selectedCefrLevelId])

  // Fetch dữ liệu
  useEffect(() => {
    const fetchDecks = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await getDecksApi({
          q: debouncedQuery || undefined,
          tagId: selectedTagId || undefined,
          cefrLevelId: selectedCefrLevelId || undefined,
          page,
          limit: LIMIT,
        })
        
        if (response.success && response.data) {
          // Xử lý list decks (tùy cấu trúc trả về, hỗ trợ cả Spring Data Pageable structure)
          const fetchedDecks = response.data.content || response.data.items || response.data
          setDecks(Array.isArray(fetchedDecks) ? fetchedDecks : [])
          
          const fetchedTotalPages = response.data.totalPages || 1
          setTotalPages(fetchedTotalPages)
        } else {
          setError(response.message || 'Không thể tải danh sách bộ từ vựng.')
        }
      } catch (err) {
        setError('Đã có lỗi mạng hoặc hệ thống. Vui lòng thử lại.')
      } finally {
        setLoading(false)
      }
    }
    
    fetchDecks()
  }, [debouncedQuery, selectedTagId, selectedCefrLevelId, page])

  const handleDeckClick = (deckId) => {
    if (onNavigate) {
      onNavigate(`/vocabulary/${deckId}`)
    }
  }

  return (
    <main className="vocabulary-list-page">
      <div className="container vocabulary-list-container">
        {/* Header Section */}
        <header className="vocabulary-list-header glass-card">
          <div className="vocabulary-list-header__title">
            <span className="material-symbols-outlined icon-display">library_books</span>
            <h1 className="text-headline-lg">Thư viện từ vựng</h1>
          </div>
          <p className="text-body-lg text-disabled">
            Khám phá các bộ từ vựng được chọn lọc giúp bạn mở rộng vốn từ nhanh chóng.
          </p>
        </header>

        {/* Filter Section */}
        <section className="vocabulary-list-filters glass-well">
          <div className="vocabulary-list-filters__search">
            <Input
              id="search-decks"
              placeholder="Tìm kiếm theo tên bộ từ vựng..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              rightElement={<span className="material-symbols-outlined">search</span>}
            />
          </div>
          <Filter
            cefrLevels={CEFR_LEVELS}
            tags={TAGS}
            selectedCefrLevelId={selectedCefrLevelId}
            selectedTagId={selectedTagId}
            onCefrChange={setSelectedCefrLevelId}
            onTagChange={setSelectedTagId}
          />
        </section>

        {/* Content Section */}
        <section className="vocabulary-list-content">
          {loading ? (
            <div className="page-loading">
              <div className="app-loading-spinner" />
            </div>
          ) : error ? (
            <div className="empty-state glass-card">
              <span className="material-symbols-outlined empty-state__icon text-error">error</span>
              <p className="text-body-lg text-error">{error}</p>
              <button type="button" className="btn-secondary" onClick={() => setPage(1)}>
                Thử lại
              </button>
            </div>
          ) : decks.length === 0 ? (
            <div className="empty-state glass-card">
              <span className="material-symbols-outlined empty-state__icon text-disabled">inbox</span>
              <p className="text-body-lg">Không tìm thấy bộ từ vựng nào phù hợp.</p>
            </div>
          ) : (
            <>
              <div className="vocabulary-grid">
                {decks.map((deck) => (
                  <article
                    key={deck.id || deck._id}
                    className="vocabulary-card glass-card"
                    onClick={() => handleDeckClick(deck.id || deck._id)}
                  >
                    <div className="vocabulary-card__icon glass-thumb">
                      <span className="material-symbols-outlined">style</span>
                    </div>
                    <div className="vocabulary-card__info">
                      <h3 className="text-title-md">{deck.name || deck.title}</h3>
                      <p className="text-body-sm text-disabled">
                        {deck.description || 'Chưa có mô tả cho bộ từ vựng này.'}
                      </p>
                    </div>
                    <div className="vocabulary-card__footer">
                      {deck.cefrLevel && (
                        <span className="badge--primary">{deck.cefrLevel}</span>
                      )}
                      <span className="text-body-sm text-disabled">
                        {deck.totalWords || 0} từ vựng
                      </span>
                    </div>
                  </article>
                ))}
              </div>

              {totalPages > 1 && (
                <div className="vocabulary-pagination-wrapper">
                  <Pagination
                    currentPage={page}
                    totalPages={totalPages}
                    onPageChange={setPage}
                  />
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </main>
  )
}

export default VocabularyListPage
