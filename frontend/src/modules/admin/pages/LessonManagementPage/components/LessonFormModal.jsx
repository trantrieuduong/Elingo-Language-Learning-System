import React, { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { CloseIcon, PlusIcon } from '../../../components/icons/AdminIcons';
import { createAdminLesson, updateAdminLesson } from '../../../services/adminContentApi';
import { uploadFileForVerification } from '../../../../../services/uploadFile';
import { extractVideoId } from '../utils/youtube';
import { formatMs } from '../utils/timeMask';
import YouTubePlayer from './YouTubePlayer';
import styles from './LessonFormModal.module.css';

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

export default function LessonFormModal({
  isOpen,
  mode = 'create',
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
    sourceUrl: '',
    durationMs: null,
    thumbnailUrl: '',
    isPremium: false,
    tagIds: [],
    cefrLevelIds: [],
  });

  const [previewUrl, setPreviewUrl] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [videoError, setVideoError] = useState(null);
  const [videoReady, setVideoReady] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    if (mode === 'edit' && initialData) {
      setFormData({
        title: initialData.title || '',
        description: initialData.description || '',
        sourceUrl: initialData.sourceUrl || '',
        durationMs: initialData.durationMs || null,
        thumbnailUrl: initialData.thumbnailUrl || '',
        isPremium: Boolean(initialData.isPremium),
        tagIds: initialData.tags ? initialData.tags.map((tg) => tg.id) : (initialData.tagIds || []),
        cefrLevelIds: initialData.cefrLevels
          ? initialData.cefrLevels.map((c) => c.id)
          : (initialData.cefrLevelIds || []),
      });
      setPreviewUrl(initialData.thumbnailUrl || '');
      setVideoReady(Boolean(initialData.durationMs));
    } else {
      setFormData({
        title: '',
        description: '',
        sourceUrl: '',
        durationMs: null,
        thumbnailUrl: '',
        isPremium: false,
        tagIds: [],
        cefrLevelIds: [],
      });
      setPreviewUrl('');
      setVideoReady(false);
    }
    setError(null);
    setVideoError(null);
    setUploadingImage(false);
    setSubmitting(false);
  }, [isOpen, mode, initialData]);

  if (!isOpen) return null;

  const trimmedUrl = formData.sourceUrl.trim();
  const videoId = extractVideoId(trimmedUrl);
  const isUrlInvalid = Boolean(trimmedUrl && !videoId);

  const handleSourceUrlChange = (value) => {
    setFormData((prev) => ({
      ...prev,
      sourceUrl: value,
      durationMs: null,
    }));
    setVideoReady(false);
    setVideoError(null);
    if (error) setError(null);
  };

  const handlePlayerReady = ({ durationMs }) => {
    setFormData((prev) => ({ ...prev, durationMs }));
    setVideoReady(true);
    setVideoError(null);
  };

  const handlePlayerError = (code) => {
    setVideoReady(false);
    if (code === 101 || code === 150) {
      setVideoError('EMBED_BLOCKED');
    } else if (code === 'DURATION_UNAVAILABLE') {
      setVideoError('DURATION_UNAVAILABLE');
    } else {
      setVideoError('INVALID');
    }
  };

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      setError(t('content.lessons.thumbnailTypeError'));
      return;
    }

    if (file.size > MAX_IMAGE_SIZE) {
      setError(t('content.lessons.thumbnailSizeError'));
      return;
    }

    try {
      setError(null);
      setUploadingImage(true);
      const localUrl = URL.createObjectURL(file);
      setPreviewUrl(localUrl);
      const verifiedKey = await uploadFileForVerification(file, file.type);
      setFormData((prev) => ({ ...prev, thumbnailUrl: verifiedKey }));
    } catch (err) {
      setError(err?.message || t('content.lessons.uploadError'));
    } finally {
      setUploadingImage(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleRemoveImage = () => {
    setPreviewUrl('');
    setFormData((prev) => ({ ...prev, thumbnailUrl: '' }));
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const toggleTag = (id) => {
    setFormData((prev) => {
      const exists = prev.tagIds.includes(id);
      return {
        ...prev,
        tagIds: exists ? prev.tagIds.filter((x) => x !== id) : [...prev.tagIds, id],
      };
    });
  };

  const toggleCefr = (id) => {
    setFormData((prev) => {
      const exists = prev.cefrLevelIds.includes(id);
      return {
        ...prev,
        cefrLevelIds: exists
          ? prev.cefrLevelIds.filter((x) => x !== id)
          : [...prev.cefrLevelIds, id],
      };
    });
  };

  const canSubmit =
    Boolean(formData.title.trim()) &&
    formData.title.trim().length <= 100 &&
    formData.description.trim().length <= 1000 &&
    Boolean(videoId) &&
    videoReady &&
    Number(formData.durationMs) > 0 &&
    videoError !== 'EMBED_BLOCKED' &&
    !uploadingImage &&
    !submitting;

  const handleSubmit = async (e) => {
    e.preventDefault();
    const title = formData.title.trim();
    if (!title) {
      setError(t('content.lessons.titleRequired'));
      return;
    }
    if (title.length > 100) {
      setError(t('content.lessons.titleTooLong'));
      return;
    }
    if (!canSubmit) return;

    try {
      setSubmitting(true);
      setError(null);

      const payload = {
        title,
        description: formData.description.trim() || undefined,
        sourceUrl: trimmedUrl,
        durationMs: formData.durationMs,
        thumbnailUrl: formData.thumbnailUrl || undefined,
        isPremium: formData.isPremium,
        tagIds: formData.tagIds,
        cefrLevelIds: formData.cefrLevelIds,
      };

      const saved =
        mode === 'edit' && initialData
          ? await updateAdminLesson(initialData.id, payload)
          : await createAdminLesson(payload);

      onSuccess(saved);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || t('feedback.errorDesc'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.modalBackdrop} onClick={() => !submitting && onClose()}>
      <div
        className={styles.modalCard}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="lesson-form-modal-title"
      >
        <div className={styles.modalHeader}>
          <h3 id="lesson-form-modal-title" className={styles.modalTitle}>
            {mode === 'edit'
              ? t('content.lessons.editModalTitle')
              : t('content.lessons.createModalTitle')}
          </h3>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            disabled={submitting}
            aria-label={t('content.lessons.cancelBtn')}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className={styles.modalBody}>
            {error && <p className={styles.errorMessage}>{error}</p>}

            {/* Tiêu đề */}
            <div className={styles.formGroup}>
              <label htmlFor="lesson-title-input" className={styles.formLabel}>
                {t('content.lessons.lessonTitleLabel')} *
              </label>
              <input
                id="lesson-title-input"
                type="text"
                className={styles.formInput}
                placeholder={t('content.lessons.lessonTitlePlaceholder')}
                maxLength={100}
                value={formData.title}
                onChange={(e) => {
                  setFormData((prev) => ({ ...prev, title: e.target.value }));
                  if (error) setError(null);
                }}
                disabled={submitting}
              />
            </div>

            {/* Mô tả */}
            <div className={styles.formGroup}>
              <label htmlFor="lesson-desc-input" className={styles.formLabel}>
                {t('content.lessons.lessonDescriptionLabel')}
              </label>
              <textarea
                id="lesson-desc-input"
                className={styles.formTextarea}
                maxLength={1000}
                value={formData.description}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, description: e.target.value }))
                }
                disabled={submitting}
              />
            </div>

            {/* Link YouTube */}
            <div className={styles.formGroup}>
              <label htmlFor="lesson-url-input" className={styles.formLabel}>
                {t('content.lessons.sourceUrlLabel')} *
              </label>
              <input
                id="lesson-url-input"
                type="url"
                className={styles.formInput}
                placeholder={t('content.lessons.sourceUrlPlaceholder')}
                maxLength={500}
                value={formData.sourceUrl}
                onChange={(e) => handleSourceUrlChange(e.target.value)}
                disabled={submitting}
              />
              {isUrlInvalid && (
                <p className={styles.fieldError}>{t('content.lessons.sourceUrlInvalid')}</p>
              )}
            </div>

            {/* Xem trước video & Thời lượng */}
            {videoId && (
              <div className={styles.formGroup}>
                <div className={styles.previewHeader}>
                  <span className={styles.formLabel}>{t('content.lessons.videoPreview')}</span>
                  <span className={styles.durationBadge}>
                    {t('content.lessons.durationLabel')}:{' '}
                    {formData.durationMs
                      ? formatMs(formData.durationMs)
                      : t('content.lessons.durationLoading')}
                  </span>
                </div>
                <YouTubePlayer
                  key={videoId}
                  videoId={videoId}
                  onReady={handlePlayerReady}
                  onError={handlePlayerError}
                />
                {videoError === 'EMBED_BLOCKED' && (
                  <p className={styles.errorMessage}>{t('content.lessons.embedBlocked')}</p>
                )}
                {(videoError === 'DURATION_UNAVAILABLE' || videoError === 'INVALID') && (
                  <p className={styles.errorMessage}>{t('content.lessons.videoUnavailable')}</p>
                )}
              </div>
            )}

            {/* Ảnh đại diện (Thumbnail) */}
            <div className={styles.uploadSection}>
              <span className={styles.formLabel}>{t('content.lessons.thumbnailLabel')}</span>
              <div className={styles.uploadPreviewContainer}>
                {previewUrl ? (
                  <img src={previewUrl} alt="Thumbnail preview" className={styles.previewImage} />
                ) : (
                  <div className={styles.placeholderThumb}>
                    <PlusIcon size={24} />
                  </div>
                )}
                <div className={styles.uploadActions}>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    onChange={handleFileChange}
                    className={styles.fileInputHidden}
                    disabled={uploadingImage || submitting}
                  />
                  <button
                    type="button"
                    className={styles.uploadBtn}
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploadingImage || submitting}
                  >
                    {previewUrl
                      ? t('content.lessons.changeImage')
                      : t('content.lessons.uploadImage')}
                  </button>
                  {previewUrl && (
                    <button
                      type="button"
                      className={styles.removeImageBtn}
                      onClick={handleRemoveImage}
                      disabled={uploadingImage || submitting}
                    >
                      {t('content.lessons.removeImage')}
                    </button>
                  )}
                  {uploadingImage && (
                    <span className={styles.uploadStatusText}>
                      {t('content.lessons.uploading')}
                    </span>
                  )}
                  <p className={styles.hintText}>{t('content.lessons.thumbnailHint')}</p>
                </div>
              </div>
            </div>

            {/* Trình độ CEFR */}
            {cefrLevels.length > 0 && (
              <div className={styles.formGroup}>
                <span className={styles.formLabel}>{t('content.lessons.selectCefr')}</span>
                <div className={styles.multiCheckGroup}>
                  {cefrLevels.map((lvl) => (
                    <label key={lvl.id} className={styles.chipCheckLabel}>
                      <input
                        type="checkbox"
                        checked={formData.cefrLevelIds.includes(lvl.id)}
                        onChange={() => toggleCefr(lvl.id)}
                        disabled={submitting}
                      />
                      <span>{lvl.label}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* Chủ đề (Tag) */}
            {tags.length > 0 && (
              <div className={styles.formGroup}>
                <span className={styles.formLabel}>{t('content.lessons.selectTags')}</span>
                <div className={styles.multiCheckGroup}>
                  {tags.map((tg) => (
                    <label key={tg.id} className={styles.chipCheckLabel}>
                      <input
                        type="checkbox"
                        checked={formData.tagIds.includes(tg.id)}
                        onChange={() => toggleTag(tg.id)}
                        disabled={submitting}
                      />
                      <span>#{tg.label}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* Gói Premium */}
            <label className={styles.checkboxLabel}>
              <input
                type="checkbox"
                checked={formData.isPremium}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, isPremium: e.target.checked }))
                }
                disabled={submitting}
              />
              <span>{t('content.lessons.isPremiumLabel')}</span>
            </label>
          </div>

          <div className={styles.modalFooter}>
            <button
              type="button"
              className={styles.cancelBtn}
              onClick={onClose}
              disabled={submitting}
            >
              {t('content.lessons.cancelBtn')}
            </button>
            <button type="submit" className={styles.primaryBtn} disabled={!canSubmit}>
              {mode === 'create'
                ? t('content.lessons.createBtn')
                : t('content.lessons.saveBtn')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

LessonFormModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['create', 'edit']),
  initialData: PropTypes.object,
  tags: PropTypes.array,
  cefrLevels: PropTypes.array,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
