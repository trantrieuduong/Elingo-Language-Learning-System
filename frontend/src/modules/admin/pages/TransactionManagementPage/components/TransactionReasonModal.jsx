import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminModal from '../../../components/AdminModal/AdminModal';
import { CheckCircleIcon, ArrowDownRightIcon } from '../../../components/icons/AdminIcons';
import { confirmTransaction, refundTransaction } from '../../../services/adminPremiumApi';
import modalStyles from '../../../components/AdminModal/AdminModal.module.css';
import styles from './TransactionModal.module.css';

export default function TransactionReasonModal({
  isOpen,
  transaction,
  mode,
  onClose,
  onSuccess,
}) {
  const { t } = useTranslation('admin');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (isOpen) {
      setReason('');
      setErrorMsg('');
    }
  }, [isOpen, transaction, mode]);

  if (!isOpen || !transaction) return null;

  const isConfirm = mode === 'confirm';

  const handleSubmit = async (e) => {
    e.preventDefault();
    const trimmed = reason.trim();
    if (!trimmed) {
      setErrorMsg(t('premiumTransactions.reasonModal.reasonRequired'));
      return;
    }
    if (trimmed.length > 500) {
      setErrorMsg(t('premiumTransactions.reasonModal.reasonMaxLength'));
      return;
    }

    try {
      setSubmitting(true);
      setErrorMsg('');
      const updated = isConfirm
        ? await confirmTransaction(transaction.id, trimmed)
        : await refundTransaction(transaction.id, trimmed);
      onSuccess(updated, mode);
      onClose();
    } catch (err) {
      const code = err?.response?.data?.error?.code;
      if (code === 'PAYMENT_TRANSACTION_ALREADY_PROCESSED') {
        setErrorMsg(t('premiumTransactions.errors.alreadyProcessed'));
      } else if (code === 'PAYMENT_TRANSACTION_NOT_REFUNDABLE') {
        setErrorMsg(t('premiumTransactions.errors.notRefundable'));
      } else if (code === 'PAYMENT_TRANSACTION_NOT_FOUND') {
        setErrorMsg(t('premiumTransactions.errors.notFound'));
      } else {
        setErrorMsg(err?.response?.data?.error?.message || t('premiumTransactions.errors.generic'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AdminModal
      isOpen={isOpen}
      onClose={onClose}
      closeDisabled={submitting}
      closeLabel={t('premiumTransactions.reasonModal.cancel')}
      titleId="tx-reason-title"
      title={
        isConfirm
          ? t('premiumTransactions.reasonModal.confirmTitle')
          : t('premiumTransactions.reasonModal.refundTitle')
      }
      subtitle={
        isConfirm
          ? t('premiumTransactions.reasonModal.confirmSubtitle')
          : t('premiumTransactions.reasonModal.refundSubtitle')
      }
      icon={
        isConfirm ? (
          <CheckCircleIcon size={20} className={modalStyles.iconGreen} />
        ) : (
          <ArrowDownRightIcon size={20} className={modalStyles.iconRed} />
        )
      }
    >
      <form onSubmit={handleSubmit} className={styles.modalBody}>
        <div className={styles.targetCard}>
          <span className={styles.targetLabel}>
            {t('premiumTransactions.reasonModal.targetLabel')}
          </span>
          <strong className={styles.targetValue}>
            #{transaction.id} — {transaction.providerRefCode || '—'} ({transaction.username})
          </strong>
        </div>

        <div className={isConfirm ? styles.noticeBox : styles.warningBox}>
          {isConfirm
            ? t('premiumTransactions.reasonModal.confirmNotice')
            : t('premiumTransactions.reasonModal.refundWarning')}
        </div>

        <div className={styles.formField}>
          <label htmlFor="tx-reason-input" className={styles.label}>
            {t('premiumTransactions.reasonModal.reasonLabel')}{' '}
            <span className={styles.required}>*</span>
          </label>
          <textarea
            id="tx-reason-input"
            rows={4}
            maxLength={500}
            className={styles.textarea}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder={t('premiumTransactions.reasonModal.reasonPlaceholder')}
          />
          <div className={styles.charCounter}>
            {reason.length}/500 {t('premiumTransactions.reasonModal.charCount')}
          </div>
        </div>

        {errorMsg && <div className={styles.errorText}>{errorMsg}</div>}

        <div className={styles.modalFooter}>
          <div className={styles.footerRight}>
            <button
              type="button"
              className={styles.cancelBtn}
              onClick={onClose}
              disabled={submitting}
            >
              {t('premiumTransactions.reasonModal.cancel')}
            </button>
            <button
              type="submit"
              className={isConfirm ? styles.submitBtn : styles.dangerSubmitBtn}
              disabled={submitting}
            >
              {submitting
                ? t('premiumPlans.formModal.saving')
                : isConfirm
                  ? t('premiumTransactions.reasonModal.confirmBtn')
                  : t('premiumTransactions.reasonModal.refundBtn')}
            </button>
          </div>
        </div>
      </form>
    </AdminModal>
  );
}

TransactionReasonModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  transaction: PropTypes.object,
  mode: PropTypes.oneOf(['confirm', 'refund']),
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
