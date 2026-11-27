import React, { useCallback, useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  ActivityIcon,
  CheckCircleIcon,
  EyeIcon,
  MessageSquareIcon,
  RefreshIcon,
  XCircleIcon,
} from '../../components/icons/AdminIcons';
import {
  getPostGroups,
  searchModerationLogs,
  searchModerationPosts,
} from '../../services/adminCommunityApi';
import PostModerationFilterBar from './components/PostModerationFilterBar';
import ModerationLogFilterBar from './components/ModerationLogFilterBar';
import PostModerationDetailModal from './components/PostModerationDetailModal';
import ModerationDecisionModal from './components/ModerationDecisionModal';
import styles from './PostModerationPage.module.css';

const DEFAULT_POST_STATUS = 'NEEDS_REVIEW';

const STATUS_BADGE_MAP = {
  PUBLISHED: styles.badgeGreen,
  NEEDS_REVIEW: styles.badgeAmber,
  PENDING: styles.badgeBlue,
  REJECTED: styles.badgeRed,
  DELETED: styles.badgeMuted,
};

const ACTION_BADGE_MAP = {
  AUTO_APPROVE: styles.badgeGreen,
  MANUAL_APPROVE: styles.badgeGreen,
  FLAG_FOR_REVIEW: styles.badgeAmber,
  AUTO_REJECT: styles.badgeRed,
  MANUAL_REJECT: styles.badgeRed,
};

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

const isValidDateRange = (fromStr, toStr) => {
  if (!fromStr || !toStr) return true;
  const from = new Date(fromStr);
  const to = new Date(toStr);
  if (from > to) return false;
  return (to - from) / (1000 * 60 * 60 * 24) <= 366;
};

