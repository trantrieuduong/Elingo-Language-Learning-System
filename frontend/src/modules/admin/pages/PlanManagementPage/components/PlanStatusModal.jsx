import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, CheckCircleIcon, XCircleIcon } from '../../../components/icons/AdminIcons';
import { enablePlan, disablePlan } from '../../../services/adminPremiumApi';
import styles from './PlanModal.module.css';

export default function PlanStatusModal({
  isOpen,
  plan,
  targetAction,
  onClose,
  onToggled,
}) {
  const { t } = useTranslation('admin');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (isOpen) {
      setErrorMsg('');
    }
  }, [isOpen, plan, targetAction]);

  if (!isOpen || !plan) return null;

  const isEnable = targetAction === 'enable';

  const handleConfirm = async () => {
    try {
      setSubmitting(true);
      setErrorMsg('');
      const updated = isEnable ? await enablePlan(plan.id) : await disablePlan(plan.id);
      onToggled(updated, targetAction);
      onClose();
    } catch (err) {
      const code = err?.response?.data?.error?.code;
      if (code === 'PREMIUM_PLAN_ALREADY_ACTIVE') {
        setErrorMsg(t('premiumPlans.errors.alreadyActive'));
      } else if (code === 'PREMIUM_PLAN_ALREADY_INACTIVE') {
        setErrorMsg(t('premiumPlans.errors.alreadyInactive'));
      } else if (code === 'PREMIUM_PLAN_NOT_FOUND') {
        setErrorMsg(t('premiumPlans.errors.notFound'));
      } else {
        setErrorMsg(err?.response?.data?.error?.message || t('premiumPlans.errors.generic'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="plan-status-title">
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            {isEnable ? (
              <CheckCircleIcon size={20} className={styles.iconGreen} />
            ) : (
              <XCircleIcon size={20} className={styles.iconRed} />
            )}
            <div>
              <h2 id="plan-status-title" className={styles.modalTitle}>
                {isEnable
                  ? t('premiumPlans.statusModal.enableTitle')
                  : t('premiumPlans.statusModal.disableTitle')}
              </h2>
              <p className={styles.modalSubtitle}>
                {isEnable
                  ? t('premiumPlans.statusModal.enableSubtitle')
                  : t('premiumPlans.statusModal.disableSubtitle')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('premiumPlans.statusModal.cancel')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <div className={styles.modalBody}>
          <div className={styles.targetCard}>
            <span className={styles.targetLabel}>{t('premiumPlans.statusModal.targetLabel')}</span>
            <strong className={styles.targetValue}>
              #{plan.id} — {plan.name}
            </strong>
          </div>

          <p className={styles.confirmMessage}>
            {isEnable
              ? t('premiumPlans.statusModal.enableConfirm')
              : t('premiumPlans.statusModal.disableConfirm')}
          </p>

          <div className={isEnable ? styles.noticeBox : styles.warningBox}>
            {isEnable
              ? t('premiumPlans.statusModal.enableNotice')
              : t('premiumPlans.statusModal.disableNotice')}
          </div>

          {errorMsg && <div className={styles.errorText}>{errorMsg}</div>}

          <div className={styles.modalFooter}>
            <button type="button" className={styles.cancelBtn} onClick={onClose} disabled={submitting}>
              {t('premiumPlans.statusModal.cancel')}
            </button>
            <button
              type="button"
              className={isEnable ? styles.submitBtn : styles.dangerSubmitBtn}
              onClick={handleConfirm}
              disabled={submitting}
            >
              {submitting
                ? t('premiumPlans.formModal.saving')
                : isEnable
                ? t('premiumPlans.statusModal.enableBtn')
                : t('premiumPlans.statusModal.disableBtn')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

PlanStatusModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  plan: PropTypes.object,
  targetAction: PropTypes.oneOf(['enable', 'disable']),
  onClose: PropTypes.func.isRequired,
  onToggled: PropTypes.func.isRequired,
};
