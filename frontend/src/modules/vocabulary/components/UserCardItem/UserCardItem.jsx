import { useTranslation } from 'react-i18next'
import VocabularyFlashCard from '../../../../components/VocabularyFlashCard/VocabularyFlashCard'
import './UserCardItem.css'

function UserCardItem({ card, onEdit, onDelete }) {
  const { t } = useTranslation('learning')

  const frontMeta = (
    <>
      <span className="badge badge--default">{card.topicName}</span>
      <span className="user-card-item__meta-trailing">
        <span className="user-card-item__order text-body-sm">
          {t('userCards.cardOrder', { order: card.order })}
        </span>
        {card.flagsStarred && (
          <span className="user-card-item__star" aria-label={t('userCards.starred')}>
            <span className="material-symbols-outlined" aria-hidden="true">
              star
            </span>
          </span>
        )}
      </span>
    </>
  )

  return (
    <article className="user-card-item glass-card">
      <VocabularyFlashCard
        card={card}
        compact
        frontMeta={frontMeta}
        className="user-card-item__flip"
      />

      <div className="user-card-item__actions">
        <button
          type="button"
          className="btn-icon"
          onClick={() => onEdit?.(card)}
          aria-label={t('userCards.editCard', { term: card.term })}
          title={t('userCards.edit')}
        >
          <span className="material-symbols-outlined" aria-hidden="true">
            edit
          </span>
        </button>
        <button
          type="button"
          className="btn-icon user-card-item__delete"
          onClick={() => onDelete?.(card)}
          aria-label={t('userCards.deleteCard', { term: card.term })}
          title={t('userCards.delete')}
        >
          <span className="material-symbols-outlined" aria-hidden="true">
            delete
          </span>
        </button>
      </div>
    </article>
  )
}

export default UserCardItem
