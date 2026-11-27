import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import UserDeckCard from '@/modules/vocabulary/components/UserDeckCard/UserDeckCard'
import { MOCK_DECK, MOCK_PUBLISHED_DECK } from '../../utils/mockData'
import { tr } from '../../utils/i18n'

describe('UserDeckCard', () => {
  describe('Status badges', () => {
    it('renders PUBLISHED badge when status is PUBLISHED', () => {
      render(<UserDeckCard deck={MOCK_PUBLISHED_DECK} />)
      expect(screen.getByText(tr('learning:userDeck.statusPublished'))).toBeInTheDocument()
    })

    it('renders DRAFT badge when status is DRAFT', () => {
      render(<UserDeckCard deck={MOCK_DECK} />)
      expect(screen.getByText(tr('learning:userDeck.statusDraft'))).toBeInTheDocument()
    })

    it('renders Archived badge when status is neither PUBLISHED nor DRAFT', () => {
      render(<UserDeckCard deck={{ ...MOCK_DECK, status: 'ARCHIVED' }} />)
      expect(screen.getByText(tr('learning:userDeck.statusArchived'))).toBeInTheDocument()
    })
  })

  describe('Content & Progress', () => {
    it('displays title, description, DeckStatsPill, and ProgressBar when cardCount > 0', () => {
      const { container } = render(<UserDeckCard deck={MOCK_DECK} />)

      expect(screen.getByText(MOCK_DECK.title)).toBeInTheDocument()
      expect(screen.getByText(MOCK_DECK.description)).toBeInTheDocument()
      expect(
        screen.getByText(
          tr('learning:deckList.cardStats', {
            topicCount: MOCK_DECK.topicCount,
            cardCount: MOCK_DECK.cardCount,
          })
        )
      ).toBeInTheDocument()
      expect(container.querySelector('.user-deck-card__progress')).toBeInTheDocument()
    })

    it('displays noDescription fallback when description is empty and hides ProgressBar when cardCount is 0', () => {
      const { container } = render(
        <UserDeckCard deck={{ ...MOCK_DECK, description: '', cardCount: 0 }} />
      )

      expect(screen.getByText(tr('learning:deckList.noDescription'))).toBeInTheDocument()
      expect(container.querySelector('.user-deck-card__progress')).not.toBeInTheDocument()
    })
  })

  describe('Tags & CEFR', () => {
    it('renders CEFR and tag badges when present', () => {
      render(<UserDeckCard deck={MOCK_DECK} />)
      expect(screen.getByText('A1')).toBeInTheDocument()
      expect(screen.getByText('Du lịch')).toBeInTheDocument()
    })

    it('hides tags section when cefrLevels and tags are empty', () => {
      const { container } = render(
        <UserDeckCard deck={{ ...MOCK_DECK, cefrLevels: [], tags: [] }} />
      )
      expect(container.querySelector('.user-deck-card__tags')).not.toBeInTheDocument()
    })
  })

  describe('Button interactions', () => {
    it('renders OptionsMenu and calls onOptionsClick when clicked', async () => {
      const user = userEvent.setup()
      const onOptionsClick = vi.fn()
      const { container } = render(
        <UserDeckCard deck={MOCK_DECK} onOptionsClick={onOptionsClick} />
      )

      const optBtn = container.querySelector('.user-deck-card__options-btn')
      expect(optBtn).toBeInTheDocument()
      await user.click(optBtn)
      expect(onOptionsClick).toHaveBeenCalledWith(MOCK_DECK)
    })

    it('hides OptionsMenu when onOptionsClick is not provided', () => {
      const { container } = render(<UserDeckCard deck={MOCK_DECK} />)
      expect(container.querySelector('.user-deck-card__options-btn')).not.toBeInTheDocument()
    })

    it('renders manage-topics trigger and calls onManageTopics when clicked', async () => {
      const user = userEvent.setup()
      const onManageTopics = vi.fn()
      const { container } = render(
        <UserDeckCard deck={MOCK_DECK} onManageTopics={onManageTopics} />
      )

      const openBtn = container.querySelector('.user-deck-card__open-topics')
      expect(openBtn).toBeInTheDocument()
      await user.click(openBtn)
      expect(onManageTopics).toHaveBeenCalledWith(MOCK_DECK)
    })
  })
})
