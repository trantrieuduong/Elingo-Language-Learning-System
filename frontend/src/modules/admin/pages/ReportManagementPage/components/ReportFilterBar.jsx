import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminFilterBar, {
  AdminFilterNumber,
  AdminFilterSelect,
} from '../../../components/AdminFilterBar/AdminFilterBar';

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

  const statusOptions = [
    { value: '', label: t('reports.filter.allStatus') },
    { value: 'PENDING', label: t('reports.status.PENDING') },
    { value: 'RESOLVED', label: t('reports.status.RESOLVED') },
    { value: 'REJECTED', label: t('reports.status.REJECTED') },
  ];

  const targetTypeOptions = [
    { value: '', label: t('reports.filter.allTargetTypes') },
    { value: 'POST', label: t('reports.targetType.POST') },
    { value: 'COMMENT', label: t('reports.targetType.COMMENT') },
    { value: 'USER', label: t('reports.targetType.USER') },
    { value: 'DECK', label: t('reports.targetType.DECK') },
    { value: 'LESSON', label: t('reports.targetType.LESSON') },
  ];

  const sortOptions = [
    { value: 'createdAt,desc', label: t('reports.filter.sortCreatedDesc') },
    { value: 'createdAt,asc', label: t('reports.filter.sortCreatedAsc') },
    { value: 'resolvedAt,desc', label: t('reports.filter.sortResolvedDesc') },
    { value: 'id,desc', label: t('reports.filter.sortIdDesc') },
  ];

  return (
    <AdminFilterBar
      fromLabel={t('reports.filter.fromDate')}
      fromValue={createdFrom}
      onFromChange={onCreatedFromChange}
      toLabel={t('reports.filter.toDate')}
      toValue={createdTo}
      onToChange={onCreatedToChange}
      sortValue={sort}
      onSortChange={onSortChange}
      sortLabel={t('reports.filter.sortLabel')}
      sortOptions={sortOptions}
      applyLabel={t('reports.filter.apply')}
      resetLabel={t('reports.filter.reset')}
      filterError={filterError}
      onApply={onApply}
      onReset={onReset}
    >
      <AdminFilterSelect
        value={status}
        onChange={onStatusChange}
        ariaLabel={t('reports.columns.status')}
        options={statusOptions}
      />
      <AdminFilterSelect
        value={targetType}
        onChange={onTargetTypeChange}
        ariaLabel={t('reports.columns.targetType')}
        options={targetTypeOptions}
      />
      <AdminFilterNumber
        value={reporterId}
        onChange={onReporterIdChange}
        placeholder={t('reports.filter.reporterIdPlaceholder')}
      />
      <AdminFilterNumber
        value={targetId}
        onChange={onTargetIdChange}
        placeholder={t('reports.filter.targetIdPlaceholder')}
      />
    </AdminFilterBar>
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
