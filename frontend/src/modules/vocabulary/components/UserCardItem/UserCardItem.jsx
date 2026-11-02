import { useTranslation } from 'react-i18next';
import VocabularyFlashCard from '../../../../components/VocabularyFlashCard/VocabularyFlashCard';
import FlashcardStarButton from '../../../../components/FlashcardStarButton/FlashcardStarButton';
import './UserCardItem.css';

function UserCardItem({ card, onEdit, onDelete, onToggleStar, isStarProcessing = false }) {
  const { t } = useTranslation(['learning', 'common']);
  const isStarred = Boolean(card.state?.starred);

  const frontMeta = isStarred ? (
    <span className="user-card-item__star">
      <span className="material-symbols-outlined">star</span>
    </span>
  ) : null;

  return (
    <article className="user-card-item glass-card">
      <VocabularyFlashCard
        card={card}
        compact
        frontMeta={frontMeta}
        className="user-card-item__flip"
      />

      <div className="user-card-item__actions">
        <FlashcardStarButton
          isStarred={isStarred}
          isProcessing={isStarProcessing}
          onToggle={() => onToggleStar?.(card)}
          className="user-card-item__star-toggle"
        />
        <button
          type="button"
          className="btn-icon"
          onClick={() => onEdit?.(card)}
          title={t('common:edit')}
        >
          <span className="material-symbols-outlined">edit</span>
        </button>
        <button
          type="button"
          className="btn-icon user-card-item__delete"
          onClick={() => onDelete?.(card)}
          title={t('common:delete')}
        >
          <span className="material-symbols-outlined">delete</span>
        </button>
      </div>
    </article>
  );
}

export default UserCardItem;
