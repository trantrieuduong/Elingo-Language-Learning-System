const getScoreClass = (score, passScore) => {
  if (score < passScore) return "shadowing-score-circle-val--fail";
  return score >= 80
    ? "shadowing-score-circle-val--pass"
    : "shadowing-score-circle-val--warning";
};

const getWordClass = (accuracy) => {
  if (accuracy >= 80) return "shadowing-word-chip--good";
  return accuracy >= 50
    ? "shadowing-word-chip--medium"
    : "shadowing-word-chip--poor";
};

function ShadowingResult({
  displayWordsList,
  localAudioUrl,
  passScore,
  segmentResult,
  t,
}) {
  const audioUrl = localAudioUrl || segmentResult.audioUrl;
  const hasWordsAccuracy = segmentResult.wordsAccuracy?.length > 0;
  return (
    <div className="shadowing-result-card glass-card">
      <div className="shadowing-result-top">
        {/* Vòng tròn điểm số SVG */}
        <div className="shadowing-score-circle-wrapper">
          <svg className="shadowing-score-svg" viewBox="0 0 100 100">
            <circle
              className="shadowing-score-circle-bg"
              cx="50"
              cy="50"
              r="42"
            />
            <circle
              className={`shadowing-score-circle-val ${getScoreClass(
                segmentResult.score,
                passScore
              )}`}
              cx="50"
              cy="50"
              r="42"
              strokeDasharray={264}
              strokeDashoffset={
                264 -
                (264 * Math.max(0, Math.min(100, segmentResult.score))) / 100
              }
            />
          </svg>
          <div className="shadowing-score-text-overlay">
            <span className="shadowing-score-number">
              {segmentResult.score}
            </span>
            <span className="shadowing-score-unit">/ 100</span>
          </div>
        </div>
        {/* Thông tin nhãn kết quả */}
        <div className="shadowing-result-meta">
          <div className="shadowing-result-badge-row">
            <span
              className={`shadowing-badge ${
                segmentResult.passed
                  ? "shadowing-badge--pass"
                  : "shadowing-badge--fail"
              }`}
            >
              {segmentResult.passed
                ? t("lessonStudy.passedBadge")
                : t("lessonStudy.failedBadge")}
            </span>
          </div>
          {segmentResult.isHistorical ? (
            <p className="shadowing-best-score">
              {t("shadowing.bestScoreLabel", { score: segmentResult.score })}
            </p>
          ) : (
            <ShadowingCurrentResultMeta segmentResult={segmentResult} t={t} />
          )}
        </div>
      </div>
      {/* Nghe lại bản ghi âm */}
      {audioUrl && (
        <div className="shadowing-audio-player-row">
          <span className="material-symbols-outlined">volume_up</span>
          <span className="shadowing-audio-label">
            {t("shadowing.latestRecordingLabel")}
          </span>
          <audio controls src={audioUrl} className="shadowing-audio-element" />
        </div>
      )}
      {/* Thẻ phân tích từng từ (tô màu theo độ chính xác) */}
      {hasWordsAccuracy && (
        <ShadowingWordsAccuracy
          displayWordsList={displayWordsList}
          t={t}
          wordsAccuracy={segmentResult.wordsAccuracy}
        />
      )}
    </div>
  );
}

function ShadowingCurrentResultMeta({ segmentResult, t }) {
  return (
    <>
      <p className="shadowing-best-score">
        {t("shadowing.scoreLabel")}: {segmentResult.score}
      </p>
      {typeof segmentResult.bestScore === "number" && (
        <p className="shadowing-best-score-sub">
          {t("shadowing.bestScoreLabel", { score: segmentResult.bestScore })}
        </p>
      )}
      {segmentResult.penalty > 0 && (
        <p className="shadowing-penalty-notice">
          {t("shadowing.originalScoreNotice", {
            raw: segmentResult.rawScore,
            penalty: segmentResult.penalty,
            attempts: segmentResult.attemptCount,
          })}
        </p>
      )}
    </>
  );
}

function ShadowingWordsAccuracy({ displayWordsList, t, wordsAccuracy }) {
  return (
    <div className="shadowing-words-analysis">
      <div className="shadowing-words-title">
        {t("shadowing.wordAccuracyTitle")}
      </div>
      <div className="shadowing-words-flow">
        {wordsAccuracy.map((wordItem, index) => {
          const accuracy = wordItem.accuracy ?? 0;
          const text = displayWordsList[index] || wordItem.word;
          return (
            <span
              key={index}
              className={`shadowing-word-chip ${getWordClass(accuracy)}`}
              title={`${text}: ${accuracy}%`}
            >
              {text}
            </span>
          );
        })}
      </div>
      <div className="shadowing-words-legend">
        <span className="shadowing-legend-item">
          <span className="shadowing-legend-dot shadowing-legend-dot--good"></span>
          <span>{t("shadowing.wordAccuracyLegendGood")}</span>
        </span>
        <span className="shadowing-legend-item">
          <span className="shadowing-legend-dot shadowing-legend-dot--medium"></span>
          <span>{t("shadowing.wordAccuracyLegendMedium")}</span>
        </span>
        <span className="shadowing-legend-item">
          <span className="shadowing-legend-dot shadowing-legend-dot--poor"></span>
          <span>{t("shadowing.wordAccuracyLegendPoor")}</span>
        </span>
      </div>
    </div>
  );
}

export default ShadowingResult;
