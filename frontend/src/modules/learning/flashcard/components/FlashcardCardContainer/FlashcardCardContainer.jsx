import { useTranslation } from 'react-i18next';
import VocabularyFlashCard from '../../../../../components/VocabularyFlashCard/VocabularyFlashCard';
import FlashcardStarButton from '../../../../../components/FlashcardStarButton/FlashcardStarButton';
import useFlashcardAudio from '../../hooks/useFlashcardAudio';
import './FlashcardCardContainer.css';

/**
 * Vùng hiển thị thẻ Flashcard 3D kèm nút gắn sao và đánh dấu đã thuộc ở góc thẻ.
 * @param {Object} card - Dữ liệu thẻ flashcard hiện tại
 * @param {string} cardKey - Key định danh để reset trạng thái lật của thẻ
 * @param {boolean} isProcessing - Trạng thái đang gửi API
 * @param {boolean} isStarred - Thẻ có được gắn sao không
 * @param {Function} onToggleStar - Hàm xử lý bật/tắt gắn sao
 * @param {Function} onToggleHide - Hàm xử lý đánh dấu thẻ đã thuộc
 */
export default function FlashcardCardContainer({
  card,
  cardKey,
  isProcessing = false,
  isStarred,
  onToggleStar,
  onToggleHide,
}) {
  const { t } = useTranslation('learning');
  const resolvedCardKey = cardKey !== undefined ? cardKey : (card?.id ?? 0);
  const { playingAudioKey, handlePlayAudio } = useFlashcardAudio(resolvedCardKey);

  if (!card) return null;

  return (
    <div className="flashcard-card-container">
      <div className="flashcard-card-actions">
        <FlashcardStarButton
          isStarred={isStarred}
          isProcessing={isProcessing}
          onToggle={onToggleStar}
          className="flashcard-card-btn flashcard-card-btn-star"
          stopCardInteraction
        />

        <button
          type="button"
          className="btn-icon flashcard-card-btn flashcard-card-btn-learned"
          onClick={(e) => {
            e.stopPropagation();
            e.preventDefault();
            if (onToggleHide) onToggleHide();
          }}
          onPointerDown={(e) => e.stopPropagation()}
          disabled={isProcessing}
          title={t('flashcard.actions.markLearned')}
        >
          <span className="material-symbols-outlined">check_circle</span>
        </button>
      </div>

      <VocabularyFlashCard
        key={resolvedCardKey}
        card={card}
        onPlayAudio={handlePlayAudio}
        playingAudioKey={playingAudioKey}
      />
    </div>
  );
}
