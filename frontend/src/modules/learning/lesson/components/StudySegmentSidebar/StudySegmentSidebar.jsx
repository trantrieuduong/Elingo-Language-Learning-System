import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import './StudySegmentSidebar.css';

function StudySegmentSidebar({
  segments = [],
  currentIndex = 0,
  mode = 'dictation',
  passScore = 60,
  onSelect,
}) {
  const { t } = useTranslation(['learning']);
  const activeItemRef = useRef(null);

  // Tự động cuộn segment đang học vào vùng nhìn thấy trong danh sách
  useEffect(() => {
    if (activeItemRef.current && typeof activeItemRef.current.scrollIntoView === 'function') {
      activeItemRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
      });
    }
  }, [currentIndex]);

  if (!segments || segments.length === 0) return null;

  // Tên trường trong userProgress theo mode:
  // Dictation: dictationAttemptCount, dictationBestScore
  // Shadowing: shadowingAttemptCount, shadowingBestScore
  const attemptField = mode === 'shadowing' ? 'shadowingAttemptCount' : 'dictationAttemptCount';
  const bestScoreField = mode === 'shadowing' ? 'shadowingBestScore' : 'dictationBestScore';

  // Kiểm tra segment đã hoàn thành (đã làm ít nhất 1 lần nộp)
  const isSegmentAttempted = (seg) => {
    const count = seg?.userProgress?.[attemptField];
    return typeof count === 'number' && count > 0;
  };

  // Số lượng segment đã làm và tỷ lệ phần trăm tiến độ
  const completedSegmentsCount = segments.filter(isSegmentAttempted).length;
  const progressPct = Math.round((completedSegmentsCount / segments.length) * 100);

  // Khóa tuần tự: người dùng được chọn các segment từ đầu tới hết segment đầu tiên chưa làm.
  // Các segment phía sau segment chưa làm đầu tiên sẽ bị khóa (disabled).
  // Nếu đã làm hết mọi segment (firstIncompleteIdx === -1), tất cả đều mở khóa.
  const firstIncompleteIdx = segments.findIndex((seg) => !isSegmentAttempted(seg));
  const maxSelectableIdx = firstIncompleteIdx === -1 ? segments.length - 1 : firstIncompleteIdx;

  const handleSelect = (idx) => {
    if (idx <= maxSelectableIdx && typeof onSelect === 'function') {
      onSelect(idx);
    }
  };

  // Helper tính màu điểm theo ngưỡng passScore
  const getScoreBadgeClass = (score) => {
    if (score < passScore) return 'study-segment-sidebar__score--fail';
    if (score < 80) return 'study-segment-sidebar__score--warning';
    return 'study-segment-sidebar__score--pass';
  };

  return (
    <aside className="study-segment-sidebar glass-card">
      {/* Header thanh tiến độ */}
      <div className="study-segment-sidebar__progress-header">
        <span className="study-segment-sidebar__progress-title">
          {mode === 'shadowing'
            ? t('studySidebar.shadowingProgress')
            : t('studySidebar.dictationProgress')}
        </span>
        <span className="study-segment-sidebar__progress-pct">{progressPct}%</span>
      </div>

      <div className="study-segment-sidebar__progress-track">
        <div
          className="study-segment-sidebar__progress-fill"
          style={{ width: `${progressPct}%` }}
        />
      </div>

      {/* Danh sách cuộn nội bộ các segment */}
      <div className="study-segment-sidebar__list">
        {segments.map((seg, idx) => {
          const isSelected = idx === currentIndex;
          const isAttempted = isSegmentAttempted(seg);
          const isLocked = idx > maxSelectableIdx;
          const score = seg?.userProgress?.[bestScoreField];
          const orderText = String(idx + 1).padStart(2, '0');

          let stateClass = '';
          if (isSelected) stateClass = 'study-segment-sidebar__item--selected';
          else if (isAttempted) stateClass = 'study-segment-sidebar__item--completed';
          else if (isLocked) stateClass = 'study-segment-sidebar__item--locked';

          return (
            <button
              key={seg.id || idx}
              ref={isSelected ? activeItemRef : null}
              type="button"
              className={`study-segment-sidebar__item ${stateClass}`}
              onClick={() => !isLocked && handleSelect(idx)}
              disabled={isLocked}
            >
              <div className="study-segment-sidebar__item-header">
                <span className="study-segment-sidebar__order">{orderText}</span>

                {/* Huy hiệu điểm hoặc Icon trạng thái */}
                {isAttempted && typeof score === 'number' ? (
                  <span
                    className={`study-segment-sidebar__score ${getScoreBadgeClass(score)}`}
                    title={t('studySidebar.bestScore', { score: Math.round(score) })}
                  >
                    {Math.round(score)}
                  </span>
                ) : isSelected ? (
                  <span
                    className="material-symbols-outlined study-segment-sidebar__icon study-segment-sidebar__icon--studying"
                    title={t('studySidebar.studying')}
                  >
                    edit_note
                  </span>
                ) : isLocked ? (
                  <span
                    className="material-symbols-outlined study-segment-sidebar__icon study-segment-sidebar__icon--locked"
                    title={t('studySidebar.locked')}
                  >
                    lock
                  </span>
                ) : null}
              </div>

              {/* Preview nội dung segment */}
              <div className="study-segment-sidebar__preview">
                {isAttempted ? (
                  <p className="study-segment-sidebar__text">
                    {seg.transcriptOriginal || seg.transcriptNormalized}
                  </p>
                ) : (
                  <div className="study-segment-sidebar__masked">••••••••••••••••••••••••</div>
                )}
              </div>
            </button>
          );
        })}
      </div>
    </aside>
  );
}

export default StudySegmentSidebar;
