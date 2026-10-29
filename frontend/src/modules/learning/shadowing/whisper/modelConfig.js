/**
 * Cấu hình mô hình Whisper cho Transformers.js.
 * - Mô hình nhẹ (whisper-tiny.en): ~39MB, phù hợp mobile hoặc máy yếu.
 * - Mô hình chuẩn (whisper-base.en): ~73MB, phù hợp desktop/máy tính có GPU hoặc CPU ổn định.
 */

export const MODEL_OPTIONS = {
  LIGHT: 'onnx-community/whisper-tiny.en',
  STANDARD: 'onnx-community/whisper-base.en',
};

/**
 * Đoán xem thiết bị hiện tại có phải mobile hay không.
 * @returns {boolean}
 */
export function isMobileDevice() {
  if (typeof navigator === 'undefined') return false;

  if (navigator.userAgentData?.mobile !== undefined) {
    return navigator.userAgentData.mobile;
  }

  if (typeof window !== 'undefined' && window.matchMedia) {
    const coarsePointer = window.matchMedia('(pointer: coarse)').matches;
    const smallScreen = window.innerWidth <= 768;
    if (coarsePointer && smallScreen) return true;
  }

  const ua = navigator.userAgent || '';
  return /Android|iPhone|iPad|iPod|Opera Mini|IEMobile|WPDesktop/i.test(ua);
}

/**
 * Chọn cấu hình mô hình Whisper cho môi trường chạy.
 * @param {Object} [params]
 * @param {boolean} [params.isMobile]
 * @param {'light'|'standard'|null} [params.userOverride] - Lựa chọn ghi đè của người dùng
 * @returns {{ modelId: string, device: 'wasm', dtype: 'q8', modelType: 'light'|'standard' }}
 */
export function pickModelConfig({ isMobile = false, userOverride = null } = {}) {
  const modelType = userOverride || (isMobile ? 'light' : 'standard');
  const modelId = modelType === 'light' ? MODEL_OPTIONS.LIGHT : MODEL_OPTIONS.STANDARD;

  return {
    modelId,
    device: 'wasm',
    dtype: 'q8',
    modelType,
  };
}
