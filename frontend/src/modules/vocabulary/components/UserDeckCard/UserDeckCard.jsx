import { useTranslation } from 'react-i18next';
import ProgressBar from '../../../../components/ProgressBar/ProgressBar';
import DeckStatsPill from '../DeckStatsPill/DeckStatsPill';
import './UserDeckCard.css';

function UserDeckCard({ deck, onEdit, onDelete, onToggleStatus, onManageTopics }) {
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

      {/* Ảnh bìa */}
      {deck.coverImageUrl ? (
        <img
          src={deck.coverImageUrl}
          alt={deck.title || t('userDeck.form.coverPreviewAlt')}
          className="user-deck-card__cover"
          loading="lazy"
        />
      ) : (
        <div className="user-deck-card__cover-placeholder">
          <span className="material-symbols-outlined">layers</span>
        </div>
      )}

      {/* Nội dung card */}
      <div className="user-deck-card__body">
        {/* Tags và CEFR levels */}
        {((deck.cefrLevels && deck.cefrLevels.length > 0) ||
          (deck.tags && deck.tags.length > 0)) && (
          <div className="user-deck-card__tags">
            {deck.cefrLevels?.map((cefr) => (
              <span
                key={cefr.id}
                className="badge badge--soft-blue user-deck-card__cefr-tag"
              >
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

        {/* Footer chứa các nút hành động CRUD */}
        <div className="user-deck-card__actions">
          <div className="user-deck-card__action-group">
            {/* Nút chuyển đổi nhanh trạng thái Publish / Draft */}
            <button
              type="button"
              className={`btn-icon user-deck-card__btn-status ${
                isPublished ? 'user-deck-card__btn-status--published' : ''
              }`}
              onClick={() => onToggleStatus?.(deck)}
              title={t(isPublished ? 'userDeck.switchToDraft' : 'userDeck.publishDeck')}
            >
              <span className="material-symbols-outlined">
                {isPublished ? 'unpublished' : 'publish'}
              </span>
            </button>

            {/* Nút sửa */}
            <button
              type="button"
              className="btn-icon btn-icon--edit user-deck-card__btn-edit"
              onClick={() => onEdit?.(deck)}
              title={t('userDeck.editDeck')}
            >
              <span className="material-symbols-outlined">edit</span>
            </button>

            {/* Nút xóa */}
            <button
              type="button"
              className="btn-icon btn-icon--delete user-deck-card__btn-delete"
              onClick={() => onDelete?.(deck)}
              title={t('userDeck.deleteDeck')}
            >
              <span className="material-symbols-outlined">delete</span>
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}

export default UserDeckCard;
