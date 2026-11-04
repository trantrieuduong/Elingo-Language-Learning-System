import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import './PostCardCommentInput.css';

const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
const COMMENT_MAX_LENGTH = 1000;

function PostCardCommentInput({
  avatarUrl,
  userName = 'User',
  onCommentError,
  onSubmit,
  mode = 'create',
  initialContent = '',
  initialImageUrl = '',
  onCancel,
  placeholder,
}) {
  const { t } = useTranslation();
  const imageInputRef = useRef(null);
  const isEditMode = mode === 'edit';
  const isReplyMode = mode === 'reply';
  const [comment, setComment] = useState(initialContent);
  const [imageFile, setImageFile] = useState(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState('');
  const [existingImageUrl, setExistingImageUrl] = useState(initialImageUrl);
  const [imageError, setImageError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!imagePreviewUrl) return undefined;
    return () => URL.revokeObjectURL(imagePreviewUrl);
  }, [imagePreviewUrl]);

  const handleImageChange = (event) => {
    const selectedFile = event.target.files?.[0];
    setImageError('');

    if (!selectedFile) return;
    if (!selectedFile.type.startsWith('image/')) {
      setImageError(t('community:postCard.imageTypeError'));
      return;
    }
    if (selectedFile.size > MAX_IMAGE_SIZE_BYTES) {
      setImageError(t('community:postCard.imageSizeError'));
      return;
    }

    setImageFile(selectedFile);
    setImagePreviewUrl(URL.createObjectURL(selectedFile));
    setExistingImageUrl('');
  };

  const handleRemoveImage = () => {
    setImageFile(null);
    setImagePreviewUrl('');
    setExistingImageUrl('');
    setImageError('');
  };

  const handleKeyDown = (event) => {
    if (event.key !== 'Enter' || event.shiftKey) {
      return;
    }
    event.currentTarget.form?.requestSubmit();
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!comment.trim() || isSubmitting) return;
    if (comment.trim().length > COMMENT_MAX_LENGTH) {
      onCommentError?.(
        t('community:comments.contentMaxLength', { max: COMMENT_MAX_LENGTH.toLocaleString() })
      );
      return;
    }

    setIsSubmitting(true);
    try {
      const succeeded = onSubmit
        ? isEditMode
          ? await onSubmit(comment.trim(), imageFile, existingImageUrl || null)
          : await onSubmit(comment.trim(), imageFile)
        : true;
      if (succeeded !== false && !isEditMode) {
        setComment('');
        handleRemoveImage();
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className={`post-card-comment-input${isEditMode ? ' post-card-comment-input--edit' : ''}${
        isReplyMode ? ' post-card-comment-input--reply' : ''
      }`}
    >
      {!isEditMode && (
        <img
          src={avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(userName)}`}
          alt={userName}
          className="post-card-comment-input__avatar"
        />
      )}

      <div className="post-card-comment-input__field glass-well">
        <textarea
          value={comment}
          onChange={(event) => setComment(event.target.value)}
          onKeyDown={handleKeyDown}
          disabled={isSubmitting}
          rows={isEditMode ? 3 : 1}
          autoFocus={isEditMode}
          placeholder={placeholder || t('community:postCard.writeComment')}
          className="post-card-comment-input__textarea"
        />

        <input
          ref={imageInputRef}
          type="file"
          accept="image/*"
          onChange={handleImageChange}
          className="post-card-comment-input__file-input"
          tabIndex={-1}
          aria-label={t('community:postCard.attachImage')}
        />
        <button
          type="button"
          className="btn-icon glass-thumb post-card-comment-input__image-btn"
          onClick={() => imageInputRef.current?.click()}
          disabled={isSubmitting}
          aria-label={t('community:postCard.attachImage')}
          title={t('community:postCard.attachImage')}
        >
          <span className="material-symbols-outlined">image</span>
        </button>
        {isEditMode ? (
          <>
            <button
              type="button"
              className="btn-secondary post-card-comment-input__cancel-btn"
              onClick={onCancel}
              disabled={isSubmitting}
            >
              {t('community:comments.cancel')}
            </button>
            <button
              type="submit"
              disabled={!comment.trim() || isSubmitting}
              className="btn-primary post-card-comment-input__submit-btn"
              aria-label={
                isSubmitting ? t('community:comments.saving') : t('community:comments.save')
              }
            >
              {isSubmitting ? (
                <span className="app-loading-spinner" />
              ) : (
                <span className="material-symbols-outlined">check</span>
              )}
            </button>
          </>
        ) : (
          <button
            type="submit"
            disabled={!comment.trim() || isSubmitting}
            className="btn-primary post-card-comment-input__submit-btn"
            aria-label={
              isReplyMode ? t('community:comments.reply') : t('community:postCard.comment')
            }
          >
            <span className="material-symbols-outlined">send</span>
          </button>
        )}
      </div>
      {(imagePreviewUrl || existingImageUrl) && (
        <div className="post-card-comment-input__image-preview">
          <img
            src={imagePreviewUrl || existingImageUrl}
            alt={imageFile?.name || t('community:comments.attachedImage', { name: userName })}
          />
          <button
            type="button"
            className="btn-icon glass-thumb post-card-comment-input__remove-image"
            onClick={handleRemoveImage}
            disabled={isSubmitting}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
      )}
      {imageError && (
        <p className="post-card-comment-input__error" role="alert">
          {imageError}
        </p>
      )}
      {isEditMode && (
        <button
          type="button"
          className="post-card-comment-input__cancel-mobile btn-secondary"
          onClick={onCancel}
          disabled={isSubmitting}
        >
          {t('community:comments.cancel')}
        </button>
      )}
    </form>
  );
}

export default PostCardCommentInput;