export default function PostModerationPage({ initialPostId = '' }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  const [activeTab, setActiveTab] = useState('posts');
  const [groups, setGroups] = useState([]);
  const [toastMessage, setToastMessage] = useState(null);

  // Tab 1: Posts filter state (mặc định NEEDS_REVIEW)
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState(DEFAULT_POST_STATUS);
  const [groupId, setGroupId] = useState('');
  const [userId, setUserId] = useState('');
  const [createdFrom, setCreatedFrom] = useState('');
  const [createdTo, setCreatedTo] = useState('');
  const [sort, setSort] = useState('createdAt,desc');
  const [postFilterError, setPostFilterError] = useState('');
  const [appliedPostFilters, setAppliedPostFilters] = useState({
    keyword: '',
    status: DEFAULT_POST_STATUS,
    groupId: '',
    userId: '',
    createdFrom: '',
    createdTo: '',
    sort: 'createdAt,desc',
  });
  const [postPage, setPostPage] = useState(1);
  const [postData, setPostData] = useState({
    content: [],
    page: 1,
    size: 20,
    totalElements: 0,
    totalPages: 1,
    isLast: true,
  });
  const [postLoading, setPostLoading] = useState(true);
  const [postError, setPostError] = useState(null);

  // Tab 2: Logs filter state
  const [logPostId, setLogPostId] = useState('');
  const [logActorType, setLogActorType] = useState('');
  const [logAction, setLogAction] = useState('');
  const [logAiLabel, setLogAiLabel] = useState('');
  const [logCreatedFrom, setLogCreatedFrom] = useState('');
  const [logCreatedTo, setLogCreatedTo] = useState('');
  const [logSort, setLogSort] = useState('createdAt,desc');
  const [logFilterError, setLogFilterError] = useState('');
  const [appliedLogFilters, setAppliedLogFilters] = useState({
    postId: '',
    actorType: '',
    action: '',
    aiLabel: '',
    createdFrom: '',
    createdTo: '',
    sort: 'createdAt,desc',
  });
  const [logPage, setLogPage] = useState(1);
  const [logData, setLogData] = useState({
    content: [],
    page: 1,
    size: 20,
    totalElements: 0,
    totalPages: 1,
    isLast: true,
  });
  const [logLoading, setLogLoading] = useState(false);
  const [logError, setLogError] = useState(null);

  const [refreshing, setRefreshing] = useState(false);
  const [detailModal, setDetailModal] = useState({
    isOpen: Boolean(initialPostId),
    postId: initialPostId || null,
  });
  const [decisionModal, setDecisionModal] = useState({ isOpen: false, mode: 'approve', post: null });

  useEffect(() => {
    if (initialPostId) {
      setDetailModal({ isOpen: true, postId: initialPostId });
    }
  }, [initialPostId]);

  useEffect(() => {
    if (!toastMessage) return undefined;
    const timer = setTimeout(() => setToastMessage(null), 3500);
    return () => clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    getPostGroups()
      .then((list) => setGroups(Array.isArray(list) ? list : []))
      .catch(() => {
        // Không chặn trang nếu danh sách nhóm phụ trợ lỗi
      });
  }, []);

  const fetchPosts = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setPostLoading(true);
    setPostError(null);
    try {
      const result = await searchModerationPosts({
        ...appliedPostFilters,
        page: postPage,
        size: 20,
      });
      setPostData(result);
    } catch (err) {
      setPostError(err);
    } finally {
      setPostLoading(false);
      setRefreshing(false);
    }
  }, [appliedPostFilters, postPage]);

  const fetchLogs = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLogLoading(true);
    setLogError(null);
    try {
      const result = await searchModerationLogs({
        ...appliedLogFilters,
        page: logPage,
        size: 20,
      });
      setLogData(result);
    } catch (err) {
      setLogError(err);
    } finally {
      setLogLoading(false);
      setRefreshing(false);
    }
  }, [appliedLogFilters, logPage]);

  useEffect(() => {
    if (activeTab === 'posts') {
      fetchPosts();
    } else {
      fetchLogs();
    }
  }, [activeTab, fetchPosts, fetchLogs]);

  const handleApplyPostFilters = (e) => {
    e.preventDefault();
    if (!isValidDateRange(createdFrom, createdTo)) {
      setPostFilterError(t('postModeration.filter.invalidDateRange'));
      return;
    }
    setPostFilterError('');
    setPageResetAndApplyPosts({
      keyword,
      status,
      groupId,
      userId,
      createdFrom,
      createdTo,
      sort,
    });
  };

  const setPageResetAndApplyPosts = (nextFilters) => {
    setPageForPosts(nextFilters);
  };

  const setPageForPosts = (nextFilters) => {
    setPostPage(1);
    setAppliedPostFilters(nextFilters);
  };

  const handleResetPostFilters = () => {
    setKeyword('');
    setStatus(DEFAULT_POST_STATUS);
    setGroupId('');
    setUserId('');
    setCreatedFrom('');
    setCreatedTo('');
    setSort('createdAt,desc');
    setPostFilterError('');
    setPageForPosts({
      keyword: '',
      status: DEFAULT_POST_STATUS,
      groupId: '',
      userId: '',
      createdFrom: '',
      createdTo: '',
      sort: 'createdAt,desc',
    });
  };

  const handleApplyLogFilters = (e) => {
    e.preventDefault();
    if (!isValidDateRange(logCreatedFrom, logCreatedTo)) {
      setLogFilterError(t('postModeration.filter.invalidDateRange'));
      return;
    }
    setLogFilterError('');
    setLogPage(1);
    setAppliedLogFilters({
      postId: logPostId,
      actorType: logActorType,
      action: logAction,
      aiLabel: logAiLabel,
      createdFrom: logCreatedFrom,
      createdTo: logCreatedTo,
      sort: logSort,
    });
  };

  const handleResetLogFilters = () => {
    setLogPostId('');
    setLogActorType('');
    setLogAction('');
    setLogAiLabel('');
    setLogCreatedFrom('');
    setLogCreatedTo('');
    setLogSort('createdAt,desc');
    setLogFilterError('');
    setLogPage(1);
    setAppliedLogFilters({
      postId: '',
      actorType: '',
      action: '',
      aiLabel: '',
      createdFrom: '',
      createdTo: '',
      sort: 'createdAt,desc',
    });
  };

  const handleDecisionSuccess = (mode) => {
    setDecisionModal({ isOpen: false, mode: 'approve', post: null });
    setDetailModal({ isOpen: false, postId: null });
    setToastMessage(
      mode === 'reject'
        ? t('postModeration.toast.rejected')
        : t('postModeration.toast.approved')
    );
    fetchPosts(true);
  };

  const postColumns = useMemo(() => [
    {
      key: 'id',
      dataIndex: 'id',
      title: 'ID',
      width: '70px',
      render: (val) => `#${val}`,
    },
    {
      key: 'title',
      title: t('postModeration.columns.post'),
      render: (_, record) => (
        <div className={styles.postTitleCell}>
          <span className={styles.postTitleText} title={record.title}>
            {record.title}
          </span>
          <span className={styles.postSnippet} title={record.content}>
            {record.content}
          </span>
        </div>
      ),
    },
    {
      key: 'author',
      title: t('postModeration.columns.author'),
      render: (_, record) => (
        <div className={styles.authorCell}>
          <span className={styles.authorName}>{record.author?.username || '—'}</span>
          <span className={styles.mutedMeta}>#{record.author?.id || '—'}</span>
        </div>
      ),
    },
    {
      key: 'groupName',
      dataIndex: 'groupName',
      title: t('postModeration.columns.group'),
    },
    {
      key: 'status',
      dataIndex: 'status',
      title: t('postModeration.columns.status'),
      render: (val) => (
        <span className={`${styles.badge} ${STATUS_BADGE_MAP[val] || styles.badgeMuted}`}>
          {t(`postModeration.status.${val}`, val)}
        </span>
      ),
    },
    {
      key: 'latestModeration',
      title: t('postModeration.columns.aiResult'),
      render: (_, record) => {
        const latest = record.latestModeration;
        if (!latest) return '—';
        return (
          <div className={styles.aiCell}>
            <span className={`${styles.badge} ${ACTION_BADGE_MAP[latest.action] || styles.badgeMuted}`}>
              {t(`postModeration.action.${latest.action}`, latest.action)}
            </span>
            {latest.aiLabel && (
              <span className={styles.mutedMeta}>
                {latest.aiLabel}
                {latest.aiScore !== null && latest.aiScore !== undefined
                  ? ` (${Number(latest.aiScore).toFixed(2)})`
                  : ''}
              </span>
            )}
          </div>
        );
      },
    },
    {
      key: 'createdAt',
      dataIndex: 'createdAt',
      title: t('postModeration.columns.createdAt'),
      render: (val) => formatDateTime(val, locale),
    },
    {
      key: 'actions',
      title: t('postModeration.columns.actions'),
      align: 'right',
      render: (_, record) => {
        const canModerate = record.status === 'PENDING' || record.status === 'NEEDS_REVIEW';
        return (
          <div className={styles.actionGroup}>
            <button
              type="button"
              className={styles.iconBtn}
              title={t('postModeration.actions.view')}
              aria-label={t('postModeration.actions.view')}
              onClick={() => setDetailModal({ isOpen: true, postId: record.id })}
            >
              <EyeIcon size={15} />
            </button>
            {canModerate && (
              <>
                <button
                  type="button"
                  className={`${styles.iconBtn} ${styles.iconBtnApprove}`}
                  title={t('postModeration.actions.approve')}
                  aria-label={t('postModeration.actions.approve')}
                  onClick={() => setDecisionModal({ isOpen: true, mode: 'approve', post: record })}
                >
                  <CheckCircleIcon size={15} />
                </button>
                <button
                  type="button"
                  className={`${styles.iconBtn} ${styles.iconBtnReject}`}
                  title={t('postModeration.actions.reject')}
                  aria-label={t('postModeration.actions.reject')}
                  onClick={() => setDecisionModal({ isOpen: true, mode: 'reject', post: record })}
                >
                  <XCircleIcon size={15} />
                </button>
              </>
            )}
          </div>
        );
      },
    },
  ], [t, locale]);

  const logColumns = useMemo(() => [
    {
      key: 'id',
      dataIndex: 'id',
      title: 'ID',
      width: '70px',
      render: (val) => `#${val}`,
    },
    {
      key: 'postId',
      dataIndex: 'postId',
      title: t('postModeration.logs.columns.postId'),
      render: (val) => (
        <button
          type="button"
          className={styles.linkBtn}
          onClick={() => setDetailModal({ isOpen: true, postId: val })}
        >
          #{val}
        </button>
      ),
    },
    {
      key: 'actorType',
      title: t('postModeration.logs.columns.actor'),
      render: (_, record) => (
        <div className={styles.authorCell}>
          <span className={styles.authorName}>
            {t(`postModeration.actorType.${record.actorType}`, record.actorType)}
          </span>
          {record.actorUsername && (
            <span className={styles.mutedMeta}>{record.actorUsername}</span>
          )}
        </div>
      ),
    },
    {
      key: 'action',
      dataIndex: 'action',
      title: t('postModeration.logs.columns.action'),
      render: (val) => (
        <span className={`${styles.badge} ${ACTION_BADGE_MAP[val] || styles.badgeMuted}`}>
          {t(`postModeration.action.${val}`, val)}
        </span>
      ),
    },
    {
      key: 'aiLabel',
      title: t('postModeration.logs.columns.aiLabel'),
      render: (_, record) => (
        record.aiLabel ? (
          <div className={styles.authorCell}>
            <span className={styles.authorName}>{record.aiLabel}</span>
            {record.aiScore !== null && record.aiScore !== undefined && (
              <span className={styles.mutedMeta}>{Number(record.aiScore).toFixed(4)}</span>
            )}
          </div>
        ) : '—'
      ),
    },
    {
      key: 'note',
      dataIndex: 'note',
      title: t('postModeration.logs.columns.note'),
      render: (val) => val || '—',
    },
    {
      key: 'createdAt',
      dataIndex: 'createdAt',
      title: t('postModeration.columns.createdAt'),
      render: (val) => formatDateTime(val, locale),
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
            aria-label={t('postModeration.detail.close')}
          >
            ×
          </button>
        </div>
      )}

      <div className={styles.pageHeader}>
        <div className={styles.headerInfo}>
          <div className={styles.titleRow}>
            <MessageSquareIcon size={22} className={styles.headerIcon} />
            <h1 className={styles.title}>{t('postModeration.title')}</h1>
          </div>
          <p className={styles.subtitle}>{t('postModeration.subtitle')}</p>
        </div>
        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => (activeTab === 'posts' ? fetchPosts(true) : fetchLogs(true))}
            disabled={refreshing || postLoading || logLoading}
          >
            <RefreshIcon size={15} className={refreshing ? styles.spinning : ''} />
            <span>{t('postModeration.refresh')}</span>
          </button>
        </div>
      </div>

      <div className={styles.tabBar} role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'posts'}
          className={`${styles.tabBtn} ${activeTab === 'posts' ? styles.tabBtnActive : ''}`}
          onClick={() => setActiveTab('posts')}
        >
          <MessageSquareIcon size={15} />
          <span>{t('postModeration.tabs.posts')}</span>
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'logs'}
          className={`${styles.tabBtn} ${activeTab === 'logs' ? styles.tabBtnActive : ''}`}
          onClick={() => setActiveTab('logs')}
        >
          <ActivityIcon size={15} />
          <span>{t('postModeration.tabs.logs')}</span>
        </button>
      </div>

      {activeTab === 'posts' ? (
        <>
          <PostModerationFilterBar
            keyword={keyword}
            onKeywordChange={setKeyword}
            status={status}
            onStatusChange={setStatus}
            groupId={groupId}
            onGroupIdChange={setGroupId}
            groups={groups}
            userId={userId}
            onUserIdChange={setUserId}
            createdFrom={createdFrom}
            onCreatedFromChange={setCreatedFrom}
            createdTo={createdTo}
            onCreatedToChange={setCreatedTo}
            sort={sort}
            onSortChange={setSort}
            filterError={postFilterError}
            onApply={handleApplyPostFilters}
            onReset={handleResetPostFilters}
          />

          {postError ? (
            <AdminErrorState onRetry={() => fetchPosts()} />
          ) : (
            <div className={styles.tableSection}>
              <DataTable
                columns={postColumns}
                data={postData.content}
                isLoading={postLoading}
                emptyMessage={t('postModeration.emptyPosts')}
              />
              <AdminPagination
                page={postPage}
                totalPages={postData.totalPages}
                totalElements={postData.totalElements}
                isLast={Boolean(postData.isLast ?? postData.last)}
                onPageChange={setPostPage}
                unitLabel={t('postModeration.postsCountSuffix')}
                isLoading={postLoading}
              />
            </div>
          )}
        </>
      ) : (
        <>
          <ModerationLogFilterBar
            postId={logPostId}
            onPostIdChange={setLogPostId}
            actorType={logActorType}
            onActorTypeChange={setLogActorType}
            action={logAction}
            onActionChange={setLogAction}
            aiLabel={logAiLabel}
            onAiLabelChange={setLogAiLabel}
            createdFrom={logCreatedFrom}
            onCreatedFromChange={setLogCreatedFrom}
            createdTo={logCreatedTo}
            onCreatedToChange={setLogCreatedTo}
            sort={logSort}
            onSortChange={setLogSort}
            filterError={logFilterError}
            onApply={handleApplyLogFilters}
            onReset={handleResetLogFilters}
          />

          {logError ? (
            <AdminErrorState onRetry={() => fetchLogs()} />
          ) : (
            <div className={styles.tableSection}>
              <DataTable
                columns={logColumns}
                data={logData.content}
                isLoading={logLoading}
                emptyMessage={t('postModeration.emptyLogs')}
              />
              <AdminPagination
                page={logPage}
                totalPages={logData.totalPages}
                totalElements={logData.totalElements}
                isLast={Boolean(logData.isLast ?? logData.last)}
                onPageChange={setLogPage}
                unitLabel={t('postModeration.logsCountSuffix')}
                isLoading={logLoading}
              />
            </div>
          )}
        </>
      )}

      <PostModerationDetailModal
        isOpen={detailModal.isOpen}
        postId={detailModal.postId}
        onClose={() => setDetailModal({ isOpen: false, postId: null })}
        onOpenDecision={(mode, post) => setDecisionModal({ isOpen: true, mode, post })}
      />

      <ModerationDecisionModal
        isOpen={decisionModal.isOpen}
        mode={decisionModal.mode}
        post={decisionModal.post}
        onClose={() => setDecisionModal({ isOpen: false, mode: 'approve', post: null })}
        onSuccess={handleDecisionSuccess}
      />
    </div>
  );
}

PostModerationPage.propTypes = {
  initialPostId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  onNavigate: PropTypes.func,
};
