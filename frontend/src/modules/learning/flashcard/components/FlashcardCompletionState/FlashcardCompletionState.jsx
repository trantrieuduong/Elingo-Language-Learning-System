import { useTranslation } from 'react-i18next';
import './FlashcardCompletionState.css';

export default function FlashcardCompletionState({ message }) {
  const { t } = useTranslation('learning');

  return (
    <div className="flashcard-completion-state">
      <span
        className="material-symbols-outlined flashcard-completion-state__icon flashcard-completion-state__icon--success"
      >
        task_alt
      </span>
      <h2 className="text-title-md">{t('flashcard.completeTitle')}</h2>
      {message && <p className="text-body-md flashcard-completion-state__message">{message}</p>}
    </div>
  );
}
