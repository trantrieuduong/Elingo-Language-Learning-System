import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { AlertTriangleIcon, CloseIcon, ExternalLinkIcon } from '../../../components/icons/AdminIcons';
import { getReportById } from '../../../services/adminCommunityApi';
import styles from '../../PostModerationPage/components/CommunityModal.module.css';

const getTargetUrl = (targetType, targetId) => {
  if (!targetId) return null;
  switch (targetType) {
    case 'POST':
      return `/community/posts/${targetId}`;
    case 'DECK':
      return `/admin/decks/${targetId}`;
    case 'USER':
      return `/profile/${targetId}`;
    default:
      return null;
  }
};

const STATUS_BADGE_MAP = {
  PENDING: styles.badgeAmber,
  RESOLVED: styles.badgeGreen,
  REJECTED: styles.badgeRed,
};

const formatDateTime = (iso, locale) => {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function ReportDetailModal({
  isOpen,
  reportId,
  onClose,
  onOpenDecision,
}) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen || !reportId) return undefined;
    let cancelled = false;
    setLoading(true);
    setError('');

    getReportById(reportId)
      .then((data) => {
        if (!cancelled) setDetail(data);
      })
      .catch(() => {
        if (!cancelled) setError(t('reports.detail.loadError'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [isOpen, reportId, t]);

  if (!isOpen) return null;

  const isPending = detail && detail.status === 'PENDING';

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={`${styles.modal} ${styles.modalWide}`}>
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            <AlertTriangleIcon size={20} className={styles.headerIcon} />
            <div>
              <h2 className={styles.modalTitle}>
                {t('reports.detail.title')} #{reportId}
              </h2>
              <p className={styles.modalSubtitle}>{t('reports.detail.subtitle')}</p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('reports.detail.close')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <div className={styles.modalBody}>
          {loading && <div className={styles.loadingText}>{t('feedback.loadingList')}</div>}
          {error && <div className={styles.errorAlert}>{error}</div>}

          {!loading && !error && detail && (
            <>
              <div className={styles.detailGrid}>
                <div className={styles.detailItem}>
                  <span className={styles.detailLabel}>{t('reports.columns.target')}</span>
                  <div className={styles.targetValueRow}>
                    <span className={styles.detailValue}>
                      {t(`reports.targetType.${detail.targetType}`, detail.targetType)} #{detail.targetId}
                    </span>
                    {getTargetUrl(detail.targetType, detail.targetId) && (
                      <a
                        href={getTargetUrl(detail.targetType, detail.targetId)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={styles.targetLinkBtn}
                      >
                        <span>{t('reports.actions.openTarget')}</span>
                        <ExternalLinkIcon size={12} />
                      </a>
                    )}
                  </div>
                </div>
                <div className={styles.detailItem}>
                  <span className={styles.detailLabel}>{t('reports.columns.status')}</span>
                  <div>
                    <span className={`${styles.badge} ${STATUS_BADGE_MAP[detail.status] || styles.badgeMuted}`}>
                      {t(`reports.status.${detail.status}`, detail.status)}
                    </span>
                  </div>
                </div>
                <div className={styles.detailItem}>
                  <span className={styles.detailLabel}>{t('reports.columns.reporter')}</span>
                  <span className={styles.detailValue}>
                    {detail.reporterUsername || '—'} (#{detail.reporterId || '—'})
                  </span>
                </div>
                <div className={styles.detailItem}>
                  <span className={styles.detailLabel}>{t('reports.columns.createdAt')}</span>
                  <span className={styles.detailValue}>{formatDateTime(detail.createdAt, locale)}</span>
                </div>
              </div>

              <div className={styles.contentBox}>
                <span className={styles.detailLabel}>{t('reports.columns.description')}</span>
                <p className={styles.postContent}>{detail.description || '—'}</p>
              </div>

              {Array.isArray(detail.imageUrls) && detail.imageUrls.length > 0 && (
                <>
                  <div className={styles.sectionTitle}>{t('reports.detail.evidenceSection')}</div>
                  <div className={styles.mediaGrid}>
                    {detail.imageUrls.map((url, idx) => (
                      <a
                        key={`${url}-${idx}`}
                        href={url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={styles.mediaCard}
                      >
                        <img
                          className={styles.mediaImage}
                          src={url}
                          alt={`${t('reports.detail.evidenceAlt')} ${idx + 1}`}
                        />
                      </a>
                    ))}
                  </div>
                </>
              )}

              {detail.status !== 'PENDING' && (
                <>
                  <div className={styles.sectionTitle}>{t('reports.detail.resolutionSection')}</div>
                  <div className={styles.detailGrid}>
                    <div className={styles.detailItem}>
                      <span className={styles.detailLabel}>{t('reports.detail.resolvedBy')}</span>
                      <span className={styles.detailValue}>
                        {detail.resolvedByUsername || '—'}
                        {detail.resolvedById ? ` (#${detail.resolvedById})` : ''}
                      </span>
                    </div>
                    <div className={styles.detailItem}>
                      <span className={styles.detailLabel}>{t('reports.detail.resolvedAt')}</span>
                      <span className={styles.detailValue}>{formatDateTime(detail.resolvedAt, locale)}</span>
                    </div>
                  </div>
                  <div className={styles.contentBox}>
                    <span className={styles.detailLabel}>{t('reports.detail.resolutionNote')}</span>
                    <p className={styles.postContent}>{detail.resolutionNote || '—'}</p>
                  </div>
                </>
              )}
            </>
          )}
        </div>

        <div className={styles.modalFooter}>
          <button type="button" className={styles.cancelBtn} onClick={onClose}>
            {t('reports.detail.close')}
          </button>
          {isPending && (
            <>
              <button
                type="button"
                className={styles.rejectBtn}
                onClick={() => onOpenDecision('reject', detail)}
              >
                {t('reports.actions.reject')}
              </button>
              <button
                type="button"
                className={styles.approveBtn}
                onClick={() => onOpenDecision('resolve', detail)}
              >
                {t('reports.actions.resolve')}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

ReportDetailModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  reportId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  onClose: PropTypes.func.isRequired,
  onOpenDecision: PropTypes.func.isRequired,
};
