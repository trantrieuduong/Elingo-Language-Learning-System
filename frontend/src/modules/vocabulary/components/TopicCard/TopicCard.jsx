import { useTranslation } from 'react-i18next';
import './TopicCard.css';

/**
 * @param {Object} topic - Dữ liệu TopicResponse { id, name, slug, order, cardCount, learnedCardCount }
 */
function TopicCard({ topic, isSelected = false, onSelect, onEdit, onDelete }) {
  const { t } = useTranslation('learning');

  return (
    <div className={`topic-card${isSelected ? ' topic-card--active' : ''}`}>
      <button
        type="button"
        className="topic-card__select"
        onClick={() => onSelect?.(topic)}
      >
        <span className="topic-card__name" title={topic.name}>
          {topic.name}
        </span>
      </button>

      {isSelected && (
        <div className="topic-card__actions">
          <button
            type="button"
            className="btn-icon btn-icon--edit topic-card__btn-edit"
            onClick={() => onEdit?.(topic)}
            title={t('topic.editTopic')}
          >
            <span className="material-symbols-outlined">edit</span>
          </button>
          <button
            type="button"
            className="btn-icon btn-icon--delete topic-card__btn-delete"
            onClick={() => onDelete?.(topic)}
            title={t('topic.deleteTopic')}
          >
            <span className="material-symbols-outlined">delete</span>
          </button>
        </div>
      )}
    </div>
  );
}

export default TopicCard;
