/**
 * Lớp quản lý ghi âm Micro trên trình duyệt và xuất ra Float32Array mono 16000 Hz.
 * Sử dụng AudioContext + MediaStreamAudioSourceNode + AudioWorklet/ScriptProcessor hoặc resample OfflineAudioContext.
 *
 * Đảm bảo:
 * - 16 kHz mono Float32Array (-1.0 đến 1.0).
 * - Dọn dẹp triệt để MediaStream tracks, AudioContext khi dừng hoặc lỗi.
 * - Giới hạn thời gian tự động: maxDurationSec (10s..60s).
 */
export class AudioRecorder {
  constructor({ maxDurationSec = 30, onAutoStop } = {}) {
    this.maxDurationSec = Math.max(10, Math.min(60, maxDurationSec))
    this.onAutoStop = onAutoStop
    this.stream = null
    this.audioContext = null
    this.source = null
    this.processor = null
    this.recordedChunks = []
    this.isRecording = false
    this.timer = null
    this.startTime = 0
  }

  /**
   * Bắt đầu ghi âm từ micro người dùng.
   * @returns {Promise<void>}
   */
  async start() {
    if (this.isRecording) return

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      const err = new Error('NOT_SUPPORTED')
      err.code = 'NOT_SUPPORTED'
      throw err
    }

    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      })
    } catch (err) {
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        const pErr = new Error('PERMISSION_DENIED')
        pErr.code = 'PERMISSION_DENIED'
        throw pErr
      }
      throw err
    }

    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    this.audioContext = new AudioContextClass()
    this.source = this.audioContext.createMediaStreamSource(this.stream)

    // Dùng buffer 4096 samples để thu thập PCM ở sampleRate hiện tại
    this.processor = this.audioContext.createScriptProcessor(4096, 1, 1)
    this.recordedChunks = []
    this.isRecording = true
    this.startTime = Date.now()

    this.processor.onaudioprocess = (e) => {
      if (!this.isRecording) return
      const input = e.inputBuffer.getChannelData(0)
      this.recordedChunks.push(new Float32Array(input))
    }

    this.source.connect(this.processor)
    this.processor.connect(this.audioContext.destination)

    // Tự động dừng khi vượt quá thời lượng tối đa
    if (this.timer) clearTimeout(this.timer)
    this.timer = setTimeout(() => {
      if (this.isRecording) {
        if (typeof this.onAutoStop === 'function') {
          this.onAutoStop()
        }
      }
    }, this.maxDurationSec * 1000)
  }

  /**
   * Dừng ghi âm và trả về mảng mẫu Float32Array 16000 Hz mono.
   * @returns {Promise<{ audio16k: Float32Array, durationSec: number }>}
   */
  async stop() {
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }

    this.isRecording = false
    const durationSec = (Date.now() - this.startTime) / 1000

    const currentContextSampleRate = this.audioContext ? this.audioContext.sampleRate : 44100
    const rawChunks = [...this.recordedChunks]
    this.recordedChunks = []

    this.cleanup()

    // Ghép các chunk lại thành một mảng Float32Array duy nhất
    const totalLength = rawChunks.reduce((acc, c) => acc + c.length, 0)
    const merged = new Float32Array(totalLength)
    let offset = 0
    for (const chunk of rawChunks) {
      merged.set(chunk, offset)
      offset += chunk.length
    }

    // Nếu thời lượng quá ngắn (< 0.5s), coi như không có âm thanh
    if (durationSec < 0.5 || totalLength === 0) {
      return { audio16k: new Float32Array(0), durationSec }
    }

    // Resample về chuẩn 16000 Hz nếu sampleRate khác 16000
    const audio16k = await resampleAudio(merged, currentContextSampleRate, 16000)
    return { audio16k, durationSec }
  }

  /**
   * Dọn dẹp tài nguyên phần cứng & AudioContext
   */
  cleanup() {
    this.isRecording = false
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }

    if (this.processor) {
      try {
        this.processor.disconnect()
      } catch {
        /* noop */
      }
      this.processor = null
    }

    if (this.source) {
      try {
        this.source.disconnect()
      } catch {
        /* noop */
      }
      this.source = null
    }

    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop())
      this.stream = null
    }

    if (this.audioContext && this.audioContext.state !== 'closed') {
      try {
        this.audioContext.close()
      } catch {
        /* noop */
      }
      this.audioContext = null
    }
  }
}

/**
 * Resample âm thanh từ sampleRate gốc sang targetSampleRate (16000 Hz) bằng OfflineAudioContext.
 * @param {Float32Array} audioData
 * @param {number} origSampleRate
 * @param {number} targetSampleRate
 * @returns {Promise<Float32Array>}
 */
async function resampleAudio(audioData, origSampleRate, targetSampleRate = 16000) {
  if (origSampleRate === targetSampleRate) {
    return audioData
  }

  const OfflineCtxClass = window.OfflineAudioContext || window.webkitOfflineAudioContext
  const targetLength = Math.round((audioData.length * targetSampleRate) / origSampleRate)
  const offlineCtx = new OfflineCtxClass(1, targetLength, targetSampleRate)

  const buffer = offlineCtx.createBuffer(1, audioData.length, origSampleRate)
  buffer.copyToChannel(audioData, 0)

  const source = offlineCtx.createBufferSource()
  source.buffer = buffer
  source.connect(offlineCtx.destination)
  source.start(0)

  const renderedBuffer = await offlineCtx.startRendering()
  return renderedBuffer.getChannelData(0)
}
