import { useTranslation } from 'react-i18next';
import SearchInput from '../SearchInput/SearchInput';
import Filter from '../Filter/Filter';
import './FilterSection.css';

/**
 * FilterSection — Component vùng lọc dùng chung cho các trang danh sách
 *
 * @param {string} searchQuery - Giá trị tìm kiếm hiện tại
 * @param {Function} onSearchChange - (e) => void - Callback khi giá trị tìm kiếm thay đổi
 * @param {string} searchPlaceholder - Placeholder cho ô tìm kiếm
 * @param {Array} cefrLevels - [{_id, name}] — danh sách CEFR levels
 * @param {Array} tags - [{_id, name}] — danh sách tags
 * @param {Array} selectedCefrLevelIds - Các ID CEFR đang chọn
 * @param {Array} selectedTagIds - Các ID tag đang chọn
 * @param {boolean} hasActiveFilters - Có filter đang active hay không
 * @param {Function} onClearFilters - Callback xóa tất cả filters
 * @param {React.ReactNode} extraControls - Các control bổ sung (như status pills)
 */
function FilterSection({
  searchQuery,
  onSearchChange,
  searchPlaceholder,
  cefrLevels = [],
  tags = [],
  selectedCefrLevelIds = [],
  selectedTagIds = [],
  onCefrChange,
  onTagChange,
  hasActiveFilters = false,
  onClearFilters,
  extraControls = null,
  showCategoryFilters = true,
}) {
  const { t } = useTranslation(['common']);

  const handleClearSearch = () => {
    // Tạo synthetic event để tương thích với onSearchChange
    const syntheticEvent = {
      target: { value: '' },
    };
    onSearchChange(syntheticEvent);
  };

  return (
    <section className="filter-section glass-well">
      <div className="filter-section__top">
        <SearchInput
          id="filter-section-search"
          className="filter-section__search-input"
          placeholder={searchPlaceholder}
          value={searchQuery}
          onChange={onSearchChange}
          onClear={handleClearSearch}
          clearLabel={t('clearSearch')}
        />

        {/* Extra controls (như status pills cho My Decks) */}
        {extraControls}

        {/* Nút xóa filter */}
        {hasActiveFilters && (
          <button type="button" className="btn-clear-filter" onClick={onClearFilters}>
            <span className="material-symbols-outlined">filter_alt_off</span>
            {t('filter.clearFilters')}
          </button>
        )}
      </div>

      {/* Component Filter với CEFR và Tags */}
      {showCategoryFilters && (
        <Filter
          cefrLevels={cefrLevels}
          tags={tags}
          selectedCefrLevelIds={selectedCefrLevelIds}
          selectedTagIds={selectedTagIds}
          onCefrChange={onCefrChange}
          onTagChange={onTagChange}
        />
      )}
    </section>
  );
}

export default FilterSection;
