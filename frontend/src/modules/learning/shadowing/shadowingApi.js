import apiClient from '../../../services/apiClient'

/**
 * Cập nhật tiến độ Shadowing cho 1 segment trong bài học.
 *
 * @param {string} slug - Slug của bài học (ví dụ: 'rick-astley-never-gonna-give-you-up')
 * @param {string|number} segmentId - ID của phân đoạn cần nộp điểm
 * @param {Object} payload - Dữ liệu chấm điểm Shadowing
 * @param {number} payload.attemptCount - Số lần thử hiện tại (1..1000)
 * @param {number} payload.score - Điểm số nguyên từ 0..100
 * @param {Array<{word: string, accuracy: number}>} payload.wordsAccuracy - Mảng độ chính xác từng từ
 * @param {string} payload.audioUrl - verifiedFileKey nhận từ uploadFileForVerification
 * @returns {Promise<Object>} response.data chứa { score, passed, segmentProgress, lessonProgress }
 */
export async function patchShadowingProgressApi(slug, segmentId, payload) {
  const response = await apiClient.patch(
    `/lessons/${encodeURIComponent(slug)}/segments/${encodeURIComponent(segmentId)}/shadowing-progress`,
    payload
  )
  return response.data
}
