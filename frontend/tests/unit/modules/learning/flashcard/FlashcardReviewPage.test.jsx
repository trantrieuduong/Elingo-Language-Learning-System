import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FlashcardReviewPage from '@/modules/learning/flashcard/pages/FlashcardReviewPage'
import {
  getCardsForReviewApi,
  submitSrsReviewApi,
  toggleStarApi,
  toggleHideApi,
} from '@/modules/learning/flashcard/flashcardApi'
import { tr, trRx } from '../../../../utils/i18n'

vi.mock('@/modules/learning/flashcard/flashcardApi', () => ({
  getCardsForReviewApi: vi.fn(),
  submitSrsReviewApi: vi.fn(),
  toggleStarApi: vi.fn(),
  toggleHideApi: vi.fn(),
}))

const mockDueCards = [
  {
    id: 1,
    term: 'eloquent',
    translation: 'hùng biện',
    state: { starred: false },
  },
  {
    card: { id: 2, term: 'pragmatic', translation: 'thực tế' },
    flagsStarred: true,
  },
]

describe('FlashcardReviewPage', () => {
  const mockNavigate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    getCardsForReviewApi.mockResolvedValue({ success: true, data: mockDueCards })
    submitSrsReviewApi.mockResolvedValue({ success: true })
    toggleStarApi.mockResolvedValue({ success: true })
    toggleHideApi.mockResolvedValue({ success: true })
  })

  it('shows PageLoading while fetching due cards', () => {
    getCardsForReviewApi.mockImplementation(() => new Promise(() => {}))
    const { container } = render(<FlashcardReviewPage onNavigate={mockNavigate} />)
    expect(container.querySelector('.app-loading-spinner')).toBeInTheDocument()
  })

  it('renders EmptyState on load error and navigates to /decks when action clicked', async () => {
    const user = userEvent.setup()
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    getCardsForReviewApi.mockRejectedValueOnce(new Error('Network error'))

    render(<FlashcardReviewPage onNavigate={mockNavigate} />)

    expect(await screen.findByText(tr('common:loadError'))).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: tr('learning:flashcard.backToDecks') }))
    expect(mockNavigate).toHaveBeenCalledWith('/decks')
    errorSpy.mockRestore()
  })

  it('renders EmptyState when API returns success=false', async () => {
    getCardsForReviewApi.mockResolvedValueOnce({ success: false })
    render(<FlashcardReviewPage onNavigate={mockNavigate} />)
    expect(await screen.findByText(tr('common:loadError'))).toBeInTheDocument()
  })

  it('renders completion state immediately when there are no due cards', async () => {
    getCardsForReviewApi.mockResolvedValueOnce({ success: true, data: [] })
    render(<FlashcardReviewPage onNavigate={mockNavigate} />)

    expect(await screen.findByText(tr('learning:flashcard.reviewCompleted'))).toBeInTheDocument()
  })

  it('renders due cards (direct & wrapped formats), handles star/review/hide, and completes session', async () => {
    const user = userEvent.setup()
    render(<FlashcardReviewPage onNavigate={mockNavigate} />)

    // First card ('eloquent') is displayed both in card and sidebar
    await waitFor(() => {
      expect(getCardsForReviewApi).toHaveBeenCalledWith(100)
    })
    expect(screen.getAllByText('eloquent')).toHaveLength(2)
    expect(screen.getByText(tr('learning:flashcard.reviewSessionProgress'))).toBeInTheDocument()
    expect(screen.getByText('0 / 2 (0%)')).toBeInTheDocument()

    // Toggle star on card 1
    await user.click(screen.getByTitle(tr('learning:flashcard.actions.star')))
    await waitFor(() => {
      expect(toggleStarApi).toHaveBeenCalledWith(1, true)
    })
    expect(screen.getByTitle(tr('learning:flashcard.actions.unstar'))).toBeInTheDocument()

    // Review card 1 (Easy = 3) -> moves to wrapped card 2 ('pragmatic', already starred)
    await user.click(screen.getByRole('button', { name: trRx('learning:flashcard.ratings.easy') }))
    await waitFor(() => {
      expect(submitSrsReviewApi).toHaveBeenCalledWith(1, 3)
    })
    expect(screen.getByText('1 / 2 (50%)')).toBeInTheDocument()
    expect(screen.getAllByText('pragmatic')).toHaveLength(2)
    expect(screen.getByTitle(tr('learning:flashcard.actions.unstar'))).toBeInTheDocument()

    // Hide card 2 -> completes review session
    await user.click(screen.getByTitle(tr('learning:flashcard.actions.markLearned')))
    await waitFor(() => {
      expect(toggleHideApi).toHaveBeenCalledWith(2, true)
    })
    expect(await screen.findByText(tr('learning:flashcard.reviewCompleted'))).toBeInTheDocument()
    expect(screen.getByText('2 / 2 (100%)')).toBeInTheDocument()
  })

  it('shows toast for generic card action error and EmptyState for PREMIUM_REQUIRED', async () => {
    const user = userEvent.setup()
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    submitSrsReviewApi
      .mockRejectedValueOnce({ response: { data: { code: 'RATE_LIMIT_EXCEEDED' } } })
      .mockRejectedValueOnce({ response: { data: { code: 'PREMIUM_REQUIRED' } } })

    render(<FlashcardReviewPage onNavigate={mockNavigate} />)
    await screen.findByText('0 / 2 (0%)')

    await user.click(screen.getByRole('button', { name: trRx('learning:flashcard.ratings.hard') }))
    expect(await screen.findByRole('alert')).toHaveTextContent(tr('api:error.RATE_LIMIT_EXCEEDED'))

    await user.click(screen.getByRole('button', { name: trRx('learning:flashcard.ratings.hard') }))
    expect(await screen.findByText(tr('api:error.PREMIUM_REQUIRED'))).toBeInTheDocument()
    errorSpy.mockRestore()
  })

  it('navigates to /decks when BackButton is clicked', async () => {
    const user = userEvent.setup()
    render(<FlashcardReviewPage onNavigate={mockNavigate} />)

    await screen.findByText('0 / 2 (0%)')
    await user.click(screen.getByRole('button', { name: trRx('common:back') }))
    expect(mockNavigate).toHaveBeenCalledWith('/decks')
  })
})
