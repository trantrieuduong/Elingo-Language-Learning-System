import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { SearchIcon } from '../../../components/icons/AdminIcons';
import styles from './CommunityFilterBar.module.css';

export default function ModerationLogFilterBar({
  postId,
  onPostIdChange,
  actorType,
  onActorTypeChange,
  action,
  onActionChange,
  aiLabel,
  onAiLabelChange,
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
        <input
          type="number"
          min="1"
          step="1"
          className={styles.smallInput}
          value={postId}
          onChange={(e) => onPostIdChange(e.target.value)}
          placeholder={t('postModeration.logs.postIdPlaceholder')}
          aria-label={t('postModeration.logs.postIdPlaceholder')}
        />

        <select
          className={styles.selectInput}
          value={actorType}
          onChange={(e) => onActorTypeChange(e.target.value)}
          aria-label={t('postModeration.logs.columns.actor')}
        >
          <option value="">{t('postModeration.logs.allActorTypes')}</option>
          <option value="AI">{t('postModeration.actorType.AI')}</option>
          <option value="ADMIN">{t('postModeration.actorType.ADMIN')}</option>
        </select>

        <select
          className={styles.selectInput}
          value={action}
          onChange={(e) => onActionChange(e.target.value)}
          aria-label={t('postModeration.logs.columns.action')}
        >
          <option value="">{t('postModeration.logs.allActions')}</option>
          <option value="FLAG_FOR_REVIEW">{t('postModeration.action.FLAG_FOR_REVIEW')}</option>
          <option value="AUTO_APPROVE">{t('postModeration.action.AUTO_APPROVE')}</option>
          <option value="AUTO_REJECT">{t('postModeration.action.AUTO_REJECT')}</option>
          <option value="MANUAL_APPROVE">{t('postModeration.action.MANUAL_APPROVE')}</option>
          <option value="MANUAL_REJECT">{t('postModeration.action.MANUAL_REJECT')}</option>
        </select>

        <div className={styles.searchBox}>
          <SearchIcon size={15} className={styles.searchIcon} />
          <input
            type="text"
            maxLength={100}
            className={styles.searchInput}
            value={aiLabel}
            onChange={(e) => onAiLabelChange(e.target.value)}
            placeholder={t('postModeration.logs.aiLabelPlaceholder')}
            aria-label={t('postModeration.logs.aiLabelPlaceholder')}
          />
        </div>
      </div>

      <div className={styles.filterRow}>
        <div className={styles.dateGroup}>
          <label className={styles.dateLabel}>
            <span>{t('postModeration.filter.fromDate')}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={createdFrom}
              onChange={(e) => onCreatedFromChange(e.target.value)}
            />
          </label>
          <label className={styles.dateLabel}>
            <span>{t('postModeration.filter.toDate')}</span>
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
          aria-label={t('postModeration.filter.sortLabel')}
        >
          <option value="createdAt,desc">{t('postModeration.filter.sortCreatedDesc')}</option>
          <option value="createdAt,asc">{t('postModeration.filter.sortCreatedAsc')}</option>
          <option value="id,desc">{t('postModeration.filter.sortIdDesc')}</option>
        </select>

        <div className={styles.buttonGroup}>
          <button type="submit" className={styles.applyBtn}>
            {t('postModeration.filter.apply')}
          </button>
          <button type="button" className={styles.resetBtn} onClick={onReset}>
            {t('postModeration.filter.reset')}
          </button>
        </div>
      </div>

      {filterError && <p className={styles.filterError}>{filterError}</p>}
    </form>
  );
}

ModerationLogFilterBar.propTypes = {
  postId: PropTypes.string.isRequired,
  onPostIdChange: PropTypes.func.isRequired,
  actorType: PropTypes.string.isRequired,
  onActorTypeChange: PropTypes.func.isRequired,
  action: PropTypes.string.isRequired,
  onActionChange: PropTypes.func.isRequired,
  aiLabel: PropTypes.string.isRequired,
  onAiLabelChange: PropTypes.func.isRequired,
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
