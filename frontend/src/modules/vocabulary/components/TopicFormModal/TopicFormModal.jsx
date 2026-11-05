import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import Input from '../../../../components/Input/Input';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import './TopicFormModal.css';

/**
 * @param {'create'|'edit'} mode
 */
function TopicFormModal({
  isOpen,
  mode = 'create',
  topic = null,
  onSubmit,
  onClose,
  isSubmitting = false,
}) {
  const { t } = useTranslation(['learning', 'api', 'common']);

  const [name, setName] = useState('');
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');

  // Nạp dữ liệu khi modal mở
  useEffect(() => {
    if (!isOpen) return;
    setName(mode === 'edit' && topic ? (topic.name ?? '') : '');
    setErrors({});
    setGeneralError('');
  }, [isOpen, mode, topic]);

  // Đóng modal khi nhấn Escape
  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === 'Escape' && isOpen) onClose?.();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen) return undefined;

    const previousBodyOverflow = document.body.style.overflow;
    const previousRootOverflow = document.documentElement.style.overflow;
    document.body.style.overflow = 'hidden';
    document.documentElement.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousBodyOverflow;
      document.documentElement.style.overflow = previousRootOverflow;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleNameChange = (e) => {
    const value = e.target.value;
    setName(value);
    if (errors.name) setErrors((prev) => ({ ...prev, name: '' }));
    if (generalError) setGeneralError('');
  };

  const validate = () => {
    const newErrors = {};
    const trimmedName = name.trim();
    if (!trimmedName) {
      newErrors.name = t('topic.errorNameRequired');
    } else if (trimmedName.length > 50) {
      newErrors.name = t('topic.errorNameTooLong');
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    if (!validate()) return;

    const payload = { name: name.trim() };

    try {
      const res = await onSubmit?.(payload);
      if (res && !res.success) {
        const errorCode = res.code;
        const msg =
          (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
          res.message ||
          t('topic.submitError');
        setGeneralError(msg);
      }
    } catch (err) {
      const responseData = err?.response?.data;
      const errorCode = responseData?.code;
      const msg =
        (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
        responseData?.message ||
        t('topic.submitError');
      setGeneralError(msg);
    }
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) onClose?.();
  };

  const isCreate = mode === 'create';

  return createPortal(
    <div className="topic-form-overlay" onClick={handleOverlayClick}>
      <div className="topic-form-modal glass-capsule">
        {/* Header */}
        <div className="topic-form-header">
          <div className="topic-form-header__icon glass-thumb">
            <span className="material-symbols-outlined">{isCreate ? 'add_circle' : 'edit'}</span>
          </div>
          <div>
            <h2 className="topic-form-header__title text-headline-sm">
              {isCreate ? t('topic.createTitle') : t('topic.editTitle')}
            </h2>
            <p className="topic-form-header__subtitle text-body-sm">
              {isCreate ? t('topic.createSubtitle') : t('topic.editSubtitle')}
            </p>
          </div>
          <button type="button" className="btn-icon topic-form-close" onClick={onClose}>
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <FormAlert message={generalError} />

        <form onSubmit={handleSubmit} noValidate className="topic-form-body">
          <Input
            id="topic-name"
            label={t('topic.labelName')}
            type="text"
            placeholder={t('topic.placeholderName')}
            value={name}
            onChange={handleNameChange}
            error={errors.name}
            disabled={isSubmitting}
            autoFocus
          />

          {/* Footer actions */}
          <div className="topic-form-footer">
            <button
              type="button"
              className="btn-secondary topic-form-cancel-btn"
              onClick={onClose}
              disabled={isSubmitting}
            >
              {t('common:cancel')}
            </button>
            <button type="submit" className="btn-primary" disabled={isSubmitting}>
              {isSubmitting ? (
                <>
                  <span className="topic-form-spinner" />
                  {t('common:saving')}
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined">
                    {isCreate ? 'add_circle' : 'save'}
                  </span>
                  {isCreate ? t('topic.submitCreate') : t('common:saveChanges')}
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}

export default TopicFormModal;
