import { useTranslation } from 'react-i18next';
import '../../../../components/Filter/Filter.css';
import './TopicCard.css';

/**
 * TopicCard — Card chọn chủ đề, chỉ hiển thị hành động quản lý khi được chọn.
 * Mô phỏng TopicResponse: { id, name, slug, order, cardCount, unlearnedCardCount }
 *
 * @param {Object} topic - Dữ liệu TopicResponse
 */
function TopicCard({ topic, isSelected = false, onSelect, onEdit, onDelete }) {
  const { t } = useTranslation('learning');

  return (
    <div className="topic-card">
      <button
        type="button"
        className={`filter-pill topic-card__select${isSelected ? ' filter-pill--active' : ''}`}
        onClick={() => onSelect?.(topic)}
        aria-label={t('topic.selectTopicFor', { name: topic.name })}
        aria-pressed={isSelected}
      >
        <span className="topic-card__name" title={topic.name}>
          {topic.name}
        </span>
      </button>

      {isSelected && (
        <div className="topic-card__actions">
          <button
            type="button"
            className="btn-icon topic-card__btn-edit"
            onClick={() => onEdit?.(topic)}
            title={t('topic.editTopic')}
            aria-label={t('topic.editTopic')}
          >
            <span className="material-symbols-outlined">edit</span>
          </button>
          <button
            type="button"
            className="btn-icon topic-card__btn-delete"
            onClick={() => onDelete?.(topic)}
            title={t('topic.deleteTopic')}
            aria-label={t('topic.deleteTopic')}
          >
            <span className="material-symbols-outlined">delete</span>
          </button>
        </div>
      )}
    </div>
  );
}

export default TopicCard;
