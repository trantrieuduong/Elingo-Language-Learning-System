import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import styles from './VisualCharts.module.css';

/**
 * Category Breakdown Component
 * Displays a multi-segment horizontal gauge bar and itemized percentage list
 */
export default function CategoryBreakdown({ title, items = [], total, totalLabel }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language && i18n.language.startsWith('en') ? 'en-US' : 'vi-VN';
  const resolvedTotalLabel = totalLabel || t('charts.total');

  const computedTotal =
    typeof total === 'number'
      ? total
      : items.reduce((sum, item) => sum + (Number(item.count || item.value) || 0), 0);

  return (
    <div className={styles.breakdownCard}>
      {title && (
        <div className={styles.breakdownHeader}>
          <h4 className={styles.breakdownTitle}>{title}</h4>
          {computedTotal > 0 && (
            <span className={styles.breakdownTotal}>
              {resolvedTotalLabel}: <strong>{computedTotal.toLocaleString(locale)}</strong>
            </span>
          )}
        </div>
      )}

      {/* Progress segmented bar */}
      <div className={styles.segmentBar}>
        {items.map((item, idx) => {
          const val = Number(item.count || item.value) || 0;
          const percentage =
            typeof item.percentage === 'number'
              ? item.percentage
              : computedTotal > 0
                ? (val / computedTotal) * 100
                : 0;

          if (percentage <= 0) return null;

          return (
            <div
              key={item.category || item.label || idx}
              className={styles.segmentItem}
              style={{
                width: `${percentage}%`,
                background: item.color || 'var(--adm-primary)',
              }}
              title={`${item.label || item.category}: ${percentage.toFixed(1)}%`}
            />
          );
        })}
      </div>

      {/* Legend list */}
      <div className={styles.breakdownList}>
        {items.map((item, idx) => {
          const val = Number(item.count || item.value) || 0;
          const percentage =
            typeof item.percentage === 'number'
              ? item.percentage
              : computedTotal > 0
                ? (val / computedTotal) * 100
                : 0;

          return (
            <div key={item.category || item.label || idx} className={styles.breakdownRow}>
              <div className={styles.breakdownLabelGroup}>
                <span
                  className={styles.breakdownDot}
                  style={{ background: item.color || 'var(--adm-primary)' }}
                />
                <span className={styles.breakdownLabel}>{item.label || item.category}</span>
              </div>
              <div className={styles.breakdownValGroup}>
                <span className={styles.breakdownCount}>{val.toLocaleString(locale)}</span>
                <span className={styles.breakdownPercent}>({percentage.toFixed(1)}%)</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

CategoryBreakdown.propTypes = {
  title: PropTypes.string,
  items: PropTypes.arrayOf(
    PropTypes.shape({
      category: PropTypes.string,
      label: PropTypes.string,
      count: PropTypes.number,
      value: PropTypes.number,
      percentage: PropTypes.number,
      color: PropTypes.string,
    })
  ).isRequired,
  total: PropTypes.number,
  totalLabel: PropTypes.string,
};
