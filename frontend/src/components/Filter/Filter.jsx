import { useTranslation } from 'react-i18next';
import './Filter.css';

function Filter({
  cefrLevels = [],
  tags = [],
  selectedCefrLevelIds = [],
  selectedTagIds = [],
  onCefrChange,
  onTagChange,
}) {
  const { t } = useTranslation();

  const handleToggle = (selectedIds, id, onChange) => {
    onChange(
      selectedIds.includes(id)
        ? selectedIds.filter((selectedValue) => selectedValue !== id)
        : [...selectedIds, id]
    );
  };

  return (
    <div className="filter-bar">
      {/* CEFR Level filter */}
      {cefrLevels.length > 0 && (
        <div className="filter-group">
          <span className="filter-group__label text-label-md">{t('filter.level')}</span>
          <div className="filter-group__pills">
            <button
              type="button"
              className={`filter-pill ${selectedCefrLevelIds.length === 0 ? 'filter-pill--active' : ''}`}
              onClick={() => onCefrChange([])}
            >
              {t('all')}
            </button>
            {cefrLevels.map((level) => (
              <button
                key={level._id}
                type="button"
                className={`filter-pill ${selectedCefrLevelIds.includes(level._id) ? 'filter-pill--active' : ''}`}
                onClick={() => handleToggle(selectedCefrLevelIds, level._id, onCefrChange)}
              >
                {level.name}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Tag / Topic filter */}
      {tags.length > 0 && (
        <div className="filter-group">
          <span className="filter-group__label text-label-md">{t('filter.topic')}</span>
          <div className="filter-group__pills">
            <button
              type="button"
              className={`filter-pill ${selectedTagIds.length === 0 ? 'filter-pill--active' : ''}`}
              onClick={() => onTagChange([])}
            >
              {t('all')}
            </button>
            {tags.map((tag) => (
              <button
                key={tag._id}
                type="button"
                className={`filter-pill ${selectedTagIds.includes(tag._id) ? 'filter-pill--active' : ''}`}
                onClick={() => handleToggle(selectedTagIds, tag._id, onTagChange)}
              >
                {tag.name}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default Filter;
