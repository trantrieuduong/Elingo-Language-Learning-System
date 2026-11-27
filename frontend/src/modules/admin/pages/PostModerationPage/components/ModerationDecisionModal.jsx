import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminDecisionModal from '../../../components/AdminModal/AdminDecisionModal';
import { approveModerationPost, rejectModerationPost } from '../../../services/adminCommunityApi';

export default function ModerationDecisionModal({
  isOpen,
  mode,
  post,
  onClose,
  onSuccess,
}) {
  const { t } = useTranslation('admin');

  if (!isOpen || !post) return null;

  const isReject = mode === 'reject';

  const handleConfirm = async (note) => {
    if (isReject) {
      await rejectModerationPost(post.id, note);
    } else {
      await approveModerationPost(post.id, note);
    }
    onSuccess(mode);
  };

  return (
    <AdminDecisionModal
      isOpen={isOpen}
      isReject={isReject}
      resetKey={`${mode}-${post.id}`}
      title={
        isReject
          ? t('postModeration.decision.rejectTitle')
          : t('postModeration.decision.approveTitle')
      }
      subtitle={`#${post.id} — ${post.title}`}
      closeLabel={t('postModeration.detail.close')}
      noteInputId="moderation-note"
      noteLabel={t('postModeration.decision.noteLabel')}
      notePlaceholder={
        isReject
          ? t('postModeration.decision.rejectPlaceholder')
          : t('postModeration.decision.approvePlaceholder')
      }
      noteRequiredMessage={t('postModeration.decision.noteRequired')}
      submitErrorFallback={t('postModeration.decision.submitError')}
      cancelLabel={t('postModeration.decision.cancel')}
      submitLabel={
        isReject ? t('postModeration.actions.reject') : t('postModeration.actions.approve')
      }
      submittingLabel={t('postModeration.decision.submitting')}
      onClose={onClose}
      onConfirm={handleConfirm}
    />
  );
}

ModerationDecisionModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['approve', 'reject']).isRequired,
  post: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
