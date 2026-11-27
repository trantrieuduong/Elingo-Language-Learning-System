import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminModal from '../../../components/AdminModal/AdminModal';
import styles from './AuditLogDetailModal.module.css';

export default function AuditLogDetailModal({ log, onClose }) {
  const { t } = useTranslation('admin');

  const formattedTime = log.createdAt
    ? log.createdAt.replace('T', ' ').substring(0, 19)
    : '—';

  const metaItems = [
    { label: t('auditLogs.logId'), value: `#${log.id}` },
    { label: t('auditLogs.adminId'), value: `Admin #${log.adminId}` },
    { label: t('auditLogs.timestamp'), value: formattedTime },
    { label: t('auditLogs.ipAddress'), value: log.ipAddress || '—', mono: true },
    { label: t('auditLogs.action'), value: log.action, badgeClass: styles.actionBadge },
    {
      label: t('auditLogs.targetType'),
      value: `${log.targetType}${log.targetId ? ` (#${log.targetId})` : ''}`,
      badgeClass: styles.targetBadge,
    },
  ];

  return (
    <AdminModal
      isOpen={Boolean(log)}
      onClose={onClose}
      closeOnBackdrop
      closeLabel={t('common.close')}
      titleId="modal-title"
      title={t('auditLogs.detailModalTitle')}
      subtitle={t('auditLogs.detailModalSubtitle')}
      footer={
        <button type="button" className={styles.closeModalBtn} onClick={onClose}>
          {t('common.close')}
        </button>
      }
    >
      <div className={styles.body}>
        <div className={styles.metaGrid}>
          {metaItems.map((item) => (
            <div key={item.label} className={styles.metaItem}>
              <span className={styles.metaLabel}>{item.label}</span>
              <span
                className={
                  item.badgeClass ||
                  (item.mono ? `${styles.metaValue} ${styles.mono}` : styles.metaValue)
                }
              >
                {item.value}
              </span>
            </div>
          ))}
        </div>

        <div className={styles.detailSection}>
          <span className={styles.sectionLabel}>{t('auditLogs.detailPayload')}</span>
          <div className={styles.codeBlock}>
            <pre>{log.detail || '(no detail recorded)'}</pre>
          </div>
        </div>
      </div>
    </AdminModal>
  );
}

AuditLogDetailModal.propTypes = {
  log: PropTypes.shape({
    id: PropTypes.number.isRequired,
    adminId: PropTypes.number.isRequired,
    action: PropTypes.string.isRequired,
    targetType: PropTypes.string.isRequired,
    targetId: PropTypes.number,
    detail: PropTypes.string,
    ipAddress: PropTypes.string,
    createdAt: PropTypes.string,
  }).isRequired,
  onClose: PropTypes.func.isRequired,
};
