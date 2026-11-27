import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import TopicListPage from '@/modules/vocabulary/pages/TopicListPage/TopicListPage'
import { toggleStarApi } from '@/modules/learning/flashcard/flashcardApi'
import { server } from '../../mocks/server'
import { MOCK_CARD, MOCK_DECK, MOCK_TOPIC } from '../../utils/mockData'
import { tr } from '../../utils/i18n'

vi.mock('@/modules/learning/flashcard/flashcardApi', () => ({
  toggleStarApi: vi.fn(),
}))

const API_URL = import.meta.env.VITE_API_URL

describe('Vocabulary topic and card flow (integration)', () => {
  const mockNavigate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Deck loading & error states', () => {
    it('renders error EmptyState when fetching deck fails', async () => {
      server.use(
        http.get(`${API_URL}/decks/:deckId`, () =>
          HttpResponse.json({ success: false }, { status: 500 })
        )
      )

      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      expect(await screen.findByText(tr('common:loadError'))).toBeInTheDocument()
    })

    it('renders deck header with title and navigates correctly on Back and Study Now clicks', async () => {
      const user = userEvent.setup()
      server.use(
        http.get(`${API_URL}/decks/:deckId`, () =>
          HttpResponse.json({ success: true, data: MOCK_DECK })
        )
      )

      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      expect(await screen.findByRole('heading', { name: MOCK_DECK.title })).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.studyNow')) }))
      expect(mockNavigate).toHaveBeenCalledWith(`/decks/${MOCK_DECK.slug}`, {
        returnTo: '/decks/deck-1/topics?tab=my-decks',
      })

      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:back')) }))
      expect(mockNavigate).toHaveBeenCalledWith('/decks?tab=my-decks')
    })
  })

  describe('Topic list & CRUD', () => {
    beforeEach(() => {
      server.use(
        http.get(`${API_URL}/decks/:deckId`, () =>
          HttpResponse.json({ success: true, data: MOCK_DECK })
        )
      )
    })

    it('renders EmptyState when deck has no topics', async () => {
      server.use(
        http.get(`${API_URL}/decks/:deckId/topics`, () =>
          HttpResponse.json({ success: true, data: [] })
        )
      )

      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      expect(await screen.findByText(tr('learning:topic.emptyTitle'))).toBeInTheDocument()
    })

    it('auto-selects the first topic and allows switching to another topic', async () => {
      const user = userEvent.setup()
      const secondTopic = { ...MOCK_TOPIC, id: 2, name: 'Travel' }
      const requestedTopicIds = []

      server.use(
        http.get(`${API_URL}/decks/:deckId/topics`, () =>
          HttpResponse.json({ success: true, data: [MOCK_TOPIC, secondTopic] })
        ),
        http.get(`${API_URL}/topics/:topicId/cards`, ({ params }) => {
          requestedTopicIds.push(params.topicId)
          return HttpResponse.json({ success: true, data: [] })
        })
      )

      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      expect(
        await screen.findByText(tr('learning:userCards.cardsInTopic', { topic: MOCK_TOPIC.name }))
      ).toBeInTheDocument()

      await user.click(screen.getByText(secondTopic.name))
      expect(
        await screen.findByText(tr('learning:userCards.cardsInTopic', { topic: secondTopic.name }))
      ).toBeInTheDocument()
      expect(requestedTopicIds).toEqual(['1', '2'])
    })

    it('creates, updates, and deletes a topic successfully', async () => {
      const user = userEvent.setup()
      let currentTopics = [MOCK_TOPIC]

      server.use(
        http.get(`${API_URL}/decks/:deckId/topics`, () =>
          HttpResponse.json({ success: true, data: currentTopics })
        ),
        http.post(`${API_URL}/topics`, async ({ request }) => {
          const body = await request.json()
          const created = { id: 2, name: body.name, slug: 'new-topic' }
          currentTopics = [...currentTopics, created]
          return HttpResponse.json({ success: true, data: created })
        }),
        http.put(`${API_URL}/topics/:topicId`, async ({ request }) => {
          const body = await request.json()
          currentTopics = currentTopics.map((t) => (t.id === 1 ? { ...t, name: body.name } : t))
          return HttpResponse.json({ success: true, data: currentTopics[0] })
        }),
        http.delete(`${API_URL}/topics/:topicId`, () => {
          currentTopics = currentTopics.filter((t) => t.id !== 1)
          return HttpResponse.json({ success: true })
        })
      )

      render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      await screen.findByText(MOCK_TOPIC.name)

      await user.click(screen.getByRole('button', { name: new RegExp(tr('learning:topic.createBtn')) }))
      await user.type(screen.getByLabelText(tr('learning:topic.labelName')), 'Food')
      const topicSubmitBtns = screen.getAllByRole('button', {
        name: new RegExp(tr('learning:topic.submitCreate')),
      })
      await user.click(topicSubmitBtns[topicSubmitBtns.length - 1])
      expect(await screen.findByText(tr('learning:topic.createSuccess'))).toBeInTheDocument()
      expect(await screen.findByText('Food')).toBeInTheDocument()

      await user.click(screen.getByTitle(tr('learning:topic.editTopic')))
      const editInput = screen.getByLabelText(tr('learning:topic.labelName'))
      await user.clear(editInput)
      await user.type(editInput, 'Wild Animals')
      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) }))
      expect(await screen.findByText(tr('learning:topic.updateSuccess'))).toBeInTheDocument()
      expect(await screen.findByText('Wild Animals')).toBeInTheDocument()

      await user.click(screen.getByTitle(tr('learning:topic.deleteTopic')))
      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:topic.deleteConfirm')) })
      )
      expect(await screen.findByText(tr('learning:topic.deleteSuccess'))).toBeInTheDocument()
    })
  })

  describe('Card list, CRUD & Star toggle', () => {
    beforeEach(() => {
      server.use(
        http.get(`${API_URL}/decks/:deckId`, () =>
          HttpResponse.json({ success: true, data: MOCK_DECK })
        ),
        http.get(`${API_URL}/decks/:deckId/topics`, () =>
          HttpResponse.json({ success: true, data: [MOCK_TOPIC] })
        )
      )
    })

    it('displays card list and creates, edits, and deletes cards successfully', async () => {
      const user = userEvent.setup()
      let currentCards = [MOCK_CARD]

      server.use(
        http.get(`${API_URL}/topics/:topicId/cards`, () =>
          HttpResponse.json({ success: true, data: currentCards })
        ),
        http.post(`${API_URL}/cards`, async ({ request }) => {
          const body = await request.json()
          const created = { id: 'card-new', ...body, state: { starred: false } }
          currentCards = [...currentCards, created]
          return HttpResponse.json({ success: true, data: created })
        }),
        http.put(`${API_URL}/cards/:cardId`, async ({ request }) => {
          const body = await request.json()
          currentCards = currentCards.map((c) => (c.id === 'card-1' ? { ...c, ...body } : c))
          return HttpResponse.json({ success: true, data: currentCards[0] })
        }),
        http.delete(`${API_URL}/cards/:cardId`, () => {
          currentCards = []
          return HttpResponse.json({ success: true })
        })
      )

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

      expect(await screen.findByText(tr('learning:userCards.createSuccess'))).toBeInTheDocument()
      expect(await screen.findByText('world')).toBeInTheDocument()

      const editButtons = screen.getAllByTitle(tr('common:edit'))
      await user.click(editButtons[0])
      const termInput = screen.getByLabelText(tr('learning:userCards.termLabel'))
      await user.clear(termInput)
      await user.type(termInput, 'hi')
      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) }))
      expect(await screen.findByText(tr('learning:userCards.updateSuccess'))).toBeInTheDocument()

      const deleteButtons = screen.getAllByTitle(tr('common:delete'))
      await user.click(deleteButtons[0])
      const confirmDeleteBtns = screen.getAllByRole('button', {
        name: new RegExp(tr('common:delete')),
      })
      await user.click(confirmDeleteBtns[confirmDeleteBtns.length - 1])
      expect(await screen.findByText(tr('learning:userCards.deleteSuccess'))).toBeInTheDocument()
    })

    it('updates starred state directly on success and shows error toast on failure', async () => {
      const user = userEvent.setup()
      server.use(
        http.get(`${API_URL}/topics/:topicId/cards`, () =>
          HttpResponse.json({ success: true, data: [MOCK_CARD] })
        )
      )

      toggleStarApi
        .mockResolvedValueOnce({ success: true })
        .mockResolvedValueOnce({ success: false, message: 'Cannot star card' })

      const { container } = render(<TopicListPage deckId="deck-1" onNavigate={mockNavigate} />)
      await screen.findByText(MOCK_CARD.term)

      const starBtn = container.querySelector('.user-card-item__star-toggle')
      await user.click(starBtn)
      await waitFor(() => {
        expect(toggleStarApi).toHaveBeenCalledWith(MOCK_CARD.id, true)
        expect(container.querySelector('.user-card-item__star')).toBeInTheDocument()
      })

      await user.click(starBtn)
      expect(await screen.findByText('Cannot star card')).toBeInTheDocument()
    })
  })
})
