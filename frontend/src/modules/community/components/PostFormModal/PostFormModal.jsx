import { useCallback, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import Input from '../../../../components/Input/Input';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import { uploadFileForVerification } from '../../../../services/uploadFile';
import './PostFormModal.css';

const TITLE_MAX_LENGTH = 255;
const CONTENT_MAX_LENGTH = 2000;
const MEDIA_MAX_FILES = 5;

function PostFormModal({
  isOpen,
  onClose,
  onSubmit,
  groups,
  initialPost = null,
  isLoading = false,
}) {
  const { t } = useTranslation();
  const fileInputRef = useRef(null);
  const mediaRef = useRef([]);

  const availableGroups = groups || [];
  const isEditing = Boolean(initialPost);

  // States form
  const [group, setGroup] = useState('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [media, setMedia] = useState([]);
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [uploadProgressText, setUploadProgressText] = useState('');

  const isFormBusy = isLoading || isSubmitting;

  const resetForm = useCallback((post = null) => {
    setGroup(post?.groupId?.toString() || '');
    setTitle(post?.title || '');
    setContent(post?.content || '');
    setMedia((prev) => {
      prev.forEach((item) => {
        if (item.isLocal && item.url) URL.revokeObjectURL(item.url);
      });
      return (post?.medias || []).map((item, index) => ({
        id: `existing-${post.id}-${item.order ?? index}`,
        file: null,
        fileKey: item.mediaUrl || item.url,
        isLocal: false,
        type: (item.mediaType || item.type || 'image').toLowerCase(),
        url: item.mediaUrl || item.url,
      }));
    });
    setErrors({});
    setGeneralError('');
    setUploadProgressText('');
  }, []);

  const handleClose = useCallback(() => {
    if (isFormBusy) return;
    resetForm();
    onClose?.();
  }, [isFormBusy, onClose, resetForm]);

  useEffect(() => {
    if (isOpen) resetForm(initialPost);
  }, [initialPost, isOpen, resetForm]);

  useEffect(() => {
    mediaRef.current = media;
  }, [media]);

  useEffect(() => {
    return () => {
      mediaRef.current.forEach((item) => {
        if (item.isLocal && item.url) URL.revokeObjectURL(item.url);
      });
    };
  }, []);

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !isFormBusy) {
        handleClose();
      }
    };

    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = '';
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, isFormBusy, handleClose]);

  if (!isOpen) return null;

  const addFiles = (fileList) => {
    const rawFiles = Array.from(fileList || []);
    const acceptedFiles = rawFiles.filter(
      (file) => file.type.startsWith('image/') || file.type.startsWith('video/')
    );

    const remainingSlots = MEDIA_MAX_FILES - media.length;

    if (remainingSlots <= 0) {
      setErrors((prev) => ({
        ...prev,
        media: t('community:postForm.errors.mediaMaxFiles', { max: MEDIA_MAX_FILES }),
      }));
      return;
    }

    const nextFiles = acceptedFiles.slice(0, remainingSlots).map((file) => ({
      id: `${file.name}-${file.lastModified}-${crypto.randomUUID()}`,
      file,
      isLocal: true,
      type: file.type.startsWith('video/') ? 'video' : 'image',
      url: URL.createObjectURL(file),
    }));

    setMedia((prev) => [...prev, ...nextFiles]);
    setErrors((prev) => ({
      ...prev,
      media:
        acceptedFiles.length > remainingSlots
          ? t('community:postForm.errors.firstFilesSelected', { slots: remainingSlots })
          : undefined,
    }));
  };

  const handleFileChange = (e) => {
    if (e.target.files) addFiles(e.target.files);
    e.target.value = '';
  };

  const handleRemoveMedia = (id) => {
    setMedia((prev) => {
      const removed = prev.find((item) => item.id === id);
      if (removed?.isLocal && removed.url) URL.revokeObjectURL(removed.url);
      return prev.filter((item) => item.id !== id);
    });
    setErrors((prev) => ({ ...prev, media: undefined }));
  };

  const validate = () => {
    const newErrors = {};

    if (!isEditing && !group) newErrors.group = t('community:postForm.errors.groupRequired');
    if (!title.trim()) newErrors.title = t('community:postForm.errors.titleRequired');
    else if (title.trim().length > TITLE_MAX_LENGTH) {
      newErrors.title = t('community:postForm.errors.titleMaxLength', { max: TITLE_MAX_LENGTH });
    }

    if (!content.trim()) newErrors.content = t('community:postForm.errors.contentRequired');
    else if (content.trim().length > CONTENT_MAX_LENGTH) {
      newErrors.content = t('community:postForm.errors.contentMaxLength', {
        max: CONTENT_MAX_LENGTH.toLocaleString(),
      });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    if (!validate()) return;

    setIsSubmitting(true);
    setErrors({});

    try {
      const uploadedMedia = [];
      const newMedia = media.filter((item) => item.file);
      if (media.length > 0) {
        for (let i = 0; i < media.length; i++) {
          const item = media[i];
          if (!item.file) {
            uploadedMedia.push({
              id: item.id,
              fileKey: item.fileKey,
              type: item.type,
              localUrl: item.url,
            });
            continue;
          }

          setUploadProgressText(
            t('community:postForm.uploadProgress', {
              current: newMedia.findIndex((mediaItem) => mediaItem.id === item.id) + 1,
              total: newMedia.length,
            })
          );

          const fileType = item.file.type || 'image/jpeg';
          const verifiedFileKey = await uploadFileForVerification(item.file, fileType);

          uploadedMedia.push({
            id: item.id,
            fileKey: verifiedFileKey,
            type: item.type,
            name: item.file.name,
            size: item.file.size,
            localUrl: item.url,
          });
        }
      }

      setUploadProgressText(
        t(isEditing ? 'community:postForm.updatingPost' : 'community:postForm.creatingPost')
      );

      if (onSubmit) {
        const response = await onSubmit({
          group,
          title,
          content,
          media,
          uploadedMedia,
        });
        if (response && !response.success) {
          const code = response.code;
          const msg =
            (code && t(`api:error.${code}`, { defaultValue: '' })) ||
            response.message ||
            t('community:postForm.errors.uploadFailed');
          setGeneralError(msg);
        }
      }
    } catch (error) {
      const responseData = error?.response?.data;
      const errorCode = responseData?.code;

      const errorMessage =
        (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
        responseData?.message ||
        error?.message ||
        t('community:postForm.errors.uploadFailed');

      setGeneralError(errorMessage);
    } finally {
      setIsSubmitting(false);
      setUploadProgressText('');
    }
  };

  const handleOverlayClick = () => {
    handleClose();
  };

  return createPortal(
    <div className="post-form-modal__overlay" onClick={handleOverlayClick} role="dialog">
      <div className="post-form-modal__container" onClick={(e) => e.stopPropagation()}>
        {/* Header Modal */}
        <header className="post-form-modal__header">
          <div className="post-form-modal__header-info">
            <div className="post-form-modal__icon-badge glass-thumb">
              <span className="material-symbols-outlined">post_add</span>
            </div>
            <div>
              <h2 className="post-form-modal__title">
                {t(
                  isEditing ? 'community:postForm.editModalTitle' : 'community:postForm.modalTitle'
                )}
              </h2>
              <p className="post-form-modal__description">
                {t(
                  isEditing
                    ? 'community:postForm.editModalSubtitle'
                    : 'community:postForm.modalSubtitle'
                )}
              </p>
            </div>
          </div>

          <button
            type="button"
            disabled={isFormBusy}
            onClick={handleClose}
            className="btn-icon post-form-modal__close-btn"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        <FormAlert message={generalError} />

        {/* Form Body */}
        <form onSubmit={handleSubmit} noValidate className="post-form-modal__form">
          <div className="post-form-modal__scroll-body">
            <div
              className={`post-form-modal__row ${isEditing ? 'post-form-modal__row--editing' : ''}`}
            >
              {!isEditing && (
                <div className="post-form-modal__group-wrapper">
                  <label htmlFor="post-group" className="input-label">
                    {t('community:postForm.groupLabel')}
                  </label>
                  <select
                    id="post-group"
                    value={group}
                    disabled={isFormBusy || availableGroups.length === 0}
                    onChange={(e) => {
                      setGroup(e.target.value);
                      if (errors.group) {
                        setErrors((prev) => ({ ...prev, group: undefined }));
                      }
                    }}
                    className={`input-field ${errors.group ? 'input-field--error' : ''}`}
                  >
                    {availableGroups.length === 0 ? (
                      <option value="" disabled>
                        {t('community:postForm.loadingGroups')}
                      </option>
                    ) : (
                      <>
                        <option value="">{t('community:postForm.selectGroupPlaceholder')}</option>
                        {availableGroups.map((item) => (
                          <option key={item.value} value={item.value}>
                            {item.label}
                          </option>
                        ))}
                      </>
                    )}
                  </select>
                  {errors.group && (
                    <p id="post-group-error" className="input-error" role="alert">
                      {errors.group}
                    </p>
                  )}
                </div>
              )}

              {/* Input Title */}
              <div className="post-form-modal__title-wrapper">
                <Input
                  id="post-title"
                  label={t('community:postForm.titleLabel')}
                  placeholder={t('community:postForm.titlePlaceholder')}
                  value={title}
                  disabled={isFormBusy}
                  error={errors.title}
                  onChange={(e) => {
                    setTitle(e.target.value);
                    if (errors.title) {
                      setErrors((prev) => ({ ...prev, title: undefined }));
                    }
                  }}
                />
              </div>
            </div>

            {/* Textarea Content */}
            <div className="post-form-modal__content-wrapper">
              <div className="post-form-modal__label-row">
                <label htmlFor="post-content" className="input-label">
                  {t('community:postForm.contentLabel')}
                </label>
              </div>
              <textarea
                id="post-content"
                rows={5}
                placeholder={t('community:postForm.contentPlaceholder')}
                value={content}
                disabled={isFormBusy}
                onChange={(e) => {
                  setContent(e.target.value);
                  if (errors.content) {
                    setErrors((prev) => ({ ...prev, content: undefined }));
                  }
                }}
                className={`input-field post-form-modal__textarea ${errors.content ? 'input-field--error' : ''}`}
              />
              <div className="post-form-modal__hint-row">
                {errors.content && (
                  <p id="post-content-error" className="input-error" role="alert">
                    {errors.content}
                  </p>
                )}
              </div>
            </div>

            {/* Media Upload Area */}
            <div className="post-form-modal__media-wrapper">
              <div className="post-form-modal__label-row">
                <span className="input-label">{t('community:postForm.mediaLabel')}</span>
              </div>

              <input
                ref={fileInputRef}
                id="post-media-input"
                type="file"
                accept="image/*,video/*"
                multiple
                disabled={isFormBusy || media.length >= MEDIA_MAX_FILES}
                onChange={handleFileChange}
                className="post-form-modal__file-hidden"
              />

              <div className="post-form-modal__upload-box">
                <button
                  type="button"
                  disabled={isFormBusy || media.length >= MEDIA_MAX_FILES}
                  onClick={() => fileInputRef.current?.click()}
                  className="btn-secondary post-form-modal__browse-btn"
                >
                  <span className="material-symbols-outlined">add_photo_alternate</span>
                  <span>{t('community:postForm.browseDevice')}</span>
                </button>
                <span className="post-form-modal__upload-note">
                  {t('community:postForm.mediaNote', { max: MEDIA_MAX_FILES })}
                </span>
              </div>

              {errors.media && (
                <p className="input-error post-form-modal__media-error" role="alert">
                  {errors.media}
                </p>
              )}

              {/* Media Preview Grid */}
              {media.length > 0 && (
                <div className="post-form-modal__preview-grid">
                  {media.map((item, index) => (
                    <figure key={item.id} className="post-form-modal__preview-item">
                      {item.type === 'video' ? (
                        <video src={item.url} className="post-form-modal__preview-asset" muted />
                      ) : (
                        <img src={item.url} alt="" className="post-form-modal__preview-asset" />
                      )}
                      <div className="post-form-modal__preview-bar">
                        {item.type === 'video' && (
                          <span className="post-form-modal__preview-tag">
                            {t('community:postCard.videoBadge')}
                          </span>
                        )}
                        <button
                          type="button"
                          disabled={isFormBusy}
                          onClick={() => handleRemoveMedia(item.id)}
                          className="post-form-modal__remove-btn btn-icon"
                        >
                          <span className="material-symbols-outlined">close</span>
                        </button>
                      </div>
                    </figure>
                  ))}

                  {media.length < MEDIA_MAX_FILES && (
                    <button
                      type="button"
                      disabled={isFormBusy}
                      onClick={() => fileInputRef.current?.click()}
                      className="post-form-modal__add-more-btn"
                    >
                      <span className="material-symbols-outlined">add</span>
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Footer Actions */}
          <footer className="post-form-modal__footer">
            <button
              type="button"
              disabled={isFormBusy}
              onClick={handleClose}
              className="btn-secondary post-form-modal__cancel-btn"
            >
              {t('community:postForm.cancel')}
            </button>
            <button
              type="submit"
              disabled={isFormBusy}
              className="btn-primary post-form-modal__submit-btn"
            >
              <span className={`material-symbols-outlined ${isFormBusy ? 'animate-spin' : ''}`}>
                {isFormBusy ? 'progress_activity' : 'send'}
              </span>
              <span>
                {uploadProgressText ||
                  (isFormBusy
                    ? t('community:postForm.processing')
                    : t(
                        isEditing ? 'community:postForm.saveChanges' : 'community:postForm.submit'
                      ))}
              </span>
            </button>
          </footer>
        </form>
      </div>
    </div>,
    document.body
  );
}

export default PostFormModal;
