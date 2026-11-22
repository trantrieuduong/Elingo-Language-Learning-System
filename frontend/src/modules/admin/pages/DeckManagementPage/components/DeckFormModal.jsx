import React, { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, PlusIcon } from '../../../components/icons/AdminIcons';
import { createSystemDeck, updateSystemDeck } from '../../../services/adminContentApi';
import { uploadFileForVerification } from '../../../../../services/uploadFile';
import styles from './DeckFormModal.module.css';

export default function DeckFormModal({
  isOpen,
  mode = 'create', // 'create' | 'edit'
  initialData = null,
  tags = [],
  cefrLevels = [],
  onClose,
  onSuccess,
}) {
  const { t } = useTranslation('admin');
  const fileInputRef = useRef(null);

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    coverImageUrl: '',
    isPremium: false,
    tagIds: [],
    cefrLevelIds: [],
  });

  const [previewUrl, setPreviewUrl] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Sync initialData when modal opens
  useEffect(() => {
    if (isOpen) {
      if (mode === 'edit' && initialData) {
        setFormData({
          title: initialData.title || '',
          description: initialData.description || '',
          coverImageUrl: initialData.coverImageUrl || '',
          isPremium: !!initialData.isPremium,
          tagIds: initialData.tags ? initialData.tags.map((tg) => tg.id) : (initialData.tagIds || []),
          cefrLevelIds: initialData.cefrLevels ? initialData.cefrLevels.map((c) => c.id) : (initialData.cefrLevelIds || []),
        });
        setPreviewUrl(initialData.coverImageUrl || '');
      } else {
        setFormData({
          title: '',
          description: '',
          coverImageUrl: '',
          isPremium: false,
          tagIds: [],
          cefrLevelIds: [],
        });
        setPreviewUrl('');
      }
      setError(null);
      setUploadingImage(false);
      setSubmitting(false);
    }
  }, [isOpen, mode, initialData]);

  if (!isOpen) return null;

  // Handle select file & upload presigned url -> verification
  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setError(t('content.decks.imageHint'));
      return;
    }

    // 5MB limit
    if (file.size > 5 * 1024 * 1024) {
      setError(t('content.decks.imageHint'));
      return;
    }

    try {
      setError(null);
      setUploadingImage(true);

      // Local preview
      const localUrl = URL.createObjectURL(file);
      setPreviewUrl(localUrl);

      // Upload process
      const verifiedKey = await uploadFileForVerification(file, file.type);
      setFormData((prev) => ({ ...prev, coverImageUrl: verifiedKey }));
    } catch (err) {
      setError(err?.message || t('content.decks.uploadError'));
    } finally {
      setUploadingImage(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleRemoveImage = () => {
    setPreviewUrl('');
    setFormData((prev) => ({ ...prev, coverImageUrl: '' }));
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.title.trim()) return;

    try {
      setSubmitting(true);
      setError(null);

      const payload = {
        title: formData.title.trim(),
        description: formData.description.trim() || undefined,
        coverImageUrl: formData.coverImageUrl.trim() || undefined,
        isPremium: formData.isPremium,
        tagIds: formData.tagIds.length > 0 ? formData.tagIds : undefined,
        cefrLevelIds: formData.cefrLevelIds.length > 0 ? formData.cefrLevelIds : undefined,
      };

      let result;
      if (mode === 'create') {
        result = await createSystemDeck(payload);
      } else {
        result = await updateSystemDeck(initialData.id, payload);
      }

      if (onSuccess) onSuccess(result);
      if (onClose) onClose();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || t('feedback.errorDesc'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.modalBackdrop} onClick={onClose}>
      <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className={styles.modalHeader}>
          <h3 className={styles.modalTitle}>
            {mode === 'create'
              ? t('content.decks.createSystemDeck')
              : t('content.decks.editDeckBtn')}
          </h3>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            aria-label={t('content.decks.cancelBtn')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit}>
          <div className={styles.modalBody}>
            {error && <p className={styles.errorMessage}>{error}</p>}

            {/* Title */}
            <div className={styles.formGroup}>
              <label htmlFor="modalDeckTitle" className={styles.formLabel}>
                {t('content.decks.deckName')} *
              </label>
              <input
                id="modalDeckTitle"
                type="text"
                className={styles.formInput}
                value={formData.title}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, title: e.target.value }))
                }
                required
                autoFocus
              />
            </div>

            {/* Description */}
            <div className={styles.formGroup}>
              <label htmlFor="modalDeckDesc" className={styles.formLabel}>
                {t('content.decks.deckDescription')}
              </label>
              <textarea
                id="modalDeckDesc"
                className={styles.formTextarea}
                value={formData.description}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, description: e.target.value }))
                }
              />
            </div>

            {/* Cover Image Upload */}
            <div className={styles.uploadSection}>
              <span className={styles.formLabel}>{t('content.decks.coverImageUrl')}</span>
              <div className={styles.uploadPreviewContainer}>
                {previewUrl ? (
                  <img src={previewUrl} alt="Cover preview" className={styles.previewImage} />
                ) : (
                  <div className={styles.placeholderThumb}>
                    <PlusIcon size={24} />
                  </div>
                )}
                <div className={styles.uploadActions}>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    className={styles.fileInputHidden}
                    onChange={handleFileChange}
                    disabled={uploadingImage}
                  />
                  <button
                    type="button"
                    className={styles.uploadBtn}
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploadingImage}
                  >
                    {previewUrl
                      ? t('content.decks.changeImage')
                      : t('content.decks.uploadImage')}
                  </button>
                  {previewUrl && (
                    <button
                      type="button"
                      className={styles.removeImageBtn}
                      onClick={handleRemoveImage}
                      disabled={uploadingImage}
                    >
                      {t('content.decks.removeImage')}
                    </button>
                  )}
                  {uploadingImage && (
                    <span className={styles.uploadStatusText}>
                      {t('content.decks.uploading')}
                    </span>
                  )}
                  <p className={styles.hintText}>{t('content.decks.imageHint')}</p>
                </div>
              </div>
            </div>

            {/* Premium Tier Checkbox */}
            <label className={styles.checkboxLabel}>
              <input
                type="checkbox"
                checked={formData.isPremium}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, isPremium: e.target.checked }))
                }
              />
              <span>{t('content.decks.isPremiumLabel')}</span>
            </label>

            {/* CEFR Level Multi-Select */}
            <div className={styles.formGroup}>
              <span className={styles.formLabel}>{t('content.decks.selectCefr')}</span>
              <div className={styles.multiCheckGroup}>
                {cefrLevels.map((lvl) => {
                  const checked = formData.cefrLevelIds.includes(lvl.id);
                  return (
                    <label key={lvl.id} className={styles.chipCheckLabel}>
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => {
                          setFormData((prev) => ({
                            ...prev,
                            cefrLevelIds: checked
                              ? prev.cefrLevelIds.filter((id) => id !== lvl.id)
                              : [...prev.cefrLevelIds, lvl.id],
                          }));
                        }}
                      />
                      <span>{lvl.label}</span>
                    </label>
                  );
                })}
              </div>
            </div>

            {/* Tags Multi-Select */}
            <div className={styles.formGroup}>
              <span className={styles.formLabel}>{t('content.decks.selectTags')}</span>
              <div className={styles.multiCheckGroup}>
                {tags.map((tg) => {
                  const checked = formData.tagIds.includes(tg.id);
                  return (
                    <label key={tg.id} className={styles.chipCheckLabel}>
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => {
                          setFormData((prev) => ({
                            ...prev,
                            tagIds: checked
                              ? prev.tagIds.filter((id) => id !== tg.id)
                              : [...prev.tagIds, tg.id],
                          }));
                        }}
                      />
                      <span>#{tg.label}</span>
                    </label>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className={styles.modalFooter}>
            <button
              type="button"
              className={styles.cancelBtn}
              onClick={onClose}
              disabled={submitting || uploadingImage}
            >
              {t('content.decks.cancelBtn')}
            </button>
            <button
              type="submit"
              className={styles.primaryBtn}
              disabled={submitting || uploadingImage || !formData.title.trim()}
            >
              {mode === 'create'
                ? t('content.decks.createBtn')
                : t('content.decks.saveBtn')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

DeckFormModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['create', 'edit']),
  initialData: PropTypes.object,
  tags: PropTypes.array,
  cefrLevels: PropTypes.array,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
};
