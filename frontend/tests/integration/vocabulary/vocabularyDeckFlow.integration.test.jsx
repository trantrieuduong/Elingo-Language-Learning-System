import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import DeckListPage from '@/modules/vocabulary/pages/DeckListPage/DeckListPage'
import { useAuth } from '@/context/AuthContext'
import { server } from '../../mocks/server'
import {
  MOCK_AUTH_CONTEXT_DEFAULT,
  MOCK_AUTH_CONTEXT_LOGGED_IN,
  MOCK_DECK,
  MOCK_PUBLISHED_DECK,
} from '../../utils/mockData'
import { tr } from '../../utils/i18n'

vi.mock('@/context/AuthContext')

const API_URL = import.meta.env.VITE_API_URL

describe('Vocabulary deck flow (integration)', () => {
  const mockNavigate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    window.history.pushState({}, '', '/decks')
    useAuth.mockReturnValue(MOCK_AUTH_CONTEXT_DEFAULT)
  })

  describe('Guest user (user = null)', () => {
    it('shows only system-decks and community-decks tabs and fetches ownerType=SYSTEM by default', async () => {
      let capturedOwnerType = null
      server.use(
        http.get(`${API_URL}/decks`, ({ request }) => {
          capturedOwnerType = new URL(request.url).searchParams.get('ownerType')
          return HttpResponse.json({
            success: true,
            data: { content: [MOCK_DECK], totalPages: 1 },
          })
        })
      )

      render(<DeckListPage onNavigate={mockNavigate} />)

      expect(screen.getByText(tr('learning:userDeck.tabs.systemDecks'))).toBeInTheDocument()
      expect(screen.getByText(tr('learning:userDeck.tabs.explore'))).toBeInTheDocument()
      expect(screen.queryByText(tr('learning:userDeck.tabs.myDecks'))).not.toBeInTheDocument()

      expect(await screen.findByText(MOCK_DECK.title)).toBeInTheDocument()
      expect(capturedOwnerType).toBe('SYSTEM')
    })

    it('renders EmptyState when no decks are returned', async () => {
      server.use(
        http.get(`${API_URL}/decks`, () =>
          HttpResponse.json({ success: true, data: { content: [], totalPages: 1 } })
        )
      )

      render(<DeckListPage onNavigate={mockNavigate} />)
      expect(await screen.findByText(tr('learning:deckList.emptyTitle'))).toBeInTheDocument()
    })
  })

  describe('Authenticated user (user = MOCK_USER)', () => {
    beforeEach(() => {
      useAuth.mockReturnValue(MOCK_AUTH_CONTEXT_LOGGED_IN)
    })

    it('shows 3 tabs, switches to my-decks to fetch /users/me/decks, and opens DeckFormModal', async () => {
      const user = userEvent.setup()
      server.use(
        http.get(`${API_URL}/users/me/decks`, () =>
          HttpResponse.json({ success: true, data: [MOCK_DECK] })
        )
      )

      render(<DeckListPage onNavigate={mockNavigate} />)

      const myDecksTab = screen.getByText(tr('learning:userDeck.tabs.myDecks'))
      expect(myDecksTab).toBeInTheDocument()
      await user.click(myDecksTab)

      expect(await screen.findByText(MOCK_DECK.title)).toBeInTheDocument()

      const createBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:userDeck.createBtn')),
      })
      await user.click(createBtn)
      expect(screen.getByText(tr('learning:userDeck.form.createTitle'))).toBeInTheDocument()
    })

    it('creates a new deck, shows success toast, and refreshes the list', async () => {
      const user = userEvent.setup()
      let fetchCount = 0
      server.use(
        http.get(`${API_URL}/users/me/decks`, () => {
          fetchCount += 1
          return HttpResponse.json({
            success: true,
            data: fetchCount === 1 ? [] : [{ ...MOCK_DECK, title: 'Brand New Deck' }],
          })
        }),
        http.post(`${API_URL}/decks`, () =>
          HttpResponse.json({ success: true, data: { id: 'new-1', title: 'Brand New Deck' } })
        )
      )

      render(<DeckListPage onNavigate={mockNavigate} />)
      await user.click(screen.getByText(tr('learning:userDeck.tabs.myDecks')))

      const createBtns = await screen.findAllByRole('button', {
        name: new RegExp(tr('learning:userDeck.createBtn')),
      })
      await user.click(createBtns[0])

      await user.type(screen.getByLabelText(tr('learning:userDeck.form.titleLabel')), 'Brand New Deck')
      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.form.createDeck')) })
      )

      expect(await screen.findByText(tr('learning:userDeck.createSuccess'))).toBeInTheDocument()
      expect(await screen.findByText('Brand New Deck')).toBeInTheDocument()
    })

    it('displays USER_NOT_FOUND error message inside DeckFormModal on failure', async () => {
      const user = userEvent.setup()
      server.use(
        http.get(`${API_URL}/users/me/decks`, () => HttpResponse.json({ success: true, data: [] })),
        http.post(`${API_URL}/decks`, () =>
          HttpResponse.json(
            { success: false, code: 'USER_NOT_FOUND', message: 'User not found' },
            { status: 404 }
          )
        )
      )

      render(<DeckListPage onNavigate={mockNavigate} />)
      await user.click(screen.getByText(tr('learning:userDeck.tabs.myDecks')))

      const createBtns = await screen.findAllByRole('button', {
        name: new RegExp(tr('learning:userDeck.createBtn')),
      })
      await user.click(createBtns[0])

      await user.type(screen.getByLabelText(tr('learning:userDeck.form.titleLabel')), 'Test Deck')
      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.form.createDeck')) })
      )

      expect(await screen.findByText(tr('api:error.USER_NOT_FOUND'))).toBeInTheDocument()
    })

    it('filters my-decks by PUBLISHED, DRAFT, and ALL status pills', async () => {
      const user = userEvent.setup()
      server.use(
        http.get(`${API_URL}/users/me/decks`, () =>
          HttpResponse.json({
            success: true,
            data: [MOCK_DECK, { ...MOCK_PUBLISHED_DECK, ownerId: 1 }],
          })
        )
      )

      render(<DeckListPage onNavigate={mockNavigate} />)
      await user.click(screen.getByText(tr('learning:userDeck.tabs.myDecks')))
      await screen.findByText(MOCK_DECK.title)

      await user.click(screen.getByRole('button', { name: tr('learning:userDeck.statusPublished') }))
      expect(screen.queryByText(MOCK_DECK.title)).not.toBeInTheDocument()
      expect(screen.getByText(MOCK_PUBLISHED_DECK.title)).toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: tr('learning:userDeck.statusDraft') }))
      expect(screen.getByText(MOCK_DECK.title)).toBeInTheDocument()
      expect(screen.queryByText(MOCK_PUBLISHED_DECK.title)).not.toBeInTheDocument()

      await user.click(screen.getByRole('button', { name: tr('learning:userDeck.statusAll') }))
      expect(screen.getByText(MOCK_PUBLISHED_DECK.title)).toBeInTheDocument()
    })

    it('opens OptionsModal to toggle status, edit, and delete deck in my-decks', async () => {
      const user = userEvent.setup()
      server.use(
        http.get(`${API_URL}/users/me/decks`, () =>
          HttpResponse.json({ success: true, data: [MOCK_DECK] })
        ),
        http.put(`${API_URL}/decks/:deckId/publication`, () =>
          HttpResponse.json({ success: true, data: {} })
        ),
        http.put(`${API_URL}/decks/:deckId`, () =>
          HttpResponse.json({ success: true, data: { ...MOCK_DECK, title: 'Edited Deck' } })
        ),
        http.delete(`${API_URL}/decks/:deckId`, () => HttpResponse.json({ success: true }))
      )

      const { container } = render(<DeckListPage onNavigate={mockNavigate} />)
      await user.click(screen.getByText(tr('learning:userDeck.tabs.myDecks')))
      await screen.findByText(MOCK_DECK.title)

      await user.click(container.querySelector('.user-deck-card__options-btn'))
      await user.click(screen.getByText(tr('learning:userDeck.publishDeck')))
      expect(await screen.findByText(tr('learning:userDeck.statusChanged'))).toBeInTheDocument()

      await user.click(container.querySelector('.user-deck-card__options-btn'))
      await user.click(screen.getByText(tr('learning:userDeck.editDeck')))
      const titleInput = screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))
      await user.clear(titleInput)
      await user.type(titleInput, 'Edited Deck')
      await user.click(screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) }))
      expect(await screen.findByText(tr('learning:userDeck.updateSuccess'))).toBeInTheDocument()

      await user.click(container.querySelector('.user-deck-card__options-btn'))
      await user.click(screen.getByText(tr('learning:userDeck.deleteDeck')))
      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.deleteConfirm')) })
      )
      expect(await screen.findByText(tr('learning:userDeck.deleteSuccess'))).toBeInTheDocument()
    })
  })

  describe('system-decks & community-decks tabs', () => {
    it('navigates to /decks/:slug on deck click and shows premium badge when isPremium is true', async () => {
      const user = userEvent.setup()
      server.use(
        http.get(`${API_URL}/decks`, () =>
          HttpResponse.json({
            success: true,
            data: { content: [{ ...MOCK_DECK, isPremium: true }], totalPages: 1 },
          })
        )
      )

      render(<DeckListPage onNavigate={mockNavigate} />)
      const deckTitle = await screen.findByText(MOCK_DECK.title)
      expect(screen.getByText(tr('learning:deckList.premium'))).toBeInTheDocument()

      await user.click(deckTitle)
      expect(mockNavigate).toHaveBeenCalledWith(`/decks/${MOCK_DECK.slug}`, {
        returnTo: '/decks?tab=system-decks',
      })
    })

    it('fetches ownerType=USER on community-decks and shows OptionsMenu only for non-owned decks', async () => {
      const user = userEvent.setup()
      useAuth.mockReturnValue(MOCK_AUTH_CONTEXT_LOGGED_IN)

      let capturedOwnerType = null
      server.use(
        http.get(`${API_URL}/decks`, ({ request }) => {
          capturedOwnerType = new URL(request.url).searchParams.get('ownerType')
          return HttpResponse.json({
            success: true,
            data: { content: [MOCK_DECK, MOCK_PUBLISHED_DECK], totalPages: 1 },
          })
        })
      )

      const { container } = render(<DeckListPage onNavigate={mockNavigate} />)
      await user.click(screen.getByText(tr('learning:userDeck.tabs.explore')))

      expect(await screen.findByText(MOCK_PUBLISHED_DECK.title)).toBeInTheDocument()
      expect(capturedOwnerType).toBe('USER')
      expect(container.querySelectorAll('.vocabulary-card__options-btn')).toHaveLength(1)
    })
  })

  describe('Search, Pagination & Error state', () => {
    it('debounces search input by 500ms and fetches with q=keyword', async () => {
      const user = userEvent.setup()
      const requestedQueries = []
      server.use(
        http.get(`${API_URL}/decks`, ({ request }) => {
          requestedQueries.push(new URL(request.url).searchParams.get('q'))
          return HttpResponse.json({
            success: true,
            data: { content: [MOCK_DECK], totalPages: 1 },
          })
        })
      )

      render(<DeckListPage onNavigate={mockNavigate} />)
      await screen.findByText(MOCK_DECK.title)

      const searchInput = screen.getByPlaceholderText(tr('learning:deckList.searchPlaceholder'))
      await user.type(searchInput, 'travel')

      await waitFor(
        () => {
          expect(requestedQueries).toContain('travel')
        },
        { timeout: 1500 }
      )
    })

    it('renders Pagination when totalPages > 1 and fetches page=2 on click', async () => {
      const user = userEvent.setup()
      const requestedPages = []
      server.use(
        http.get(`${API_URL}/decks`, ({ request }) => {
          requestedPages.push(new URL(request.url).searchParams.get('page'))
          return HttpResponse.json({
            success: true,
            data: { content: [MOCK_DECK], totalPages: 2 },
          })
        })
      )

      render(<DeckListPage onNavigate={mockNavigate} />)
      await screen.findByText(MOCK_DECK.title)

      await user.click(screen.getByRole('button', { name: '2' }))
      await waitFor(() => {
        expect(requestedPages).toContain('2')
      })
    })

    it('renders error EmptyState on fetch failure and retries on button click', async () => {
      const user = userEvent.setup()
      let shouldFail = true
      server.use(
        http.get(`${API_URL}/decks`, () => {
          if (shouldFail) {
            return HttpResponse.json({ success: false }, { status: 500 })
          }
          return HttpResponse.json({
            success: true,
            data: { content: [MOCK_DECK], totalPages: 1 },
          })
        })
      )

      render(<DeckListPage onNavigate={mockNavigate} />)
      expect(await screen.findByText(tr('common:loadError'))).toBeInTheDocument()

      shouldFail = false
      await user.click(screen.getByRole('button', { name: tr('common:retry') }))
      expect(await screen.findByText(MOCK_DECK.title)).toBeInTheDocument()
    })
  })
})
