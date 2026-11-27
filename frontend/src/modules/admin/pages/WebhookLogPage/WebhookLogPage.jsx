import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  ActivityIcon,
  RefreshIcon,
  EyeIcon,
  CheckCircleIcon,
  XCircleIcon,
  ClockIcon,
  ShieldCheckIcon,
} from '../../components/icons/AdminIcons';
import { searchWebhookLogs } from '../../services/adminPremiumApi';
import WebhookLogFilterBar from './components/WebhookLogFilterBar';
import WebhookLogDetailModal from './components/WebhookLogDetailModal';
import styles from './WebhookLogPage.module.css';

const formatDateTime = (isoString, locale = 'vi-VN') => {
  if (!isoString) return '—';
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return isoString;
  return date.toLocaleString(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

const isValidDateRange = (fromStr, toStr) => {
  if (!fromStr || !toStr) return true;
  const from = new Date(fromStr);
  const to = new Date(toStr);
  if (from > to) return false;
  const diffDays = (to - from) / (1000 * 60 * 60 * 24);
  return diffDays <= 366;
};

export default function WebhookLogPage({ initialTransactionId = '', onNavigate: _onNavigate }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  const [providerId, setProviderId] = useState('');
  const [transactionId, setTransactionId] = useState(initialTransactionId);
  const [status, setStatus] = useState('');
  const [signatureValid, setSignatureValid] = useState('');
  const [receivedFrom, setReceivedFrom] = useState('');
  const [receivedTo, setReceivedTo] = useState('');
  const [sort, setSort] = useState('receivedAt,desc');
  const [filterError, setFilterError] = useState('');

  const [appliedFilters, setAppliedFilters] = useState({
    providerId: '',
    transactionId: initialTransactionId,
    status: '',
    signatureValid: '',
    receivedFrom: '',
    receivedTo: '',
    sort: 'receivedAt,desc',
  });

  // Đồng bộ khi chuyển từ chi tiết giao dịch sang với query ?transactionId=...
  useEffect(() => {
    setTransactionId(initialTransactionId);
    setPage(1);
    setAppliedFilters((prev) => ({
      ...prev,
      transactionId: initialTransactionId,
    }));
  }, [initialTransactionId]);

  const [page, setPage] = useState(1);
  const pageSize = 20;

  const [logData, setLogData] = useState({
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

  const [detailModal, setDetailModal] = useState({ isOpen: false, logId: null });

  const loadLogs = useCallback(
    async (isManualRefresh = false) => {
      try {
        if (isManualRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }
        setError(null);

        const res = await searchWebhookLogs({
          ...appliedFilters,
          page,
          size: pageSize,
        });
        setLogData(res);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [appliedFilters, page]
  );

  useEffect(() => {
    loadLogs(false);
  }, [loadLogs]);

  const handleApplyFilters = (e) => {
    if (e) e.preventDefault();
    if (!isValidDateRange(receivedFrom, receivedTo)) {
      setFilterError(t('premiumWebhookLogs.errors.invalidDateRange'));
      return;
    }
    setFilterError('');
    setPage(1);
    setAppliedFilters({
      providerId,
      transactionId,
      status,
      signatureValid,
      receivedFrom,
      receivedTo,
      sort,
    });
  };

  const handleResetFilters = () => {
    setProviderId('');
    setTransactionId('');
    setStatus('');
    setSignatureValid('');
    setReceivedFrom('');
    setReceivedTo('');
    setSort('receivedAt,desc');
    setFilterError('');
    setPage(1);
    setAppliedFilters({
      providerId: '',
      transactionId: '',
      status: '',
      signatureValid: '',
      receivedFrom: '',
      receivedTo: '',
      sort: 'receivedAt,desc',
    });
  };

  const renderStatusBadge = useCallback(
    (logStatus) => {
      const statusMap = {
        PROCESSED: { cls: styles.badgeGreen, icon: <CheckCircleIcon size={13} /> },
        RECEIVED: { cls: styles.badgeBlue, icon: <ClockIcon size={13} /> },
        IGNORED: { cls: styles.badgeAmber, icon: <ClockIcon size={13} /> },
        FAILED: { cls: styles.badgeRed, icon: <XCircleIcon size={13} /> },
      };
      const cfg = statusMap[logStatus] || { cls: styles.badgeBlue, icon: null };
      return (
        <span className={`${styles.statusBadge} ${cfg.cls}`}>
          {cfg.icon}
          <span>{t(`premiumWebhookLogs.status.${logStatus}`, logStatus)}</span>
        </span>
      );
    },
    [t]
  );

  const columns = useMemo(
    () => [
      {
        key: 'id',
        dataIndex: 'id',
        title: t('premiumWebhookLogs.columns.id'),
        width: '68px',
        render: (val) => <span className={styles.monoText}>#{val}</span>,
      },
      {
        key: 'providerCode',
        dataIndex: 'providerCode',
        title: t('premiumWebhookLogs.columns.provider'),
        render: (val, record) => (
          <div className={styles.providerCell}>
            <span className={styles.providerTag}>{val || '—'}</span>
            {record.providerId && <span className={styles.mutedText}>#{record.providerId}</span>}
          </div>
        ),
      },
      {
        key: 'transactionId',
        dataIndex: 'transactionId',
        title: t('premiumWebhookLogs.columns.transactionId'),
        render: (val) =>
          val ? <span className={styles.monoText}>#{val}</span> : <span className={styles.mutedText}>—</span>,
      },
      {
        key: 'eventId',
        dataIndex: 'eventId',
        title: t('premiumWebhookLogs.columns.eventId'),
        render: (val) => <span className={styles.eventId}>{val || '—'}</span>,
      },
      {
        key: 'signatureValid',
        dataIndex: 'signatureValid',
        title: t('premiumWebhookLogs.columns.signature'),
        render: (val) => (
          <span className={`${styles.statusBadge} ${val ? styles.badgeGreen : styles.badgeRed}`}>
            {val ? <ShieldCheckIcon size={13} /> : <XCircleIcon size={13} />}
            <span>
              {val
                ? t('premiumWebhookLogs.signature.valid')
                : t('premiumWebhookLogs.signature.invalid')}
            </span>
          </span>
        ),
      },
      {
        key: 'status',
        dataIndex: 'status',
        title: t('premiumWebhookLogs.columns.status'),
        render: (val) => renderStatusBadge(val),
      },
      {
        key: 'receivedAt',
        dataIndex: 'receivedAt',
        title: t('premiumWebhookLogs.columns.receivedAt'),
        render: (val) => <span className={styles.dateText}>{formatDateTime(val, locale)}</span>,
      },
      {
        key: 'processedAt',
        dataIndex: 'processedAt',
        title: t('premiumWebhookLogs.columns.processedAt'),
        render: (val) => <span className={styles.dateText}>{formatDateTime(val, locale)}</span>,
      },
      {
        key: 'actions',
        title: t('premiumWebhookLogs.columns.actions'),
        align: 'right',
        render: (_val, record) => (
          <button
            type="button"
            className={styles.actionBtn}
            onClick={() => setDetailModal({ isOpen: true, logId: record.id })}
            title={t('premiumWebhookLogs.actions.viewDetail')}
          >
            <EyeIcon size={15} />
            <span>{t('premiumWebhookLogs.actions.viewDetail')}</span>
          </button>
        ),
      },
    ],
    [locale, renderStatusBadge, t]
  );

  return (
    <div className={styles.pageContainer}>
      <div className={styles.pageHeader}>
        <div className={styles.headerInfo}>
          <div className={styles.titleRow}>
            <ActivityIcon size={22} className={styles.headerIcon} />
            <h1 className={styles.title}>{t('premiumWebhookLogs.title')}</h1>
          </div>
          <p className={styles.subtitle}>{t('premiumWebhookLogs.subtitle')}</p>
        </div>

        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => loadLogs(true)}
            disabled={loading || refreshing}
          >
            <RefreshIcon size={15} className={refreshing ? styles.spinning : ''} />
            <span>{t('premiumWebhookLogs.refreshBtn')}</span>
          </button>
        </div>
      </div>

      <WebhookLogFilterBar
        providerId={providerId}
        onProviderIdChange={setProviderId}
        transactionId={transactionId}
        onTransactionIdChange={setTransactionId}
        status={status}
        onStatusChange={setStatus}
        signatureValid={signatureValid}
        onSignatureValidChange={setSignatureValid}
        receivedFrom={receivedFrom}
        onReceivedFromChange={setReceivedFrom}
        receivedTo={receivedTo}
        onReceivedToChange={setReceivedTo}
        sort={sort}
        onSortChange={setSort}
        filterError={filterError}
        onApply={handleApplyFilters}
        onReset={handleResetFilters}
      />

      {error ? (
        <AdminErrorState onRetry={() => loadLogs(false)} />
      ) : (
        <div className={styles.tableCard}>
          <DataTable
            columns={columns}
            data={logData.content || []}
            isLoading={loading}
            emptyMessage={t('premiumWebhookLogs.emptyList')}
            borderless
          />
          <AdminPagination
            page={page}
            totalPages={logData.totalPages}
            totalElements={logData.totalElements}
            isLast={Boolean(logData.isLast ?? logData.last)}
            onPageChange={setPage}
            unitLabel={t('premiumWebhookLogs.unitLabel')}
            isLoading={loading}
          />
        </div>
      )}

      <WebhookLogDetailModal
        isOpen={detailModal.isOpen}
        logId={detailModal.logId}
        onClose={() => setDetailModal({ isOpen: false, logId: null })}
      />
    </div>
  );
}

WebhookLogPage.propTypes = {
  initialTransactionId: PropTypes.string,
  onNavigate: PropTypes.func,
};
