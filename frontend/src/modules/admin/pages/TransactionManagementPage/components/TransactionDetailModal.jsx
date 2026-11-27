import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  CloseIcon,
  CreditCardIcon,
  CheckCircleIcon,
  ArrowDownRightIcon,
  ExternalLinkIcon,
} from '../../../components/icons/AdminIcons';
import { getTransactionById } from '../../../services/adminPremiumApi';
import styles from './TransactionModal.module.css';

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
    second: '2-digit',
  });
};

const formatMetadata = (metadata) => {
  if (!metadata) return null;
  try {
    return JSON.stringify(JSON.parse(metadata), null, 2);
  } catch {
    return String(metadata);
  }
};

export default function TransactionDetailModal({
  isOpen,
  transactionId,
  onClose,
  onOpenAction,
  onViewWebhooks,
}) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language?.startsWith('en') ? 'en-US' : 'vi-VN';

  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (!isOpen || !transactionId) return;
    let cancelled = false;
    setLoading(true);
    setErrorMsg('');
    setDetail(null);

    getTransactionById(transactionId)
      .then((res) => {
        if (!cancelled) setDetail(res);
      })
      .catch((err) => {
        if (!cancelled) {
          setErrorMsg(
            err?.response?.data?.error?.code === 'PAYMENT_TRANSACTION_NOT_FOUND'
              ? t('premiumTransactions.errors.notFound')
              : t('premiumTransactions.errors.generic')
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [isOpen, transactionId, t]);

  if (!isOpen) return null;

  const formattedMeta = formatMetadata(detail?.metadata);

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="tx-detail-title">
      <div className={`${styles.modal} ${styles.modalWide}`}>
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            <CreditCardIcon size={20} className={styles.headerIcon} />
            <div>
              <h2 id="tx-detail-title" className={styles.modalTitle}>
                {t('premiumTransactions.detailModal.title')}
              </h2>
              <p className={styles.modalSubtitle}>
                {t('premiumTransactions.detailModal.subtitle')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('premiumTransactions.detailModal.close')}
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
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.txId')}</span>
                  <strong className={styles.infoValue}>#{detail.id}</strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.refCode')}</span>
                  <strong className={styles.infoValue}>{detail.providerRefCode || '—'}</strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.status')}</span>
                  <strong className={styles.infoValue}>
                    {t(`premiumTransactions.status.${detail.status}`, detail.status)}
                  </strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.amount')}</span>
                  <strong className={styles.infoValue}>
                    {formatAmount(detail.amount, detail.currency, locale)}
                  </strong>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.provider')}</span>
                  <strong className={styles.infoValue}>
                    {detail.providerName || detail.providerCode || '—'}
                  </strong>
                </div>
              </div>

              <div className={styles.sectionTitle}>
                {t('premiumTransactions.detailModal.userSection')}
              </div>
              <div className={styles.detailGrid}>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.userId')}</span>
                  <span className={styles.infoValue}>#{detail.userId}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.username')}</span>
                  <span className={styles.infoValue}>{detail.username || '—'}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.fullName')}</span>
                  <span className={styles.infoValue}>{detail.userFullName || '—'}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.email')}</span>
                  <span className={styles.infoValue}>{detail.userEmail || '—'}</span>
                </div>
              </div>

              <div className={styles.sectionTitle}>
                {t('premiumTransactions.detailModal.planSection')}
              </div>
              <div className={styles.detailGrid}>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.planName')}</span>
                  <span className={styles.infoValue}>{detail.planName || '—'}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.duration')}</span>
                  <span className={styles.infoValue}>
                    {detail.planDurationDays} {t('premiumTransactions.detailModal.daysUnit')}
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumTransactions.detailModal.subscriptionId')}
                  </span>
                  <span className={styles.infoValue}>
                    {detail.subscriptionId
                      ? `#${detail.subscriptionId} (${detail.subscriptionStatus || ''})`
                      : t('premiumTransactions.detailModal.noSubscription')}
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumTransactions.detailModal.subscriptionStartAt')}
                  </span>
                  <span className={styles.infoValue}>
                    {formatDateTime(detail.subscriptionStartAt, locale)}
                  </span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>
                    {t('premiumTransactions.detailModal.subscriptionEndAt')}
                  </span>
                  <span className={styles.infoValue}>
                    {formatDateTime(detail.subscriptionEndAt, locale)}
                  </span>
                </div>
              </div>

              <div className={styles.sectionTitle}>
                {t('premiumTransactions.detailModal.timestampsSection')}
              </div>
              <div className={styles.detailGrid}>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.createdAt')}</span>
                  <span className={styles.infoValue}>{formatDateTime(detail.createdAt, locale)}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.paidAt')}</span>
                  <span className={styles.infoValue}>{formatDateTime(detail.paidAt, locale)}</span>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>{t('premiumTransactions.detailModal.updatedAt')}</span>
                  <span className={styles.infoValue}>{formatDateTime(detail.updatedAt, locale)}</span>
                </div>
              </div>

              <div className={styles.sectionTitle}>
                {t('premiumTransactions.detailModal.metadataSection')}
              </div>
              {formattedMeta ? (
                <pre className={styles.codeBlock}>{formattedMeta}</pre>
              ) : (
                <div className={styles.emptyMeta}>
                  {t('premiumTransactions.detailModal.noMetadata')}
                </div>
              )}
            </>
          )}

          <div className={styles.modalFooter}>
            {detail && (
              <button
                type="button"
                className={styles.linkBtn}
                onClick={() => onViewWebhooks(detail.id)}
              >
                <ExternalLinkIcon size={14} />
                <span>{t('premiumTransactions.detailModal.viewWebhooks')}</span>
              </button>
            )}
            <div className={styles.footerRight}>
              {detail?.status === 'PENDING' && (
                <button
                  type="button"
                  className={styles.submitBtn}
                  onClick={() => onOpenAction(detail, 'confirm')}
                >
                  <CheckCircleIcon size={15} />
                  <span>{t('premiumTransactions.actions.confirm')}</span>
                </button>
              )}
              {detail?.status === 'SUCCESS' && (
                <button
                  type="button"
                  className={styles.dangerSubmitBtn}
                  onClick={() => onOpenAction(detail, 'refund')}
                >
                  <ArrowDownRightIcon size={15} />
                  <span>{t('premiumTransactions.actions.refund')}</span>
                </button>
              )}
              <button type="button" className={styles.cancelBtn} onClick={onClose}>
                {t('premiumTransactions.detailModal.close')}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

TransactionDetailModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  transactionId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  onClose: PropTypes.func.isRequired,
  onOpenAction: PropTypes.func.isRequired,
  onViewWebhooks: PropTypes.func.isRequired,
};
