import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminFilterBar, {
  AdminFilterNumber,
  AdminFilterSelect,
} from '../../../components/AdminFilterBar/AdminFilterBar';

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

  const statusOptions = [
    { value: '', label: t('premiumWebhookLogs.filter.allStatus') },
    { value: 'RECEIVED', label: t('premiumWebhookLogs.status.RECEIVED') },
    { value: 'PROCESSED', label: t('premiumWebhookLogs.status.PROCESSED') },
    { value: 'IGNORED', label: t('premiumWebhookLogs.status.IGNORED') },
    { value: 'FAILED', label: t('premiumWebhookLogs.status.FAILED') },
  ];

  const signatureOptions = [
    { value: '', label: t('premiumWebhookLogs.filter.allSignature') },
    { value: 'true', label: t('premiumWebhookLogs.filter.signatureValid') },
    { value: 'false', label: t('premiumWebhookLogs.filter.signatureInvalid') },
  ];

  const sortOptions = [
    { value: 'receivedAt,desc', label: t('premiumWebhookLogs.filter.sortReceivedDesc') },
    { value: 'receivedAt,asc', label: t('premiumWebhookLogs.filter.sortReceivedAsc') },
    { value: 'processedAt,desc', label: t('premiumWebhookLogs.filter.sortProcessedDesc') },
  ];

  return (
    <AdminFilterBar
      fromLabel={t('premiumWebhookLogs.filter.fromDate')}
      fromValue={receivedFrom}
      onFromChange={onReceivedFromChange}
      toLabel={t('premiumWebhookLogs.filter.toDate')}
      toValue={receivedTo}
      onToChange={onReceivedToChange}
      sortValue={sort}
      onSortChange={onSortChange}
      sortLabel={t('premiumWebhookLogs.filter.sortLabel')}
      sortOptions={sortOptions}
      applyLabel={t('premiumWebhookLogs.filter.apply')}
      resetLabel={t('premiumWebhookLogs.filter.reset')}
      filterError={filterError}
      errorVariant="banner"
      onApply={onApply}
      onReset={onReset}
    >
      <AdminFilterNumber
        value={providerId}
        onChange={onProviderIdChange}
        placeholder={t('premiumWebhookLogs.filter.providerIdPlaceholder')}
      />
      <AdminFilterNumber
        value={transactionId}
        onChange={onTransactionIdChange}
        placeholder={t('premiumWebhookLogs.filter.transactionIdPlaceholder')}
      />
      <AdminFilterSelect
        value={status}
        onChange={onStatusChange}
        ariaLabel={t('premiumWebhookLogs.columns.status')}
        options={statusOptions}
      />
      <AdminFilterSelect
        value={signatureValid}
        onChange={onSignatureValidChange}
        ariaLabel={t('premiumWebhookLogs.columns.signature')}
        options={signatureOptions}
      />
    </AdminFilterBar>
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
