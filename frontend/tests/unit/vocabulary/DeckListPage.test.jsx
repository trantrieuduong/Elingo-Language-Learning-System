import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import DeckListPage from '@/modules/vocabulary/pages/DeckListPage/DeckListPage'
import { useAuth } from '@/context/AuthContext'
import {
  getDecksApi,
  getUserDecksApi,
  createUserDeckApi,
  updateUserDeckApi,
  publishUserDeckApi,
  deleteUserDeckApi,
  getTagsApi,
  getCefrLevelsApi,
} from '@/modules/vocabulary/vocabularyApi'
import {
  MOCK_AUTH_CONTEXT_DEFAULT,
  MOCK_AUTH_CONTEXT_LOGGED_IN,
  MOCK_DECK,
  MOCK_PUBLISHED_DECK,
} from '../../utils/mockData'
import { tr } from '../../utils/i18n'

vi.mock('@/context/AuthContext')
vi.mock('@/modules/vocabulary/vocabularyApi', () => ({
  getDecksApi: vi.fn(),
  getUserDecksApi: vi.fn(),
  createUserDeckApi: vi.fn(),
  updateUserDeckApi: vi.fn(),
  publishUserDeckApi: vi.fn(),
  deleteUserDeckApi: vi.fn(),
  getTagsApi: vi.fn(),
  getCefrLevelsApi: vi.fn(),
}))

