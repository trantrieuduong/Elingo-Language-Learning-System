import LessonYoutubePlayer from "../../lesson/components/LessonYoutubePlayer/LessonYoutubePlayer";
import StudySegmentSidebar from "../../lesson/components/StudySegmentSidebar/StudySegmentSidebar";
import BackButton from "../../../../components/BackButton/BackButton";
import ShadowingCompletionCard from "./ShadowingCompletionCard";
import ShadowingPracticeCard from "./ShadowingPracticeCard";

function ShadowingLessonLayout(props) {
  const {
    currentIndex,
    currentSegment,
    handleReplay,
    handleSelectSegment,
    isCompletedLesson,
    lesson,
    onNavigate,
    passScore,
    playerRef,
    segments,
    stepState,
    t,
    videoId,
  } = props;
  return (
    <div className="shadowing-page-container">
      {/* Top Bar */}
      <div className="shadowing-top-bar">
        <BackButton
          label={t("common:back")}
          onClick={() => onNavigate && onNavigate("/lessons")}
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
              disabled={stepState === "recording"}
            >
              <span className="material-symbols-outlined">replay</span>
              <span>{t("lessonStudy.replaySegment")}</span>
            </button>
          </div>
        </div>

        {/* Cột 2: Khu vực học chính hoặc Màn hình kết quả */}
        <main className="shadowing-col-main">
          {/* ── MÀN HÌNH HOÀN THÀNH TOÀN BỘ BÀI HỌC / KHU VỰC LUYỆN NÓI & KẾT QUẢ ĐOẠN ── */}
          {isCompletedLesson ? (
            <ShadowingCompletionCard {...props} />
          ) : (
            <ShadowingPracticeCard {...props} />
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
    </div>
  );
}

export default ShadowingLessonLayout;
