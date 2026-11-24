import { AudioRecorder } from './audioRecorder';
import { encodeWav } from './wav';
import { scoreShadowing } from './shadowingScore';
import { patchShadowingProgressApi } from '../shadowingApi';
import { uploadFileForVerification } from '../../../../services/uploadFile';

// Bản ghi không hợp lệ nếu thời lượng dưới 0.5 giây hoặc không có dữ liệu âm thanh
const isInvalidRecording = ({ audio16k, durationSec }) => durationSec < 0.5 || audio16k.length === 0;

const getStartErrorMessage = (error, t) => {
  if (error.code === 'PERMISSION_DENIED') return t('shadowing.micPermissionDenied');
  if (error.code === 'NOT_SUPPORTED') return t('shadowing.micNotSupported');
  return error.message || t('shadowing.cannotStartRecording');
};

const shouldDiscardFileKey = (error) => {
  const code = error?.response?.data?.code;
  return code === 'FILE_NOT_FOUND' || code === 'FILE_ACCESS_DENIED';
};

/**
 * Quản lý toàn bộ quy trình ghi âm Shadowing, từ khởi tạo model,
 * ghi âm, nhận diện giọng nói đến chấm điểm và lưu kết quả lên server
 */
export const createRecordingHandler = ({
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
}) => {
  // Đưa quy trình về trạng thái chờ và cập nhật thông báo tương ứng
  const setIdleStatus = (type, message) => {
    setStepState('idle');
    setStatusType(type);
    setStatusMessage(message);
  };

  const clearTimer = () => {
    if (!recordingTimerRef.current) return;
    clearInterval(recordingTimerRef.current);
    recordingTimerRef.current = null;
  };

  // Cập nhật kết quả sau khi server xác nhận lưu tiến độ Shadowing thành công
  const saveResult = (resultData, rawScore, wordsAccuracy, fileKey) => {
    const bestScore = resultData.segmentProgress?.shadowingBestScore ?? resultData.score;
    setSavedFileKey(null);
    setStepState('idle');
    setStatusType('');
    setStatusMessage('');

    // Lưu kết quả lượt luyện tập hiện tại để hiển thị cho người học
    setSegmentResult({
      score: resultData.score,
      bestScore,
      rawScore,
      penalty: Math.max(0, (attemptCount - 1) * 10),
      attemptCount,
      passed: resultData.passed,
      wordsAccuracy,
      isHistorical: false,
      segmentProgress: resultData.segmentProgress,
    });

    // Đồng bộ tiến độ của đoạn hiện tại trong danh sách các đoạn bài học
    setSegments((previous) =>
      previous.map((segment, index) =>
        index === currentIndex
          ? {
              ...segment,
              userProgress: {
                ...(segment.userProgress || {}),
                shadowingAttemptCount: attemptCount,
                shadowingBestScore: bestScore,
                wordsAccuracy,
                shadowingLatestAudioUrl:
                  resultData.segmentProgress?.shadowingLatestAudioUrl || fileKey,
              },
            }
          : segment
      )
    );
    setAttemptCount((previous) => Math.min(1000, previous + 1));
  };

  const submitScore = async (wavBlob, rawScore, wordsAccuracy) => {
    let fileKey = savedFileKey;
    setStepState('uploading');
    setStatusType('processing');
    setStatusMessage(t('shadowing.statusUploading'));

    // Chỉ upload khi chưa có fileKey hợp lệ, tránh upload lại file khi gửi API thất bại
    if (!fileKey) {
      fileKey = await uploadFileForVerification(wavBlob, 'audio/wav');
      setSavedFileKey(fileKey);
    }
    if (!lesson?.id) return setIdleStatus('error', t('shadowing.submitError'));

    setStepState('submitting');
    setStatusMessage(t('shadowing.statusSubmitting'));

    // Gửi điểm số, độ chính xác từ vựng và fileKey âm thanh lên server
    const response = await patchShadowingProgressApi(lesson.id, currentSegment.id, {
      attemptCount: Math.min(1000, attemptCount),
      score: rawScore,
      wordsAccuracy,
      audioUrl: fileKey,
    });
    saveResult(response?.data || response, rawScore, wordsAccuracy, fileKey);
  };

  // Kết thúc ghi âm, xử lý dữ liệu âm thanh và thực hiện chấm điểm
  const stopRecording = async () => {
    clearTimer();
    setStepState('scoring');
    setStatusType('processing');
    setStatusMessage(t('shadowing.statusScoring'));
    try {
      if (!recorderRef.current) throw new Error(t('shadowing.recorderNotFound'));

      // Dừng microphone và lấy dữ liệu âm thanh đã được chuẩn hóa
      const recording = await recorderRef.current.stop();
      recorderRef.current = null;

      // Không tiếp tục xử lý nếu người dùng ghi âm quá ngắn hoặc không có dữ liệu
      if (isInvalidRecording(recording)) return setIdleStatus('warning', t('shadowing.tooShort'));

      // Mã hóa dữ liệu âm thanh thành file WAV để phát lại và upload lên server
      const wavBlob = encodeWav(recording.audio16k, 16000);
      setLocalAudioUrl(URL.createObjectURL(wavBlob));

      let recognizedText;

      try {
        // Sử dụng Whisper để nhận diện nội dung người học vừa phát âm
        recognizedText = await transcribe(recording.audio16k);
      } catch (error) {
        return setIdleStatus('error', error.message || t('shadowing.recognizeError'));
      }
      if (!recognizedText?.trim()) return setIdleStatus('warning', t('shadowing.tooShort'));

      // So sánh nội dung nhận diện với transcript chuẩn hóa của đoạn hiện tại
      const { score, wordsAccuracy } = scoreShadowing(
        currentSegment?.transcriptNormalized || '',
        recognizedText
      );

      // Gửi kết quả chấm điểm và file ghi âm lên server
      await submitScore(wavBlob, score, wordsAccuracy);
    } catch (error) {
      if (shouldDiscardFileKey(error)) setSavedFileKey(null);
      setIdleStatus(
        'error',
        error?.response?.data?.message || error.message || t('shadowing.submitError')
      );
    }
  };

  // Hiển thị trạng thái chuẩn bị model khi model chưa từng được tải trước đó
  const showModelPreparing = () => {
    if (!modelLoadedBefore) {
      setStatusType('processing');
      setStatusMessage(t('shadowing.modelPreparing'));
    }
  };

  // Model được tự tải khi vào trang; nếu chưa sẵn sàng thì thử tải lại khi người dùng bấm ghi âm
  const prepareModel = () => {
    loadModel(modelConfig);
    showModelPreparing();
  };

  // Khởi tạo microphone và bắt đầu ghi âm cho đoạn hội thoại hiện tại
  const startRecording = async () => {
    setStatusMessage('');
    setStatusType('');
    try {
      // Tính thời lượng đoạn âm thanh gốc để giới hạn thời gian ghi âm
      const duration = ((currentSegment?.endMs || 0) - (currentSegment?.startMs || 0)) / 1000;
      const recorder = new AudioRecorder({
        maxDurationSec: Math.max(10, Math.min(60, Math.round(duration * 3) || 30)),
        onAutoStop: handleAutoStopRecording,
      });

      // Chỉ cập nhật trạng thái ghi âm sau khi microphone khởi động thành công
      await recorder.start();
      recorderRef.current = recorder;

      setStepState('recording');
      setStatusType('recording');
      setStatusMessage(t('shadowing.statusRecording'));
      setRecordingSeconds(0);

      // Cập nhật thời gian ghi âm mỗi giây để hiển thị trên giao diện
      recordingTimerRef.current = setInterval(
        () => setRecordingSeconds((seconds) => seconds + 1),
        1000
      );
    } catch (error) {
      setIdleStatus('error', getStartErrorMessage(error, t));
    }
  };

  return async () => {
    // Không cho phép thực hiện thao tác mới khi đang ở các bước xử lý khác
    if (stepState !== 'idle' && stepState !== 'recording') return;

    // Nếu đang ghi âm, thao tác tiếp theo sẽ kết thúc ghi âm và chấm điểm
    if (stepState === 'recording') return stopRecording();

    // Nếu Whisper đang tải model, chỉ hiển thị thông báo chờ
    if (whisperStatus === 'downloading') return showModelPreparing();

    // Nếu model chưa sẵn sàng, tiến hành kiểm tra điều kiện và tải model
    if (whisperStatus !== 'ready') return prepareModel();
    
    return startRecording();
  };
};
