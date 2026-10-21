import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import Modal from '../../../../components/Modal/Modal'
import SearchInput from '../../../../components/SearchInput/SearchInput'
import BackButton from '../../../../components/BackButton/BackButton'
import UserCardFormModal from '../../components/UserCardFormModal/UserCardFormModal'
import UserCardItem from '../../components/UserCardItem/UserCardItem'
import { getTopicsApi, getUserDecksApi } from '../../vocabularyApi'
import { getMockCardsForTopic } from '../../userCardMockData'
import './UserCardManagementPage.css'

function UserCardManagementPage({ deckId, topicId, onNavigate }) {
  const { t } = useTranslation(['learning', 'common'])
  const [deck, setDeck] = useState(null)
  const [topic, setTopic] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [contextError, setContextError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [formMode, setFormMode] = useState('create')
  const [selectedCard, setSelectedCard] = useState(null)
  const [cardToDelete, setCardToDelete] = useState(null)

  useEffect(() => {
    const fetchContext = async () => {
      setIsLoading(true)
      setContextError('')
      try {
        const [decksResponse, topicsResponse] = await Promise.all([
          getUserDecksApi({ page: 1, limit: 100 }),
          getTopicsApi({ deckId, page: 1, limit: 100 }),
        ])
        const userDecks = decksResponse?.data?.content ?? []
        const deckTopics = topicsResponse?.data?.content ?? []
        setDeck(userDecks.find((userDeck) => String(userDeck.id) === String(deckId)) ?? null)
        setTopic(deckTopics.find((deckTopic) => String(deckTopic.id) === String(topicId)) ?? null)
      } catch {
        setDeck(null)
        setTopic(null)
        setContextError(t('userCards.contextLoadError'))
      } finally {
        setIsLoading(false)
      }
    }

    fetchContext()
  }, [deckId, topicId, t])

  const cards = useMemo(
    () => getMockCardsForTopic(topic),
    [topic]
  )
  const filteredCards = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLocaleLowerCase()
    if (!normalizedQuery) return cards

    return cards.filter((card) => card.term.toLocaleLowerCase().includes(normalizedQuery))
  }, [cards, searchQuery])

  const handleOpenCreate = () => {
    setFormMode('create')
    setSelectedCard(null)
    setIsFormOpen(true)
  }

  const handleOpenEdit = (card) => {
    setFormMode('edit')
    setSelectedCard(card)
    setIsFormOpen(true)
  }

  const handleCloseForm = () => {
    setIsFormOpen(false)
    setSelectedCard(null)
  }

  const handleCloseDelete = () => {
    setCardToDelete(null)
  }

  if (isLoading) {
    return (
      <main className="user-card-management-page">
        <div className="page-loading">
          <div className="app-loading-spinner" />
        </div>
      </main>
    )
  }

  if (!deck || !topic) {
    return (
      <main className="user-card-management-page">
        <div className="container user-card-management-page__container">
          <div className="topic-list-empty glass-card">
            <h1 className="text-headline-sm" role={contextError ? 'alert' : undefined}>
              {contextError || t('userCards.topicNotFound')}
            </h1>
            <BackButton
              label={t('common:back')}
              onClick={() => onNavigate?.(deck ? `/decks/${deckId}/topics` : '/decks')}
            />
          </div>
        </div>
      </main>
    )
  }

  return (
    <main className="user-card-management-page">
      <div className="container user-card-management-page__container">
        <nav className="user-card-management-page__breadcrumb" aria-label={t('userCards.breadcrumb')}>
          <button type="button" onClick={() => onNavigate?.('/decks')}>
            {t('userDeck.tabs.myDecks')}
          </button>
          <span className="material-symbols-outlined" aria-hidden="true">chevron_right</span>
          <button type="button" onClick={() => onNavigate?.(`/decks/${deckId}/topics`)}>
            {deck.title}
          </button>
          <span className="material-symbols-outlined" aria-hidden="true">chevron_right</span>
          <span>{topic.name}</span>
        </nav>
        <header className="user-card-management-page__header">
          <div className="user-card-management-page__heading">
            <BackButton
              label={t('common:back')}
              onClick={() => onNavigate?.(`/decks/${deckId}/topics`)}
            />
            <div className="user-card-management-page__title-group">
              <span className="user-card-management-page__icon glass-thumb" aria-hidden="true">
                <span className="material-symbols-outlined">style</span>
              </span>
              <div>
                <h1 className="text-headline-md">
                  {t('userCards.topicCardsTitle', { topic: topic.name })}
                </h1>
                <p className="text-body-md">
                  {t('userCards.topicCardsSubtitle', { cards: cards.length })}
                </p>
              </div>
            </div>
          </div>
          <button
            type="button"
            className="btn-primary user-card-management-page__create-btn"
            onClick={handleOpenCreate}
          >
            <span className="material-symbols-outlined" aria-hidden="true">add_circle</span>
            {t('userCards.createCard')}
          </button>
        </header>

        <section className="user-card-management-page__list-section">
          <SearchInput
            id="user-card-search"
            className="user-card-management-page__search"
            placeholder={t('userCards.searchPlaceholder')}
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            onClear={() => setSearchQuery('')}
            clearLabel={t('userCards.clearSearch')}
          />

          <div className="user-card-management-page__grid">
            {filteredCards.map((card) => (
              <UserCardItem
                key={card.id}
                card={card}
                onEdit={handleOpenEdit}
                onDelete={setCardToDelete}
              />
            ))}
            {filteredCards.length === 0 && (
              <div className="user-card-management-page__empty glass-well">
                <span className="material-symbols-outlined" aria-hidden="true">
                  {cards.length === 0 ? 'style' : 'search'}
                </span>
                <p className="text-body-md">
                  {cards.length === 0
                    ? t('userCards.emptyTopicCards')
                    : t('userCards.noSearchResults')}
                </p>
              </div>
            )}
          </div>
        </section>
      </div>

      <UserCardFormModal
        isOpen={isFormOpen}
        mode={formMode}
        card={selectedCard}
        topics={[topic]}
        isTopicLocked
        onClose={handleCloseForm}
      />

      <Modal
        isOpen={!!cardToDelete}
        title={t('userCards.deleteModalTitle')}
        message={t('userCards.deleteModalMessage', { term: cardToDelete?.term ?? '' })}
        confirmText={t('userCards.delete')}
        cancelText={t('userCards.cancel')}
        isDanger
        onConfirm={handleCloseDelete}
        onCancel={handleCloseDelete}
      />
    </main>
  )
}

export default UserCardManagementPage
