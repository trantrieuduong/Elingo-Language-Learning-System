import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import Input from '../../../../components/Input/Input';
import FormAlert from '../FormAlert/FormAlert';
import './UserCardFormModal.css';

const POS_OPTIONS = [
  'adjective',
  'adverb',
  'auxiliary verb',
  'collocation',
  'conjunction',
  'determiner',
  'idiom',
  'interjection',
  'modal verb',
  'noun',
  'phrasal verb',
  'phrase',
  'preposition',
  'pronoun',
  'verb',
];

/**
 * UserCardFormModal — Modal tạo mới hoặc chỉnh sửa thẻ từ vựng (Card).
 * Trường dữ liệu khớp 100% với CreateUserCardRequest và UpdateUserCardRequest ở backend:
 * - term: Từ vựng (bắt buộc, max 100 ký tự)
 * - pos: Từ loại (tùy chọn)
 * - translation: Bản dịch (bắt buộc, max 500 ký tự)
 * - explanationVi: Giải thích tiếng Việt (tùy chọn, max 255 ký tự)
 * - explanationEn: Giải thích tiếng Anh (tùy chọn, max 255 ký tự)
 * - examplesVi: Ví dụ tiếng Việt (tùy chọn, max 255 ký tự)
 * - examplesEn: Ví dụ tiếng Anh (tùy chọn, max 255 ký tự)
 */
