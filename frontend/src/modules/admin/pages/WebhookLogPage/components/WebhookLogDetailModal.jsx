import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, ActivityIcon } from '../../../components/icons/AdminIcons';
import { getWebhookLogById } from '../../../services/adminPremiumApi';
import styles from './WebhookLogDetailModal.module.css';

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

const formatPayload = (rawPayload) => {
  if (!rawPayload) return null;
  try {
    return JSON.stringify(JSON.parse(rawPayload), null, 2);
  } catch {
    return String(rawPayload);
  }
};

export default function WebhookLogDetailModal({ isOpen, logId, onClose }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!isOpen || !logId) return;
    let cancelled = false;
    setLoading(true);
    setErrorMsg('');
    setDetail(null);
    setCopied(false);

    getWebhookLogById(logId)
      .then((res) => {
        if (!cancelled) setDetail(res);
      })
      .catch((err) => {
        if (!cancelled) {
          setErrorMsg(
            err?.response?.data?.error?.code === 'PAYMENT_WEBHOOK_LOG_NOT_FOUND'
              ? t('premiumWebhookLogs.errors.notFound')
              : t('premiumWebhookLogs.errors.generic')
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [isOpen, logId, t]);

  if (!isOpen) return null;

  const formattedPayload = formatPayload(detail?.payload);

  const handleCopy = async () => {
    if (!formattedPayload || !navigator?.clipboard?.writeText) return;
    try {
      await navigator.clipboard.writeText(formattedPayload);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Bỏ qua nếu trình duyệt chặn clipboard
    }
  };

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="webhook-detail-title">
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            <ActivityIcon size={20} className={styles.headerIcon} />
            <div>
              <h2 id="webhook-detail-title" className={styles.modalTitle}>
                {t('premiumWebhookLogs.detailModal.title')}
              </h2>
              <p className={styles.modalSubtitle}>
                {t('premiumWebhookLogs.detailModal.subtitle')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('premiumWebhookLogs.detailModal.close')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <div className={styles.modalBody}>
          {loading && <div className={styles.loadingText}>{t('feedback.loadingList')}</div>}
          {errorMsg && <div className={styles.errorText}>{errorMsg}</div>}

          {!loading && !errorMsg && detail && (
            <>
              <div className={styles.detailGrid}>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumWebhookLogs.detailModal.logId')}
                  </span>
                  <strong className={styles.infoValue}>#{detail.id}</strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumWebhookLogs.detailModal.provider')}
                  </span>
                  <strong className={styles.infoValue}>
                    {detail.providerCode || '—'} (#{detail.providerId || '—'})
                  </strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumWebhookLogs.detailModal.transactionId')}
                  </span>
                  <strong className={styles.infoValue}>
                    {detail.transactionId ? `#${detail.transactionId}` : '—'}
                  </strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumWebhookLogs.detailModal.eventId')}
                  </span>
                  <strong className={styles.infoValue}>{detail.eventId || '—'}</strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumWebhookLogs.detailModal.signatureValid')}
                  </span>
                  <strong className={styles.infoValue}>
                    {detail.signatureValid
                      ? t('premiumWebhookLogs.signature.valid')
                      : t('premiumWebhookLogs.signature.invalid')}
                  </strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumWebhookLogs.detailModal.status')}
                  </span>
                  <strong className={styles.infoValue}>
                    {t(`premiumWebhookLogs.status.${detail.status}`, detail.status)}
                  </strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumWebhookLogs.detailModal.receivedAt')}
                  </span>
                  <span className={styles.infoValue}>
                    {formatDateTime(detail.receivedAt, locale)}
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumWebhookLogs.detailModal.processedAt')}
                  </span>
                  <span className={styles.infoValue}>
                    {formatDateTime(detail.processedAt, locale)}
                  </span>
                </div>
              </div>

              <div className={styles.payloadHeader}>
                <span className={styles.payloadTitle}>
                  {t('premiumWebhookLogs.detailModal.payloadTitle')}
                </span>
                {formattedPayload && (
                  <button type="button" className={styles.copyBtn} onClick={handleCopy}>
                    {copied
                      ? t('premiumWebhookLogs.detailModal.copiedPayload')
                      : t('premiumWebhookLogs.detailModal.copyPayload')}
                  </button>
                )}
              </div>

              {formattedPayload ? (
                <pre className={styles.codeBlock}>{formattedPayload}</pre>
              ) : (
                <div className={styles.emptyPayload}>
                  {t('premiumWebhookLogs.detailModal.noPayload')}
                </div>
              )}
            </>
          )}

          <div className={styles.modalFooter}>
            <button type="button" className={styles.closeFooterBtn} onClick={onClose}>
              {t('premiumWebhookLogs.detailModal.close')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

WebhookLogDetailModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  logId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  onClose: PropTypes.func.isRequired,
};
