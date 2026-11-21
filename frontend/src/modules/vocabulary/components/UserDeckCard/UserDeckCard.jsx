import { useTranslation } from 'react-i18next';
import ProgressBar from '../../../../components/ProgressBar/ProgressBar';
import DeckStatsPill from '../DeckStatsPill/DeckStatsPill';
import OptionsMenu from '../../../../components/OptionsMenu/OptionsMenu';
import './UserDeckCard.css';

function UserDeckCard({ deck, onOptionsClick, onManageTopics }) {
  const { t } = useTranslation('learning');
  const isPublished = deck.status === 'PUBLISHED';
  const isDraft = deck.status === 'DRAFT';
  const totalCards = deck.cardCount ?? 0;
  const learnedCards = deck.learnedCardCount ?? 0;

  return (
    <article className="user-deck-card glass-card">
      {/* Badge trạng thái ở góc trên bên trái */}
      <div className="user-deck-card__status-wrap">
        {isPublished && (
          <span className="badge badge--mint user-deck-card__status-badge">
            <span className="material-symbols-outlined">public</span>
            {t('userDeck.statusPublished')}
          </span>
        )}
        {isDraft && (
          <span className="badge badge--lavender user-deck-card__status-badge">
            <span className="material-symbols-outlined">edit_note</span>
            {t('userDeck.statusDraft')}
          </span>
        )}
        {!isPublished && !isDraft && (
          <span className="badge badge--pink user-deck-card__status-badge">
            <span className="material-symbols-outlined">archive</span>
            {t('userDeck.statusArchived')}
          </span>
        )}
      </div>

      {onOptionsClick && (
        <OptionsMenu
          className="user-deck-card__options-btn"
          onClick={(e) => {
            e.stopPropagation();
            onOptionsClick(deck);
          }}
        />
      )}

      {onManageTopics && (
        <button
          type="button"
          className="user-deck-card__open-topics"
          onClick={() => onManageTopics(deck)}
        />
      )}

      {/* Số lượng chủ đề & từ ở góc trên bên phải */}
      <DeckStatsPill
        className="user-deck-card__stats-pill"
        topicCount={deck.topicCount ?? 0}
        cardCount={totalCards}
      />

      {/* Nội dung card */}
      <div className="user-deck-card__body">
        {/* Tags và CEFR levels */}
        {((deck.cefrLevels && deck.cefrLevels.length > 0) ||
          (deck.tags && deck.tags.length > 0)) && (
          <div className="user-deck-card__tags">
            {deck.cefrLevels?.map((cefr) => (
              <span key={cefr.id} className="badge badge--soft-blue user-deck-card__cefr-tag">
                {cefr.label}
              </span>
            ))}
            {deck.tags?.map((tag) => (
              <span key={tag.id} className="badge badge--default">
                {tag.label}
              </span>
            ))}
          </div>
        )}

        <div className="user-deck-card__header">
          <h3 className="user-deck-card__title text-title-md" title={deck.title}>
            {deck.title}
          </h3>
        </div>

        <p className="user-deck-card__description text-body-sm">
          {deck.description || t('deckList.noDescription')}
        </p>

        {/* Thanh tiến độ học */}
        {totalCards > 0 && (
          <div className="user-deck-card__progress">
            <ProgressBar title={t('deckList.progress')} current={learnedCards} total={totalCards} />
          </div>
        )}
      </div>
    </article>
  );
}

export default UserDeckCard;
