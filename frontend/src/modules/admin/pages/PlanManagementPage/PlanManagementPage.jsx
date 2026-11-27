import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  CrownIcon,
  RefreshIcon,
  PlusIcon,
  EditIcon,
  CheckCircleIcon,
  XCircleIcon,
  SearchIcon,
} from '../../components/icons/AdminIcons';
import { getAllPlans } from '../../services/adminPremiumApi';
import PlanFormModal from './components/PlanFormModal';
import PlanStatusModal from './components/PlanStatusModal';
import styles from './PlanManagementPage.module.css';

const formatPrice = (price, currency = 'VND', locale = 'vi-VN') => {
  if (price === undefined || price === null) return '—';
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency || 'VND',
      maximumFractionDigits: currency === 'VND' ? 0 : 2,
    }).format(Number(price));
  } catch {
    return `${Number(price).toLocaleString(locale)} ${currency || ''}`.trim();
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

export default function PlanManagementPage({ onNavigate: _onNavigate }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Client-side filter (controller GET /admin/premium/plans trả toàn bộ danh sách)
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [toastMessage, setToastMessage] = useState(null);
  const [formModal, setFormModal] = useState({ isOpen: false, plan: null });
  const [statusModal, setStatusModal] = useState({ isOpen: false, plan: null, targetAction: 'enable' });

  const showToast = useCallback((msg) => {
    setToastMessage(msg);
  }, []);

  useEffect(() => {
    if (!toastMessage) return undefined;
    const timer = setTimeout(() => setToastMessage(null), 3500);
    return () => clearTimeout(timer);
  }, [toastMessage]);

  const loadPlans = useCallback(async (isManualRefresh = false) => {
    try {
      if (isManualRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);
      const data = await getAllPlans();
      setPlans(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadPlans(false);
  }, [loadPlans]);

  const summary = useMemo(() => {
    const total = plans.length;
    const active = plans.filter((p) => Boolean(p.isActive)).length;
    return {
      total,
      active,
      inactive: total - active,
    };
  }, [plans]);

  const filteredPlans = useMemo(() => {
    const kw = searchTerm.trim().toLowerCase();
    return plans.filter((plan) => {
      if (statusFilter === 'ACTIVE' && !plan.isActive) return false;
      if (statusFilter === 'INACTIVE' && plan.isActive) return false;
      if (!kw) return true;
      const nameMatch = (plan.name || '').toLowerCase().includes(kw);
      const currencyMatch = (plan.currency || '').toLowerCase().includes(kw);
      return nameMatch || currencyMatch;
    });
  }, [plans, searchTerm, statusFilter]);

  const handlePlanSaved = useCallback(
    (savedPlan, isEdit) => {
      setPlans((prev) => {
        if (isEdit) {
          return prev.map((p) => (p.id === savedPlan.id ? savedPlan : p));
        }
        return [savedPlan, ...prev];
      });
      showToast(t(isEdit ? 'premiumPlans.formModal.editSuccess' : 'premiumPlans.formModal.createSuccess'));
    },
    [showToast, t]
  );

  const handlePlanToggled = useCallback(
    (updatedPlan, action) => {
      setPlans((prev) => prev.map((p) => (p.id === updatedPlan.id ? updatedPlan : p)));
      showToast(
        t(
          action === 'enable'
            ? 'premiumPlans.statusModal.enableSuccess'
            : 'premiumPlans.statusModal.disableSuccess'
        )
      );
    },
    [showToast, t]
  );

  const columns = useMemo(
    () => [
      {
        key: 'id',
        dataIndex: 'id',
        title: t('premiumPlans.columns.id'),
        width: '72px',
        render: (val) => <span className={styles.monoText}>#{val}</span>,
      },
      {
        key: 'name',
        dataIndex: 'name',
        title: t('premiumPlans.columns.name'),
        render: (val, record) => (
          <div className={styles.planNameCell}>
            <span className={styles.planName}>{val}</span>
            <span className={styles.currencyTag}>{record.currency}</span>
          </div>
        ),
      },
      {
        key: 'price',
        dataIndex: 'price',
        title: t('premiumPlans.columns.price'),
        align: 'right',
        render: (val, record) => (
          <span className={styles.priceText}>{formatPrice(val, record.currency, locale)}</span>
        ),
      },
      {
        key: 'durationDays',
        dataIndex: 'durationDays',
        title: t('premiumPlans.columns.duration'),
        render: (val) => (
          <span className={styles.durationText}>
            {val} {t('premiumPlans.daysUnit')}
          </span>
        ),
      },
      {
        key: 'isActive',
        dataIndex: 'isActive',
        title: t('premiumPlans.columns.status'),
        render: (val) => (
          <span className={`${styles.statusBadge} ${val ? styles.badgeGreen : styles.badgeRed}`}>
            {val ? <CheckCircleIcon size={13} /> : <XCircleIcon size={13} />}
            <span>{val ? t('premiumPlans.activeStatus') : t('premiumPlans.inactiveStatus')}</span>
          </span>
        ),
      },
      {
        key: 'updatedAt',
        dataIndex: 'updatedAt',
        title: t('premiumPlans.columns.updatedAt'),
        render: (val) => <span className={styles.dateText}>{formatDateTime(val, locale)}</span>,
      },
      {
        key: 'actions',
        title: t('premiumPlans.columns.actions'),
        align: 'right',
        render: (_val, record) => (
          <div className={styles.actionGroup}>
            <button
              type="button"
              className={styles.actionBtn}
              onClick={() => setFormModal({ isOpen: true, plan: record })}
              title={t('premiumPlans.actions.edit')}
            >
              <EditIcon size={15} />
              <span>{t('premiumPlans.actions.edit')}</span>
            </button>
            {record.isActive ? (
              <button
                type="button"
                className={`${styles.actionBtn} ${styles.dangerBtn}`}
                onClick={() =>
                  setStatusModal({ isOpen: true, plan: record, targetAction: 'disable' })
                }
                title={t('premiumPlans.actions.disable')}
              >
                <XCircleIcon size={15} />
                <span>{t('premiumPlans.actions.disable')}</span>
              </button>
            ) : (
              <button
                type="button"
                className={`${styles.actionBtn} ${styles.successBtn}`}
                onClick={() =>
                  setStatusModal({ isOpen: true, plan: record, targetAction: 'enable' })
                }
                title={t('premiumPlans.actions.enable')}
              >
                <CheckCircleIcon size={15} />
                <span>{t('premiumPlans.actions.enable')}</span>
              </button>
            )}
          </div>
        ),
      },
    ],
    [locale, t]
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
            <CrownIcon size={22} className={styles.headerIcon} />
            <h1 className={styles.title}>{t('premiumPlans.title')}</h1>
          </div>
          <p className={styles.subtitle}>{t('premiumPlans.subtitle')}</p>
        </div>

        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => loadPlans(true)}
            disabled={loading || refreshing}
          >
            <RefreshIcon size={15} className={refreshing ? styles.spinning : ''} />
            <span>{t('premiumPlans.refreshBtn')}</span>
          </button>
          <button
            type="button"
            className={styles.primaryBtn}
            onClick={() => setFormModal({ isOpen: true, plan: null })}
          >
            <PlusIcon size={16} />
            <span>{t('premiumPlans.createBtn')}</span>
          </button>
        </div>
      </div>

      {/* Summary Strip */}
      <div className={styles.summaryGrid}>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>{t('premiumPlans.summaryTotal')}</span>
          <strong className={styles.summaryValue}>{summary.total}</strong>
        </div>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>{t('premiumPlans.summaryActive')}</span>
          <strong className={`${styles.summaryValue} ${styles.textGreen}`}>{summary.active}</strong>
        </div>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>{t('premiumPlans.summaryInactive')}</span>
          <strong className={`${styles.summaryValue} ${styles.textRed}`}>{summary.inactive}</strong>
        </div>
      </div>

      {/* Filter Bar */}
      <div className={styles.filterBar}>
        <div className={styles.searchBox}>
          <SearchIcon size={15} className={styles.searchIcon} />
          <input
            type="text"
            className={styles.searchInput}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder={t('premiumPlans.searchPlaceholder')}
            aria-label={t('premiumPlans.searchPlaceholder')}
          />
        </div>
        <select
          className={styles.selectInput}
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          aria-label={t('premiumPlans.columns.status')}
        >
          <option value="ALL">{t('premiumPlans.allStatus')}</option>
          <option value="ACTIVE">{t('premiumPlans.activeStatus')}</option>
          <option value="INACTIVE">{t('premiumPlans.inactiveStatus')}</option>
        </select>
      </div>

      {/* Content */}
      {error ? (
        <AdminErrorState onRetry={() => loadPlans(false)} />
      ) : (
        <div className={styles.tableCard}>
          <DataTable
            columns={columns}
            data={filteredPlans}
            isLoading={loading}
            emptyMessage={t('premiumPlans.emptyList')}
            borderless
          />
        </div>
      )}

      {/* Modals */}
      <PlanFormModal
        isOpen={formModal.isOpen}
        plan={formModal.plan}
        onClose={() => setFormModal({ isOpen: false, plan: null })}
        onSaved={handlePlanSaved}
      />
      <PlanStatusModal
        isOpen={statusModal.isOpen}
        plan={statusModal.plan}
        targetAction={statusModal.targetAction}
        onClose={() => setStatusModal({ isOpen: false, plan: null, targetAction: 'enable' })}
        onToggled={handlePlanToggled}
      />
    </div>
  );
}

PlanManagementPage.propTypes = {
  onNavigate: PropTypes.func,
};
