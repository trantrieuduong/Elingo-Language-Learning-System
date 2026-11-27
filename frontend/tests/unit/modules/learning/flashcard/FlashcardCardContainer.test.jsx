import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FlashcardCardContainer from '@/modules/learning/flashcard/components/FlashcardCardContainer/FlashcardCardContainer'
import { LANGS, tr, useLang } from '../../../../utils/i18n'

const mockCard = {
  id: 5,
  term: 'resilient',
  translation: 'kiên cường',
  phonetics: [],
}

describe('FlashcardCardContainer', () => {
  it('returns null when card is null', () => {
    const { container } = render(<FlashcardCardContainer card={null} />)
    expect(container.firstChild).toBeNull()
  })

  describe.each(LANGS)('localized titles (%s)', (lng) => {
    it('renders star and mark-learned buttons with translated titles', async () => {
      await useLang(lng)
      render(
        <FlashcardCardContainer
          card={mockCard}
          isStarred={false}
          onToggleStar={vi.fn()}
          onToggleHide={vi.fn()}
        />
      )

      expect(screen.getByTitle(tr('learning:flashcard.actions.star'))).toBeInTheDocument()
      expect(screen.getByTitle(tr('learning:flashcard.actions.markLearned'))).toBeInTheDocument()
      expect(screen.getByText('resilient')).toBeInTheDocument()
    })
  })

  it('calls onToggleStar and onToggleHide when action buttons are clicked', async () => {
    const user = userEvent.setup()
    const onToggleStar = vi.fn()
    const onToggleHide = vi.fn()

    render(
      <FlashcardCardContainer
        card={mockCard}
        cardKey="topic-1-5"
        isStarred={true}
        onToggleStar={onToggleStar}
        onToggleHide={onToggleHide}
      />
    )

    await user.click(screen.getByTitle(tr('learning:flashcard.actions.unstar')))
    expect(onToggleStar).toHaveBeenCalledTimes(1)

    await user.click(screen.getByTitle(tr('learning:flashcard.actions.markLearned')))
    expect(onToggleHide).toHaveBeenCalledTimes(1)
  })

  it('disables action buttons when isProcessing is true', () => {
    render(
      <FlashcardCardContainer
        card={mockCard}
        isProcessing={true}
        isStarred={false}
        onToggleStar={vi.fn()}
        onToggleHide={vi.fn()}
      />
    )

    expect(screen.getByTitle(tr('learning:flashcard.actions.star'))).toBeDisabled()
    expect(screen.getByTitle(tr('learning:flashcard.actions.markLearned'))).toBeDisabled()
  })
})
