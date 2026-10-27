import { useTranslation } from 'react-i18next';
import { getYouTubeThumbnail } from '../../utils/youtube';
import { formatDuration } from '../../utils/format';
import './LessonCard.css';

function LessonCard({ lesson, onClick }) {
  const { t } = useTranslation(['learning']);

  if (!lesson) return null;

  const {
    title,
    description,
    durationMs,
    sourceUrl,
    thumbnailUrl,
    isPremium,
    cefrLevels = [],
    tags = [],
    userProgress,
  } = lesson;

  // Xác định thumbnail: ưu tiên thumbnailUrl từ BE, fallback sang YouTube thumbnail từ sourceUrl
  const finalThumbnail = thumbnailUrl || getYouTubeThumbnail(sourceUrl);
  const durationText = formatDuration(durationMs);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onClick?.(lesson);
    }
  };

  // Helper tính trạng thái huy hiệu chế độ
  const renderModeBadge = (mode, status, pct) => {
    const isCompleted = status === 'COMPLETED';
    const isInProgress = status === 'IN_PROGRESS';

    let badgeClass = 'lesson-card__mode-badge--neutral';
    if (isCompleted) {
      badgeClass = 'lesson-card__mode-badge--completed';
    } else if (isInProgress) {
      badgeClass = 'lesson-card__mode-badge--progress';
    }

    const roundedPct = typeof pct === 'number' ? Math.round(pct) : 0;

    return (
      <span className={`lesson-card__mode-badge ${badgeClass}`}>
        <span className="material-symbols-outlined lesson-card__mode-icon">
          {mode === 'dictation' ? 'edit_note' : 'record_voice_over'}
        </span>
        <span className="lesson-card__mode-name">
          {mode === 'dictation' ? t('lessonList.dictation') : t('lessonList.shadowing')}
        </span>
        {isCompleted && (
          <span className="material-symbols-outlined lesson-card__mode-check">check_circle</span>
        )}
        {isInProgress && <span className="lesson-card__mode-pct">{roundedPct}%</span>}
      </span>
    );
  };

  return (
    <div
      className="lesson-card glass-card"
      tabIndex={0}
      onClick={() => onClick?.(lesson)}
      onKeyDown={handleKeyDown}
    >
      {/* Thumbnail */}
      <div className="lesson-card__thumbnail-container">
        {finalThumbnail ? (
          <img
            src={finalThumbnail}
            alt={title || ''}
            className="lesson-card__thumbnail"
            loading="lazy"
          />
        ) : (
          <div className="lesson-card__thumbnail-placeholder">
            <span className="material-symbols-outlined lesson-card__placeholder-icon">
              play_lesson
            </span>
          </div>
        )}

        {/* Thời lượng */}
        {durationText && (
          <span className="lesson-card__duration-badge">
            <span className="material-symbols-outlined lesson-card__duration-icon">schedule</span>
            {durationText}
          </span>
        )}

        {/* Huy hiệu Premium */}
        {isPremium && (
          <span className="lesson-card__premium-badge">
            <span className="material-symbols-outlined lesson-card__premium-icon">
              workspace_premium
            </span>
            {t('lessonList.premium')}
          </span>
        )}
      </div>

      {/* Body */}
      <div className="lesson-card__body">
        {/* Tags & CEFR */}
        <div className="lesson-card__tags">
          {cefrLevels.map((cefr) => (
            <span key={cefr.id} className="badge badge--soft-blue lesson-card__cefr-tag">
              {cefr.label}
            </span>
          ))}
          {tags.map((tag) => (
            <span key={tag.id} className="badge badge--default lesson-card__topic-tag">
              {tag.label}
            </span>
          ))}
        </div>

        {/* Tiêu đề & Mô tả */}
        <h3 className="lesson-card__title text-title-md">{title}</h3>
        <p className="lesson-card__desc text-body-sm text-disabled">
          {description || t('lessonList.noDescription')}
        </p>

        {/* Footer hiển thị 2 chế độ học */}
        <div className="lesson-card__footer">
          {renderModeBadge(
            'dictation',
            userProgress?.dictationStatus,
            userProgress?.dictationProgressPct
          )}
          {renderModeBadge(
            'shadowing',
            userProgress?.shadowingStatus,
            userProgress?.shadowingProgressPct
          )}
        </div>
      </div>
    </div>
  );
}

export default LessonCard;
