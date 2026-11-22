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

export default function DateRangeFilter({ value, onChange, onRefresh, isLoading = false }) {
  const { t } = useTranslation('admin');
  const [localFrom, setLocalFrom] = useState(value.from);
  const [localTo, setLocalTo] = useState(value.to);
  const [localGranularity, setLocalGranularity] = useState(value.granularity || 'DAY');
  const [activePreset, setActivePreset] = useState(30);

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
    onChange({
      from: localFrom,
      to: localTo,
      granularity: localGranularity,
    });
  };

  const handleGranularityChange = (g) => {
    setLocalGranularity(g);
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
            className={styles.dateInput}
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
            className={styles.dateInput}
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
          <button type="submit" className={styles.applyBtn}>
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
