import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import Input from '../../../../components/Input/Input'
import './UserCardFormModal.css'

const PRONUNCIATION_LOCALES = ['en-US', 'en-GB']

function PronunciationFields({ initialPhonetics, t }) {
  const [phonetics, setPhonetics] = useState(() => {
    const entries = initialPhonetics?.length
      ? initialPhonetics.slice(0, 2)
      : [{ text: '', audioUrl: '', locale: PRONUNCIATION_LOCALES[0] }]

    if (entries.length === 2 && entries[0].locale === entries[1].locale) {
      entries[1] = {
        ...entries[1],
        locale: PRONUNCIATION_LOCALES.find((locale) => locale !== entries[0].locale),
      }
    }

    return entries
  })

  const handleAddPronunciation = () => {
    setPhonetics((current) => {
      if (current.length >= 2) return current

      const locale = PRONUNCIATION_LOCALES.find(
        (availableLocale) => availableLocale !== current[0].locale
      )

      return [...current, { text: '', audioUrl: '', locale }]
    })
  }

  const handleLocaleChange = (index, locale) => {
    setPhonetics((current) =>
      current.map((phonetic, phoneticIndex) =>
        phoneticIndex === index ? { ...phonetic, locale } : phonetic
      )
    )
  }

  return (
    <fieldset className="user-card-form-modal__phonetics">
      <legend className="input-label">{t('userCards.pronunciations')}</legend>
      {phonetics.map((phonetic, index) => {
        const otherLocale = phonetics.find((_, phoneticIndex) => phoneticIndex !== index)?.locale

        return (
          <div className="user-card-form-modal__phonetic-row" key={index}>
            <Input
              id={`user-card-phonetic-${index}`}
              label={t('userCards.phoneticText', { index: index + 1 })}
              placeholder="/əˈkɑːmplɪʃ/"
              defaultValue={phonetic.text}
            />
            <div className="user-card-form-modal__field">
              <label className="input-label" htmlFor={`user-card-locale-${index}`}>
                {t('userCards.locale')}
              </label>
              <select
                className="user-card-form-modal__select"
                id={`user-card-locale-${index}`}
                value={phonetic.locale}
                onChange={(event) => handleLocaleChange(index, event.target.value)}
              >
                <option value="en-US" disabled={otherLocale === 'en-US'}>
                  {t('userCards.englishUs')}
                </option>
                <option value="en-GB" disabled={otherLocale === 'en-GB'}>
                  {t('userCards.englishUk')}
                </option>
              </select>
            </div>
            <Input
              id={`user-card-audio-${index}`}
              label={t('userCards.audioUrl')}
              placeholder="https://"
              defaultValue={phonetic.audioUrl}
            />
          </div>
        )
      })}
      {phonetics.length < 2 && (
        <div className="user-card-form-modal__phonetic-actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={handleAddPronunciation}
          >
            <span className="material-symbols-outlined" aria-hidden="true">add</span>
            {t('userCards.addPronunciation')}
          </button>
          <span className="text-body-sm">{t('userCards.pronunciationLimit')}</span>
        </div>
      )}
    </fieldset>
  )
}

function UserCardImagePicker({ initialImageUrl, t }) {
  const [imagePreviewUrl, setImagePreviewUrl] = useState(initialImageUrl)
  const imagePreviewObjectUrl = useRef('')

  useEffect(
    () => () => {
      if (imagePreviewObjectUrl.current) {
        URL.revokeObjectURL(imagePreviewObjectUrl.current)
      }
    },
    []
  )

  const handleImageChange = (event) => {
    const file = event.target.files?.[0]
    if (!file) return

    if (imagePreviewObjectUrl.current) {
      URL.revokeObjectURL(imagePreviewObjectUrl.current)
    }

    const objectUrl = URL.createObjectURL(file)
    imagePreviewObjectUrl.current = objectUrl
    setImagePreviewUrl(objectUrl)
    event.target.value = ''
  }

  return (
    <div className="user-card-form-modal__image-field">
      <span className="input-label user-card-form-modal__image-label">
        {t('userCards.imageLabel')}
      </span>
      <label className="user-card-form-modal__image-upload" htmlFor="user-card-image">
        <input
          id="user-card-image"
          className="user-card-form-modal__image-input"
          type="file"
          accept="image/*"
          aria-label={t('userCards.imageUploadAction')}
          onChange={handleImageChange}
        />
        <span className="material-symbols-outlined" aria-hidden="true">
          upload_file
        </span>
        <span>{t('userCards.imageUploadAction')}</span>
      </label>
      <div className="user-card-form-modal__image-preview">
        {imagePreviewUrl ? (
          <img
            src={imagePreviewUrl}
            alt={t('userCards.imagePreviewAlt')}
            onError={() => setImagePreviewUrl('')}
          />
        ) : (
          <div className="user-card-form-modal__image-placeholder">
            <span className="material-symbols-outlined" aria-hidden="true">
              image
            </span>
            <span>{t('userCards.imagePreviewPlaceholder')}</span>
          </div>
        )}
      </div>
    </div>
  )
}

