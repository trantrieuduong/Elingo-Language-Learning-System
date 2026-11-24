function ShadowingCompletionCard({
  onNavigate,
  passedSegmentsCount,
  segments,
  slug,
  t,
}) {
  return (
    <div className="shadowing-completion-card glass-card">
      <div className="shadowing-completion-icon-wrapper">
        <span className="material-symbols-outlined shadowing-completion-icon">
          verified
        </span>
      </div>
      <h2 className="shadowing-completion-title">
        {t("shadowing.completedTitle")}
      </h2>
      <p className="shadowing-completion-desc">
        {t("shadowing.completedSubtitle")}
      </p>
      <div className="dictation-completion-stats">
        <span className="dictation-completion-stats-tag">
          {t("lessonStudy.passedSegmentsStats", {
            passed: passedSegmentsCount,
            total: segments.length,
          })}
        </span>
      </div>
      <div className="shadowing-completion-actions">
        <button
          type="button"
          className="btn-secondary"
          onClick={() => onNavigate && onNavigate("/lessons")}
        >
          {t("common:back")}
        </button>
        <button
          type="button"
          className="btn-primary"
          onClick={() =>
            onNavigate &&
            onNavigate(`/lessons/dictation/${encodeURIComponent(slug)}`)
          }
        >
          <span className="material-symbols-outlined">edit_note</span>
          <span>{t("shadowing.learnDictation")}</span>
        </button>
      </div>
    </div>
  );
}

export default ShadowingCompletionCard;
