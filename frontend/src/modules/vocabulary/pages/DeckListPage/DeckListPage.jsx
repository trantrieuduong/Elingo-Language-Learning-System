import { useState, useEffect } from 'react'
import { getDecksApi, getTagsApi, getCefrLevelsApi } from '../../vocabularyApi'
import Pagination from '../../../../components/Pagination/Pagination'
import Filter from '../../../../components/Filter/Filter'
import Input from '../../../../components/Input/Input'
import './DeckListPage.css'

/**
 * Trang danh sách các bộ từ vựng công khai (decks)
 * @param {Function} onNavigate - Hàm điều hướng từ App.jsx
 */
function DeckListPage({ onNavigate }) {
  // States dữ liệu
  const [decks, setDecks] = useState([])
  const [tags, setTags] = useState([])
  const [cefrLevels, setCefrLevels] = useState([])
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
  // retryCount: trigger re-fetch khi bấm "Thử lại" khi mất mạng (trường hợp setPage(1) không load lại khi page hiện tại là 1)
  const [retryCount, setRetryCount] = useState(0)

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

  useEffect(() => {
    const fetchMetadata = async () => {
      try {
        const [tagsRes, cefrRes] = await Promise.all([
          getTagsApi(),
          getCefrLevelsApi()
        ])
        
        if (tagsRes.success && tagsRes.data) {
          // Format theo id, name của component Filter
          setTags(tagsRes.data.map(tag => ({ _id: tag.code, name: tag.label })))
        }
        if (cefrRes.success && cefrRes.data) {
          setCefrLevels(cefrRes.data.map(cefr => ({ _id: cefr.code, name: cefr.label })))
        }
      } catch (err) {
        console.error('Failed to fetch metadata:', err)
      }
    }
    
    fetchMetadata()
  }, [])

  // Fetch dữ liệu
  useEffect(() => {
    const fetchDecks = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await getDecksApi({
          keyword: debouncedQuery || undefined,
          tagCode: selectedTagId || undefined,
          cefrCode: selectedCefrLevelId || undefined,
          page,
        })
        
        if (response.success && response.data) {
          const fetchedDecks = response.data.content
          setDecks(Array.isArray(fetchedDecks) ? fetchedDecks : [])
          
          const fetchedTotalPages = response.data.totalPages
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
  }, [debouncedQuery, selectedTagId, selectedCefrLevelId, page, retryCount])

  const handleDeckClick = (deckId) => {
      onNavigate(`/decks/${deckId}`)
  }

  const hasActiveFilters = searchQuery !== '' || selectedTagId !== null || selectedCefrLevelId !== null

  const handleClearFilters = () => {
    setSearchQuery('')
    setDebouncedQuery('')
    setSelectedTagId(null)
    setSelectedCefrLevelId(null)
    setPage(1)
  }

  return (
    <main className="deck-list-page">
      <div className="container deck-list-container">
        {/* Filter Section */}
        <section className="deck-list-filters glass-well">
          <div className="deck-list-filters__top">
            <div className="deck-list-filters__search">
              <Input
                id="search-decks"
                placeholder="Tìm kiếm theo tên bộ từ vựng..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                rightElement={<span className="material-symbols-outlined">search</span>}
              />
            </div>
            {hasActiveFilters && (
              <button 
                type="button" 
                className="btn-clear-filter"
                onClick={handleClearFilters}
              >
                <span className="material-symbols-outlined">filter_alt_off</span>
                Xóa bộ lọc
              </button>
            )}
          </div>
          <Filter
            cefrLevels={cefrLevels}
            tags={tags}
            selectedCefrLevelId={selectedCefrLevelId}
            selectedTagId={selectedTagId}
            onCefrChange={setSelectedCefrLevelId}
            onTagChange={setSelectedTagId}
          />
        </section>

        {/* Content Section */}
        <section className="deck-list-content">
          {loading ? (
            <div className="page-loading">
              <div className="app-loading-spinner" />
            </div>
          ) : error ? (
            <div className="empty-state glass-card">
              <span className="material-symbols-outlined empty-state__icon text-error">error</span>
              <p className="text-body-lg text-error">{error}</p>{/* Lỗi danh sách nội dung */}
              <button type="button" className="btn-secondary" onClick={() => setRetryCount(c => c + 1)}>
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
                    <span className="vocabulary-card__word-count">
                      {`${deck.topicCount} chủ đề - ${deck.cardCount} từ`}
                    </span>
                    {deck.coverImageUrl ? (
                      <img 
                        src={deck.coverImageUrl} 
                        alt={deck.title}
                        className="vocabulary-card__cover-image"
                      />
                    ) : (
                      <div className="vocabulary-card__cover-placeholder">
                        <span className="material-symbols-outlined">layers</span>
                      </div>
                    )}
                    <div className="vocabulary-card__body">
                      <div className="vocabulary-card__info">
                        <h3 className="text-title-md">{deck.name || deck.title}</h3>
                        <p className="text-body-sm text-disabled">
                          {deck.description || 'Chưa có mô tả cho bộ từ vựng này.'}
                        </p>
                      </div>
                      <div className="vocabulary-card__footer">
                        <div className="vocabulary-card__tags">
                          {deck.cefrLevels?.map((cefr) => (
                            <span key={cefr.id} className="badge badge--primary">{cefr.label}</span>
                          ))}
                          {deck.tags?.map((tag) => (
                            <span key={tag.id} className="badge badge--default">{tag.label}</span>
                          ))}
                        </div>
                      </div>
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

export default DeckListPage
