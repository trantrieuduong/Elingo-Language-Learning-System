import apiClient from '../../../services/apiClient';

/**
 * Cập nhật tiến độ học Dictation cho một phân đoạn (segment) của bài học.
 * Server sẽ chấm điểm dựa trên attemptCount, hintUsedCount và số từ của segment.
 * Endpoint: PATCH /lessons/{lessonId}/segments/{segmentId}/dictation-progress
 *
 * @param {number} lessonId - ID của bài học
 * @param {number|string} segmentId - ID của phân đoạn (segment)
 * @param {Object} data - Dữ liệu nộp bài
 * @param {number} data.attemptCount - Số lần thử nộp cho segment này (tối thiểu 1)
 * @param {number} data.hintUsedCount - Số từ đã mở gợi ý (tối thiểu 0)
 * @returns {Promise<Object>} Object chứa response.data ({ score, passed, segmentProgress, lessonProgress })
 */
export const patchDictationProgressApi = async (
  lessonId,
  segmentId,
  { attemptCount, hintUsedCount }
) => {
  const response = await apiClient.patch(
    `/lessons/${encodeURIComponent(lessonId)}/segments/${encodeURIComponent(segmentId)}/dictation-progress`,
    { attemptCount, hintUsedCount }
  );
  return response.data;
};
