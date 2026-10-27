import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  getTopicsByDeckSlugApi,
  getUnlearnedCardsByTopicIdApi,
  submitSrsReviewApi,
  toggleStarApi,
  toggleHideApi,
} from '../flashcardApi';
import FlashcardStudyArea from '../components/FlashcardStudyArea/FlashcardStudyArea';
import FlashcardCompletionState from '../components/FlashcardCompletionState/FlashcardCompletionState';
import FlashcardSessionLayout from '../components/FlashcardSessionLayout/FlashcardSessionLayout';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import ProgressBar from '../../../../components/ProgressBar/ProgressBar';
import Toast from '../../../../components/Toast/Toast';
import './FlashcardSessionItems.css';
import './FlashcardStudyPage.css';

function FlashcardStudyPage({ deckSlug, returnTo, onNavigate }) {
  const { t } = useTranslation(['learning', 'api', 'common']);
  const backPath =
    typeof returnTo === 'string' && /^\/decks(?:\/|\?|$)/.test(returnTo) ? returnTo : '/decks';
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [selectedTopicId, setSelectedTopicId] = useState(null);

  const [topicProgress, setTopicProgress] = useState({});
  const [unlearnedCardsByTopic, setUnlearnedCardsByTopic] = useState({});
  const [topicCardsError, setTopicCardsError] = useState({});

  // State quản lý trạng thái xử lý tương tác thẻ
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    const fetchTopics = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await getTopicsByDeckSlugApi(deckSlug);
        if (res.success && res.data) {
          setTopics(res.data);

          const progressMap = {};
          res.data.forEach((t) => {
            const total = t.cardCount ?? 0;
            const learned = t.learnedCardCount ?? 0;
            progressMap[t.id] = {
              learned: Math.min(learned, total),
              total,
            };
          });
          setTopicProgress(progressMap);

          // Tự động chọn topic đầu tiên nếu có
          if (res.data.length > 0) {
            setSelectedTopicId(res.data[0].id);
          }
        } else {
          setError({
            message: t('common:loadError'),
            actionLabel: t('flashcard.backToDecks'),
            actionPath: backPath,
          });
        }
      } catch (err) {
        console.error('Lỗi khi tải topics:', err);
        const errorCode = err.response?.data?.code;
        const errorMessage =
          (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
          t('common:loadError');

        if (errorCode === 'PREMIUM_REQUIRED') {
          setError({
            message: errorMessage,
            actionLabel: t('flashcard.backToDecks'),
            actionPath: backPath,
          });
        } else {
          setError({
            message: errorMessage,
            actionLabel: t('flashcard.backToDecks'),
            actionPath: backPath,
          });
        }
      } finally {
        setLoading(false);
      }
    };

    if (deckSlug) {
      fetchTopics();
    }
  }, [deckSlug, backPath, t]);

  useEffect(() => {
    if (!selectedTopicId) return;

    // Nếu đã load thẻ của topic này rồi thì thôi
    if (unlearnedCardsByTopic[selectedTopicId] !== undefined) return;

    const fetchCards = async () => {
      setTopicCardsError((prev) => ({ ...prev, [selectedTopicId]: false }));
      try {
        const res = await getUnlearnedCardsByTopicIdApi(selectedTopicId);
        if (res && res.success) {
          // Lọc bỏ thẻ đã ẩn và đã học (chỉ giữ thẻ chưa học và không bị ẩn)
          const allCards = Array.isArray(res.data) ? res.data : [];
          const unlearnedCards = allCards.filter((card) => {
            const isHidden = Boolean(card.state?.hidden);
            const isLearned = Boolean(card.state?.learned);
            return !isHidden && !isLearned;
          });
          setUnlearnedCardsByTopic((prev) => ({
            ...prev,
            [selectedTopicId]: unlearnedCards,
          }));
        } else {
          setTopicCardsError((prev) => ({ ...prev, [selectedTopicId]: true }));
        }
      } catch (err) {
        console.error('Lỗi khi tải danh sách thẻ cho topic:', err);
        const errorCode = err.response?.data?.code;
        const errorMessage =
          (errorCode && t(`api:error.${errorCode}`, { defaultValue: '' })) ||
          t('common:loadError');

        if (errorCode === 'PREMIUM_REQUIRED') {
          setError({
            message: errorMessage,
            actionLabel: t('flashcard.backToDecks'),
            actionPath: backPath,
          });
        } else if (errorCode === 'DECK_NOT_FOUND') {
          setError({
            message: errorMessage,
            actionLabel: t('flashcard.backToDecks'),
            actionPath: backPath,
          });
        } else {
          setTopicCardsError((prev) => ({ ...prev, [selectedTopicId]: true }));
        }
      }
    };

    fetchCards();
  }, [selectedTopicId, unlearnedCardsByTopic, backPath, t]);

  const removeCurrentCardAndUpdateProgress = () => {
    setUnlearnedCardsByTopic((prev) => {
      const list = prev[selectedTopicId] || [];
      return {
        ...prev,
        [selectedTopicId]: list.slice(1),
      };
    });

    setTopicProgress((prev) => {
      const current = prev[selectedTopicId];
      if (!current) return prev;
      return {
        ...prev,
        [selectedTopicId]: {
          ...current,
          learned: Math.min(current.total, (current.learned ?? 0) + 1),
        },
      };
    });
  };

  const [toastMessage, setToastMessage] = useState('');
  const handleToastDismiss = useCallback(() => setToastMessage(''), []);

  /**
   * Xử lý lỗi chung khi tương tác với flashcard (Review, Star, Hide).
   */
  const handleCardActionError = (err) => {
    const errorCode = err.response?.data?.code;
    const mappedMessage = errorCode && t(`api:error.${errorCode}`, { defaultValue: '' });
    const errorMessage =
      mappedMessage || err.response?.data?.message || t('api:common.UNKNOWN_ERROR');

    if (errorCode === 'PREMIUM_REQUIRED') {
      setError({
        message: errorMessage,
        actionLabel: t('flashcard.backToDecks'),
        actionPath: backPath,
      });
    } else if (errorCode === 'DECK_NOT_FOUND' || errorCode === 'DECK_ACCESS_DENIED') {
      setError({
        message: errorMessage,
        actionLabel: t('flashcard.backToDecks'),
        actionPath: backPath,
      });
    } else {
      // Các lỗi thao tác thẻ như RATE_LIMIT_EXCEEDED, CARD_NOT_FOUND, CARD_ACCESS_DENIED,...
      setToastMessage(errorMessage);
    }
  };

  /**
   * - Grade 0 (Học lại)
   * - Grade 1 (Khó)
   * - Grade 2 (Dễ)
   * - Grade 3 (Quá dễ)
   *
   * @param {number} grade - Điểm đánh giá (0, 1, 2, 3)
   * @param {string} gradeLabel - Nhãn tiếng Việt tương ứng
   */
  const handleReviewCard = async (grade, gradeLabel) => {
    if (!selectedTopicId || isProcessing) return;

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || [];
    if (currentCardList.length === 0) return;

    const currentCard = currentCardList[0];
    if (!currentCard || !currentCard.id) return;

    setIsProcessing(true);
    try {
      const res = await submitSrsReviewApi(currentCard.id, grade);
      if (res && res.success) {
        removeCurrentCardAndUpdateProgress();
      } else {
        console.error('Đánh giá thẻ không thành công:', res);
      }
    } catch (err) {
      console.error(`Lỗi khi gửi đánh giá SRS thẻ ID ${currentCard.id} (${gradeLabel}):`, err);
      handleCardActionError(err);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleToggleStar = async () => {
    if (!selectedTopicId || isProcessing) return;

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || [];
    if (currentCardList.length === 0) return;

    const currentCard = currentCardList[0];
    if (!currentCard || !currentCard.id) return;

    const isStarred = Boolean(currentCard.state?.starred ?? currentCard.flagsStarred);
    const nextStarredState = !isStarred;

    setIsProcessing(true);
    try {
      const res = await toggleStarApi(currentCard.id, nextStarredState);
      if (res && res.success) {
        // Cập nhật trạng thái star trong danh sách thẻ của topic
        setUnlearnedCardsByTopic((prev) => {
          const list = prev[selectedTopicId] || [];
          const updatedList = list.map((card, idx) => {
            if (idx === 0) {
              return {
                ...card,
                flagsStarred: nextStarredState,
                state: card.state
                  ? { ...card.state, starred: nextStarredState }
                  : { starred: nextStarredState },
              };
            }
            return card;
          });
          return {
            ...prev,
            [selectedTopicId]: updatedList,
          };
        });
      } else {
        console.error('Đổi trạng thái gắn sao không thành công:', res);
      }
    } catch (err) {
      console.error(`Lỗi khi đổi trạng thái gắn sao thẻ ID ${currentCard.id}:`, err);
      handleCardActionError(err);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleToggleHide = async () => {
    if (!selectedTopicId || isProcessing) return;

    const currentCardList = unlearnedCardsByTopic[selectedTopicId] || [];
    if (currentCardList.length === 0) return;

    const currentCard = currentCardList[0];
    if (!currentCard || !currentCard.id) return;

    setIsProcessing(true);
    try {
      const res = await toggleHideApi(currentCard.id, true);
      if (res && res.success) {
        removeCurrentCardAndUpdateProgress();
      } else {
        console.error('Ẩn thẻ không thành công:', res);
      }
    } catch (err) {
      console.error(`Lỗi khi ẩn thẻ ID ${currentCard.id}:`, err);
      handleCardActionError(err);
    } finally {
      setIsProcessing(false);
    }
  };

  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return (
      <div className="flashcard-page-container">
        <EmptyState
          icon="error"
          isError
          description={error.message}
          actionLabel={error.actionLabel}
          actionVariant="primary"
          onAction={() => {
            if (onNavigate) onNavigate(error.actionPath);
          }}
        />
      </div>
    );
  }

  const currentCardList = selectedTopicId ? unlearnedCardsByTopic[selectedTopicId] || [] : [];
  const currentCard = currentCardList[0] || null;
  const isCurrentCardStarred = Boolean(currentCard?.state?.starred ?? currentCard?.flagsStarred);

  const progressValues = Object.values(topicProgress);
  const globalTotal = progressValues.reduce((acc, curr) => acc + (curr.total || 0), 0);
  const globalLearned = progressValues.reduce((acc, curr) => acc + (curr.learned || 0), 0);

  return (
    <>
      <FlashcardSessionLayout
        backLabel={t('common:back')}
        onBack={() => onNavigate?.(backPath)}
        progress={
          <div>
            {progressValues.length > 0 && globalTotal > 0 && (
              <div className="glass-card flashcard-global-progress">
                <ProgressBar
                  title={t('flashcard.overallProgress')}
                  current={globalLearned}
                  total={globalTotal}
                />
              </div>
            )}
          </div>
        }
        sidebar={
          <>
            {topics.length === 0 && !loading && (
              <div className="empty-state glass-card flashcard-empty-topic">
                <span className="material-symbols-outlined">category</span>
                <p>{t('flashcard.noTopicsInDeck')}</p>
              </div>
            )}

            {topics.map((topic) => {
              const progress = topicProgress[topic.id];
              const learned = progress?.learned ?? 0;
              const total = progress?.total ?? 0;
              const percent = total > 0 ? Math.round((learned / total) * 100) : 0;

              return (
                <div
                  key={topic.id}
                  className={`glass-card flashcard-session-item flashcard-topic-item ${
                    selectedTopicId === topic.id ? 'active' : ''
                  }`}
                  onClick={() => setSelectedTopicId(topic.id)}
                >
                  <div className="flashcard-session-item-header">
                    <h3 className="flashcard-session-item-title">{topic.name}</h3>
                    {progress && (
                      <span className="flashcard-progress-stats">
                        {learned} / {total}
                      </span>
                    )}
                  </div>
                  {topic.description && <p className="flashcard-topic-desc">{topic.description}</p>}
                  {progress && total > 0 && (
                    <div className="flashcard-progress-track">
                      <div className="flashcard-progress-bar" style={{ width: `${percent}%` }} />
                    </div>
                  )}
                </div>
              );
            })}
          </>
        }
      >
        {!selectedTopicId ? (
          <div className="flashcard-empty-placeholder">
            <span className="material-symbols-outlined flashcard-empty-placeholder-icon">
              style
            </span>
            <p className="text-body-md flashcard-placeholder-text">
              {t('flashcard.noCardsToStudy')}
            </p>
          </div>
        ) : topicCardsError[selectedTopicId] ? (
          <div className="flashcard-empty-placeholder">
            <span className="material-symbols-outlined flashcard-empty-placeholder-icon flashcard-placeholder-icon--error">
              wifi_off
            </span>
            <h2 className="text-title-md">{t('common:loadError')}</h2>
            <button
              className="btn-primary flashcard-placeholder-btn"
              onClick={() => {
                setTopicCardsError((prev) => ({ ...prev, [selectedTopicId]: false }));
                setUnlearnedCardsByTopic((prev) => ({ ...prev, [selectedTopicId]: undefined }));
              }}
            >
              {t('common:retry')}
            </button>
          </div>
        ) : unlearnedCardsByTopic[selectedTopicId] === undefined ? (
          <div className="flashcard-empty-placeholder">
            <div className="app-loading-spinner" />
            <p className="text-body-md">{t('flashcard.loadingCards')}</p>
          </div>
        ) : currentCard ? (
          <FlashcardStudyArea
            card={currentCard}
            cardKey={`${selectedTopicId}-${currentCard.id ?? 0}`}
            isProcessing={isProcessing}
            isStarred={isCurrentCardStarred}
            onToggleStar={handleToggleStar}
            onToggleHide={handleToggleHide}
            onReviewCard={handleReviewCard}
          />
        ) : (
          <FlashcardCompletionState message={t('flashcard.studyCompleted')} />
        )}
      </FlashcardSessionLayout>
      <Toast message={toastMessage} variant="error" onDismiss={handleToastDismiss} />
    </>
  );
}

export default FlashcardStudyPage;
