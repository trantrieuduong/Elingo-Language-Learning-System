import { useTranslation } from 'react-i18next';
import './FlashcardStarButton.css';

function FlashcardStarButton({
  isStarred = false,
  isProcessing = false,
  onToggle,
  className = '',
  stopCardInteraction = false,
}) {
  const { t } = useTranslation('learning');

  const handleClick = (event) => {
    if (stopCardInteraction) {
      event.stopPropagation();
      event.preventDefault();
    }
    if (onToggle) onToggle();
  };

  const handlePointerDown = (event) => {
    if (stopCardInteraction) event.stopPropagation();
  };

  return (
    <button
      type="button"
      className={`btn-icon flashcard-star-button ${className} ${
        isStarred ? 'active-star' : ''
      }`.trim()}
      onClick={handleClick}
      onPointerDown={handlePointerDown}
      disabled={isProcessing}
      title={isStarred ? t('flashcard.actions.unstar') : t('flashcard.actions.star')}
    >
      <span
        className="material-symbols-outlined"
        style={isStarred ? { fontVariationSettings: "'FILL' 1" } : {}}
      >
        star
      </span>
    </button>
  );
}

export default FlashcardStarButton;
