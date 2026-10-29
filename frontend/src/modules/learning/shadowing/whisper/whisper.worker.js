import { pipeline } from '@huggingface/transformers';

/**
 * Web Worker phụ trách tải mô hình Whisper và thực hiện nhận dạng giọng nói (ASR).
 * Chạy tách biệt hoàn toàn khỏi Main Thread để không gây giật lag giao diện.
 */

let transcriber = null;
let currentModelId = null;
let isLoading = false;

self.onmessage = async (e) => {
  const { type, payload } = e.data || {};

  if (type === 'load') {
    const { modelId, device, dtype } = payload || {};

    // Bỏ qua nếu model đã sẵn sàng
    if (transcriber && currentModelId === modelId) {
      self.postMessage({ type: 'ready', payload: { modelId } });
      return;
    }

    // Bỏ qua lệnh 'load' trùng khi đang trong quá trình tải
    if (isLoading) {
      return;
    }

    isLoading = true;
    self.postMessage({ type: 'loading', payload: { progress: 0 } });

    let timeoutId = null;
    const timeoutPromise = new Promise((_, reject) => {
      timeoutId = setTimeout(() => {
        reject(new Error('Tải mô hình Whisper quá thời gian chờ (120s). Vui lòng thử lại.'));
      }, 120000);
    });

    try {
      const pipelinePromise = pipeline('automatic-speech-recognition', modelId, {
        device: device || 'wasm',
        dtype: dtype || 'q8',
        progress_callback: (progressInfo) => {
          if (progressInfo.status === 'progress') {
            self.postMessage({
              type: 'loading',
              payload: {
                progress: Math.round(progressInfo.progress || 0),
                file: progressInfo.file,
              },
            });
          }
        },
      });

      transcriber = await Promise.race([pipelinePromise, timeoutPromise]);
      currentModelId = modelId;
      self.postMessage({ type: 'ready', payload: { modelId } });
    } catch (err) {
      console.error('[Whisper Worker] Lỗi tải mô hình:', err);
      self.postMessage({
        type: 'error',
        payload: {
          code: 'LOAD_FAILED',
          message: err?.message || 'Không thể tải mô hình Whisper',
        },
      });
    } finally {
      if (timeoutId) clearTimeout(timeoutId);
      isLoading = false;
    }
  } else if (type === 'transcribe') {
    const { audio } = payload || {};
    try {
      if (!transcriber) {
        throw new Error('Mô hình Whisper chưa sẵn sàng');
      }

      // transcribe audio Float32Array 16 kHz (model .en không nhận param language/task)
      const result = await transcriber(audio, {
        chunk_length_s: 30,
      });

      self.postMessage({
        type: 'transcribed',
        payload: {
          text: result?.text || '',
        },
      });
    } catch (err) {
      console.error('[Whisper Worker] Lỗi nhận dạng âm thanh:', err);
      self.postMessage({
        type: 'error',
        payload: {
          code: 'TRANSCRIBE_FAILED',
          message: err?.message || 'Lỗi khi nhận dạng âm thanh',
        },
      });
    }
  }
};
