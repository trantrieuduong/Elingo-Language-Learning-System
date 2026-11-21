import { useState, useCallback } from 'react';

/**
 * Hook tiện ích để quản lý mở/đóng ReportModal cho bất kỳ đối tượng nào (POST, COMMENT, USER, DECK, LESSON)
 */
export function useReportModal() {
  const [reportState, setReportState] = useState({
    isOpen: false,
    targetType: null,
    targetId: null,
    targetLabel: '',
  });

  const openReport = useCallback(({ targetType, targetId, targetLabel }) => {
    setReportState({
      isOpen: true,
      targetType,
      targetId,
      targetLabel,
    });
  }, []);

  const closeReport = useCallback(() => {
    setReportState((prev) => ({
      ...prev,
      isOpen: false,
    }));
  }, []);

  return {
    isReportOpen: reportState.isOpen,
    targetType: reportState.targetType,
    targetId: reportState.targetId,
    targetLabel: reportState.targetLabel,
    openReport,
    closeReport,
  };
}

export default useReportModal;