function UserCardFormModal({
  isOpen,
  mode = 'create',
  card = null,
  topics = [],
  isTopicLocked = false,
  onClose,
}) {
  const { t } = useTranslation('learning')
  const isEdit = mode === 'edit'

  useEffect(() => {
    if (!isOpen) return undefined

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose?.()
    }

    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [isOpen, onClose])

  if (!isOpen) return null

  return createPortal(
    <div
      className="modal-overlay user-card-form-overlay"
      onClick={onClose}
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
            aria-label={t('userCards.close')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        <div className="user-card-form-modal__body">
          <div className="user-card-form-modal__grid">
            <div className="user-card-form-modal__field">
              <label className="input-label" htmlFor="user-card-topic">
                {t('userCards.topicRequired')}
              </label>
              <select
                className="user-card-form-modal__select"
                id="user-card-topic"
                defaultValue={card?.topicId ?? topics[0]?.id ?? ''}
                disabled={isEdit || isTopicLocked}
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
            </div>

            {isEdit && (
              <Input
                id="user-card-order"
                label={t('userCards.order')}
                type="number"
                defaultValue={card?.order ?? ''}
                min="0"
              />
            )}

            <Input
              id="user-card-term"
              label={t('userCards.termRequired')}
              placeholder={t('userCards.termPlaceholder')}
              defaultValue={card?.term ?? ''}
              autoFocus
            />
            <Input
              id="user-card-pos"
              label={t('userCards.partOfSpeech')}
              placeholder={t('userCards.partOfSpeechPlaceholder')}
              defaultValue={card?.pos ?? ''}
            />
            <Input
              id="user-card-translation"
              label={t('userCards.translation')}
              placeholder={t('userCards.translationPlaceholder')}
              defaultValue={card?.translation ?? ''}
            />
            <UserCardImagePicker
              key={`${card?.id ?? 'new'}-${card?.imageUrl ?? ''}`}
              initialImageUrl={card?.imageUrl ?? ''}
              t={t}
            />
          </div>

          <div className="user-card-form-modal__grid user-card-form-modal__grid--textareas">
            <div className="user-card-form-modal__field">
              <label className="input-label" htmlFor="user-card-explanation-vi">
                {t('userCards.explanationVi')}
              </label>
              <textarea
                className="user-card-form-modal__textarea"
                id="user-card-explanation-vi"
                rows="3"
                placeholder={t('userCards.explanationPlaceholder')}
                defaultValue={card?.explanationVi ?? ''}
              />
            </div>
            <div className="user-card-form-modal__field">
              <label className="input-label" htmlFor="user-card-explanation-en">
                {t('userCards.explanationEn')}
              </label>
              <textarea
                className="user-card-form-modal__textarea"
                id="user-card-explanation-en"
                rows="3"
                placeholder={t('userCards.explanationPlaceholder')}
                defaultValue={card?.explanationEn ?? ''}
              />
            </div>
            <div className="user-card-form-modal__field">
              <label className="input-label" htmlFor="user-card-example-vi">
                {t('userCards.examplesVi')}
              </label>
              <textarea
                className="user-card-form-modal__textarea"
                id="user-card-example-vi"
                rows="2"
                placeholder={t('userCards.examplePlaceholder')}
                defaultValue={card?.examplesVi ?? ''}
              />
            </div>
            <div className="user-card-form-modal__field">
              <label className="input-label" htmlFor="user-card-example-en">
                {t('userCards.examplesEn')}
              </label>
              <textarea
                className="user-card-form-modal__textarea"
                id="user-card-example-en"
                rows="2"
                placeholder={t('userCards.examplePlaceholder')}
                defaultValue={card?.examplesEn ?? ''}
              />
            </div>
          </div>

          <PronunciationFields
            key={`${mode}-${card?.id ?? 'new'}`}
            initialPhonetics={card?.phonetics}
            t={t}
          />
        </div>
        <div className="user-card-form-modal__actions">
          <button type="button" className="btn-secondary" onClick={onClose}>
            {t('userCards.cancel')}
          </button>
          <button type="button" className="btn-primary">
            <span className="material-symbols-outlined" aria-hidden="true">
              {isEdit ? 'save' : 'add'}
            </span>
            {t(isEdit ? 'userCards.saveChanges' : 'userCards.createCard')}
          </button>
        </div>
      </div>
    </div>,
    document.body
  )
}

export default UserCardFormModal
