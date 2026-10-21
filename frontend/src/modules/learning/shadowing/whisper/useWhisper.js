import { useState, useEffect, useRef, useCallback } from 'react'

/**
 * Hook quản lý Web Worker tải và thực thi Whisper AI.
 * Trạng thái: 'idle' | 'downloading' | 'ready' | 'transcribing' | 'error'
 */
export function useWhisper() {
  const [status, setStatus] = useState('idle')
  const [downloadProgress, setDownloadProgress] = useState(0)
  const [downloadFile, setDownloadFile] = useState('')
  const [error, setError] = useState(null)

  const workerRef = useRef(null)
  const pendingTranscribeResolve = useRef(null)
  const pendingTranscribeReject = useRef(null)

  // Khởi tạo Worker một lần duy nhất
  useEffect(() => {
    try {
      const worker = new Worker(new URL('./whisper.worker.js', import.meta.url), {
        type: 'module',
      })

      worker.onmessage = (e) => {
        const { type, payload } = e.data || {}

        if (type === 'loading') {
          setStatus('downloading')
          setDownloadProgress(payload.progress || 0)
          setDownloadFile(payload.file || '')
        } else if (type === 'ready') {
          setStatus('ready')
          setDownloadProgress(100)
          setError(null)
        } else if (type === 'transcribed') {
          setStatus('ready')
          if (pendingTranscribeResolve.current) {
            pendingTranscribeResolve.current(payload.text || '')
            pendingTranscribeResolve.current = null
            pendingTranscribeReject.current = null
          }
        } else if (type === 'error') {
          setStatus('error')
          setError(payload)
          if (pendingTranscribeReject.current) {
            pendingTranscribeReject.current(new Error(payload.message || 'Whisper Error'))
            pendingTranscribeResolve.current = null
            pendingTranscribeReject.current = null
          }
        }
      }

      worker.onerror = (err) => {
        setStatus('error')
        setError({ code: 'WORKER_ERROR', message: err.message || 'Worker gặp sự cố' })
      }

      workerRef.current = worker
    } catch (err) {
      setStatus('error')
      setError({ code: 'INIT_FAILED', message: err.message || 'Không thể tạo Web Worker' })
    }

    return () => {
      if (workerRef.current) {
        workerRef.current.terminate()
        workerRef.current = null
      }
    }
  }, [])

  /**
   * Bắt đầu tải mô hình vào Worker.
   */
  const loadModel = useCallback(({ modelId, device, dtype }) => {
    if (!workerRef.current) return
    setStatus('downloading')
    setError(null)
    setDownloadProgress(0)
    workerRef.current.postMessage({
      type: 'load',
      payload: { modelId, device, dtype },
    })
  }, [])

  /**
   * Chạy nhận dạng âm thanh Float32Array 16kHz
   */
  const transcribe = useCallback((audioFloat32) => {
    return new Promise((resolve, reject) => {
      if (!workerRef.current) {
        reject(new Error('Whisper Worker chưa được khởi tạo'))
        return
      }

      setStatus('transcribing')
      pendingTranscribeResolve.current = resolve
      pendingTranscribeReject.current = reject

      workerRef.current.postMessage({
        type: 'transcribe',
        payload: { audio: audioFloat32 },
      })
    })
  }, [])

  return {
    status,
    downloadProgress,
    downloadFile,
    error,
    loadModel,
    transcribe,
  }
}
