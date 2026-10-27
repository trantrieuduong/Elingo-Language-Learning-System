import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { getLessonDetailApi, getLessonSegmentsApi } from '../../lesson/lessonApi';
import { patchShadowingProgressApi } from '../shadowingApi';
import { uploadFileForVerification } from '../../../../services/uploadFile';
import { getYouTubeVideoId } from '../../lesson/utils/youtube';
import { splitWords } from '../../lesson/utils/text';
import { AudioRecorder } from '../utils/audioRecorder';
import { encodeWav } from '../utils/wav';
import { scoreShadowing } from '../utils/shadowingScore';
import { useWhisper } from '../whisper/useWhisper';
import { pickModelConfig, isMobileDevice } from '../whisper/modelConfig';
import LessonYoutubePlayer from '../../lesson/components/LessonYoutubePlayer/LessonYoutubePlayer';
import StudySegmentSidebar from '../../lesson/components/StudySegmentSidebar/StudySegmentSidebar';
import BackButton from '../../../../components/BackButton/BackButton';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import './ShadowingPage.css';

function ShadowingPage({ slug, onNavigate }) {
  const { t } = useTranslation(['learning', 'api', 'common']);

  // Dữ liệu bài học và phân đoạn
  const [lesson, setLesson] = useState(null);
  const [segments, setSegments] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);

  // Trạng thái tải bài học
  const [loading, setLoading] = useState(true);
  const [errorType, setErrorType] = useState(null); // null | 'not_found' | 'premium' | 'general'
  const [errorMessage, setErrorMessage] = useState('');

  // Trạng thái máy của một lượt: 'idle' | 'recording' | 'scoring' | 'uploading' | 'submitting'
  const [stepState, setStepState] = useState('idle');
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [statusMessage, setStatusMessage] = useState('');
  const [statusType, setStatusType] = useState(''); // 'recording' | 'processing' | 'warning' | 'error' | 'success'

  // Số lần thử của segment hiện tại
  const [attemptCount, setAttemptCount] = useState(1);

  // Kết quả sau khi nộp (hoặc kết quả BE khi mở lại segment đã làm)
  const [segmentResult, setSegmentResult] = useState(null);
  const [localAudioUrl, setLocalAudioUrl] = useState(null);
  const [savedFileKey, setSavedFileKey] = useState(null); // Lưu verifiedFileKey để gửi lại nếu PATCH lỗi

  // Mô hình AI Whisper & Web Worker
  const {
    status: whisperStatus,
    downloadProgress,
    error: whisperError,
    loadModel,
    transcribe,
  } = useWhisper();

  const [showModelModal, setShowModelModal] = useState(false);
  const [chosenModelType, setChosenModelType] = useState('standard');

  // Cờ kiểm tra mô hình đã từng được nạp/tải hoàn tất trong các phiên trước hay chưa
  const [modelLoadedBefore] = useState(() => {
    try {
      return !!localStorage.getItem('el_whisper_loaded');
    } catch {
      return false;
    }
  });

  // Màn hình hoàn thành
  const [isCompletedLesson, setIsCompletedLesson] = useState(false);

  // Refs
  const playerRef = useRef(null);
  const recorderRef = useRef(null);
  const recordingTimerRef = useRef(null);
  const segmentsRef = useRef([]);

  useEffect(() => {
    segmentsRef.current = segments;
  }, [segments]);

  // 1. Khởi tạo chọn loại mô hình và tự động nạp ngầm mô hình nếu người dùng đã đồng ý trước đó
  useEffect(() => {
    let savedModel = null;
    let hasAgreed = false;

    try {
      hasAgreed = !!localStorage.getItem('el_whisper_agreed');
      savedModel = localStorage.getItem('el_whisper_model');
    } catch {
      /* noop */
    }

    const mobile = isMobileDevice();
    const activeModel = savedModel || (mobile ? 'light' : 'standard');
    setChosenModelType(activeModel);

    // Nếu đã đồng ý tải trước đó, nạp ngầm vào RAM ngay khi mount (từ cache trình duyệt)
    if (hasAgreed) {
      const config = pickModelConfig({
        isMobile: mobile,
        userOverride: activeModel,
      });
      loadModel(config);
    }
  }, [loadModel]);

  // 2. Tải bài học và danh sách segment khi slug thay đổi
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' });
    let isSubscribed = true;
    setLoading(true);
    setErrorType(null);
    setErrorMessage('');
    setIsCompletedLesson(false);
    setSegmentResult(null);

    const fetchData = async () => {
      try {
        const [lessonRes, segmentsRes] = await Promise.all([
          getLessonDetailApi(slug),
          getLessonSegmentsApi(slug),
        ]);

        if (!isSubscribed) return;

        const lessonData = lessonRes?.data || lessonRes;
        const rawSegments = segmentsRes?.data || segmentsRes;
        const segmentsList = Array.isArray(rawSegments) ? rawSegments : [];

        setLesson(lessonData);
        setSegments(segmentsList);

        // Mở ở segment đầu tiên có shadowingAttemptCount bằng 0 (hoặc 0 nếu đã làm hết)
        const firstIncompleteIdx = segmentsList.findIndex((seg) => {
          const count = seg?.userProgress?.shadowingAttemptCount;
          return typeof count !== 'number' || count <= 0;
        });

        setCurrentIndex(firstIncompleteIdx !== -1 ? firstIncompleteIdx : 0);
      } catch (err) {
        if (!isSubscribed) return;
        const errorCode = err?.response?.data?.code;
        if (errorCode === 'PREMIUM_REQUIRED') {
          setErrorType('premium');
          setErrorMessage(
            t('api:error.PREMIUM_REQUIRED', {
              defaultValue: t('common:loadError'),
            })
          );
        } else if (errorCode === 'LESSON_NOT_FOUND' || err?.response?.status === 404) {
          setErrorType('not_found');
        } else {
          setErrorType('general');
          setErrorMessage(err?.response?.data?.message || t('shadowing.loadError'));
        }
      } finally {
        if (isSubscribed) setLoading(false);
      }
    };

    if (slug) {
      fetchData();
    }

    return () => {
      isSubscribed = false;
    };
  }, [slug, t]);

  const currentSegment = segments[currentIndex];
  const passScore = lesson?.shadowingPassScore ?? 60;
  const videoId = lesson ? getYouTubeVideoId(lesson.sourceUrl) : '';
  const currentSegmentId = currentSegment?.id;

  // 3. Reset trạng thái CHỈ KHI chuyển sang segment khác (theo currentSegmentId)
  useEffect(() => {
    if (!currentSegmentId) return;

    setLocalAudioUrl((prevUrl) => {
      if (prevUrl) URL.revokeObjectURL(prevUrl);
      return null;
    });

    setSavedFileKey(null);
    setStatusMessage('');
    setStatusType('');
    setStepState('idle');
    setRecordingSeconds(0);

    if (recordingTimerRef.current) {
      clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = null;
    }
    if (recorderRef.current) {
      recorderRef.current.cleanup();
      recorderRef.current = null;
    }

    const currentSeg = segmentsRef.current.find((s) => s.id === currentSegmentId);
    if (currentSeg) {
      // Số lần thử = userProgress.shadowingAttemptCount nếu > 0, ngược lại 1
      const savedAttempts = currentSeg.userProgress?.shadowingAttemptCount;
      setAttemptCount(savedAttempts && savedAttempts > 0 ? savedAttempts : 1);

      // Nếu segment này đã làm trước đó, nạp dữ liệu kết quả từ BE
      if (currentSeg.userProgress && currentSeg.userProgress.shadowingAttemptCount > 0) {
        const bestScore = currentSeg.userProgress.shadowingBestScore ?? 0;
        const isPassed = bestScore >= passScore;
        setSegmentResult({
          score: bestScore,
          bestScore,
          passed: isPassed,
          wordsAccuracy: currentSeg.userProgress.wordsAccuracy || [],
          isHistorical: true,
          audioUrl: currentSeg.userProgress.shadowingLatestAudioUrl,
        });
      } else {
        setSegmentResult(null);
      }
    }
  }, [currentSegmentId, passScore]);

  // Dọn dẹp URL Blob và recorder khi unmount trang
  useEffect(() => {
    return () => {
      if (recorderRef.current) {
        recorderRef.current.cleanup();
      }
      if (recordingTimerRef.current) {
        clearInterval(recordingTimerRef.current);
      }
    };
  }, []);

  // Dọn dẹp localAudioUrl khi unmount trang
  useEffect(() => {
    return () => {
      if (localAudioUrl) {
        URL.revokeObjectURL(localAudioUrl);
      }
    };
  }, [localAudioUrl]);

  // Lắng nghe thay đổi trạng thái của Whisper để cập nhật thông báo
  useEffect(() => {
    if (whisperStatus === 'ready') {
      let isLoaded = false;
      try {
        isLoaded = !!localStorage.getItem('el_whisper_loaded');
      } catch {
        /* noop */
      }

      // Chỉ hiển thị "Mô hình đã sẵn sàng" ở lần tải đầu tiên
      if (!isLoaded) {
        setStatusMessage(t('shadowing.modelReady'));
        setStatusType('success');
        try {
          localStorage.setItem('el_whisper_loaded', 'true');
        } catch {
          /* noop */
        }
      }
    } else if (whisperStatus === 'error') {
      setStatusType('error');
      if (whisperError?.message?.includes('120s')) {
        setStatusMessage(t('shadowing.modelTimeout'));
      } else {
        setStatusMessage(whisperError?.message || t('shadowing.modelLoadFailed'));
      }
    }
  }, [whisperStatus, whisperError, t]);

  // Danh sách từ tham chiếu để hiển thị và so khớp
  const refWords = useMemo(() => {
    return splitWords(currentSegment?.transcriptNormalized || '');
  }, [currentSegment]);

  const origWords = useMemo(() => {
    return (currentSegment?.transcriptOriginal || '').trim().split(/\s+/).filter(Boolean);
  }, [currentSegment]);

  // Nút Nghe lại đoạn: phát lại video và tăng attemptCount +1
  const handleReplay = () => {
    if (playerRef.current && typeof playerRef.current.replay === 'function') {
      playerRef.current.replay();
      setAttemptCount((prev) => Math.min(1000, prev + 1));
    }
  };

  const toggleRecordingRef = useRef(null);

  // Tự động dừng ghi âm khi hết thời lượng tối đa
  const handleAutoStopRecording = useCallback(() => {
    if (toggleRecordingRef.current) {
      toggleRecordingRef.current();
    }
  }, []);

  // Bấm nút Ghi âm / Dừng ghi âm
  const handleToggleRecording = async () => {
    // Nếu đang trong các bước xử lý/chấm điểm, chặn bấm trùng
    if (stepState !== 'idle' && stepState !== 'recording') {
      return;
    }

    // ── ĐANG GHI ÂM -> BẤM DỪNG VÀ XỬ LÝ ──
    if (stepState === 'recording') {
      if (recordingTimerRef.current) {
        clearInterval(recordingTimerRef.current);
        recordingTimerRef.current = null;
      }

      setStepState('scoring');
      setStatusType('processing');
      setStatusMessage(t('shadowing.statusScoring'));

      try {
        if (!recorderRef.current) {
          throw new Error(t('shadowing.recorderNotFound'));
        }

        const { audio16k, durationSec } = await recorderRef.current.stop();
        recorderRef.current = null;

        // 1. Kiểm tra thời lượng quá ngắn (< 0.5s) hoặc rỗng
        if (durationSec < 0.5 || audio16k.length === 0) {
          setStepState('idle');
          setStatusType('warning');
          setStatusMessage(t('shadowing.tooShort'));
          return;
        }

        // Tạo WAV blob trước để sẵn sàng upload
        const wavBlob = encodeWav(audio16k, 16000);
        const blobUrl = URL.createObjectURL(wavBlob);
        setLocalAudioUrl(blobUrl);

        // 2. Chạy Whisper nhận dạng
        let recognizedText = '';
        try {
          recognizedText = await transcribe(audio16k);
        } catch (whisperErr) {
          // Lỗi Whisper -> báo lỗi rõ ràng
          setStepState('idle');
          setStatusType('error');
          setStatusMessage(whisperErr.message || t('shadowing.recognizeError'));
          return;
        }

        if (!recognizedText || !recognizedText.trim()) {
          setStepState('idle');
          setStatusType('warning');
          setStatusMessage(t('shadowing.tooShort'));
          return;
        }

        // 3. Tính điểm bằng hàm thuần scoreShadowing
        const { score: rawScore, wordsAccuracy } = scoreShadowing(
          currentSegment?.transcriptNormalized || '',
          recognizedText
        );

        // 4. Upload file WAV lên storage
        setStepState('uploading');
        setStatusType('processing');
        setStatusMessage(t('shadowing.statusUploading'));

        let verifiedFileKey = savedFileKey;
        if (!verifiedFileKey) {
          verifiedFileKey = await uploadFileForVerification(wavBlob, 'audio/wav');
          setSavedFileKey(verifiedFileKey);
        }

        // 5. Nộp kết quả chấm điểm lên backend
        if (!lesson?.id) {
          setStepState('idle');
          setStatusType('error');
          setStatusMessage(t('shadowing.submitError'));
          return;
        }

        setStepState('submitting');
        setStatusType('processing');
        setStatusMessage(t('shadowing.statusSubmitting'));

        const payload = {
          attemptCount: Math.min(1000, attemptCount),
          score: rawScore,
          wordsAccuracy,
          audioUrl: verifiedFileKey,
        };

        const resultRes = await patchShadowingProgressApi(lesson.id, currentSegment.id, payload);
        const resultData = resultRes?.data || resultRes;

        // Nộp thành công: xóa savedFileKey, cập nhật immutably
        setSavedFileKey(null);
        setStepState('idle');
        setStatusMessage('');
        setStatusType('');

        // Điểm trừ phạt theo số lần thử: penalty = (attemptCount - 1) * 10
        const penalty = Math.max(0, (attemptCount - 1) * 10);
        const bestScore = resultData.segmentProgress?.shadowingBestScore ?? resultData.score;

        setSegmentResult({
          score: resultData.score,
          bestScore,
          rawScore,
          penalty,
          attemptCount,
          passed: resultData.passed,
          wordsAccuracy,
          isHistorical: false,
          segmentProgress: resultData.segmentProgress,
        });

        // Cập nhật segments list immutably (dùng shadowingLatestAudioUrl từ BE, không dùng blobUrl)
        setSegments((prevList) => {
          const nextList = [...prevList];
          nextList[currentIndex] = {
            ...nextList[currentIndex],
            userProgress: {
              ...(nextList[currentIndex].userProgress || {}),
              shadowingAttemptCount: attemptCount,
              shadowingBestScore: bestScore,
              wordsAccuracy,
              shadowingLatestAudioUrl:
                resultData.segmentProgress?.shadowingLatestAudioUrl || verifiedFileKey,
            },
          };
          return nextList;
        });

        // Tăng attemptCount +1 cho lần ghi kế tiếp
        setAttemptCount((prev) => Math.min(1000, prev + 1));
      } catch (err) {
        setStepState('idle');
        setStatusType('error');
        const errCode = err?.response?.data?.code;
        const errMsg = err?.response?.data?.message || err.message || t('shadowing.submitError');

        // Nếu file key bị lỗi truy cập thì cần xóa để lần tới upload lại
        if (errCode === 'FILE_NOT_FOUND' || errCode === 'FILE_ACCESS_DENIED') {
          setSavedFileKey(null);
        }

        setStatusMessage(errMsg);
      }
      return;
    }

    // ── ĐANG RẢNH -> BẤM ĐỂ BẮT ĐẦU GHI ÂM ──
    // 1. Nếu đang trong quá trình tải model, không gọi loadModel nữa
    if (whisperStatus === 'downloading') {
      if (!modelLoadedBefore) {
        setStatusType('processing');
        setStatusMessage(t('shadowing.modelPreparing'));
      }
      return;
    }

    // 2. Kiểm tra xem mô hình Whisper đã sẵn sàng chưa, nếu chưa thì hỏi tải
    if (whisperStatus !== 'ready') {
      let hasAgreed = false;
      try {
        hasAgreed = !!localStorage.getItem('el_whisper_agreed');
        if (!hasAgreed) {
          setShowModelModal(true);
          return;
        }
      } catch {
        /* noop */
      }

      // Tự động load mô hình nếu đã đồng ý trước đó
      const config = pickModelConfig({
        isMobile: isMobileDevice(),
        userOverride: chosenModelType,
      });
      loadModel(config);

      // Nếu chưa từng nạp model trước đó thì mới hiện thông báo đang chuẩn bị
      if (!modelLoadedBefore) {
        setStatusType('processing');
        setStatusMessage(t('shadowing.modelPreparing'));
      }
      return;
    }

    // 3. Whisper đã sẵn sàng -> Xóa statusMessage cũ và bắt đầu ghi âm
    setStatusMessage('');
    setStatusType('');

    try {
      const segDurationSec = ((currentSegment?.endMs || 0) - (currentSegment?.startMs || 0)) / 1000;
      const maxDurationSec = Math.max(10, Math.min(60, Math.round(segDurationSec * 3) || 30));

      const recorder = new AudioRecorder({
        maxDurationSec,
        onAutoStop: handleAutoStopRecording,
      });

      await recorder.start();
      recorderRef.current = recorder;
      setStepState('recording');
      setStatusType('recording');
      setStatusMessage(t('shadowing.statusRecording'));
      setRecordingSeconds(0);

      recordingTimerRef.current = setInterval(() => {
        setRecordingSeconds((prev) => prev + 1);
      }, 1000);
    } catch (err) {
      setStepState('idle');
      setStatusType('error');
      if (err.code === 'PERMISSION_DENIED') {
        setStatusMessage(t('shadowing.micPermissionDenied'));
      } else if (err.code === 'NOT_SUPPORTED') {
        setStatusMessage(t('shadowing.micNotSupported'));
      } else {
        setStatusMessage(err.message || t('shadowing.cannotStartRecording'));
      }
    }
  };

  useEffect(() => {
    toggleRecordingRef.current = handleToggleRecording;
  });

  // Xác nhận tải mô hình từ modal
  const handleConfirmDownloadModel = () => {
    try {
      localStorage.setItem('el_whisper_agreed', 'true');
      localStorage.setItem('el_whisper_model', chosenModelType);
    } catch {
      /* noop */
    }
    setShowModelModal(false);

    const config = pickModelConfig({
      isMobile: isMobileDevice(),
      userOverride: chosenModelType,
    });
    loadModel(config);
    setStatusType('processing');
    setStatusMessage(t('shadowing.modelPreparing'));
  };

  // Chuyển sang đoạn tiếp theo
  const handleNextSegment = () => {
    if (currentIndex < segments.length - 1) {
      setCurrentIndex((prev) => prev + 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      setIsCompletedLesson(true);
    }
  };

  // Chọn segment từ Sidebar
  const handleSelectSegment = (index) => {
    if (stepState !== 'idle') return;
    if (index >= 0 && index < segments.length) {
      setCurrentIndex(index);
      setIsCompletedLesson(false);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  // ── RENDER CÁC TRẠNG THÁI TẢI / LỖI ──
  if (loading) {
    return (
      <div className="shadowing-page-container">
        <div className="dictation-loading glass-card">
          <span className="material-symbols-outlined spinning">sync</span>
          <p>{t('shadowing.loading')}</p>
        </div>
      </div>
    );
  }

  if (errorType || !lesson) {
    const isPremium = errorType === 'premium';
    return (
      <div className="shadowing-page-container">
        <EmptyState
          icon="error"
          isError
          title={errorType === 'not_found' ? t('shadowing.notFoundTitle') : undefined}
          description={
            isPremium
              ? errorMessage
              : errorType === 'not_found'
                ? t('shadowing.notFoundDesc')
                : errorMessage || t('shadowing.loadError')
          }
          actionLabel={t('lessonStudy.backToLessons')}
          actionVariant="primary"
          onAction={() => onNavigate && onNavigate('/lessons')}
        />
      </div>
    );
  }

  if (segments.length === 0) {
    return (
      <div className="shadowing-page-container">
        <EmptyState
          icon="layers_clear"
          description={t('lessonStudy.noSegments')}
          actionLabel={t('lessonStudy.backToLessons')}
          actionVariant="primary"
          onAction={() => onNavigate && onNavigate('/lessons')}
        />
      </div>
    );
  }

  // Tính số đoạn đã đạt yêu cầu để hiển thị thống kê (shadowingBestScore >= passScore)
  const passedSegmentsCount = segments.filter((s) => {
    const best = s.userProgress?.shadowingBestScore;
    return typeof best === 'number' && best >= passScore;
  }).length;

  // Quyết định danh sách từ hiển thị trong kết quả
  const displayWordsList =
    origWords.length === (segmentResult?.wordsAccuracy?.length || 0) ? origWords : refWords;

  return (
    <div className="shadowing-page-container">
      {/* Top Bar */}
      <div className="shadowing-top-bar">
        <BackButton
          label={t('common:back')}
          onClick={() => onNavigate && onNavigate('/lessons')}
        />
      </div>

      {/* 3-Column Layout */}
      <div className="shadowing-layout">
        {/* Cột 1: Player YouTube */}
        <div className="shadowing-col-player">
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
          <div className="shadowing-player-actions">
            <button
              type="button"
              className="btn-secondary shadowing-replay-btn"
              onClick={handleReplay}
              disabled={stepState === 'recording'}
            >
              <span className="material-symbols-outlined">replay</span>
              <span>{t('lessonStudy.replaySegment')}</span>
            </button>
          </div>
        </div>

        {/* Cột 2: Khu vực học chính hoặc Màn hình kết quả */}
        <main className="shadowing-col-main">
          {isCompletedLesson ? (
            /* ── MÀN HÌNH HOÀN THÀNH TOÀN BỘ BÀI HỌC ── */
            <div className="shadowing-completion-card glass-card">
              <div className="shadowing-completion-icon-wrapper">
                <span className="material-symbols-outlined shadowing-completion-icon">
                  verified
                </span>
              </div>
              <h2 className="shadowing-completion-title">{t('shadowing.completedTitle')}</h2>
              <p className="shadowing-completion-desc">{t('shadowing.completedSubtitle')}</p>

              <div className="dictation-completion-stats">
                <span className="dictation-completion-stats-tag">
                  {t('lessonStudy.passedSegmentsStats', {
                    passed: passedSegmentsCount,
                    total: segments.length,
                  })}
                </span>
              </div>

              <div className="shadowing-completion-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => onNavigate && onNavigate('/lessons')}
                >
                  {t('common:back')}
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() =>
                    onNavigate && onNavigate(`/lessons/dictation/${encodeURIComponent(slug)}`)
                  }
                >
                  <span className="material-symbols-outlined">edit_note</span>
                  <span>{t('shadowing.learnDictation')}</span>
                </button>
              </div>
            </div>
          ) : (
            /* ── KHU VỰC LUYỆN NÓI & KẾT QUẢ ĐOẠN ── */
            <div className="shadowing-study-card glass-card">
              <div className="shadowing-study-header">
                <h3 className="shadowing-study-title">{t('shadowing.speakPrompt')}</h3>
                <span className="shadowing-attempts-info">
                  {t('shadowing.attemptsCount', { count: attemptCount })}
                </span>
              </div>

              {/* Hộp thoại câu tiếng Anh cần đọc theo */}
              <div className="shadowing-dialogue-box">
                <p className="shadowing-dialogue-text">
                  {currentSegment?.transcriptOriginal || currentSegment?.transcriptNormalized}
                </p>
              </div>

              {/* Hàng 3 nút điều khiển (theo MinLish) */}
              <div className="shadowing-controls-row">
                <button
                  type="button"
                  className={`btn-primary shadowing-record-btn ${
                    stepState === 'recording' ? 'shadowing-record-btn--recording' : ''
                  }`}
                  onClick={handleToggleRecording}
                  disabled={stepState !== 'idle' && stepState !== 'recording'}
                >
                  <span className="material-symbols-outlined">
                    {stepState === 'recording' ? 'stop' : 'mic'}
                  </span>
                  <span>
                    {stepState === 'recording'
                      ? `${t('shadowing.recordingBtn')} (${recordingSeconds}s)`
                      : t('shadowing.recordBtn')}
                  </span>
                </button>

                {/* Nút Tiếp tục: chỉ bật khi segment này đã có lần thử > 0 và đang không ghi */}
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleNextSegment}
                  disabled={
                    stepState !== 'idle' ||
                    (!currentSegment?.userProgress?.shadowingAttemptCount && !segmentResult)
                  }
                >
                  <span>
                    {currentIndex < segments.length - 1
                      ? t('lessonStudy.nextSegment')
                      : t('lessonStudy.finishLesson')}
                  </span>
                  <span className="material-symbols-outlined">arrow_forward</span>
                </button>
              </div>

              {/* Dòng trạng thái (Status message / tải mô hình / lỗi) */}
              {whisperStatus === 'downloading' && !modelLoadedBefore && (
                <div className="shadowing-status-banner shadowing-status-banner--processing">
                  <span className="material-symbols-outlined spinning">sync</span>
                  <span>{t('shadowing.downloadProgress', { progress: downloadProgress })}</span>
                </div>
              )}

              {statusMessage && (
                <div className={`shadowing-status-banner shadowing-status-banner--${statusType}`}>
                  <span className="material-symbols-outlined">
                    {statusType === 'error'
                      ? 'error'
                      : statusType === 'warning'
                        ? 'warning'
                        : statusType === 'recording'
                          ? 'graphic_eq'
                          : statusType === 'success'
                            ? 'check_circle'
                            : 'hourglass_top'}
                  </span>
                  <span>{statusMessage}</span>
                </div>
              )}

              {/* ── KHỐI KẾT QUẢ ĐÁNH GIÁ (KHI VỪA NỘP HOẶC MỞ LẠI SEGMENT ĐÃ LÀM) ── */}
              {segmentResult && (
                <div className="shadowing-result-card glass-card">
                  <div className="shadowing-result-top">
                    {/* Vòng tròn điểm số SVG */}
                    <div className="shadowing-score-circle-wrapper">
                      <svg className="shadowing-score-svg" viewBox="0 0 100 100">
                        <circle className="shadowing-score-circle-bg" cx="50" cy="50" r="42" />
                        <circle
                          className={`shadowing-score-circle-val ${
                            segmentResult.score >= passScore
                              ? segmentResult.score >= 80
                                ? 'shadowing-score-circle-val--pass'
                                : 'shadowing-score-circle-val--warning'
                              : 'shadowing-score-circle-val--fail'
                          }`}
                          cx="50"
                          cy="50"
                          r="42"
                          strokeDasharray={264}
                          strokeDashoffset={
                            264 - (264 * Math.max(0, Math.min(100, segmentResult.score))) / 100
                          }
                        />
                      </svg>
                      <div className="shadowing-score-text-overlay">
                        <span className="shadowing-score-number">{segmentResult.score}</span>
                        <span className="shadowing-score-unit">/ 100</span>
                      </div>
                    </div>

                    {/* Thông tin nhãn kết quả */}
                    <div className="shadowing-result-meta">
                      <div className="shadowing-result-badge-row">
                        <span
                          className={`shadowing-badge ${
                            segmentResult.passed ? 'shadowing-badge--pass' : 'shadowing-badge--fail'
                          }`}
                        >
                          {segmentResult.passed
                            ? t('lessonStudy.passedBadge')
                            : t('lessonStudy.failedBadge')}
                        </span>
                      </div>

                      {segmentResult.isHistorical ? (
                        <p className="shadowing-best-score">
                          {t('shadowing.bestScoreLabel', { score: segmentResult.score })}
                        </p>
                      ) : (
                        <>
                          <p className="shadowing-best-score">
                            {t('shadowing.scoreLabel')}: {segmentResult.score}
                          </p>
                          {typeof segmentResult.bestScore === 'number' && (
                            <p className="shadowing-best-score-sub">
                              {t('shadowing.bestScoreLabel', { score: segmentResult.bestScore })}
                            </p>
                          )}
                          {segmentResult.penalty > 0 && (
                            <p className="shadowing-penalty-notice">
                              {t('shadowing.originalScoreNotice', {
                                raw: segmentResult.rawScore,
                                penalty: segmentResult.penalty,
                                attempts: segmentResult.attemptCount,
                              })}
                            </p>
                          )}
                        </>
                      )}
                    </div>
                  </div>

                  {/* Nghe lại bản ghi âm */}
                  {(localAudioUrl || segmentResult.audioUrl) && (
                    <div className="shadowing-audio-player-row">
                      <span className="material-symbols-outlined">volume_up</span>
                      <span className="shadowing-audio-label">
                        {t('shadowing.latestRecordingLabel')}
                      </span>
                      <audio
                        controls
                        src={localAudioUrl || segmentResult.audioUrl}
                        className="shadowing-audio-element"
                      />
                    </div>
                  )}

                  {/* Thẻ phân tích từng từ (tô màu theo độ chính xác) */}
                  {segmentResult.wordsAccuracy && segmentResult.wordsAccuracy.length > 0 && (
                    <div className="shadowing-words-analysis">
                      <div className="shadowing-words-title">
                        {t('shadowing.wordAccuracyTitle')}
                      </div>

                      <div className="shadowing-words-flow">
                        {segmentResult.wordsAccuracy.map((wordItem, idx) => {
                          const acc = wordItem.accuracy ?? 0;
                          const chipClass =
                            acc >= 80
                              ? 'shadowing-word-chip--good'
                              : acc >= 50
                                ? 'shadowing-word-chip--medium'
                                : 'shadowing-word-chip--poor';

                          const textToShow = displayWordsList[idx] || wordItem.word;

                          return (
                            <span
                              key={idx}
                              className={`shadowing-word-chip ${chipClass}`}
                              title={`${textToShow}: ${acc}%`}
                            >
                              {textToShow}
                            </span>
                          );
                        })}
                      </div>

                      {/* Chú giải trợ năng */}
                      <div className="shadowing-words-legend">
                        <span className="shadowing-legend-item">
                          <span className="shadowing-legend-dot shadowing-legend-dot--good"></span>
                          <span>{t('shadowing.wordAccuracyLegendGood')}</span>
                        </span>
                        <span className="shadowing-legend-item">
                          <span className="shadowing-legend-dot shadowing-legend-dot--medium"></span>
                          <span>{t('shadowing.wordAccuracyLegendMedium')}</span>
                        </span>
                        <span className="shadowing-legend-item">
                          <span className="shadowing-legend-dot shadowing-legend-dot--poor"></span>
                          <span>{t('shadowing.wordAccuracyLegendPoor')}</span>
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </main>

        {/* Cột 3: Sidebar tiến độ & danh sách segment */}
        <div className="shadowing-col-sidebar">
          <StudySegmentSidebar
            segments={segments}
            currentIndex={currentIndex}
            mode="shadowing"
            passScore={passScore}
            onSelect={handleSelectSegment}
          />
        </div>
      </div>

      {/* ── MODAL HỎI ĐỒNG Ý TẢI MÔ HÌNH AI ── */}
      {showModelModal && (
        <div className="shadowing-model-modal-overlay" onClick={() => setShowModelModal(false)}>
          <div className="shadowing-model-modal glass-capsule" onClick={(e) => e.stopPropagation()}>
            <div className="shadowing-model-modal-header">
              <div className="shadowing-model-modal-title-group">
                <h2 className="shadowing-model-modal-title">
                  {t('shadowing.askDownloadModelTitle')}
                </h2>
                <p className="shadowing-model-modal-desc">
                  {t('shadowing.askDownloadModelDesc', {
                    size: chosenModelType === 'light' ? '39 MB' : '73 MB',
                  })}
                </p>
              </div>
              <button type="button" className="btn-icon" onClick={() => setShowModelModal(false)}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="shadowing-model-options">
              <label
                className={`shadowing-model-radio-label glass-card ${
                  chosenModelType === 'standard' ? 'shadowing-model-radio-label--active' : ''
                }`}
              >
                <input
                  type="radio"
                  name="modelOption"
                  value="standard"
                  checked={chosenModelType === 'standard'}
                  onChange={() => setChosenModelType('standard')}
                />
                <span className="shadowing-model-option-text">
                  {t('shadowing.modelStandard')} (~73 MB)
                </span>
              </label>

              <label
                className={`shadowing-model-radio-label glass-card ${
                  chosenModelType === 'light' ? 'shadowing-model-radio-label--active' : ''
                }`}
              >
                <input
                  type="radio"
                  name="modelOption"
                  value="light"
                  checked={chosenModelType === 'light'}
                  onChange={() => setChosenModelType('light')}
                />
                <span className="shadowing-model-option-text">
                  {t('shadowing.modelLight')} (~39 MB)
                </span>
              </label>
            </div>

            <div className="shadowing-model-modal-actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setShowModelModal(false)}
              >
                {t('common:cancel')}
              </button>
              <button type="button" className="btn-primary" onClick={handleConfirmDownloadModel}>
                {t('shadowing.btnDownload')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ShadowingPage;
