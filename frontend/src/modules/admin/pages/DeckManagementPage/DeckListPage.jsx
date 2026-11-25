import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  BookOpenIcon,
  LayersIcon,
  PlusIcon,
  SearchIcon,
  EyeIcon,
  EditIcon,
  TrashIcon,
  CloseIcon,
  CrownIcon,
  RefreshIcon,
} from '../../components/icons/AdminIcons';
import {
  fetchAdminDecks,
  updateDeckStatus,
  deleteUserDeck,
  fetchAdminTags,
  fetchCefrLevels,
} from '../../services/adminContentApi';
import DeckFormModal from './components/DeckFormModal';
import OrphanedCardsTab from './components/OrphanedCardsTab';
import styles from './DeckListPage.module.css';

export default function DeckListPage({ onNavigate }) {
  const { t } = useTranslation('admin');

  // Page-level tab: 'decks' | 'orphanedCards'
  const [activeTab, setActiveTab] = useState('decks');

  // Filter state (ownerType default to SYSTEM)
  const [ownerType, setOwnerType] = useState('SYSTEM');
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [isPremium, setIsPremium] = useState('');
  const [selectedTagId, setSelectedTagId] = useState('');
  const [selectedCefrId, setSelectedCefrId] = useState('');
  const [page, setPage] = useState(1); // 1-indexed for Spring Pageable
  const pageSize = 20;

  // Metadata catalogs
  const [tags, setTags] = useState([]);
  const [cefrLevels, setCefrLevels] = useState([]);

  // Data & loading state
  const [deckData, setDeckData] = useState({
    content: [],
    page: 1,
    size: 20,
    totalElements: 0,
    totalPages: 1,
    isLast: true,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Reusable Create/Edit System Deck Modal
  const [formModal, setFormModal] = useState({
    isOpen: false,
    mode: 'create', // 'create' | 'edit'
    initialData: null,
  });

  // Modal delete user deck (violation)
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    deck: null,
    reason: '',
    submitting: false,
    error: null,
  });

  // Modal update status (quick status change)
  const [statusModal, setStatusModal] = useState({
    isOpen: false,
    deck: null,
    status: 'PUBLISHED',
    submitting: false,
    error: null,
  });

  // Load catalogs on mount
  useEffect(() => {
    const loadCatalogs = async () => {
      try {
        const [tagsData, cefrData] = await Promise.all([
          fetchAdminTags(),
          fetchCefrLevels(),
        ]);
        setTags(tagsData || []);
        setCefrLevels(cefrData || []);
      } catch (err) {
        // Silently tolerate catalog failure
      }
    };
    loadCatalogs();
  }, []);

  // Load decks
  const loadDecks = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const params = {
        keyword: keyword || undefined,
        ownerType,
        status: status || undefined,
        isPremium: isPremium === '' ? undefined : isPremium === 'true',
        tagIds: selectedTagId ? [Number(selectedTagId)] : undefined,
        cefrLevelIds: selectedCefrId ? [Number(selectedCefrId)] : undefined,
        page,
        size: pageSize,
        sort: 'createdAt,desc',
      };

      const res = await fetchAdminDecks(params);
      if (res) {
        setDeckData(res);
      }
    } catch (err) {
      setError(err?.message || t('feedback.errorDesc'));
    } finally {
      setLoading(false);
    }
  }, [keyword, ownerType, status, isPremium, selectedTagId, selectedCefrId, page, t]);

  useEffect(() => {
    loadDecks();
  }, [loadDecks]);

  // Handle ownerType change
  const handleOwnerTypeChange = (type) => {
    setOwnerType(type);
    setPage(1);
  };

  // Open modal create
  const handleOpenCreate = () => {
    setFormModal({
      isOpen: true,
      mode: 'create',
      initialData: null,
    });
  };

  // Open modal edit (System decks only)
  const handleOpenEdit = (deck) => {
    setFormModal({
      isOpen: true,
      mode: 'edit',
      initialData: deck,
    });
  };

  // Status Change Modal
  const handleOpenStatusModal = (deck) => {
    setStatusModal({
      isOpen: true,
      deck,
      status: deck.status || 'PUBLISHED',
      submitting: false,
      error: null,
    });
  };

  const handleCloseStatusModal = () => {
    setStatusModal((prev) => ({ ...prev, isOpen: false, error: null }));
  };

  const handleConfirmStatusChange = async (e) => {
    e.preventDefault();
    if (!statusModal.deck) return;
    try {
      setStatusModal((prev) => ({ ...prev, submitting: true, error: null }));
      await updateDeckStatus(statusModal.deck.id, statusModal.status);
      handleCloseStatusModal();
      loadDecks();
    } catch (err) {
      setStatusModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  // Delete User Deck Modal
  const handleOpenDelete = (deck) => {
    setDeleteModal({
      isOpen: true,
      deck,
      reason: '',
      submitting: false,
      error: null,
    });
  };

  const handleCloseDelete = () => {
    setDeleteModal({
      isOpen: false,
      deck: null,
      reason: '',
      submitting: false,
      error: null,
    });
  };

  const handleConfirmDelete = async (e) => {
    e.preventDefault();
    if (!deleteModal.deck) return;
    const trimmedReason = deleteModal.reason.trim();
    if (!trimmedReason) return;

    try {
      setDeleteModal((prev) => ({ ...prev, submitting: true, error: null }));
      await deleteUserDeck(deleteModal.deck.id, trimmedReason);
      handleCloseDelete();
      loadDecks();
    } catch (err) {
      setDeleteModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  // Table columns definition with Tags and CEFR separated into distinct columns
  const columns = useMemo(
    () => [
      {
        title: t('content.decks.deckName'),
        key: 'deckInfo',
        render: (_, record) => (
          <div className={styles.deckCell}>
            {record.coverImageUrl ? (
              <img
                src={record.coverImageUrl}
                alt=""
                className={styles.deckCover}
                onError={(e) => {
                  e.target.style.display = 'none';
                }}
              />
            ) : (
              <div className={styles.deckCoverPlaceholder}>
                {record.title ? record.title.charAt(0).toUpperCase() : 'D'}
              </div>
            )}
            <div className={styles.deckInfo}>
              <a
                href={`/admin/decks/${record.id}`}
                className={styles.deckTitle}
                onClick={(e) => {
                  e.preventDefault();
                  if (onNavigate) onNavigate(`/admin/decks/${record.id}`);
                }}
              >
                {record.title}
              </a>
              <span className={styles.deckSlug}>{record.slug}</span>
              {record.description && (
                <span className={styles.deckDescription} title={record.description}>
                  {record.description}
                </span>
              )}
            </div>
          </div>
        ),
      },
      {
        title: t('content.decks.ownerTypeFilter'),
        dataIndex: 'ownerType',
        key: 'ownerType',
        width: '120px',
        render: (val) => (
          <span
            className={`${styles.badge} ${
              val === 'SYSTEM' ? styles.badgeSystem : styles.badgeUser
            }`}
          >
            {val === 'SYSTEM'
              ? t('content.decks.systemOwner')
              : t('content.decks.userOwner')}
          </span>
        ),
      },
      {
        title: t('content.decks.statusFilter'),
        dataIndex: 'status',
        key: 'status',
        width: '120px',
        render: (val) => {
          let badgeClass = styles.badgePublished;
          let label = t('content.decks.statusPublished');
          if (val === 'DRAFT') {
            badgeClass = styles.badgeDraft;
            label = t('content.decks.statusDraft');
          } else if (val === 'ARCHIVED') {
            badgeClass = styles.badgeArchived;
            label = t('content.decks.statusArchived');
          }
          return <span className={`${styles.badge} ${badgeClass}`}>{label}</span>;
        },
      },
      {
        title: t('content.decks.premiumFilter'),
        dataIndex: 'isPremium',
        key: 'isPremium',
        width: '100px',
        render: (val) => (
          <span className={`${styles.badge} ${val ? styles.badgePremium : styles.badgeFree}`}>
            {val ? (
              <>
                <CrownIcon size={12} />
                {t('content.decks.premiumTier')}
              </>
            ) : (
              t('content.decks.freeTier')
            )}
          </span>
        ),
      },
      // Cột CEFR Level riêng biệt
      {
        title: t('content.decks.cefrFilter'),
        dataIndex: 'cefrLevels',
        key: 'cefrLevels',
        width: '110px',
        render: (levels) => (
          <div className={styles.pillsGroup}>
            {levels?.length > 0
              ? levels.map((cefr) => (
                  <span key={cefr.id} className={styles.cefrPill}>
                    {cefr.label}
                  </span>
                ))
              : '—'}
          </div>
        ),
      },
      // Cột Chủ đề (Tag) riêng biệt
      {
        title: t('content.decks.tagFilter'),
        dataIndex: 'tags',
        key: 'tags',
        width: '150px',
        render: (tagList) => (
          <div className={styles.pillsGroup}>
            {tagList?.length > 0
              ? tagList.map((tag) => (
                  <span key={tag.id} className={styles.tagPill}>
                    #{tag.label}
                  </span>
                ))
              : '—'}
          </div>
        ),
      },
      {
        title: t('content.topics.title'),
        key: 'metrics',
        width: '130px',
        render: (_, record) => (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, fontSize: 12 }}>
            <span style={{ fontWeight: 600, color: 'var(--adm-text-main)' }}>
              {t('content.decks.topicsCount', { count: record.topicCount || 0 })}
            </span>
            <span style={{ color: 'var(--adm-text-subtle)' }}>
              {t('content.decks.cardsCount', { count: record.cardCount || 0 })}
            </span>
          </div>
        ),
      },
      {
        title: t('content.decks.actions'),
        key: 'actions',
        width: '140px',
        align: 'right',
        render: (_, record) => {
          const isSystem = record.ownerType === 'SYSTEM';
          return (
            <div className={styles.actionsCell}>
              {/* Nút Xem chi tiết */}
              <button
                type="button"
                className={styles.actionBtn}
                onClick={() => onNavigate && onNavigate(`/admin/decks/${record.id}`)}
                title={t('content.decks.viewDetail')}
                aria-label={`${t('content.decks.viewDetail')} ${record.title}`}
              >
                <EyeIcon size={15} />
              </button>

              {/* Nếu là Deck Hệ thống: Cho phép Sửa và Đổi status */}
              {isSystem ? (
                <>
                  <button
                    type="button"
                    className={styles.actionBtn}
                    onClick={() => handleOpenEdit(record)}
                    title={t('content.decks.editDeckBtn')}
                    aria-label={`${t('content.decks.editDeckBtn')} ${record.title}`}
                  >
                    <EditIcon size={15} />
                  </button>
                  <button
                    type="button"
                    className={styles.actionBtn}
                    onClick={() => handleOpenStatusModal(record)}
                    title={t('content.decks.changeStatusBtn')}
                    aria-label={`${t('content.decks.changeStatusBtn')} ${record.title}`}
                  >
                    <RefreshIcon size={14} />
                  </button>
                </>
              ) : (
                /* Nếu là Deck Học viên: Chỉ cho phép Xóa cứng do vi phạm (Hard Delete) */
                <button
                  type="button"
                  className={`${styles.actionBtn} ${styles.deleteActionBtn}`}
                  onClick={() => handleOpenDelete(record)}
                  title={t('content.decks.deleteUserDeckBtn')}
                  aria-label={`${t('content.decks.deleteUserDeckBtn')} ${record.title}`}
                >
                  <TrashIcon size={15} />
                </button>
              )}
            </div>
          );
        },
      },
    ],
    [t, onNavigate]
  );

  return (
    <div className={styles.pageContainer}>
      {/* Header */}
      <div className={styles.headerSection}>
        <div className={styles.titleArea}>
          <h1 className={styles.title}>{t('content.decks.title')}</h1>
          <p className={styles.subtitle}>{t('content.decks.subtitle')}</p>
        </div>
        {activeTab === 'decks' && (
          <button type="button" className={styles.primaryBtn} onClick={handleOpenCreate}>
            <PlusIcon size={16} />
            {t('content.decks.createSystemDeck')}
          </button>
        )}
      </div>

      {/* Page Tabs */}
      <div className={styles.tabNavRow} role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'decks'}
          className={`${styles.tabBtn} ${activeTab === 'decks' ? styles.tabBtnActive : ''}`}
          onClick={() => setActiveTab('decks')}
        >
          <BookOpenIcon size={16} />
          <span>{t('content.decks.tabs.deckList')}</span>
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'orphanedCards'}
          className={`${styles.tabBtn} ${activeTab === 'orphanedCards' ? styles.tabBtnActive : ''}`}
          onClick={() => setActiveTab('orphanedCards')}
        >
          <LayersIcon size={16} />
          <span>{t('content.decks.tabs.orphanedCards')}</span>
        </button>
      </div>

      {activeTab === 'orphanedCards' ? (
        <OrphanedCardsTab />
      ) : (
        <>
          {/* Filter Card */}
      <div className={styles.filterCard}>
        {/* OwnerType Segments: Only 2 options (SYSTEM and USER) */}
        <div className={styles.ownerTypeBar}>
          <button
            type="button"
            className={`${styles.segmentBtn} ${ownerType === 'SYSTEM' ? styles.active : ''}`}
            onClick={() => handleOwnerTypeChange('SYSTEM')}
          >
            {t('content.decks.systemOwner')}
          </button>
          <button
            type="button"
            className={`${styles.segmentBtn} ${ownerType === 'USER' ? styles.active : ''}`}
            onClick={() => handleOwnerTypeChange('USER')}
          >
            {t('content.decks.userOwner')}
          </button>
        </div>

        {/* Filter Inputs Grid (5 columns: Search, Tag, Status, Tier, CEFR) */}
        <div className={styles.filterGrid}>
          {/* Keyword Search */}
          <div className={styles.filterItem}>
            <span className={styles.filterLabel}>{t('content.decks.searchPlaceholder')}</span>
            <div className={styles.searchInputWrapper}>
              <SearchIcon size={15} style={{ color: 'var(--adm-text-disabled)' }} />
              <input
                type="text"
                className={styles.searchInput}
                placeholder={t('content.decks.searchPlaceholder')}
                value={keyword}
                onChange={(e) => {
                  setKeyword(e.target.value);
                  setPage(1);
                }}
              />
            </div>
          </div>

          {/* Tag Filter */}
          <div className={styles.filterItem}>
            <span className={styles.filterLabel}>{t('content.decks.tagFilter')}</span>
            <select
              className={styles.filterSelect}
              value={selectedTagId}
              onChange={(e) => {
                setSelectedTagId(e.target.value);
                setPage(1);
              }}
            >
              <option value="">{t('content.decks.allTags')}</option>
              {tags.map((tg) => (
                <option key={tg.id} value={tg.id}>
                  #{tg.label}
                </option>
              ))}
            </select>
          </div>

          {/* Status Select (DRAFT, PUBLISHED, ARCHIVED) */}
          <div className={styles.filterItem}>
            <span className={styles.filterLabel}>{t('content.decks.statusFilter')}</span>
            <select
              className={styles.filterSelect}
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(1);
              }}
            >
              <option value="">{t('content.decks.allStatuses')}</option>
              <option value="DRAFT">{t('content.decks.statusDraft')}</option>
              <option value="PUBLISHED">{t('content.decks.statusPublished')}</option>
              <option value="ARCHIVED">{t('content.decks.statusArchived')}</option>
            </select>
          </div>

          {/* Premium Filter */}
          <div className={styles.filterItem}>
            <span className={styles.filterLabel}>{t('content.decks.premiumFilter')}</span>
            <select
              className={styles.filterSelect}
              value={isPremium}
              onChange={(e) => {
                setIsPremium(e.target.value);
                setPage(1);
              }}
            >
              <option value="">{t('content.decks.allTiers')}</option>
              <option value="false">{t('content.decks.freeTier')}</option>
              <option value="true">{t('content.decks.premiumTier')}</option>
            </select>
          </div>

          {/* CEFR Level Filter */}
          <div className={styles.filterItem}>
            <span className={styles.filterLabel}>{t('content.decks.cefrFilter')}</span>
            <select
              className={styles.filterSelect}
              value={selectedCefrId}
              onChange={(e) => {
                setSelectedCefrId(e.target.value);
                setPage(1);
              }}
            >
              <option value="">{t('content.decks.allCefrLevels')}</option>
              {cefrLevels.map((lvl) => (
                <option key={lvl.id} value={lvl.id}>
                  {lvl.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={loadDecks}
        />
      )}

      {/* Table & Pagination Card */}
      {!error && (
        <div className={styles.tableCard}>
          <DataTable
            borderless
            columns={columns}
            data={deckData.content}
            keyField="id"
            isLoading={loading}
            emptyMessage={t('feedback.emptyList')}
          />

          {/* Pagination Bar */}
          {!loading && deckData.totalElements > 0 && (
            <AdminPagination
              page={page}
              totalPages={deckData.totalPages}
              totalElements={deckData.totalElements}
              isLast={Boolean(deckData.isLast)}
              onPageChange={setPage}
              unitLabel={t('common.items')}
              isLoading={loading}
            />
          )}
        </div>
      )}

      {/* Reusable Create/Edit System Deck Modal */}
      <DeckFormModal
        isOpen={formModal.isOpen}
        mode={formModal.mode}
        initialData={formModal.initialData}
        tags={tags}
        cefrLevels={cefrLevels}
        onClose={() => setFormModal((prev) => ({ ...prev, isOpen: false }))}
        onSuccess={() => loadDecks()}
      />

      {/* Modal Quick Status Change */}
      {statusModal.isOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseStatusModal}>
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>{t('content.decks.changeStatusBtn')}</h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={handleCloseStatusModal}
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <form onSubmit={handleConfirmStatusChange}>
              <div className={styles.modalBody}>
                {statusModal.error && (
                  <p className={styles.errorMessage}>{statusModal.error}</p>
                )}
                <p style={{ margin: 0, fontSize: 13, color: 'var(--adm-text-main)' }}>
                  {statusModal.deck?.title}
                </p>
                <div className={styles.formGroup}>
                  <label htmlFor="deckStatusSelect" className={styles.formLabel}>
                    {t('content.decks.statusFilter')}
                  </label>
                  <select
                    id="deckStatusSelect"
                    className={styles.filterSelect}
                    value={statusModal.status}
                    onChange={(e) =>
                      setStatusModal((prev) => ({ ...prev, status: e.target.value }))
                    }
                  >
                    <option value="DRAFT">{t('content.decks.statusDraft')}</option>
                    <option value="PUBLISHED">{t('content.decks.statusPublished')}</option>
                    <option value="ARCHIVED">{t('content.decks.statusArchived')}</option>
                  </select>
                </div>
              </div>
              <div className={styles.modalFooter}>
                <button
                  type="button"
                  className={styles.cancelBtn}
                  onClick={handleCloseStatusModal}
                  disabled={statusModal.submitting}
                >
                  {t('content.decks.cancelBtn')}
                </button>
                <button
                  type="submit"
                  className={styles.primaryBtn}
                  disabled={statusModal.submitting}
                >
                  {t('content.decks.saveBtn')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Delete User Deck (Hard Delete with Violation Reason) */}
      {deleteModal.isOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseDelete}>
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>
                {t('content.decks.deleteUserDeckModalTitle')}
              </h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={handleCloseDelete}
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <form onSubmit={handleConfirmDelete}>
              <div className={styles.modalBody}>
                {deleteModal.error && (
                  <p className={styles.errorMessage}>{deleteModal.error}</p>
                )}
                <p style={{ margin: 0, fontSize: 13, color: 'var(--adm-danger)' }}>
                  {t('content.decks.deleteUserDeckModalDesc')}
                </p>
                {deleteModal.deck && (
                  <div
                    style={{
                      padding: 10,
                      borderRadius: 'var(--adm-radius-sm)',
                      background: 'var(--adm-bg-surface-subtle)',
                      fontSize: 13,
                      fontWeight: 600,
                    }}
                  >
                    {deleteModal.deck.title} ({deleteModal.deck.slug})
                  </div>
                )}
                <div className={styles.formGroup}>
                  <label htmlFor="violationReasonInput" className={styles.formLabel}>
                    {t('content.decks.reasonPlaceholder')} *
                  </label>
                  <textarea
                    id="violationReasonInput"
                    className={styles.formTextarea}
                    placeholder={t('content.decks.reasonPlaceholder')}
                    value={deleteModal.reason}
                    onChange={(e) =>
                      setDeleteModal((prev) => ({ ...prev, reason: e.target.value }))
                    }
                    required
                    autoFocus
                  />
                </div>
              </div>
              <div className={styles.modalFooter}>
                <button
                  type="button"
                  className={styles.cancelBtn}
                  onClick={handleCloseDelete}
                  disabled={deleteModal.submitting}
                >
                  {t('content.decks.cancelBtn')}
                </button>
                <button
                  type="submit"
                  className={styles.dangerBtn}
                  disabled={deleteModal.submitting || !deleteModal.reason.trim()}
                >
                  {t('content.decks.confirmDeleteBtn')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
        </>
      )}
    </div>
  );
}

DeckListPage.propTypes = {
  onNavigate: PropTypes.func,
};
