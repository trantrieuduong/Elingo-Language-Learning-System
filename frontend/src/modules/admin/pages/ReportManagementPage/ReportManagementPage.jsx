import React, { useCallback, useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  AlertTriangleIcon,
  CheckCircleIcon,
  ExternalLinkIcon,
  EyeIcon,
  RefreshIcon,
  XCircleIcon,
} from '../../components/icons/AdminIcons';
import { searchReports } from '../../services/adminCommunityApi';
import ReportFilterBar from './components/ReportFilterBar';
import ReportDetailModal from './components/ReportDetailModal';
import ReportDecisionModal from './components/ReportDecisionModal';
import styles from './ReportManagementPage.module.css';

const DEFAULT_REPORT_STATUS = 'PENDING';

const STATUS_BADGE_MAP = {
  PENDING: styles.badgeAmber,
  RESOLVED: styles.badgeGreen,
  REJECTED: styles.badgeRed,
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

const getTargetUrl = (targetType, targetId) => {
  if (!targetId) return null;
  switch (targetType) {
    case 'POST':
      return `/community/posts/${targetId}`;
    case 'DECK':
      return `/admin/decks/${targetId}`;
    case 'USER':
      return `/profile/${targetId}`;
    case 'LESSON':
      return `/admin/lessons/${targetId}`;
    default:
      return null;
  }
};

export default function ReportManagementPage() {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  // Filter state (mặc định PENDING)
  const [status, setStatus] = useState(DEFAULT_REPORT_STATUS);
  const [targetType, setTargetType] = useState('');
  const [reporterId, setReporterId] = useState('');
  const [targetId, setTargetId] = useState('');
  const [createdFrom, setCreatedFrom] = useState('');
  const [createdTo, setCreatedTo] = useState('');
  const [sort, setSort] = useState('createdAt,desc');
  const [filterError, setFilterError] = useState('');

  const [appliedFilters, setAppliedFilters] = useState({
    status: DEFAULT_REPORT_STATUS,
    targetType: '',
    reporterId: '',
    targetId: '',
    createdFrom: '',
    createdTo: '',
    sort: 'createdAt,desc',
  });

  const [page, setPage] = useState(1);
  const pageSize = 20;

  const [reportData, setReportData] = useState({
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

  const [detailModal, setDetailModal] = useState({ isOpen: false, reportId: null });
  const [decisionModal, setDecisionModal] = useState({ isOpen: false, mode: 'resolve', report: null });

  useEffect(() => {
    if (!toastMessage) return undefined;
    const timer = setTimeout(() => setToastMessage(null), 3500);
    return () => clearTimeout(timer);
  }, [toastMessage]);

  const fetchReports = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    try {
      const result = await searchReports({
        ...appliedFilters,
        page,
        size: pageSize,
      });
      setReportData(result);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [appliedFilters, page]);

  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  const handleApplyFilters = (e) => {
    e.preventDefault();
    if (!isValidDateRange(createdFrom, createdTo)) {
      setFilterError(t('reports.filter.invalidDateRange'));
      return;
    }
    setFilterError('');
    setPage(1);
    setAppliedFilters({
      status,
      targetType,
      reporterId,
      targetId,
      createdFrom,
      createdTo,
      sort,
    });
  };

  const handleResetFilters = () => {
    setStatus(DEFAULT_REPORT_STATUS);
    setTargetType('');
    setReporterId('');
    setTargetId('');
    setCreatedFrom('');
    setCreatedTo('');
    setSort('createdAt,desc');
    setFilterError('');
    setPage(1);
    setAppliedFilters({
      status: DEFAULT_REPORT_STATUS,
      targetType: '',
      reporterId: '',
      targetId: '',
      createdFrom: '',
      createdTo: '',
      sort: 'createdAt,desc',
    });
  };

  const handleDecisionSuccess = (mode) => {
    setDecisionModal({ isOpen: false, mode: 'resolve', report: null });
    setDetailModal({ isOpen: false, reportId: null });
    setToastMessage(
      mode === 'reject'
        ? t('reports.toast.rejected')
        : t('reports.toast.resolved')
    );
    fetchReports(true);
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
      key: 'target',
      title: t('reports.columns.target'),
      render: (_, record) => {
        const targetUrl = getTargetUrl(record.targetType, record.targetId);
        return (
          <div className={styles.targetCell}>
            <span className={`${styles.badge} ${styles.badgeBlue}`}>
              {t(`reports.targetType.${record.targetType}`, record.targetType)}
            </span>
            {targetUrl ? (
              <a
                href={targetUrl}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.targetLink}
                title={t('reports.actions.openTarget')}
              >
                <span>#{record.targetId}</span>
                <ExternalLinkIcon size={12} />
              </a>
            ) : (
              <span className={styles.mutedMeta}>#{record.targetId}</span>
            )}
          </div>
        );
      },
    },
    {
      key: 'description',
      dataIndex: 'description',
      title: t('reports.columns.description'),
      render: (val) => (
        <div className={styles.descCell} title={val || ''}>
          {val || '—'}
        </div>
      ),
    },
    {
      key: 'reporter',
      title: t('reports.columns.reporter'),
      render: (_, record) => (
        <div className={styles.userCell}>
          <span className={styles.userName}>{record.reporterUsername || '—'}</span>
          <span className={styles.mutedMeta}>#{record.reporterId || '—'}</span>
        </div>
      ),
    },
    {
      key: 'evidenceCount',
      title: t('reports.columns.evidence'),
      render: (_, record) => (
        <span className={`${styles.badge} ${styles.badgeMuted}`}>
          {Array.isArray(record.imageUrls) ? record.imageUrls.length : 0}
        </span>
      ),
    },
    {
      key: 'status',
      dataIndex: 'status',
      title: t('reports.columns.status'),
      render: (val) => (
        <span className={`${styles.badge} ${STATUS_BADGE_MAP[val] || styles.badgeMuted}`}>
          {t(`reports.status.${val}`, val)}
        </span>
      ),
    },
    {
      key: 'createdAt',
      dataIndex: 'createdAt',
      title: t('reports.columns.createdAt'),
      render: (val) => formatDateTime(val, locale),
    },
    {
      key: 'actions',
      title: t('reports.columns.actions'),
      align: 'right',
      render: (_, record) => {
        const isPending = record.status === 'PENDING';
        return (
          <div className={styles.actionGroup}>
            <button
              type="button"
              className={styles.iconBtn}
              title={t('reports.actions.view')}
              aria-label={t('reports.actions.view')}
              onClick={() => setDetailModal({ isOpen: true, reportId: record.id })}
            >
              <EyeIcon size={15} />
            </button>
            {isPending && (
              <>
                <button
                  type="button"
                  className={`${styles.iconBtn} ${styles.iconBtnResolve}`}
                  title={t('reports.actions.resolve')}
                  aria-label={t('reports.actions.resolve')}
                  onClick={() => setDecisionModal({ isOpen: true, mode: 'resolve', report: record })}
                >
                  <CheckCircleIcon size={15} />
                </button>
                <button
                  type="button"
                  className={`${styles.iconBtn} ${styles.iconBtnReject}`}
                  title={t('reports.actions.reject')}
                  aria-label={t('reports.actions.reject')}
                  onClick={() => setDecisionModal({ isOpen: true, mode: 'reject', report: record })}
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
            aria-label={t('reports.detail.close')}
          >
            ×
          </button>
        </div>
      )}

      <div className={styles.pageHeader}>
        <div className={styles.headerInfo}>
          <div className={styles.titleRow}>
            <AlertTriangleIcon size={22} className={styles.headerIcon} />
            <h1 className={styles.title}>{t('reports.title')}</h1>
          </div>
          <p className={styles.subtitle}>{t('reports.subtitle')}</p>
        </div>
        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => fetchReports(true)}
            disabled={refreshing || loading}
          >
            <RefreshIcon size={15} className={refreshing ? styles.spinning : ''} />
            <span>{t('reports.refresh')}</span>
          </button>
        </div>
      </div>

      <ReportFilterBar
        status={status}
        onStatusChange={setStatus}
        targetType={targetType}
        onTargetTypeChange={setTargetType}
        reporterId={reporterId}
        onReporterIdChange={setReporterId}
        targetId={targetId}
        onTargetIdChange={setTargetId}
        createdFrom={createdFrom}
        onCreatedFromChange={setCreatedFrom}
        createdTo={createdTo}
        onCreatedToChange={setCreatedTo}
        sort={sort}
        onSortChange={setSort}
        filterError={filterError}
        onApply={handleApplyFilters}
        onReset={handleResetFilters}
      />

      {error ? (
        <AdminErrorState onRetry={() => fetchReports()} />
      ) : (
        <div className={styles.tableSection}>
          <DataTable
            columns={columns}
            data={reportData.content}
            isLoading={loading}
            emptyMessage={t('reports.emptyList')}
          />
          <AdminPagination
            page={page}
            totalPages={reportData.totalPages}
            totalElements={reportData.totalElements}
            isLast={Boolean(reportData.isLast ?? reportData.last)}
            onPageChange={setPage}
            unitLabel={t('reports.reportsCountSuffix')}
            isLoading={loading}
          />
        </div>
      )}

      <ReportDetailModal
        isOpen={detailModal.isOpen}
        reportId={detailModal.reportId}
        onClose={() => setDetailModal({ isOpen: false, reportId: null })}
        onOpenDecision={(mode, report) => setDecisionModal({ isOpen: true, mode, report })}
      />

      <ReportDecisionModal
        isOpen={decisionModal.isOpen}
        mode={decisionModal.mode}
        report={decisionModal.report}
        onClose={() => setDecisionModal({ isOpen: false, mode: 'resolve', report: null })}
        onSuccess={handleDecisionSuccess}
      />
    </div>
  );
}

ReportManagementPage.propTypes = {
  onNavigate: PropTypes.func,
};
