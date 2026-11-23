import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  CrownIcon,
  EditIcon,
  TrashIcon,
  PlusIcon,
  CloseIcon,
  EyeIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  RefreshIcon,
  AlertTriangleIcon,
} from '../../components/icons/AdminIcons';
import {
  fetchAdminDeckDetail,
  fetchDeckTopics,
  updateDeckStatus,
  deleteUserDeck,
  reorderDeckTopics,
  createSystemTopic,
  updateSystemTopic,
  deleteSystemTopic,
  fetchAdminTags,
  fetchCefrLevels,
} from '../../services/adminContentApi';
import DeckFormModal from './components/DeckFormModal';
import styles from './DeckDetailPage.module.css';

export default function DeckDetailPage({ deckId, onNavigate }) {
  const { t } = useTranslation('admin');

  const [deck, setDeck] = useState(null);
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Catalogs
  const [tags, setTags] = useState([]);
  const [cefrLevels, setCefrLevels] = useState([]);

  // Reusable Edit Deck Modal
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  // Modal change status
  const [statusModal, setStatusModal] = useState({
    isOpen: false,
    status: 'PUBLISHED',
    submitting: false,
    error: null,
  });

  // Modal delete user deck (violation)
  const [deleteDeckModal, setDeleteDeckModal] = useState({
    isOpen: false,
    reason: '',
    submitting: false,
    error: null,
  });

  // Modal create/edit topic
  const [topicModal, setTopicModal] = useState({
    isOpen: false,
    mode: 'create', // 'create' | 'edit'
    topicId: null,
    name: '',
    submitting: false,
    error: null,
  });

  // Modal delete topic
  const [deleteTopicModal, setDeleteTopicModal] = useState({
    isOpen: false,
    topic: null,
    submitting: false,
    error: null,
  });

  // Load catalogs once
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
        // tolerate failure
      }
    };
    loadCatalogs();
  }, []);

  // Load deck & topics
  const loadData = useCallback(async () => {
    if (!deckId) return;
    try {
      setLoading(true);
      setError(null);
      const [deckData, topicsData] = await Promise.all([
        fetchAdminDeckDetail(deckId),
        fetchDeckTopics(deckId),
      ]);
      setDeck(deckData);
      setTopics(topicsData || []);
    } catch (err) {
      setError(err?.message || t('feedback.errorDesc'));
    } finally {
      setLoading(false);
    }
  }, [deckId, t]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const isUserDeck = deck?.ownerType === 'USER';

  // Open Status Modal
  const handleOpenStatusModal = () => {
    if (!deck) return;
    setStatusModal({
      isOpen: true,
      status: deck.status || 'PUBLISHED',
      submitting: false,
      error: null,
    });
  };

  const handleSubmitStatus = async (e) => {
    e.preventDefault();
    try {
      setStatusModal((prev) => ({ ...prev, submitting: true, error: null }));
      const updated = await updateDeckStatus(deckId, statusModal.status);
      setDeck(updated);
      setStatusModal((prev) => ({ ...prev, isOpen: false }));
    } catch (err) {
      setStatusModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  // Delete User Deck
  const handleOpenDeleteDeck = () => {
    setDeleteDeckModal({
      isOpen: true,
      reason: '',
      submitting: false,
      error: null,
    });
  };

  const handleConfirmDeleteDeck = async (e) => {
    e.preventDefault();
    const trimmed = deleteDeckModal.reason.trim();
    if (!trimmed) return;

    try {
      setDeleteDeckModal((prev) => ({ ...prev, submitting: true, error: null }));
      await deleteUserDeck(deckId, trimmed);
      if (onNavigate) onNavigate('/admin/decks');
    } catch (err) {
      setDeleteDeckModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  // Topic CRUD
  const handleOpenCreateTopic = () => {
    setTopicModal({
      isOpen: true,
      mode: 'create',
      topicId: null,
      name: '',
      submitting: false,
      error: null,
    });
  };

  const handleOpenEditTopic = (topic) => {
    setTopicModal({
      isOpen: true,
      mode: 'edit',
      topicId: topic.id,
      name: topic.name || '',
      submitting: false,
      error: null,
    });
  };

  const handleSubmitTopicModal = async (e) => {
    e.preventDefault();
    const trimmed = topicModal.name.trim();
    if (!trimmed) return;

    try {
      setTopicModal((prev) => ({ ...prev, submitting: true, error: null }));
      if (topicModal.mode === 'create') {
        const created = await createSystemTopic({ deckId, name: trimmed });
        setTopics((prev) => [...prev, created]);
      } else {
        const updated = await updateSystemTopic(topicModal.topicId, { name: trimmed });
        setTopics((prev) => prev.map((tp) => (tp.id === topicModal.topicId ? updated : tp)));
      }
      setTopicModal((prev) => ({ ...prev, isOpen: false }));
    } catch (err) {
      setTopicModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  const handleOpenDeleteTopic = (topic) => {
    setDeleteTopicModal({
      isOpen: true,
      topic,
      submitting: false,
      error: null,
    });
  };

  const handleConfirmDeleteTopic = async () => {
    if (!deleteTopicModal.topic) return;
    try {
      setDeleteTopicModal((prev) => ({ ...prev, submitting: true, error: null }));
      await deleteSystemTopic(deleteTopicModal.topic.id);
      setTopics((prev) => prev.filter((tp) => tp.id !== deleteTopicModal.topic.id));
      setDeleteTopicModal({ isOpen: false, topic: null, submitting: false, error: null });
    } catch (err) {
      setDeleteTopicModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  // Reorder Topics (Move Up / Down)
  const handleMoveTopic = async (index, direction) => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= topics.length) return;

    const newTopics = [...topics];
    const temp = newTopics[index];
    newTopics[index] = newTopics[targetIndex];
    newTopics[targetIndex] = temp;

    setTopics(newTopics);

    try {
      const orderedIds = newTopics.map((t) => t.id);
      await reorderDeckTopics(deckId, orderedIds);
    } catch (err) {
      // Revert on failure
      loadData();
    }
  };

  // Topics Table Columns
  const topicColumns = useMemo(
    () => [
      {
        title: t('content.topics.order'),
        key: 'order',
        width: '80px',
        render: (_, __, index) => (
          <span style={{ fontWeight: 600, fontFamily: 'var(--adm-font-mono)' }}>
            #{index + 1}
          </span>
        ),
      },
      {
        title: t('content.topics.topicName'),
        dataIndex: 'name',
        key: 'name',
        render: (val, record) => (
          <a
            href={`/admin/decks/${deckId}/topics/${record.id}`}
            style={{ fontWeight: 600, color: 'var(--adm-text-main)', textDecoration: 'none' }}
            onClick={(e) => {
              e.preventDefault();
              if (onNavigate) onNavigate(`/admin/decks/${deckId}/topics/${record.id}`);
            }}
          >
            {val}
          </a>
        ),
      },
      {
        title: t('content.topics.cardCount'),
        dataIndex: 'cardCount',
        key: 'cardCount',
        width: '140px',
        render: (val) => (
          <span>{t('content.decks.cardsCount', { count: val || 0 })}</span>
        ),
      },
      {
        title: t('content.decks.actions'),
        key: 'actions',
        width: '180px',
        align: 'right',
        render: (_, record, index) => (
          <div className={styles.actionsCell}>
            {/* Xem thẻ từ vựng */}
            <button
              type="button"
              className={styles.actionBtn}
              onClick={() =>
                onNavigate && onNavigate(`/admin/decks/${deckId}/topics/${record.id}`)
              }
              title={t('content.topics.viewCardsBtn')}
              aria-label={`${t('content.topics.viewCardsBtn')} ${record.name}`}
            >
              <EyeIcon size={14} />
            </button>

            {/* Chỉ hiện các nút quản lý nếu deck là SYSTEM */}
            {!isUserDeck && (
              <>
                <button
                  type="button"
                  className={styles.actionBtn}
                  disabled={index === 0}
                  onClick={() => handleMoveTopic(index, 'up')}
                  title={t('content.topics.moveUp')}
                  aria-label={`${t('content.topics.moveUp')} ${record.name}`}
                >
                  <ArrowUpIcon size={14} />
                </button>
                <button
                  type="button"
                  className={styles.actionBtn}
                  disabled={index === topics.length - 1}
                  onClick={() => handleMoveTopic(index, 'down')}
                  title={t('content.topics.moveDown')}
                  aria-label={`${t('content.topics.moveDown')} ${record.name}`}
                >
                  <ArrowDownIcon size={14} />
                </button>
                <button
                  type="button"
                  className={styles.actionBtn}
                  onClick={() => handleOpenEditTopic(record)}
                  title={t('content.topics.editTopic')}
                  aria-label={`${t('content.topics.editTopic')} ${record.name}`}
                >
                  <EditIcon size={14} />
                </button>
                <button
                  type="button"
                  className={`${styles.actionBtn} ${styles.deleteActionBtn}`}
                  onClick={() => handleOpenDeleteTopic(record)}
                  title={t('content.topics.deleteTopic')}
                  aria-label={`${t('content.topics.deleteTopic')} ${record.name}`}
                >
                  <TrashIcon size={14} />
                </button>
              </>
            )}
          </div>
        ),
      },
    ],
    [t, deckId, onNavigate, isUserDeck, topics.length]
  );

  if (error && !deck) {
    return (
      <div className={styles.pageContainer}>
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={loadData}
        />
      </div>
    );
  }

  return (
    <div className={styles.pageContainer}>
      {/* User Warning Banner */}
      {isUserDeck && (
        <div className={styles.userWarningBanner}>
          <AlertTriangleIcon size={18} />
          <span>
            {t('content.decks.userDeckWarning', { ownerId: deck.ownerId || 'N/A' })}
          </span>
        </div>
      )}

      {/* Deck Overview Card */}
      {deck && (
        <div className={styles.overviewCard}>
          <div className={styles.coverBox}>
            {deck.coverImageUrl ? (
              <img src={deck.coverImageUrl} alt="" className={styles.coverImg} />
            ) : (
              <span className={styles.coverPlaceholder}>
                {deck.title ? deck.title.charAt(0).toUpperCase() : 'D'}
              </span>
            )}
          </div>

          <div className={styles.detailContent}>
            <div className={styles.titleRow}>
              <h1 className={styles.deckTitle}>{deck.title}</h1>
              <div className={styles.headerActions}>
                {isUserDeck ? (
                  <button
                    type="button"
                    className={styles.dangerBtn}
                    onClick={handleOpenDeleteDeck}
                  >
                    <TrashIcon size={15} />
                    {t('content.decks.deleteUserDeckBtn')}
                  </button>
                ) : (
                  <>
                    <button
                      type="button"
                      className={styles.secondaryBtn}
                      onClick={() => setIsEditModalOpen(true)}
                    >
                      <EditIcon size={14} />
                      {t('content.decks.editDeckBtn')}
                    </button>
                    <button
                      type="button"
                      className={styles.secondaryBtn}
                      onClick={handleOpenStatusModal}
                    >
                      <RefreshIcon size={14} />
                      {t('content.decks.changeStatusBtn')}
                    </button>
                  </>
                )}
              </div>
            </div>

            <span className={styles.deckSlug}>{deck.slug}</span>
            {deck.description && (
              <p className={styles.deckDescription}>{deck.description}</p>
            )}

            <div className={styles.badgesRow}>
              {/* OwnerType */}
              <span
                className={`${styles.badge} ${
                  isUserDeck ? styles.badgeUser : styles.badgeSystem
                }`}
              >
                {isUserDeck
                  ? t('content.decks.userOwner')
                  : t('content.decks.systemOwner')}
              </span>

              {/* Status (DRAFT, PUBLISHED, ARCHIVED) */}
              <span
                className={`${styles.badge} ${
                  deck.status === 'PUBLISHED'
                    ? styles.badgePublished
                    : deck.status === 'ARCHIVED'
                    ? styles.badgeArchived
                    : styles.badgeDraft
                }`}
              >
                {deck.status === 'PUBLISHED'
                  ? t('content.decks.statusPublished')
                  : deck.status === 'ARCHIVED'
                  ? t('content.decks.statusArchived')
                  : t('content.decks.statusDraft')}
              </span>

              {/* Premium */}
              <span
                className={`${styles.badge} ${
                  deck.isPremium ? styles.badgePremium : styles.badgeFree
                }`}
              >
                {deck.isPremium ? (
                  <>
                    <CrownIcon size={12} />
                    {t('content.decks.premiumTier')}
                  </>
                ) : (
                  t('content.decks.freeTier')
                )}
              </span>

              {/* CEFR */}
              {deck.cefrLevels?.map((c) => (
                <span key={c.id} className={styles.cefrPill}>
                  {c.label}
                </span>
              ))}

              {/* Tags */}
              {deck.tags?.map((tg) => (
                <span key={tg.id} className={styles.tagPill}>
                  #{tg.label}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Topics Section */}
      <div className={styles.sectionHeader}>
        <h2 className={styles.sectionTitle}>{t('content.topics.title')}</h2>
        {!isUserDeck && (
          <button
            type="button"
            className={styles.primaryBtn}
            onClick={handleOpenCreateTopic}
          >
            <PlusIcon size={16} />
            {t('content.topics.createTopicBtn')}
          </button>
        )}
      </div>

      <DataTable
        columns={topicColumns}
        data={topics}
        keyField="id"
        isLoading={loading}
        emptyMessage={t('feedback.emptyList')}
      />

      {/* Reusable Edit Deck Modal (SYSTEM decks only) */}
      <DeckFormModal
        isOpen={isEditModalOpen}
        mode="edit"
        initialData={deck}
        tags={tags}
        cefrLevels={cefrLevels}
        onClose={() => setIsEditModalOpen(false)}
        onSuccess={(updated) => setDeck(updated)}
      />

      {/* Change Status Modal */}
      {statusModal.isOpen && (
        <div
          className={styles.modalBackdrop}
          onClick={() => setStatusModal((prev) => ({ ...prev, isOpen: false }))}
        >
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>{t('content.decks.changeStatusBtn')}</h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={() => setStatusModal((prev) => ({ ...prev, isOpen: false }))}
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmitStatus}>
              <div className={styles.modalBody}>
                {statusModal.error && (
                  <p className={styles.errorMessage}>{statusModal.error}</p>
                )}
                <div className={styles.formGroup}>
                  <label htmlFor="deckStatusDetailSelect" className={styles.formLabel}>
                    {t('content.decks.statusFilter')}
                  </label>
                  <select
                    id="deckStatusDetailSelect"
                    className={styles.formSelect}
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
                  onClick={() => setStatusModal((prev) => ({ ...prev, isOpen: false }))}
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

      {/* Delete User Deck (Hard Delete with Reason) */}
      {deleteDeckModal.isOpen && (
        <div
          className={styles.modalBackdrop}
          onClick={() => setDeleteDeckModal((prev) => ({ ...prev, isOpen: false }))}
        >
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>
                {t('content.decks.deleteUserDeckModalTitle')}
              </h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={() => setDeleteDeckModal((prev) => ({ ...prev, isOpen: false }))}
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <form onSubmit={handleConfirmDeleteDeck}>
              <div className={styles.modalBody}>
                {deleteDeckModal.error && (
                  <p className={styles.errorMessage}>{deleteDeckModal.error}</p>
                )}
                <p style={{ margin: 0, fontSize: 13, color: 'var(--adm-danger)' }}>
                  {t('content.decks.deleteUserDeckModalDesc')}
                </p>
                <div className={styles.formGroup}>
                  <label htmlFor="userDeckReason" className={styles.formLabel}>
                    {t('content.decks.reasonPlaceholder')} *
                  </label>
                  <textarea
                    id="userDeckReason"
                    className={styles.formTextarea}
                    placeholder={t('content.decks.reasonPlaceholder')}
                    value={deleteDeckModal.reason}
                    onChange={(e) =>
                      setDeleteDeckModal((prev) => ({ ...prev, reason: e.target.value }))
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
                  onClick={() => setDeleteDeckModal((prev) => ({ ...prev, isOpen: false }))}
                  disabled={deleteDeckModal.submitting}
                >
                  {t('content.decks.cancelBtn')}
                </button>
                <button
                  type="submit"
                  className={styles.dangerBtn}
                  disabled={deleteDeckModal.submitting || !deleteDeckModal.reason.trim()}
                >
                  {t('content.decks.confirmDeleteBtn')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Topic Create / Edit Modal */}
      {topicModal.isOpen && (
        <div
          className={styles.modalBackdrop}
          onClick={() => setTopicModal((prev) => ({ ...prev, isOpen: false }))}
        >
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>
                {topicModal.mode === 'create'
                  ? t('content.topics.createTopicBtn')
                  : t('content.topics.editTopic')}
              </h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={() => setTopicModal((prev) => ({ ...prev, isOpen: false }))}
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmitTopicModal}>
              <div className={styles.modalBody}>
                {topicModal.error && (
                  <p className={styles.errorMessage}>{topicModal.error}</p>
                )}
                <div className={styles.formGroup}>
                  <label htmlFor="topicNameInput" className={styles.formLabel}>
                    {t('content.topics.topicNameLabel')} *
                  </label>
                  <input
                    id="topicNameInput"
                    type="text"
                    className={styles.formInput}
                    placeholder={t('content.topics.topicNamePlaceholder')}
                    value={topicModal.name}
                    onChange={(e) =>
                      setTopicModal((prev) => ({ ...prev, name: e.target.value }))
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
                  onClick={() => setTopicModal((prev) => ({ ...prev, isOpen: false }))}
                  disabled={topicModal.submitting}
                >
                  {t('content.decks.cancelBtn')}
                </button>
                <button
                  type="submit"
                  className={styles.primaryBtn}
                  disabled={topicModal.submitting || !topicModal.name.trim()}
                >
                  {topicModal.mode === 'create'
                    ? t('content.decks.createBtn')
                    : t('content.decks.saveBtn')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Topic Modal */}
      {deleteTopicModal.isOpen && (
        <div
          className={styles.modalBackdrop}
          onClick={() =>
            setDeleteTopicModal({ isOpen: false, topic: null, submitting: false, error: null })
          }
        >
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>{t('content.topics.deleteTopic')}</h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={() =>
                  setDeleteTopicModal({
                    isOpen: false,
                    topic: null,
                    submitting: false,
                    error: null,
                  })
                }
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <div className={styles.modalBody}>
              {deleteTopicModal.error && (
                <p className={styles.errorMessage}>{deleteTopicModal.error}</p>
              )}
              <p style={{ margin: 0, fontSize: 13, color: 'var(--adm-text-main)' }}>
                {t('content.topics.deleteTopicConfirm')}
              </p>
              {deleteTopicModal.topic && (
                <div
                  style={{
                    padding: 8,
                    borderRadius: 'var(--adm-radius-xs)',
                    background: 'var(--adm-bg-surface-subtle)',
                    fontWeight: 600,
                    fontSize: 13,
                  }}
                >
                  {deleteTopicModal.topic.name}
                </div>
              )}
            </div>
            <div className={styles.modalFooter}>
              <button
                type="button"
                className={styles.cancelBtn}
                onClick={() =>
                  setDeleteTopicModal({
                    isOpen: false,
                    topic: null,
                    submitting: false,
                    error: null,
                  })
                }
                disabled={deleteTopicModal.submitting}
              >
                {t('content.decks.cancelBtn')}
              </button>
              <button
                type="button"
                className={styles.dangerBtn}
                onClick={handleConfirmDeleteTopic}
                disabled={deleteTopicModal.submitting}
              >
                {t('content.topics.deleteTopic')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

DeckDetailPage.propTypes = {
  deckId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  onNavigate: PropTypes.func,
};
