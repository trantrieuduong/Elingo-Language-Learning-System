import './Filter.css'

/**
 * Filter component — pill buttons cho lọc theo CEFR level và tag/category.
 * Dùng chung cho các trang có danh sách cần lọc.
 *
 * @param {Array} cefrLevels - [{_id, name}] — danh sách CEFR levels
 * @param {Array} tags - [{_id, name}] — danh sách tags/topics
 * @param {string|null} selectedCefrLevelId - ID CEFR đang chọn
 * @param {string|null} selectedTagId - ID tag đang chọn
 * @param {Function} onCefrChange - (id: string|null) => void
 * @param {Function} onTagChange - (id: string|null) => void
 */
function Filter({
  cefrLevels = [],
  tags = [],
  selectedCefrLevelId,
  selectedTagId,
  onCefrChange,
  onTagChange,
}) {
  return (
    <div className="filter-bar">
      {/* CEFR Level filter */}
      {cefrLevels.length > 0 && (
        <div className="filter-group">
          <span className="filter-group__label text-label-md">Cấp độ</span>
          <div className="filter-group__pills">
            <button
              type="button"
              className={`filter-pill ${!selectedCefrLevelId ? 'filter-pill--active' : ''}`}
              onClick={() => onCefrChange(null)}
            >
              Tất cả
            </button>
            {cefrLevels.map((level) => (
              <button
                key={level._id}
                type="button"
                className={`filter-pill ${selectedCefrLevelId === level._id ? 'filter-pill--active' : ''}`}
                onClick={() => onCefrChange(level._id)}
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
          <span className="filter-group__label text-label-md">Chủ đề</span>
          <div className="filter-group__pills">
            <button
              type="button"
              className={`filter-pill ${!selectedTagId ? 'filter-pill--active' : ''}`}
              onClick={() => onTagChange(null)}
            >
              Tất cả
            </button>
            {tags.map((tag) => (
              <button
                key={tag._id}
                type="button"
                className={`filter-pill ${selectedTagId === tag._id ? 'filter-pill--active' : ''}`}
                onClick={() => onTagChange(tag._id)}
              >
                {tag.name}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default Filter
