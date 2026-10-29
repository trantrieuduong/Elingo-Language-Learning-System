import { useTranslation } from 'react-i18next';
import './DeckStatsPill.css';

function DeckStatsPill({ topicCount = 0, cardCount = 0, className = '' }) {
  const { t } = useTranslation('learning');

  return (
    <span className={`deck-stats-pill ${className}`.trim()}>
      {t('deckList.cardStats', { topicCount, cardCount })}
    </span>
  );
}

export default DeckStatsPill;
