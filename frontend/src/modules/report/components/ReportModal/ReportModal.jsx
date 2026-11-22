import { useCallback, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import { uploadFileForVerification } from '../../../../services/uploadFile';
import { ALLOWED_IMAGE_ACCEPT } from '../../../../constants/fileTypes';
import { createReportApi } from '../../reportApi';
import '../../../../components/Input/Input.css';
import './ReportModal.css';

const DESCRIPTION_MIN_LENGTH = 10;
const DESCRIPTION_MAX_LENGTH = 1000;
const MAX_IMAGES = 5;

/**
 * @param {'POST'|'COMMENT'|'USER'|'DECK'|'LESSON'} targetType
 */
function ReportModal({
  isOpen,
  onClose,
  targetType,
  targetId,
  targetLabel = '',
  onSuccess,
}) {
  const { t } = useTranslation(['report', 'common']);
  const fileInputRef = useRef(null);

  const [description, setDescription] = useState('');
  const [descriptionError, setDescriptionError] = useState('');
  const [selectedFiles, setSelectedFiles] = useState([]); // { file, previewUrl }
  const [errorAlert, setErrorAlert] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const resetState = useCallback(() => {
    setDescription('');
    setDescriptionError('');
    setSelectedFiles((prev) => {
      prev.forEach((item) => {
        if (item.previewUrl) URL.revokeObjectURL(item.previewUrl);
      });
      return [];
    });
    setErrorAlert('');
    setIsSubmitting(false);
  }, []);

  const handleClose = useCallback(() => {
    if (isSubmitting) return;
    resetState();
    onClose?.();
  }, [isSubmitting, onClose, resetState]);

  useEffect(() => {
    if (isOpen) {
      resetState();
    }
  }, [isOpen, resetState]);

  // Dọn dẹp object URL khi unmount
  useEffect(() => {
    return () => {
      selectedFiles.forEach((item) => {
        if (item.previewUrl) URL.revokeObjectURL(item.previewUrl);
      });
    };
  }, [selectedFiles]);

  // Xử lý phím ESC và khóa scroll body
  useEffect(() => {
    if (!isOpen) return undefined;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !isSubmitting) {
        handleClose();
      }
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, isSubmitting, handleClose]);

  if (!isOpen) return null;

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;

    const remainingSlots = MAX_IMAGES - selectedFiles.length;
    const acceptedFiles = files.slice(0, remainingSlots);
    const newItems = acceptedFiles.map((file) => ({
      file,
      previewUrl: URL.createObjectURL(file),
    }));

    setSelectedFiles((prev) => [...prev, ...newItems]);
    if (files.length > remainingSlots) {
      setErrorAlert(t('common:firstFilesSelected', { slots: remainingSlots }));
    } else {
      setErrorAlert('');
    }

    e.target.value = '';
  };

  const handleRemoveFile = (indexToRemove) => {
    if (isSubmitting) return;
    setSelectedFiles((prev) => {
      const removed = prev[indexToRemove];
      if (removed?.previewUrl) URL.revokeObjectURL(removed.previewUrl);
      return prev.filter((_, idx) => idx !== indexToRemove);
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting) return;

    const trimmedDesc = description.trim();
    if (!trimmedDesc) {
      setDescriptionError(t('report:errors.descriptionRequired'));
      return;
    }
    if (trimmedDesc.length > DESCRIPTION_MAX_LENGTH) {
      setDescriptionError(t('report:errors.descriptionMax'));
      return;
    }

    setIsSubmitting(true);
    setErrorAlert('');
    setDescriptionError('');

    try {
      // Upload các ảnh đính kèm qua uploadFileForVerification()
      const imageUrls = [];
      for (const item of selectedFiles) {
        if (item.file) {
          const verifiedKey = await uploadFileForVerification(item.file, item.file.type);
          imageUrls.push(verifiedKey);
        }
      }

      await createReportApi({
        targetType,
        targetId,
        description: trimmedDesc,
        imageUrls,
      });

      onSuccess?.();
      handleClose();
    } catch (err) {
      setErrorAlert(err?.response?.data?.message || err?.message || t('report:errors.submitFailed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return createPortal(
    <div className="report-modal__overlay">
      <div
        className="report-modal__container glass-card"
        role="dialog"
      >
        <header className="report-modal__header">
          <h2 id="report-modal-title" className="text-headline-sm">
            <span className="material-symbols-outlined report-modal__header-icon">flag</span>
            <span>{t('report:modalTitle')}</span>
          </h2>
          <button
            type="button"
            className="btn-icon report-modal__close"
            onClick={handleClose}
            disabled={isSubmitting}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        <form onSubmit={handleSubmit} className="report-modal__form">
          <div className="report-modal__scroll-body">
            {errorAlert && <FormAlert message={errorAlert} />}

            <div className="report-modal__target-info">
              <div className="report-modal__target-header">
                <span>{t('report:report')}:</span>
                <span className="report-modal__target-badge">
                  {t(`report:targetTypes.${targetType}`)}
                </span>
              </div>
              {targetLabel && (
                <div className="report-modal__target-label" title={targetLabel}>
                  {targetLabel}
                </div>
              )}
            </div>

            <div className="report-modal__field">
              <label htmlFor="report-description" className="report-modal__label">
                {t('report:descriptionLabel')} *
              </label>
              <textarea
                id="report-description"
                className={`input-field report-modal__textarea ${descriptionError ? 'input-field--error' : ''}`}
                placeholder={t('report:descriptionPlaceholder')}
                value={description}
                onChange={(e) => {
                  setDescription(e.target.value);
                  if (descriptionError) setDescriptionError('');
                }}
                disabled={isSubmitting}
                rows={4}
              />
              {descriptionError && <p className="input-error">{descriptionError}</p>}
            </div>

            <div className="report-modal__upload-zone">
              <div className="report-modal__label-row">
                <span className="report-modal__label">
                  {t('report:attachImages')}
                </span>
              </div>

              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileChange}
                accept={ALLOWED_IMAGE_ACCEPT}
                multiple
                hidden
                disabled={isSubmitting || selectedFiles.length >= MAX_IMAGES}
              />

              {selectedFiles.length < MAX_IMAGES && (
                <div className="report-modal__upload-box">
                  <button
                    type="button"
                    className="btn-secondary report-modal__upload-btn"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isSubmitting}
                  >
                    <span className="material-symbols-outlined">add_photo_alternate</span>
                    <span>{t('report:chooseFiles')}</span>
                  </button>
                  <span className="report-modal__upload-note">
                    {t('common:maxFiles', { max: MAX_IMAGES })}
                  </span>
                </div>
              )}

              {selectedFiles.length > 0 && (
                <div className="report-modal__media-preview-list">
                  {selectedFiles.map((item, idx) => (
                    <div key={item.previewUrl} className="report-modal__media-item">
                      <img
                        src={item.previewUrl}
                        alt={`Preview ${idx + 1}`}
                        className="report-modal__media-thumb"
                      />
                      <button
                        type="button"
                        className="btn-icon report-modal__media-remove"
                        onClick={() => handleRemoveFile(idx)}
                        disabled={isSubmitting}
                      >
                        <span className="material-symbols-outlined">close</span>
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <footer className="report-modal__footer">
            <button
              type="button"
              className="btn-secondary"
              onClick={handleClose}
              disabled={isSubmitting}
            >
              {t('common:cancel')}
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={isSubmitting || description.trim().length < DESCRIPTION_MIN_LENGTH}
            >
              {isSubmitting ? t('report:submitting') : t('report:submit')}
            </button>
          </footer>
        </form>
      </div>
    </div>,
    document.body
  );
}

export default ReportModal;
