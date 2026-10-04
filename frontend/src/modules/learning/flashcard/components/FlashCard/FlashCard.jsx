import { useState, useEffect, useRef } from 'react'
import './FlashCard.css'

/**
 * Format nhãn ngôn ngữ (locale) hiển thị ngắn gọn (ví dụ: en-US -> US, en-UK -> UK).
 * @param {string} locale
 * @returns {string}
 */
const formatLocale = (locale) => {
  if (!locale || typeof locale !== 'string') return ''
  const upper = locale.toUpperCase().trim()
  if (upper.includes('UK') || upper.includes('GB')) return 'UK'
  if (upper.includes('US')) return 'US'
  if (upper.includes('VN') || upper.includes('VI')) return 'VN'
  return locale
}

/**
 * Đảm bảo phiên âm hiển thị trong cặp dấu gạch chéo /.../
 * @param {string} text
 * @returns {string}
 */
const formatPhonetic = (text) => {
  if (!text || typeof text !== 'string') return ''
  const trimmed = text.trim()
  if (trimmed.startsWith('/') || trimmed.startsWith('[')) return trimmed
  return `/${trimmed}/`
}

/**
 * Phát âm dự phòng qua Web Speech API nếu không có file âm thanh hoặc tải file lỗi.
 * @param {string} text - Từ vựng cần phát âm
 * @param {string} locale - Locale ngôn ngữ (en-US, en-GB, ...)
 * @returns {boolean}
 */
const playSpeechFallback = (text, locale = 'en-US') => {
  if (!text || typeof window === 'undefined' || !('speechSynthesis' in window)) {
    return false
  }
  try {
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    if (locale && (locale.includes('UK') || locale.includes('GB'))) {
      utterance.lang = 'en-GB'
    } else if (locale && (locale.includes('VN') || locale.includes('VI'))) {
      utterance.lang = 'vi-VN'
    } else {
      utterance.lang = 'en-US'
    }
    utterance.rate = 0.9
    window.speechSynthesis.speak(utterance)
    return true
  } catch (err) {
    console.error('Lỗi khi phát âm qua SpeechSynthesis:', err)
    return false
  }
}

/**
 * Trích xuất danh sách phiên âm và audio từ thẻ từ vựng với đầy đủ fallback.
 * @param {Object} cardData
 * @returns {Array<{ text: string, audioUrl: string, locale: string }>}
 */
const extractPhonetics = (cardData) => {
  if (!cardData) return []
  const list = []

  // 1. Kiểm tra mảng phonetics trả về từ backend (CardResponse.phonetics)
  if (Array.isArray(cardData.phonetics) && cardData.phonetics.length > 0) {
    cardData.phonetics.forEach((p) => {
      if (p && (p.text || p.audioUrl || p.audio_url)) {
        list.push({
          text: p.text || '',
          audioUrl: p.audioUrl || p.audio_url || '',
          locale: p.locale || ''
        })
      }
    })
  }

  // 2. Fallback nếu dữ liệu đặt trực tiếp ở root object
  if (list.length === 0) {
    const fallbackText = cardData.phonetic || cardData.phonetic_text || ''
    const fallbackAudio = cardData.audioUrl || cardData.audio_url || ''
    if (fallbackText || fallbackAudio) {
      list.push({
        text: fallbackText,
        audioUrl: fallbackAudio,
        locale: ''
      })
    }
  }

  // 3. Nếu chưa có phiên âm nhưng có từ vựng, tạo mục mặc định để hỗ trợ nút nghe audio qua Web Speech
  if (list.length === 0 && cardData.term) {
    list.push({
      text: '',
      audioUrl: '',
      locale: 'en-US'
    })
  }

  return list
}

/**
 * FlashCard component — Thẻ flashcard 3D hỗ trợ lật mặt, hiển thị ảnh, phiên âm và nút nghe phát âm.
 *
 * @param {Object} card - Dữ liệu thẻ từ vựng (CardResponse)
 */
