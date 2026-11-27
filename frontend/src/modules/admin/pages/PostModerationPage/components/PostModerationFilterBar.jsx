import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { SearchIcon } from '../../../components/icons/AdminIcons';
import styles from './CommunityFilterBar.module.css';

export default function PostModerationFilterBar({
  keyword,
  onKeywordChange,
  status,
  onStatusChange,
  groupId,
  onGroupIdChange,
  groups,
  userId,
  onUserIdChange,
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
        <div className={styles.searchBox}>
          <SearchIcon size={15} className={styles.searchIcon} />
          <input
            type="text"
            maxLength={100}
            className={styles.searchInput}
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            placeholder={t('postModeration.filter.keywordPlaceholder')}
            aria-label={t('postModeration.filter.keywordPlaceholder')}
          />
        </div>

        <select
          className={styles.selectInput}
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          aria-label={t('postModeration.columns.status')}
        >
          <option value="">{t('postModeration.filter.allStatus')}</option>
          <option value="NEEDS_REVIEW">{t('postModeration.status.NEEDS_REVIEW')}</option>
          <option value="PENDING">{t('postModeration.status.PENDING')}</option>
          <option value="PUBLISHED">{t('postModeration.status.PUBLISHED')}</option>
          <option value="REJECTED">{t('postModeration.status.REJECTED')}</option>
          <option value="DELETED">{t('postModeration.status.DELETED')}</option>
        </select>

        <select
          className={styles.selectInput}
          value={groupId}
          onChange={(e) => onGroupIdChange(e.target.value)}
          aria-label={t('postModeration.columns.group')}
        >
          <option value="">{t('postModeration.filter.allGroups')}</option>
          {groups.map((g) => (
            <option key={g.id} value={String(g.id)}>
              {g.groupName || g.name}
            </option>
          ))}
        </select>

        <input
          type="number"
          min="1"
          step="1"
          className={styles.smallInput}
          value={userId}
          onChange={(e) => onUserIdChange(e.target.value)}
          placeholder={t('postModeration.filter.userIdPlaceholder')}
          aria-label={t('postModeration.filter.userIdPlaceholder')}
        />
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

PostModerationFilterBar.propTypes = {
  keyword: PropTypes.string.isRequired,
  onKeywordChange: PropTypes.func.isRequired,
  status: PropTypes.string.isRequired,
  onStatusChange: PropTypes.func.isRequired,
  groupId: PropTypes.string.isRequired,
  onGroupIdChange: PropTypes.func.isRequired,
  groups: PropTypes.arrayOf(PropTypes.object).isRequired,
  userId: PropTypes.string.isRequired,
  onUserIdChange: PropTypes.func.isRequired,
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
