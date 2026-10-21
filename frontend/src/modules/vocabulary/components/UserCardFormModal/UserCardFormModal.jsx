import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import Input from '../../../../components/Input/Input'
import './UserCardFormModal.css'

/**
 * UserCardFormModal — Modal tạo mới hoặc chỉnh sửa thẻ từ vựng (Card).
 * Trường dữ liệu khớp 100% với CreateUserCardRequest và UpdateUserCardRequest ở backend:
 * - topicId: ID chủ đề (chỉ gửi khi create)
 * - term: Từ vựng (bắt buộc, max 100 ký tự)
 * - pos: Từ loại (tùy chọn, max 50 ký tự)
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
  topics = [],
  isTopicLocked = false,
  onSubmit,
  onClose,
  isSubmitting = false,
}) {
  const { t } = useTranslation(['learning', 'api', 'common'])
  const isEdit = mode === 'edit'

  const [topicId, setTopicId] = useState('')
  const [term, setTerm] = useState('')
  const [pos, setPos] = useState('')
  const [translation, setTranslation] = useState('')
  const [explanationVi, setExplanationVi] = useState('')
  const [explanationEn, setExplanationEn] = useState('')
  const [examplesVi, setExamplesVi] = useState('')
  const [examplesEn, setExamplesEn] = useState('')
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')

  useEffect(() => {
    if (isOpen) {
      if (isEdit && card) {
        setTopicId(card.topicId ?? topics[0]?.id ?? '')
        setTerm(card.term || '')
        setPos(card.pos || '')
        setTranslation(card.translation || '')
        setExplanationVi(card.explanationVi || '')
        setExplanationEn(card.explanationEn || '')
        setExamplesVi(card.examplesVi || '')
        setExamplesEn(card.examplesEn || '')
      } else {
        setTopicId(topics[0]?.id ?? '')
        setTerm('')
        setPos('')
        setTranslation('')
        setExplanationVi('')
        setExplanationEn('')
        setExamplesVi('')
        setExamplesEn('')
      }
      setErrors({})
      setFormError('')
    }
  }, [isOpen, isEdit, card, topics])

  useEffect(() => {
    if (!isOpen) return undefined

    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !isSubmitting) onClose?.()
    }

    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [isOpen, isSubmitting, onClose])

  if (!isOpen) return null

  const validate = () => {
    const newErrors = {}
    if (!term.trim()) {
      newErrors.term = t('userCards.termRequired')
    } else if (term.trim().length > 100) {
      newErrors.term = t('userCards.termMaxLength')
    }

    if (!translation.trim()) {
      newErrors.translation = t('userCards.translationPlaceholder')
    } else if (translation.trim().length > 500) {
      newErrors.translation = t('userCards.translationMaxLength')
    }

    if (!isEdit && !topicId) {
      newErrors.topicId = t('userCards.topicRequired')
    }

    if (pos.trim().length > 50) {
      newErrors.pos = t('userCards.posMaxLength')
    }
    if (explanationVi.trim().length > 255) {
      newErrors.explanationVi = t('userCards.explanationMaxLength')
    }
    if (explanationEn.trim().length > 255) {
      newErrors.explanationEn = t('userCards.explanationMaxLength')
    }
    if (examplesVi.trim().length > 255) {
      newErrors.examplesVi = t('userCards.examplesMaxLength')
    }
    if (examplesEn.trim().length > 255) {
      newErrors.examplesEn = t('userCards.examplesMaxLength')
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return

    const payload = {
      term: term.trim(),
      pos: pos.trim() || null,
      translation: translation.trim(),
      explanationVi: explanationVi.trim() || null,
      explanationEn: explanationEn.trim() || null,
      examplesVi: examplesVi.trim() || null,
      examplesEn: examplesEn.trim() || null,
    }

    if (!isEdit) {
      payload.topicId = Number(topicId)
    }

    try {
      const res = await onSubmit?.(payload)
      if (res && !res.success) {
        const errorCode = res.code
        const msg = (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) || res.message || t('userCards.saveError')
        setFormError(msg)
      }
    } catch (err) {
      const errorCode = err.response?.data?.code
      const msg = (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) || err.response?.data?.message || err.message || t('userCards.saveError')
      setFormError(msg)
    }
  }

  return createPortal(
    <div
      className="modal-overlay user-card-form-overlay"
      onClick={() => !isSubmitting && onClose?.()}
      role="dialog"
      aria-modal="true"
      aria-labelledby="user-card-form-title"
    >
      <div
        className="user-card-form-modal glass-capsule glass-sheen"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="user-card-form-modal__header">
          <div className="user-card-form-modal__heading">
            <span className="user-card-form-modal__icon glass-thumb" aria-hidden="true">
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
            aria-label={t('common:close')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        {formError && (
          <div className="topic-form-error-toast" role="alert" style={{ margin: '0 0 var(--spacing-md)' }}>
            <span className="material-symbols-outlined" aria-hidden="true">error</span>
            {formError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="user-card-form-modal__body">
            <div className="user-card-form-modal__grid">
              {/* Chủ đề */}
              <div className={`user-card-form-modal__field ${isEdit ? 'user-card-form-modal__field--hidden' : ''}`}>
                <label className="input-label" htmlFor="user-card-topic">
                  {t('userCards.topicRequired')}
                </label>
                <select
                  className="user-card-form-modal__select"
                  id="user-card-topic"
                  value={topicId}
                  onChange={(e) => setTopicId(e.target.value)}
                  disabled={isEdit || isTopicLocked || isSubmitting}
                >
                  {topics.map((topic) => (
                    <option key={topic.id} value={topic.id}>{topic.name}</option>
                  ))}
                </select>
                {isEdit && (
                  <span className="user-card-form-modal__hint text-body-sm">
                    {t('userCards.topicEditHint')}
                  </span>
                )}
                {errors.topicId && (
                  <span className="input-error text-body-sm">{errors.topicId}</span>
                )}
              </div>

              {/* Từ loại (Part of Speech) */}
              <Input
                id="user-card-pos"
                label={t('userCards.partOfSpeech')}
                placeholder={t('userCards.partOfSpeechPlaceholder')}
                value={pos}
                onChange={(e) => {
                  setPos(e.target.value)
                  if (errors.pos) setErrors((prev) => ({ ...prev, pos: '' }))
                }}
                error={errors.pos}
                disabled={isSubmitting}
              />

              {/* Từ vựng (Term) */}
              <Input
                id="user-card-term"
                label={t('userCards.termRequired')}
                placeholder={t('userCards.termPlaceholder')}
                value={term}
                onChange={(e) => {
                  setTerm(e.target.value)
                  if (errors.term) setErrors((prev) => ({ ...prev, term: '' }))
                }}
                error={errors.term}
                disabled={isSubmitting}
                autoFocus
              />

              {/* Bản dịch (Translation) */}
              <Input
                id="user-card-translation"
                label={t('userCards.translation')}
                placeholder={t('userCards.translationPlaceholder')}
                value={translation}
                onChange={(e) => {
                  setTranslation(e.target.value)
                  if (errors.translation) setErrors((prev) => ({ ...prev, translation: '' }))
                }}
                error={errors.translation}
                disabled={isSubmitting}
              />
            </div>

            {/* Giải thích và Ví dụ */}
            <div className="user-card-form-modal__grid user-card-form-modal__grid--textareas">
              {/* Giải thích tiếng Việt */}
              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-explanation-vi">
                  {t('userCards.explanationVi')}
                </label>
                <textarea
                  className="user-card-form-modal__textarea"
                  id="user-card-explanation-vi"
                  rows="3"
                  placeholder={t('userCards.explanationPlaceholder')}
                  value={explanationVi}
                  onChange={(e) => {
                    setExplanationVi(e.target.value)
                    if (errors.explanationVi) setErrors((prev) => ({ ...prev, explanationVi: '' }))
                  }}
                  disabled={isSubmitting}
                />
                {errors.explanationVi && (
                  <span className="input-error text-body-sm">{errors.explanationVi}</span>
                )}
              </div>

              {/* Giải thích tiếng Anh */}
              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-explanation-en">
                  {t('userCards.explanationEn')}
                </label>
                <textarea
                  className="user-card-form-modal__textarea"
                  id="user-card-explanation-en"
                  rows="3"
                  placeholder={t('userCards.explanationPlaceholder')}
                  value={explanationEn}
                  onChange={(e) => {
                    setExplanationEn(e.target.value)
                    if (errors.explanationEn) setErrors((prev) => ({ ...prev, explanationEn: '' }))
                  }}
                  disabled={isSubmitting}
                />
                {errors.explanationEn && (
                  <span className="input-error text-body-sm">{errors.explanationEn}</span>
                )}
              </div>

              {/* Ví dụ tiếng Việt */}
              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-example-vi">
                  {t('userCards.examplesVi')}
                </label>
                <textarea
                  className="user-card-form-modal__textarea"
                  id="user-card-example-vi"
                  rows="2"
                  placeholder={t('userCards.examplePlaceholder')}
                  value={examplesVi}
                  onChange={(e) => {
                    setExamplesVi(e.target.value)
                    if (errors.examplesVi) setErrors((prev) => ({ ...prev, examplesVi: '' }))
                  }}
                  disabled={isSubmitting}
                />
                {errors.examplesVi && (
                  <span className="input-error text-body-sm">{errors.examplesVi}</span>
                )}
              </div>

              {/* Ví dụ tiếng Anh */}
              <div className="user-card-form-modal__field">
                <label className="input-label" htmlFor="user-card-example-en">
                  {t('userCards.examplesEn')}
                </label>
                <textarea
                  className="user-card-form-modal__textarea"
                  id="user-card-example-en"
                  rows="2"
                  placeholder={t('userCards.examplePlaceholder')}
                  value={examplesEn}
                  onChange={(e) => {
                    setExamplesEn(e.target.value)
                    if (errors.examplesEn) setErrors((prev) => ({ ...prev, examplesEn: '' }))
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
            <button
              type="submit"
              className="btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <span className="app-loading-spinner" style={{ width: 18, height: 18 }} />
                  {t('common:saving')}
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined" aria-hidden="true">
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
  )
}

export default UserCardFormModal