export default function FlashCard({ card }) {
  const [isFlipped, setIsFlipped] = useState(false)
  const [imgError, setImgError] = useState(false)
  const [playingAudioKey, setPlayingAudioKey] = useState(null)
  const audioRef = useRef(null)

  // Dọn dẹp trình phát âm thanh khi component unmount
  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current = null
      }
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
    }
  }, [])

  if (!card) return null

  // Trích xuất an toàn các trường dữ liệu (hỗ trợ cả camelCase và snake_case)
  const term = card.term || ''
  const pos = card.pos || ''
  const imageUrl = card.imageUrl || card.image_url || ''
  const translation = card.translation || card.term || ''
  const explanationVi = card.explanationVi || card.explanation_vi || ''
  const explanationEn = card.explanationEn || card.explanation_en || ''
  const examplesVi = card.examplesVi || card.examples_vi || ''
  const examplesEn = card.examplesEn || card.examples_en || ''

  const phoneticsList = extractPhonetics(card)

  const handleFlip = () => {
    setIsFlipped((prev) => !prev)
  }

  /**
   * Xử lý phát âm khi click nút loa:
   * Ngăn sự kiện nổi bọt (stopPropagation) để không kích hoạt lật thẻ flashcard.
   */
  const handlePlayAudio = (e, audioUrl, locale = 'en-US', key = 'default') => {
    if (e && typeof e.stopPropagation === 'function') {
      e.stopPropagation()
    }

    // Nếu đang phát chính audio này thì dừng lại (toggle play/pause)
    if (playingAudioKey === key) {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current.currentTime = 0
      }
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
      setPlayingAudioKey(null)
      return
    }

    // Dừng âm thanh đang phát trước đó nếu có
    if (audioRef.current) {
      audioRef.current.pause()
      audioRef.current.currentTime = 0
    }
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }

    if (audioUrl && typeof audioUrl === 'string' && audioUrl.trim().length > 0) {
      setPlayingAudioKey(key)
      try {
        if (!audioRef.current) {
          audioRef.current = new Audio()
        }
        audioRef.current.src = audioUrl

        audioRef.current.onended = () => {
          setPlayingAudioKey(null)
        }

        audioRef.current.onerror = () => {
          console.warn('Không thể phát file âm thanh URL, chuyển sang dự phòng Web Speech API.')
          const fallbackSuccess = playSpeechFallback(term, locale)
          if (!fallbackSuccess) {
            setPlayingAudioKey(null)
          } else {
            setTimeout(() => setPlayingAudioKey(null), 1200)
          }
        }

        const playPromise = audioRef.current.play()
        if (playPromise !== undefined) {
          playPromise.catch((err) => {
            console.warn('Lỗi khi gọi play audio, chuyển sang dự phòng Web Speech API:', err)
            const fallbackSuccess = playSpeechFallback(term, locale)
            if (!fallbackSuccess) {
              setPlayingAudioKey(null)
            } else {
              setTimeout(() => setPlayingAudioKey(null), 1200)
            }
          })
        }
      } catch (err) {
        console.error('Lỗi khởi tạo Audio player:', err)
        const fallbackSuccess = playSpeechFallback(term, locale)
        if (!fallbackSuccess) {
          setPlayingAudioKey(null)
        } else {
          setTimeout(() => setPlayingAudioKey(null), 1200)
        }
      }
    } else if (term) {
      // Trường hợp không có URL file âm thanh: dùng Web Speech API
      setPlayingAudioKey(key)
      const fallbackSuccess = playSpeechFallback(term, locale)
      if (!fallbackSuccess) {
        setPlayingAudioKey(null)
      } else {
        setTimeout(() => setPlayingAudioKey(null), 1200)
      }
    }
  }

  return (
    <div
      className="flashcard-wrapper"
      onClick={handleFlip}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          handleFlip()
        }
      }}
      aria-label={`Thẻ từ vựng: ${term}. Nhấn phím cách hoặc click để lật thẻ.`}
    >
      <div className={`flashcard-inner ${isFlipped ? 'flipped' : ''}`}>
        {/* ── MẶT TRƯỚC: Ảnh minh họa, Từ vựng, Phiên âm, Audio ── */}
        <div className="flashcard-front glass-card">
          {/* Ảnh minh họa (chỉ hiển thị khi có URL hợp lệ và tải thành công) */}
          {imageUrl && !imgError && (
            <div className="flashcard-image-wrapper">
              <img
                src={imageUrl}
                alt={term || 'Minh họa từ vựng'}
                className="flashcard-image"
                onError={() => setImgError(true)}
                loading="lazy"
              />
            </div>
          )}

          {/* Từ vựng chính */}
          <h2 className="flashcard-term">{term}</h2>

          {/* Từ loại (Part of Speech) */}
          {pos && <span className="flashcard-pos badge-info">{pos}</span>}

          {/* Nhóm phiên âm & Nút Audio phát âm */}
          {phoneticsList.length > 0 && (
            <div className="flashcard-phonetics-group">
              {phoneticsList.map((item, index) => {
                const key = `phonetic-${index}`
                const isPlaying = playingAudioKey === key
                const localeLabel = formatLocale(item.locale)
                const formattedPhonetic = formatPhonetic(item.text)

                return (
                  <div key={key} className="flashcard-phonetic-item">
                    {localeLabel && (
                      <span className="flashcard-phonetic-locale">{localeLabel}</span>
                    )}
                    {formattedPhonetic && (
                      <span className="flashcard-phonetic-text">{formattedPhonetic}</span>
                    )}
                    <button
                      type="button"
                      className={`btn-icon flashcard-audio-btn ${isPlaying ? 'playing' : ''}`}
                      onClick={(e) => handlePlayAudio(e, item.audioUrl, item.locale, key)}
                      title={`Nghe phát âm ${localeLabel ? `(${localeLabel})` : ''} của "${term}"`}
                      aria-label={`Phát âm ${localeLabel ? `(${localeLabel})` : ''} của "${term}"`}
                    >
                      <span className={`material-symbols-outlined ${isPlaying ? 'pulse-icon' : ''}`}>
                        volume_up
                      </span>
                    </button>
                  </div>
                )
              })}
            </div>
          )}

          <p className="flashcard-hint">
            <span className="material-symbols-outlined flashcard-hint-icon">touch_app</span>
            Chạm để xem nghĩa & ví dụ
          </p>
        </div>

        {/* ── MẶT SAU: Dịch nghĩa, Giải thích, Ví dụ ── */}
        <div className="flashcard-back glass-card">
          <h2 className="flashcard-term">{translation}</h2>

          <div className="flashcard-details">
            {explanationVi && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">menu_book</span>
                <div>
                  <p className="flashcard-section-label">Giải thích (VI):</p>
                  <p>{explanationVi}</p>
                </div>
              </div>
            )}

            {explanationEn && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">menu_book</span>
                <div>
                  <p className="flashcard-section-label">Định nghĩa (EN):</p>
                  <p>{explanationEn}</p>
                </div>
              </div>
            )}

            {(examplesEn || examplesVi) && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">record_voice_over</span>
                <div>
                  <p className="flashcard-section-label">Ví dụ:</p>
                  {examplesEn && <p className="flashcard-example-en">"{examplesEn}"</p>}
                  {examplesVi && <p className="flashcard-example-vi">{examplesVi}</p>}
                </div>
              </div>
            )}
          </div>

          <p className="flashcard-hint">
            <span className="material-symbols-outlined flashcard-hint-icon">touch_app</span>
            Chạm để lật lại mặt trước
          </p>
        </div>
      </div>
    </div>
  )
}
