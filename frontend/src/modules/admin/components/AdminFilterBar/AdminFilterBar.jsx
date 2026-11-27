import React from 'react';
import PropTypes from 'prop-types';
import { SearchIcon } from '../icons/AdminIcons';
import styles from './AdminFilterBar.module.css';

/**
 * Ô tìm kiếm từ khóa chuẩn cho thanh bộ lọc Admin.
 */
export function AdminFilterSearch({ value, onChange, placeholder, ariaLabel, maxLength = 100 }) {
  return (
    <div className={styles.searchBox}>
      <SearchIcon size={15} className={styles.searchIcon} />
      <input
        type="text"
        maxLength={maxLength}
        className={styles.searchInput}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        aria-label={ariaLabel || placeholder}
      />
    </div>
  );
}

AdminFilterSearch.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string.isRequired,
  ariaLabel: PropTypes.string,
  maxLength: PropTypes.number,
};

/**
 * Ô chọn (select) chuẩn cho thanh bộ lọc Admin.
 */
export function AdminFilterSelect({ value, onChange, ariaLabel, options }) {
  return (
    <select
      className={styles.selectInput}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label={ariaLabel}
    >
      {options.map((opt) => (
        <option key={String(opt.value)} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  );
}

AdminFilterSelect.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  ariaLabel: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }),
  ).isRequired,
};

/**
 * Ô nhập số ID dương chuẩn cho thanh bộ lọc Admin.
 */
export function AdminFilterNumber({ value, onChange, placeholder, ariaLabel }) {
  return (
    <input
      type="number"
      min="1"
      step="1"
      className={styles.smallInput}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      aria-label={ariaLabel || placeholder}
    />
  );
}

AdminFilterNumber.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string.isRequired,
  ariaLabel: PropTypes.string,
};

/**
 * Thanh bộ lọc dùng chung cho các trang quản trị Admin.
 * Hàng trên nhận children (các trường lọc đặc thù); hàng dưới chuẩn hóa khoảng ngày, sắp xếp, nút Áp dụng/Đặt lại và thông báo lỗi.
 */
export default function AdminFilterBar({
  children,
  fromLabel,
  fromValue,
  onFromChange,
  toLabel,
  toValue,
  onToChange,
  sortValue,
  onSortChange,
  sortLabel,
  sortOptions,
  applyLabel,
  resetLabel,
  filterError,
  errorVariant = 'text',
  onApply,
  onReset,
}) {
  return (
    <form className={styles.filterCard} onSubmit={onApply}>
      <div className={styles.filterRow}>{children}</div>

      <div className={styles.filterRow}>
        <div className={styles.dateGroup}>
          <label className={styles.dateLabel}>
            <span>{fromLabel}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={fromValue}
              onChange={(e) => onFromChange(e.target.value)}
            />
          </label>
          <label className={styles.dateLabel}>
            <span>{toLabel}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={toValue}
              onChange={(e) => onToChange(e.target.value)}
            />
          </label>
        </div>

        <AdminFilterSelect
          value={sortValue}
          onChange={onSortChange}
          ariaLabel={sortLabel}
          options={sortOptions}
        />

        <div className={styles.buttonGroup}>
          <button type="submit" className={styles.applyBtn}>
            {applyLabel}
          </button>
          <button type="button" className={styles.resetBtn} onClick={onReset}>
            {resetLabel}
          </button>
        </div>
      </div>

      {filterError &&
        (errorVariant === 'banner' ? (
          <div className={styles.errorBanner}>{filterError}</div>
        ) : (
          <p className={styles.filterError}>{filterError}</p>
        ))}
    </form>
  );
}

AdminFilterBar.propTypes = {
  children: PropTypes.node.isRequired,
  fromLabel: PropTypes.string.isRequired,
  fromValue: PropTypes.string.isRequired,
  onFromChange: PropTypes.func.isRequired,
  toLabel: PropTypes.string.isRequired,
  toValue: PropTypes.string.isRequired,
  onToChange: PropTypes.func.isRequired,
  sortValue: PropTypes.string.isRequired,
  onSortChange: PropTypes.func.isRequired,
  sortLabel: PropTypes.string.isRequired,
  sortOptions: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }),
  ).isRequired,
  applyLabel: PropTypes.string.isRequired,
  resetLabel: PropTypes.string.isRequired,
  filterError: PropTypes.string,
  errorVariant: PropTypes.oneOf(['text', 'banner']),
  onApply: PropTypes.func.isRequired,
  onReset: PropTypes.func.isRequired,
};
