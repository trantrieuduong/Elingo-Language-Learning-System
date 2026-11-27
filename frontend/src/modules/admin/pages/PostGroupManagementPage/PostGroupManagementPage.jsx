import React, { useCallback, useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  CheckCircleIcon,
  EditIcon,
  LayersIcon,
  PlusIcon,
  RefreshIcon,
  SearchIcon,
  TrashIcon,
} from '../../components/icons/AdminIcons';
import { searchPostGroups } from '../../services/adminCommunityApi';
import filterStyles from '../PostModerationPage/components/CommunityFilterBar.module.css';
import PostGroupFormModal from './components/PostGroupFormModal';
import PostGroupDeleteModal from './components/PostGroupDeleteModal';
import styles from './PostGroupManagementPage.module.css';

const formatDateTime = (isoString, locale) => {
  if (!isoString) return '—';
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return isoString;
  return date.toLocaleString(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function PostGroupManagementPage() {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  const [keyword, setKeyword] = useState('');
  const [sort, setSort] = useState('createdAt,desc');
  const [appliedFilters, setAppliedFilters] = useState({
    keyword: '',
    sort: 'createdAt,desc',
  });

  const [page, setPage] = useState(1);
  const pageSize = 20;

  const [groupData, setGroupData] = useState({
    content: [],
    page: 1,
    size: 20,
    totalElements: 0,
    totalPages: 1,
    isLast: true,
  });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);

  const [formModal, setFormModal] = useState({ isOpen: false, mode: 'create', group: null });
  const [deleteModal, setDeleteModal] = useState({ isOpen: false, group: null });

  useEffect(() => {
    if (!toastMessage) return undefined;
    const timer = setTimeout(() => setToastMessage(null), 3500);
    return () => clearTimeout(timer);
  }, [toastMessage]);

  const fetchGroups = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    try {
      const result = await searchPostGroups({
        ...appliedFilters,
        page,
        size: pageSize,
      });
      setGroupData(result);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [appliedFilters, page]);

  useEffect(() => {
    fetchGroups();
  }, [fetchGroups]);

  const handleApplyFilters = (e) => {
    e.preventDefault();
    setPage(1);
    setAppliedFilters({ keyword, sort });
  };

  const handleResetFilters = () => {
    setKeyword('');
    setSort('createdAt,desc');
    setPage(1);
    setAppliedFilters({ keyword: '', sort: 'createdAt,desc' });
  };

  const handleFormSuccess = (mode) => {
    setFormModal({ isOpen: false, mode: 'create', group: null });
    setToastMessage(
      mode === 'edit' ? t('postGroups.toast.updated') : t('postGroups.toast.created')
    );
    fetchGroups(true);
  };

  const handleDeleteSuccess = () => {
    setDeleteModal({ isOpen: false, group: null });
    setToastMessage(t('postGroups.toast.deleted'));
    fetchGroups(true);
  };

  const columns = useMemo(() => [
    {
      key: 'id',
      dataIndex: 'id',
      title: 'ID',
      width: '70px',
      render: (val) => `#${val}`,
    },
    {
      key: 'groupName',
      dataIndex: 'groupName',
      title: t('postGroups.columns.name'),
      render: (val) => <span className={styles.groupNameText}>{val}</span>,
    },
    {
      key: 'description',
      dataIndex: 'description',
      title: t('postGroups.columns.description'),
      render: (val) => (
        <div className={styles.descCell} title={val || ''}>
          {val || '—'}
        </div>
      ),
    },
    {
      key: 'postCount',
      dataIndex: 'postCount',
      title: t('postGroups.columns.postCount'),
      render: (val) => (
        <span className={`${styles.badge} ${(val || 0) > 0 ? styles.badgeBlue : styles.badgeMuted}`}>
          {val ?? 0}
        </span>
      ),
    },
    {
      key: 'createdAt',
      dataIndex: 'createdAt',
      title: t('postGroups.columns.createdAt'),
      render: (val) => formatDateTime(val, locale),
    },
    {
      key: 'updatedAt',
      dataIndex: 'updatedAt',
      title: t('postGroups.columns.updatedAt'),
      render: (val) => formatDateTime(val, locale),
    },
    {
      key: 'actions',
      title: t('postGroups.columns.actions'),
      align: 'right',
      render: (_, record) => (
        <div className={styles.actionGroup}>
          <button
            type="button"
            className={styles.iconBtn}
            title={t('postGroups.actions.edit')}
            aria-label={t('postGroups.actions.edit')}
            onClick={() => setFormModal({ isOpen: true, mode: 'edit', group: record })}
          >
            <EditIcon size={15} />
          </button>
          <button
            type="button"
            className={`${styles.iconBtn} ${styles.iconBtnDanger}`}
            title={t('postGroups.actions.delete')}
            aria-label={t('postGroups.actions.delete')}
            onClick={() => setDeleteModal({ isOpen: true, group: record })}
          >
            <TrashIcon size={15} />
          </button>
        </div>
      ),
    },
  ], [t, locale]);

  return (
    <div className={styles.pageContainer}>
      {toastMessage && (
        <div className={styles.toast} role="status">
          <CheckCircleIcon size={16} className={styles.toastIcon} />
          <span>{toastMessage}</span>
          <button
            type="button"
            className={styles.toastCloseBtn}
            onClick={() => setToastMessage(null)}
            aria-label={t('postGroups.form.cancel')}
          >
            ×
          </button>
        </div>
      )}

      <div className={styles.pageHeader}>
        <div className={styles.headerInfo}>
          <div className={styles.titleRow}>
            <LayersIcon size={22} className={styles.headerIcon} />
            <h1 className={styles.title}>{t('postGroups.title')}</h1>
          </div>
          <p className={styles.subtitle}>{t('postGroups.subtitle')}</p>
        </div>
        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.primaryBtn}
            onClick={() => setFormModal({ isOpen: true, mode: 'create', group: null })}
          >
            <PlusIcon size={15} />
            <span>{t('postGroups.createBtn')}</span>
          </button>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => fetchGroups(true)}
            disabled={refreshing || loading}
          >
            <RefreshIcon size={15} className={refreshing ? styles.spinning : ''} />
            <span>{t('postGroups.refresh')}</span>
          </button>
        </div>
      </div>

      <form className={filterStyles.filterCard} onSubmit={handleApplyFilters}>
        <div className={filterStyles.filterRow}>
          <div className={filterStyles.searchBox}>
            <SearchIcon size={15} className={filterStyles.searchIcon} />
            <input
              type="text"
              maxLength={100}
              className={filterStyles.searchInput}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder={t('postGroups.filter.keywordPlaceholder')}
              aria-label={t('postGroups.filter.keywordPlaceholder')}
            />
          </div>

          <select
            className={filterStyles.selectInput}
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            aria-label={t('postGroups.filter.sortLabel')}
          >
            <option value="createdAt,desc">{t('postGroups.filter.sortCreatedDesc')}</option>
            <option value="createdAt,asc">{t('postGroups.filter.sortCreatedAsc')}</option>
            <option value="groupName,asc">{t('postGroups.filter.sortNameAsc')}</option>
            <option value="id,desc">{t('postGroups.filter.sortIdDesc')}</option>
          </select>

          <div className={filterStyles.buttonGroup}>
            <button type="submit" className={filterStyles.applyBtn}>
              {t('postGroups.filter.apply')}
            </button>
            <button type="button" className={filterStyles.resetBtn} onClick={handleResetFilters}>
              {t('postGroups.filter.reset')}
            </button>
          </div>
        </div>
      </form>

      {error ? (
        <AdminErrorState onRetry={() => fetchGroups()} />
      ) : (
        <div className={styles.tableSection}>
          <DataTable
            columns={columns}
            data={groupData.content}
            isLoading={loading}
            emptyMessage={t('postGroups.emptyList')}
          />
          <AdminPagination
            page={page}
            totalPages={groupData.totalPages}
            totalElements={groupData.totalElements}
            isLast={Boolean(groupData.isLast ?? groupData.last)}
            onPageChange={setPage}
            unitLabel={t('postGroups.groupsCountSuffix')}
            isLoading={loading}
          />
        </div>
      )}

      <PostGroupFormModal
        isOpen={formModal.isOpen}
        mode={formModal.mode}
        group={formModal.group}
        onClose={() => setFormModal({ isOpen: false, mode: 'create', group: null })}
        onSuccess={handleFormSuccess}
      />

      <PostGroupDeleteModal
        isOpen={deleteModal.isOpen}
        group={deleteModal.group}
        onClose={() => setDeleteModal({ isOpen: false, group: null })}
        onSuccess={handleDeleteSuccess}
      />
    </div>
  );
}

PostGroupManagementPage.propTypes = {
  onNavigate: PropTypes.func,
};
