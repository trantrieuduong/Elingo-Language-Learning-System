import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import TopicListPage from '@/modules/vocabulary/pages/TopicListPage/TopicListPage'
import { toggleStarApi } from '@/modules/learning/flashcard/flashcardApi'
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
} from '@/modules/vocabulary/vocabularyApi'
import { MOCK_CARD, MOCK_DECK, MOCK_TOPIC } from '../../utils/mockData'
import { tr } from '../../utils/i18n'

vi.mock('@/modules/vocabulary/vocabularyApi', () => ({
  getDeckByIdApi: vi.fn(),
  getTopicsApi: vi.fn(),
  createTopicApi: vi.fn(),
  updateTopicApi: vi.fn(),
  deleteTopicApi: vi.fn(),
  updateUserDeckApi: vi.fn(),
  getCardsByTopicApi: vi.fn(),
  createUserCardApi: vi.fn(),
  updateUserCardApi: vi.fn(),
  deleteUserCardApi: vi.fn(),
}))

vi.mock('@/modules/learning/flashcard/flashcardApi', () => ({
  toggleStarApi: vi.fn(),
}))

describe('TopicListPage (unit)', () => {
  const mockNavigate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    getDeckByIdApi.mockResolvedValue({ success: true, data: MOCK_DECK })
    getTopicsApi.mockResolvedValue({ success: true, data: [MOCK_TOPIC] })
    getCardsByTopicApi.mockResolvedValue({ success: true, data: [MOCK_CARD] })
  })

  describe('Deck header & loading states', () => {
    it('renders error EmptyState when getDeckByIdApi fails', async () => {
      getDeckByIdApi.mockRejectedValue(new Error('Failed'))
      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)

      expect(await screen.findByText(tr('common:loadError'))).toBeInTheDocument()
    })

    it('renders deck title and navigates on Back and Study Now clicks', async () => {
      const user = userEvent.setup()
      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)

      expect(await screen.findByRole('heading', { name: MOCK_DECK.title })).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.studyNow')) }))
      expect(mockNavigate).toHaveBeenCalledWith(`/decks/${MOCK_DECK.slug}`, {
        returnTo: '/decks/deck-1/topics?tab=my-decks',
      })

      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:back')) }))
      expect(mockNavigate).toHaveBeenCalledWith('/decks?tab=my-decks')
    })

    it('calls updateUserDeckApi when editing deck from header', async () => {
      const user = userEvent.setup()
      updateUserDeckApi.mockResolvedValue({
        success: true,
        data: { ...MOCK_DECK, title: 'Updated Deck' },
      })

      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      await screen.findByRole('heading', { name: MOCK_DECK.title })

      await user.click(screen.getByTitle(tr('learning:userDeck.editDeck')))
      const titleInput = screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))
      await user.clear(titleInput)
      await user.type(titleInput, 'Updated Deck')
      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) }))

      await waitFor(() => {
        expect(updateUserDeckApi).toHaveBeenCalledWith('deck-1', {
          title: 'Updated Deck',
          description: MOCK_DECK.description,
        })
      })
      expect(await screen.findByRole('heading', { name: 'Updated Deck' })).toBeInTheDocument()
    })
  })

  describe('Topic CRUD', () => {
    it('renders EmptyState when getTopicsApi returns empty array', async () => {
      getTopicsApi.mockResolvedValue({ success: true, data: [] })
      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)

      expect(await screen.findByText(tr('learning:topic.emptyTitle'))).toBeInTheDocument()
    })

    it('calls createTopicApi, updateTopicApi, and deleteTopicApi on topic actions', async () => {
      const user = userEvent.setup()
      createTopicApi.mockResolvedValue({ success: true, data: { id: 2, name: 'Food' } })
      updateTopicApi.mockResolvedValue({ success: true, data: { ...MOCK_TOPIC, name: 'Pets' } })
      deleteTopicApi.mockResolvedValue({ success: true })

      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      await screen.findByText(MOCK_TOPIC.name)

      await user.click(screen.getByRole('button', { name: new RegExp(tr('learning:topic.createBtn')) }))
      await user.type(screen.getByLabelText(tr('learning:topic.labelName')), 'Food')
      const topicSubmitBtns = screen.getAllByRole('button', {
        name: new RegExp(tr('learning:topic.submitCreate')),
      })
      await user.click(topicSubmitBtns[topicSubmitBtns.length - 1])
      expect(createTopicApi).toHaveBeenCalledWith({ name: 'Food', deckId: 'deck-1' })

      await user.click(screen.getByTitle(tr('learning:topic.editTopic')))
      const editInput = screen.getByLabelText(tr('learning:topic.labelName'))
      await user.clear(editInput)
      await user.type(editInput, 'Pets')
      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) }))
      expect(updateTopicApi).toHaveBeenCalledWith(MOCK_TOPIC.id, { name: 'Pets' })

      await user.click(screen.getByTitle(tr('learning:topic.deleteTopic')))
      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:topic.deleteConfirm')) })
      )
      expect(deleteTopicApi).toHaveBeenCalledWith(MOCK_TOPIC.id)
    })
  })

  describe('Card CRUD & Star toggle', () => {
    it('calls createUserCardApi, updateUserCardApi, and deleteUserCardApi on card actions', async () => {
      const user = userEvent.setup()
      createUserCardApi.mockResolvedValue({ success: true, data: { id: 'card-2' } })
      updateUserCardApi.mockResolvedValue({ success: true, data: MOCK_CARD })
      deleteUserCardApi.mockResolvedValue({ success: true })

      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      expect(await screen.findByText(MOCK_CARD.term)).toBeInTheDocument()

      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:userCards.createCard')) })
      )
      await user.type(screen.getByLabelText(tr('learning:userCards.termLabel')), 'world')
      await user.type(screen.getByLabelText(tr('learning:userCards.translation')), 'thế giới')
      const modalSubmitBtns = screen.getAllByRole('button', {
        name: new RegExp(tr('learning:userCards.createCard')),
      })
      await user.click(modalSubmitBtns[modalSubmitBtns.length - 1])
      expect(createUserCardApi).toHaveBeenCalledWith(
        expect.objectContaining({
          topicId: MOCK_TOPIC.id,
          term: 'world',
          translation: 'thế giới',
        })
      )

      const editButtons = await screen.findAllByTitle(tr('common:edit'))
      await user.click(editButtons[0])
      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) }))
      expect(updateUserCardApi).toHaveBeenCalledWith(
        MOCK_CARD.id,
        expect.objectContaining({ term: MOCK_CARD.term })
      )

      const deleteButtons = await screen.findAllByTitle(tr('common:delete'))
      await user.click(deleteButtons[0])
      const confirmDeleteBtns = screen.getAllByRole('button', {
        name: new RegExp(tr('common:delete')),
      })
      await user.click(confirmDeleteBtns[confirmDeleteBtns.length - 1])
      expect(deleteUserCardApi).toHaveBeenCalledWith(MOCK_CARD.id)
    })

    it('calls toggleStarApi and updates card starred state on success', async () => {
      const user = userEvent.setup()
      toggleStarApi.mockResolvedValue({ success: true })

      const { container } = render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      await screen.findByText(MOCK_CARD.term)

      await user.click(container.querySelector('.user-card-item__star-toggle'))
      await waitFor(() => {
        expect(toggleStarApi).toHaveBeenCalledWith(MOCK_CARD.id, true)
        expect(container.querySelector('.user-card-item__star')).toBeInTheDocument()
      })
    })
  })
})
