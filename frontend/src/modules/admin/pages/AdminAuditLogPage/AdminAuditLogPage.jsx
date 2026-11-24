import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  RefreshIcon,
  EyeIcon,
} from '../../components/icons/AdminIcons';
import {
  fetchAuditLogs,
  fetchAuditActions,
  fetchAuditTargetTypes,
} from '../../services/adminAuditApi';
import AuditLogDetailModal from './components/AuditLogDetailModal';
import styles from './AdminAuditLogPage.module.css';

export default function AdminAuditLogPage({ onNavigate: _onNavigate }) {
  const { t } = useTranslation('admin');

  // Filter state
  const [action, setAction] = useState('');
  const [targetType, setTargetType] = useState('');
  const [adminId, setAdminId] = useState('');
  const [targetId, setTargetId] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  // Pagination state 
  const [page, setPage] = useState(1);
  const pageSize = 20;

  // Metadata catalogs
  const [actionsList, setActionsList] = useState([]);
  const [targetTypesList, setTargetTypesList] = useState([]);

  // Data & loading state
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

  // Detail Modal state
  const [detailModal, setDetailModal] = useState({
    isOpen: false,
    log: null,
  });

  // Load action and targetType lists on mount
  useEffect(() => {
    const loadCatalogs = async () => {
      try {
        const [actRes, tgtRes] = await Promise.all([
          fetchAuditActions(),
          fetchAuditTargetTypes(),
        ]);
        setActionsList(actRes || []);
        setTargetTypesList(tgtRes || []);
      } catch {
        // Silently tolerate catalog failure
      }
    };
    loadCatalogs();
  }, []);

  // Load audit logs
  const loadLogs = useCallback(
    async (isManualRefresh = false) => {
      try {
        if (isManualRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }
        setError(null);

        const params = {
          page, // 1-indexed (one-indexed-parameters: true)
          size: pageSize,
        };

        if (action) params.action = action;
        if (targetType) params.targetType = targetType;
        if (adminId.trim()) params.adminId = Number(adminId.trim());
        if (targetId.trim()) params.targetId = targetId.trim();
        if (fromDate) params.from = `${fromDate}T00:00:00`;
        if (toDate) params.to = `${toDate}T23:59:59`;

        const res = await fetchAuditLogs(params);
        setLogData(res || {
          content: [],
          page: 1,
          size: pageSize,
          totalElements: 0,
          totalPages: 1,
          isLast: true,
        });
      } catch (err) {
        setError(err.message || t('feedback.errorDesc'));
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [action, targetType, adminId, targetId, fromDate, toDate, page, pageSize, t]
  );

  useEffect(() => {
    loadLogs(false);
  }, [loadLogs]);

  const handleApplyFilter = (e) => {
    e.preventDefault();
    setPage(1);
    loadLogs(false);
  };

  const handleResetFilter = () => {
    setAction('');
    setTargetType('');
    setAdminId('');
    setTargetId('');
    setFromDate('');
    setToDate('');
    setPage(1);
  };

  const handleOpenDetail = (log) => {
    setDetailModal({
      isOpen: true,
      log,
    });
  };

  const handleCloseDetail = () => {
    setDetailModal({
      isOpen: false,
      log: null,
    });
  };

  // Helper to determine action badge styling
  const getActionClass = (act) => {
    if (!act) return styles.actionDefault;
    if (
      act.includes('CREATE') ||
      act.includes('PUBLISH') ||
      act.includes('GRANT') ||
      act.includes('UNLOCK') ||
      act.includes('APPROVE') ||
      act.includes('RESOLVE')
    ) {
      return styles.actionCreate;
    }
    if (act.includes('UPDATE') || act.includes('CHANGE')) {
      return styles.actionUpdate;
    }
    if (
      act.includes('DELETE') ||
      act.includes('LOCK') ||
      act.includes('ARCHIVE') ||
      act.includes('REJECT') ||
      act.includes('REVOKE')
    ) {
      return styles.actionDelete;
    }
    return styles.actionDefault;
  };

  // Table columns definition
  const columns = useMemo(
    () => [
      {
        key: 'createdAt',
        dataIndex: 'createdAt',
        title: t('auditLogs.colTime'),
        width: '16%',
        render: (time) => {
          if (!time) return '—';
          const formatted = time.replace('T', ' ').substring(0, 19);
          return <span className={styles.timeText}>{formatted}</span>;
        },
      },
      {
        key: 'adminId',
        dataIndex: 'adminId',
        title: t('auditLogs.colAdmin'),
        width: '12%',
        render: (id) => (
          <span className={styles.adminBadge}>
            Admin #{id}
          </span>
        ),
      },
      {
        key: 'action',
        dataIndex: 'action',
        title: t('auditLogs.colAction'),
        width: '20%',
        render: (act) => (
          <span className={`${styles.actionBadge} ${getActionClass(act)}`}>
            {act}
          </span>
        ),
      },
      {
        key: 'target',
        title: t('auditLogs.colTarget'),
        width: '18%',
        render: (_, record) => (
          <div className={styles.targetCell}>
            <span className={styles.targetTypeBadge}>{record.targetType}</span>
            {record.targetId && (
              <span className={styles.targetIdText} title={record.targetId}>
                #{record.targetId}
              </span>
            )}
          </div>
        ),
      },
      {
        key: 'detail',
        dataIndex: 'detail',
        title: t('auditLogs.colDetail'),
        width: '16%',
        render: (det) => (
          <div className={styles.detailCell} title={det || ''}>
            {det ? (
              <span className={styles.detailText}>{det}</span>
            ) : (
              <span className={styles.emptyText}>—</span>
            )}
          </div>
        ),
      },
      {
        key: 'ipAddress',
        dataIndex: 'ipAddress',
        title: t('auditLogs.colIp'),
        width: '11%',
        render: (ip) => <span className={styles.ipText}>{ip || '—'}</span>,
      },
      {
        key: 'actions',
        title: t('common.details'),
        width: '7%',
        align: 'right',
        render: (_, record) => (
          <button
            type="button"
            className={styles.viewBtn}
            onClick={() => handleOpenDetail(record)}
            title={t('common.details')}
            aria-label={t('common.details')}
          >
            <EyeIcon size={15} />
          </button>
        ),
      },
    ],
    [t]
  );

  return (
    <div className={styles.container}>
      {/* Header */}
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <h1 className={styles.title}>{t('auditLogs.title')}</h1>
          <p className={styles.subtitle}>{t('auditLogs.subtitle')}</p>
        </div>
        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => loadLogs(true)}
            disabled={loading || refreshing}
            title={t('common.refresh')}
            aria-label={t('common.refresh')}
          >
            <RefreshIcon
              size={16}
              className={refreshing ? styles.spinning : ''}
            />
            <span>{t('common.refresh')}</span>
          </button>
        </div>
      </header>

      {/* Filter Form Card */}
      <form onSubmit={handleApplyFilter} className={styles.filterCard}>
        <div className={styles.filterGrid}>
          {/* Action Filter */}
          <div className={styles.fieldGroup}>
            <label htmlFor="filter-action" className={styles.fieldLabel}>
              {t('auditLogs.filterAction')}
            </label>
            <select
              id="filter-action"
              className={styles.select}
              value={action}
              onChange={(e) => setAction(e.target.value)}
            >
              <option value="">{t('auditLogs.allActions')}</option>
              {actionsList.map((act) => (
                <option key={act} value={act}>
                  {act}
                </option>
              ))}
            </select>
          </div>

          {/* Target Type Filter */}
          <div className={styles.fieldGroup}>
            <label htmlFor="filter-target-type" className={styles.fieldLabel}>
              {t('auditLogs.filterTargetType')}
            </label>
            <select
              id="filter-target-type"
              className={styles.select}
              value={targetType}
              onChange={(e) => setTargetType(e.target.value)}
            >
              <option value="">{t('auditLogs.allTargetTypes')}</option>
              {targetTypesList.map((tgt) => (
                <option key={tgt} value={tgt}>
                  {tgt}
                </option>
              ))}
            </select>
          </div>

          {/* Admin ID Filter */}
          <div className={styles.fieldGroup}>
            <label htmlFor="filter-admin-id" className={styles.fieldLabel}>
              {t('auditLogs.filterAdminId')}
            </label>
            <input
              id="filter-admin-id"
              type="number"
              className={styles.input}
              placeholder={t('auditLogs.filterAdminIdPlaceholder')}
              value={adminId}
              onChange={(e) => setAdminId(e.target.value)}
            />
          </div>

          {/* Target ID Filter */}
          <div className={styles.fieldGroup}>
            <label htmlFor="filter-target-id" className={styles.fieldLabel}>
              {t('auditLogs.filterTargetId')}
            </label>
            <input
              id="filter-target-id"
              type="text"
              className={styles.input}
              placeholder={t('auditLogs.filterTargetIdPlaceholder')}
              value={targetId}
              onChange={(e) => setTargetId(e.target.value)}
            />
          </div>

          {/* From Date */}
          <div className={styles.fieldGroup}>
            <label htmlFor="filter-from-date" className={styles.fieldLabel}>
              {t('auditLogs.filterFrom')}
            </label>
            <input
              id="filter-from-date"
              type="date"
              className={styles.input}
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
          </div>

          {/* To Date */}
          <div className={styles.fieldGroup}>
            <label htmlFor="filter-to-date" className={styles.fieldLabel}>
              {t('auditLogs.filterTo')}
            </label>
            <input
              id="filter-to-date"
              type="date"
              className={styles.input}
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
          </div>
        </div>

        {/* Filter Buttons */}
        <div className={styles.filterActions}>
          <button
            type="button"
            className={styles.resetBtn}
            onClick={handleResetFilter}
          >
            {t('common.reset')}
          </button>
          <button type="submit" className={styles.applyBtn}>
            {t('common.apply')}
          </button>
        </div>
      </form>

      {/* Table Area */}
      {error ? (
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={() => loadLogs(false)}
        />
      ) : (
        <div className={styles.tableCard}>
          <DataTable
            borderless
            columns={columns}
            data={logData.content}
            keyField="id"
            isLoading={loading}
            emptyMessage={t('feedback.emptyList')}
          />

          {/* Pagination Bar */}
          {!loading && logData.totalElements > 0 && (
            <AdminPagination
              page={page}
              totalPages={logData.totalPages}
              totalElements={logData.totalElements}
              isLast={Boolean(logData.isLast ?? logData.last)}
              onPageChange={setPage}
              unitLabel={t('common.items')}
              isLoading={loading}
            />
          )}
        </div>
      )}

      {/* Detail Modal */}
      {detailModal.isOpen && detailModal.log && (
        <AuditLogDetailModal
          log={detailModal.log}
          onClose={handleCloseDetail}
        />
      )}
    </div>
  );
}

AdminAuditLogPage.propTypes = {
  onNavigate: PropTypes.func,
};
