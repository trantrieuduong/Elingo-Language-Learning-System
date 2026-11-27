import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  CreditCardIcon,
  RefreshIcon,
  EyeIcon,
  CheckCircleIcon,
  XCircleIcon,
  ClockIcon,
  ArrowDownRightIcon,
} from '../../components/icons/AdminIcons';
import { getAllPlans, searchTransactions } from '../../services/adminPremiumApi';
import TransactionFilterBar from './components/TransactionFilterBar';
import TransactionDetailModal from './components/TransactionDetailModal';
import TransactionReasonModal from './components/TransactionReasonModal';
import styles from './TransactionManagementPage.module.css';

const formatAmount = (amount, currency = 'VND', locale = 'vi-VN') => {
  if (amount === undefined || amount === null) return '—';
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency || 'VND',
      maximumFractionDigits: currency === 'VND' ? 0 : 2,
    }).format(Number(amount));
  } catch {
    return `${Number(amount).toLocaleString(locale)} ${currency || ''}`.trim();
  }
};

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

export default function TransactionManagementPage({ onNavigate }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  // Filter draft & applied state
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [planId, setPlanId] = useState('');
  const [userId, setUserId] = useState('');
  const [createdFrom, setCreatedFrom] = useState('');
  const [createdTo, setCreatedTo] = useState('');
  const [sort, setSort] = useState('createdAt,desc');
  const [filterError, setFilterError] = useState('');

  const [appliedFilters, setAppliedFilters] = useState({
    keyword: '',
    status: '',
    planId: '',
    userId: '',
    createdFrom: '',
    createdTo: '',
    sort: 'createdAt,desc',
  });

  // 1-indexed pagination
  const [page, setPage] = useState(1);
  const pageSize = 20;

  // Data state
  const [plans, setPlans] = useState([]);
  const [txData, setTxData] = useState({
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
  const [detailModal, setDetailModal] = useState({ isOpen: false, transactionId: null });
  const [reasonModal, setReasonModal] = useState({
    isOpen: false,
    transaction: null,
    mode: 'confirm',
  });

  useEffect(() => {
    if (!toastMessage) return undefined;
    const timer = setTimeout(() => setToastMessage(null), 3500);
    return () => clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    getAllPlans()
      .then((list) => setPlans(Array.isArray(list) ? list : []))
      .catch(() => {
        // Không chặn trang giao dịch nếu danh sách gói phụ trợ lỗi
      });
  }, []);

  const loadTransactions = useCallback(
    async (isManualRefresh = false) => {
      try {
        if (isManualRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }
        setError(null);

        const res = await searchTransactions({
          ...appliedFilters,
          page,
          size: pageSize,
        });
        setTxData(res);
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
    loadTransactions(false);
  }, [loadTransactions]);

  const handleApplyFilters = (e) => {
    if (e) e.preventDefault();
    if (!isValidDateRange(createdFrom, createdTo)) {
      setFilterError(t('premiumTransactions.errors.invalidDateRange'));
      return;
    }
    setFilterError('');
    setPage(1);
    setAppliedFilters({
      keyword,
      status,
      planId,
      userId,
      createdFrom,
      createdTo,
      sort,
    });
  };

  const handleResetFilters = () => {
    setKeyword('');
    setStatus('');
    setPlanId('');
    setUserId('');
    setCreatedFrom('');
    setCreatedTo('');
    setSort('createdAt,desc');
    setFilterError('');
    setPage(1);
    setAppliedFilters({
      keyword: '',
      status: '',
      planId: '',
      userId: '',
      createdFrom: '',
      createdTo: '',
      sort: 'createdAt,desc',
    });
  };

  const handleActionSuccess = useCallback(
    (updatedDetail, mode) => {
      setTxData((prev) => ({
        ...prev,
        content: (prev.content || []).map((item) =>
          item.id === updatedDetail.id
            ? {
                ...item,
                status: updatedDetail.status,
                paidAt: updatedDetail.paidAt,
              }
            : item
        ),
      }));
      setToastMessage(
        t(
          mode === 'confirm'
            ? 'premiumTransactions.reasonModal.confirmSuccess'
            : 'premiumTransactions.reasonModal.refundSuccess'
        )
      );
    },
    [t]
  );

  const renderStatusBadge = useCallback(
    (txStatus) => {
      const statusMap = {
        SUCCESS: { cls: styles.badgeGreen, icon: <CheckCircleIcon size={13} /> },
        PENDING: { cls: styles.badgeAmber, icon: <ClockIcon size={13} /> },
        FAILED: { cls: styles.badgeRed, icon: <XCircleIcon size={13} /> },
        REFUNDED: { cls: styles.badgeBlue, icon: <ArrowDownRightIcon size={13} /> },
      };
      const cfg = statusMap[txStatus] || { cls: styles.badgeBlue, icon: null };
      return (
        <span className={`${styles.statusBadge} ${cfg.cls}`}>
          {cfg.icon}
          <span>{t(`premiumTransactions.status.${txStatus}`, txStatus)}</span>
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
        title: t('premiumTransactions.columns.id'),
        width: '68px',
        render: (val) => <span className={styles.monoText}>#{val}</span>,
      },
      {
        key: 'providerRefCode',
        dataIndex: 'providerRefCode',
        title: t('premiumTransactions.columns.refCode'),
        render: (val) => <span className={styles.refCode}>{val || '—'}</span>,
      },
      {
        key: 'user',
        dataIndex: 'username',
        title: t('premiumTransactions.columns.user'),
        render: (val, record) => (
          <div className={styles.userCell}>
            <span className={styles.userPrimary}>{val || `#${record.userId}`}</span>
            <span className={styles.userSecondary}>{record.userEmail || '—'}</span>
          </div>
        ),
      },
      {
        key: 'planName',
        dataIndex: 'planName',
        title: t('premiumTransactions.columns.plan'),
        render: (val) => <span className={styles.planText}>{val || '—'}</span>,
      },
      {
        key: 'providerCode',
        dataIndex: 'providerCode',
        title: t('premiumTransactions.columns.provider'),
        render: (val) =>
          val ? <span className={styles.providerTag}>{val}</span> : <span className={styles.mutedText}>—</span>,
      },
      {
        key: 'amount',
        dataIndex: 'amount',
        title: t('premiumTransactions.columns.amount'),
        align: 'right',
        render: (val, record) => (
          <span className={styles.amountText}>{formatAmount(val, record.currency, locale)}</span>
        ),
      },
      {
        key: 'status',
        dataIndex: 'status',
        title: t('premiumTransactions.columns.status'),
        render: (val) => renderStatusBadge(val),
      },
      {
        key: 'paidAt',
        dataIndex: 'paidAt',
        title: t('premiumTransactions.columns.paidAt'),
        render: (val) => <span className={styles.dateText}>{formatDateTime(val, locale)}</span>,
      },
      {
        key: 'createdAt',
        dataIndex: 'createdAt',
        title: t('premiumTransactions.columns.createdAt'),
        render: (val) => <span className={styles.dateText}>{formatDateTime(val, locale)}</span>,
      },
      {
        key: 'actions',
        title: t('premiumTransactions.columns.actions'),
        align: 'right',
        render: (_val, record) => (
          <div className={styles.actionGroup}>
            <button
              type="button"
              className={styles.actionBtn}
              onClick={() => setDetailModal({ isOpen: true, transactionId: record.id })}
              title={t('premiumTransactions.actions.viewDetail')}
              aria-label={t('premiumTransactions.actions.viewDetail')}
            >
              <EyeIcon size={15} />
            </button>
            {record.status === 'PENDING' && (
              <button
                type="button"
                className={`${styles.actionBtn} ${styles.successBtn}`}
                onClick={() =>
                  setReasonModal({ isOpen: true, transaction: record, mode: 'confirm' })
                }
                title={t('premiumTransactions.actions.confirm')}
              >
                <CheckCircleIcon size={15} />
                <span>{t('premiumTransactions.actions.confirm')}</span>
              </button>
            )}
            {record.status === 'SUCCESS' && (
              <button
                type="button"
                className={`${styles.actionBtn} ${styles.dangerBtn}`}
                onClick={() =>
                  setReasonModal({ isOpen: true, transaction: record, mode: 'refund' })
                }
                title={t('premiumTransactions.actions.refund')}
              >
                <ArrowDownRightIcon size={15} />
                <span>{t('premiumTransactions.actions.refund')}</span>
              </button>
            )}
          </div>
        ),
      },
    ],
    [locale, renderStatusBadge, t]
  );

  return (
    <div className={styles.pageContainer}>
      {toastMessage && (
        <div className={styles.toast} role="status" aria-live="polite">
          <CheckCircleIcon size={16} className={styles.toastIcon} />
          <span>{toastMessage}</span>
          <button
            type="button"
            className={styles.toastCloseBtn}
            onClick={() => setToastMessage(null)}
            aria-label={t('common.close')}
          >
            ×
          </button>
        </div>
      )}

      {/* Header */}
      <div className={styles.pageHeader}>
        <div className={styles.headerInfo}>
          <div className={styles.titleRow}>
            <CreditCardIcon size={22} className={styles.headerIcon} />
            <h1 className={styles.title}>{t('premiumTransactions.title')}</h1>
          </div>
          <p className={styles.subtitle}>{t('premiumTransactions.subtitle')}</p>
        </div>

        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => loadTransactions(true)}
            disabled={loading || refreshing}
          >
            <RefreshIcon size={15} className={refreshing ? styles.spinning : ''} />
            <span>{t('premiumTransactions.refreshBtn')}</span>
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <TransactionFilterBar
        keyword={keyword}
        onKeywordChange={setKeyword}
        status={status}
        onStatusChange={setStatus}
        planId={planId}
        onPlanIdChange={setPlanId}
        plans={plans}
        userId={userId}
        onUserIdChange={setUserId}
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

      {/* Table & Pagination */}
      {error ? (
        <AdminErrorState onRetry={() => loadTransactions(false)} />
      ) : (
        <div className={styles.tableCard}>
          <DataTable
            columns={columns}
            data={txData.content || []}
            isLoading={loading}
            emptyMessage={t('premiumTransactions.emptyList')}
            borderless
          />
          <AdminPagination
            page={page}
            totalPages={txData.totalPages}
            totalElements={txData.totalElements}
            isLast={Boolean(txData.isLast ?? txData.last)}
            onPageChange={setPage}
            unitLabel={t('premiumTransactions.unitLabel')}
            isLoading={loading}
          />
        </div>
      )}

      {/* Modals */}
      <TransactionDetailModal
        isOpen={detailModal.isOpen}
        transactionId={detailModal.transactionId}
        onClose={() => setDetailModal({ isOpen: false, transactionId: null })}
        onOpenAction={(tx, mode) => {
          setDetailModal({ isOpen: false, transactionId: null });
          setReasonModal({ isOpen: true, transaction: tx, mode });
        }}
        onViewWebhooks={(txId) => {
          setDetailModal({ isOpen: false, transactionId: null });
          if (onNavigate) {
            onNavigate(`/admin/webhook-logs?transactionId=${txId}`);
          }
        }}
      />

      <TransactionReasonModal
        isOpen={reasonModal.isOpen}
        transaction={reasonModal.transaction}
        mode={reasonModal.mode}
        onClose={() => setReasonModal({ isOpen: false, transaction: null, mode: 'confirm' })}
        onSuccess={handleActionSuccess}
      />
    </div>
  );
}

TransactionManagementPage.propTypes = {
  onNavigate: PropTypes.func,
};
