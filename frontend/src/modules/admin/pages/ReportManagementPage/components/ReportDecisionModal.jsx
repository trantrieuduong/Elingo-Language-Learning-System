import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminDecisionModal from '../../../components/AdminModal/AdminDecisionModal';
import { rejectReport, resolveReport } from '../../../services/adminCommunityApi';

export default function ReportDecisionModal({
  isOpen,
  mode,
  report,
  onClose,
  onSuccess,
}) {
  const { t } = useTranslation('admin');

  if (!isOpen || !report) return null;

  const isReject = mode === 'reject';

  const handleConfirm = async (resolutionNote) => {
    if (isReject) {
      await rejectReport(report.id, resolutionNote);
    } else {
      await resolveReport(report.id, resolutionNote);
    }
    onSuccess(mode);
  };

  return (
    <AdminDecisionModal
      isOpen={isOpen}
      isReject={isReject}
      resetKey={`${mode}-${report.id}`}
      title={isReject ? t('reports.decision.rejectTitle') : t('reports.decision.resolveTitle')}
      subtitle={`#${report.id} — ${t(`reports.targetType.${report.targetType}`, report.targetType)} #${report.targetId}`}
      closeLabel={t('reports.detail.close')}
      noteInputId="report-resolution-note"
      noteLabel={t('reports.decision.noteLabel')}
      notePlaceholder={
        isReject
          ? t('reports.decision.rejectPlaceholder')
          : t('reports.decision.resolvePlaceholder')
      }
      noteRequiredMessage={t('reports.decision.noteRequired')}
      submitErrorFallback={t('reports.decision.submitError')}
      cancelLabel={t('reports.decision.cancel')}
      submitLabel={isReject ? t('reports.actions.reject') : t('reports.actions.resolve')}
      submittingLabel={t('reports.decision.submitting')}
      onClose={onClose}
      onConfirm={handleConfirm}
    />
  );
}

ReportDecisionModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(['resolve', 'reject']).isRequired,
  report: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func.isRequired,
};
