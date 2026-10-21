import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  getUserDecksApi,
  getTopicsApi,
  createTopicApi,
  updateTopicApi,
  deleteTopicApi,
  updateUserDeckApi,
} from '../../vocabularyApi';
import Pagination from '../../../../components/Pagination/Pagination';
import Modal from '../../../../components/Modal/Modal';
import SearchInput from '../../../../components/SearchInput/SearchInput';
import BackButton from '../../../../components/BackButton/BackButton';
import TopicCard from '../../components/TopicCard/TopicCard';
import TopicFormModal from '../../components/TopicFormModal/TopicFormModal';
import DeckFormModal from '../../components/DeckFormModal/DeckFormModal';
import UserCardFormModal from '../../components/UserCardFormModal/UserCardFormModal';
import UserCardItem from '../../components/UserCardItem/UserCardItem';
import { getMockCardsForTopic } from '../../userCardMockData';
import './TopicListPage.css';

const LIMIT = 9;

/**
 * TopicListPage — Trang quản lý danh sách chủ đề (Topics) với đầy đủ CRUD.
 *
 * @param {Function} onNavigate - Hàm điều hướng từ App.jsx
 */
function TopicListPage({ deckId, onNavigate }) {
  const { t } = useTranslation('learning');

  // States dữ liệu
  const [deck, setDeck] = useState(null);
  const [deckLoading, setDeckLoading] = useState(true);
  const [deckLoadError, setDeckLoadError] = useState('');
  const [topics, setTopics] = useState([]);
  const [selectedTopicId, setSelectedTopicId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // States filter và tìm kiếm
  const [cardSearchQuery, setCardSearchQuery] = useState('');

  // States phân trang
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // States modal tạo / sửa
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formMode, setFormMode] = useState('create'); // 'create' | 'edit'
  const [editingTopic, setEditingTopic] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  // States modal xóa
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [deletingTopic, setDeletingTopic] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // States thông báo thành công
  const [successMsg, setSuccessMsg] = useState('');
  const [isDeckFormOpen, setIsDeckFormOpen] = useState(false);
  const [isCardFormOpen, setIsCardFormOpen] = useState(false);
  const [cardToEdit, setCardToEdit] = useState(null);
  const [cardToDelete, setCardToDelete] = useState(null);

  const selectedTopic =
    topics.find((topic) => String(topic.id) === String(selectedTopicId)) ?? null;
  const topicCards = getMockCardsForTopic(selectedTopic);
  const normalizedCardSearchQuery = cardSearchQuery.trim().toLocaleLowerCase();
  const filteredTopicCards = topicCards.filter((card) =>
    card.term.toLocaleLowerCase().includes(normalizedCardSearchQuery)
  );

  useEffect(() => {
    const fetchDeck = async () => {
      setDeckLoading(true);
      setDeckLoadError('');
      try {
        const response = await getUserDecksApi({ page: 1, limit: 100 });
        const userDecks = response?.data?.content ?? [];
        setDeck(userDecks.find((userDeck) => String(userDeck.id) === String(deckId)) ?? null);
      } catch {
        setDeck(null);
        setDeckLoadError(t('topic.deckLoadError'));
      } finally {
        setDeckLoading(false);
      }
    };

    fetchDeck();
  }, [deckId, t]);

  // ── Fetch topics ──
  useEffect(() => {
    const fetchTopics = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await getTopicsApi({ deckId, page, limit: LIMIT });
        if (res?.success && res.data) {
          const nextTopics = res.data.content ?? [];
          setTopics(nextTopics);
          setSelectedTopicId((currentId) =>
            nextTopics.some((topic) => String(topic.id) === String(currentId))
              ? currentId
              : (nextTopics[0]?.id ?? null)
          );
          setTotalPages(res.data.totalPages ?? 1);
          setTotalElements(res.data.totalElements ?? 0);
        } else {
          setError(t('topic.loadError'));
        }
      } catch {
        setError(t('topic.loadError'));
      } finally {
        setLoading(false);
      }
    };
    fetchTopics();
  }, [deckId, page, refreshTrigger, t]);

  // Auto-dismiss thông báo thành công sau 3 giây
  useEffect(() => {
    if (!successMsg) return;
    const timer = setTimeout(() => setSuccessMsg(''), 3000);
    return () => clearTimeout(timer);
  }, [successMsg]);

  // ── Handlers ──
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
        setSuccessMsg(formMode === 'create' ? t('topic.createSuccess') : t('topic.updateSuccess'));
        setIsFormOpen(false);
        setEditingTopic(null);
        setRefreshTrigger((prev) => prev + 1);
      } else {
        setFormError(res?.message ?? t('topic.submitError'));
      }
    } catch {
      setFormError(t('topic.submitError'));
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
        setSuccessMsg(t('topic.deleteSuccess'));
        setIsDeleteOpen(false);
        setDeletingTopic(null);
        if (topics.length === 1 && page > 1) setPage((prev) => prev - 1);
        else setRefreshTrigger((prev) => prev + 1);
      }
    } catch {
      setError(t('topic.submitError'));
    } finally {
      setIsDeleting(false);
    }
  };

  const handleDeckUpdate = async (formData) => {
    const response = await updateUserDeckApi(deckId, formData);
    if (response?.success) {
      setDeck(response.data);
      setIsDeckFormOpen(false);
      setSuccessMsg(t('userDeck.updateSuccess'));
    }
    return response;
  };

  const handleCloseCardForm = () => {
    setIsCardFormOpen(false);
    setCardToEdit(null);
  };

  return (
    <main className="topic-list-page">
      <div className="container topic-list-page__container">
        {deckLoading ? (
          <div className="page-loading">
            <div className="app-loading-spinner" />
          </div>
        ) : deckLoadError ? (
          <div className="topic-list-empty glass-card" role="alert">
            <h1 className="text-headline-sm">{deckLoadError}</h1>
            <BackButton label={t('topic.backToDecks')} onClick={() => onNavigate?.('/decks')} />
          </div>
        ) : !deck ? (
          <div className="topic-list-empty glass-card">
            <h1 className="text-headline-sm">{t('topic.deckNotFound')}</h1>
            <BackButton label={t('topic.backToDecks')} onClick={() => onNavigate?.('/decks')} />
          </div>
        ) : (
          <>
            <header className="topic-list-page__header">
              <div className="topic-list-page__heading">
                <BackButton label={t('flashcard.back')} onClick={() => onNavigate?.('/decks')} />
                <div className="topic-list-page__title-group">
                  <div className="topic-list-page__title-row">
                    <h1 className="text-headline-md topic-list-page__title">{deck.title}</h1>
                    <button
                      type="button"
                      className="btn-icon topic-list-page__edit-deck-btn"
                      aria-label={t('userDeck.editDeck')}
                      title={t('userDeck.editDeck')}
                      onClick={() => setIsDeckFormOpen(true)}
                    >
                      <span className="material-symbols-outlined" aria-hidden="true">
                        edit
                      </span>
                    </button>
                  </div>
                  <div className="topic-list-page__stats">
                    <span>
                      <span className="material-symbols-outlined" aria-hidden="true">
                        style
                      </span>
                      {t('userCards.deckCardCount', { count: deck.cardCount ?? 0 })}
                    </span>
                    <span>
                      <span className="material-symbols-outlined" aria-hidden="true">
                        category
                      </span>
                      {t('userCards.deckTopicCount', { count: totalElements })}
                    </span>
                    <button
                      type="button"
                      className="btn-primary topic-list-page__learn-btn"
                      onClick={() => onNavigate?.(`/decks/${deckId}`)}
                    >
                      <span className="material-symbols-outlined" aria-hidden="true">
                        play_arrow
                      </span>
                      {t('userDeck.studyNow')}
                    </button>
                  </div>
                </div>
              </div>
            </header>

            {successMsg && (
              <div className="topic-list-success-toast" role="status" aria-live="polite">
                <span className="material-symbols-outlined" aria-hidden="true">
                  check_circle
                </span>
                {successMsg}
              </div>
            )}

            <div className="topic-list-page__content">
              <section className="topic-list-page__topics-section">
                <div className="topic-list-page__topics-toolbar">
                  <SearchInput
                    id="card-search"
                    className="topic-list-page__search-bar"
                    placeholder={t('userCards.searchByTermPlaceholder')}
                    value={cardSearchQuery}
                    onChange={(event) => setCardSearchQuery(event.target.value)}
                    onClear={() => setCardSearchQuery('')}
                    clearLabel={t('userCards.clearSearch')}
                  />
                  <button
                    type="button"
                    id="topic-create-btn"
                    className="btn-primary topic-list-page__create-btn"
                    onClick={handleOpenCreate}
                  >
                    <span className="material-symbols-outlined" aria-hidden="true">
                      add
                    </span>
                    {t('topic.createBtn')}
                  </button>
                </div>

                {loading ? (
                  <div className="page-loading">
                    <div className="app-loading-spinner" />
                  </div>
                ) : error ? (
                  <div className="page-loading">
                    <p className="error-message">{error}</p>
                    <button
                      type="button"
                      className="btn-secondary"
                      onClick={() => setRefreshTrigger((prev) => prev + 1)}
                    >
                      {t('topic.retry')}
                    </button>
                  </div>
                ) : topics.length === 0 ? (
                  <div className="topic-list-empty glass-well">
                    <h2 className="text-headline-sm">{t('topic.emptyTitle')}</h2>
                    <p className="text-body-md text-disabled">{t('topic.emptyDesc')}</p>
                  </div>
                ) : (
                  <>
                    <div className="topic-list-grid">
                      {topics.map((topic) => (
                        <TopicCard
                          key={topic.id}
                          topic={topic}
                          isSelected={String(topic.id) === String(selectedTopic?.id)}
                          onEdit={handleOpenEdit}
                          onDelete={handleOpenDelete}
                          onSelect={handleSelectTopic}
                          onCreateCard={handleOpenCreateCard}
                        />
                      ))}
                    </div>

                    <Pagination
                      currentPage={page}
                      totalPages={totalPages}
                      onPageChange={(nextPage) => setPage(nextPage)}
                    />
                  </>
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
                        <span className="material-symbols-outlined" aria-hidden="true">
                          upload_file
                        </span>
                        {t('userCards.importVocabulary')}
                      </button>
                      <button
                        type="button"
                        className="btn-primary"
                        onClick={() => handleOpenCreateCard(selectedTopic)}
                      >
                        <span className="material-symbols-outlined" aria-hidden="true">
                          add
                        </span>
                        {t('userCards.createCard')}
                      </button>
                    </div>
                  </div>

                  <div
                    className={`topic-list-page__cards-grid${topicCards.length === 0 ? ' topic-list-page__cards-grid--empty' : ''}`}
                  >
                    {filteredTopicCards.length > 0 ? (
                      filteredTopicCards.map((card) => (
                        <UserCardItem
                          key={card.id}
                          card={{ ...card, topicName: selectedTopic.name }}
                          onEdit={handleOpenEditCard}
                          onDelete={setCardToDelete}
                        />
                      ))
                    ) : (
                      <div className="topic-list-page__cards-empty glass-well">
                        <p className="text-body-md">
                          {topicCards.length > 0 && normalizedCardSearchQuery
                            ? t('userCards.noSearchResults')
                            : t('userCards.emptyTopicCards')}
                        </p>
                      </div>
                    )}
                  </div>
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
      />
      {isFormOpen && formError && (
        <div className="topic-form-error-toast" role="alert">
          <span className="material-symbols-outlined" aria-hidden="true">
            error
          </span>
          {formError}
        </div>
      )}

      <Modal
        isOpen={isDeleteOpen}
        title={t('topic.deleteModalTitle')}
        message={t('topic.deleteModalMsg', { name: deletingTopic?.name ?? '' })}
        confirmText={t('topic.deleteConfirm')}
        cancelText={t('topic.cancel')}
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
      />

      <UserCardFormModal
        isOpen={isCardFormOpen}
        mode={cardToEdit ? 'edit' : 'create'}
        card={cardToEdit}
        topics={selectedTopic ? [selectedTopic] : []}
        isTopicLocked
        onClose={handleCloseCardForm}
      />

      <Modal
        isOpen={!!cardToDelete}
        title={t('userCards.deleteModalTitle')}
        message={t('userCards.deleteModalMessage', { term: cardToDelete?.term ?? '' })}
        confirmText={t('userCards.delete')}
        cancelText={t('topic.cancel')}
        isDanger
        onConfirm={() => setCardToDelete(null)}
        onCancel={() => setCardToDelete(null)}
      />
    </main>
  );
}

export default TopicListPage;
