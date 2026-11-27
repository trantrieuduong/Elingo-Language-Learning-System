import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import styles from './WebhookLogFilterBar.module.css';

export default function WebhookLogFilterBar({
  providerId,
  onProviderIdChange,
  transactionId,
  onTransactionIdChange,
  status,
  onStatusChange,
  signatureValid,
  onSignatureValidChange,
  receivedFrom,
  onReceivedFromChange,
  receivedTo,
  onReceivedToChange,
  sort,
  onSortChange,
  filterError,
  onApply,
  onReset,
}) {
  const { t } = useTranslation('admin');

  return (
    <form className={styles.filterCard} onSubmit={onApply}>
      <div className={styles.filterRow}>
        <input
          type="number"
          min="1"
          step="1"
          className={styles.smallInput}
          value={providerId}
          onChange={(e) => onProviderIdChange(e.target.value)}
          placeholder={t('premiumWebhookLogs.filter.providerIdPlaceholder')}
          aria-label={t('premiumWebhookLogs.filter.providerIdPlaceholder')}
        />

        <input
          type="number"
          min="1"
          step="1"
          className={styles.smallInput}
          value={transactionId}
          onChange={(e) => onTransactionIdChange(e.target.value)}
          placeholder={t('premiumWebhookLogs.filter.transactionIdPlaceholder')}
          aria-label={t('premiumWebhookLogs.filter.transactionIdPlaceholder')}
        />

        <select
          className={styles.selectInput}
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          aria-label={t('premiumWebhookLogs.columns.status')}
        >
          <option value="">{t('premiumWebhookLogs.filter.allStatus')}</option>
          <option value="RECEIVED">{t('premiumWebhookLogs.status.RECEIVED')}</option>
          <option value="PROCESSED">{t('premiumWebhookLogs.status.PROCESSED')}</option>
          <option value="IGNORED">{t('premiumWebhookLogs.status.IGNORED')}</option>
          <option value="FAILED">{t('premiumWebhookLogs.status.FAILED')}</option>
        </select>

        <select
          className={styles.selectInput}
          value={signatureValid}
          onChange={(e) => onSignatureValidChange(e.target.value)}
          aria-label={t('premiumWebhookLogs.columns.signature')}
        >
          <option value="">{t('premiumWebhookLogs.filter.allSignature')}</option>
          <option value="true">{t('premiumWebhookLogs.filter.signatureValid')}</option>
          <option value="false">{t('premiumWebhookLogs.filter.signatureInvalid')}</option>
        </select>
      </div>

      <div className={styles.filterRow}>
        <div className={styles.dateGroup}>
          <label className={styles.dateLabel}>
            <span>{t('premiumWebhookLogs.filter.fromDate')}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={receivedFrom}
              onChange={(e) => onReceivedFromChange(e.target.value)}
            />
          </label>
          <label className={styles.dateLabel}>
            <span>{t('premiumWebhookLogs.filter.toDate')}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={receivedTo}
              onChange={(e) => onReceivedToChange(e.target.value)}
            />
          </label>
        </div>

        <select
          className={styles.selectInput}
          value={sort}
          onChange={(e) => onSortChange(e.target.value)}
          aria-label={t('premiumWebhookLogs.filter.sortLabel')}
        >
          <option value="receivedAt,desc">{t('premiumWebhookLogs.filter.sortReceivedDesc')}</option>
          <option value="receivedAt,asc">{t('premiumWebhookLogs.filter.sortReceivedAsc')}</option>
          <option value="processedAt,desc">{t('premiumWebhookLogs.filter.sortProcessedDesc')}</option>
        </select>

        <div className={styles.buttonGroup}>
          <button type="submit" className={styles.applyBtn}>
            {t('premiumWebhookLogs.filter.apply')}
          </button>
          <button type="button" className={styles.resetBtn} onClick={onReset}>
            {t('premiumWebhookLogs.filter.reset')}
          </button>
        </div>
      </div>

      {filterError && <div className={styles.errorBanner}>{filterError}</div>}
    </form>
  );
}

WebhookLogFilterBar.propTypes = {
  providerId: PropTypes.string.isRequired,
  onProviderIdChange: PropTypes.func.isRequired,
  transactionId: PropTypes.string.isRequired,
  onTransactionIdChange: PropTypes.func.isRequired,
  status: PropTypes.string.isRequired,
  onStatusChange: PropTypes.func.isRequired,
  signatureValid: PropTypes.string.isRequired,
  onSignatureValidChange: PropTypes.func.isRequired,
  receivedFrom: PropTypes.string.isRequired,
  onReceivedFromChange: PropTypes.func.isRequired,
  receivedTo: PropTypes.string.isRequired,
  onReceivedToChange: PropTypes.func.isRequired,
  sort: PropTypes.string.isRequired,
  onSortChange: PropTypes.func.isRequired,
  filterError: PropTypes.string,
  onApply: PropTypes.func.isRequired,
  onReset: PropTypes.func.isRequired,
};
