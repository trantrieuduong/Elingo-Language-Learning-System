import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import Modal from '../../../../components/Modal/Modal'
import SearchInput from '../../../../components/SearchInput/SearchInput'
import BackButton from '../../../../components/BackButton/BackButton'
import UserCardFormModal from '../../components/UserCardFormModal/UserCardFormModal'
import UserCardItem from '../../components/UserCardItem/UserCardItem'
import {
  getTopicsApi,
  getUserDecksApi,
  getDecksApi,
  getCardsByTopicApi,
  createUserCardApi,
  updateUserCardApi,
  deleteUserCardApi,
} from '../../vocabularyApi'
import './UserCardManagementPage.css'

function UserCardManagementPage({ deckId, topicId, onNavigate }) {
  const { t } = useTranslation(['learning', 'api', 'common'])
  const [deck, setDeck] = useState(null)
  const [topic, setTopic] = useState(null)
  const [cards, setCards] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [contextError, setContextError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [formMode, setFormMode] = useState('create')
  const [selectedCard, setSelectedCard] = useState(null)
  const [cardToDelete, setCardToDelete] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [refreshTrigger, setRefreshTrigger] = useState(0)
  const [toastMessage, setToastMessage] = useState('')

  useEffect(() => {
    if (!toastMessage) return
    const timer = setTimeout(() => setToastMessage(''), 3000)
    return () => clearTimeout(timer)
  }, [toastMessage])

  useEffect(() => {
    let isMounted = true
    const fetchContextAndCards = async () => {
      setIsLoading(true)
      setContextError('')
      try {
        const [userDecksRes, pubDecksRes, topicsRes, cardsRes] = await Promise.allSettled([
          getUserDecksApi({ page: 1, limit: 100 }),
          getDecksApi({ page: 1, limit: 100 }),
          getTopicsApi({ deckId, page: 1, limit: 100 }),
          getCardsByTopicApi(topicId),
        ])

        if (!isMounted) return

        const allDecks = [
          ...(userDecksRes.value?.data?.content || []),
          ...(pubDecksRes.value?.data?.content || []),
        ]
        const matchedDeck = allDecks.find((d) => String(d.id) === String(deckId)) ?? null
        setDeck(matchedDeck)

        const allTopics = topicsRes.value?.data?.content || []
        const matchedTopic = allTopics.find((t) => String(t.id) === String(topicId)) ?? null
        setTopic(matchedTopic)

        if (cardsRes.status === 'fulfilled' && cardsRes.value?.success && Array.isArray(cardsRes.value.data)) {
          setCards(cardsRes.value.data)
        } else {
          setCards([])
        }
      } catch {
        if (!isMounted) return
        setDeck(null)
        setTopic(null)
        setContextError(t('userCards.contextLoadError'))
      } finally {
        if (isMounted) setIsLoading(false)
      }
    }

    fetchContextAndCards()
    return () => {
      isMounted = false
    }
  }, [deckId, topicId, refreshTrigger, t])

  const filteredCards = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase()
    if (!normalizedQuery) return cards

    return cards.filter(
      (card) =>
        card.term?.toLowerCase().includes(normalizedQuery) ||
        card.translation?.toLowerCase().includes(normalizedQuery)
    )
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
    if (isSubmitting) return
    setIsFormOpen(false)
    setSelectedCard(null)
  }

  const handleCloseDelete = () => {
    if (isDeleting) return
    setCardToDelete(null)
  }

  const handleFormSubmit = async (formData) => {
    setIsSubmitting(true)
    try {
      let res
      if (formMode === 'create') {
        res = await createUserCardApi({ ...formData, topicId })
      } else {
        res = await updateUserCardApi(selectedCard.id, formData)
      }

      if (res?.success) {
        setIsFormOpen(false)
        setSelectedCard(null)
        setToastMessage(formMode === 'create' ? t('userCards.createSuccess') : t('userCards.updateSuccess'))
        setRefreshTrigger((prev) => prev + 1)
        return res
      }
      return res
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleConfirmDelete = async () => {
    if (!cardToDelete) return
    setIsDeleting(true)
    try {
      const res = await deleteUserCardApi(cardToDelete.id)
      if (res?.success) {
        setCardToDelete(null)
        setToastMessage(t('userCards.deleteSuccess'))
        setRefreshTrigger((prev) => prev + 1)
      } else {
        const errorCode = res?.code
        const msg = (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) || res?.message || t('common:error')
        setToastMessage(msg)
      }
    } catch (err) {
      console.error('Lỗi khi xóa thẻ:', err)
      const errorCode = err.response?.data?.code
      const msg = (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) || err.response?.data?.message || err.message || t('common:error')
      setToastMessage(msg)
    } finally {
      setIsDeleting(false)
    }
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
        {toastMessage && (
          <div className="topic-list-success-toast" role="status" aria-live="polite">
            <span className="material-symbols-outlined" aria-hidden="true">
              check_circle
            </span>
            {toastMessage}
          </div>
        )}

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
            clearLabel={t('common:clearSearch')}
          />

          <div className="user-card-management-page__grid">
            {filteredCards.map((card) => (
              <UserCardItem
                key={card.id}
                card={{ ...card, topicName: topic.name }}
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
        onSubmit={handleFormSubmit}
        onClose={handleCloseForm}
        isSubmitting={isSubmitting}
      />

      <Modal
        isOpen={!!cardToDelete}
        title={t('userCards.deleteModalTitle')}
        message={t('userCards.deleteModalMessage', { term: cardToDelete?.term ?? '' })}
        confirmText={t('common:delete')}
        cancelText={t('common:cancel')}
        isDanger
        isLoading={isDeleting}
        onConfirm={handleConfirmDelete}
        onCancel={handleCloseDelete}
      />
    </main>
  )
}

export default UserCardManagementPage
