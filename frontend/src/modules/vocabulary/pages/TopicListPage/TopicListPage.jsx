import { useState, useEffect, useCallback } from 'react';
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

function TopicListPage({ deckId, onNavigate }) {
  const { t } = useTranslation(['learning', 'api', 'common']);

  // States dữ liệu
  const [deck, setDeck] = useState(null);
  const [deckLoading, setDeckLoading] = useState(true);
  const [deckLoadError, setDeckLoadError] = useState('');
  const [topics, setTopics] = useState([]);
  const [selectedTopicId, setSelectedTopicId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // States danh sách thẻ của chủ đề được chọn
  const [topicCards, setTopicCards] = useState([]);
  const [loadedCardsRequestKey, setLoadedCardsRequestKey] = useState(null);
  const [cardsLoading, setCardsLoading] = useState(false);
  const [cardsLoadError, setCardsLoadError] = useState('');
  const [cardRefreshTrigger, setCardRefreshTrigger] = useState(0);

  // States modal tạo / sửa topic
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formMode, setFormMode] = useState('create'); // 'create' | 'edit'
  const [editingTopic, setEditingTopic] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  // States modal xóa topic
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [deletingTopic, setDeletingTopic] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

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
  const [isDeckSubmitting, setIsDeckSubmitting] = useState(false);

  // States modal tạo / sửa card
  const [isCardFormOpen, setIsCardFormOpen] = useState(false);
  const [cardToEdit, setCardToEdit] = useState(null);
  const [cardToDelete, setCardToDelete] = useState(null);
  const [isCardSubmitting, setIsCardSubmitting] = useState(false);
  const [isDeletingCard, setIsDeletingCard] = useState(false);
  const [isTogglingCardStar, setIsTogglingCardStar] = useState(false);

  const selectedTopic =
    topics.find((topic) => String(topic.id) === String(selectedTopicId)) ?? null;

  useEffect(() => {
    let isActive = true;

    const fetchDeck = async () => {
      setDeckLoading(true);
      setDeckLoadError('');
      try {
        const response = await getDeckByIdApi(deckId);
        if (!isActive) return;

        if (!response?.success || !response.data) {
          setDeck(null);
          setDeckLoadError(t('common:loadError'));
          return;
        }
        setDeck(response.data);
      } catch {
        if (!isActive) return;
        setDeck(null);
        setDeckLoadError(t('common:loadError'));
      } finally {
        if (isActive) setDeckLoading(false);
      }
    };

    fetchDeck();

    return () => {
      isActive = false;
    };
  }, [deckId, refreshTrigger, t]);

  useEffect(() => {
    let isActive = true;

    const fetchTopics = async () => {
      setLoading(true);
      setError(null);
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
          setError(t('common:loadError'));
        }
      } catch {
        if (!isActive) return;
        setError(t('common:loadError'));
      } finally {
        if (isActive) setLoading(false);
      }
    };
    fetchTopics();

    return () => {
      isActive = false;
    };
  }, [deckId, refreshTrigger, t]);

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
      setCardsLoading(true);
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
            setCardsLoadError(t('common:loadError'));
          }
        }
      } catch {
        if (isCurrentRequest) {
          setTopicCards([]);
          setLoadedCardsRequestKey(requestKey);
          setCardsLoadError(t('common:loadError'));
        }
      } finally {
        if (isCurrentRequest) {
          setCardsLoading(false);
        }
      }
    };

    fetchCards();
    return () => {
      isCurrentRequest = false;
    };
  }, [selectedTopicId, cardRefreshTrigger, t]);

  const currentCardsRequestKey = selectedTopicId
    ? `${selectedTopicId}:${cardRefreshTrigger}`
    : null;

  // ── Handlers Topic ──
  const handleOpenCreate = () => {
    setFormMode('create');
    setEditingTopic(null);
    setFormError('');
    setIsFormOpen(true);
  };

  const handleOpenEdit = (topic) => {
    setFormMode('edit');
    setEditingTopic(topic);
    setFormError('');
    setIsFormOpen(true);
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

  const handleCloseForm = () => {
    if (isSubmitting) return;
    setIsFormOpen(false);
    setEditingTopic(null);
    setFormError('');
  };

  const handleFormSubmit = async (formData) => {
    setIsSubmitting(true);
    setFormError('');
    try {
      let res;
      if (formMode === 'create') {
        res = await createTopicApi({ ...formData, deckId });
      } else {
        res = await updateTopicApi(editingTopic.id, formData);
      }

      if (res?.success) {
        showToast(formMode === 'create' ? t('topic.createSuccess') : t('topic.updateSuccess'));
        setIsFormOpen(false);
        setEditingTopic(null);
        setRefreshTrigger((prev) => prev + 1);
      } else {
        const errorCode = res?.code;
        const msg =
          (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
          res?.message ||
          t('topic.submitError');
        setFormError(msg);
      }
    } catch (err) {
      const errorCode = err?.response?.data?.code;
      const msg =
        (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
        err?.response?.data?.message ||
        err?.message ||
        t('topic.submitError');
      setFormError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenDelete = (topic) => {
    setDeletingTopic(topic);
    setIsDeleteOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!deletingTopic) return;
    setIsDeleting(true);
    try {
      const res = await deleteTopicApi(deletingTopic.id);
      if (res?.success) {
        showToast(t('topic.deleteSuccess'));
        setIsDeleteOpen(false);
        setDeletingTopic(null);
        setRefreshTrigger((prev) => prev + 1);
      } else {
        showToast(getApiErrorMessage(res, t('topic.deleteError'), t), 'error');
      }
    } catch (err) {
      showToast(getApiErrorMessage(err, t('topic.deleteError'), t), 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleDeckUpdate = async (formData) => {
    setIsDeckSubmitting(true);
    try {
      const response = await updateUserDeckApi(deckId, formData);
      if (response?.success) {
        setDeck(response.data);
        setIsDeckFormOpen(false);
        showToast(t('userDeck.updateSuccess'));
      }
      return response;
    } finally {
      setIsDeckSubmitting(false);
    }
  };

  // ── Handlers Card ──
  const handleCloseCardForm = () => {
    if (isCardSubmitting) return;
    setIsCardFormOpen(false);
    setCardToEdit(null);
  };

  const handleCardFormSubmit = async (formData) => {
    setIsCardSubmitting(true);
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
        setCardRefreshTrigger((prev) => prev + 1);
        setRefreshTrigger((prev) => prev + 1); // update cardCount on topics/deck
        return res;
      }
      return res;
    } finally {
      setIsCardSubmitting(false);
    }
  };

  const handleToggleCardStar = async (card) => {
    if (!card?.id || isTogglingCardStar) return;

    const nextStarredState = !card.state?.starred;
    const getStarErrorMessage = (errorData) => {
      const errorCode = errorData?.code;
      return (
        (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
        errorData?.message ||
        t('api:common.UNKNOWN_ERROR')
      );
    };

    setIsTogglingCardStar(true);
    try {
      const res = await toggleStarApi(card.id, nextStarredState);
      if (res?.success) {
        setTopicCards((currentCards) =>
          currentCards.map((currentCard) =>
            String(currentCard.id) === String(card.id)
              ? {
                  ...currentCard,
                  state: {
                    ...currentCard.state,
                    starred: nextStarredState,
                  },
                }
              : currentCard
          )
        );
      } else {
        showToast(getStarErrorMessage(res), 'error');
      }
    } catch (err) {
      showToast(getStarErrorMessage(err?.response?.data), 'error');
    } finally {
      setIsTogglingCardStar(false);
    }
  };

  const handleConfirmDeleteCard = async () => {
    if (!cardToDelete) return;
    setIsDeletingCard(true);
    try {
      const res = await deleteUserCardApi(cardToDelete.id);
      if (res?.success) {
        setCardToDelete(null);
        showToast(t('userCards.deleteSuccess'));
        setCardRefreshTrigger((prev) => prev + 1);
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
        {deckLoading ? (
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
                      onClick={() => setIsDeckFormOpen(true)}
                    >
                      <span className="material-symbols-outlined">edit</span>
                    </button>
                  </div>
                  <div className="topic-list-page__stats">
                    <span>{t('userCards.deckCardCount', { count: deck.cardCount ?? 0 })}</span>
                    <span>{t('userCards.deckTopicCount', { count: topics.length })}</span>
                    <button
                      type="button"
                      className="btn-primary topic-list-page__learn-btn"
                      onClick={() =>
                        onNavigate?.(`/decks/${deck.slug}`, {
                          returnTo: `/decks/${deckId}/topics?tab=my-decks`,
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
                onClick={handleOpenCreate}
              >
                <span className="material-symbols-outlined">add</span>
                {t('topic.createBtn')}
              </button>
            </header>

            <div className="topic-list-page__content">
              <section className="topic-list-page__topics-section">
                {loading ? (
                  <PageLoading />
                ) : error ? (
                  <EmptyState
                    icon="error"
                    isError
                    description={error}
                    actionLabel={t('common:retry')}
                    onAction={() => setRefreshTrigger((prev) => prev + 1)}
                  />
                ) : topics.length === 0 ? (
                  <EmptyState
                    icon="category"
                    title={t('topic.emptyTitle')}
                    description={t('topic.emptyDesc')}
                  />
                ) : (
                  <div className="topic-list-grid">
                    {topics.map((topic) => (
                      <TopicCard
                        key={topic.id}
                        topic={topic}
                        isSelected={String(topic.id) === String(selectedTopic?.id)}
                        onEdit={handleOpenEdit}
                        onDelete={handleOpenDelete}
                        onSelect={handleSelectTopic}
                      />
                    ))}
                  </div>
                )}
              </section>

              {selectedTopic && !loading && !error && (
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
                        onClick={() => handleOpenCreateCard(selectedTopic)}
                      >
                        <span className="material-symbols-outlined">add</span>
                        {t('userCards.createCard')}
                      </button>
                    </div>
                  </div>

                  {loadedCardsRequestKey !== currentCardsRequestKey || cardsLoading ? (
                    <PageLoading />
                  ) : cardsLoadError ? (
                    <EmptyState
                      icon="error"
                      isError
                      description={cardsLoadError}
                      actionLabel={t('common:retry')}
                      onAction={() => setCardRefreshTrigger((prev) => prev + 1)}
                    />
                  ) : (
                    <div
                      className={`topic-list-page__cards-grid${topicCards.length === 0 ? ' topic-list-page__cards-grid--empty' : ''}`}
                    >
                      {topicCards.map((card) => (
                        <UserCardItem
                          key={card.id}
                          card={card}
                          onEdit={handleOpenEditCard}
                          onDelete={setCardToDelete}
                          onToggleStar={handleToggleCardStar}
                          isStarProcessing={isTogglingCardStar}
                        />
                      ))}
                      {topicCards.length === 0 && (
                        <EmptyState icon="style" description={t('userCards.emptyTopicCards')} />
                      )}
                    </div>
                  )}
                </section>
              )}
            </div>
          </>
        )}
      </div>

      <TopicFormModal
        isOpen={isFormOpen}
        mode={formMode}
        topic={editingTopic}
        onSubmit={handleFormSubmit}
        onClose={handleCloseForm}
        isSubmitting={isSubmitting}
        generalError={formError}
      />

      <ConfirmModal
        isOpen={isDeleteOpen}
        title={t('topic.deleteModalTitle')}
        message={t('topic.deleteModalMsg', { name: deletingTopic?.name ?? '' })}
        confirmText={t('topic.deleteConfirm')}
        cancelText={t('common:cancel')}
        onConfirm={handleConfirmDelete}
        onCancel={() => {
          if (!isDeleting) {
            setIsDeleteOpen(false);
            setDeletingTopic(null);
          }
        }}
        isDanger
        isLoading={isDeleting}
      />

      <DeckFormModal
        isOpen={isDeckFormOpen}
        mode="edit"
        initialData={deck}
        onSubmit={handleDeckUpdate}
        onClose={() => setIsDeckFormOpen(false)}
        isLoading={isDeckSubmitting}
      />

      <UserCardFormModal
        isOpen={isCardFormOpen}
        mode={cardToEdit ? 'edit' : 'create'}
        card={cardToEdit}
        onSubmit={handleCardFormSubmit}
        onClose={handleCloseCardForm}
        isSubmitting={isCardSubmitting}
      />

      <ConfirmModal
        isOpen={!!cardToDelete}
        title={t('userCards.deleteModalTitle')}
        message={t('userCards.deleteModalMessage', { term: cardToDelete?.term ?? '' })}
        confirmText={t('common:delete')}
        cancelText={t('common:cancel')}
        isDanger
        isLoading={isDeletingCard}
        onConfirm={handleConfirmDeleteCard}
        onCancel={() => {
          if (!isDeletingCard) setCardToDelete(null);
        }}
      />
      <Toast message={toastMessage} variant={toastVariant} onDismiss={handleToastDismiss} />
    </main>
  );
}

export default TopicListPage;
