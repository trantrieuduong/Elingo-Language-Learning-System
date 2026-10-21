import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import {
  getDecksApi,
  getUserDecksApi,
  createUserDeckApi,
  updateUserDeckApi,
  deleteUserDeckApi,
  getTagsApi,
  getCefrLevelsApi,
} from '../../vocabularyApi'
import Pagination from '../../../../components/Pagination/Pagination'
import Filter from '../../../../components/Filter/Filter'
import SearchInput from '../../../../components/SearchInput/SearchInput'
import ProgressBar from '../../../../components/ProgressBar/ProgressBar'
import Modal from '../../../../components/Modal/Modal'
import UserDeckCard from '../../components/UserDeckCard/UserDeckCard'
import DeckFormModal from '../../components/DeckFormModal/DeckFormModal'
import './DeckListPage.css'

/**
 * Trang danh sách các bộ từ vựng công khai (decks)
 * @param {Function} onNavigate - Hàm điều hướng từ App.jsx
 */
function DeckListPage({ onNavigate }) {
  const { t } = useTranslation('learning')

  // Tab: 'system-decks' hoặc 'my-decks'
  const [activeTab, setActiveTab] = useState('system-decks')

  // States danh mục dùng chung
  const [tags, setTags] = useState([])
  const [cefrLevels, setCefrLevels] = useState([])

  // States filter và tìm kiếm
  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [selectedTagId, setSelectedTagId] = useState(null)
  const [selectedCefrLevelId, setSelectedCefrLevelId] = useState(null)

  // ── 1. States cho tab "Khám phá" (Explore) ──
  const [decks, setDecks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [retryCount, setRetryCount] = useState(0)

  // ── 2. States cho tab "Bộ từ của tôi" (My Decks CRUD) ──
  const [userDecks, setUserDecks] = useState([])
  const [userDecksLoading, setUserDecksLoading] = useState(true)
  const [userDecksError, setUserDecksError] = useState(null)
  const [userDeckStatusFilter, setUserDeckStatusFilter] = useState('ALL') // ALL | PUBLISHED | DRAFT
  const [userDeckPage, setUserDeckPage] = useState(1)
  const [userDeckTotalPages, setUserDeckTotalPages] = useState(1)
  const [userDeckRefreshTrigger, setUserDeckRefreshTrigger] = useState(0)

  // ── 3. States cho Modals & Toasts ──
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [formModalMode, setFormModalMode] = useState('create') // 'create' | 'edit'
  const [selectedDeckForEdit, setSelectedDeckForEdit] = useState(null)
  const [isFormSubmitting, setIsFormSubmitting] = useState(false)

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
  const [deckToDelete, setDeckToDelete] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const [toastMessage, setToastMessage] = useState('')

  // Toast tự tắt sau 3 giây (Pattern 9.5)
  useEffect(() => {
    if (!toastMessage) return
    const timer = setTimeout(() => setToastMessage(''), 3000)
    return () => clearTimeout(timer)
  }, [toastMessage])

  // Debounce tìm kiếm chung
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
      setPage(1) // Reset về trang 1 khi đổi keyword
      setUserDeckPage(1)
    }, 500)
    return () => clearTimeout(timer)
  }, [searchQuery])

  // Lọc dữ liệu khi tag/cefr thay đổi
  const handleCefrChange = (id) => {
    setSelectedCefrLevelId(id)
    setPage(1)
    setUserDeckPage(1)
  }

  const handleTagChange = (id) => {
    setSelectedTagId(id)
    setPage(1)
    setUserDeckPage(1)
  }

  useEffect(() => {
    const fetchMetadata = async () => {
      try {
        const [tagsRes, cefrRes] = await Promise.all([getTagsApi(), getCefrLevelsApi()])

        if (tagsRes.success && tagsRes.data) {
          // Format theo id, name của component Filter
          setTags(tagsRes.data.map((tag) => ({ _id: tag.code, name: tag.label })))
        }
        if (cefrRes.success && cefrRes.data) {
          setCefrLevels(cefrRes.data.map((cefr) => ({ _id: cefr.code, name: cefr.label })))
        }
      } catch (err) {
        console.error('Failed to fetch metadata:', err)
      }
    }

    fetchMetadata()
  }, [])

  // ── Fetch dữ liệu cho tab Khám phá ──
  useEffect(() => {
    if (activeTab !== 'system-decks') return

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
          setTotalPages(response.data.totalPages)
        } else {
          setError(response.message || t('deckList.loadError'))
        }
      } catch (err) {
        console.error('Failed to fetch decks:', err)
        setError(err.response?.data?.message || t('deckList.networkError'))
      } finally {
        setLoading(false)
      }
    }

    fetchDecks()
  }, [activeTab, debouncedQuery, selectedTagId, selectedCefrLevelId, page, retryCount, t])

  // ── Fetch dữ liệu cho tab Bộ từ của tôi (User Decks CRUD) ──
  useEffect(() => {
    if (activeTab !== 'my-decks') return

    const fetchUserDecks = async () => {
      setUserDecksLoading(true)
      setUserDecksError(null)
      try {
        const response = await getUserDecksApi({
          keyword: debouncedQuery || undefined,
          status: userDeckStatusFilter,
          tagCode: selectedTagId || undefined,
          cefrCode: selectedCefrLevelId || undefined,
          page: userDeckPage,
          limit: 9,
        })

        if (response.success && response.data) {
          const content = response.data.content || []
          setUserDecks(content)
          setUserDeckTotalPages(response.data.totalPages)
        } else {
          setUserDecksError(response.message || 'Không thể tải danh sách bộ từ của bạn')
        }
      } catch (err) {
        console.error('Failed to fetch user decks:', err)
        setUserDecksError('Đã có lỗi xảy ra khi tải bộ từ của bạn')
      } finally {
        setUserDecksLoading(false)
      }
    }

    fetchUserDecks()
  }, [
    activeTab,
    debouncedQuery,
    userDeckStatusFilter,
    selectedTagId,
    selectedCefrLevelId,
    userDeckPage,
    userDeckRefreshTrigger,
  ])

  // ── CRUD Actions Handlers ──

  // Mở modal tạo mới
  const handleOpenCreateModal = () => {
    setFormModalMode('create')
    setSelectedDeckForEdit(null)
    setIsFormModalOpen(true)
  }

  // Mở modal sửa
  const handleOpenEditModal = (deck) => {
    setFormModalMode('edit')
    setSelectedDeckForEdit(deck)
    setIsFormModalOpen(true)
  }

  // Mở modal xác nhận xóa
  const handleOpenDeleteModal = (deck) => {
    setDeckToDelete(deck)
    setIsDeleteModalOpen(true)
  }

  // Submit form tạo / sửa deck
  const handleFormSubmit = async (formData) => {
    setIsFormSubmitting(true)
    try {
      let res
      if (formModalMode === 'create') {
        res = await createUserDeckApi(formData)
      } else {
        res = await updateUserDeckApi(selectedDeckForEdit.id, formData)
      }

      if (res.success) {
        setIsFormModalOpen(false)
        setToastMessage(
          formModalMode === 'create'
            ? t('userDeck.createSuccess')
            : t('userDeck.updateSuccess')
        )
        // Refresh danh sách
        setUserDeckRefreshTrigger((prev) => prev + 1)
        return res
      }
      return res
    } finally {
      setIsFormSubmitting(false)
    }
  }

  // Xác nhận xóa deck
  const handleConfirmDelete = async () => {
    if (!deckToDelete) return
    setIsDeleting(true)
    try {
      const res = await deleteUserDeckApi(deckToDelete.id)
      if (res.success) {
        setIsDeleteModalOpen(false)
        setDeckToDelete(null)
        setToastMessage(t('userDeck.deleteSuccess'))
        setUserDeckRefreshTrigger((prev) => prev + 1)
      }
    } catch (err) {
      console.error('Lỗi khi xóa bộ từ:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  // Chuyển đổi trạng thái nhanh (DRAFT <-> PUBLISHED)
  const handleToggleStatus = async (deck) => {
    const newStatus = deck.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
    try {
      const res = await updateUserDeckApi(deck.id, {
        title: deck.title,
        slug: deck.slug,
        status: newStatus,
      })
      if (res.success) {
        setToastMessage(t('userDeck.statusChanged'))
        setUserDeckRefreshTrigger((prev) => prev + 1)
      }
    } catch (err) {
      console.error('Lỗi khi cập nhật trạng thái:', err)
    }
  }

  // Điều hướng vào học flashcard
  const handleStudyDeck = (deck) => {
    onNavigate(`/decks/${deck.id}`)
  }

  const handleManageTopics = (deck) => {
    onNavigate(`/decks/${deck.id}/topics`)
  }

  const handleDeckClick = (deckId) => {
    onNavigate(`/decks/${deckId}`)
  }

  const hasActiveFilters =
    searchQuery !== '' || selectedTagId !== null || selectedCefrLevelId !== null

  const handleClearFilters = () => {
    setSearchQuery('')
    setDebouncedQuery('')
    setSelectedTagId(null)
    setSelectedCefrLevelId(null)
    setPage(1)
    setUserDeckPage(1)
  }

  return (
    <main className="deck-list-page">
      <div className="container deck-list-container">
        {/* Toast thông báo thành công */}
        {toastMessage && (
          <div className="deck-toast-wrapper" role="status">
            <div className="deck-toast glass-card">
              <span className="material-symbols-outlined deck-toast__icon">check_circle</span>
              <span className="deck-toast__text">{toastMessage}</span>
            </div>
          </div>
        )}

        {/* Thanh chuyển đổi tab bộ từ */}
        <section className="deck-tabs-section">
          <div className="deck-tabs glass-well">
            <button
              type="button"
              className={`deck-tab-btn ${activeTab === 'system-decks' ? 'deck-tab-btn--active' : ''}`}
              onClick={() => setActiveTab('system-decks')}
              aria-pressed={activeTab === 'system-decks'}
            >
              <span className="material-symbols-outlined">auto_stories</span>
              {t('userDeck.tabs.systemDecks')}
            </button>
            <button
              type="button"
              className="deck-tab-btn"
              disabled
              aria-disabled="true"
            >
              <span className="material-symbols-outlined">explore</span>
              {t('userDeck.tabs.explore')}
            </button>
            <button
              type="button"
              className={`deck-tab-btn ${activeTab === 'my-decks' ? 'deck-tab-btn--active' : ''}`}
              onClick={() => setActiveTab('my-decks')}
            >
              <span className="material-symbols-outlined">folder_shared</span>
              {t('userDeck.tabs.myDecks')}
            </button>
          </div>

          {/* Nút tạo bộ từ mới khi ở tab Bộ từ của tôi */}
          {activeTab === 'my-decks' && (
            <button
              type="button"
              className="btn-primary deck-btn-create-top"
              onClick={handleOpenCreateModal}
            >
              <span className="material-symbols-outlined">add_circle</span>
              {t('userDeck.createBtn')}
            </button>
          )}
        </section>

        {/* Thanh lọc & tìm kiếm */}
        <section className="deck-list-filters glass-well">
          <div className="deck-list-filters__top">
            <SearchInput
              id="search-decks"
              className="deck-list-filters__search"
              placeholder={t('deckList.searchPlaceholder')}
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              onClear={() => setSearchQuery('')}
              clearLabel={t('deckList.clearSearch')}
            />

            {/* Filter trạng thái khi ở tab My Decks */}
            {activeTab === 'my-decks' && (
              <div className="deck-status-filters">
                <button
                  type="button"
                  className={`deck-status-pill ${
                    userDeckStatusFilter === 'ALL' ? 'deck-status-pill--active' : ''
                  }`}
                  onClick={() => {
                    setUserDeckStatusFilter('ALL')
                    setUserDeckPage(1)
                  }}
                >
                  {t('userDeck.statusAll')}
                </button>
                <button
                  type="button"
                  className={`deck-status-pill ${
                    userDeckStatusFilter === 'PUBLISHED' ? 'deck-status-pill--active' : ''
                  }`}
                  onClick={() => {
                    setUserDeckStatusFilter('PUBLISHED')
                    setUserDeckPage(1)
                  }}
                >
                  {t('userDeck.statusPublished')}
                </button>
                <button
                  type="button"
                  className={`deck-status-pill ${
                    userDeckStatusFilter === 'DRAFT' ? 'deck-status-pill--active' : ''
                  }`}
                  onClick={() => {
                    setUserDeckStatusFilter('DRAFT')
                    setUserDeckPage(1)
                  }}
                >
                  {t('userDeck.statusDraft')}
                </button>
              </div>
            )}

            {hasActiveFilters && (
              <button type="button" className="btn-clear-filter" onClick={handleClearFilters}>
                <span className="material-symbols-outlined">filter_alt_off</span>
                {t('deckList.clearFilters')}
              </button>
            )}
          </div>

          <Filter
            cefrLevels={cefrLevels}
            tags={tags}
            selectedCefrLevelId={selectedCefrLevelId}
            selectedTagId={selectedTagId}
            onCefrChange={handleCefrChange}
            onTagChange={handleTagChange}
          />
        </section>

        {/* ========================================================================= */}
        {/* TAB 1: BỘ TỪ HỆ THỐNG                                                     */}
        {/* ========================================================================= */}
        {activeTab === 'system-decks' && (
          <section className="deck-list-content">
            {loading ? (
              <div className="page-loading">
                <div className="app-loading-spinner" />
              </div>
            ) : error ? (
              <div className="empty-state glass-card">
                <span className="material-symbols-outlined empty-state__icon text-error">error</span>
                <p className="text-body-lg text-error">{error}</p>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setRetryCount((c) => c + 1)}
                >
                  {t('deckList.retry')}
                </button>
              </div>
            ) : decks.length === 0 ? (
              <div className="empty-state glass-card">
                <span className="material-symbols-outlined empty-state__icon text-disabled">
                  inbox
                </span>
                <p className="text-body-lg">{t('deckList.empty')}</p>
              </div>
            ) : (
              <>
                <div className="vocabulary-grid">
                  {decks.map((deck) => {
                    const totalCards = deck.cardCount ?? 0
                    const learnedCards = deck.learnedCardCount ?? 0

                    return (
                      <article
                        key={deck.id}
                        className={`vocabulary-card glass-card ${
                          deck.isPremium ? 'vocabulary-card--premium' : ''
                        }`}
                        onClick={() => handleDeckClick(deck.id)}
                      >
                        <span
                          className={`vocabulary-card__badge ${
                            deck.isPremium
                              ? 'vocabulary-card__badge--premium'
                              : 'vocabulary-card__badge--free'
                          }`}
                        >
                          {deck.isPremium && (
                            <span className="material-symbols-outlined">workspace_premium</span>
                          )}
                          {deck.isPremium ? t('deckList.premium') : t('deckList.free')}
                        </span>
                        <span className="vocabulary-card__word-count">
                          {t('deckList.cardStats', {
                            topicCount: deck.topicCount ?? 0,
                            cardCount: deck.cardCount ?? 0,
                          })}
                        </span>
                        {deck.coverImageUrl ? (
                          <img
                            src={deck.coverImageUrl}
                            alt={deck.title || ''}
                            className="vocabulary-card__cover-image"
                            loading="lazy"
                          />
                        ) : (
                          <div className="vocabulary-card__cover-placeholder">
                            <span className="material-symbols-outlined">layers</span>
                          </div>
                        )}
                        <div className="vocabulary-card__body">
                          <div className="vocabulary-card__info">
                            <h3 className="text-title-md">{deck.title}</h3>
                            <p className="text-body-sm text-disabled">
                              {deck.description || t('deckList.noDescription')}
                            </p>
                          </div>
                          <div className="vocabulary-card__progress">
                            <ProgressBar
                              title={t('deckList.progress')}
                              current={learnedCards}
                              total={totalCards}
                            />
                          </div>
                          <div className="vocabulary-card__footer">
                            <div className="vocabulary-card__tags">
                              {deck.cefrLevels?.map((cefr) => (
                                <span key={cefr.id} className="badge badge--primary">
                                  {cefr.label}
                                </span>
                              ))}
                              {deck.tags?.map((tag) => (
                                <span key={tag.id} className="badge badge--default">
                                  {tag.label}
                                </span>
                              ))}
                            </div>
                          </div>
                        </div>
                      </article>
                    )
                  })}
                </div>

                <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
              </>
            )}
          </section>
        )}

        {/* ========================================================================= */}
        {/* TAB 2: BỘ TỪ CỦA TÔI (USER DECKS CRUD MOCK DATA)                         */}
        {/* ========================================================================= */}
        {activeTab === 'my-decks' && (
          <section className="deck-list-content">
            {userDecksLoading ? (
              <div className="page-loading">
                <div className="app-loading-spinner" />
              </div>
            ) : userDecksError ? (
              <div className="empty-state glass-card">
                <span className="material-symbols-outlined empty-state__icon text-error">error</span>
                <p className="text-body-lg text-error">{userDecksError}</p>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setUserDeckRefreshTrigger((c) => c + 1)}
                >
                  {t('deckList.retry')}
                </button>
              </div>
            ) : userDecks.length === 0 ? (
              <div className="empty-state glass-card user-deck-empty">
                <span className="material-symbols-outlined empty-state__icon user-deck-empty__icon">
                  folder_open
                </span>
                <h3 className="text-headline-sm">{t('userDeck.emptyMyDecksTitle')}</h3>
                <p className="text-body-md text-disabled user-deck-empty__desc">
                  {t('userDeck.emptyMyDecksDesc')}
                </p>
                </div>

            ) : (
              <>
                <div className="vocabulary-grid">
                  {userDecks.map((deck) => (
                    <UserDeckCard
                      key={deck.id}
                      deck={deck}
                      onEdit={handleOpenEditModal}
                      onDelete={handleOpenDeleteModal}
                      onToggleStatus={handleToggleStatus}
                      onStudy={handleStudyDeck}
                      onManageTopics={handleManageTopics}
                    />
                  ))}
                </div>

                <Pagination
                  currentPage={userDeckPage}
                  totalPages={userDeckTotalPages}
                  onPageChange={setUserDeckPage}
                />
              </>
            )}
          </section>
        )}
      </div>

      {/* Modal Tạo / Chỉnh sửa User Deck */}
      <DeckFormModal
        isOpen={isFormModalOpen}
        mode={formModalMode}
        initialData={selectedDeckForEdit}
        onClose={() => setIsFormModalOpen(false)}
        onSubmit={handleFormSubmit}
        isLoading={isFormSubmitting}
      />

      {/* Modal Xác nhận Xóa User Deck */}
      <Modal
        isOpen={isDeleteModalOpen}
        title={t('userDeck.deleteModalTitle')}
        message={t('userDeck.deleteModalMsg', { title: deckToDelete?.title || '' })}
        confirmText={t('userDeck.deleteConfirm')}
        cancelText={t('userDeck.cancel')}
        isDanger={true}
        isLoading={isDeleting}
        onConfirm={handleConfirmDelete}
        onCancel={() => setIsDeleteModalOpen(false)}
      />
    </main>
  )
}

export default DeckListPage
