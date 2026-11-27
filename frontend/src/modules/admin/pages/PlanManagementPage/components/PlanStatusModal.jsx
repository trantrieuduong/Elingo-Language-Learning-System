import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminModal from '../../../components/AdminModal/AdminModal';
import { CheckCircleIcon, XCircleIcon } from '../../../components/icons/AdminIcons';
import { enablePlan, disablePlan } from '../../../services/adminPremiumApi';
import modalStyles from '../../../components/AdminModal/AdminModal.module.css';
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
    <AdminModal
      isOpen={isOpen}
      onClose={onClose}
      closeDisabled={submitting}
      closeLabel={t('premiumPlans.statusModal.cancel')}
      titleId="plan-status-title"
      title={
        isEnable
          ? t('premiumPlans.statusModal.enableTitle')
          : t('premiumPlans.statusModal.disableTitle')
      }
      subtitle={
        isEnable
          ? t('premiumPlans.statusModal.enableSubtitle')
          : t('premiumPlans.statusModal.disableSubtitle')
      }
      icon={
        isEnable ? (
          <CheckCircleIcon size={20} className={modalStyles.iconGreen} />
        ) : (
          <XCircleIcon size={20} className={modalStyles.iconRed} />
        )
      }
      footer={
        <>
          <button
            type="button"
            className={styles.cancelBtn}
            onClick={onClose}
            disabled={submitting}
          >
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
        </>
      }
    >
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
      </div>
    </AdminModal>
  );
}

PlanStatusModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  plan: PropTypes.object,
  targetAction: PropTypes.oneOf(['enable', 'disable']),
  onClose: PropTypes.func.isRequired,
  onToggled: PropTypes.func.isRequired,
};
