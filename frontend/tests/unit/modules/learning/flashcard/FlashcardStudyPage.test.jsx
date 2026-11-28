import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FlashcardStudyPage from '@/modules/learning/flashcard/pages/FlashcardStudyPage'
import {
  getTopicsByDeckSlugApi,
  getUnlearnedCardsByTopicIdApi,
  submitSrsReviewApi,
  toggleStarApi,
  toggleHideApi,
} from '@/modules/learning/flashcard/flashcardApi'
import { tr, trRx } from '../../../../utils/i18n'

vi.mock('@/modules/learning/flashcard/flashcardApi', () => ({
  getTopicsByDeckSlugApi: vi.fn(),
  getUnlearnedCardsByTopicIdApi: vi.fn(),
  submitSrsReviewApi: vi.fn(),
  toggleStarApi: vi.fn(),
  toggleHideApi: vi.fn(),
}))

const mockTopics = [
  { id: 1, name: 'Topic 1', description: 'First topic', cardCount: 2, learnedCardCount: 0 },
  { id: 2, name: 'Topic 2', description: 'Second topic', cardCount: 1, learnedCardCount: 0 },
]

const mockTopic1Cards = [
  { id: 101, term: 'abandon', translation: 'từ bỏ', state: { hidden: false, learned: false, starred: false } },
  { id: 102, term: 'hidden-word', translation: 'ẩn', state: { hidden: true, learned: false, starred: false } },
  { id: 103, term: 'learned-word', translation: 'đã học', state: { hidden: false, learned: true, starred: false } },
  { id: 104, term: 'benefit', translation: 'lợi ích', state: { hidden: false, learned: false, starred: true } },
]

const mockTopic2Cards = [
  { id: 201, term: 'clarify', translation: 'làm rõ', state: { hidden: false, learned: false, starred: false } },
]

