import { useState, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import Input from '../../../../components/Input/Input'
import SlugRegenerateButton from '../SlugRegenerateButton/SlugRegenerateButton'
import { generateSlug, isValidSlug } from '../../userDeckMockData'
import './TopicFormModal.css'

/**
 * TopicFormModal — Modal tạo mới hoặc chỉnh sửa chủ đề (Topic).
 *
 * Create → gửi CreateTopicRequest { name, slug }
 * Edit   → gửi UpdateTopicRequest { name, slug, order }
 *
 * @param {boolean}  isOpen      - Hiển thị modal hay không
 * @param {'create'|'edit'} mode - Chế độ tạo mới hoặc chỉnh sửa
 * @param {Object}   topic       - Dữ liệu topic hiện tại (chỉ khi mode='edit')
 * @param {Function} onSubmit    - Callback (formData) khi form hợp lệ và được submit
 * @param {Function} onClose     - Callback đóng modal
 * @param {boolean}  isSubmitting - Trạng thái đang gửi request
 */
function TopicFormModal({ isOpen, mode = 'create', topic = null, onSubmit, onClose, isSubmitting = false }) {
  const { t } = useTranslation(['learning', 'common'])

  // ── Form state ──
  const [form, setForm] = useState({ name: '', slug: '', order: '' })
  const [errors, setErrors] = useState({})
  const [slugManuallyEdited, setSlugManuallyEdited] = useState(false)

  // Nạp dữ liệu khi modal mở
  useEffect(() => {
    if (!isOpen) return
    if (mode === 'edit' && topic) {
      setForm({
        name: topic.name ?? '',
        slug: topic.slug ?? '',
        order: topic.order != null ? String(topic.order) : '',
      })
      setSlugManuallyEdited(true) // slug đã có sẵn, không tự động ghi đè
    } else {
      setForm({ name: '', slug: '', order: '' })
      setSlugManuallyEdited(false)
    }
    setErrors({})
  }, [isOpen, mode, topic])

  // Đóng modal khi nhấn Escape
  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === 'Escape' && isOpen) onClose?.()
    }
    document.addEventListener('keydown', handleKey)
    return () => document.removeEventListener('keydown', handleKey)
  }, [isOpen, onClose])

  useEffect(() => {
    if (!isOpen) return undefined

    const previousBodyOverflow = document.body.style.overflow
    const previousRootOverflow = document.documentElement.style.overflow
    document.body.style.overflow = 'hidden'
    document.documentElement.style.overflow = 'hidden'

    return () => {
      document.body.style.overflow = previousBodyOverflow
      document.documentElement.style.overflow = previousRootOverflow
    }
  }, [isOpen])

  if (!isOpen) return null

  // ── Handlers ──
  const handleNameChange = (e) => {
    const name = e.target.value
    setForm((prev) => {
      const updated = { ...prev, name }
      if (!slugManuallyEdited) {
        updated.slug = generateSlug(name)
      }
      return updated
    })
    if (errors.name) setErrors((prev) => ({ ...prev, name: '' }))
  }

  const handleSlugChange = (e) => {
    setSlugManuallyEdited(true)
    setForm((prev) => ({ ...prev, slug: e.target.value }))
    if (errors.slug) setErrors((prev) => ({ ...prev, slug: '' }))
  }

  const handleRegenerateSlug = () => {
    setForm((prev) => ({ ...prev, slug: generateSlug(prev.name) }))
    setSlugManuallyEdited(false)
    if (errors.slug) setErrors((prev) => ({ ...prev, slug: '' }))
  }

  const handleOrderChange = (e) => {
    setForm((prev) => ({ ...prev, order: e.target.value }))
    if (errors.order) setErrors((prev) => ({ ...prev, order: '' }))
  }

  const validate = () => {
    const newErrors = {}
    if (!form.name.trim()) {
      newErrors.name = t('topic.errorNameRequired')
    }
    if (!form.slug.trim()) {
      newErrors.slug = t('topic.errorSlugRequired')
    } else if (!isValidSlug(form.slug.trim())) {
      newErrors.slug = t('topic.errorSlugInvalid')
    }
    if (mode === 'edit' && form.order !== '') {
      const orderNum = Number(form.order)
      if (!Number.isInteger(orderNum) || orderNum < 1) {
        newErrors.order = t('topic.errorOrderInvalid')
      }
    }
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!validate()) return

    const payload = { name: form.name.trim() }

    onSubmit?.(payload)
  }

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) onClose?.()
  }

  const isCreate = mode === 'create'

  return createPortal(
    <div className="topic-form-overlay" onClick={handleOverlayClick} role="dialog" aria-modal="true">
      <div className="topic-form-modal glass-capsule">
        {/* Header */}
        <div className="topic-form-header">
          <div className="topic-form-header__icon glass-thumb">
            <span className="material-symbols-outlined">
              {isCreate ? 'add_circle' : 'edit'}
            </span>
          </div>
          <div>
            <h2 className="topic-form-header__title text-headline-sm">
              {isCreate ? t('topic.createTitle') : t('topic.editTitle')}
            </h2>
            <p className="topic-form-header__subtitle text-body-sm">
              {isCreate ? t('topic.createSubtitle') : t('topic.editSubtitle')}
            </p>
          </div>
          <button
            type="button"
            className="btn-icon topic-form-close"
            onClick={onClose}
            aria-label={t('common:close')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} noValidate className="topic-form-body">
          {/* Tên chủ đề */}
          <Input
            id="topic-name"
            label={t('topic.labelName')}
            type="text"
            placeholder={t('topic.placeholderName')}
            value={form.name}
            onChange={handleNameChange}
            error={errors.name}
            disabled={isSubmitting}
          />

          {/* Slug */}
          <div className="topic-form-slug-field">
            <Input
              id="topic-slug"
              label={t('topic.labelSlug')}
              type="text"
              placeholder="animals-and-nature"
              value={form.slug}
              onChange={handleSlugChange}
              error={errors.slug}
              disabled={isSubmitting}
              rightElement={
                <SlugRegenerateButton
                  label={t('topic.regenerateSlug')}
                  onClick={handleRegenerateSlug}
                  disabled={isSubmitting}
                />
              }
            />
            {!slugManuallyEdited && form.name && (
              <p className="topic-form-slug-hint text-body-sm">
                <span className="material-symbols-outlined">auto_fix_high</span>
                {t('topic.slugAutoGenerated')}
              </p>
            )}
            <p className="topic-form-slug-rule text-body-sm">
              {t('topic.slugRule')}
            </p>
          </div>

          {/* Thứ tự (chỉ khi edit) */}
          {mode === 'edit' && (
            <Input
              id="topic-order"
              className="topic-form-unsupported-field"
              label={t('topic.labelOrder')}
              type="number"
              placeholder="1"
              value={form.order}
              onChange={handleOrderChange}
              error={errors.order}
              disabled={isSubmitting}
            />
          )}

          {/* Footer actions */}
          <div className="topic-form-footer">
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
  )
}

export default TopicFormModal
