import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminFilterBar, {
  AdminFilterNumber,
  AdminFilterSearch,
  AdminFilterSelect,
} from '../../../components/AdminFilterBar/AdminFilterBar';

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

  const statusOptions = [
    { value: '', label: t('premiumTransactions.filter.allStatus') },
    { value: 'PENDING', label: t('premiumTransactions.status.PENDING') },
    { value: 'SUCCESS', label: t('premiumTransactions.status.SUCCESS') },
    { value: 'FAILED', label: t('premiumTransactions.status.FAILED') },
    { value: 'REFUNDED', label: t('premiumTransactions.status.REFUNDED') },
  ];

  const planOptions = [
    { value: '', label: t('premiumTransactions.filter.allPlans') },
    ...plans.map((p) => ({ value: String(p.id), label: `${p.name} (#${p.id})` })),
  ];

  const sortOptions = [
    { value: 'createdAt,desc', label: t('premiumTransactions.filter.sortCreatedDesc') },
    { value: 'createdAt,asc', label: t('premiumTransactions.filter.sortCreatedAsc') },
    { value: 'paidAt,desc', label: t('premiumTransactions.filter.sortPaidDesc') },
    { value: 'amount,desc', label: t('premiumTransactions.filter.sortAmountDesc') },
    { value: 'amount,asc', label: t('premiumTransactions.filter.sortAmountAsc') },
  ];

  return (
    <AdminFilterBar
      fromLabel={t('premiumTransactions.filter.fromDate')}
      fromValue={createdFrom}
      onFromChange={onCreatedFromChange}
      toLabel={t('premiumTransactions.filter.toDate')}
      toValue={createdTo}
      onToChange={onCreatedToChange}
      sortValue={sort}
      onSortChange={onSortChange}
      sortLabel={t('premiumTransactions.filter.sortLabel')}
      sortOptions={sortOptions}
      applyLabel={t('premiumTransactions.filter.apply')}
      resetLabel={t('premiumTransactions.filter.reset')}
      filterError={filterError}
      errorVariant="banner"
      onApply={onApply}
      onReset={onReset}
    >
      <AdminFilterSearch
        value={keyword}
        onChange={onKeywordChange}
        placeholder={t('premiumTransactions.filter.keywordPlaceholder')}
      />
      <AdminFilterSelect
        value={status}
        onChange={onStatusChange}
        ariaLabel={t('premiumTransactions.columns.status')}
        options={statusOptions}
      />
      <AdminFilterSelect
        value={planId}
        onChange={onPlanIdChange}
        ariaLabel={t('premiumTransactions.columns.plan')}
        options={planOptions}
      />
      <AdminFilterNumber
        value={userId}
        onChange={onUserIdChange}
        placeholder={t('premiumTransactions.filter.userIdPlaceholder')}
      />
    </AdminFilterBar>
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
