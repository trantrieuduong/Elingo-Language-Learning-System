import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { SearchIcon, RefreshIcon } from '../../../components/icons/AdminIcons';
import styles from './UserFilterBar.module.css';

export default function UserFilterBar({
  keyword,
  onKeywordChange,
  role,
  onRoleChange,
  isVerified,
  onVerifiedChange,
  isActive,
  onActiveChange,
  createdFrom,
  onCreatedFromChange,
  createdTo,
  onCreatedToChange,
  onApply,
  onReset,
  loading = false,
}) {
  const { t } = useTranslation('admin');

  const isInvalidDateRange = Boolean(createdFrom && createdTo && createdFrom > createdTo);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (!isInvalidDateRange) {
        onApply();
      }
    }
  };

  return (
    <div className={styles.filterCard}>
      {/* Hàng 1: Tìm kiếm từ khóa và các dropdown chọn lọc */}
      <div className={styles.topRow}>
        {/* Keyword Search */}
        <div className={styles.searchBox}>
          <SearchIcon size={16} className={styles.searchIcon} />
          <input
            type="text"
            className={styles.searchInput}
            placeholder={t('users.filters.searchPlaceholder')}
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={loading}
          />
        </div>

        {/* Role Select */}
        <div className={styles.selectWrapper}>
          <label className={styles.visuallyHidden} htmlFor="user-filter-role">
            {t('users.filters.role')}
          </label>
          <select
            id="user-filter-role"
            className={styles.select}
            value={role}
            onChange={(e) => onRoleChange(e.target.value)}
            disabled={loading}
          >
            <option value="">{t('users.filters.allRoles')}</option>
            <option value="USER">{t('users.filters.roleUser')}</option>
            <option value="ADMIN">{t('users.filters.roleAdmin')}</option>
          </select>
        </div>

        {/* Verification Status */}
        <div className={styles.selectWrapper}>
          <label className={styles.visuallyHidden} htmlFor="user-filter-verified">
            {t('users.filters.verified')}
          </label>
          <select
            id="user-filter-verified"
            className={styles.select}
            value={isVerified}
            onChange={(e) => onVerifiedChange(e.target.value)}
            disabled={loading}
          >
            <option value="">{t('users.filters.allVerified')}</option>
            <option value="true">{t('users.filters.verifiedYes')}</option>
            <option value="false">{t('users.filters.verifiedNo')}</option>
          </select>
        </div>

        {/* Account Status */}
        <div className={styles.selectWrapper}>
          <label className={styles.visuallyHidden} htmlFor="user-filter-status">
            {t('users.filters.status')}
          </label>
          <select
            id="user-filter-status"
            className={styles.select}
            value={isActive}
            onChange={(e) => onActiveChange(e.target.value)}
            disabled={loading}
          >
            <option value="">{t('users.filters.allStatus')}</option>
            <option value="true">{t('users.filters.statusActive')}</option>
            <option value="false">{t('users.filters.statusLocked')}</option>
          </select>
        </div>
      </div>

      {/* Hàng 2: Bộ chọn khoảng ngày và các nút thao tác */}
      <div className={styles.bottomRow}>
        <div className={styles.dateGroup}>
          <div className={styles.dateField}>
            <span className={styles.dateLabel}>{t('users.filters.createdFrom')}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={createdFrom}
              onChange={(e) => onCreatedFromChange(e.target.value)}
              disabled={loading}
            />
          </div>
          <span className={styles.dateSeparator}>—</span>
          <div className={styles.dateField}>
            <span className={styles.dateLabel}>{t('users.filters.createdTo')}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={createdTo}
              onChange={(e) => onCreatedToChange(e.target.value)}
              disabled={loading}
            />
          </div>
        </div>

        {/* Action Buttons */}
        <div className={styles.actionsGroup}>
          <button
            type="button"
            className={styles.applyBtn}
            onClick={onApply}
            disabled={loading || isInvalidDateRange}
          >
            <SearchIcon size={15} />
            <span>{t('users.filters.searchBtn')}</span>
          </button>
          <button
            type="button"
            className={styles.resetBtn}
            onClick={onReset}
            disabled={loading}
          >
            <RefreshIcon size={15} />
            <span>{t('users.filters.resetBtn')}</span>
          </button>
        </div>
      </div>

      {isInvalidDateRange && (
        <div className={styles.dateRangeError}>
          {t('users.filters.invalidDateRange')}
        </div>
      )}
    </div>
  );
}

UserFilterBar.propTypes = {
  keyword: PropTypes.string.isRequired,
  onKeywordChange: PropTypes.func.isRequired,
  role: PropTypes.string.isRequired,
  onRoleChange: PropTypes.func.isRequired,
  isVerified: PropTypes.string.isRequired,
  onVerifiedChange: PropTypes.func.isRequired,
  isActive: PropTypes.string.isRequired,
  onActiveChange: PropTypes.func.isRequired,
  createdFrom: PropTypes.string.isRequired,
  onCreatedFromChange: PropTypes.func.isRequired,
  createdTo: PropTypes.string.isRequired,
  onCreatedToChange: PropTypes.func.isRequired,
  onApply: PropTypes.func.isRequired,
  onReset: PropTypes.func.isRequired,
  loading: PropTypes.bool,
};
