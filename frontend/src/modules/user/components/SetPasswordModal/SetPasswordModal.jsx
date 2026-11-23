import { useCallback, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import Input from '../../../../components/Input/Input';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import { changePasswordApi, setPasswordApi } from '../../profileApi';
import './SetPasswordModal.css';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!]).{8,}$/;

function SetPasswordModal({ isOpen, hasPassword = false, onClose, onSuccess }) {
  const { t } = useTranslation(['user', 'api', 'common']);
  const [form, setForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    setForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
    setErrors({});
    setGeneralError('');
    setIsSubmitting(false);
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  const handleClose = useCallback(() => {
    if (isSubmitting) return;
    setErrors({});
    setGeneralError('');
    onClose?.();
  }, [isSubmitting, onClose]);

  useEffect(() => {
    if (!isOpen) return undefined;
    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !isSubmitting) handleClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isSubmitting, handleClose]);

  if (!isOpen) return null;

  const handleChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: '' }));
    setGeneralError('');
  };

  const validate = () => {
    const nextErrors = {};
    if (hasPassword && !form.oldPassword) {
      nextErrors.oldPassword = t('user:setPasswordModal.oldPasswordRequired');
    }
    if (!form.newPassword) {
      nextErrors.newPassword = t('user:setPasswordModal.newPasswordRequired');
    } else if (!PASSWORD_REGEX.test(form.newPassword)) {
      nextErrors.newPassword = t('user:setPasswordModal.newPasswordInvalidFormat');
    }
    if (!form.confirmPassword) {
      nextErrors.confirmPassword = t('user:setPasswordModal.confirmPasswordRequired');
    } else if (form.confirmPassword !== form.newPassword) {
      nextErrors.confirmPassword = t('user:setPasswordModal.confirmPasswordMismatch');
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting) return;
    setGeneralError('');
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const response = hasPassword
        ? await changePasswordApi({
            oldPassword: form.oldPassword,
            newPassword: form.newPassword,
          })
        : await setPasswordApi(form.newPassword);
      if (response?.success) {
        onSuccess?.();
        onClose?.();
      } else {
        const errorCode = response?.errors?.[0]?.code || response?.code;
        const fallbackMessage = response?.errors?.[0]?.message || response?.message || t('api:common.UNKNOWN_ERROR');
        setGeneralError(errorCode
          ? t(`api:error.${errorCode}`, { defaultValue: fallbackMessage })
          : fallbackMessage);
      }
    } catch (error) {
      const errorData = error?.response?.data;
      const errorCode = errorData?.errors?.[0]?.code || errorData?.code;
      const fallbackMessage = errorData?.errors?.[0]?.message || errorData?.message || t('api:common.UNKNOWN_ERROR');
      setGeneralError(errorCode
        ? t(`api:error.${errorCode}`, { defaultValue: fallbackMessage })
        : fallbackMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return createPortal(
    <div className="set-password-modal-overlay">
      <div className="set-password-modal-container glass-modal" role="dialog" onClick={(event) => event.stopPropagation()}>
        <header className="set-password-modal-header">
          <h2 className="set-password-modal-title text-headline-sm">
            {t(hasPassword ? 'user:setPasswordModal.changeTitle' : 'user:setPasswordModal.title')}
          </h2>
          <button type="button" className="btn-icon set-password-modal-close" onClick={handleClose} disabled={isSubmitting}>
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>
        <p className="set-password-modal-description">
          {t(hasPassword ? 'user:setPasswordModal.changeDescription' : 'user:setPasswordModal.description')}
        </p>
        {generalError && <FormAlert message={generalError} />}
        <form className="set-password-modal-form" onSubmit={handleSubmit} noValidate>
          {hasPassword && (
            <Input
              id="set-password-old-password"
              label={t('user:setPasswordModal.oldPassword')}
              type="password"
              value={form.oldPassword}
              onChange={(event) => handleChange('oldPassword', event.target.value)}
              placeholder={t('user:setPasswordModal.oldPasswordPlaceholder')}
              error={errors.oldPassword}
              disabled={isSubmitting}
            />
          )}
          <Input
            id="set-password-new-password"
            label={t('user:setPasswordModal.newPassword')}
            type="password"
            value={form.newPassword}
            onChange={(event) => handleChange('newPassword', event.target.value)}
            placeholder={t('user:setPasswordModal.newPasswordPlaceholder')}
            error={errors.newPassword}
            disabled={isSubmitting}
          />
          <Input
            id="set-password-confirm-password"
            label={t('user:setPasswordModal.confirmPassword')}
            type="password"
            value={form.confirmPassword}
            onChange={(event) => handleChange('confirmPassword', event.target.value)}
            placeholder={t('user:setPasswordModal.confirmPasswordPlaceholder')}
            error={errors.confirmPassword}
            disabled={isSubmitting}
          />
          <div className="set-password-modal-actions">
            <button type="button" className="btn-secondary" onClick={handleClose} disabled={isSubmitting}>
              {t('common:cancel')}
            </button>
            <button type="submit" className="btn-primary set-password-submit-btn" disabled={isSubmitting}>
              {isSubmitting
                ? t('common:saving')
                : t('common:saveChanges')}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}

export default SetPasswordModal;
