import { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import {
  getDeckByIdApi,
  getTopicsApi,
  createTopicApi,
  updateTopicApi,
  deleteTopicApi,
  updateUserDeckApi,
  getCardsByTopicApi,
  createUserCardApi,
  updateUserCardApi,
  deleteUserCardApi,
} from '../../vocabularyApi';
import { toggleStarApi } from '../../../learning/flashcard/flashcardApi';
import ConfirmModal from '../../../../components/ConfirmModal/ConfirmModal';
import BackButton from '../../../../components/BackButton/BackButton';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import Toast from '../../../../components/Toast/Toast';
import TopicCard from '../../components/TopicCard/TopicCard';
import TopicFormModal from '../../components/TopicFormModal/TopicFormModal';
import DeckFormModal from '../../components/DeckFormModal/DeckFormModal';
import UserCardFormModal from '../../components/UserCardFormModal/UserCardFormModal';
import UserCardItem from '../../components/UserCardItem/UserCardItem';
import './TopicListPage.css';

const DECK_LIST_MY_DECKS_URL = '/decks?tab=my-decks';

const getApiErrorMessage = (error, fallback, t) => {
  const errorData = error?.response?.data ?? error;
  const errorCode = errorData?.code;
  return (
    (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
    errorData?.message ||
    error?.message ||
    fallback
  );
};

const getStarErrorMessage = (errorData, t) => {
  const errorCode = errorData?.code;
  return (
    (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
    errorData?.message ||
    t('api:common.UNKNOWN_ERROR')
  );
};

const updateCardStarredState = (currentCards, cardId, nextStarredState) =>
  currentCards.map((currentCard) =>
    String(currentCard.id) === String(cardId)
      ? {
          ...currentCard,
          state: {
            ...currentCard.state,
            starred: nextStarredState,
          },
        }
      : currentCard
  );

function TopicListHeader({
  deck,
  topicsCount,
  onNavigate,
  onEditDeck,
  onCreateTopic,
  t,
}) {
  return (
    <header className="topic-list-page__header">
      <div className="topic-list-page__heading">
        <BackButton
          label={t('common:back')}
          onClick={() => onNavigate?.(DECK_LIST_MY_DECKS_URL)}
        />
        <div className="topic-list-page__title-group">
          <div className="topic-list-page__title-row">
            <h1 className="text-headline-md topic-list-page__title">{deck.title}</h1>
            <button
              type="button"
              className="btn-icon topic-list-page__edit-deck-btn"
              title={t('userDeck.editDeck')}
              onClick={onEditDeck}
            >
              <span className="material-symbols-outlined">edit</span>
            </button>
          </div>
          <div className="topic-list-page__stats">
            <span>{t('userCards.deckCardCount', { count: deck.cardCount })}</span>
            <span>{t('userCards.deckTopicCount', { count: topicsCount })}</span>
            <button
              type="button"
              className="btn-primary topic-list-page__learn-btn"
              onClick={() =>
                onNavigate?.(`/decks/${deck.slug}`, {
                  returnTo: `/decks/${deck.id}/topics?tab=my-decks`,
                })
              }
            >
              <span className="material-symbols-outlined">play_arrow</span>
              {t('userDeck.studyNow')}
            </button>
          </div>
        </div>
      </div>
      <button
        type="button"
        id="topic-create-btn"
        className="btn-primary topic-list-page__create-btn"
        onClick={onCreateTopic}
      >
        <span className="material-symbols-outlined">add</span>
        {t('topic.createBtn')}
      </button>
    </header>
  );
}

function TopicsSection({
  loading,
  error,
  topics,
  selectedTopic,
  onRetry,
  onEdit,
  onDelete,
  onSelect,
  t,
}) {
  if (loading) {
    return (
      <section className="topic-list-page__topics-section">
        <PageLoading />
      </section>
    );
  }
  if (error) {
    return (
      <section className="topic-list-page__topics-section">
        <EmptyState
          icon="error"
          isError
          description={error}
          actionLabel={t('common:retry')}
          onAction={onRetry}
        />
      </section>
    );
  }
  if (topics.length === 0) {
    return (
      <section className="topic-list-page__topics-section">
        <EmptyState
          icon="category"
          title={t('topic.emptyTitle')}
          description={t('topic.emptyDesc')}
        />
      </section>
    );
  }
  return (
    <section className="topic-list-page__topics-section">
      <div className="topic-list-grid">
        {topics.map((topic) => (
          <TopicCard
            key={topic.id}
            topic={topic}
            isSelected={String(topic.id) === String(selectedTopic?.id)}
            onEdit={onEdit}
            onDelete={onDelete}
            onSelect={onSelect}
          />
        ))}
      </div>
    </section>
  );
}

function CardsSection({
  selectedTopic,
  topicCards,
  isLoadingCards,
  cardsLoadError,
  isTogglingCardStar,
  onOpenCreateCard,
  onRetryCards,
  onEditCard,
  onDeleteCard,
  onToggleCardStar,
  t,
}) {
  return (
    <section className="topic-list-page__cards-section">
      <div className="topic-list-page__cards-header">
        <div>
          <h2 className="text-headline-sm">
            {t('userCards.cardsInTopic', { topic: selectedTopic.name })}
          </h2>
          <p className="text-body-sm">
            {t('userCards.topicCardsSubtitle', { cards: topicCards.length })}
          </p>
        </div>
        <div className="topic-list-page__cards-actions">
          <button type="button" className="btn-secondary">
            <span className="material-symbols-outlined">upload_file</span>
            {t('userCards.importVocabulary')}
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={() => onOpenCreateCard(selectedTopic)}
          >
            <span className="material-symbols-outlined">add</span>
            {t('userCards.createCard')}
          </button>
        </div>
      </div>

      {isLoadingCards ? (
        <PageLoading />
      ) : cardsLoadError ? (
        <EmptyState
          icon="error"
          isError
          description={cardsLoadError}
          actionLabel={t('common:retry')}
          onAction={onRetryCards}
        />
      ) : (
        <div
          className={`topic-list-page__cards-grid${topicCards.length === 0 ? ' topic-list-page__cards-grid--empty' : ''}`}
        >
          {topicCards.map((card) => (
            <UserCardItem
              key={card.id}
              card={card}
              onEdit={onEditCard}
              onDelete={onDeleteCard}
              onToggleStar={onToggleCardStar}
              isStarProcessing={isTogglingCardStar}
            />
          ))}
          {topicCards.length === 0 && (
            <EmptyState icon="style" description={t('userCards.emptyTopicCards')} />
          )}
        </div>
      )}
    </section>
  );
}

function TopicListPage({ deckId, onNavigate }) {
  const { t } = useTranslation(['learning', 'api', 'common']);
  const translationRef = useRef(t);
  useEffect(() => {
    translationRef.current = t;
  }, [t]);

  // States dữ liệu
  const [deck, setDeck] = useState(null);
  const [isLoadingDeck, setIsLoadingDeck] = useState(true);
  const [deckLoadError, setDeckLoadError] = useState('');
  const [topics, setTopics] = useState([]);
  const [selectedTopicId, setSelectedTopicId] = useState(null);
  const [isLoadingTopics, setIsLoadingTopics] = useState(true);
  const [topicsLoadError, setTopicsLoadError] = useState(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // States danh sách thẻ của chủ đề được chọn
  const [topicCards, setTopicCards] = useState([]);
  const [loadedCardsRequestKey, setLoadedCardsRequestKey] = useState(null);
  const [isLoadingCards, setIsLoadingCards] = useState(false);
  const [cardsLoadError, setCardsLoadError] = useState('');
  const [cardRefreshTrigger, setCardRefreshTrigger] = useState(0);

  // States modal tạo / sửa topic
  const [isTopicFormOpen, setIsTopicFormOpen] = useState(false);
  const [topicFormMode, setTopicFormMode] = useState('create'); // 'create' | 'edit'
  const [editingTopic, setEditingTopic] = useState(null);
  const [isSubmittingTopic, setIsSubmittingTopic] = useState(false);

  // States modal xóa topic
  const [deletingTopic, setDeletingTopic] = useState(null);
  const [isDeletingTopic, setIsDeletingTopic] = useState(false);

  // States thông báo thành công
  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState('success');
  const handleToastDismiss = useCallback(() => setToastMessage(''), []);
  const showToast = (message, variant = 'success') => {
    setToastVariant(variant);
    setToastMessage(message);
  };

  // States modal sửa deck
  const [isDeckFormOpen, setIsDeckFormOpen] = useState(false);
  const [isSubmittingDeck, setIsSubmittingDeck] = useState(false);

  // States modal tạo / sửa card
  const [isCardFormOpen, setIsCardFormOpen] = useState(false);
  const [cardToEdit, setCardToEdit] = useState(null);
  const [deletingCard, setDeletingCard] = useState(null);
  const [isSubmittingCard, setIsSubmittingCard] = useState(false);
  const [isDeletingCard, setIsDeletingCard] = useState(false);
  const [isTogglingCardStar, setIsTogglingCardStar] = useState(false);

  const selectedTopic =
    topics.find((topic) => String(topic.id) === String(selectedTopicId)) ?? null;

  useEffect(() => {
    let isActive = true;

    const fetchDeck = async () => {
      setIsLoadingDeck(true);
      setDeckLoadError('');
      try {
        const response = await getDeckByIdApi(deckId);
        if (!isActive) return;

        if (!response?.success || !response.data) {
          setDeck(null);
          setDeckLoadError(translationRef.current('common:loadError'));
          return;
        }
        setDeck(response.data);
      } catch {
        if (!isActive) return;
        setDeck(null);
        setDeckLoadError(translationRef.current('common:loadError'));
      } finally {
        if (isActive) setIsLoadingDeck(false);
      }
    };

    fetchDeck();

    return () => {
      isActive = false;
    };
  }, [deckId, refreshTrigger]);

  useEffect(() => {
    let isActive = true;

    const fetchTopics = async () => {
      setIsLoadingTopics(true);
      setTopicsLoadError(null);
      try {
        const res = await getTopicsApi(deckId);
        if (!isActive) return;

        if (res?.success && res.data) {
          const fetchedTopics = res.data;
          setTopics(fetchedTopics);
          setSelectedTopicId((currentId) =>
            fetchedTopics.some((topic) => topic.id === currentId)
              ? currentId
              : (fetchedTopics[0]?.id ?? null)
          );
        } else {
          setTopicsLoadError(translationRef.current('common:loadError'));
        }
      } catch {
        if (!isActive) return;
        setTopicsLoadError(translationRef.current('common:loadError'));
      } finally {
        if (isActive) setIsLoadingTopics(false);
      }
    };
    fetchTopics();

    return () => {
      isActive = false;
    };
  }, [deckId, refreshTrigger]);

  // ── Fetch cards của selected topic từ BE ──
  useEffect(() => {
    let isCurrentRequest = true;

    if (!selectedTopicId) {
      return () => {
        isCurrentRequest = false;
      };
    }

    const requestKey = `${selectedTopicId}:${cardRefreshTrigger}`;
    const fetchCards = async () => {
      setIsLoadingCards(true);
      setCardsLoadError('');
      try {
        const res = await getCardsByTopicApi(selectedTopicId);
        if (res?.success && Array.isArray(res.data)) {
          if (isCurrentRequest) {
            setTopicCards(res.data);
            setLoadedCardsRequestKey(requestKey);
          }
        } else {
          if (isCurrentRequest) {
            setTopicCards([]);
            setLoadedCardsRequestKey(requestKey);
            setCardsLoadError(translationRef.current('common:loadError'));
          }
        }
      } catch {
        if (isCurrentRequest) {
          setTopicCards([]);
          setLoadedCardsRequestKey(requestKey);
          setCardsLoadError(translationRef.current('common:loadError'));
        }
      } finally {
        if (isCurrentRequest) {
          setIsLoadingCards(false);
        }
      }
    };

    fetchCards();
    return () => {
      isCurrentRequest = false;
    };
  }, [selectedTopicId, cardRefreshTrigger]);

  const currentCardsRequestKey = selectedTopicId
    ? `${selectedTopicId}:${cardRefreshTrigger}`
    : null;

  const handleOpenCreateTopicModal = () => {
    setTopicFormMode('create');
    setEditingTopic(null);
    setIsTopicFormOpen(true);
  };

  const handleOpenEditTopicModal = (topic) => {
    setTopicFormMode('edit');
    setEditingTopic(topic);
    setIsTopicFormOpen(true);
  };

  const handleSelectTopic = (topic) => {
    setSelectedTopicId(topic.id);
  };

  const handleOpenCreateCard = (topic) => {
    setSelectedTopicId(topic.id);
    setCardToEdit(null);
    setIsCardFormOpen(true);
  };

  const handleOpenEditCard = (card) => {
    setCardToEdit(card);
    setIsCardFormOpen(true);
  };

  const handleCloseTopicForm = () => {
    if (isSubmittingTopic) return;
    setIsTopicFormOpen(false);
    setEditingTopic(null);
  };

  const handleSubmitTopicForm = async (formData) => {
    setIsSubmittingTopic(true);
    try {
      let res;
      if (topicFormMode === 'create') {
        res = await createTopicApi({ ...formData, deckId });
      } else {
        res = await updateTopicApi(editingTopic.id, formData);
      }

      if (res?.success) {
        showToast(
          topicFormMode === 'create' ? t('topic.createSuccess') : t('topic.updateSuccess')
        );
        setIsTopicFormOpen(false);
        setEditingTopic(null);
        setRefreshTrigger((prev) => prev + 1);
      }

      return res;
    } catch (err) {
      throw err;
    } finally {
      setIsSubmittingTopic(false);
    }
  };

  const handleOpenDeleteTopicModal = (topic) => {
    setDeletingTopic(topic);
  };

  const handleCloseDeleteTopicModal = () => {
    if (isDeletingTopic) return;
    setDeletingTopic(null);
  };

  const handleConfirmDeleteTopic = async () => {
    if (!deletingTopic) return;
    setIsDeletingTopic(true);
    try {
      const res = await deleteTopicApi(deletingTopic.id);
      if (res?.success) {
        showToast(t('topic.deleteSuccess'));
        setDeletingTopic(null);
        setRefreshTrigger((prev) => prev + 1);
      } else {
        showToast(getApiErrorMessage(res, t('topic.deleteError'), t), 'error');
      }
    } catch (err) {
      showToast(getApiErrorMessage(err, t('topic.deleteError'), t), 'error');
    } finally {
      setIsDeletingTopic(false);
    }
  };

  const handleOpenEditDeckModal = () => {
    setIsDeckFormOpen(true);
  };

  const handleCloseDeckForm = () => {
    setIsDeckFormOpen(false);
  };

  const handleSubmitDeckForm = async (formData) => {
    setIsSubmittingDeck(true);
    try {
      const response = await updateUserDeckApi(deckId, formData);
      if (response?.success) {
        setDeck(response.data);
        setIsDeckFormOpen(false);
        showToast(t('userDeck.updateSuccess'));
      }
      return response;
    } finally {
      setIsSubmittingDeck(false);
    }
  };

  const refreshTopicCards = () => {
    setCardRefreshTrigger((prev) => prev + 1);
  };

  const handleCloseCardForm = () => {
    if (isSubmittingCard) return;
    setIsCardFormOpen(false);
    setCardToEdit(null);
  };

  const handleSubmitCardForm = async (formData) => {
    setIsSubmittingCard(true);
    try {
      let res;
      if (cardToEdit) {
        res = await updateUserCardApi(cardToEdit.id, formData);
      } else {
        res = await createUserCardApi({ ...formData, topicId: selectedTopicId });
      }

      if (res?.success) {
        setIsCardFormOpen(false);
        setCardToEdit(null);
        showToast(cardToEdit ? t('userCards.updateSuccess') : t('userCards.createSuccess'));
        refreshTopicCards();
        setRefreshTrigger((prev) => prev + 1); // update cardCount on topics/deck
        return res;
      }
      return res;
    } finally {
      setIsSubmittingCard(false);
    }
  };

  const handleToggleCardStar = async (card) => {
    if (!card?.id || isTogglingCardStar) return;

    const nextStarredState = !card.state?.starred;

    setIsTogglingCardStar(true);
    try {
      const res = await toggleStarApi(card.id, nextStarredState);
      if (res?.success) {
        setTopicCards((currentCards) =>
          updateCardStarredState(currentCards, card.id, nextStarredState)
        );
      } else {
        showToast(getStarErrorMessage(res, t), 'error');
      }
    } catch (err) {
      showToast(getStarErrorMessage(err?.response?.data, t), 'error');
    } finally {
      setIsTogglingCardStar(false);
    }
  };

  const handleCloseDeleteCardModal = () => {
    if (isDeletingCard) return;
    setDeletingCard(null);
  };

  const handleConfirmDeleteCard = async () => {
    if (!deletingCard) return;
    setIsDeletingCard(true);
    try {
      const res = await deleteUserCardApi(deletingCard.id);
      if (res?.success) {
        setDeletingCard(null);
        showToast(t('userCards.deleteSuccess'));
        refreshTopicCards();
        setRefreshTrigger((prev) => prev + 1);
      } else {
        showToast(getApiErrorMessage(res, t('userCards.deleteError'), t), 'error');
      }
    } catch (err) {
      showToast(getApiErrorMessage(err, t('userCards.deleteError'), t), 'error');
    } finally {
      setIsDeletingCard(false);
    }
  };

  return (
    <main className="topic-list-page">
      <div className="container topic-list-page__container">
        {isLoadingDeck ? (
          <PageLoading />
        ) : deckLoadError ? (
          <EmptyState
            icon="error"
            isError
            title={deckLoadError}
            actionLabel={t('common:back')}
            onAction={() => onNavigate?.(DECK_LIST_MY_DECKS_URL)}
          />
        ) : !deck ? (
          <EmptyState
            icon="inbox"
            title={t('topic.deckNotFound')}
            actionLabel={t('common:back')}
            onAction={() => onNavigate?.(DECK_LIST_MY_DECKS_URL)}
          />
        ) : (
          <>
            <TopicListHeader
              deck={deck}
              topicsCount={topics.length}
              onNavigate={onNavigate}
              onEditDeck={handleOpenEditDeckModal}
              onCreateTopic={handleOpenCreateTopicModal}
              t={t}
            />

            <div className="topic-list-page__content">
              <TopicsSection
                loading={isLoadingTopics}
                error={topicsLoadError}
                topics={topics}
                selectedTopic={selectedTopic}
                onRetry={() => setRefreshTrigger((prev) => prev + 1)}
                onEdit={handleOpenEditTopicModal}
                onDelete={handleOpenDeleteTopicModal}
                onSelect={handleSelectTopic}
                t={t}
              />

              {selectedTopic && !isLoadingTopics && !topicsLoadError && (
                <CardsSection
                  selectedTopic={selectedTopic}
                  topicCards={topicCards}
                  isLoadingCards={loadedCardsRequestKey !== currentCardsRequestKey || isLoadingCards}
                  cardsLoadError={cardsLoadError}
                  isTogglingCardStar={isTogglingCardStar}
                  onOpenCreateCard={handleOpenCreateCard}
                  onRetryCards={refreshTopicCards}
                  onEditCard={handleOpenEditCard}
                  onDeleteCard={setDeletingCard}
                  onToggleCardStar={handleToggleCardStar}
                  t={t}
                />
              )}
            </div>
          </>
        )}
      </div>

      <TopicFormModal
        isOpen={isTopicFormOpen}
        mode={topicFormMode}
        topic={editingTopic}
        onSubmit={handleSubmitTopicForm}
        onClose={handleCloseTopicForm}
        isSubmitting={isSubmittingTopic}
      />

      <ConfirmModal
        isOpen={!!deletingTopic}
        title={t('topic.deleteModalTitle')}
        message={t('topic.deleteModalMsg', { name: deletingTopic?.name ?? '' })}
        confirmText={t('topic.deleteConfirm')}
        cancelText={t('common:cancel')}
        onConfirm={handleConfirmDeleteTopic}
        onCancel={handleCloseDeleteTopicModal}
        isDanger
        isLoading={isDeletingTopic}
      />

      <DeckFormModal
        isOpen={isDeckFormOpen}
        mode="edit"
        initialData={deck}
        onSubmit={handleSubmitDeckForm}
        onClose={handleCloseDeckForm}
        isLoading={isSubmittingDeck}
      />

      <UserCardFormModal
        isOpen={isCardFormOpen}
        mode={cardToEdit ? 'edit' : 'create'}
        card={cardToEdit}
        onSubmit={handleSubmitCardForm}
        onClose={handleCloseCardForm}
        isSubmitting={isSubmittingCard}
      />

      <ConfirmModal
        isOpen={!!deletingCard}
        title={t('userCards.deleteModalTitle')}
        message={t('userCards.deleteModalMessage', { term: deletingCard?.term ?? '' })}
        confirmText={t('common:delete')}
        cancelText={t('common:cancel')}
        isDanger
        isLoading={isDeletingCard}
        onConfirm={handleConfirmDeleteCard}
        onCancel={handleCloseDeleteCardModal}
      />
      <Toast message={toastMessage} variant={toastVariant} onDismiss={handleToastDismiss} />
    </main>
  );
}

export default TopicListPage;
