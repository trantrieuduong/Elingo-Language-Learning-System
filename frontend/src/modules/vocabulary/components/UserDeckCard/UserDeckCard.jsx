import { useTranslation } from 'react-i18next'
import ProgressBar from '../../../../components/ProgressBar/ProgressBar'
import './UserDeckCard.css'

/**
 * UserDeckCard — Card hiển thị bộ từ vựng do người dùng tự tạo.
 * Hỗ trợ hiển thị trạng thái DRAFT / PUBLISHED / ARCHIVED và các hành động CRUD.
 *
 * @param {Object} deck - Dữ liệu DeckResponse
 * @param {Function} onEdit - Callback khi nhấn Sửa
 * @param {Function} onDelete - Callback khi nhấn Xóa
 * @param {Function} onToggleStatus - Callback khi chuyển đổi DRAFT <-> PUBLISHED
 * @param {Function} onStudy - Callback khi bấm Học
 * @param {Function} onManageTopics - Callback mở danh sách chủ đề
 */
function UserDeckCard({ deck, onEdit, onDelete, onToggleStatus, onStudy, onManageTopics }) {
  const { t } = useTranslation('learning')
  const isPublished = deck.status === 'PUBLISHED'
  const isDraft = deck.status === 'DRAFT'
  const totalCards = deck.cardCount ?? 0
  const learnedCards = deck.learnedCardCount ?? 0

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
          aria-label={`${t('userDeck.manageTopics')}: ${deck.title}`}
        />
      )}

      {/* Số lượng chủ đề & từ ở góc trên bên phải */}
      <span className="user-deck-card__stats-pill">
        {t('deckList.cardStats', {
          topicCount: deck.topicCount ?? 0,
          cardCount: totalCards,
        })}
      </span>

      {/* Ảnh bìa */}
      {deck.coverImageUrl ? (
        <img
          src={deck.coverImageUrl}
          alt={deck.title || 'Deck cover'}
          className="user-deck-card__cover"
          loading="lazy"
        />
      ) : (
        <div className="user-deck-card__cover-placeholder">
          <span className="material-symbols-outlined">style</span>
        </div>
      )}

      {/* Nội dung card */}
      <div className="user-deck-card__body">
        {/* Tags và CEFR levels */}
        {((deck.cefrLevels && deck.cefrLevels.length > 0) || (deck.tags && deck.tags.length > 0)) && (
          <div className="user-deck-card__tags">
            {deck.cefrLevels?.map((cefr) => (
              <span key={cefr.id || cefr.code} className="badge badge--soft-blue user-deck-card__cefr-tag" style={{ fontWeight: 'bold' }}>
                {cefr.code || cefr.label}
              </span>
            ))}
            {deck.tags?.map((tag) => (
              <span key={tag.id || tag.code} className="badge badge--default">
                {tag.label || tag.code}
              </span>
            ))}
          </div>
        )}

        <div className="user-deck-card__header">
          <h3 className="user-deck-card__title text-title-md" title={deck.title}>
            {deck.title}
          </h3>
          <span className="user-deck-card__slug">/{deck.slug}</span>
        </div>

        <p className="user-deck-card__description text-body-sm">
          {deck.description || t('deckList.noDescription')}
        </p>

        {/* Thanh tiến độ học */}
        {totalCards > 0 && (
          <div className="user-deck-card__progress">
            <ProgressBar
              title={t('deckList.progress')}
              current={learnedCards}
              total={totalCards}
            />
          </div>
        )}

        {/* Footer chứa các nút hành động CRUD */}
        <div className="user-deck-card__actions">
          {/* Nút học flashcard */}
          <button
            type="button"
            className="btn-primary user-deck-card__btn-study"
            onClick={() => onStudy?.(deck)}
            title={t('userDeck.studyDeck')}
          >
            <span className="material-symbols-outlined">play_arrow</span>
            {t('userDeck.studyNow')}
          </button>

          <div className="user-deck-card__action-group">
            {/* Nút chuyển đổi nhanh trạng thái Publish / Draft */}
            <button
              type="button"
              className={`btn-icon user-deck-card__btn-status ${
                isPublished ? 'user-deck-card__btn-status--published' : ''
              }`}
              onClick={() => onToggleStatus?.(deck)}
              title={t(isPublished ? 'userDeck.switchToDraft' : 'userDeck.publishDeck')}
              aria-label={t(isPublished ? 'userDeck.switchToDraft' : 'userDeck.publishDeck')}
            >
              <span className="material-symbols-outlined">
                {isPublished ? 'unpublished' : 'publish'}
              </span>
            </button>

            {/* Nút sửa */}
            <button
              type="button"
              className="btn-icon user-deck-card__btn-edit"
              onClick={() => onEdit?.(deck)}
              title={t('userDeck.editDeck')}
              aria-label={t('userDeck.editDeck')}
            >
              <span className="material-symbols-outlined">edit</span>
            </button>

            {/* Nút xóa */}
            <button
              type="button"
              className="btn-icon user-deck-card__btn-delete"
              onClick={() => onDelete?.(deck)}
              title={t('userDeck.deleteDeck')}
              aria-label={t('userDeck.deleteDeck')}
            >
              <span className="material-symbols-outlined">delete</span>
            </button>
          </div>
        </div>
      </div>
    </article>
  )
}

export default UserDeckCard
