import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from '@/i18n';
import FlashcardStudyPage from '@/modules/learning/flashcard/pages/FlashcardStudyPage';
import {
  getTopicsByDeckSlugApi,
  getUnlearnedCardsByTopicIdApi,
} from '@/modules/learning/flashcard/flashcardApi';

vi.mock('@/modules/learning/flashcard/flashcardApi', () => ({
  getTopicsByDeckSlugApi: vi.fn(),
  getUnlearnedCardsByTopicIdApi: vi.fn(),
  submitSrsReviewApi: vi.fn(),
  toggleStarApi: vi.fn(),
  toggleHideApi: vi.fn(),
}));

describe('FlashcardStudyPage locale changes', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('vi');
    getTopicsByDeckSlugApi.mockResolvedValue({
      success: true,
      data: [
        { id: 1, name: 'Topic one', cardCount: 0 },
        { id: 2, name: 'Topic two', cardCount: 0 },
      ],
    });
    getUnlearnedCardsByTopicIdApi.mockResolvedValue({ success: true, data: [] });
  });

  it('keeps the selected topic and does not reload topics when locale changes', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <FlashcardStudyPage deckSlug="test-deck" onNavigate={vi.fn()} />
    );

    await screen.findByText('Topic one');
    await user.click(screen.getByText('Topic two'));
    await waitFor(() => {
      expect(getUnlearnedCardsByTopicIdApi).toHaveBeenCalledWith(2);
    });

    await act(async () => {
      await i18n.changeLanguage('en');
    });

    expect(getTopicsByDeckSlugApi).toHaveBeenCalledTimes(1);
    expect(container.querySelector('.flashcard-topic-item.active')).toHaveTextContent('Topic two');
  });
});
