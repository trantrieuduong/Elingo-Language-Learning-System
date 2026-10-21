import { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { getLessonDetailApi, getLessonSegmentsApi } from '../../lesson/lessonApi'
import { patchDictationProgressApi } from '../dictationApi'
import { getYouTubeVideoId } from '../../lesson/utils/youtube'
import { splitWords } from '../../lesson/utils/text'
import { getWordStatuses, isAllDone } from '../utils/dictation'
import LessonYoutubePlayer from '../../lesson/components/LessonYoutubePlayer/LessonYoutubePlayer'
import StudySegmentSidebar from '../../lesson/components/StudySegmentSidebar/StudySegmentSidebar'
import BackButton from '../../../../components/BackButton/BackButton'
import './DictationPage.css'

function DictationPage({ slug, onNavigate }) {
  const { t } = useTranslation(['learning', 'common'])

  // Dữ liệu bài học và danh sách phân đoạn
  const [lesson, setLesson] = useState(null)
  const [segments, setSegments] = useState([])
  const [currentIndex, setCurrentIndex] = useState(0)

  // Trạng thái tải và lỗi
  const [loading, setLoading] = useState(true)
  const [errorType, setErrorType] = useState(null) // null | 'not_found' | 'general'
  const [errorMessage, setErrorMessage] = useState('')

  // Trạng thái học tập của segment hiện tại
  const [userInput, setUserInput] = useState('')
  const [revealedIndices, setRevealedIndices] = useState(new Set())
  const [attemptCount, setAttemptCount] = useState(1)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')

  // Kết quả nộp bài của segment hiện tại (null nếu đang làm, object nếu vừa nộp thành công)
  const [segmentResult, setSegmentResult] = useState(null)

  // Trạng thái hoàn thành toàn bộ bài học
  const [isCompletedLesson, setIsCompletedLesson] = useState(false)

  // Refs
  const playerRef = useRef(null)
  const textareaRef = useRef(null)

  // 1. Tải thông tin bài học và danh sách segment khi slug thay đổi
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' })
    let isSubscribed = true
    setLoading(true)
    setErrorType(null)
    setErrorMessage('')
    setIsCompletedLesson(false)
    setSegmentResult(null)

    const fetchData = async () => {
      try {
        const [lessonRes, segmentsRes] = await Promise.all([
          getLessonDetailApi(slug),
          getLessonSegmentsApi(slug),
        ])

        if (!isSubscribed) return

        const lessonData = lessonRes?.data || lessonRes
        const rawSegments = segmentsRes?.data || segmentsRes
        const segmentsList = Array.isArray(rawSegments) ? rawSegments : []

        setLesson(lessonData)
        setSegments(segmentsList)

        // Tìm segment đầu tiên chưa hoàn thành (chưa có dictationAttemptCount > 0)
        const firstIncompleteIdx = segmentsList.findIndex((seg) => {
          const count = seg?.userProgress?.dictationAttemptCount
          return typeof count !== 'number' || count <= 0
        })

        setCurrentIndex(firstIncompleteIdx !== -1 ? firstIncompleteIdx : 0)
      } catch (err) {
        if (!isSubscribed) return
        const errorCode = err?.response?.data?.code
        if (errorCode === 'LESSON_NOT_FOUND' || err?.response?.status === 404) {
          setErrorType('not_found')
        } else {
          setErrorType('general')
          setErrorMessage(err?.response?.data?.message || t('dictation.errorLoad'))
        }
      } finally {
        if (isSubscribed) setLoading(false)
      }
    }

    if (slug) {
      fetchData()
    }

    return () => {
      isSubscribed = false
    }
  }, [slug, t])

  // 2. Reset trạng thái khi chuyển sang segment khác
  useEffect(() => {
    setUserInput('')
    setRevealedIndices(new Set())
    setAttemptCount(1)
    setSubmitError('')
    setSegmentResult(null)

    // Tự động focus vào ô textarea sau khi render
    const timer = setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus()
      }
    }, 100)

    return () => clearTimeout(timer)
  }, [currentIndex])

  const currentSegment = segments[currentIndex]
  const passScore = lesson?.dictationPassScore ?? 60
  const videoId = lesson ? getYouTubeVideoId(lesson.sourceUrl) : ''

  // Tách từ tham chiếu từ transcriptNormalized (đảm bảo đồng bộ với BE chấm điểm)
  const normalizedText = currentSegment?.transcriptNormalized || ''
  const refWords = splitWords(normalizedText)

  // Trạng thái hiển thị từng viên từ
  const wordStatuses = getWordStatuses({
    refWords,
    userInput,
    revealedIndices,
  })

  // Đã gõ/mở gợi ý đúng hết mọi từ trong segment
  const allDone = isAllDone(wordStatuses)

  // Tiết lộ một từ đơn lẻ bằng nút mắt
  const handleRevealWord = (index) => {
    if (isSubmitting || segmentResult) return
    setRevealedIndices((prev) => {
      const next = new Set(prev)
      next.add(index)
      return next
    })
  }

  // Tiết lộ tất cả các từ CHƯA đúng
  const handleShowAllRemaining = () => {
    if (isSubmitting || segmentResult) return
    setRevealedIndices((prev) => {
      const next = new Set(prev)
      wordStatuses.forEach((wordObj, idx) => {
        if (wordObj.status !== 'correct') {
          next.add(idx)
        }
      })
      return next
    })
  }

  // Nghe lại đoạn hiện tại
  const handleReplay = () => {
    if (playerRef.current && typeof playerRef.current.replay === 'function') {
      playerRef.current.replay()
    }
  }

  // Nộp bài segment hiện tại
  const handleSubmitSegment = async () => {
    if (isSubmitting || !currentSegment || !allDone) return

    setIsSubmitting(true)
    setSubmitError('')

    const hintUsedCount = revealedIndices.size

    try {
      const response = await patchDictationProgressApi(slug, currentSegment.id, {
        attemptCount,
        hintUsedCount,
      })

      const data = response?.data || response

      // Cập nhật kết quả segment vừa nộp
      setSegmentResult({
        score: typeof data.score === 'number' ? Math.round(data.score) : 0,
        passed: Boolean(data.passed),
        hintUsedCount,
        segmentProgress: data.segmentProgress,
        lessonProgress: data.lessonProgress,
      })

      // Cập nhật immutably vào danh sách segments để sidebar đồng bộ ngay lập tức
      setSegments((prevSegments) =>
        prevSegments.map((seg, idx) => {
          if (idx === currentIndex) {
            return {
              ...seg,
              userProgress: data.segmentProgress || seg.userProgress,
            }
          }
          return seg
        })
      )
    } catch (err) {
      setSubmitError(err?.response?.data?.message || t('dictation.errorLoad'))
    } finally {
      setIsSubmitting(false)
    }
  }

  // Làm lại đoạn này: tăng attemptCount lên 1, reset ô nhập và gợi ý, quay về trạng thái làm bài
  const handleRetrySegment = () => {
    setAttemptCount((prev) => prev + 1)
    setUserInput('')
    setRevealedIndices(new Set())
    setSegmentResult(null)
    setSubmitError('')
    if (playerRef.current && typeof playerRef.current.replay === 'function') {
      playerRef.current.replay()
    }
    setTimeout(() => {
      if (textareaRef.current) textareaRef.current.focus()
    }, 50)
  }

  // Chuyển sang đoạn tiếp theo hoặc hoàn thành bài
  const handleNextSegment = () => {
    if (currentIndex < segments.length - 1) {
      setCurrentIndex((prev) => prev + 1)
    } else {
      setIsCompletedLesson(true)
    }
  }

  // Chọn segment từ sidebar
  const handleSelectSegment = (index) => {
    setIsCompletedLesson(false)
    setCurrentIndex(index)
  }

  // ── Màn hình Đang tải ──
  if (loading) {
    return (
      <div className="dictation-page-container">
        <div className="dictation-status-card glass-card">
          <div className="dictation-spinner" />
          <p className="dictation-status-text">{t('dictation.loading')}</p>
        </div>
      </div>
    )
  }

  // ── Màn hình Lỗi hoặc Không tìm thấy ──
  if (errorType || !lesson) {
    return (
      <div className="dictation-page-container">
        <div className="dictation-status-card glass-card">
          <span className="material-symbols-outlined dictation-status-icon dictation-status-icon--error">
            error
          </span>
          <p className="dictation-status-text">
            {errorType === 'not_found'
              ? t('dictation.notFound')
              : errorMessage || t('dictation.errorLoad')}
          </p>
          <button
            type="button"
            className="btn-primary"
            onClick={() => onNavigate && onNavigate('/learning')}
          >
            {t('common:back')}
          </button>
        </div>
      </div>
    )
  }

  // ── Màn hình Bài học rỗng không có segment ──
  if (segments.length === 0) {
    return (
      <div className="dictation-page-container">
        <div className="dictation-status-card glass-card">
          <span className="material-symbols-outlined dictation-status-icon">
            layers_clear
          </span>
          <p className="dictation-status-text">{t('dictation.emptySegments')}</p>
          <button
            type="button"
            className="btn-primary"
            onClick={() => onNavigate && onNavigate('/learning')}
          >
            {t('common:back')}
          </button>
        </div>
      </div>
    )
  }

  // Đếm số segment đạt yêu cầu (bestScore >= passScore)
  const passedSegmentsCount = segments.filter((seg) => {
    const best = seg?.userProgress?.dictationBestScore
    return typeof best === 'number' && best >= passScore
  }).length

  return (
    <div className="dictation-page-container">
      {/* Thanh điều hướng quay lại danh sách */}
      <div className="dictation-top-bar">
        <BackButton
          label={t('common:back')}
          onClick={() => onNavigate && onNavigate('/learning')}
        />
      </div>

      {/* Layout 3 cột: Player | Khu học tập / Kết quả | Sidebar tiến độ */}
      <div className="dictation-layout">
        {/* Cột 1: Player YouTube */}
        <div className="dictation-col-player">
          <LessonYoutubePlayer
            ref={playerRef}
            videoId={videoId}
            startMs={currentSegment?.startMs || 0}
            endMs={currentSegment?.endMs || 0}
            title={lesson?.title}
            description={lesson?.description}
            thumbnailUrl={lesson?.thumbnailUrl}
            sourceUrl={lesson?.sourceUrl}
          />
          <div className="dictation-player-actions">
            <button
              type="button"
              className="btn-secondary dictation-replay-btn"
              onClick={handleReplay}
            >
              <span className="material-symbols-outlined">replay</span>
              <span>{t('lessonStudy.replaySegment')}</span>
            </button>
          </div>
        </div>

        {/* Cột 2: Khu vực học chính hoặc Màn hình kết quả */}
        <main className="dictation-col-main">
          {isCompletedLesson ? (
            /* ── MÀN HÌNH HOÀN THÀNH TOÀN BỘ BÀI HỌC ── */
            <div className="dictation-completion-card glass-card">
              <div className="dictation-completion-icon-wrapper">
                <span className="material-symbols-outlined dictation-completion-icon">
                  verified
                </span>
              </div>
              <h2 className="dictation-completion-title">
                {t('dictation.completedTitle')}
              </h2>
              <p className="dictation-completion-desc">
                {t('dictation.completedSubtitle')}
              </p>

              <div className="dictation-completion-stats">
                <span className="dictation-completion-stats-tag">
                  {t('lessonStudy.passedSegmentsStats', {
                    passed: passedSegmentsCount,
                    total: segments.length,
                  })}
                </span>
              </div>

              <div className="dictation-completion-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => onNavigate && onNavigate('/learning')}
                >
                  {t('common:back')}
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() =>
                    onNavigate && onNavigate(`/learning/shadowing/${encodeURIComponent(slug)}`)
                  }
                >
                  <span className="material-symbols-outlined">record_voice_over</span>
                  <span>{t('dictation.learnShadowing')}</span>
                </button>
              </div>
            </div>
          ) : segmentResult ? (
            /* ── MÀN HÌNH KẾT QUẢ CỦA PHÂN ĐOẠN ── */
            <div className="dictation-result-card glass-card">
              <div className="dictation-result-header">
                <span
                  className={`dictation-result-badge ${
                    segmentResult.passed
                      ? 'dictation-result-badge--pass'
                      : 'dictation-result-badge--fail'
                  }`}
                >
                  {segmentResult.passed
                    ? t('lessonStudy.passedBadge')
                    : t('lessonStudy.failedBadge')}
                </span>
                <span className="dictation-result-score">
                  {t('dictation.currentScore', { score: segmentResult.score })}
                </span>
              </div>

              <div className="dictation-result-meta">
                <p className="dictation-result-best">
                  {t('dictation.bestScore', {
                    score: Math.round(
                      segmentResult.segmentProgress?.dictationBestScore ?? segmentResult.score
                    ),
                  })}
                </p>
                {segmentResult.score <
                  (segmentResult.segmentProgress?.dictationBestScore ?? 0) && (
                  <p className="dictation-result-notice">
                    {t('dictation.bestScoreNotice')}
                  </p>
                )}
                {segmentResult.hintUsedCount > 0 && (
                  <p className="dictation-result-hint-count">
                    {t('dictation.hintsUsedInfo', {
                      count: segmentResult.hintUsedCount,
                    })}
                  </p>
                )}
              </div>

              <div className="dictation-result-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleRetrySegment}
                >
                  <span className="material-symbols-outlined">refresh</span>
                  <span>{t('dictation.retrySegmentBtn')}</span>
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  onClick={handleNextSegment}
                >
                  <span>
                    {currentIndex < segments.length - 1
                      ? t('lessonStudy.nextSegment')
                      : t('lessonStudy.finishLesson')}
                  </span>
                  <span className="material-symbols-outlined">arrow_forward</span>
                </button>
              </div>
            </div>
          ) : (
            /* ── KHU VỰC GÕ CHÍNH TẢ & GỢI Ý ── */
            <div className="dictation-study-card glass-card">
              <div className="dictation-study-header">
                <h3 className="dictation-study-title">
                  {t('dictation.typeWhatYouHear')}
                </h3>
              </div>

              {/* Textarea nhập câu */}
              <div className="dictation-textarea-wrapper">
                <textarea
                  ref={textareaRef}
                  value={userInput}
                  onChange={(e) => setUserInput(e.target.value)}
                  placeholder={t('dictation.inputPlaceholder')}
                  className="dictation-textarea"
                  rows={4}
                  spellCheck={false}
                  autoCorrect="off"
                  autoCapitalize="off"
                  autoComplete="off"
                  lang="en"
                  disabled={isSubmitting}
                />
              </div>

              {/* Lưới các viên từ so khớp */}
              <div className="dictation-words-section">
                <div className="dictation-words-title">
                  {t('dictation.vocabularyHint')}
                </div>
                <div className="dictation-words-grid">
                  {wordStatuses.map((wordObj, idx) => {
                    const isCorrect = wordObj.status === 'correct'
                    const isRevealed = wordObj.status === 'revealed'
                    const canReveal = !isCorrect && !isRevealed

                    return (
                      <div key={idx} className="dictation-word-wrapper">
                        {canReveal && (
                          <button
                            type="button"
                            className="dictation-eye-btn"
                            onClick={() => handleRevealWord(idx)}
                            aria-label={t('dictation.revealWordAria')}
                            title={t('dictation.revealWordAria')}
                          >
                            <span className="material-symbols-outlined">
                              visibility
                            </span>
                          </button>
                        )}
                        <div
                          className={`dictation-word-pill dictation-word-pill--${wordObj.status}`}
                        >
                          {wordObj.displayText}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>

              {/* Cảnh báo khi đã dùng gợi ý */}
              {revealedIndices.size > 0 && (
                <div className="dictation-hint-warning">
                  <span className="material-symbols-outlined dictation-hint-warning-icon">
                    warning
                  </span>
                  <span>{t('dictation.hintWarning')}</span>
                </div>
              )}

              {/* Hiển thị lỗi nộp bài nếu có */}
              {submitError && (
                <div className="dictation-submit-error">
                  <span className="material-symbols-outlined">error</span>
                  <span>{submitError}</span>
                </div>
              )}

              {/* Thanh thao tác: Hiện tất cả & Hoàn thành đoạn */}
              <div className="dictation-actions-row">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleShowAllRemaining}
                  disabled={isSubmitting || allDone}
                >
                  {t('dictation.showAllBtn')}
                </button>

                <button
                  type="button"
                  className={`btn-primary dictation-submit-btn ${
                    allDone ? 'dictation-submit-btn--ready' : ''
                  }`}
                  onClick={handleSubmitSegment}
                  disabled={!allDone || isSubmitting}
                >
                  <span>
                    {isSubmitting
                      ? t('dictation.submitting')
                      : t('dictation.completeSegmentBtn')}
                  </span>
                  <span className="material-symbols-outlined">check</span>
                </button>
              </div>
            </div>
          )}
        </main>

        {/* Cột 3: Sidebar tiến độ & danh sách segment (dùng chung cho cả Shadowing) */}
        <div className="dictation-col-sidebar">
          <StudySegmentSidebar
            segments={segments}
            currentIndex={currentIndex}
            mode="dictation"
            passScore={passScore}
            onSelect={handleSelectSegment}
          />
        </div>
      </div>
    </div>
  )
}

export default DictationPage
