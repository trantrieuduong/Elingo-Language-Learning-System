import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import './FilterSidebar.css';

const STATUS_OPTIONS = [
  { value: 'ALL', labelKey: 'community:status.all' },
  { value: 'PUBLISHED', labelKey: 'community:status.published' },
  { value: 'PENDING', labelKey: 'community:status.pending' },
  { value: 'NEEDS_REVIEW', labelKey: 'community:status.needsReview' },
  { value: 'REJECTED', labelKey: 'community:status.rejected' },
];

/**
 * FilterSidebar — Sidebar lọc bài viết theo nhóm hoặc trạng thái.
 *
 * @param {'group'|'status'} filterType - Loại dữ liệu cần lọc
 * @param {string|number} selectedValue - Giá trị đang chọn
 * @param {Function} onSelect - Callback khi chọn một bộ lọc
 * @param {Array} groups - Danh sách nhóm từ API [{id, groupName, description}]
 */
function FilterSidebar({ filterType = 'group', selectedValue = 'ALL', onSelect, groups = [] }) {
  const { t } = useTranslation();
  const [isOpenMobile, setIsOpenMobile] = useState(false);
  const containerRef = useRef(null);

  const isStatusFilter = filterType === 'status';
  const options = isStatusFilter
    ? STATUS_OPTIONS.map((option) => ({
        value: option.value,
        label: t(option.labelKey),
      }))
    : [
        { value: 'ALL', label: t('community:sidebar.allPosts') },
        ...groups.map((group) => ({ value: group.id, label: group.groupName })),
      ];
  const selectedOption =
    options.find((option) => String(option.value) === String(selectedValue)) || options[0];
  const title = t(isStatusFilter ? 'community:status.title' : 'community:postGroups.title');

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpenMobile(false);
      }
    };

    if (isOpenMobile) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpenMobile]);

  const handleSelect = (value) => {
    onSelect?.(value);
    setIsOpenMobile(false);
  };

  return (
    <aside
      ref={containerRef}
      className={`filter-sidebar glass-card ${isOpenMobile ? 'filter-sidebar--mobile-open' : ''}`}
    >
      <button
        type="button"
        className="filter-sidebar__mobile-trigger"
        onClick={() => setIsOpenMobile((prev) => !prev)}
      >
        <span className="filter-sidebar__mobile-current">
          <span className="filter-sidebar__label">{selectedOption?.label}</span>
        </span>

        <span
          className={`material-symbols-outlined filter-sidebar__mobile-arrow ${
            isOpenMobile ? 'filter-sidebar__mobile-arrow--open' : ''
          }`}
        >
          expand_more
        </span>
      </button>

      <div className="filter-sidebar__content">
        <div className="filter-sidebar__header">
          <h2 className="filter-sidebar__title">{title}</h2>
        </div>

        <nav className="filter-sidebar__nav">
          <ul className="filter-sidebar__list">
            {options.map((option) => {
              const isSelected = String(selectedValue) === String(option.value);
              return (
                <li key={option.value} className="filter-sidebar__item">
                  <button
                    type="button"
                    onClick={() => handleSelect(option.value)}
                    className={`filter-sidebar__btn ${
                      isSelected ? 'filter-sidebar__btn--active' : ''
                    }`}
                  >
                    <span className="filter-sidebar__label">{option.label}</span>
                  </button>
                </li>
              );
            })}
          </ul>
        </nav>
      </div>
    </aside>
  );
}

export default FilterSidebar;
