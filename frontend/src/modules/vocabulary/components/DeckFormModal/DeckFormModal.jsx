import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import Input from '../../../../components/Input/Input';
import FormAlert from '../FormAlert/FormAlert';
import './DeckFormModal.css';

/** Modal tạo hoặc chỉnh sửa bộ từ cá nhân (UserDeckRequest). */
function DeckFormModal({
  isOpen,
  mode = 'create',
  initialData = null,
  onClose,
  onSubmit,
  isLoading = false,
}) {
  const { t } = useTranslation(['learning', 'api', 'common']);
  const isEdit = mode === 'edit';
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setTitle(isEdit ? initialData?.title || '' : '');
    setDescription(isEdit ? initialData?.description || '' : '');
    setErrors({});
    setGeneralError('');
  }, [isOpen, isEdit, initialData]);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !isLoading) onClose?.();
    };
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, isLoading, onClose]);

  const validateForm = () => {
    const nextErrors = {};
    if (!title.trim()) nextErrors.title = t('userDeck.form.titleRequired');
    else if (title.trim().length > 50) nextErrors.title = t('userDeck.form.titleMaxLength');
    if (description.trim().length > 255) {
      nextErrors.description = t('userDeck.form.descriptionMaxLength');
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const applyServerFieldErrors = (data) => {
    const serverErrors = Array.isArray(data?.errors) ? data.errors : [];
    const fieldErrors = serverErrors.reduce((result, error) => {
      if (error?.field === 'title' || error?.field === 'description') {
        result[error.field] = error.message;
      }
      return result;
    }, {});

    setErrors(fieldErrors);
    return Object.keys(fieldErrors).length > 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setGeneralError('');
    if (!validateForm()) return;
    try {
      const response = await onSubmit({ title: title.trim(), description: description.trim() });
      if (response && !response.success) {
        const hasFieldErrors = applyServerFieldErrors(response);
        const translated = response.code && t(`api:error.${response.code}`, { defaultValue: '' });
        setGeneralError(hasFieldErrors ? '' : translated || response.message || t('userDeck.form.saveError'));
      }
    } catch (error) {
      const responseData = error.response?.data;
      const hasFieldErrors = applyServerFieldErrors(responseData);
      const code = responseData?.code;
      const translated = code && t(`api:error.${code}`, { defaultValue: '' });
      setGeneralError(
        hasFieldErrors
          ? ''
          : translated || responseData?.message || error.message || t('userDeck.form.systemError')
      );
    }
  };

  if (!isOpen) return null;

  return createPortal(
    <div
      className="deck-form-overlay"
      onClick={() => !isLoading && onClose?.()}
    >
      <div className="deck-form-modal glass-capsule" onClick={(event) => event.stopPropagation()}>
        <header className="deck-form-header">
          <div className="deck-form-header__info">
            <span className="material-symbols-outlined deck-form-header__icon">
              {isEdit ? 'edit_document' : 'library_add'}
            </span>
            <div>
              <h2 id="deck-modal-title" className="text-headline-sm">
                {t(isEdit ? 'userDeck.form.editTitle' : 'userDeck.form.createTitle')}
              </h2>
              <p className="deck-form-header__subtitle text-body-sm">
                {t(isEdit ? 'userDeck.form.editSubtitle' : 'userDeck.form.createSubtitle')}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="btn-icon deck-form-header__btn-close"
            onClick={onClose}
            disabled={isLoading}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        <FormAlert message={generalError} />

        <form className="deck-form-body" onSubmit={handleSubmit} noValidate>
          <div className="deck-form-field">
            <Input
              id="deck-title-input"
              label={t('userDeck.form.titleLabel')}
              placeholder={t('userDeck.form.titlePlaceholder')}
              value={title}
              onChange={(event) => {
                setTitle(event.target.value);
                if (errors.title) setErrors((previous) => ({ ...previous, title: '' }));
              }}
              error={errors.title}
              disabled={isLoading}
              autoFocus
            />
          </div>
          <div className="deck-form-field">
            <label htmlFor="deck-desc-input" className="input-label">
              {t('userDeck.form.descriptionLabel')}
            </label>
            <textarea
              id="deck-desc-input"
              className={`input-field deck-form-textarea ${errors.description ? 'deck-form-textarea--error' : ''}`}
              rows={5}
              placeholder={t('userDeck.form.descriptionPlaceholder')}
              value={description}
              onChange={(event) => {
                setDescription(event.target.value);
                if (errors.description) setErrors((previous) => ({ ...previous, description: '' }));
              }}
              disabled={isLoading}
            />
            {errors.description && (
              <p className="input-error">{errors.description}</p>
            )}
          </div>
          <footer className="deck-form-actions">
            <button
              type="button"
              className="btn-secondary deck-form-cancel-btn"
              onClick={onClose}
              disabled={isLoading}
            >
              {t('common:cancel')}
            </button>
            <button type="submit" className="btn-primary deck-form-btn-submit" disabled={isLoading}>
              {isLoading ? (
                <>
                  <span className="app-loading-spinner deck-form-spinner" />
                  {t('common:saving')}
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined">
                    {isEdit ? 'save' : 'add'}
                  </span>
                  {t(isEdit ? 'common:saveChanges' : 'userDeck.form.createDeck')}
                </>
              )}
            </button>
          </footer>
        </form>
      </div>
    </div>,
    document.body
  );
}

export default DeckFormModal;
