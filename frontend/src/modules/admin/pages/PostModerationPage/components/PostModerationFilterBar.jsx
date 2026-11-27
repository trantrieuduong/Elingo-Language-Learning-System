import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminFilterBar, {
  AdminFilterNumber,
  AdminFilterSearch,
  AdminFilterSelect,
} from '../../../components/AdminFilterBar/AdminFilterBar';

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

  const statusOptions = [
    { value: '', label: t('postModeration.filter.allStatus') },
    { value: 'NEEDS_REVIEW', label: t('postModeration.status.NEEDS_REVIEW') },
    { value: 'PENDING', label: t('postModeration.status.PENDING') },
    { value: 'PUBLISHED', label: t('postModeration.status.PUBLISHED') },
    { value: 'REJECTED', label: t('postModeration.status.REJECTED') },
    { value: 'DELETED', label: t('postModeration.status.DELETED') },
  ];

  const groupOptions = [
    { value: '', label: t('postModeration.filter.allGroups') },
    ...groups.map((g) => ({ value: String(g.id), label: g.groupName || g.name })),
  ];

  const sortOptions = [
    { value: 'createdAt,desc', label: t('postModeration.filter.sortCreatedDesc') },
    { value: 'createdAt,asc', label: t('postModeration.filter.sortCreatedAsc') },
    { value: 'id,desc', label: t('postModeration.filter.sortIdDesc') },
  ];

  return (
    <AdminFilterBar
      fromLabel={t('postModeration.filter.fromDate')}
      fromValue={createdFrom}
      onFromChange={onCreatedFromChange}
      toLabel={t('postModeration.filter.toDate')}
      toValue={createdTo}
      onToChange={onCreatedToChange}
      sortValue={sort}
      onSortChange={onSortChange}
      sortLabel={t('postModeration.filter.sortLabel')}
      sortOptions={sortOptions}
      applyLabel={t('postModeration.filter.apply')}
      resetLabel={t('postModeration.filter.reset')}
      filterError={filterError}
      onApply={onApply}
      onReset={onReset}
    >
      <AdminFilterSearch
        value={keyword}
        onChange={onKeywordChange}
        placeholder={t('postModeration.filter.keywordPlaceholder')}
      />
      <AdminFilterSelect
        value={status}
        onChange={onStatusChange}
        ariaLabel={t('postModeration.columns.status')}
        options={statusOptions}
      />
      <AdminFilterSelect
        value={groupId}
        onChange={onGroupIdChange}
        ariaLabel={t('postModeration.columns.group')}
        options={groupOptions}
      />
      <AdminFilterNumber
        value={userId}
        onChange={onUserIdChange}
        placeholder={t('postModeration.filter.userIdPlaceholder')}
      />
    </AdminFilterBar>
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
