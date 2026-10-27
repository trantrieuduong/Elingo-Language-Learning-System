import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  getCardsForReviewApi,
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
import './FlashcardReviewPage.css';

/**
 * FlashcardReviewPage — Trang ôn tập flashcard đến hạn (SM-2) tại route /review.
 * Hiển thị nút quay lại, tiến độ phiên, danh sách thẻ còn lại và khu vực ôn tập.
 *
 * @param {Function} onNavigate - Hàm điều hướng custom router từ App.jsx
 */
function FlashcardReviewPage({ onNavigate }) {
  const { t } = useTranslation(['learning', 'api', 'common']);
  const [reviewCards, setReviewCards] = useState([]);
  const [initialReviewTotal, setInitialReviewTotal] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    const fetchDueReviewCards = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await getCardsForReviewApi(100);
        if (res && res.success) {
          const rawList = Array.isArray(res.data) ? res.data : [];
          const formattedCards = rawList
            .map((item) => {
              // Trường hợp backend trả về CardResponse trực tiếp
              if (item && item.id) {
                return {
                  ...item,
                  flagsStarred: Boolean(item.state?.starred ?? item.flagsStarred),
                };
              }
              // Trường hợp backend trả về bọc { card: {...}, flagsStarred }
              if (item?.card) {
                return {
                  ...item.card,
                  flagsStarred: Boolean(item.card?.state?.starred ?? item.flagsStarred),
                };
              }
              return item;
            })
            .filter(Boolean);
          setReviewCards(formattedCards);
          setInitialReviewTotal(formattedCards.length);
        } else {
          setError({
            message: t('common:loadError'),
            actionLabel: t('flashcard.backToDecks'),
            actionPath: '/decks',
          });
        }
      } catch (err) {
        console.error('Lỗi khi tải flashcard đến hạn ôn tập:', err);
        setError({
          message: t('common:loadError'),
          actionLabel: t('flashcard.backToDecks'),
          actionPath: '/decks',
        });
      } finally {
        setLoading(false);
      }
    };

    fetchDueReviewCards();
  }, [t]);

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
        actionPath: '/decks',
      });
    } else if (errorCode === 'DECK_NOT_FOUND' || errorCode === 'DECK_ACCESS_DENIED') {
      setError({
        message: errorMessage,
        actionLabel: t('flashcard.backToDecks'),
        actionPath: '/decks',
      });
    } else {
      setToastMessage(errorMessage);
    }
  };

  /**
   * Đánh giá mức độ ghi nhớ SM-2 (0: Học lại, 1: Khó, 2: Dễ, 3: Quá dễ)
   */
  const handleReviewCard = async (grade, gradeLabel) => {
    if (reviewCards.length === 0 || isProcessing) return;

    const currentCard = reviewCards[0];
    const cardId = currentCard?.id;
    if (!cardId) return;

    setIsProcessing(true);
    try {
      const res = await submitSrsReviewApi(cardId, grade);
      if (res && res.success) {
        setReviewCards((prev) => prev.slice(1));
      } else {
        console.error('Đánh giá thẻ không thành công:', res);
      }
    } catch (err) {
      console.error(`Lỗi khi gửi đánh giá SRS thẻ ID ${cardId} (${gradeLabel}):`, err);
      handleCardActionError(err);
    } finally {
      setIsProcessing(false);
    }
  };

  /**
   * Đổi trạng thái gắn sao cho thẻ hiện tại
   */
  const handleToggleStar = async () => {
    if (reviewCards.length === 0 || isProcessing) return;

    const currentCard = reviewCards[0];
    const cardId = currentCard?.id;
    if (!cardId) return;

    const isStarred = Boolean(currentCard.state?.starred ?? currentCard.flagsStarred);
    const nextStarredState = !isStarred;

    setIsProcessing(true);
    try {
      const res = await toggleStarApi(cardId, nextStarredState);
      if (res && res.success) {
        setReviewCards((prev) => {
          if (prev.length === 0) return prev;
          const updated = [...prev];
          updated[0] = {
            ...updated[0],
            flagsStarred: nextStarredState,
            state: updated[0].state
              ? { ...updated[0].state, starred: nextStarredState }
              : { starred: nextStarredState },
          };
          return updated;
        });
      } else {
        console.error('Đổi trạng thái gắn sao không thành công:', res);
      }
    } catch (err) {
      console.error(`Lỗi khi đổi trạng thái gắn sao thẻ ID ${cardId}:`, err);
      handleCardActionError(err);
    } finally {
      setIsProcessing(false);
    }
  };

  /**
   * Đánh dấu thẻ hiện tại đã thuộc và bỏ khỏi phiên ôn tập
   */
  const handleToggleHide = async () => {
    if (reviewCards.length === 0 || isProcessing) return;

    const currentCard = reviewCards[0];
    const cardId = currentCard?.id;
    if (!cardId) return;

    setIsProcessing(true);
    try {
      const res = await toggleHideApi(cardId, true);
      if (res && res.success) {
        setReviewCards((prev) => prev.slice(1));
      } else {
        console.error('Ẩn thẻ không thành công:', res);
      }
    } catch (err) {
      console.error(`Lỗi khi ẩn thẻ ID ${cardId}:`, err);
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

  const currentCard = reviewCards[0] || null;
  const isCurrentCardStarred = Boolean(currentCard?.state?.starred ?? currentCard?.flagsStarred);
  const currentCardId = currentCard?.id;
  const reviewedCount = initialReviewTotal - reviewCards.length;

  return (
    <>
      <FlashcardSessionLayout
        layoutClassName="flashcard-review-layout"
        sidebarClassName="flashcard-sidebar flashcard-review-sidebar"
        backLabel={t('common:back')}
        onBack={() => onNavigate?.('/decks')}
        progress={
          <div className="glass-card flashcard-global-progress">
            <ProgressBar
              title={t('flashcard.reviewSessionProgress')}
              current={reviewedCount}
              total={initialReviewTotal}
            />
          </div>
        }
        sidebar={reviewCards.map((card, index) => {
          return (
            <div
              key={card.id ?? index}
              className="glass-card flashcard-session-item flashcard-review-item"
            >
              <div className="flashcard-session-item-header">
                <h3 className="flashcard-session-item-title">
                  {card.term || t('flashcard.card.imageAlt')}
                </h3>
              </div>
            </div>
          );
        })}
      >
        {currentCard ? (
          <FlashcardStudyArea
            card={currentCard}
            cardKey={`review-card-${currentCardId}`}
            isProcessing={isProcessing}
            isStarred={isCurrentCardStarred}
            onToggleStar={handleToggleStar}
            onToggleHide={handleToggleHide}
            onReviewCard={handleReviewCard}
          />
        ) : (
          <FlashcardCompletionState message={t('flashcard.reviewCompleted')} />
        )}
      </FlashcardSessionLayout>
      <Toast message={toastMessage} variant="error" onDismiss={handleToastDismiss} />
    </>
  );
}

export default FlashcardReviewPage;
