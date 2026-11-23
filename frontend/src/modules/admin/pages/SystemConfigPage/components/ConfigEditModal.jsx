import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, ShieldCheckIcon } from '../../../components/icons/AdminIcons';
import { updateSystemConfig } from '../../../services/adminSystemConfigApi';
import styles from './ConfigEditModal.module.css';

export default function ConfigEditModal({ config, onClose, onSuccess }) {
  const { t } = useTranslation('admin');

  const [value, setValue] = useState(
    config.isSecret ? '********' : config.configValue || ''
  );
  const [description, setDescription] = useState(config.description || '');
  const [submitting, setSubmitting] = useState(false);
  const [validationError, setValidationError] = useState(null);
  const [apiError, setApiError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setValidationError(null);
    setApiError(null);

    // Validation
    if (value === null || value === undefined || value.trim() === '') {
      setValidationError(t('systemConfig.requiredValue'));
      return;
    }

    if (config.valueType === 'INT') {
      const parsed = Number(value.trim());
      if (!Number.isInteger(parsed)) {
        setValidationError(t('systemConfig.invalidInt'));
        return;
      }
    }

    try {
      setSubmitting(true);
      const payload = {
        value: value.trim(),
        description: description.trim(),
      };
      const updated = await updateSystemConfig(config.configKey, payload);
      onSuccess(updated);
    } catch (err) {
      setApiError(
        err.response?.data?.message || err.message || t('systemConfig.updateError')
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div
        className={styles.modal}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        {/* Header */}
        <div className={styles.header}>
          <div className={styles.headerText}>
            <h2 id="modal-title" className={styles.title}>
              {t('systemConfig.editModalTitle')}
            </h2>
            <p className={styles.subtitle}>
              {t('systemConfig.editModalSubtitle')}
            </p>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('common.close')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        {/* Read-only Info Card */}
        <div className={styles.infoCard}>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>{t('systemConfig.key')}:</span>
            <span className={styles.infoKey}>{config.configKey}</span>
          </div>
          <div className={styles.infoBadges}>
            <span className={styles.badge}>{config.module}</span>
            <span className={styles.badge}>{config.valueType}</span>
            {config.isSecret && (
              <span className={styles.secretBadge}>
                <ShieldCheckIcon size={12} />
                <span>{t('systemConfig.secret')}</span>
              </span>
            )}
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className={styles.form}>
          {/* Value field */}
          <div className={styles.formGroup}>
            <label htmlFor="config-value-input" className={styles.label}>
              {t('systemConfig.valueLabel')}{' '}
              <span className={styles.requiredStar}>*</span>
            </label>

            {config.valueType === 'BOOLEAN' ? (
              <select
                id="config-value-input"
                className={styles.select}
                value={value}
                onChange={(e) => setValue(e.target.value)}
              >
                <option value="true">true</option>
                <option value="false">false</option>
              </select>
            ) : config.valueType === 'INT' ? (
              <input
                id="config-value-input"
                type="number"
                step="1"
                className={styles.input}
                value={value}
                onChange={(e) => setValue(e.target.value)}
              />
            ) : (
              <input
                id="config-value-input"
                type="text"
                className={styles.input}
                value={value}
                onChange={(e) => setValue(e.target.value)}
              />
            )}

            {config.isSecret && (
              <p className={styles.hintText}>{t('systemConfig.secretHint')}</p>
            )}
          </div>

          {/* Description field */}
          <div className={styles.formGroup}>
            <label htmlFor="config-desc-input" className={styles.label}>
              {t('systemConfig.descriptionLabel')}
            </label>
            <textarea
              id="config-desc-input"
              rows={3}
              className={styles.textarea}
              placeholder={t('systemConfig.descriptionPlaceholder')}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          {/* Errors */}
          {validationError && (
            <div className={styles.errorBanner}>{validationError}</div>
          )}
          {apiError && <div className={styles.errorBanner}>{apiError}</div>}

          {/* Actions */}
          <div className={styles.actions}>
            <button
              type="button"
              className={styles.cancelBtn}
              onClick={onClose}
              disabled={submitting}
            >
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              className={styles.saveBtn}
              disabled={submitting}
            >
              {submitting ? t('feedback.loadingList') : t('common.save')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

ConfigEditModal.propTypes = {
  config: PropTypes.shape({
    configKey: PropTypes.string.isRequired,
    configValue: PropTypes.string,
    valueType: PropTypes.string.isRequired,
    isSecret: PropTypes.bool,
    module: PropTypes.string.isRequired,
    description: PropTypes.string,
  }).isRequired,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
