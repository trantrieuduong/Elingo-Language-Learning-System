import ShadowingResult from "./ShadowingResult";

const getStatusIcon = (statusType) =>
  ({
    error: "error",
    warning: "warning",
    recording: "graphic_eq",
    success: "check_circle",
  }[statusType] || "hourglass_top");

function ShadowingPracticeCard(props) {
  const {
    attemptCount,
    currentIndex,
    currentSegment,
    downloadProgress,
    handleNextSegment,
    handleToggleRecording,
    modelLoadedBefore,
    recordingSeconds,
    segmentResult,
    statusMessage,
    statusType,
    stepState,
    t,
    whisperStatus,
  } = props;
  const isRecording = stepState === "recording";
  const canRecord = stepState === "idle" || isRecording;
  const canAdvance =
    stepState === "idle" &&
    (currentSegment?.userProgress?.shadowingAttemptCount || segmentResult);
  return (
    <div className="shadowing-study-card glass-card">
      <div className="shadowing-study-header">
        <h3 className="shadowing-study-title">{t("shadowing.speakPrompt")}</h3>
        <span className="shadowing-attempts-info">
          {t("shadowing.attemptsCount", { count: attemptCount })}
        </span>
      </div>

      {/* Hộp thoại câu tiếng Anh cần đọc theo */}
      <div className="shadowing-dialogue-box">
        <p className="shadowing-dialogue-text">
          {currentSegment?.transcriptOriginal ||
            currentSegment?.transcriptNormalized}
        </p>
      </div>

      {/* Hàng 3 nút điều khiển (theo MinLish) */}
      <div className="shadowing-controls-row">
        <button
          type="button"
          className={`btn-primary shadowing-record-btn ${
            isRecording ? "shadowing-record-btn--recording" : ""
          }`}
          onClick={handleToggleRecording}
          disabled={!canRecord}
        >
          <span className="material-symbols-outlined">
            {isRecording ? "stop" : "mic"}
          </span>
          <span>
            {isRecording
              ? `${t("shadowing.recordingBtn")} (${recordingSeconds}s)`
              : t("shadowing.recordBtn")}
          </span>
        </button>
        {/* Nút Tiếp tục: chỉ bật khi segment này đã có lần thử > 0 và đang không ghi */}
        <button
          type="button"
          className="btn-secondary"
          onClick={handleNextSegment}
          disabled={!canAdvance}
        >
          <span>
            {currentIndex < props.segments.length - 1
              ? t("lessonStudy.nextSegment")
              : t("lessonStudy.finishLesson")}
          </span>
          <span className="material-symbols-outlined">arrow_forward</span>
        </button>
      </div>

      {/* Dòng trạng thái (Status message / tải mô hình / lỗi) */}
      {whisperStatus === "downloading" && !modelLoadedBefore && (
        <div className="shadowing-status-banner shadowing-status-banner--processing">
          <span className="material-symbols-outlined spinning">sync</span>
          <span>
            {t("shadowing.downloadProgress", { progress: downloadProgress })}
          </span>
        </div>
      )}
      {statusMessage && (
        <div
          className={`shadowing-status-banner shadowing-status-banner--${statusType}`}
        >
          <span className="material-symbols-outlined">
            {getStatusIcon(statusType)}
          </span>
          <span>{statusMessage}</span>
        </div>
      )}

      {/* ── KHỐI KẾT QUẢ ĐÁNH GIÁ (KHI VỪA NỘP HOẶC MỞ LẠI SEGMENT ĐÃ LÀM) ── */}
      {segmentResult && <ShadowingResult {...props} />}
    </div>
  );
}

export default ShadowingPracticeCard;
