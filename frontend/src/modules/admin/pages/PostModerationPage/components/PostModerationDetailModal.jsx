import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, ShieldCheckIcon } from '../../../components/icons/AdminIcons';
import { getModerationPostDetail } from '../../../services/adminCommunityApi';
import styles from './CommunityModal.module.css';

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

export default function PostModerationDetailModal({
  isOpen,
  postId,
  onClose,
  onOpenDecision,
}) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen || !postId) return undefined;
    let cancelled = false;
    setLoading(true);
    setError('');

    getModerationPostDetail(postId)
      .then((data) => {
        if (!cancelled) setDetail(data);
      })
      .catch(() => {
        if (!cancelled) setError(t('postModeration.detail.loadError'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [isOpen, postId, t]);

  if (!isOpen) return null;

  const canModerate = detail && (detail.status === 'PENDING' || detail.status === 'NEEDS_REVIEW');

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={`${styles.modal} ${styles.modalWide}`}>
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            <ShieldCheckIcon size={20} className={styles.headerIcon} />
            <div>
              <h2 className={styles.modalTitle}>
                {t('postModeration.detail.title')} #{postId}
              </h2>
              <p className={styles.modalSubtitle}>{t('postModeration.detail.subtitle')}</p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('postModeration.detail.close')}
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
                  <span className={styles.detailLabel}>{t('postModeration.columns.author')}</span>
                  <span className={styles.detailValue}>
                    {detail.author?.username || '—'} (#{detail.author?.id || '—'})
                  </span>
                </div>
                <div className={styles.detailItem}>
                  <span className={styles.detailLabel}>{t('postModeration.columns.group')}</span>
                  <span className={styles.detailValue}>{detail.groupName || '—'}</span>
                </div>
                <div className={styles.detailItem}>
                  <span className={styles.detailLabel}>{t('postModeration.columns.status')}</span>
                  <div>
                    <span className={`${styles.badge} ${STATUS_BADGE_MAP[detail.status] || styles.badgeMuted}`}>
                      {t(`postModeration.status.${detail.status}`, detail.status)}
                    </span>
                  </div>
                </div>
                <div className={styles.detailItem}>
                  <span className={styles.detailLabel}>{t('postModeration.columns.createdAt')}</span>
                  <span className={styles.detailValue}>{formatDateTime(detail.createdAt, locale)}</span>
                </div>
              </div>

              <div className={styles.contentBox}>
                <h3 className={styles.postTitle}>{detail.title}</h3>
                <p className={styles.postContent}>{detail.content}</p>
              </div>

              {Array.isArray(detail.medias) && detail.medias.length > 0 && (
                <>
                  <div className={styles.sectionTitle}>{t('postModeration.detail.mediaSection')}</div>
                  <div className={styles.mediaGrid}>
                    {detail.medias.map((media, idx) => (
                      <div key={`${media.mediaUrl}-${idx}`} className={styles.mediaCard}>
                        {media.mediaType === 'VIDEO' ? (
                          <video className={styles.mediaVideo} controls src={media.mediaUrl} />
                        ) : (
                          <a href={media.mediaUrl} target="_blank" rel="noopener noreferrer">
                            <img
                              className={styles.mediaImage}
                              src={media.mediaUrl}
                              alt={detail.title}
                            />
                          </a>
                        )}
                      </div>
                    ))}
                  </div>
                </>
              )}

              <div className={styles.sectionTitle}>{t('postModeration.detail.logsSection')}</div>
              {Array.isArray(detail.moderationLogs) && detail.moderationLogs.length > 0 ? (
                <div className={styles.logList}>
                  {detail.moderationLogs.map((logItem) => (
                    <div key={logItem.id} className={styles.logItem}>
                      <div className={styles.logTop}>
                        <div className={styles.logMeta}>
                          <span className={`${styles.badge} ${ACTION_BADGE_MAP[logItem.action] || styles.badgeMuted}`}>
                            {t(`postModeration.action.${logItem.action}`, logItem.action)}
                          </span>
                          <span className={`${styles.badge} ${styles.badgeMuted}`}>
                            {t(`postModeration.actorType.${logItem.actorType}`, logItem.actorType)}
                            {logItem.actorUsername ? `: ${logItem.actorUsername}` : ''}
                          </span>
                          {logItem.aiLabel && (
                            <span className={`${styles.badge} ${styles.badgeBlue}`}>
                              {logItem.aiLabel}
                              {logItem.aiScore !== null && logItem.aiScore !== undefined
                                ? ` (${Number(logItem.aiScore).toFixed(2)})`
                                : ''}
                            </span>
                          )}
                        </div>
                        <span className={styles.logTime}>{formatDateTime(logItem.createdAt, locale)}</span>
                      </div>
                      {logItem.note && <p className={styles.logNote}>{logItem.note}</p>}
                    </div>
                  ))}
                </div>
              ) : (
                <p className={styles.logNote}>{t('postModeration.detail.noLogs')}</p>
              )}
            </>
          )}
        </div>

        <div className={styles.modalFooter}>
          <button type="button" className={styles.cancelBtn} onClick={onClose}>
            {t('postModeration.detail.close')}
          </button>
          {canModerate && (
            <>
              <button
                type="button"
                className={styles.rejectBtn}
                onClick={() => onOpenDecision('reject', detail)}
              >
                {t('postModeration.actions.reject')}
              </button>
              <button
                type="button"
                className={styles.approveBtn}
                onClick={() => onOpenDecision('approve', detail)}
              >
                {t('postModeration.actions.approve')}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

PostModerationDetailModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  postId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  onClose: PropTypes.func.isRequired,
  onOpenDecision: PropTypes.func.isRequired,
};
