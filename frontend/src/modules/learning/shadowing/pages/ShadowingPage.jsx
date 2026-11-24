import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { getLessonDetailApi, getLessonSegmentsApi } from '../../lesson/lessonApi';
import { getYouTubeVideoId } from '../../lesson/utils/youtube';
import { splitWords } from '../../lesson/utils/text';
import { createRecordingHandler } from '../utils/recordingLogic';
import { useWhisper } from '../whisper/useWhisper';
import { pickModelConfig, isMobileDevice } from '../whisper/modelConfig';
import ShadowingPageContent from '../components/ShadowingPageContent';
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

  // Tự nhận diện thiết bị để chọn model: mobile -> tiny (light), PC -> base (standard)
  // Chỉ tính một lần khi mount để cấu hình ổn định suốt phiên
  const modelConfig = useMemo(() => pickModelConfig({ isMobile: isMobileDevice() }), []);

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

  // 1. Tự động tải model phù hợp với thiết bị ngay khi vào trang (lần sau lấy từ cache trình duyệt)
  useEffect(() => {
    loadModel(modelConfig);
  }, [loadModel, modelConfig]);

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
  const handleToggleRecording = createRecordingHandler({
    attemptCount,
    currentIndex,
    currentSegment,
    handleAutoStopRecording,
    lesson,
    loadModel,
    modelConfig,
    modelLoadedBefore,
    recorderRef,
    recordingTimerRef,
    savedFileKey,
    setAttemptCount,
    setLocalAudioUrl,
    setRecordingSeconds,
    setSavedFileKey,
    setSegmentResult,
    setSegments,
    setStatusMessage,
    setStatusType,
    setStepState,
    stepState,
    t,
    transcribe,
    whisperStatus,
  });

  useEffect(() => {
    toggleRecordingRef.current = handleToggleRecording;
  });

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
  const displayWordsList =
    origWords.length === (segmentResult?.wordsAccuracy?.length || 0) ? origWords : refWords;

  const passedSegmentsCount = segments.filter((segment) => {
    const bestScore = segment.userProgress?.shadowingBestScore;
    return typeof bestScore === 'number' && bestScore >= passScore;
  }).length;

  return (
    <ShadowingPageContent
      attemptCount={attemptCount}
      currentIndex={currentIndex}
      currentSegment={currentSegment}
      displayWordsList={displayWordsList}
      downloadProgress={downloadProgress}
      errorMessage={errorMessage}
      errorType={errorType}
      handleNextSegment={handleNextSegment}
      handleReplay={handleReplay}
      handleSelectSegment={handleSelectSegment}
      handleToggleRecording={handleToggleRecording}
      isCompletedLesson={isCompletedLesson}
      lesson={lesson}
      loading={loading}
      localAudioUrl={localAudioUrl}
      modelLoadedBefore={modelLoadedBefore}
      onNavigate={onNavigate}
      passScore={passScore}
      passedSegmentsCount={passedSegmentsCount}
      playerRef={playerRef}
      recordingSeconds={recordingSeconds}
      segmentResult={segmentResult}
      segments={segments}
      slug={slug}
      statusMessage={statusMessage}
      statusType={statusType}
      stepState={stepState}
      t={t}
      videoId={videoId}
      whisperStatus={whisperStatus}
    />
  );
}

export default ShadowingPage;
