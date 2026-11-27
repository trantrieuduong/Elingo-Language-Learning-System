import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import styles from '../../PostModerationPage/components/CommunityFilterBar.module.css';

export default function ReportFilterBar({
  status,
  onStatusChange,
  targetType,
  onTargetTypeChange,
  reporterId,
  onReporterIdChange,
  targetId,
  onTargetIdChange,
  createdFrom,
  onCreatedFromChange,
  createdTo,
  onCreatedToChange,
  sort,
  onSortChange,
  filterError,
  onApply,
  onReset,
}) {
  const { t } = useTranslation('admin');

  return (
    <form className={styles.filterCard} onSubmit={onApply}>
      <div className={styles.filterRow}>
        <select
          className={styles.selectInput}
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          aria-label={t('reports.columns.status')}
        >
          <option value="">{t('reports.filter.allStatus')}</option>
          <option value="PENDING">{t('reports.status.PENDING')}</option>
          <option value="RESOLVED">{t('reports.status.RESOLVED')}</option>
          <option value="REJECTED">{t('reports.status.REJECTED')}</option>
        </select>

        <select
          className={styles.selectInput}
          value={targetType}
          onChange={(e) => onTargetTypeChange(e.target.value)}
          aria-label={t('reports.columns.targetType')}
        >
          <option value="">{t('reports.filter.allTargetTypes')}</option>
          <option value="POST">{t('reports.targetType.POST')}</option>
          <option value="COMMENT">{t('reports.targetType.COMMENT')}</option>
          <option value="USER">{t('reports.targetType.USER')}</option>
          <option value="DECK">{t('reports.targetType.DECK')}</option>
          <option value="LESSON">{t('reports.targetType.LESSON')}</option>
        </select>

        <input
          type="number"
          min="1"
          step="1"
          className={styles.smallInput}
          value={reporterId}
          onChange={(e) => onReporterIdChange(e.target.value)}
          placeholder={t('reports.filter.reporterIdPlaceholder')}
          aria-label={t('reports.filter.reporterIdPlaceholder')}
        />

        <input
          type="number"
          min="1"
          step="1"
          className={styles.smallInput}
          value={targetId}
          onChange={(e) => onTargetIdChange(e.target.value)}
          placeholder={t('reports.filter.targetIdPlaceholder')}
          aria-label={t('reports.filter.targetIdPlaceholder')}
        />
      </div>

      <div className={styles.filterRow}>
        <div className={styles.dateGroup}>
          <label className={styles.dateLabel}>
            <span>{t('reports.filter.fromDate')}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={createdFrom}
              onChange={(e) => onCreatedFromChange(e.target.value)}
            />
          </label>
          <label className={styles.dateLabel}>
            <span>{t('reports.filter.toDate')}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={createdTo}
              onChange={(e) => onCreatedToChange(e.target.value)}
            />
          </label>
        </div>

        <select
          className={styles.selectInput}
          value={sort}
          onChange={(e) => onSortChange(e.target.value)}
          aria-label={t('reports.filter.sortLabel')}
        >
          <option value="createdAt,desc">{t('reports.filter.sortCreatedDesc')}</option>
          <option value="createdAt,asc">{t('reports.filter.sortCreatedAsc')}</option>
          <option value="resolvedAt,desc">{t('reports.filter.sortResolvedDesc')}</option>
          <option value="id,desc">{t('reports.filter.sortIdDesc')}</option>
        </select>

        <div className={styles.buttonGroup}>
          <button type="submit" className={styles.applyBtn}>
            {t('reports.filter.apply')}
          </button>
          <button type="button" className={styles.resetBtn} onClick={onReset}>
            {t('reports.filter.reset')}
          </button>
        </div>
      </div>

      {filterError && <p className={styles.filterError}>{filterError}</p>}
    </form>
  );
}

ReportFilterBar.propTypes = {
  status: PropTypes.string.isRequired,
  onStatusChange: PropTypes.func.isRequired,
  targetType: PropTypes.string.isRequired,
  onTargetTypeChange: PropTypes.func.isRequired,
  reporterId: PropTypes.string.isRequired,
  onReporterIdChange: PropTypes.func.isRequired,
  targetId: PropTypes.string.isRequired,
  onTargetIdChange: PropTypes.func.isRequired,
  createdFrom: PropTypes.string.isRequired,
  onCreatedFromChange: PropTypes.func.isRequired,
  createdTo: PropTypes.string.isRequired,
  onCreatedToChange: PropTypes.func.isRequired,
  sort: PropTypes.string.isRequired,
  onSortChange: PropTypes.func.isRequired,
  filterError: PropTypes.string,
  onApply: PropTypes.func.isRequired,
  onReset: PropTypes.func.isRequired,
};
