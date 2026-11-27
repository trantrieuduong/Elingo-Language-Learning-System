import React, { useState, useEffect, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { TrashIcon } from '../../../components/icons/AdminIcons';
import { clampMs, formatMs, maskTimeInput, parseTime } from '../utils/timeMask';
import styles from './SegmentForm.module.css';

export default function SegmentForm({
  mode = null,
  segment = null,
  segments = [],
  durationMs = null,
  defaultStartMs = 0,
  saving = false,
  error = null,
  onSave,
  onDelete,
  onDirtyChange,
  onCancel,
}) {
  const { t } = useTranslation('admin');

  const initialValues = useMemo(() => {
    if (mode === 'edit' && segment) {
      return {
        startMs: segment.startMs,
        endMs: segment.endMs,
        startText: formatMs(segment.startMs),
        endText: formatMs(segment.endMs),
        transcriptOriginal: segment.transcriptOriginal || '',
        translation: segment.translation || '',
      };
    }
    if (mode === 'new') {
      const start = clampMs(defaultStartMs, durationMs);
      const end = clampMs(start + 5000, durationMs);
      return {
        startMs: start,
        endMs: end,
        startText: formatMs(start),
        endText: formatMs(end),
        transcriptOriginal: '',
        translation: '',
      };
    }
    return {
      startMs: null,
      endMs: null,
      startText: '',
      endText: '',
      transcriptOriginal: '',
      translation: '',
    };
  }, [mode, segment, defaultStartMs, durationMs]);

  const [startText, setStartText] = useState(initialValues.startText);
  const [endText, setEndText] = useState(initialValues.endText);
  const [transcriptOriginal, setTranscriptOriginal] = useState(initialValues.transcriptOriginal);
  const [translation, setTranslation] = useState(initialValues.translation);

  useEffect(() => {
    setStartText(initialValues.startText);
    setEndText(initialValues.endText);
    setTranscriptOriginal(initialValues.transcriptOriginal);
    setTranslation(initialValues.translation);
  }, [initialValues]);

  const isDirty =
    mode !== null &&
    (startText !== initialValues.startText ||
      endText !== initialValues.endText ||
      transcriptOriginal !== initialValues.transcriptOriginal ||
      translation !== initialValues.translation);

  useEffect(() => {
    onDirtyChange?.(isDirty);
  }, [isDirty, onDirtyChange]);

  if (!mode) {
    return (
      <div className={styles.hintCard}>
        <p className={styles.selectHint}>{t('content.lessons.selectHint')}</p>
      </div>
    );
  }

  const startMs =
    startText === initialValues.startText ? initialValues.startMs : parseTime(startText);
  const endMs =
    endText === initialValues.endText ? initialValues.endMs : parseTime(endText);

  let validationError = null;
  if (startMs === null || endMs === null) {
    validationError = t('content.lessons.timeInvalid');
  } else if (startMs >= endMs) {
    validationError = t('content.lessons.timeOrderError');
  } else if (durationMs && endMs > durationMs) {
    validationError = t('content.lessons.timeExceedsError');
  } else {
    const currentId = mode === 'edit' && segment ? segment.id : null;
    const overlaps = segments.some(
      (s) => s.id !== currentId && s.startMs < endMs && s.endMs > startMs
    );
    if (overlaps) {
      validationError = t('content.lessons.timeOverlapError');
    } else if (!transcriptOriginal.trim()) {
      validationError = t('content.lessons.transcriptRequired');
    } else if (!translation.trim()) {
      validationError = t('content.lessons.translationRequired');
    }
  }

  const handleStartBlur = () => {
    const ms = parseTime(startText);
    if (ms !== null) {
      setStartText(formatMs(clampMs(ms, durationMs)));
    }
  };

  const handleEndBlur = () => {
    const ms = parseTime(endText);
    if (ms !== null) {
      setEndText(formatMs(clampMs(ms, durationMs)));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validationError || !isDirty || saving) return;
    onSave({
      startMs,
      endMs,
      transcriptOriginal: transcriptOriginal.trim(),
      translation: translation.trim(),
    });
  };

  return (
    <form className={styles.formCard} onSubmit={handleSubmit}>
      <div className={styles.formHeader}>
        <h3 className={styles.formTitle}>
          {mode === 'new'
            ? t('content.lessons.newSegmentTitle')
            : t('content.lessons.editSegmentTitle')}
        </h3>
      </div>

      <div className={styles.formBody}>
        <div className={styles.timeRow}>
          <div className={styles.formGroup}>
            <label htmlFor="seg-start-input" className={styles.formLabel}>
              {t('content.lessons.startTimeLabel')}
            </label>
            <input
              id="seg-start-input"
              type="text"
              inputMode="numeric"
              placeholder="mm:ss"
              className={styles.timeInput}
              value={startText}
              onChange={(e) => setStartText(maskTimeInput(e.target.value))}
              onBlur={handleStartBlur}
              disabled={saving}
            />
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="seg-end-input" className={styles.formLabel}>
              {t('content.lessons.endTimeLabel')}
            </label>
            <input
              id="seg-end-input"
              type="text"
              inputMode="numeric"
              placeholder="mm:ss"
              className={styles.timeInput}
              value={endText}
              onChange={(e) => setEndText(maskTimeInput(e.target.value))}
              onBlur={handleEndBlur}
              disabled={saving}
            />
          </div>
        </div>

        <div className={styles.formGroup}>
          <label htmlFor="seg-transcript-input" className={styles.formLabel}>
            {t('content.lessons.transcriptLabel')} *
          </label>
          <textarea
            id="seg-transcript-input"
            className={styles.textarea}
            maxLength={2000}
            value={transcriptOriginal}
            onChange={(e) => setTranscriptOriginal(e.target.value)}
            disabled={saving}
          />
        </div>

        <div className={styles.formGroup}>
          <label htmlFor="seg-translation-input" className={styles.formLabel}>
            {t('content.lessons.translationLabel')} *
          </label>
          <textarea
            id="seg-translation-input"
            className={styles.textarea}
            maxLength={2000}
            value={translation}
            onChange={(e) => setTranslation(e.target.value)}
            disabled={saving}
          />
        </div>

        {validationError && <p className={styles.validationError}>{validationError}</p>}
        {error && <p className={styles.serverError}>{error}</p>}
      </div>

      <div className={styles.formFooter}>
        <div>
          {mode === 'edit' && segment && (
            <button
              type="button"
              className={styles.deleteBtn}
              onClick={() => onDelete(segment)}
              disabled={saving}
            >
              <TrashIcon size={14} />
              <span>{t('content.lessons.deleteSegmentBtn')}</span>
            </button>
          )}
        </div>

        <div className={styles.rightActions}>
          <button
            type="button"
            className={styles.cancelBtn}
            onClick={onCancel}
            disabled={saving}
          >
            {t('content.lessons.cancelBtn')}
          </button>
          <button
            type="submit"
            className={styles.saveBtn}
            disabled={!isDirty || Boolean(validationError) || saving}
          >
            {t('content.lessons.saveSegmentBtn')}
          </button>
        </div>
      </div>
    </form>
  );
}

SegmentForm.propTypes = {
  mode: PropTypes.oneOf(['new', 'edit', null]),
  segment: PropTypes.object,
  segments: PropTypes.array,
  durationMs: PropTypes.number,
  defaultStartMs: PropTypes.number,
  saving: PropTypes.bool,
  error: PropTypes.string,
  onSave: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  onDirtyChange: PropTypes.func,
  onCancel: PropTypes.func.isRequired,
};
