import React, { useState, useEffect, useMemo, useCallback } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  TagIcon,
  PlusIcon,
  EditIcon,
  TrashIcon,
  SearchIcon,
  CloseIcon,
} from '../../components/icons/AdminIcons';
import {
  fetchAdminTags,
  createAdminTag,
  updateAdminTag,
  deleteAdminTag,
} from '../../services/adminContentApi';
import styles from './TagListPage.module.css';

export default function TagListPage({ onNavigate }) {
  const { t } = useTranslation('admin');

  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Modal create/edit state
  const [modalState, setModalState] = useState({
    isOpen: false,
    mode: 'create', // 'create' | 'edit'
    tagId: null,
    label: '',
    error: null,
    submitting: false,
  });

  // Modal delete state
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    tag: null,
    submitting: false,
    error: null,
  });

  const loadTags = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchAdminTags();
      setTags(data || []);
    } catch (err) {
      setError(err?.message || t('feedback.errorDesc'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    loadTags();
  }, [loadTags]);

  const filteredTags = useMemo(() => {
    if (!searchTerm.trim()) return tags;
    const term = searchTerm.toLowerCase().trim();
    return tags.filter((tag) => tag.label?.toLowerCase().includes(term));
  }, [tags, searchTerm]);

  // Handle open modal
  const handleOpenCreate = () => {
    setModalState({
      isOpen: true,
      mode: 'create',
      tagId: null,
      label: '',
      error: null,
      submitting: false,
    });
  };

  const handleOpenEdit = (tag) => {
    setModalState({
      isOpen: true,
      mode: 'edit',
      tagId: tag.id,
      label: tag.label || '',
      error: null,
      submitting: false,
    });
  };

  const handleCloseModal = () => {
    setModalState((prev) => ({ ...prev, isOpen: false, error: null }));
  };

  const handleSubmitModal = async (e) => {
    e.preventDefault();
    const labelTrimmed = modalState.label.trim();
    if (!labelTrimmed) return;

    try {
      setModalState((prev) => ({ ...prev, submitting: true, error: null }));
      if (modalState.mode === 'create') {
        const created = await createAdminTag({ label: labelTrimmed });
        setTags((prev) => [...prev, created]);
      } else {
        const updated = await updateAdminTag(modalState.tagId, { label: labelTrimmed });
        setTags((prev) => prev.map((t) => (t.id === modalState.tagId ? updated : t)));
      }
      handleCloseModal();
    } catch (err) {
      setModalState((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  // Handle delete
  const handleOpenDelete = (tag) => {
    setDeleteModal({
      isOpen: true,
      tag,
      submitting: false,
      error: null,
    });
  };

  const handleCloseDelete = () => {
    setDeleteModal({
      isOpen: false,
      tag: null,
      submitting: false,
      error: null,
    });
  };

  const handleConfirmDelete = async () => {
    if (!deleteModal.tag) return;
    try {
      setDeleteModal((prev) => ({ ...prev, submitting: true, error: null }));
      await deleteAdminTag(deleteModal.tag.id);
      setTags((prev) => prev.filter((t) => t.id !== deleteModal.tag.id));
      handleCloseDelete();
    } catch (err) {
      const serverMessage = err?.response?.data?.message;
      setDeleteModal((prev) => ({
        ...prev,
        submitting: false,
        error: serverMessage || t('content.tags.tagInUseError'),
      }));
    }
  };

  const columns = [
    {
      title: t('content.tags.tagId'),
      dataIndex: 'id',
      key: 'id',
      width: '100px',
      render: (val) => <span style={{ fontFamily: 'var(--adm-font-mono)' }}>#{val}</span>,
    },
    {
      title: t('content.tags.tagNameLabel'),
      dataIndex: 'label',
      key: 'label',
      render: (val) => (
        <span className={styles.tagChip}>
          <TagIcon size={12} />
          {val}
        </span>
      ),
    },
    {
      title: t('content.decks.actions'),
      key: 'actions',
      width: '140px',
      align: 'right',
      render: (_, record) => (
        <div className={styles.actionsCell}>
          <button
            type="button"
            className={styles.actionBtn}
            onClick={() => handleOpenEdit(record)}
            title={t('content.tags.editTag')}
            aria-label={`${t('content.tags.editTag')} ${record.label}`}
          >
            <EditIcon size={15} />
          </button>
          <button
            type="button"
            className={`${styles.actionBtn} ${styles.deleteActionBtn}`}
            onClick={() => handleOpenDelete(record)}
            title={t('content.tags.deleteTag')}
            aria-label={`${t('content.tags.deleteTag')} ${record.label}`}
          >
            <TrashIcon size={15} />
          </button>
        </div>
      ),
    },
  ];

  if (error && tags.length === 0) {
    return (
      <div className={styles.pageContainer}>
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={loadTags}
        />
      </div>
    );
  }

  return (
    <div className={styles.pageContainer}>
      {/* Header */}
      <div className={styles.headerSection}>
        <div className={styles.titleArea}>
          <h1 className={styles.title}>{t('content.tags.title')}</h1>
          <p className={styles.subtitle}>{t('content.tags.subtitle')}</p>
        </div>
        <button type="button" className={styles.primaryBtn} onClick={handleOpenCreate}>
          <PlusIcon size={16} />
          {t('content.tags.createTagBtn')}
        </button>
      </div>

      {/* Toolbar */}
      <div className={styles.toolbar}>
        <div className={styles.searchBox}>
          <SearchIcon size={16} className={styles.searchIcon} />
          <input
            type="text"
            className={styles.searchInput}
            placeholder={t('content.tags.searchPlaceholder')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className={styles.tagCountBadge}>
          {filteredTags.length} {t('common.items')}
        </div>
      </div>

      {/* Table */}
      <DataTable
        columns={columns}
        data={filteredTags}
        keyField="id"
        isLoading={loading}
        emptyMessage={t('feedback.emptyList')}
      />

      {/* Create / Edit Modal */}
      {modalState.isOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseModal}>
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>
                {modalState.mode === 'create'
                  ? t('content.tags.createTagBtn')
                  : t('content.tags.editTag')}
              </h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={handleCloseModal}
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmitModal}>
              <div className={styles.modalBody}>
                {modalState.error && (
                  <p className={styles.errorMessage}>{modalState.error}</p>
                )}
                <div className={styles.formGroup}>
                  <label htmlFor="tagLabelInput" className={styles.formLabel}>
                    {t('content.tags.tagNameLabel')}
                  </label>
                  <input
                    id="tagLabelInput"
                    type="text"
                    className={styles.formInput}
                    placeholder={t('content.tags.tagNamePlaceholder')}
                    value={modalState.label}
                    onChange={(e) =>
                      setModalState((prev) => ({ ...prev, label: e.target.value }))
                    }
                    autoFocus
                    required
                  />
                </div>
              </div>
              <div className={styles.modalFooter}>
                <button
                  type="button"
                  className={styles.cancelBtn}
                  onClick={handleCloseModal}
                  disabled={modalState.submitting}
                >
                  {t('content.decks.cancelBtn')}
                </button>
                <button
                  type="submit"
                  className={styles.primaryBtn}
                  disabled={modalState.submitting || !modalState.label.trim()}
                >
                  {modalState.mode === 'create'
                    ? t('content.decks.createBtn')
                    : t('content.decks.saveBtn')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteModal.isOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseDelete}>
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>{t('content.tags.deleteTag')}</h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={handleCloseDelete}
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <div className={styles.modalBody}>
              {deleteModal.error && (
                <p className={styles.errorMessage}>{deleteModal.error}</p>
              )}
              <p style={{ margin: 0, fontSize: 13, color: 'var(--adm-text-main)' }}>
                {t('content.tags.deleteTagConfirm')}
              </p>
              {deleteModal.tag && (
                <div style={{ marginTop: 8 }}>
                  <span className={styles.tagChip}>
                    <TagIcon size={12} />
                    {deleteModal.tag.label}
                  </span>
                </div>
              )}
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
                type="button"
                className={styles.dangerBtn}
                onClick={handleConfirmDelete}
                disabled={deleteModal.submitting}
              >
                {t('content.tags.deleteTag')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

TagListPage.propTypes = {
  onNavigate: PropTypes.func,
};
