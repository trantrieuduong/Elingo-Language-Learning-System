import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { ChevronLeftIcon, ChevronRightIcon } from '../icons/AdminIcons';
import styles from './AdminPagination.module.css';

/**
 * Component phân trang dùng chung cho toàn bộ phân hệ Elingo Admin
 * Tuân thủ quy chuẩn phân trang 1-indexed (one-indexed-parameters: true)
 * Hỗ trợ nhập trực tiếp số trang để chuyển nhanh đến trang mong muốn
 */
export default function AdminPagination({
  page = 1,
  totalPages = 1,
  totalElements = 0,
  isLast = false,
  onPageChange,
  unitLabel,
  isLoading = false,
}) {
  const { t } = useTranslation('admin');

  const resolvedUnitLabel = unitLabel || t('common.items');
  const safeTotalPages = Math.max(1, totalPages || 1);
  const isPrevDisabled = page <= 1 || isLoading;
  const isNextDisabled = Boolean(isLast) || page >= safeTotalPages || isLoading;

  const [inputVal, setInputVal] = useState(page);

  // Đồng bộ giá trị input khi page thay đổi từ bên ngoài
  useEffect(() => {
    setInputVal(page);
  }, [page]);

  const handlePrev = () => {
    if (!isPrevDisabled && onPageChange) {
      onPageChange(Math.max(1, page - 1));
    }
  };

  const handleNext = () => {
    if (!isNextDisabled && onPageChange) {
      onPageChange(page + 1);
    }
  };

  // Xác nhận chuyển trang khi blur hoặc ấn Enter
  const commitPage = () => {
    const parsed = parseInt(inputVal, 10);
    if (isNaN(parsed) || parsed < 1) {
      setInputVal(1);
      if (page !== 1 && onPageChange) onPageChange(1);
    } else if (parsed > safeTotalPages) {
      setInputVal(safeTotalPages);
      if (page !== safeTotalPages && onPageChange) onPageChange(safeTotalPages);
    } else if (parsed !== page && onPageChange) {
      onPageChange(parsed);
    } else {
      setInputVal(page);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      commitPage();
      e.target.blur();
    }
  };

  return (
    <div className={styles.paginationBar}>
      {/* Thông tin tổng số bản ghi và vị trí trang */}
      <div className={styles.paginationInfo}>
        <span className={styles.infoLabel}>
          {t('common.total')}:{' '}
          <strong className={styles.totalNumber}>{totalElements}</strong>{' '}
          {resolvedUnitLabel}
        </span>
        <span className={styles.separator}>•</span>
        <span className={styles.pageIndicator}>
          {t('common.page', 'Trang')} {page}/{safeTotalPages}
        </span>
      </div>

      {/* Cụm điều khiển chuyển trang & ô nhập số trang */}
      <div className={styles.paginationControls}>
        <button
          type="button"
          className={styles.navBtn}
          disabled={isPrevDisabled}
          onClick={handlePrev}
          title={t('common.prevPage', 'Trang trước')}
          aria-label={t('common.prevPage', 'Trang trước')}
        >
          <ChevronLeftIcon size={16} />
        </button>

        <div className={styles.pageInputWrapper} title={t('common.goToPageTooltip', 'Nhập số trang và nhấn Enter')}>
          <input
            type="number"
            min={1}
            max={safeTotalPages}
            value={inputVal}
            onChange={(e) => setInputVal(e.target.value)}
            onKeyDown={handleKeyDown}
            onBlur={commitPage}
            disabled={isLoading}
            className={styles.pageInput}
            aria-label={t('common.goToPage', 'Đến trang')}
          />
        </div>

        <button
          type="button"
          className={styles.navBtn}
          disabled={isNextDisabled}
          onClick={handleNext}
          title={t('common.nextPage', 'Trang sau')}
          aria-label={t('common.nextPage', 'Trang sau')}
        >
          <ChevronRightIcon size={16} />
        </button>
      </div>
    </div>
  );
}

AdminPagination.propTypes = {
  page: PropTypes.number.isRequired,
  totalPages: PropTypes.number,
  totalElements: PropTypes.number,
  isLast: PropTypes.bool,
  onPageChange: PropTypes.func.isRequired,
  unitLabel: PropTypes.string,
  isLoading: PropTypes.bool,
};