describe('FlashcardStudyPage', () => {
  const mockNavigate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    getTopicsByDeckSlugApi.mockResolvedValue({ success: true, data: mockTopics })
    getUnlearnedCardsByTopicIdApi.mockImplementation(async (topicId) => {
      if (topicId === 1) return { success: true, data: mockTopic1Cards }
      if (topicId === 2) return { success: true, data: mockTopic2Cards }
      return { success: true, data: [] }
    })
    submitSrsReviewApi.mockResolvedValue({ success: true })
    toggleStarApi.mockResolvedValue({ success: true })
    toggleHideApi.mockResolvedValue({ success: true })
  })

  it('shows PageLoading while fetching topics', () => {
    getTopicsByDeckSlugApi.mockImplementation(() => new Promise(() => {}))
    const { container } = render(<FlashcardStudyPage deckSlug="ielts-core" onNavigate={mockNavigate} />)
    expect(container.querySelector('.app-loading-spinner')).toBeInTheDocument()
  })

  it('renders EmptyState when topic fetch returns unsuccessful', async () => {
    const user = userEvent.setup()
    getTopicsByDeckSlugApi.mockResolvedValueOnce({ success: false })

    render(<FlashcardStudyPage deckSlug="ielts-core" returnTo="/decks?page=2" onNavigate={mockNavigate} />)

    expect(await screen.findByText(tr('common:loadError'))).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: tr('learning:flashcard.backToDecks') }))
    expect(mockNavigate).toHaveBeenCalledWith('/decks?page=2')
  })

  it('shows PREMIUM_REQUIRED error and navigates back to /decks by default', async () => {
    const user = userEvent.setup()
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    getTopicsByDeckSlugApi.mockRejectedValueOnce({
      response: { data: { code: 'PREMIUM_REQUIRED' } },
    })

    render(<FlashcardStudyPage deckSlug="pro-deck" onNavigate={mockNavigate} />)

    expect(await screen.findByText(tr('api:error.PREMIUM_REQUIRED'))).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: tr('learning:flashcard.backToDecks') }))
    expect(mockNavigate).toHaveBeenCalledWith('/decks')
    errorSpy.mockRestore()
  })

  it('renders empty topics placeholder when deck has no topics', async () => {
    getTopicsByDeckSlugApi.mockResolvedValueOnce({ success: true, data: [] })

    render(<FlashcardStudyPage deckSlug="empty-deck" onNavigate={mockNavigate} />)

    expect(await screen.findByText(tr('learning:flashcard.noTopicsInDeck'))).toBeInTheDocument()
    expect(screen.getByText(tr('learning:flashcard.noCardsToStudy'))).toBeInTheDocument()
  })

  it('loads topics and unlearned cards, filters hidden/learned cards, and switches topics', async () => {
    const user = userEvent.setup()
    render(<FlashcardStudyPage deckSlug="ielts-core" onNavigate={mockNavigate} />)

    expect(await screen.findByText('abandon')).toBeInTheDocument()
    expect(screen.queryByText('hidden-word')).not.toBeInTheDocument()
    expect(screen.queryByText('learned-word')).not.toBeInTheDocument()
    expect(screen.getByText(tr('learning:flashcard.overallProgress'))).toBeInTheDocument()

    // Switch to Topic 2
    await user.click(screen.getByText('Topic 2'))
    expect(await screen.findByText('clarify')).toBeInTheDocument()

    // Switch back to Topic 1 -> uses cached cards without re-fetching
    await user.click(screen.getByText('Topic 1'))
    expect(await screen.findByText('abandon')).toBeInTheDocument()
    expect(getUnlearnedCardsByTopicIdApi).toHaveBeenCalledTimes(2)
  })

  it('shows retry button when topic cards fail to load and retries on click', async () => {
    const user = userEvent.setup()
    getUnlearnedCardsByTopicIdApi
      .mockResolvedValueOnce({ success: false })
      .mockResolvedValueOnce({ success: true, data: mockTopic1Cards })

    render(<FlashcardStudyPage deckSlug="ielts-core" onNavigate={mockNavigate} />)

    expect(await screen.findByRole('button', { name: tr('common:retry') })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: tr('common:retry') }))

    expect(await screen.findByText('abandon')).toBeInTheDocument()
  })

  it('handles SRS review, star toggle, hide card, and completion state', async () => {
    const user = userEvent.setup()
    render(<FlashcardStudyPage deckSlug="ielts-core" onNavigate={mockNavigate} />)

    expect(await screen.findByText('abandon')).toBeInTheDocument()

    // Toggle star on first card
    await user.click(screen.getByTitle(tr('learning:flashcard.actions.star')))
    await waitFor(() => {
      expect(toggleStarApi).toHaveBeenCalledWith(101, true)
    })
    expect(screen.getByTitle(tr('learning:flashcard.actions.unstar'))).toBeInTheDocument()

    // Review first card (Good = 2) -> advances to second unlearned card ('benefit')
    await user.click(screen.getByText(tr('learning:flashcard.ratings.good')).closest('button'))
    await waitFor(() => {
      expect(submitSrsReviewApi).toHaveBeenCalledWith(101, 2)
    })
    expect(await screen.findByText('benefit')).toBeInTheDocument()

    // Mark second card as learned (hide) -> completes topic
    await user.click(screen.getByTitle(tr('learning:flashcard.actions.markLearned')))
    await waitFor(() => {
      expect(toggleHideApi).toHaveBeenCalledWith(104, true)
    })
    expect(await screen.findByText(tr('learning:flashcard.studyCompleted'))).toBeInTheDocument()
  })

  it('shows toast for RATE_LIMIT_EXCEEDED and EmptyState for DECK_NOT_FOUND on card actions', async () => {
    const user = userEvent.setup()
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    submitSrsReviewApi
      .mockRejectedValueOnce({ response: { data: { code: 'RATE_LIMIT_EXCEEDED' } } })
      .mockRejectedValueOnce({ response: { data: { code: 'DECK_NOT_FOUND' } } })

    render(<FlashcardStudyPage deckSlug="ielts-core" onNavigate={mockNavigate} />)
    expect(await screen.findByText('abandon')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: trRx('learning:flashcard.ratings.again') }))
    expect(await screen.findByRole('alert')).toHaveTextContent(tr('api:error.RATE_LIMIT_EXCEEDED'))

    await user.click(screen.getByRole('button', { name: trRx('learning:flashcard.ratings.again') }))
    expect(await screen.findByText(tr('api:error.DECK_NOT_FOUND'))).toBeInTheDocument()
    errorSpy.mockRestore()
  })

  it('navigates back when BackButton is clicked', async () => {
    const user = userEvent.setup()
    render(<FlashcardStudyPage deckSlug="ielts-core" returnTo="/decks/my" onNavigate={mockNavigate} />)

    await screen.findByText('abandon')
    await user.click(screen.getByRole('button', { name: trRx('common:back') }))
    expect(mockNavigate).toHaveBeenCalledWith('/decks/my')
  })
})
