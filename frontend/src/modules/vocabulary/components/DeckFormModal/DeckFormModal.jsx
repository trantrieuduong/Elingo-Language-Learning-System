import { useState, useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import Input from '../../../../components/Input/Input'
import SlugRegenerateButton from '../SlugRegenerateButton/SlugRegenerateButton'
import {
  generateSlug,
  isValidSlug,
  MOCK_TAGS,
  MOCK_CEFR_LEVELS,
} from '../../userDeckMockData'
import './DeckFormModal.css'

/**
 * DeckFormModal — Modal tạo mới hoặc chỉnh sửa bộ từ vựng (User Deck).
 * Dựa trên CreateDeckRequest và UpdateDeckRequest.
 *
 * @param {boolean} isOpen - Trạng thái hiển thị modal
 * @param {string} mode - 'create' hoặc 'edit'
 * @param {Object} initialData - Dữ liệu khởi tạo khi edit
 * @param {Function} onClose - Callback đóng modal
 * @param {Function} onSubmit - Callback gửi dữ liệu form
 * @param {boolean} isLoading - Trạng thái đang submit
 */
function DeckFormModal({
  isOpen,
  mode = 'create',
  initialData = null,
  onClose,
  onSubmit,
  isLoading = false,
}) {
  const { t } = useTranslation(['learning', 'common'])
  const isEdit = mode === 'edit'

  // Form states
  const [title, setTitle] = useState('')
  const [slug, setSlug] = useState('')
  const [description, setDescription] = useState('')
  const [coverImageUrl, setCoverImageUrl] = useState('')
  const [status, setStatus] = useState('DRAFT')
  const [selectedTagIds, setSelectedTagIds] = useState([])
  const [selectedCefrIds, setSelectedCefrIds] = useState([])
  const [autoSlug, setAutoSlug] = useState(true)

  // Validation & alerts
  const [errors, setErrors] = useState({})
  const [generalError, setGeneralError] = useState('')

  const modalRef = useRef(null)

  // Khởi tạo dữ liệu khi modal mở
  useEffect(() => {
    if (isOpen) {
      if (isEdit && initialData) {
        setTitle(initialData.title || '')
        setSlug(initialData.slug || '')
        setDescription(initialData.description || '')
        setCoverImageUrl(initialData.coverImageUrl || '')
        setStatus(initialData.status || 'DRAFT')
        setSelectedTagIds(initialData.tags?.map((t) => t.id) || [])
        setSelectedCefrIds(initialData.cefrLevels?.map((c) => c.id) || [])
        setAutoSlug(false) // Khi sửa thì mặc định không tự ghi đè slug
      } else {
        setTitle('')
        setSlug('')
        setDescription('')
        setCoverImageUrl('')
        setStatus('DRAFT')
        setSelectedTagIds([])
        setSelectedCefrIds([])
        setAutoSlug(true)
      }
      setErrors({})
      setGeneralError('')
    }
  }, [isOpen, isEdit, initialData])

  // Tự động sinh slug khi title thay đổi nếu bật autoSlug
  const handleTitleChange = (e) => {
    const val = e.target.value
    setTitle(val)
    if (errors.title) setErrors((prev) => ({ ...prev, title: '' }))

    if (autoSlug) {
      const generated = generateSlug(val)
      setSlug(generated)
      if (errors.slug) setErrors((prev) => ({ ...prev, slug: '' }))
    }
  }

  // Thay đổi slug thủ công
  const handleSlugChange = (e) => {
    const val = e.target.value
    setSlug(val)
    setAutoSlug(false) // Người dùng đã tự gõ slug thì tắt auto
    if (errors.slug) setErrors((prev) => ({ ...prev, slug: '' }))
  }

  // Toggle CEFR level
  const handleToggleCefr = (id) => {
    setSelectedCefrIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  // Toggle Tag
  const handleToggleTag = (id) => {
    setSelectedTagIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  // Sinh lại slug từ title hiện tại
  const handleRegenerateSlug = () => {
    const generated = generateSlug(title)
    setSlug(generated)
    setAutoSlug(true)
    if (errors.slug) setErrors((prev) => ({ ...prev, slug: '' }))
  }

  // Validate form trước khi submit
  const validateForm = () => {
    const newErrors = {}

    if (!title || !title.trim()) {
      newErrors.title = t('userDeck.form.titleRequired')
    } else if (title.trim().length < 2) {
      newErrors.title = t('userDeck.form.titleMinLength')
    }

    if (!slug || !slug.trim()) {
      newErrors.slug = t('userDeck.form.slugRequired')
    } else if (!isValidSlug(slug.trim())) {
      newErrors.slug = t('userDeck.form.slugInvalid')
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  // Xử lý submit
  const handleSubmit = async (e) => {
    e.preventDefault()
    setGeneralError('')

    if (!validateForm()) return

    const payload = {
      title: title.trim(),
      slug: slug.trim(),
      description: description.trim(),
      coverImageUrl: coverImageUrl.trim(),
      status,
      tagIds: selectedTagIds,
      cefrLevelIds: selectedCefrIds,
    }

    try {
      const res = await onSubmit(payload)
      if (res && !res.success) {
        setGeneralError(res.message || t('userDeck.form.saveError'))
      }
    } catch (err) {
      setGeneralError(err.message || t('userDeck.form.systemError'))
    }
  }

  // Khóa scroll khi mở modal & hỗ trợ phím Escape
  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !isLoading) onClose?.()
    }

    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [isOpen, isLoading, onClose])

  if (!isOpen) return null

  return createPortal(
    <div
      className="deck-form-overlay"
      onClick={() => !isLoading && onClose?.()}
      role="dialog"
      aria-modal="true"
      aria-labelledby="deck-modal-title"
    >
      <div
        className="deck-form-modal glass-capsule"
        ref={modalRef}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header Modal */}
        <div className="deck-form-header">
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
            aria-label={t('common:close')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Thông báo lỗi tổng quát */}
        {generalError && (
          <div className="deck-form-alert deck-form-alert--error" role="alert">
            <span className="material-symbols-outlined">error</span>
            <span>{generalError}</span>
          </div>
        )}

        {/* Nội dung form */}
        <form className="deck-form-body" onSubmit={handleSubmit} noValidate>
          <div className="deck-form-scroll">
            <div className="deck-form-layout">
              <div className="deck-form-primary-column">
                {/* Tên bộ từ */}
                <div className="deck-form-field">
                  <Input
                    id="deck-title-input"
                    label={t('userDeck.form.titleLabel')}
                    placeholder={t('userDeck.form.titlePlaceholder')}
                    value={title}
                    onChange={handleTitleChange}
                    error={errors.title}
                    disabled={isLoading}
                    autoFocus
                  />
                </div>

                {/* Đường dẫn định danh (Slug) */}
                <div className="deck-form-field">
                  <div className="deck-form-slug-row">
                    <div className="deck-form-slug-input">
                      <Input
                        id="deck-slug-input"
                        label={t('userDeck.form.slugLabel')}
                        placeholder={t('userDeck.form.slugPlaceholder')}
                        value={slug}
                        onChange={handleSlugChange}
                        error={errors.slug}
                        disabled={isLoading}
                        rightElement={
                          <SlugRegenerateButton
                            label={t('userDeck.form.regenerateSlug')}
                            onClick={handleRegenerateSlug}
                            disabled={isLoading}
                          />
                        }
                      />
                    </div>
                  </div>
                  <p className="deck-form-hint text-body-sm">
                    {t('userDeck.form.slugHint')}{' '}
                    <code>ielts-speaking</code>
                  </p>
                </div>

                {/* Mô tả */}
                <div className="deck-form-field">
                  <label htmlFor="deck-desc-input" className="input-label">
                    {t('userDeck.form.descriptionLabel')}
                  </label>
                  <textarea
                    id="deck-desc-input"
                    className="deck-form-textarea"
                    rows={3}
                    placeholder={t('userDeck.form.descriptionPlaceholder')}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    disabled={isLoading}
                  />
                </div>

              </div>

              <div className="deck-form-secondary-column">
                {/* Ảnh bìa */}
                <div className="deck-form-field">
                  <span className="input-label">{t('userDeck.form.coverLabel')}</span>
                  <label className="deck-form-upload" htmlFor="deck-cover-input">
                    <input
                      id="deck-cover-input"
                      className="deck-form-upload__input"
                      type="file"
                      accept="image/*"
                      disabled={isLoading}
                    />
                    <span className="material-symbols-outlined deck-form-upload__icon" aria-hidden="true">
                      upload_file
                    </span>
                    <span className="deck-form-upload__title">
                      {t('userDeck.form.coverUploadAction')}
                    </span>
                  </label>

                  <div className="deck-form-cover-preview">
                    {coverImageUrl ? (
                      <img
                        src={coverImageUrl}
                        alt={t('userDeck.form.coverPreviewAlt')}
                        onError={(e) => {
                          e.target.style.display = 'none'
                        }}
                        onLoad={(e) => {
                          e.target.style.display = 'block'
                        }}
                      />
                    ) : (
                      <div className="deck-form-cover-placeholder" aria-hidden="true">
                        <span className="material-symbols-outlined">image</span>
                        <span>{t('userDeck.form.coverPreviewPlaceholder')}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="deck-form-taxonomy-column">
                {/* Cấp độ CEFR */}
                <div className="deck-form-field">
                  <span className="input-label">{t('userDeck.form.cefrLabel')}</span>
                  <div className="deck-form-pills-row">
                    {MOCK_CEFR_LEVELS.map((cefr) => {
                      const isSelected = selectedCefrIds.includes(cefr.id)
                      return (
                        <button
                          key={cefr.id}
                          type="button"
                          className={`deck-form-pill ${
                            isSelected ? 'deck-form-pill--active badge--primary' : 'badge--default'
                          }`}
                          onClick={() => handleToggleCefr(cefr.id)}
                          disabled={isLoading}
                        >
                          {isSelected && (
                            <span className="material-symbols-outlined deck-form-pill__check">check</span>
                          )}
                          {cefr.label}
                        </button>
                      )
                    })}
                  </div>
                </div>

                {/* Thẻ phân loại (Tags) */}
                <div className="deck-form-field">
                  <span className="input-label">{t('userDeck.form.tagsLabel')}</span>
                  <div className="deck-form-pills-row">
                    {MOCK_TAGS.map((tag) => {
                      const isSelected = selectedTagIds.includes(tag.id)
                      return (
                        <button
                          key={tag.id}
                          type="button"
                          className={`deck-form-pill ${
                            isSelected ? 'deck-form-pill--active badge--primary' : 'badge--default'
                          }`}
                          onClick={() => handleToggleTag(tag.id)}
                          disabled={isLoading}
                        >
                          {isSelected && (
                            <span className="material-symbols-outlined deck-form-pill__check">check</span>
                          )}
                          {t(`userDeck.form.tags.${tag.code}`)}
                        </button>
                      )
                    })}
                  </div>
                </div>

                {isEdit && (
                  <div className="deck-form-field">
                    <span className="input-label">{t('userDeck.form.statusLabel')}</span>
                    <div className="deck-form-status-selector">
                      <label
                        className={`deck-form-status-option ${
                          status === 'DRAFT' ? 'deck-form-status-option--active' : ''
                        }`}
                      >
                        <input
                          type="radio"
                          name="deck-status"
                          value="DRAFT"
                          checked={status === 'DRAFT'}
                          onChange={() => setStatus('DRAFT')}
                          disabled={isLoading}
                        />
                        <span className="material-symbols-outlined">edit_note</span>
                        <div>
                          <strong>{t('userDeck.form.draft')}</strong>
                          <span className="text-body-sm">{t('userDeck.form.draftDescription')}</span>
                        </div>
                      </label>

                      <label
                        className={`deck-form-status-option ${
                          status === 'PUBLISHED' ? 'deck-form-status-option--active' : ''
                        }`}
                      >
                        <input
                          type="radio"
                          name="deck-status"
                          value="PUBLISHED"
                          checked={status === 'PUBLISHED'}
                          onChange={() => setStatus('PUBLISHED')}
                          disabled={isLoading}
                        />
                        <span className="material-symbols-outlined">public</span>
                        <div>
                          <strong>{t('userDeck.form.published')}</strong>
                          <span className="text-body-sm">{t('userDeck.form.publishedDescription')}</span>
                        </div>
                      </label>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Nút hành động */}
          <div className="deck-form-actions">
            <button
              type="button"
              className="btn-secondary deck-form-btn-cancel"
              onClick={onClose}
              disabled={isLoading}
            >
              {t('common:cancel')}
            </button>
            <button
              type="submit"
              className="btn-primary deck-form-btn-submit"
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <span className="app-loading-spinner deck-form-spinner" />
                  {t('common:saving')}
                </>
              ) : isEdit ? (
                <>
                  <span className="material-symbols-outlined">save</span>
                  {t('common:saveChanges')}
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined">add</span>
                  {t('userDeck.form.createDeck')}
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

export default DeckFormModal
