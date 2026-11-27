import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, CrownIcon } from '../../../components/icons/AdminIcons';
import { createPlan, updatePlan } from '../../../services/adminPremiumApi';
import styles from './PlanModal.module.css';

export default function PlanFormModal({ isOpen, plan, onClose, onSaved }) {
  const { t } = useTranslation('admin');
  const isEdit = Boolean(plan);

  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [currency, setCurrency] = useState('VND');
  const [durationDays, setDurationDays] = useState('30');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setErrorMsg('');
    if (plan) {
      setName(plan.name || '');
      setPrice(plan.price !== undefined && plan.price !== null ? String(plan.price) : '');
      setCurrency(plan.currency || 'VND');
      setDurationDays(
        plan.durationDays !== undefined && plan.durationDays !== null
          ? String(plan.durationDays)
          : '30'
      );
    } else {
      setName('');
      setPrice('');
      setCurrency('VND');
      setDurationDays('30');
    }
  }, [isOpen, plan]);

  if (!isOpen) return null;

  const validate = () => {
    const trimmedName = name.trim();
    if (!trimmedName) {
      return t('premiumPlans.formModal.validation.nameRequired');
    }
    if (trimmedName.length > 150) {
      return t('premiumPlans.formModal.validation.nameMaxLength');
    }
    const parsedPrice = Number(price);
    if (price === '' || Number.isNaN(parsedPrice) || parsedPrice < 0) {
      return t('premiumPlans.formModal.validation.priceRequired');
    }
    if (!isEdit) {
      const trimmedCurrency = currency.trim();
      if (!trimmedCurrency) {
        return t('premiumPlans.formModal.validation.currencyRequired');
      }
      if (trimmedCurrency.length > 10) {
        return t('premiumPlans.formModal.validation.currencyMaxLength');
      }
      const parsedDuration = Number(durationDays);
      if (!Number.isInteger(parsedDuration) || parsedDuration <= 0) {
        return t('premiumPlans.formModal.validation.durationRequired');
      }
    }
    return '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setErrorMsg(validationError);
      return;
    }

    try {
      setSubmitting(true);
      setErrorMsg('');
      const saved = isEdit
        ? await updatePlan(plan.id, { name: name.trim(), price: Number(price) })
        : await createPlan({
            name: name.trim(),
            price: Number(price),
            currency: currency.trim().toUpperCase(),
            durationDays: Number(durationDays),
          });
      onSaved(saved, isEdit);
      onClose();
    } catch (err) {
      const code = err?.response?.data?.error?.code;
      if (code === 'PREMIUM_PLAN_NOT_FOUND') {
        setErrorMsg(t('premiumPlans.errors.notFound'));
      } else {
        setErrorMsg(err?.response?.data?.error?.message || t('premiumPlans.errors.generic'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="plan-form-title">
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            <CrownIcon size={20} className={styles.headerIcon} />
            <div>
              <h2 id="plan-form-title" className={styles.modalTitle}>
                {isEdit
                  ? t('premiumPlans.formModal.editTitle')
                  : t('premiumPlans.formModal.createTitle')}
              </h2>
              <p className={styles.modalSubtitle}>
                {isEdit
                  ? t('premiumPlans.formModal.editSubtitle')
                  : t('premiumPlans.formModal.createSubtitle')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('premiumPlans.formModal.cancel')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className={styles.modalBody}>
          <div className={styles.noticeBox}>
            {isEdit
              ? t('premiumPlans.formModal.editNotice')
              : t('premiumPlans.formModal.createNotice')}
          </div>

          <div className={styles.formField}>
            <label htmlFor="plan-name-input" className={styles.label}>
              {t('premiumPlans.formModal.nameLabel')} <span className={styles.required}>*</span>
            </label>
            <input
              id="plan-name-input"
              type="text"
              maxLength={150}
              className={styles.input}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t('premiumPlans.formModal.namePlaceholder')}
            />
          </div>

          <div className={styles.formField}>
            <label htmlFor="plan-price-input" className={styles.label}>
              {t('premiumPlans.formModal.priceLabel')} <span className={styles.required}>*</span>
            </label>
            <input
              id="plan-price-input"
              type="number"
              min="0"
              step="any"
              className={styles.input}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              placeholder={t('premiumPlans.formModal.pricePlaceholder')}
            />
          </div>

          <div className={styles.rowTwo}>
            <div className={styles.formField}>
              <label htmlFor="plan-currency-input" className={styles.label}>
                {t('premiumPlans.formModal.currencyLabel')} <span className={styles.required}>*</span>
              </label>
              <input
                id="plan-currency-input"
                type="text"
                maxLength={10}
                className={styles.input}
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
                disabled={isEdit}
                placeholder={t('premiumPlans.formModal.currencyPlaceholder')}
              />
            </div>

            <div className={styles.formField}>
              <label htmlFor="plan-duration-input" className={styles.label}>
                {t('premiumPlans.formModal.durationLabel')} <span className={styles.required}>*</span>
              </label>
              <input
                id="plan-duration-input"
                type="number"
                min="1"
                step="1"
                className={styles.input}
                value={durationDays}
                onChange={(e) => setDurationDays(e.target.value)}
                disabled={isEdit}
                placeholder={t('premiumPlans.formModal.durationPlaceholder')}
              />
            </div>
          </div>

          {errorMsg && <div className={styles.errorText}>{errorMsg}</div>}

          <div className={styles.modalFooter}>
            <button type="button" className={styles.cancelBtn} onClick={onClose} disabled={submitting}>
              {t('premiumPlans.formModal.cancel')}
            </button>
            <button type="submit" className={styles.submitBtn} disabled={submitting}>
              {submitting
                ? t('premiumPlans.formModal.saving')
                : isEdit
                ? t('premiumPlans.formModal.saveEdit')
                : t('premiumPlans.formModal.saveCreate')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

PlanFormModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  plan: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSaved: PropTypes.func.isRequired,
};
