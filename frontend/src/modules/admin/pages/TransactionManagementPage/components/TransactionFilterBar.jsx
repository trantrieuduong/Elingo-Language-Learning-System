import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { SearchIcon } from '../../../components/icons/AdminIcons';
import styles from './TransactionFilterBar.module.css';

export default function TransactionFilterBar({
  keyword,
  onKeywordChange,
  status,
  onStatusChange,
  planId,
  onPlanIdChange,
  plans,
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
            placeholder={t('premiumTransactions.filter.keywordPlaceholder')}
            aria-label={t('premiumTransactions.filter.keywordPlaceholder')}
          />
        </div>

        <select
          className={styles.selectInput}
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          aria-label={t('premiumTransactions.columns.status')}
        >
          <option value="">{t('premiumTransactions.filter.allStatus')}</option>
          <option value="PENDING">{t('premiumTransactions.status.PENDING')}</option>
          <option value="SUCCESS">{t('premiumTransactions.status.SUCCESS')}</option>
          <option value="FAILED">{t('premiumTransactions.status.FAILED')}</option>
          <option value="REFUNDED">{t('premiumTransactions.status.REFUNDED')}</option>
        </select>

        <select
          className={styles.selectInput}
          value={planId}
          onChange={(e) => onPlanIdChange(e.target.value)}
          aria-label={t('premiumTransactions.columns.plan')}
        >
          <option value="">{t('premiumTransactions.filter.allPlans')}</option>
          {plans.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name} (#{p.id})
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
          placeholder={t('premiumTransactions.filter.userIdPlaceholder')}
          aria-label={t('premiumTransactions.filter.userIdPlaceholder')}
        />
      </div>

      <div className={styles.filterRow}>
        <div className={styles.dateGroup}>
          <label className={styles.dateLabel}>
            <span>{t('premiumTransactions.filter.fromDate')}</span>
            <input
              type="date"
              className={styles.dateInput}
              value={createdFrom}
              onChange={(e) => onCreatedFromChange(e.target.value)}
            />
          </label>
          <label className={styles.dateLabel}>
            <span>{t('premiumTransactions.filter.toDate')}</span>
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
          aria-label={t('premiumTransactions.filter.sortLabel')}
        >
          <option value="createdAt,desc">{t('premiumTransactions.filter.sortCreatedDesc')}</option>
          <option value="createdAt,asc">{t('premiumTransactions.filter.sortCreatedAsc')}</option>
          <option value="paidAt,desc">{t('premiumTransactions.filter.sortPaidDesc')}</option>
          <option value="amount,desc">{t('premiumTransactions.filter.sortAmountDesc')}</option>
          <option value="amount,asc">{t('premiumTransactions.filter.sortAmountAsc')}</option>
        </select>

        <div className={styles.buttonGroup}>
          <button type="submit" className={styles.applyBtn}>
            {t('premiumTransactions.filter.apply')}
          </button>
          <button type="button" className={styles.resetBtn} onClick={onReset}>
            {t('premiumTransactions.filter.reset')}
          </button>
        </div>
      </div>

      {filterError && <div className={styles.errorBanner}>{filterError}</div>}
    </form>
  );
}

TransactionFilterBar.propTypes = {
  keyword: PropTypes.string.isRequired,
  onKeywordChange: PropTypes.func.isRequired,
  status: PropTypes.string.isRequired,
  onStatusChange: PropTypes.func.isRequired,
  planId: PropTypes.string.isRequired,
  onPlanIdChange: PropTypes.func.isRequired,
  plans: PropTypes.array.isRequired,
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
