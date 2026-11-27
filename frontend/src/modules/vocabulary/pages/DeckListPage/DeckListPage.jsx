import { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../../context/AuthContext';
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
import Tabs from '../../../../components/Tabs/Tabs';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import Toast from '../../../../components/Toast/Toast';
import ConfirmModal from '../../../../components/ConfirmModal/ConfirmModal';
import OptionsMenu from '../../../../components/OptionsMenu/OptionsMenu';
import OptionsModal from '../../../../components/OptionsModal/OptionsModal';
import UserDeckCard from '../../components/UserDeckCard/UserDeckCard';
import DeckStatsPill from '../../components/DeckStatsPill/DeckStatsPill';
import DeckFormModal from '../../components/DeckFormModal/DeckFormModal';
import ReportModal from '../../../report/components/ReportModal/ReportModal';
import { useReportModal } from '../../../report/hooks/useReportModal';
import './DeckListPage.css';

const DECK_TABS = new Set(['system-decks', 'community-decks', 'my-decks']);

const getInitialTab = () => {
  const tab = new URLSearchParams(window.location.search).get('tab');
  return DECK_TABS.has(tab) ? tab : 'system-decks';
};

function canReportDeck(deck, user) {
  if (!user) return false;

  // Kiểm tra ownerId nếu là deck của chính người dùng
  const ownerId = deck.ownerId;
  if (ownerId && String(ownerId) === String(user.id)) return false;
  return true;
}

function computeHasActiveFilters({
  searchQuery,
  activeTab,
  selectedTagIds,
  selectedCefrLevelIds,
  userDeckStatusFilter,
}) {
  const hasSystemFilters =
    activeTab === 'system-decks' &&
    (selectedTagIds.length > 0 || selectedCefrLevelIds.length > 0);
  const hasMyDeckFilters = activeTab === 'my-decks' && userDeckStatusFilter !== 'ALL';
  return searchQuery !== '' || hasSystemFilters || hasMyDeckFilters;
}

function buildDeckTabs(t, user) {
  const baseTabs = [
    {
      key: 'system-decks',
      label: t('userDeck.tabs.systemDecks'),
      icon: 'auto_stories',
    },
    {
      key: 'community-decks',
      label: t('userDeck.tabs.explore'),
      icon: 'explore',
    },
  ];
  if (user) {
    baseTabs.push({
      key: 'my-decks',
      label: t('userDeck.tabs.myDecks'),
      icon: 'folder_shared',
    });
  }
  return baseTabs;
}

async function loadPublicDecks({
  activeTab,
  debouncedQuery,
  selectedTagIds,
  selectedCefrLevelIds,
  page,
}) {
  const isSystemTab = activeTab === 'system-decks';
  return getDecksApi({
    keyword: debouncedQuery || undefined,
    tagIds: isSystemTab ? selectedTagIds : [],
    cefrLevelIds: isSystemTab ? selectedCefrLevelIds : [],
    page,
    limit: 9,
    ownerType: activeTab === 'community-decks' ? 'USER' : 'SYSTEM',
  });
}

async function executeFetchPublicDecks({ params, isCancelled, onSuccess, onError, onFinally }) {
  try {
    const response = await loadPublicDecks(params);
    if (isCancelled()) return;

    if (response.success && response.data) {
      onSuccess(response.data.content, response.data.totalPages);
    } else {
      onError();
    }
  } catch (err) {
    if (isCancelled()) return;
    console.error('Failed to fetch decks:', err);
    onError();
  } finally {
    if (!isCancelled()) onFinally();
  }
}

async function executeFetchUserDecks({ params, isCancelled, onSuccess, onError, onFinally }) {
  try {
    const response = await getUserDecksApi({
      keyword: params.debouncedQuery || undefined,
      status: params.userDeckStatusFilter,
      page: params.userDeckPage,
      limit: 9,
    });
    if (isCancelled()) return;

    if (response.success && response.data) {
      onSuccess(response.data.content, response.data.totalPages);
    } else {
      onError();
    }
  } catch (err) {
    if (isCancelled()) return;
    console.error('Failed to fetch user decks:', err);
    onError();
  } finally {
    if (!isCancelled()) onFinally();
  }
}

async function submitDeck({ formModalMode, formData, selectedDeckForEdit }) {
  if (formModalMode !== 'create') {
    return updateUserDeckApi(selectedDeckForEdit.id, formData);
  }
  return createUserDeckApi(formData);
}

async function deleteDeck({ deckId, t, onSuccess, showToast }) {
  try {
    const res = await deleteUserDeckApi(deckId);
    if (res.success) {
      onSuccess();
      showToast(t('userDeck.deleteSuccess'));
    } else {
      showToast(res?.message || t('userDeck.deleteError'), 'error');
    }
  } catch (err) {
    const errorMessage = err.response?.data?.message || err.message || t('userDeck.deleteError');
    showToast(errorMessage, 'error');
  }
}

async function toggleDeckStatus({ deck, t, onSuccess, showToast }) {
  const isCurrentlyPublished = deck.status === 'PUBLISHED';
  try {
    const res = await publishUserDeckApi(deck.id, !isCurrentlyPublished);
    if (res?.success) {
      showToast(t('userDeck.statusChanged'));
      onSuccess();
    } else {
      showToast(res?.message || t('userDeck.statusChangeError'), 'error');
    }
  } catch (err) {
    console.error('Lỗi khi cập nhật trạng thái:', err);
    const errorMsg =
      err.response?.data?.message || err.message || t('userDeck.statusChangeError');
    showToast(errorMsg, 'error');
  }
}

function buildMyDeckOptions({ deck, t, onToggleStatus, onEdit, onDelete }) {
  return [
    {
      id: 'toggle-status',
      label: t(deck.status === 'PUBLISHED' ? 'userDeck.switchToDraft' : 'userDeck.publishDeck'),
      icon: deck.status === 'PUBLISHED' ? 'unpublished' : 'publish',
      onSelect: () => onToggleStatus(deck),
    },
    {
      id: 'edit-deck',
      label: t('userDeck.editDeck'),
      icon: 'edit',
      onSelect: () => onEdit(deck),
    },
    {
      id: 'delete-deck',
      label: t('userDeck.deleteDeck'),
      icon: 'delete',
      variant: 'danger',
      onSelect: () => onDelete(deck),
    },
  ];
}

function buildPublicDeckOptions({ deck, t, onReport }) {
  return [
    {
      id: 'report-deck',
      label: t('report:report'),
      icon: 'flag',
      onSelect: () =>
        onReport({
          targetType: 'DECK',
          targetId: deck.id,
          targetLabel: deck.title,
        }),
    },
  ];
}

function resolveDeckOptions({
  targetDeck,
  activeTab,
  user,
  t,
  onToggleStatus,
  onEdit,
  onDelete,
  onReport,
}) {
  if (!targetDeck) return [];
  if (activeTab === 'my-decks') {
    return buildMyDeckOptions({
      deck: targetDeck,
      t,
      onToggleStatus,
      onEdit,
      onDelete,
    });
  }
  if (canReportDeck(targetDeck, user)) {
    return buildPublicDeckOptions({
      deck: targetDeck,
      t,
      onReport,
    });
  }
  return [];
}

function useDeckMetadata() {
  const [tags, setTags] = useState([]);
  const [cefrLevels, setCefrLevels] = useState([]);

  useEffect(() => {
    const fetchMetadata = async () => {
      try {
        const [tagsRes, cefrRes] = await Promise.all([getTagsApi(), getCefrLevelsApi()]);

        if (tagsRes.success && tagsRes.data) {
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

  return { tags, cefrLevels };
}

function usePublicDecks({
  activeTab,
  debouncedQuery,
  selectedTagIds,
  selectedCefrLevelIds,
  page,
  retryCount,
  translationRef,
}) {
  const [decks, setDecks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    if (activeTab !== 'system-decks' && activeTab !== 'community-decks') return;

    let isActive = true;
    setLoading(true);
    setError(null);

    executeFetchPublicDecks({
      params: { activeTab, debouncedQuery, selectedTagIds, selectedCefrLevelIds, page },
      isCancelled: () => !isActive,
      onSuccess: (fetchedDecks, pages) => {
        setDecks(fetchedDecks);
        setTotalPages(pages);
      },
      onError: () => setError(translationRef.current('common:loadError')),
      onFinally: () => setLoading(false),
    });

    return () => {
      isActive = false;
    };
  }, [
    activeTab,
    debouncedQuery,
    selectedTagIds,
    selectedCefrLevelIds,
    page,
    retryCount,
    translationRef,
  ]);

  return { decks, loading, error, totalPages };
}

function useUserDecks({
  activeTab,
  debouncedQuery,
  userDeckStatusFilter,
  userDeckPage,
  userDeckRefreshTrigger,
  translationRef,
}) {
  const [decks, setDecks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    if (activeTab !== 'my-decks') return;

    let isActive = true;
    setLoading(true);
    setError(null);

    executeFetchUserDecks({
      params: { debouncedQuery, userDeckStatusFilter, userDeckPage },
      isCancelled: () => !isActive,
      onSuccess: (fetchedDecks, pages) => {
        setDecks(fetchedDecks);
        setTotalPages(pages);
      },
      onError: () => setError(translationRef.current('common:loadError')),
      onFinally: () => setLoading(false),
    });

    return () => {
      isActive = false;
    };
  }, [
    activeTab,
    debouncedQuery,
    userDeckStatusFilter,
    userDeckPage,
    userDeckRefreshTrigger,
    translationRef,
  ]);

  return {
    userDecks: decks,
    userDecksLoading: loading,
    userDecksError: error,
    userDeckTotalPages: totalPages,
  };
}

function DeckCoverImage({ deck, activeTab }) {
  if (activeTab !== 'system-decks') return null;
  if (deck.coverImageUrl) {
    return (
      <img
        src={deck.coverImageUrl}
        alt={deck.title}
        className="vocabulary-card__cover-image"
        loading="lazy"
      />
    );
  }
  return (
    <div className="vocabulary-card__cover-placeholder">
      <span className="material-symbols-outlined">layers</span>
    </div>
  );
}

function DeckCardTags({ cefrLevels, tags }) {
  if (cefrLevels.length === 0 && tags.length === 0) return null;
  return (
    <div className="vocabulary-card__tags">
      {cefrLevels.map((cefr) => (
        <span key={cefr.id} className="badge badge--soft-blue vocabulary-card__cefr-tag">
          {cefr.label}
        </span>
      ))}
      {tags.map((tag) => (
        <span key={tag.id} className="badge badge--default vocabulary-card__topic-tag">
          {tag.label}
        </span>
      ))}
    </div>
  );
}

function SystemDeckCard({ deck, activeTab, canReport, onOptionsClick, onDeckClick, t }) {
  const totalCards = deck.cardCount ?? 0;
  const learnedCards = deck.learnedCardCount ?? 0;
  const deckCefrLevels = deck.cefrLevels || [];
  const deckTags = deck.tags || [];

  return (
    <article
      className={`vocabulary-card glass-card ${
        deck.isPremium ? 'vocabulary-card--premium' : ''
      } ${activeTab === 'community-decks' ? 'vocabulary-card--without-cover' : ''} ${
        canReport ? 'vocabulary-card--with-options' : ''
      }`}
      onClick={() => onDeckClick(deck)}
    >
      
      {deck.isPremium && (
        <span className="vocabulary-card__premium-badge">
          <span className="material-symbols-outlined vocabulary-card__premium-icon">
            workspace_premium
          </span>
          {t('deckList.premium')}
        </span>
      )}

      {canReport && (
        <OptionsMenu
          className="vocabulary-card__options-btn"
          onClick={(e) => {
            e.stopPropagation();
            onOptionsClick(deck);
          }}
        />
      )}

      <DeckStatsPill
        className="vocabulary-card__word-count"
        topicCount={deck.topicCount}
        cardCount={deck.cardCount}
      />
      <DeckCoverImage deck={deck} activeTab={activeTab} />
      <div className="vocabulary-card__body">
        <DeckCardTags cefrLevels={deckCefrLevels} tags={deckTags} />
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
}

function StatusFilterPills({ statusFilter, onStatusChange, t }) {
  const statuses = [
    { key: 'ALL', label: t('userDeck.statusAll') },
    { key: 'PUBLISHED', label: t('userDeck.statusPublished') },
    { key: 'DRAFT', label: t('userDeck.statusDraft') },
  ];
  return (
    <div className="deck-status-filters">
      {statuses.map(({ key, label }) => (
        <button
          key={key}
          type="button"
          className={`deck-status-pill ${statusFilter === key ? 'deck-status-pill--active' : ''}`}
          onClick={() => onStatusChange(key)}
        >
          {label}
        </button>
      ))}
    </div>
  );
}

function PublicDecksSection({
  loading,
  error,
  decks,
  activeTab,
  user,
  hasActiveFilters,
  page,
  totalPages,
  onRetry,
  onClearFilters,
  onOptionsClick,
  onDeckClick,
  onPageChange,
  t,
}) {
  if (loading) return <PageLoading />;
  if (error) {
    return (
      <EmptyState
        icon="error"
        isError
        description={error}
        actionLabel={t('common:retry')}
        onAction={onRetry}
      />
    );
  }
  if (decks.length === 0) {
    return (
      <EmptyState
        icon="layers"
        title={t('deckList.emptyTitle')}
        description={hasActiveFilters ? t('deckList.emptyFilter') : t('deckList.emptyDefault')}
        actionLabel={hasActiveFilters ? t('common:filter.clearFilters') : undefined}
        onAction={hasActiveFilters ? onClearFilters : undefined}
        actionVariant="primary"
      />
    );
  }

  return (
    <>
      <div className="vocabulary-grid">
        {decks.map((deck) => (
          <SystemDeckCard
            key={deck.id}
            deck={deck}
            activeTab={activeTab}
            canReport={canReportDeck(deck, user)}
            onOptionsClick={onOptionsClick}
            onDeckClick={onDeckClick}
            t={t}
          />
        ))}
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={onPageChange} />
    </>
  );
}

function MyDecksSection({
  loading,
  error,
  userDecks,
  hasActiveFilters,
  page,
  totalPages,
  onRetry,
  onClearFilters,
  onCreateClick,
  onOptionsClick,
  onManageTopics,
  onPageChange,
  t,
}) {
  if (loading) return <PageLoading />;
  if (error) {
    return (
      <EmptyState
        icon="error"
        isError
        description={error}
        actionLabel={t('common:retry')}
        onAction={onRetry}
      />
    );
  }
  if (userDecks.length === 0) {
    return (
      <EmptyState
        icon="layers"
        className="deck-list-empty-state"
        title={
          hasActiveFilters ? t('userDeck.emptyFilteredTitle') : t('userDeck.emptyMyDecksTitle')
        }
        description={
          hasActiveFilters ? t('userDeck.emptyFilteredDesc') : t('userDeck.emptyMyDecksDesc')
        }
        actionLabel={hasActiveFilters ? t('common:filter.clearFilters') : t('userDeck.createBtn')}
        onAction={hasActiveFilters ? onClearFilters : onCreateClick}
        actionVariant="primary"
      />
    );
  }

  return (
    <>
      <div className="vocabulary-grid">
        {userDecks.map((deck) => (
          <UserDeckCard
            key={deck.id}
            deck={deck}
            onOptionsClick={onOptionsClick}
            onManageTopics={onManageTopics}
          />
        ))}
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={onPageChange} />
    </>
  );
}

function DeckListPage({ onNavigate }) {
  const { t } = useTranslation(['learning', 'common', 'report']);
  const translationRef = useRef(t);
  useEffect(() => {
    translationRef.current = t;
  }, [t]);

  const { user } = useAuth();

  // State report modal
  const { isReportOpen, targetType, targetId, targetLabel, openReport, closeReport } =
    useReportModal();
  const [targetDeckForOptions, setTargetDeckForOptions] = useState(null);
  const [selectedTab, setSelectedTab] = useState(getInitialTab);
  const activeTab = selectedTab === 'my-decks' && !user ? 'system-decks' : selectedTab;

  // States danh mục dùng chung
  const { tags, cefrLevels } = useDeckMetadata();

  // States filter và tìm kiếm
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [selectedTagIds, setSelectedTagIds] = useState([]);
  const [selectedCefrLevelIds, setSelectedCefrLevelIds] = useState([]);

  // States cho tab "Bộ từ hệ thống" và "Bộ từ cộng đồng"
  const [page, setPage] = useState(1);
  const [retryCount, setRetryCount] = useState(0);
  const { decks, loading, error, totalPages } = usePublicDecks({
    activeTab,
    debouncedQuery,
    selectedTagIds,
    selectedCefrLevelIds,
    page,
    retryCount,
    translationRef,
  });

  // States cho tab "Bộ từ của tôi"
  const [userDeckStatusFilter, setUserDeckStatusFilter] = useState('ALL'); // ALL | PUBLISHED | DRAFT
  const [userDeckPage, setUserDeckPage] = useState(1);
  const [userDeckRefreshTrigger, setUserDeckRefreshTrigger] = useState(0);
  const { userDecks, userDecksLoading, userDecksError, userDeckTotalPages } = useUserDecks({
    activeTab,
    debouncedQuery,
    userDeckStatusFilter,
    userDeckPage,
    userDeckRefreshTrigger,
    translationRef,
  });

  // States cho Modals và Toasts
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

  const handleFormSubmit = async (formData) => {
    setIsFormSubmitting(true);
    try {
      const res = await submitDeck({
        formModalMode,
        formData,
        selectedDeckForEdit,
      });

      if (res.success) {
        setIsFormModalOpen(false);
        const toastKey =
          formModalMode === 'create' ? 'userDeck.createSuccess' : 'userDeck.updateSuccess';
        showToast(t(toastKey));
        setUserDeckRefreshTrigger((prev) => prev + 1);
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
      await deleteDeck({
        deckId: deckToDelete.id,
        t,
        showToast,
        onSuccess: () => {
          setIsDeleteModalOpen(false);
          setDeckToDelete(null);
          setUserDeckRefreshTrigger((prev) => prev + 1);
        },
      });
    } finally {
      setIsDeleting(false);
    }
  };

  // Chuyển đổi trạng thái DRAFT <-> PUBLISHED
  const handleToggleStatus = (deck) =>
    toggleDeckStatus({
      deck,
      t,
      showToast,
      onSuccess: () => setUserDeckRefreshTrigger((prev) => prev + 1),
    });

  const handleManageTopics = (deck) => {
    onNavigate(`/decks/${deck.id}/topics?tab=my-decks`);
  };

  const handleDeckClick = (deck) => {
    onNavigate(`/decks/${deck.slug}`, { returnTo: `/decks?tab=${activeTab}` });
  };

  const hasActiveFilters = computeHasActiveFilters({
    searchQuery,
    activeTab,
    selectedTagIds,
    selectedCefrLevelIds,
    userDeckStatusFilter,
  });

  const handleClearFilters = () => {
    setSearchQuery('');
    setDebouncedQuery('');
    setSelectedTagIds([]);
    setSelectedCefrLevelIds([]);
    setUserDeckStatusFilter('ALL');
    setPage(1);
    setUserDeckPage(1);
  };

  const handleStatusFilterChange = (status) => {
    setUserDeckStatusFilter(status);
    setUserDeckPage(1);
  };

  const handleTabChange = (tabKey) => {
    setSelectedTab(tabKey);
    if (tabKey !== 'my-decks') {
      setPage(1);
    }
  };

  const deckOptions = resolveDeckOptions({
    targetDeck: targetDeckForOptions,
    activeTab,
    user,
    t,
    onToggleStatus: handleToggleStatus,
    onEdit: handleOpenEditModal,
    onDelete: handleOpenDeleteModal,
    onReport: openReport,
  });

  const isPublicTab = activeTab === 'system-decks' || activeTab === 'community-decks';

  return (
    <main className="deck-list-page">
      <div className="container deck-list-container">
        <Toast message={toastMessage} variant={toastVariant} onDismiss={handleToastDismiss} />

        {/* Thanh chuyển đổi tab bộ từ */}
        <section className="deck-tabs-section">
          <Tabs tabs={buildDeckTabs(t, user)} activeTab={activeTab} onChange={handleTabChange} />

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
              <StatusFilterPills
                statusFilter={userDeckStatusFilter}
                onStatusChange={handleStatusFilterChange}
                t={t}
              />
            )
          }
        />

        {isPublicTab && (
          <section className="deck-list-content">
            <PublicDecksSection
              loading={loading}
              error={error}
              decks={decks}
              activeTab={activeTab}
              user={user}
              hasActiveFilters={hasActiveFilters}
              page={page}
              totalPages={totalPages}
              onRetry={() => setRetryCount((c) => c + 1)}
              onClearFilters={handleClearFilters}
              onOptionsClick={setTargetDeckForOptions}
              onDeckClick={handleDeckClick}
              onPageChange={setPage}
              t={t}
            />
          </section>
        )}

        {activeTab === 'my-decks' && (
          <section className="deck-list-content">
            <MyDecksSection
              loading={userDecksLoading}
              error={userDecksError}
              userDecks={userDecks}
              hasActiveFilters={hasActiveFilters}
              page={userDeckPage}
              totalPages={userDeckTotalPages}
              onRetry={() => setUserDeckRefreshTrigger((c) => c + 1)}
              onClearFilters={handleClearFilters}
              onCreateClick={handleOpenCreateModal}
              onOptionsClick={setTargetDeckForOptions}
              onManageTopics={handleManageTopics}
              onPageChange={setUserDeckPage}
              t={t}
            />
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

      <OptionsModal
        isOpen={!!targetDeckForOptions}
        title={t('deckList.deckOptions')}
        options={deckOptions}
        onClose={() => setTargetDeckForOptions(null)}
      />

      <ReportModal
        isOpen={isReportOpen}
        targetType={targetType}
        targetId={targetId}
        targetLabel={targetLabel}
        onClose={closeReport}
        onSuccess={() => showToast(t('report:successMessage'))}
      />
    </main>
  );
}

export default DeckListPage;
