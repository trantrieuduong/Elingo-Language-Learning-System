import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminFilterBar, {
  AdminFilterNumber,
  AdminFilterSearch,
  AdminFilterSelect,
} from '../../../components/AdminFilterBar/AdminFilterBar';

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

  const actorTypeOptions = [
    { value: '', label: t('postModeration.logs.allActorTypes') },
    { value: 'AI', label: t('postModeration.actorType.AI') },
    { value: 'ADMIN', label: t('postModeration.actorType.ADMIN') },
  ];

  const actionOptions = [
    { value: '', label: t('postModeration.logs.allActions') },
    { value: 'FLAG_FOR_REVIEW', label: t('postModeration.action.FLAG_FOR_REVIEW') },
    { value: 'AUTO_APPROVE', label: t('postModeration.action.AUTO_APPROVE') },
    { value: 'AUTO_REJECT', label: t('postModeration.action.AUTO_REJECT') },
    { value: 'MANUAL_APPROVE', label: t('postModeration.action.MANUAL_APPROVE') },
    { value: 'MANUAL_REJECT', label: t('postModeration.action.MANUAL_REJECT') },
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
      <AdminFilterNumber
        value={postId}
        onChange={onPostIdChange}
        placeholder={t('postModeration.logs.postIdPlaceholder')}
      />
      <AdminFilterSelect
        value={actorType}
        onChange={onActorTypeChange}
        ariaLabel={t('postModeration.logs.columns.actor')}
        options={actorTypeOptions}
      />
      <AdminFilterSelect
        value={action}
        onChange={onActionChange}
        ariaLabel={t('postModeration.logs.columns.action')}
        options={actionOptions}
      />
      <AdminFilterSearch
        value={aiLabel}
        onChange={onAiLabelChange}
        placeholder={t('postModeration.logs.aiLabelPlaceholder')}
      />
    </AdminFilterBar>
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
