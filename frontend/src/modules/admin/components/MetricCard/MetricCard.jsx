import React from 'react';
import PropTypes from 'prop-types';
import { ArrowUpRightIcon, ArrowDownRightIcon } from '../icons/AdminIcons';
import styles from './MetricCard.module.css';

export default function MetricCard({
  title,
  value,
  subvalue,
  trend,
  trendLabel,
  icon: Icon,
  variant = 'default',
  badge,
  href,
  onClick,
  linkAriaLabel,
}) {
  const isPositive = typeof trend === 'number' && trend > 0;
  const isNegative = typeof trend === 'number' && trend < 0;
  const isClickable = Boolean(href || onClick);

  const handleClick = (e) => {
    if (!isClickable) return;
    if (onClick) {
      onClick(e);
    }
  };

  const handleKeyDown = (e) => {
    if (!isClickable) return;
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick(e);
    }
  };

  return (
    <article
      className={`${styles.card} ${styles[variant] || ''} ${isClickable ? styles.clickableCard : ''}`}
      onClick={isClickable ? handleClick : undefined}
      onKeyDown={isClickable ? handleKeyDown : undefined}
      role={isClickable ? 'link' : undefined}
      tabIndex={isClickable ? 0 : undefined}
      aria-label={linkAriaLabel || (isClickable ? `${title}: ${value}` : undefined)}
    >
      <div className={styles.header}>
        <div className={styles.titleGroup}>
          <span className={styles.title}>{title}</span>
          {isClickable && (
            <span className={styles.clickableIndicator} aria-hidden="true">
              <ArrowUpRightIcon size={13} />
            </span>
          )}
        </div>
        {Icon && (
          <div className={styles.iconWrapper} aria-hidden="true">
            <Icon size={18} />
          </div>
        )}
      </div>

      <div className={styles.valueRow}>
        <span className={styles.value}>{value}</span>
        {badge && <span className={styles.badge}>{badge}</span>}
      </div>

      {(subvalue || trend !== undefined) && (
        <div className={styles.footer}>
          {typeof trend === 'number' && (
            <span
              className={`${styles.trend} ${
                isPositive ? styles.positive : isNegative ? styles.negative : styles.neutral
              }`}
            >
              {isPositive ? (
                <ArrowUpRightIcon size={14} />
              ) : isNegative ? (
                <ArrowDownRightIcon size={14} />
              ) : null}
              {Math.abs(trend)}%
            </span>
          )}
          {trendLabel && <span className={styles.trendLabel}>{trendLabel}</span>}
          {subvalue && <span className={styles.subvalue}>{subvalue}</span>}
        </div>
      )}
    </article>
  );
}

MetricCard.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  subvalue: PropTypes.string,
  trend: PropTypes.number,
  trendLabel: PropTypes.string,
  icon: PropTypes.elementType,
  variant: PropTypes.oneOf(['default', 'primary', 'success', 'warning', 'danger']),
  badge: PropTypes.string,
  href: PropTypes.string,
  onClick: PropTypes.func,
  linkAriaLabel: PropTypes.string,
};
