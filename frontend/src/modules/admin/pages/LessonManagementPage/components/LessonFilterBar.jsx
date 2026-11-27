import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { SearchIcon, RefreshIcon } from '../../../components/icons/AdminIcons';
import styles from './LessonFilterBar.module.css';

export default function LessonFilterBar({
  keyword,
  onKeywordChange,
  status,
  onStatusChange,
  isPremium,
  onPremiumChange,
  tagId,
  onTagChange,
  cefrId,
  onCefrChange,
  tags = [],
  cefrLevels = [],
  onApply,
  onReset,
  loading = false,
}) {
  const { t } = useTranslation('admin');

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (onApply) onApply();
    }
  };

  return (
    <div className={styles.filterCard}>
      <div className={styles.filterGrid}>
        {/* Tìm kiếm từ khóa */}
        <div className={styles.filterItem}>
          <label className={styles.filterLabel} htmlFor="lesson-filter-keyword">
            {t('content.lessons.searchPlaceholder')}
          </label>
          <div className={styles.searchBox}>
            <SearchIcon size={15} className={styles.searchIcon} />
            <input
              id="lesson-filter-keyword"
              type="text"
              className={styles.searchInput}
              placeholder={t('content.lessons.searchPlaceholder')}
              value={keyword}
              onChange={(e) => onKeywordChange(e.target.value)}
              onKeyDown={handleKeyDown}
            />
          </div>
        </div>

        {/* Chủ đề (Tag) */}
        <div className={styles.filterItem}>
          <label className={styles.filterLabel} htmlFor="lesson-filter-tag">
            {t('content.lessons.tagFilter')}
          </label>
          <select
            id="lesson-filter-tag"
            className={styles.select}
            value={tagId}
            onChange={(e) => onTagChange(e.target.value)}
          >
            <option value="">{t('content.lessons.allTags')}</option>
            {tags.map((tag) => (
              <option key={tag.id} value={String(tag.id)}>
                #{tag.label}
              </option>
            ))}
          </select>
        </div>

        {/* Trạng thái */}
        <div className={styles.filterItem}>
          <label className={styles.filterLabel} htmlFor="lesson-filter-status">
            {t('content.lessons.statusFilter')}
          </label>
          <select
            id="lesson-filter-status"
            className={styles.select}
            value={status}
            onChange={(e) => onStatusChange(e.target.value)}
          >
            <option value="">{t('content.lessons.allStatuses')}</option>
            <option value="DRAFT">{t('content.lessons.statusDraft')}</option>
            <option value="PUBLISHED">{t('content.lessons.statusPublished')}</option>
            <option value="ARCHIVED">{t('content.lessons.statusArchived')}</option>
          </select>
        </div>

        {/* Phân hạng */}
        <div className={styles.filterItem}>
          <label className={styles.filterLabel} htmlFor="lesson-filter-premium">
            {t('content.lessons.premiumFilter')}
          </label>
          <select
            id="lesson-filter-premium"
            className={styles.select}
            value={isPremium}
            onChange={(e) => onPremiumChange(e.target.value)}
          >
            <option value="">{t('content.lessons.allTiers')}</option>
            <option value="false">{t('content.lessons.freeTier')}</option>
            <option value="true">{t('content.lessons.premiumTier')}</option>
          </select>
        </div>

        {/* Trình độ CEFR */}
        <div className={styles.filterItem}>
          <label className={styles.filterLabel} htmlFor="lesson-filter-cefr">
            {t('content.lessons.cefrFilter')}
          </label>
          <select
            id="lesson-filter-cefr"
            className={styles.select}
            value={cefrId}
            onChange={(e) => onCefrChange(e.target.value)}
          >
            <option value="">{t('content.lessons.allCefrLevels')}</option>
            {cefrLevels.map((level) => (
              <option key={level.id} value={String(level.id)}>
                {level.label}
              </option>
            ))}
          </select>
        </div>

        {/* Xóa bộ lọc */}
        <div className={styles.filterActionItem}>
          <button
            type="button"
            className={styles.resetBtn}
            onClick={onReset}
            disabled={loading}
          >
            <RefreshIcon size={15} />
            <span>{t('content.lessons.clearFilters')}</span>
          </button>
        </div>
      </div>
    </div>
  );
}

LessonFilterBar.propTypes = {
  keyword: PropTypes.string.isRequired,
  onKeywordChange: PropTypes.func.isRequired,
  status: PropTypes.string.isRequired,
  onStatusChange: PropTypes.func.isRequired,
  isPremium: PropTypes.string.isRequired,
  onPremiumChange: PropTypes.func.isRequired,
  tagId: PropTypes.string.isRequired,
  onTagChange: PropTypes.func.isRequired,
  cefrId: PropTypes.string.isRequired,
  onCefrChange: PropTypes.func.isRequired,
  tags: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      label: PropTypes.string.isRequired,
    })
  ),
  cefrLevels: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      label: PropTypes.string.isRequired,
    })
  ),
  onApply: PropTypes.func.isRequired,
  onReset: PropTypes.func.isRequired,
  loading: PropTypes.bool,
};
