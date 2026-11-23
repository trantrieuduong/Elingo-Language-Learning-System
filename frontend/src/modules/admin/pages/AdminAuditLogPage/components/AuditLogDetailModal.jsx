import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon } from '../../../components/icons/AdminIcons';
import styles from './AuditLogDetailModal.module.css';

export default function AuditLogDetailModal({ log, onClose }) {
  const { t } = useTranslation('admin');

  const formattedTime = log.createdAt
    ? log.createdAt.replace('T', ' ').substring(0, 19)
    : '—';

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div
        className={styles.modal}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.headerText}>
            <h2 id="modal-title" className={styles.title}>
              {t('auditLogs.detailModalTitle')}
            </h2>
            <p className={styles.subtitle}>
              {t('auditLogs.detailModalSubtitle')}
            </p>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('common.close')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        {/* Content Body */}
        <div className={styles.body}>
          {/* Metadata Grid */}
          <div className={styles.metaGrid}>
            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('auditLogs.logId')}</span>
              <span className={styles.metaValue}>#{log.id}</span>
            </div>
            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('auditLogs.adminId')}</span>
              <span className={styles.metaValue}>Admin #{log.adminId}</span>
            </div>
            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('auditLogs.timestamp')}</span>
              <span className={styles.metaValue}>{formattedTime}</span>
            </div>
            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('auditLogs.ipAddress')}</span>
              <span className={`${styles.metaValue} ${styles.mono}`}>
                {log.ipAddress || '—'}
              </span>
            </div>
            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('auditLogs.action')}</span>
              <span className={styles.actionBadge}>{log.action}</span>
            </div>
            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>{t('auditLogs.targetType')}</span>
              <span className={styles.targetBadge}>
                {log.targetType} {log.targetId ? `(#${log.targetId})` : ''}
              </span>
            </div>
          </div>

          {/* Payload / Detail Section */}
          <div className={styles.detailSection}>
            <span className={styles.sectionLabel}>
              {t('auditLogs.detailPayload')}
            </span>
            <div className={styles.codeBlock}>
              <pre>{log.detail || '(no detail recorded)'}</pre>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className={styles.footer}>
          <button type="button" className={styles.closeModalBtn} onClick={onClose}>
            {t('common.close')}
          </button>
        </div>
      </div>
    </div>
  );
}

AuditLogDetailModal.propTypes = {
  log: PropTypes.shape({
    id: PropTypes.number.isRequired,
    adminId: PropTypes.number.isRequired,
    action: PropTypes.string.isRequired,
    targetType: PropTypes.string.isRequired,
    targetId: PropTypes.string,
    detail: PropTypes.string,
    ipAddress: PropTypes.string,
    createdAt: PropTypes.string,
  }).isRequired,
  onClose: PropTypes.func.isRequired,
};
