import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { RefreshIcon } from '../icons/AdminIcons';
import styles from './DateRangeFilter.module.css';

/**
 * Format Date to YYYY-MM-DD
 */
const formatDate = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const MAX_DAYS_BY_GRANULARITY = {
  DAY: 90,
  WEEK: 180,
  MONTH: 400,
};

/**
 * Kiểm tra tính hợp lệ của khoảng ngày theo chế độ gộp (khớp StatsDateRangeRequest ở backend).
 */
const validateDateRange = (fromStr, toStr, granularity, t) => {
  if (!fromStr || !toStr) {
    return t('dashboard.filter.missingDates');
  }
  if (fromStr > toStr) {
    return t('dashboard.filter.invalidDateOrder');
  }

  const fromDate = new Date(`${fromStr}T00:00:00`);
  const toDate = new Date(`${toStr}T00:00:00`);
  const diffDays = Math.floor((toDate - fromDate) / 86400000) + 1;
  const maxDays = MAX_DAYS_BY_GRANULARITY[granularity] || MAX_DAYS_BY_GRANULARITY.DAY;

  if (diffDays > maxDays) {
    return t('dashboard.filter.exceedsMaxDays', { maxDays });
  }

  return null;
};

export default function DateRangeFilter({ value, onChange, onRefresh, isLoading = false }) {
  const { t } = useTranslation('admin');
  const [localFrom, setLocalFrom] = useState(value.from);
  const [localTo, setLocalTo] = useState(value.to);
  const [localGranularity, setLocalGranularity] = useState(value.granularity || 'DAY');
  const [activePreset, setActivePreset] = useState(30);

  const validationError = validateDateRange(localFrom, localTo, localGranularity, t);

  const handlePreset = (days) => {
    setActivePreset(days);
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - (days - 1));
    const newFrom = formatDate(start);
    const newTo = formatDate(end);
    setLocalFrom(newFrom);
    setLocalTo(newTo);
    onChange({
      from: newFrom,
      to: newTo,
      granularity: localGranularity,
    });
  };

  const handleFromChange = (val) => {
    setLocalFrom(val);
    setActivePreset(null);
  };

  const handleToChange = (val) => {
    setLocalTo(val);
    setActivePreset(null);
  };

  const handleApply = (e) => {
    e.preventDefault();
    if (validationError) return;
    onChange({
      from: localFrom,
      to: localTo,
      granularity: localGranularity,
    });
  };

  const handleGranularityChange = (g) => {
    setLocalGranularity(g);
    if (validateDateRange(localFrom, localTo, g, t)) return;
    onChange({
      from: localFrom,
      to: localTo,
      granularity: g,
    });
  };

  return (
    <div className={styles.filterBar}>
      {/* Presets */}
      <div className={styles.presetGroup}>
        <button
          type="button"
          className={`${styles.presetButton} ${activePreset === 7 ? styles.activePreset : ''}`}
          onClick={() => handlePreset(7)}
        >
          {t('dashboard.filter.preset7d')}
        </button>
        <button
          type="button"
          className={`${styles.presetButton} ${activePreset === 30 ? styles.activePreset : ''}`}
          onClick={() => handlePreset(30)}
        >
          {t('dashboard.filter.preset30d')}
        </button>
        <button
          type="button"
          className={`${styles.presetButton} ${activePreset === 90 ? styles.activePreset : ''}`}
          onClick={() => handlePreset(90)}
        >
          {t('dashboard.filter.preset90d')}
        </button>
      </div>

      <div className={styles.divider} />

      {/* Date Inputs Form */}
      <form onSubmit={handleApply} className={styles.dateForm}>
        <div className={styles.dateField}>
          <label htmlFor="adm-filter-from" className={styles.dateLabel}>
            {t('dashboard.filter.from')}
          </label>
          <input
            id="adm-filter-from"
            type="date"
            className={`${styles.dateInput} ${validationError ? styles.dateInputInvalid : ''}`}
            value={localFrom}
            onChange={(e) => handleFromChange(e.target.value)}
          />
        </div>

        <span className={styles.dateSeparator}>—</span>

        <div className={styles.dateField}>
          <label htmlFor="adm-filter-to" className={styles.dateLabel}>
            {t('dashboard.filter.to')}
          </label>
          <input
            id="adm-filter-to"
            type="date"
            className={`${styles.dateInput} ${validationError ? styles.dateInputInvalid : ''}`}
            value={localTo}
            onChange={(e) => handleToChange(e.target.value)}
          />
        </div>

        {/* Granularity switch */}
        <div className={styles.granularityGroup} role="group" aria-label="Granularity">
          <button
            type="button"
            className={`${styles.granularityBtn} ${localGranularity === 'DAY' ? styles.active : ''}`}
            onClick={() => handleGranularityChange('DAY')}
          >
            {t('dashboard.filter.day')}
          </button>
          <button
            type="button"
            className={`${styles.granularityBtn} ${localGranularity === 'WEEK' ? styles.active : ''}`}
            onClick={() => handleGranularityChange('WEEK')}
          >
            {t('dashboard.filter.week')}
          </button>
          <button
            type="button"
            className={`${styles.granularityBtn} ${localGranularity === 'MONTH' ? styles.active : ''}`}
            onClick={() => handleGranularityChange('MONTH')}
          >
            {t('dashboard.filter.month')}
          </button>
        </div>

        {/* Action Buttons: Submit and Refresh */}
        <div className={styles.actionGroup}>
          <button
            type="submit"
            className={styles.applyBtn}
            disabled={Boolean(validationError) || isLoading}
          >
            {t('dashboard.filter.apply')}
          </button>

          {onRefresh && (
            <button
              type="button"
              className={styles.refreshBtn}
              onClick={onRefresh}
              disabled={isLoading}
              title={t('dashboard.refresh')}
              aria-label={t('dashboard.refresh')}
            >
              <RefreshIcon size={16} className={isLoading ? styles.spinning : ''} />
            </button>
          )}
        </div>
      </form>

      {validationError && (
        <p className={styles.validationError} role="alert">
          {validationError}
        </p>
      )}
    </div>
  );
}

DateRangeFilter.propTypes = {
  value: PropTypes.shape({
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
    granularity: PropTypes.oneOf(['DAY', 'WEEK', 'MONTH']),
  }).isRequired,
  onChange: PropTypes.func.isRequired,
  onRefresh: PropTypes.func,
  isLoading: PropTypes.bool,
};
