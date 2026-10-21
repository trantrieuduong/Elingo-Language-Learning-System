/**
 * Mã hóa Float32Array PCM sang Blob WAV 16-bit PCM mono.
 * Hàm thuần (pure function), không phụ thuộc DOM hay Web API ngoài DataView.
 *
 * @param {Float32Array} float32Samples - Mảng mẫu âm thanh giá trị từ -1.0 đến 1.0
 * @param {number} sampleRate - Tần số lấy mẫu (mặc định 16000 Hz)
 * @returns {Blob} Blob âm thanh MIME audio/wav
 */
export function encodeWav(float32Samples, sampleRate = 16000) {
  const numChannels = 1
  const bytesPerSample = 2 // 16-bit PCM = 2 bytes
  const blockAlign = numChannels * bytesPerSample
  const byteRate = sampleRate * blockAlign
  const dataSize = float32Samples.length * bytesPerSample
  const buffer = new ArrayBuffer(44 + dataSize)
  const view = new DataView(buffer)

  /* RIFF chunk descriptor */
  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataSize, true) // file length - 8
  writeString(view, 8, 'WAVE')

  /* "fmt " sub-chunk */
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true) // Subchunk1Size for PCM (16 bytes)
  view.setUint16(20, 1, true) // AudioFormat: 1 = PCM
  view.setUint16(22, numChannels, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, byteRate, true)
  view.setUint16(32, blockAlign, true)
  view.setUint16(34, 16, true) // BitsPerSample: 16

  /* "data" sub-chunk */
  writeString(view, 36, 'data')
  view.setUint32(40, dataSize, true)

  // Ghi mẫu âm thanh 16-bit PCM (chuyển đổi float -1..1 sang integer -32768..32767)
  let offset = 44
  for (let i = 0; i < float32Samples.length; i++, offset += 2) {
    const s = Math.max(-1, Math.min(1, float32Samples[i]))
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true)
  }

  return new Blob([buffer], { type: 'audio/wav' })
}

function writeString(view, offset, string) {
  for (let i = 0; i < string.length; i++) {
    view.setUint8(offset + i, string.charCodeAt(i))
  }
}
