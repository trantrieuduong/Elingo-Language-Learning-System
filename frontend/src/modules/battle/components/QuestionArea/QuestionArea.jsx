import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import './QuestionArea.css';

// Tạo chuỗi gợi ý cho chế độ TYPING từ answerPattern và firstChar
const buildHint = (pattern = [], firstChar = '') => {
  if (!pattern || !pattern.length) return '';
  return pattern
    .map((n, i) =>
      i === 0
        ? (firstChar || '') + '_'.repeat(Math.max(0, n - (firstChar ? 1 : 0)))
        : '_'.repeat(n)
    )
    .join('  ');
};

// QuestionArea - Khu vực câu hỏi (Trắc nghiệm hoặc Tự luận)
const QuestionArea = ({
  question,
  answered = false,
  roundResult = null,
  serverOffsetMs = 0,
  myResult = null,
  onSubmit,
}) => {
  const { t } = useTranslation('battle');

  const [selectedOptionId, setSelectedOptionId] = useState(null);
  const [typed, setTyped] = useState('');
  const [percent, setPercent] = useState(100);

  // Thanh thời gian countdown
  useEffect(() => {
    if (roundResult) return undefined; // Hết vòng thì dừng thanh đếm

    const tick = () => {
      const remainMs = question.deadlineAt - serverOffsetMs - Date.now();
      const pct = (remainMs / question.timeLimitMs) * 100;
      setPercent(Math.max(0, Math.min(100, pct)));
    };

    tick();
    const id = setInterval(tick, 100);
    return () => clearInterval(id);
  }, [question, serverOffsetMs, roundResult]);

  const locked = answered || !!roundResult || percent === 0;
  const isTypingMode = question.mode === 'TYPING';

  const handleSelectOption = (optionId) => {
    if (locked) return;
    setSelectedOptionId(optionId);
    onSubmit?.({ selectedOptionId: optionId });
  };

  const handleTypingSubmit = (e) => {
    if (e) e.preventDefault();
    const clean = typed.trim();
    if (locked || !clean) return;
    onSubmit?.({ typedAnswer: clean });
  };

  return (
    <div className="battle-question-area glass-card">
      {/* Thanh thời gian */}
      <div className="battle-timer-track">
        <div
          className={`battle-timer-bar ${
            percent < 30 ? 'battle-timer-bar--warning' : ''
          }`}
          style={{ width: `${percent}%` }}
        />
      </div>

      {/* Thuật ngữ / Câu hỏi */}
      <div className="battle-question-header">
        <h2 className="battle-question-term text-headline-lg">{question.term}</h2>
      </div>

      {/* Khu vực trả lời */}
      {isTypingMode ? (
        <div className="battle-typing-container">
          {/* Gợi ý dạng p___ ___ */}
          {question.answerPattern && (
            <div className="battle-typing-hint" aria-label={t('play.hint')}>
              <span className="battle-typing-hint__label text-label-sm">
                {t('play.hint')}:
              </span>
              <span className="battle-typing-hint__text">
                {buildHint(question.answerPattern, question.firstChar)}
              </span>
            </div>
          )}

          {/* Form nhập từ vựng */}
          <form className="battle-typing-form" onSubmit={handleTypingSubmit}>
            <input
              type="text"
              className="battle-typing-input"
              value={typed}
              onChange={(e) => setTyped(e.target.value)}
              placeholder={t('play.typeHere')}
              disabled={locked}
              autoFocus
              maxLength={100}
              autoComplete="off"
              spellCheck={false}
            />
            <button
              type="submit"
              className="btn-primary battle-typing-submit-btn"
              disabled={locked || !typed.trim()}
            >
              {answered ? t('play.submitted') : t('play.submit')}
            </button>
          </form>
        </div>
      ) : (
        /* Trắc nghiệm: 4 options */
        <div className="battle-options-grid">
          {(question.options || []).map((opt) => {
            const isSelected = selectedOptionId === opt.id;
            const isCorrect =
              roundResult && opt.text === roundResult.correctAnswer;
            const isWrong =
              roundResult && isSelected && !isCorrect;

            let stateClass = '';
            if (isCorrect) {
              stateClass = 'battle-option--correct';
            } else if (isWrong) {
              stateClass = 'battle-option--wrong';
            } else if (isSelected) {
              stateClass = 'battle-option--selected';
            }

            return (
              <button
                key={opt.id}
                type="button"
                className={`battle-option glass-card ${stateClass}`}
                onClick={() => handleSelectOption(opt.id)}
                disabled={locked}
              >
                <span className="battle-option__text text-body-lg">
                  {opt.text}
                </span>
                {isCorrect && (
                  <span className="material-symbols-outlined battle-option__icon">
                    check_circle
                  </span>
                )}
                {isWrong && (
                  <span className="material-symbols-outlined battle-option__icon">
                    cancel
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}

      {/* Trạng thái chờ đối thủ */}
      {answered && !roundResult && (
        <div className="battle-question-waiting">
          <div className="app-loading-spinner battle-question-waiting__spinner" />
          <span className="text-body-md text-disabled">
            {t('play.waitingOpponent')}
          </span>
        </div>
      )}

      {/* Kết quả vòng thi đấu */}
      {roundResult && (
        <div className="battle-round-result glass-card">
          <div className="battle-round-result__header">
            <span className="battle-round-result__correct-label text-label-sm">
              {t('play.correctAnswer')}:
            </span>
            <span className="battle-round-result__correct-val text-headline-sm">
              {roundResult.correctAnswer}
            </span>
          </div>

          <div className="battle-round-result__feedback">
            {myResult?.isCorrect ? (
              <span className="battle-round-result__points battle-round-result__points--gain text-title-md">
                +{myResult.pointsGained} {t('play.points')}
              </span>
            ) : (
              <span className="battle-round-result__points battle-round-result__points--zero text-title-md">
                {!myResult?.answered ? t('play.timeUp') : t('play.zeroPoints')}
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default QuestionArea;

