import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import styles from './VisualCharts.module.css';

/**
 * Pure SVG Area / Line Time Series Chart
 * Renders smooth polyline / polygon with interactive hover tooltip
 */
export default function AreaTimeSeriesChart({
  data = [],
  series,
  height = 220,
  emptyMessage,
  valueFormatter,
}) {
  const { t, i18n } = useTranslation('admin');
  const [hoverIndex, setHoverIndex] = useState(null);

  const locale = i18n.language && i18n.language.startsWith('en') ? 'en-US' : 'vi-VN';
  const resolvedEmptyMessage = emptyMessage || t('charts.emptyTimeSeries');
  const defaultValueFormatter = (val) => Number(val).toLocaleString(locale);
  const formatter = valueFormatter || defaultValueFormatter;

  const resolvedSeries = series || [
    { key: 'value', label: t('charts.defaultValue'), color: 'var(--adm-chart-primary)' },
  ];

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

  // Find max value across all series
  let maxValue = 0;
  data.forEach((d) => {
    resolvedSeries.forEach((s) => {
      const val = Number(d[s.key]) || 0;
      if (val > maxValue) maxValue = val;
    });
  });
  if (maxValue === 0) maxValue = 10;

  // Y-axis grid ticks (4 intervals)
  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((ratio) => Math.round(maxValue * ratio));

  // Compute points for each series
  const pointsPerSeries = resolvedSeries.map((s) => {
    const points = data.map((d, index) => {
      const x = padding.left + (index / (data.length - 1 || 1)) * chartWidth;
      const val = Number(d[s.key]) || 0;
      const y = padding.top + chartHeight - (val / maxValue) * chartHeight;
      return { x, y, value: val, date: d.date || d.key };
    });
    return { ...s, points };
  });

  const activePointData = hoverIndex !== null ? data[hoverIndex] : null;

  return (
    <div className={styles.chartWrapper}>
      {/* Legend */}
      <div className={styles.legendRow}>
        {resolvedSeries.map((s) => (
          <div key={s.key} className={styles.legendItem}>
            <span className={styles.legendDot} style={{ background: s.color }} />
            <span className={styles.legendLabel}>{s.label}</span>
          </div>
        ))}
      </div>

      <svg
        viewBox={`0 0 ${width} ${height}`}
        className={styles.svgChart}
        preserveAspectRatio="none"
        onMouseLeave={() => setHoverIndex(null)}
      >
        {/* Y Grid lines and labels */}
        {yTicks.map((tick, i) => {
          const y = padding.top + chartHeight - (tick / maxValue) * chartHeight;
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
                {tick >= 1000000
                  ? `${(tick / 1000000).toFixed(1)}M`
                  : tick >= 1000
                    ? `${(tick / 1000).toFixed(0)}k`
                    : tick}
              </text>
            </g>
          );
        })}

        {/* Areas & Lines */}
        {pointsPerSeries.map((s) => {
          const linePointsStr = s.points.map((p) => `${p.x},${p.y}`).join(' ');
          const firstPoint = s.points[0];
          const lastPoint = s.points[s.points.length - 1];
          const areaPointsStr = `${firstPoint.x},${padding.top + chartHeight} ${linePointsStr} ${lastPoint.x},${padding.top + chartHeight}`;

          return (
            <g key={s.key}>
              {/* Fill polygon */}
              <polygon
                points={areaPointsStr}
                fill={s.color}
                fillOpacity="0.12"
                className={styles.areaPolygon}
              />
              {/* Stroke line */}
              <polyline
                points={linePointsStr}
                fill="none"
                stroke={s.color}
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </g>
          );
        })}

        {/* Hover interaction points */}
        {data.map((d, index) => {
          const x = padding.left + (index / (data.length - 1 || 1)) * chartWidth;
          const isHovered = hoverIndex === index;

          return (
            <g key={index}>
              {/* Vertical guideline on hover */}
              {isHovered && (
                <line
                  x1={x}
                  y1={padding.top}
                  x2={x}
                  y2={padding.top + chartHeight}
                  className={styles.hoverLine}
                />
              )}

              {/* Dots for series */}
              {pointsPerSeries.map((s) => {
                const pt = s.points[index];
                return (
                  <circle
                    key={s.key}
                    cx={pt.x}
                    cy={pt.y}
                    r={isHovered ? 4.5 : 2.5}
                    fill="var(--adm-bg-surface)"
                    stroke={s.color}
                    strokeWidth={isHovered ? 2.5 : 1.5}
                    className={styles.dataPoint}
                  />
                );
              })}

              {/* Invisible interaction bar */}
              <rect
                x={x - chartWidth / (data.length * 2 || 1)}
                y={padding.top}
                width={chartWidth / (data.length || 1)}
                height={chartHeight}
                fill="transparent"
                onMouseEnter={() => setHoverIndex(index)}
              />
            </g>
          );
        })}

        {/* X Axis sparse labels */}
        {data.map((d, index) => {
          const step = Math.max(1, Math.floor(data.length / 6));
          if (index % step !== 0 && index !== data.length - 1) return null;
          const x = padding.left + (index / (data.length - 1 || 1)) * chartWidth;
          const label = (d.date || d.key || '').slice(5); // format MM-DD
          return (
            <text key={index} x={x} y={height - 8} className={styles.axisLabel} textAnchor="middle">
              {label}
            </text>
          );
        })}
      </svg>

      {/* Floating tooltip */}
      {activePointData && hoverIndex !== null && (
        <div
          className={styles.tooltip}
          style={{
            left: `${padding.left + (hoverIndex / (data.length - 1 || 1)) * chartWidth}px`,
            top: `${padding.top + 8}px`,
          }}
        >
          <div className={styles.tooltipDate}>{activePointData.date || activePointData.key}</div>
          {resolvedSeries.map((s) => (
            <div key={s.key} className={styles.tooltipItem}>
              <span className={styles.tooltipDot} style={{ background: s.color }} />
              <span className={styles.tooltipKey}>{s.label}:</span>
              <strong className={styles.tooltipVal}>
                {formatter(activePointData[s.key] || 0)}
              </strong>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

AreaTimeSeriesChart.propTypes = {
  data: PropTypes.arrayOf(PropTypes.object),
  series: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      color: PropTypes.string.isRequired,
    })
  ),
  height: PropTypes.number,
  emptyMessage: PropTypes.string,
  valueFormatter: PropTypes.func,
};
