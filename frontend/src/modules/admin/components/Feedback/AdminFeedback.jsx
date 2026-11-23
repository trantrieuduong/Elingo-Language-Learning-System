import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { AlertTriangleIcon, RefreshIcon, LayersIcon } from '../icons/AdminIcons';
import styles from './AdminFeedback.module.css';

export function AdminSkeleton({ height = 24, width = '100%', borderRadius, className = '' }) {
  return (
    <div
      className={`${styles.skeleton} ${className}`}
      style={{
        height,
        width,
        borderRadius: borderRadius || 'var(--adm-radius-sm)',
      }}
      aria-hidden="true"
    />
  );
}

AdminSkeleton.propTypes = {
  height: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  width: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  borderRadius: PropTypes.string,
  className: PropTypes.string,
};

export function AdminSkeletonCard({ rows = 3 }) {
  return (
    <div className={styles.skeletonCard}>
      <AdminSkeleton height={16} width="40%" />
      <AdminSkeleton height={32} width="60%" />
      <div className={styles.skeletonRow}>
        <AdminSkeleton height={14} width="30%" />
        {rows > 2 && <AdminSkeleton height={14} width="25%" />}
      </div>
    </div>
  );
}

AdminSkeletonCard.propTypes = {
  rows: PropTypes.number,
};

export function AdminEmptyState({ title, description, icon: Icon = LayersIcon }) {
  return (
    <div className={styles.feedbackContainer}>
      <div className={styles.iconBox}>
        <Icon size={24} />
      </div>
      <h4 className={styles.feedbackTitle}>{title}</h4>
      {description && <p className={styles.feedbackDesc}>{description}</p>}
    </div>
  );
}

AdminEmptyState.propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.string,
  icon: PropTypes.elementType,
};

export function AdminErrorState({ message, description, onRetry }) {
  const { t } = useTranslation('admin');
  const displayText = message || description || t('feedback.errorDesc');

  return (
    <div className={`${styles.feedbackContainer} ${styles.errorContainer}`}>
      <div className={`${styles.iconBox} ${styles.errorIconBox}`}>
        <AlertTriangleIcon size={24} />
      </div>
      <h4 className={styles.feedbackTitle}>{t('feedback.errorTitle')}</h4>
      <p className={styles.feedbackDesc}>{displayText}</p>
      {onRetry && (
        <button type="button" className={styles.retryButton} onClick={onRetry}>
          <RefreshIcon size={14} />
          {t('feedback.retry')}
        </button>
      )}
    </div>
  );
}

AdminErrorState.propTypes = {
  message: PropTypes.string,
  description: PropTypes.string,
  onRetry: PropTypes.func,
};
