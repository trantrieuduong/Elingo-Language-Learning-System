import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  getDecksApi,
  getUserDecksApi,
  createUserDeckApi,
  updateUserDeckApi,
  publishUserDeckApi,
  deleteUserDeckApi,
  getTagsApi,
  getCefrLevelsApi,
} from '../../vocabularyApi';
import Pagination from '../../../../components/Pagination/Pagination';
import FilterSection from '../../../../components/FilterSection/FilterSection';
import ProgressBar from '../../../../components/ProgressBar/ProgressBar';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import Toast from '../../../../components/Toast/Toast';
import ConfirmModal from '../../../../components/ConfirmModal/ConfirmModal';
import UserDeckCard from '../../components/UserDeckCard/UserDeckCard';
import DeckStatsPill from '../../components/DeckStatsPill/DeckStatsPill';
import DeckFormModal from '../../components/DeckFormModal/DeckFormModal';
import './DeckListPage.css';

const DECK_TABS = new Set(['system-decks', 'community-decks', 'my-decks']);

const getInitialTab = () => {
  const tab = new URLSearchParams(window.location.search).get('tab');
  return DECK_TABS.has(tab) ? tab : 'system-decks';
};

function DeckListPage({ onNavigate }) {
  const { t } = useTranslation(['learning', 'common']);
  const [activeTab, setActiveTab] = useState(getInitialTab);

  // States danh mục dùng chung
  const [tags, setTags] = useState([]);
  const [cefrLevels, setCefrLevels] = useState([]);

  // States filter và tìm kiếm
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [selectedTagIds, setSelectedTagIds] = useState([]);
  const [selectedCefrLevelIds, setSelectedCefrLevelIds] = useState([]);

  // States cho tab "Bộ từ cộng đồng"
  const [decks, setDecks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [retryCount, setRetryCount] = useState(0);

  // States cho tab "Bộ từ của tôi"
  const [userDecks, setUserDecks] = useState([]);
  const [userDecksLoading, setUserDecksLoading] = useState(true);
  const [userDecksError, setUserDecksError] = useState(null);
  const [userDeckStatusFilter, setUserDeckStatusFilter] = useState('ALL'); // ALL | PUBLISHED | DRAFT
  const [userDeckPage, setUserDeckPage] = useState(1);
  const [userDeckTotalPages, setUserDeckTotalPages] = useState(1);
  const [userDeckRefreshTrigger, setUserDeckRefreshTrigger] = useState(0);

  // States cho Modals & Toasts ──
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [formModalMode, setFormModalMode] = useState('create'); // 'create' | 'edit'
  const [selectedDeckForEdit, setSelectedDeckForEdit] = useState(null);
  const [isFormSubmitting, setIsFormSubmitting] = useState(false);

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deckToDelete, setDeckToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState('success');
  const handleToastDismiss = useCallback(() => setToastMessage(''), []);
  const showToast = (message, variant = 'success') => {
    setToastVariant(variant);
    setToastMessage(message);
  };

  // Debounce tìm kiếm chung
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery);
      setPage(1); // Reset về trang 1 khi đổi keyword
      setUserDeckPage(1);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  const handleCefrChange = (ids) => {
    setSelectedCefrLevelIds(ids);
    setPage(1);
    setUserDeckPage(1);
  };

  const handleTagChange = (ids) => {
    setSelectedTagIds(ids);
    setPage(1);
    setUserDeckPage(1);
  };

  useEffect(() => {
    const fetchMetadata = async () => {
      try {
        const [tagsRes, cefrRes] = await Promise.all([getTagsApi(), getCefrLevelsApi()]);

        if (tagsRes.success && tagsRes.data) {
          // Format theo id, name của component Filter
          setTags(tagsRes.data.map((tag) => ({ _id: tag.id, name: tag.label })));
        }
        if (cefrRes.success && cefrRes.data) {
          setCefrLevels(cefrRes.data.map((cefr) => ({ _id: cefr.id, name: cefr.label })));
        }
      } catch (err) {
        console.error('Failed to fetch metadata:', err);
      }
    };

    fetchMetadata();
  }, []);

  useEffect(() => {
    if (activeTab !== 'system-decks' && activeTab !== 'community-decks') return;

    let isActive = true;

    const fetchDecks = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await getDecksApi({
          keyword: debouncedQuery || undefined,
          tagIds: activeTab === 'system-decks' ? selectedTagIds : [],
          cefrLevelIds: activeTab === 'system-decks' ? selectedCefrLevelIds : [],
          page,
          limit: 9,
          ownerType: activeTab === 'community-decks' ? 'USER' : 'SYSTEM',
        });

        if (!isActive) return;

        if (response.success && response.data) {
          const fetchedDecks = response.data.content;
          setDecks(Array.isArray(fetchedDecks) ? fetchedDecks : []);
          setTotalPages(response.data.totalPages);
        } else {
          setError(t('common:loadError'));
        }
      } catch (err) {
        if (!isActive) return;
        console.error('Failed to fetch decks:', err);
        setError(t('common:loadError'));
      } finally {
        if (isActive) setLoading(false);
      }
    };

    fetchDecks();

    return () => {
      isActive = false;
    };
  }, [activeTab, debouncedQuery, selectedTagIds, selectedCefrLevelIds, page, retryCount, t]);

  useEffect(() => {
    if (activeTab !== 'my-decks') return;

    let isActive = true;

    const fetchUserDecks = async () => {
      setUserDecksLoading(true);
      setUserDecksError(null);
      try {
        const response = await getUserDecksApi({
          keyword: debouncedQuery || undefined,
          status: userDeckStatusFilter,
          page: userDeckPage,
          limit: 9,
        });

        if (!isActive) return;

        if (response.success && response.data) {
          const content = response.data.content || [];
          setUserDecks(content);
          setUserDeckTotalPages(response.data.totalPages);
        } else {
          setUserDecksError(t('common:loadError'));
        }
      } catch (err) {
        if (!isActive) return;
        console.error('Failed to fetch user decks:', err);
        setUserDecksError(t('common:loadError'));
      } finally {
        if (isActive) setUserDecksLoading(false);
      }
    };

    fetchUserDecks();

    return () => {
      isActive = false;
    };
  }, [activeTab, debouncedQuery, userDeckStatusFilter, userDeckPage, userDeckRefreshTrigger, t]);

  const handleOpenCreateModal = () => {
    setFormModalMode('create');
    setSelectedDeckForEdit(null);
    setIsFormModalOpen(true);
  };

  const handleOpenEditModal = (deck) => {
    setFormModalMode('edit');
    setSelectedDeckForEdit(deck);
    setIsFormModalOpen(true);
  };

  const handleOpenDeleteModal = (deck) => {
    setDeckToDelete(deck);
    setIsDeleteModalOpen(true);
  };

  const handleCurrentUserNotFound = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    window.dispatchEvent(new Event('auth:logout'));
    if (onNavigate) onNavigate('/login');
  };

  const handleFormSubmit = async (formData) => {
    setIsFormSubmitting(true);
    try {
      let res;
      if (formModalMode === 'create') {
        try {
          res = await createUserDeckApi(formData);
        } catch (error) {
          if (error.response?.data?.code === 'USER_NOT_FOUND') {
            handleCurrentUserNotFound();
          }
          throw error;
        }
      } else {
        res = await updateUserDeckApi(selectedDeckForEdit.id, formData);
      }

      if (res.success) {
        setIsFormModalOpen(false);
        showToast(
          formModalMode === 'create' ? t('userDeck.createSuccess') : t('userDeck.updateSuccess')
        );
        setUserDeckRefreshTrigger((prev) => prev + 1);
        return res;
      }
      return res;
    } finally {
      setIsFormSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deckToDelete) return;
    setIsDeleting(true);
    try {
      const res = await deleteUserDeckApi(deckToDelete.id);
      if (res.success) {
        setIsDeleteModalOpen(false);
        setDeckToDelete(null);
        showToast(t('userDeck.deleteSuccess'));
        setUserDeckRefreshTrigger((prev) => prev + 1);
      } else {
        showToast(res?.message || t('userDeck.deleteError'), 'error');
      }
    } catch (err) {
      const errorMessage =
        err.response?.data?.message || err.message || t('userDeck.deleteError');
      showToast(errorMessage, 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  // Chuyển đổi trạng thái DRAFT <-> PUBLISHED
  const handleToggleStatus = async (deck) => {
    const isCurrentlyPublished = deck.status === 'PUBLISHED';
    try {
      const res = await publishUserDeckApi(deck.id, !isCurrentlyPublished);
      if (res?.success) {
        showToast(t('userDeck.statusChanged'));
        setUserDeckRefreshTrigger((prev) => prev + 1);
      } else {
        showToast(res?.message || t('userDeck.statusChangeError'), 'error');
      }
    } catch (err) {
      console.error('Lỗi khi cập nhật trạng thái:', err);
      const errorMsg =
        err.response?.data?.message || err.message || t('userDeck.statusChangeError');
      showToast(errorMsg, 'error');
    }
  };

  const handleManageTopics = (deck) => {
    onNavigate(`/decks/${deck.id}/topics?tab=my-decks`);
  };

  const handleDeckClick = (deck) => {
    onNavigate(`/decks/${deck.slug}`, { returnTo: `/decks?tab=${activeTab}` });
  };

  const hasActiveFilters =
    searchQuery !== '' ||
    (activeTab === 'system-decks' &&
      (selectedTagIds.length > 0 || selectedCefrLevelIds.length > 0)) ||
    (activeTab === 'my-decks' && userDeckStatusFilter !== 'ALL');

  const handleClearFilters = () => {
    setSearchQuery('');
    setDebouncedQuery('');
    setSelectedTagIds([]);
    setSelectedCefrLevelIds([]);
    setUserDeckStatusFilter('ALL');
    setPage(1);
    setUserDeckPage(1);
  };

  return (
    <main className="deck-list-page">
      <div className="container deck-list-container">
        <Toast message={toastMessage} variant={toastVariant} onDismiss={handleToastDismiss} />

        {/* Thanh chuyển đổi tab bộ từ */}
        <section className="deck-tabs-section">
          <div className="deck-tabs glass-well">
            <button
              type="button"
              className={`deck-tab-btn ${activeTab === 'system-decks' ? 'deck-tab-btn--active' : ''}`}
              onClick={() => setActiveTab('system-decks')}
            >
              <span className="material-symbols-outlined">auto_stories</span>
              {t('userDeck.tabs.systemDecks')}
            </button>
            <button
              type="button"
              className={`deck-tab-btn ${activeTab === 'community-decks' ? 'deck-tab-btn--active' : ''}`}
              onClick={() => setActiveTab('community-decks')}
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
        <FilterSection
          searchQuery={searchQuery}
          onSearchChange={(event) => setSearchQuery(event.target.value)}
          searchPlaceholder={t('deckList.searchPlaceholder')}
          cefrLevels={cefrLevels}
          tags={tags}
          selectedCefrLevelIds={selectedCefrLevelIds}
          selectedTagIds={selectedTagIds}
          onCefrChange={handleCefrChange}
          onTagChange={handleTagChange}
          hasActiveFilters={hasActiveFilters}
          onClearFilters={handleClearFilters}
          showCategoryFilters={activeTab === 'system-decks'}
          extraControls={
            activeTab === 'my-decks' && (
              <div className="deck-status-filters">
                <button
                  type="button"
                  className={`deck-status-pill ${
                    userDeckStatusFilter === 'ALL' ? 'deck-status-pill--active' : ''
                  }`}
                  onClick={() => {
                    setUserDeckStatusFilter('ALL');
                    setUserDeckPage(1);
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
                    setUserDeckStatusFilter('PUBLISHED');
                    setUserDeckPage(1);
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
                    setUserDeckStatusFilter('DRAFT');
                    setUserDeckPage(1);
                  }}
                >
                  {t('userDeck.statusDraft')}
                </button>
              </div>
            )
          }
        />

        {(activeTab === 'system-decks' || activeTab === 'community-decks') && (
          <section className="deck-list-content">
            {loading ? (
              <PageLoading />
            ) : error ? (
              <EmptyState
                icon="error"
                isError
                description={error}
                actionLabel={t('common:retry')}
                onAction={() => setRetryCount((c) => c + 1)}
              />
            ) : decks.length === 0 ? (
              <EmptyState
                icon="layers"
                title={t('deckList.emptyTitle')}
                description={
                  hasActiveFilters ? t('deckList.emptyFilter') : t('deckList.emptyDefault')
                }
                actionLabel={hasActiveFilters ? t('common:filter.clearFilters') : undefined}
                onAction={hasActiveFilters ? handleClearFilters : undefined}
                actionVariant="primary"
              />
            ) : (
              <>
                <div className="vocabulary-grid">
                  {decks.map((deck) => {
                    const totalCards = deck.cardCount ?? 0;
                    const learnedCards = deck.learnedCardCount ?? 0;
                    const deckCefrLevels = deck.cefrLevels || [];
                    const deckTags = deck.tags || [];

                    return (
                      <article
                        key={deck.id}
                        className={`vocabulary-card glass-card ${
                          deck.isPremium ? 'vocabulary-card--premium' : ''
                        }`}
                        onClick={() => handleDeckClick(deck)}
                      >
                        {/* Huy hiệu Premium ở góc trên bên trái ảnh bìa */}
                        {deck.isPremium && (
                          <span className="vocabulary-card__premium-badge">
                            <span className="material-symbols-outlined vocabulary-card__premium-icon">
                              workspace_premium
                            </span>
                            {t('deckList.premium')}
                          </span>
                        )}

                        <DeckStatsPill
                          className="vocabulary-card__word-count"
                          topicCount={deck.topicCount ?? 0}
                          cardCount={deck.cardCount ?? 0}
                        />
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
                          {/* Tags & CEFR đặt phía trên tiêu đề tương tự LessonCard */}
                          {(deckCefrLevels.length > 0 || deckTags.length > 0) && (
                            <div className="vocabulary-card__tags">
                              {deckCefrLevels.map((cefr) => (
                                <span
                                  key={cefr.id}
                                  className="badge badge--soft-blue vocabulary-card__cefr-tag"
                                >
                                  {cefr.label}
                                </span>
                              ))}
                              {deckTags.map((tag) => (
                                <span
                                  key={tag.id}
                                  className="badge badge--default vocabulary-card__topic-tag"
                                >
                                  {tag.label}
                                </span>
                              ))}
                            </div>
                          )}

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
                        </div>
                      </article>
                    );
                  })}
                </div>

                <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
              </>
            )}
          </section>
        )}

        {activeTab === 'my-decks' && (
          <section className="deck-list-content">
            {userDecksLoading ? (
              <PageLoading />
            ) : userDecksError ? (
              <EmptyState
                icon="error"
                isError
                description={userDecksError}
                actionLabel={t('common:retry')}
                onAction={() => setUserDeckRefreshTrigger((c) => c + 1)}
              />
            ) : userDecks.length === 0 ? (
              <EmptyState
                icon="layers"
                className="deck-list-empty-state"
                title={
                  hasActiveFilters
                    ? t('userDeck.emptyFilteredTitle')
                    : t('userDeck.emptyMyDecksTitle')
                }
                description={
                  hasActiveFilters
                    ? t('userDeck.emptyFilteredDesc')
                    : t('userDeck.emptyMyDecksDesc')
                }
                actionLabel={
                  hasActiveFilters ? t('common:filter.clearFilters') : t('userDeck.createBtn')
                }
                onAction={hasActiveFilters ? handleClearFilters : handleOpenCreateModal}
                actionVariant="primary"
              />
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
      <ConfirmModal
        isOpen={isDeleteModalOpen}
        title={t('userDeck.deleteModalTitle')}
        message={t('userDeck.deleteModalMsg', { title: deckToDelete?.title || '' })}
        confirmText={t('userDeck.deleteConfirm')}
        cancelText={t('common:cancel')}
        isDanger={true}
        isLoading={isDeleting}
        onConfirm={handleConfirmDelete}
        onCancel={() => setIsDeleteModalOpen(false)}
      />
    </main>
  );
}

export default DeckListPage;
