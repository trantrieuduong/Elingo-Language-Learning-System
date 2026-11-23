import { useState, useEffect, useRef, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import Input from '../../../../components/Input/Input';
import FormAlert from '../../../../components/FormAlert/FormAlert';
import { uploadFileForVerification } from '../../../../services/uploadFile';
import { updateProfileApi } from '../../profileApi';
import { getFallbackAvatarUrl } from '../../../../utils/utils';
import {
  ALLOWED_IMAGE_ACCEPT,
  ALLOWED_IMAGE_FORMATS,
  ALLOWED_IMAGE_TYPES,
  MAX_IMAGE_FILE_SIZE,
} from '../../../../constants/fileTypes';
import './UpdateProfileModal.css';

const USERNAME_REGEX = /^[a-zA-Z0-9]{3,15}$/;
const MAX_FULL_NAME_LENGTH = 150;
const MAX_IMAGE_FILE_SIZE_MB = MAX_IMAGE_FILE_SIZE / 1024 / 1024;

/**
 * UpdateProfileModal - Modal cập nhật thông tin cá nhân của người dùng đang đăng nhập
 *
 * @param {Object} props
 * @param {boolean} props.isOpen - Trạng thái hiển thị modal
 * @param {Object} props.profile - Dữ liệu profile hiện tại (fullName, username, avatarUrl)
 * @param {Function} props.onClose - Callback đóng modal
 * @param {Function} props.onSuccess - Callback khi cập nhật thành công
 */
function UpdateProfileModal({ isOpen, profile, onClose, onSuccess }) {
  const { t } = useTranslation(['user', 'api', 'common']);
  const fileInputRef = useRef(null);

  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState(null);
  const [avatarFile, setAvatarFile] = useState(null);
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Khởi tạo state từ profile prop khi mở modal
  useEffect(() => {
    if (!isOpen) return;

    setFullName(profile.fullName);
    setUsername(profile.username);
    setAvatarPreviewUrl(profile.avatarUrl);
    setAvatarFile(null);
    setErrors({});
    setGeneralError('');
    setIsSubmitting(false);
  }, [isOpen, profile]);

  // Clean up object URL khi preview thay đổi hoặc modal unmount
  useEffect(() => {
    return () => {
      if (avatarPreviewUrl && avatarPreviewUrl.startsWith('blob:')) {
        URL.revokeObjectURL(avatarPreviewUrl);
      }
    };
  }, [avatarPreviewUrl]);

  // Khóa scroll body khi mở modal
  useEffect(() => {
    if (!isOpen) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  // Lắng nghe phím Escape để đóng modal
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen && !isSubmitting) {
        handleClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isSubmitting]);

  const handleClose = useCallback(() => {
    if (isSubmitting) return;
    setErrors({});
    setGeneralError('');
    setAvatarFile(null);
    onClose?.();
  }, [isSubmitting, onClose]);

  if (!isOpen) return null;

  const handleAvatarSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Reset value input để cho phép chọn lại cùng 1 file
    e.target.value = '';

    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      setGeneralError(t('user:updateProfile.avatarInvalidType', {
        formats: ALLOWED_IMAGE_FORMATS,
      }));
      return;
    }

    if (file.size > MAX_IMAGE_FILE_SIZE) {
      setGeneralError(t('user:updateProfile.avatarFileTooLarge', {
        maxSize: MAX_IMAGE_FILE_SIZE_MB,
      }));
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    setAvatarFile(file);
    setAvatarPreviewUrl(previewUrl);
    setGeneralError('');
  };

  const handleFullNameChange = (e) => {
    const val = e.target.value;
    setFullName(val);
    if (errors.fullName) {
      setErrors((prev) => ({ ...prev, fullName: '' }));
    }
    if (generalError) setGeneralError('');
  };

  const handleUsernameChange = (e) => {
    const val = e.target.value;
    setUsername(val);
    if (errors.username) {
      setErrors((prev) => ({ ...prev, username: '' }));
    }
    if (generalError) setGeneralError('');
  };

  const validate = () => {
    const newErrors = {};
    const trimmedFullName = fullName.trim();
    const trimmedUsername = username.trim();

    if (!trimmedFullName) {
      newErrors.fullName = t('user:updateProfile.errorFullNameRequired');
    } else if (trimmedFullName.length > MAX_FULL_NAME_LENGTH) {
      newErrors.fullName = t('user:updateProfile.errorFullNameMaxLength');
    }

    if (!trimmedUsername) {
      newErrors.username = t('user:updateProfile.errorUsernameRequired');
    } else if (!USERNAME_REGEX.test(trimmedUsername)) {
      newErrors.username = t('user:updateProfile.errorUsernameFormat');
    }

    setErrors(newErrors);

    if (trimmedFullName.length > MAX_FULL_NAME_LENGTH) {
      setGeneralError(t('user:updateProfile.errorFullNameMaxLength'));
    }

    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting) return;

    setGeneralError('');
    if (!validate()) return;

    setIsSubmitting(true);

    try {
      let verifiedAvatarKey;

      if (avatarFile) {
        verifiedAvatarKey = await uploadFileForVerification(avatarFile, avatarFile.type);
      }

      const payload = {
        fullName: fullName.trim(),
        username: username.trim(),
      };

      if (verifiedAvatarKey) {
        payload.avatarUrl = verifiedAvatarKey;
      }

      const res = await updateProfileApi(payload);

      if (res?.success && res?.data) {
        onSuccess?.(res.data);
        handleClose();
      } else {
        const errorCode = res?.errors?.[0]?.code || res?.code;
        const fallbackMsg = res?.errors?.[0]?.message || res?.message || t('api:common.UNKNOWN_ERROR');
        const translatedMsg = errorCode ? t(`api:error.${errorCode}`, { defaultValue: fallbackMsg }) : fallbackMsg;
        setGeneralError(translatedMsg);
      }
    } catch (err) {
      const errorData = err?.response?.data;
      const errorCode = errorData?.errors?.[0]?.code || errorData?.code;
      const fallbackMsg = errorData?.errors?.[0]?.message || errorData?.message || t('api:common.UNKNOWN_ERROR');
      const translatedMsg = errorCode ? t(`api:error.${errorCode}`, { defaultValue: fallbackMsg }) : fallbackMsg;
      setGeneralError(translatedMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const currentAvatarSrc = avatarPreviewUrl || getFallbackAvatarUrl(profile.fullName);

  return createPortal(
    <div className="update-profile-modal-overlay">
      <div
        className="update-profile-modal-container glass-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
      >
        <header className="update-profile-modal-header">
          <h2 id="update-profile-modal-title" className="update-profile-modal-title text-headline-sm">
            {t('user:editProfile')}
          </h2>
          <button
            type="button"
            className="btn-icon update-profile-modal-close"
            onClick={handleClose}
            disabled={isSubmitting}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        {generalError && <FormAlert message={generalError} />}

        <form className="update-profile-modal-form" onSubmit={handleSubmit}>
          {/* Avatar Section */}
          <div className="update-profile-avatar-section glass-well">
            <div className="update-profile-avatar-preview">
              <img
                src={currentAvatarSrc}
                alt={fullName}
                className="update-profile-avatar-img"
              />
            </div>
            <div className="update-profile-avatar-controls">
              <button
                type="button"
                className="btn-secondary update-profile-avatar-btn"
                onClick={() => fileInputRef.current?.click()}
                disabled={isSubmitting}
              >
                <span className="material-symbols-outlined">photo_camera</span>
                <span>{t('user:updateProfile.changeAvatar')}</span>
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept={ALLOWED_IMAGE_ACCEPT}
                className="update-profile-avatar-file-input"
                onChange={handleAvatarSelect}
                disabled={isSubmitting}
              />
              <p className="update-profile-avatar-help">
                {t('user:updateProfile.avatarHelp', {
                  formats: ALLOWED_IMAGE_FORMATS,
                  maxSize: MAX_IMAGE_FILE_SIZE_MB,
                })}
              </p>
            </div>
          </div>

          {/* Full Name Field */}
          <Input
            id="update-profile-fullname"
            label={t('user:updateProfile.fullName')}
            value={fullName}
            onChange={handleFullNameChange}
            placeholder={t('user:updateProfile.fullNamePlaceholder')}
            error={errors.fullName}
            disabled={isSubmitting}
          />

          {/* Username Field */}
          <div className="update-profile-username-group">
            <Input
              id="update-profile-username"
              label={t('user:updateProfile.username')}
              value={username}
              onChange={handleUsernameChange}
              placeholder={t('user:updateProfile.usernamePlaceholder')}
              error={errors.username}
              disabled={isSubmitting}
            />
            <p className="update-profile-field-hint">
              {t('user:updateProfile.usernameCooldownNote')}
            </p>
          </div>

          {/* Modal Actions */}
          <div className="update-profile-modal-actions">
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
              className="btn-primary update-profile-submit-btn"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <span className="update-profile-spinner" />
                  <span>{t('common:saving')}</span>
                </>
              ) : (
                t('common:saveChanges')
              )}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}

export default UpdateProfileModal;