describe('DeckListPage (unit)', () => {
  const mockNavigate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    window.history.pushState({}, '', '/decks')
    useAuth.mockReturnValue(MOCK_AUTH_CONTEXT_DEFAULT)
    getTagsApi.mockResolvedValue({ success: true, data: [{ id: 't1', label: 'Du lịch' }] })
    getCefrLevelsApi.mockResolvedValue({ success: true, data: [{ id: 'a1', label: 'A1' }] })
    getDecksApi.mockResolvedValue({ success: true, data: { content: [MOCK_DECK], totalPages: 1 } })
    getUserDecksApi.mockResolvedValue({ success: true, data: { content: [], totalPages: 1 } })
  })

  describe('Initial render & tabs', () => {
    it('hides my-decks tab when user is null and calls getDecksApi with ownerType=SYSTEM', async () => {
      render(<DeckListPage onNavigate={mockNavigate} />)

      expect(screen.getByText(tr('learning:userDeck.tabs.systemDecks'))).toBeInTheDocument()
      expect(screen.getByText(tr('learning:userDeck.tabs.explore'))).toBeInTheDocument()
      expect(screen.queryByText(tr('learning:userDeck.tabs.myDecks'))).not.toBeInTheDocument()

      expect(await screen.findByText(MOCK_DECK.title)).toBeInTheDocument()
      expect(getDecksApi).toHaveBeenCalledWith(
        expect.objectContaining({ ownerType: 'SYSTEM', page: 1, limit: 9 })
      )
    })

    it('renders EmptyState when getDecksApi returns empty content', async () => {
      getDecksApi.mockResolvedValue({ success: true, data: { content: [], totalPages: 1 } })
      render(<DeckListPage onNavigate={mockNavigate} />)

      expect(await screen.findByText(tr('learning:deckList.emptyTitle'))).toBeInTheDocument()
    })

    it('renders error EmptyState when getDecksApi fails and retries on click', async () => {
      const user = userEvent.setup()
      getDecksApi
        .mockRejectedValueOnce(new Error('Network error'))
        .mockResolvedValueOnce({ success: true, data: { content: [MOCK_DECK], totalPages: 1 } })

      render(<DeckListPage onNavigate={mockNavigate} />)
      expect(await screen.findByText(tr('common:loadError'))).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: tr('common:retry') }))
      expect(await screen.findByText(MOCK_DECK.title)).toBeInTheDocument()
    })
  })

  describe('Authenticated my-decks CRUD', () => {
    beforeEach(() => {
      useAuth.mockReturnValue(MOCK_AUTH_CONTEXT_LOGGED_IN)
      getUserDecksApi.mockResolvedValue({
        success: true,
        data: { content: [MOCK_DECK], totalPages: 1 },
      })
    })

    it('calls getUserDecksApi when switching to my-decks tab', async () => {
      const user = userEvent.setup()
      render(<DeckListPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(tr('learning:userDeck.tabs.myDecks')))
      expect(await screen.findByText(MOCK_DECK.title)).toBeInTheDocument()
      expect(getUserDecksApi).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'ALL', page: 1, limit: 9 })
      )
    })

    it('calls createUserDeckApi and shows success toast on create submit', async () => {
      const user = userEvent.setup()
      createUserDeckApi.mockResolvedValue({ success: true, data: { id: 'new-1' } })

      render(<DeckListPage onNavigate={mockNavigate} />)
      await user.click(screen.getByText(tr('learning:userDeck.tabs.myDecks')))
      await screen.findByText(MOCK_DECK.title)

      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.createBtn')) })
      )
      await user.type(screen.getByLabelText(tr('learning:userDeck.form.titleLabel')), 'New Deck')
      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.form.createDeck')) })
      )

      await waitFor(() => {
        expect(createUserDeckApi).toHaveBeenCalledWith({ title: 'New Deck', description: '' })
      })
      expect(await screen.findByText(tr('learning:userDeck.createSuccess'))).toBeInTheDocument()
    })

    it('calls publishUserDeckApi, updateUserDeckApi, and deleteUserDeckApi from deck options', async () => {
      const user = userEvent.setup()
      publishUserDeckApi.mockResolvedValue({ success: true })
      updateUserDeckApi.mockResolvedValue({ success: true })
      deleteUserDeckApi.mockResolvedValue({ success: true })

      const { container } = render(<DeckListPage onNavigate={mockNavigate} />)
      await user.click(screen.getByText(tr('learning:userDeck.tabs.myDecks')))
      await screen.findByText(MOCK_DECK.title)

      await user.click(container.querySelector('.user-deck-card__options-btn'))
      await user.click(screen.getByText(tr('learning:userDeck.publishDeck')))
      expect(publishUserDeckApi).toHaveBeenCalledWith(MOCK_DECK.id, true)

      await user.click(container.querySelector('.user-deck-card__options-btn'))
      await user.click(screen.getByText(tr('learning:userDeck.editDeck')))
      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) }))
      expect(updateUserDeckApi).toHaveBeenCalledWith(MOCK_DECK.id, {
        title: MOCK_DECK.title,
        description: MOCK_DECK.description,
      })

      await user.click(container.querySelector('.user-deck-card__options-btn'))
      await user.click(screen.getByText(tr('learning:userDeck.deleteDeck')))
      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.deleteConfirm')) })
      )
      expect(deleteUserDeckApi).toHaveBeenCalledWith(MOCK_DECK.id)
    })

    it('filters my-decks by status pills', async () => {
      const user = userEvent.setup()
      render(<DeckListPage onNavigate={mockNavigate} />)

      await user.click(screen.getByText(tr('learning:userDeck.tabs.myDecks')))
      await screen.findByText(MOCK_DECK.title)

      await user.click(screen.getByRole('button', { name: tr('learning:userDeck.statusPublished') }))
      await waitFor(() => {
        expect(getUserDecksApi).toHaveBeenLastCalledWith(
          expect.objectContaining({ status: 'PUBLISHED', page: 1 })
        )
      })
    })
  })

  describe('Navigation, search & pagination', () => {
    it('navigates to /decks/:slug when a public deck card is clicked', async () => {
      const user = userEvent.setup()
      render(<DeckListPage onNavigate={mockNavigate} />)

      const title = await screen.findByText(MOCK_DECK.title)
      await user.click(title)
      expect(mockNavigate).toHaveBeenCalledWith(`/decks/${MOCK_DECK.slug}`, {
        returnTo: '/decks?tab=system-decks',
      })
    })

    it('switches to community-decks with ownerType=USER and shows options only for non-owned decks', async () => {
      const user = userEvent.setup()
      useAuth.mockReturnValue(MOCK_AUTH_CONTEXT_LOGGED_IN)
      getDecksApi.mockResolvedValue({
        success: true,
        data: { content: [MOCK_DECK, MOCK_PUBLISHED_DECK], totalPages: 1 },
      })

      const { container } = render(<DeckListPage onNavigate={mockNavigate} />)
      await user.click(screen.getByText(tr('learning:userDeck.tabs.explore')))

      await waitFor(() => {
        expect(getDecksApi).toHaveBeenLastCalledWith(
          expect.objectContaining({ ownerType: 'USER' })
        )
      })
      expect(container.querySelectorAll('.vocabulary-card__options-btn')).toHaveLength(1)
    })

    it('calls getDecksApi with page=2 when pagination page 2 is clicked', async () => {
      const user = userEvent.setup()
      getDecksApi.mockResolvedValue({
        success: true,
        data: { content: [MOCK_DECK], totalPages: 2 },
      })

      render(<DeckListPage onNavigate={mockNavigate} />)
      await screen.findByText(MOCK_DECK.title)

      await user.click(screen.getByRole('button', { name: '2' }))
      await waitFor(() => {
        expect(getDecksApi).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2 }))
      })
    })
  })
})
