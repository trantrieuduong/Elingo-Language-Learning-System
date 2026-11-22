import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import styles from './VisualCharts.module.css';

/**
 * Pure SVG Stacked Bar Chart for multi-source activity data
 */
export default function StackedBarChart({ data = [], keys = [], height = 240, emptyMessage }) {
  const { t } = useTranslation('admin');
  const [hoverIndex, setHoverIndex] = useState(null);

  const resolvedEmptyMessage = emptyMessage || t('charts.emptyActivity');

  if (!data || data.length === 0) {
    return (
      <div className={styles.emptyChart} style={{ height }}>
        <span>{resolvedEmptyMessage}</span>
      </div>
    );
  }

  const padding = { top: 20, right: 20, bottom: 30, left: 50 };
  const width = 640;
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  // Compute maximum stacked total
  let maxTotal = 0;
  data.forEach((d) => {
    let sum = 0;
    keys.forEach((k) => {
      sum += Number(d[k.key]) || 0;
    });
    if (sum > maxTotal) maxTotal = sum;
  });
  if (maxTotal === 0) maxTotal = 10;

  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((ratio) => Math.round(maxTotal * ratio));
  const barWidth = Math.max(4, Math.min(22, (chartWidth / data.length) * 0.65));

  const activeData = hoverIndex !== null ? data[hoverIndex] : null;

  return (
    <div className={styles.chartWrapper}>
      {/* Legend */}
      <div className={styles.legendRow}>
        {keys.map((k) => (
          <div key={k.key} className={styles.legendItem}>
            <span className={styles.legendDot} style={{ background: k.color }} />
            <span className={styles.legendLabel}>{k.label}</span>
          </div>
        ))}
      </div>

      <svg
        viewBox={`0 0 ${width} ${height}`}
        className={styles.svgChart}
        preserveAspectRatio="none"
        onMouseLeave={() => setHoverIndex(null)}
      >
        {/* Y Grid lines */}
        {yTicks.map((tick, i) => {
          const y = padding.top + chartHeight - (tick / maxTotal) * chartHeight;
          return (
            <g key={i}>
              <line
                x1={padding.left}
                y1={y}
                x2={width - padding.right}
                y2={y}
                className={styles.gridLine}
              />
              <text x={padding.left - 8} y={y + 4} className={styles.axisLabel} textAnchor="end">
                {tick}
              </text>
            </g>
          );
        })}

        {/* Stacked bars */}
        {data.map((d, index) => {
          const x =
            padding.left +
            (index / (data.length - 1 || 1)) * (chartWidth - barWidth) +
            barWidth / 2;
          let currentY = padding.top + chartHeight;

          return (
            <g key={index} onMouseEnter={() => setHoverIndex(index)} className={styles.barGroup}>
              {keys.map((k) => {
                const val = Number(d[k.key]) || 0;
                const segHeight = (val / maxTotal) * chartHeight;
                currentY -= segHeight;

                if (val <= 0) return null;

                return (
                  <rect
                    key={k.key}
                    x={x - barWidth / 2}
                    y={currentY}
                    width={barWidth}
                    height={segHeight}
                    fill={k.color}
                    rx={2}
                    className={styles.barSegment}
                  />
                );
              })}
            </g>
          );
        })}

        {/* X Axis labels */}
        {data.map((d, index) => {
          const step = Math.max(1, Math.floor(data.length / 7));
          if (index % step !== 0 && index !== data.length - 1) return null;
          const x =
            padding.left +
            (index / (data.length - 1 || 1)) * (chartWidth - barWidth) +
            barWidth / 2;
          const label = (d.date || d.key || '').slice(5);
          return (
            <text key={index} x={x} y={height - 8} className={styles.axisLabel} textAnchor="middle">
              {label}
            </text>
          );
        })}
      </svg>

      {/* Tooltip */}
      {activeData && hoverIndex !== null && (
        <div
          className={styles.tooltip}
          style={{
            left: `${padding.left + (hoverIndex / (data.length - 1 || 1)) * chartWidth}px`,
            top: `${padding.top + 8}px`,
          }}
        >
          <div className={styles.tooltipDate}>{activeData.date || activeData.key}</div>
          {keys.map((k) => (
            <div key={k.key} className={styles.tooltipItem}>
              <span className={styles.tooltipDot} style={{ background: k.color }} />
              <span className={styles.tooltipKey}>{k.label}:</span>
              <strong className={styles.tooltipVal}>{activeData[k.key] || 0}</strong>
            </div>
          ))}
          <div className={styles.tooltipDivider} />
          <div className={styles.tooltipTotal}>
            <span>{t('charts.totalCount')}:</span>
            <strong>{activeData.total || 0}</strong>
          </div>
        </div>
      )}
    </div>
  );
}

StackedBarChart.propTypes = {
  data: PropTypes.arrayOf(PropTypes.object),
  keys: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      color: PropTypes.string.isRequired,
    })
  ).isRequired,
  height: PropTypes.number,
  emptyMessage: PropTypes.string,
};