function UserCardFormModal({
  isOpen,
  mode = 'create',
  card = null,
  onSubmit,
  onClose,
  isSubmitting = false,
}) {
  const { t } = useTranslation(['learning', 'api', 'common']);
  const isEdit = mode === 'edit';

  const [term, setTerm] = useState('');
  const [pos, setPos] = useState('');
  const [translation, setTranslation] = useState('');
  const [explanationVi, setExplanationVi] = useState('');
  const [explanationEn, setExplanationEn] = useState('');
  const [examplesVi, setExamplesVi] = useState('');
  const [examplesEn, setExamplesEn] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');

  useEffect(() => {
    if (isOpen) {
      if (isEdit && card) {
        setTerm(card.term || '');
        setPos(card.pos || '');
        setTranslation(card.translation || '');
        setExplanationVi(card.explanationVi || '');
        setExplanationEn(card.explanationEn || '');
        setExamplesVi(card.examplesVi || '');
        setExamplesEn(card.examplesEn || '');
      } else {
        setTerm('');
        setPos('');
        setTranslation('');
        setExplanationVi('');
        setExplanationEn('');
        setExamplesVi('');
        setExamplesEn('');
      }
      setErrors({});
      setFormError('');
    }
  }, [isOpen, isEdit, card]);

  useEffect(() => {
    if (!isOpen) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !isSubmitting) onClose?.();
    };

    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, isSubmitting, onClose]);

  if (!isOpen) return null;

  const validate = () => {
    const newErrors = {};
    if (!term.trim()) {
      newErrors.term = t('userCards.termRequired');
    } else if (term.trim().length > 100) {
      newErrors.term = t('userCards.termTooLong');
    }

    if (!translation.trim()) {
      newErrors.translation = t('userCards.translationRequired');
    } else if (translation.trim().length > 500) {
      newErrors.translation = t('userCards.translationTooLong');
    }

    if (explanationVi.trim().length > 255) {
      newErrors.explanationVi = t('userCards.explanationTooLong');
    }

    if (explanationEn.trim().length > 255) {
      newErrors.explanationEn = t('userCards.explanationTooLong');
    }

    if (examplesVi.trim().length > 255) {
      newErrors.examplesVi = t('userCards.exampleTooLong');
    }

    if (examplesEn.trim().length > 255) {
      newErrors.examplesEn = t('userCards.exampleTooLong');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    const payload = {
      term: term.trim(),
      pos: pos.trim() || null,
      translation: translation.trim(),
      explanationVi: explanationVi.trim() || null,
      explanationEn: explanationEn.trim() || null,
      examplesVi: examplesVi.trim() || null,
      examplesEn: examplesEn.trim() || null,
    };

    try {
      const res = await onSubmit?.(payload);
      if (res && !res.success) {
        const errorCode = res.code;
        const msg =
          (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
          res.message ||
          t('userCards.saveError');
        setFormError(msg);
      }
    } catch (err) {
      const errorCode = err.response?.data?.code;
      const msg =
        (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
        err.response?.data?.message ||
        err.message ||
        t('userCards.saveError');
      setFormError(msg);
    }
  };

  return createPortal(
    <div
      className="user-card-form-overlay"
      onClick={() => !isSubmitting && onClose?.()}
    >
      <div
        className="user-card-form-modal glass-capsule glass-sheen"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="user-card-form-modal__header">
          <div className="user-card-form-modal__heading">
            <span className="user-card-form-modal__icon glass-thumb">
              <span className="material-symbols-outlined">{isEdit ? 'edit_note' : 'note_add'}</span>
            </span>
            <div>
              <h2 id="user-card-form-title" className="text-headline-sm">
                {t(isEdit ? 'userCards.editTitle' : 'userCards.createTitle')}
              </h2>
              <p className="text-body-sm">{t('userCards.formSubtitle')}</p>
            </div>
          </div>
          <button
            type="button"
            className="btn-icon"
            onClick={onClose}
            disabled={isSubmitting}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        <FormAlert message={formError} />

        <form onSubmit={handleSubmit} noValidate>
          <div className="user-card-form-modal__body">
            <div className="user-card-form-modal__grid">
              <div className="user-card-form-modal__field">
                <Input
                  id="user-card-term"
                  label={t('userCards.termLabel')}
                  placeholder={t('userCards.termPlaceholder')}
                  value={term}
                  onChange={(e) => {
                    setTerm(e.target.value);
                    if (errors.term) setErrors((prev) => ({ ...prev, term: '' }));
                  }}
                  error={errors.term}
                  disabled={isSubmitting}
                  autoFocus
                />
              </div>

              <div className="user-card-form-modal__field">
                <Input
                  id="user-card-translation"
                  label={t('userCards.translation')}
                  placeholder={t('userCards.translationPlaceholder')}
                  value={translation}
                  onChange={(e) => {
                    setTranslation(e.target.value);
                    if (errors.translation) setErrors((prev) => ({ ...prev, translation: '' }));
                  }}
                  error={errors.translation}
                  disabled={isSubmitting}
                />
              </div>

              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-pos">
                  {t('userCards.partOfSpeech')}
                </label>
                <select
                  id="user-card-pos"
                  className="input-field"
                  value={pos}
                  onChange={(event) => {
                    setPos(event.target.value);
                  }}
                  disabled={isSubmitting}
                >
                  <option value="">{t('userCards.partOfSpeechPlaceholder')}</option>
                  {POS_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="user-card-form-modal__grid user-card-form-modal__grid--textareas">

              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-explanation-vi">
                  {t('userCards.explanationVi')}
                </label>
                <textarea
                  className="input-field user-card-form-modal__textarea"
                  id="user-card-explanation-vi"
                  rows="3"
                  placeholder={t('userCards.explanationPlaceholder')}
                  value={explanationVi}
                  onChange={(e) => {
                    setExplanationVi(e.target.value);
                    if (errors.explanationVi) setErrors((prev) => ({ ...prev, explanationVi: '' }));
                  }}
                  disabled={isSubmitting}
                />
                {errors.explanationVi && (
                  <span className="input-error text-body-sm">{errors.explanationVi}</span>
                )}
              </div>

              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-explanation-en">
                  {t('userCards.explanationEn')}
                </label>
                <textarea
                  className="input-field user-card-form-modal__textarea"
                  id="user-card-explanation-en"
                  rows="3"
                  placeholder={t('userCards.explanationPlaceholder')}
                  value={explanationEn}
                  onChange={(e) => {
                    setExplanationEn(e.target.value);
                    if (errors.explanationEn) setErrors((prev) => ({ ...prev, explanationEn: '' }));
                  }}
                  disabled={isSubmitting}
                />
                {errors.explanationEn && (
                  <span className="input-error text-body-sm">{errors.explanationEn}</span>
                )}
              </div>

  
              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-example-vi">
                  {t('userCards.examplesVi')}
                </label>
                <textarea
                  className="input-field user-card-form-modal__textarea"
                  id="user-card-example-vi"
                  rows="2"
                  placeholder={t('userCards.examplePlaceholder')}
                  value={examplesVi}
                  onChange={(e) => {
                    setExamplesVi(e.target.value);
                    if (errors.examplesVi) setErrors((prev) => ({ ...prev, examplesVi: '' }));
                  }}
                  disabled={isSubmitting}
                />
                {errors.examplesVi && (
                  <span className="input-error text-body-sm">{errors.examplesVi}</span>
                )}
              </div>

              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-example-en">
                  {t('userCards.examplesEn')}
                </label>
                <textarea
                  className="input-field user-card-form-modal__textarea"
                  id="user-card-example-en"
                  rows="2"
                  placeholder={t('userCards.examplePlaceholder')}
                  value={examplesEn}
                  onChange={(e) => {
                    setExamplesEn(e.target.value);
                    if (errors.examplesEn) setErrors((prev) => ({ ...prev, examplesEn: '' }));
                  }}
                  disabled={isSubmitting}
                />
                {errors.examplesEn && (
                  <span className="input-error text-body-sm">{errors.examplesEn}</span>
                )}
              </div>
            </div>
          </div>

          <div className="user-card-form-modal__actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={onClose}
              disabled={isSubmitting}
            >
              {t('common:cancel')}
            </button>
            <button type="submit" className="btn-primary" disabled={isSubmitting}>
              {isSubmitting ? (
                <>
                  <span className="app-loading-spinner user-card-form-modal__spinner" />
                  {t('common:saving')}
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined">
                    {isEdit ? 'save' : 'add'}
                  </span>
                  {isEdit ? t('common:saveChanges') : t('userCards.createCard')}
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

export default UserCardFormModal;
