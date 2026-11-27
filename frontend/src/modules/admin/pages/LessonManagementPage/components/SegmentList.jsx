import React, { useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { AdminEmptyState } from '../../../components/Feedback/AdminFeedback';
import { PlusIcon, TrashIcon } from '../../../components/icons/AdminIcons';
import { formatMs } from '../utils/timeMask';
import styles from './SegmentList.module.css';

export default function SegmentList({
  segments = [],
  selectedKey,
  onSelect,
  onAdd,
  selectMode = false,
  onToggleSelectMode,
  checkedIds,
  onToggleChecked,
  onToggleAll,
  onRequestBulkDelete,
}) {
  const { t } = useTranslation('admin');
  const itemRefs = useRef({});

  useEffect(() => {
    if (selectMode || !selectedKey || selectedKey === 'new') return;
    const el = itemRefs.current[selectedKey];
    if (el && typeof el.scrollIntoView === 'function') {
      el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
  }, [selectedKey, selectMode]);

  const allChecked = segments.length > 0 && checkedIds.size === segments.length;

  return (
    <div className={styles.listCard}>
      {/* Header */}
      <div className={styles.listHeader}>
        <div className={styles.headerTitleGroup}>
          <h2 className={styles.listTitle}>{t('content.lessons.segmentsTitle')}</h2>
          <span className={styles.countBadge}>{segments.length}</span>
        </div>

        <div className={styles.headerActions}>
          {segments.length > 0 && (
            <button
              type="button"
              className={`${styles.selectModeBtn} ${selectMode ? styles.selectModeActive : ''}`}
              onClick={onToggleSelectMode}
            >
              {selectMode
                ? t('content.lessons.exitSelectMode')
                : t('content.lessons.selectModeBtn')}
            </button>
          )}
          <button
            type="button"
            className={styles.addBtn}
            onClick={onAdd}
            disabled={selectMode}
          >
            <PlusIcon size={15} />
            <span>{t('content.lessons.addSegment')}</span>
          </button>
        </div>
      </div>

      {/* Bulk action bar */}
      {selectMode && segments.length > 0 && (
        <div className={styles.bulkBar}>
          <label className={styles.selectAllLabel}>
            <input
              type="checkbox"
              checked={allChecked}
              onChange={onToggleAll}
              className={styles.checkbox}
            />
            <span>{t('content.lessons.selectAll')}</span>
          </label>

          <div className={styles.bulkRight}>
            <span className={styles.selectedCount}>
              {t('content.lessons.selectedCount', { count: checkedIds.size })}
            </span>
            <button
              type="button"
              className={styles.bulkDeleteBtn}
              onClick={onRequestBulkDelete}
              disabled={checkedIds.size === 0}
            >
              <TrashIcon size={14} />
              <span>{t('content.lessons.bulkDeleteBtn')}</span>
            </button>
          </div>
        </div>
      )}

      {/* List Body */}
      {segments.length === 0 ? (
        <div className={styles.emptyWrap}>
          <AdminEmptyState
            title={t('content.lessons.noSegmentsTitle')}
            description={t('content.lessons.noSegmentsDesc')}
          />
          <button type="button" className={styles.addBtn} onClick={onAdd}>
            <PlusIcon size={15} />
            <span>{t('content.lessons.addSegment')}</span>
          </button>
        </div>
      ) : (
        <div className={styles.itemsScroll}>
          {segments.map((seg, index) => {
            const isSelected = !selectMode && selectedKey === seg.id;
            const isChecked = selectMode && checkedIds.has(seg.id);
            return (
              <button
                key={seg.id}
                ref={(el) => {
                  itemRefs.current[seg.id] = el;
                }}
                type="button"
                className={`${styles.segmentItem} ${isSelected ? styles.segmentItemSelected : ''} ${
                  isChecked ? styles.segmentItemChecked : ''
                }`}
                onClick={() => (selectMode ? onToggleChecked(seg.id) : onSelect(seg.id))}
              >
                <div className={styles.itemTop}>
                  <div className={styles.itemLeftMeta}>
                    {selectMode && (
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => {}}
                        tabIndex={-1}
                        aria-label={t('content.lessons.segmentNumber', { index: index + 1 })}
                        className={styles.checkbox}
                      />
                    )}
                    <span className={styles.indexBadge}>
                      {t('content.lessons.segmentNumber', { index: index + 1 })}
                    </span>
                  </div>
                  <span className={styles.timeRange}>
                    {formatMs(seg.startMs)} – {formatMs(seg.endMs)}
                  </span>
                </div>
                <p className={styles.transcriptText}>{seg.transcriptOriginal}</p>
                <p className={styles.translationText}>{seg.translation}</p>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

SegmentList.propTypes = {
  segments: PropTypes.array.isRequired,
  selectedKey: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  onSelect: PropTypes.func.isRequired,
  onAdd: PropTypes.func.isRequired,
  selectMode: PropTypes.bool,
  onToggleSelectMode: PropTypes.func.isRequired,
  checkedIds: PropTypes.instanceOf(Set).isRequired,
  onToggleChecked: PropTypes.func.isRequired,
  onToggleAll: PropTypes.func.isRequired,
  onRequestBulkDelete: PropTypes.func.isRequired,
};
