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
  if (upper.includes('UK')) return 'UK'
  if (upper.includes('US')) return 'US'
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
  if ((trimmed.startsWith('/') && trimmed.endsWith('/'))
    || (trimmed.startsWith('[') && trimmed.endsWith(']'))) return trimmed
  return `/${trimmed}/`
}

/**
 * Trích xuất danh sách phiên âm và audio từ thẻ từ vựng.
 * @param {Object} cardData
 * @returns {Array<{ text: string, audioUrl: string, locale: string }>}
 */
const extractPhonetics = (cardData) => {
  if (!cardData || !Array.isArray(cardData.phonetics)) return []
  const list = []

  cardData.phonetics.forEach((p) => {
    if (p && (p.text || p.audioUrl)) {
      list.push({
        text: p.text || '',
        audioUrl: p.audioUrl || '',
        locale: p.locale || ''
      })
    }
  })

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

  // Dọn dẹp trình phát âm thanh khi component unmount (chuyển trang)
  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current = null
      }
    }
  }, [])

  if (!card) return null

  // Trích xuất các trường dữ liệu
  const term = card.term || ''
  const pos = card.pos || ''
  const imageUrl = card.imageUrl || ''
  const translation = card.translation || ''
  const explanationVi = card.explanationVi || ''
  const explanationEn = card.explanationEn || ''
  const examplesVi = card.examplesVi || ''
  const examplesEn = card.examplesEn || ''

  const phoneticsList = extractPhonetics(card)

  const handleFlip = () => {
    setIsFlipped((prev) => !prev)
  }

  /**
   * Xử lý phát âm khi click nút loa:
   * Ngăn sự kiện nổi bọt (stopPropagation) lên parent để không kích hoạt lật thẻ flashcard khi nhấn phát âm.
   */
  const handlePlayAudio = (e, audioUrl, key = 'default') => {
    if (e && typeof e.stopPropagation === 'function') {
      e.stopPropagation()
    }

    if (!audioUrl || typeof audioUrl !== 'string' || audioUrl.trim().length === 0) {
      return
    }

    // Nếu đang phát chính audio này thì dừng lại (toggle play/pause)
    if (playingAudioKey === key) {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current.currentTime = 0
      }
      setPlayingAudioKey(null)// tránh âm thanh đã tắt nhưng icon trên màn hình vẫn hiển thị
      return
    }

    // Dừng âm thanh đang phát trước đó nếu có
    if (audioRef.current) {
      audioRef.current.pause()
      audioRef.current.currentTime = 0
    }

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
        console.warn('Không thể phát file âm thanh URL:', audioUrl)
        setPlayingAudioKey(null)
      }

      const playPromise = audioRef.current.play()
      if (playPromise !== undefined) {
        // Xử lý DOMException nếu trình duyệt chặn play hoặc file lỗi
        playPromise.catch((err) => {
          console.warn('Lỗi khi gọi play audio:', err)
          setPlayingAudioKey(null)
        })
      }
    } catch (err) {
      console.error('Lỗi khởi tạo Audio player:', err)
      setPlayingAudioKey(null)
    }
  }

  return (
    <div
      className="flashcard-wrapper"
      onClick={handleFlip}
      role="button"
      tabIndex={0}//có thể tab đến thẻ này
      aria-label={`Thẻ từ vựng: ${term}. Nhấn phím cách hoặc click để lật thẻ.`}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {//enter or spacebar
          e.preventDefault()
          handleFlip()
        }
      }}
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
                onError={() => setImgError(true)}//Ko có, lỗi ảnh -> hiện ảnh broken, xấu giao diện
              />
            </div>
          )}

          {/* Từ vựng chính */}
          <h2 className="flashcard-term">{term}</h2>

          {/* Từ loại (Part of Speech) */}
          {pos && <span className="badge badge--primary flashcard-pos">{pos}</span>}

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
                    {item.audioUrl && (
                      <button
                        type="button"
                        className={`btn-icon flashcard-audio-btn ${isPlaying ? 'playing' : ''}`}
                        onClick={(e) => handlePlayAudio(e, item.audioUrl, key)}
                        title={`Nghe phát âm ${localeLabel ? `(${localeLabel})` : ''} của "${term}"`}
                        aria-label={`Phát âm ${localeLabel ? `(${localeLabel})` : ''} của "${term}"`}
                      >
                        <span className={`material-symbols-outlined ${isPlaying ? 'pulse-icon' : ''}`}>
                          volume_up
                        </span>
                      </button>
                    )}
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
                  <p className="flashcard-section-label">Định nghĩa (VI):</p>
                  <p className="flashcard-explanation-text">{explanationVi}</p>
                </div>
              </div>
            )}

            {explanationEn && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">menu_book</span>
                <div>
                  <p className="flashcard-section-label">Định nghĩa (EN):</p>
                  <p className="flashcard-explanation-text">{explanationEn}</p>
                </div>
              </div>
            )}

            {(examplesEn || examplesVi) && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">format_quote</span>
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
