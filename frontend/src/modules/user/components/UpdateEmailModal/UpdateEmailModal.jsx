import { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import Input from '../../../../components/Input/Input';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import { sendOtpUpdateEmailApi, updateEmailApi } from '../../profileApi';
import './UpdateEmailModal.css';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const OTP_RESEND_COOLDOWN = Number(import.meta.env.VITE_OTP_RESEND_COOLDOWN);

function UpdateEmailModal({ isOpen, onClose, onSuccess, onOtpSent }) {
  const { t } = useTranslation(['user', 'api', 'common']);

  const [form, setForm] = useState({
    newEmail: '',
    newEmailOtp: '',
    password: '',
  });
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [isSendingOtp, setIsSendingOtp] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isOtpSent, setIsOtpSent] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);

  // Reset form khi modal mở
  useEffect(() => {
    if (!isOpen) return;

    setForm({
      newEmail: '',
      newEmailOtp: '',
      password: '',
    });
    setErrors({});
    setGeneralError('');
    setIsSendingOtp(false);
    setIsSubmitting(false);
    setIsOtpSent(false);
    setResendCooldown(0);
  }, [isOpen]);

  // Đếm ngược cooldown gửi OTP
  useEffect(() => {
    if (resendCooldown <= 0) return undefined;
    const timer = setTimeout(() => setResendCooldown((sec) => sec - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendCooldown]);

  // Khóa scroll body khi mở modal
  useEffect(() => {
    if (!isOpen) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  // Đóng modal khi nhấn Escape
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen && !isSubmitting && !isSendingOtp) {
        handleClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isSubmitting, isSendingOtp]);

  const handleClose = useCallback(() => {
    if (isSubmitting || isSendingOtp) return;
    setErrors({});
    setGeneralError('');
    onClose?.();
  }, [isSubmitting, isSendingOtp, onClose]);

  if (!isOpen) return null;

  const handleChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }));
    }
    if (generalError) setGeneralError('');
  };

  const validateEmail = () => {
    const trimmedEmail = form.newEmail.trim();
    if (!trimmedEmail) {
      const msg = t('user:updateEmailModal.errorEmailRequired');
      setGeneralError(msg);
      return false;
    }
    if (!EMAIL_REGEX.test(trimmedEmail)) {
      const msg = t('user:updateEmailModal.errorEmailInvalid');
      setGeneralError(msg);
      return false;
    }
    return true;
  };

  const handleSendOtp = async () => {
    if (isSendingOtp || isSubmitting) return;
    setGeneralError('');
    if (!validateEmail()) return;

    setIsSendingOtp(true);
    try {
      const res = await sendOtpUpdateEmailApi(form.newEmail.trim());
      if (res?.success) {
        setIsOtpSent(true);
        setResendCooldown(OTP_RESEND_COOLDOWN);
        onOtpSent?.();
      } else {
        const errorCode = res?.errors?.[0]?.code || res?.code;
        const fallbackMsg = res?.errors?.[0]?.message || res?.message || t('api:common.UNKNOWN_ERROR');
        const translatedMsg = errorCode ? t(`api:error.${errorCode}`, { defaultValue: fallbackMsg }) : fallbackMsg;
        setGeneralError(translatedMsg);
      }
    } catch (err) {
      const errorData = err?.response?.data;
      const errorCode = errorData?.errors?.[0]?.code || errorData?.code;
      const fallbackMsg = errorData?.errors?.[0]?.message || errorData?.message || t('api:common.UNKNOWN_ERROR');
      const translatedMsg = errorCode ? t(`api:error.${errorCode}`, { defaultValue: fallbackMsg }) : fallbackMsg;
      setGeneralError(translatedMsg);
    } finally {
      setIsSendingOtp(false);
    }
  };

  const validateSubmit = () => {
    const trimmedEmail = form.newEmail.trim();
    const trimmedOtp = form.newEmailOtp.trim();
    const trimmedPassword = form.password.trim();

    if (!trimmedEmail) {
      setGeneralError(t('user:updateEmailModal.errorEmailRequired'));
      return false;
    }
    if (!EMAIL_REGEX.test(trimmedEmail)) {
      setGeneralError(t('user:updateEmailModal.errorEmailInvalid'));
      return false;
    }

    if (!trimmedOtp) {
      setGeneralError(t('user:updateEmailModal.errorOtpRequired'));
      return false;
    }

    if (!trimmedPassword) {
      setGeneralError(t('user:updateEmailModal.errorPasswordRequired'));
      return false;
    }

    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting || isSendingOtp) return;

    setGeneralError('');
    if (!validateSubmit()) return;

    setIsSubmitting(true);
    try {
      const payload = {
        newEmail: form.newEmail.trim(),
        newEmailOtp: form.newEmailOtp.trim(),
        password: form.password.trim(),
      };

      const res = await updateEmailApi(payload);
      if (res?.success) {
        onSuccess?.(payload.newEmail);
        handleClose();
      } else {
        const errorCode = res?.errors?.[0]?.code || res?.code;
        const fallbackMsg = res?.errors?.[0]?.message || res?.message || t('api:common.UNKNOWN_ERROR');
        const translatedMsg = errorCode ? t(`api:error.${errorCode}`, { defaultValue: fallbackMsg }) : fallbackMsg;
        setGeneralError(translatedMsg);
      }
    } catch (err) {
      const errorData = err?.response?.data;
      const errorCode = errorData?.errors?.[0]?.code || errorData?.code;
      const fallbackMsg = errorData?.errors?.[0]?.message || errorData?.message || t('api:common.UNKNOWN_ERROR');
      const translatedMsg = errorCode ? t(`api:error.${errorCode}`, { defaultValue: fallbackMsg }) : fallbackMsg;
      setGeneralError(translatedMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatCooldown = () => {
    const m = Math.floor(resendCooldown / 60);
    const s = resendCooldown % 60;
    return `${m > 0 ? `${m}:` : ''}${s < 10 && m > 0 ? `0${s}` : s}s`;
  };

  return createPortal(
    <div className="update-email-modal-overlay">
      <div
        className="update-email-modal-container glass-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
      >
        <header className="update-email-modal-header">
          <h2 id="update-email-modal-title" className="update-email-modal-title text-headline-sm">
            {t('user:updateEmailModal.title')}
          </h2>
          <button
            type="button"
            className="btn-icon update-email-modal-close"
            onClick={handleClose}
            disabled={isSubmitting || isSendingOtp}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        {generalError && <FormAlert message={generalError} />}

        <form className="update-email-modal-form" onSubmit={handleSubmit} noValidate>
          {/* New Email Field with embedded Send OTP button */}
          <Input
            id="update-email-new-email"
            label={t('user:updateEmailModal.newEmail')}
            type="email"
            value={form.newEmail}
            onChange={(e) => handleChange('newEmail', e.target.value)}
            placeholder={t('user:updateEmailModal.newEmailPlaceholder')}
            error={errors.newEmail}
            disabled={isSubmitting || isSendingOtp}
            rightElement={
              <button
                type="button"
                className="update-email-send-otp-btn"
                onClick={handleSendOtp}
                disabled={isSendingOtp || isSubmitting || resendCooldown > 0}
              >
                <span className="material-symbols-outlined">send</span>
                <span>
                  {isSendingOtp
                    ? t('user:updateEmailModal.sendingOtp')
                    : resendCooldown > 0
                      ? t('user:updateEmailModal.resendOtpTimer', { time: formatCooldown() })
                      : isOtpSent
                        ? t('user:updateEmailModal.resendOtp')
                        : t('user:updateEmailModal.sendOtp')}
                </span>
              </button>
            }
          />

          {/* OTP Field */}
          <Input
            id="update-email-otp"
            label={t('user:updateEmailModal.otp')}
            type="text"
            inputMode="numeric"
            maxLength={6}
            value={form.newEmailOtp}
            onChange={(e) => handleChange('newEmailOtp', e.target.value)}
            placeholder={t('user:updateEmailModal.otpPlaceholder')}
            error={errors.newEmailOtp}
            disabled={isSubmitting}
          />

          {/* Password Confirmation Field */}
          <Input
            id="update-email-password"
            label={t('user:updateEmailModal.password')}
            type="password"
            value={form.password}
            onChange={(e) => handleChange('password', e.target.value)}
            placeholder={t('user:updateEmailModal.passwordPlaceholder')}
            error={errors.password}
            disabled={isSubmitting}
          />

          {/* Modal Actions */}
          <div className="update-email-modal-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={handleClose}
              disabled={isSubmitting || isSendingOtp}
            >
              {t('common:cancel')}
            </button>
            <button
              type="submit"
              className="btn-primary update-email-submit-btn"
              disabled={isSubmitting || isSendingOtp}
            >
              {isSubmitting ? (
                <>
                  <span className="update-email-spinner" />
                  <span>{t('common:saving')}</span>
                </>
              ) : (
                t('common:saveChanges')
              )}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}

export default UpdateEmailModal;
