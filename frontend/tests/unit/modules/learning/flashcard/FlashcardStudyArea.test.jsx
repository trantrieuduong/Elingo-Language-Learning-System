import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FlashcardStudyArea from '@/modules/learning/flashcard/components/FlashcardStudyArea/FlashcardStudyArea'
import { tr, trRx } from '../../../../utils/i18n'

const mockCard = {
  id: 11,
  term: 'serendipity',
  translation: 'sự tình cờ may mắn',
  phonetics: [],
}

describe('FlashcardStudyArea', () => {
  it('returns null when card is null or undefined', () => {
    const { container } = render(<FlashcardStudyArea card={null} />)
    expect(container.firstChild).toBeNull()
  })

  it('renders FlashcardCardContainer and FlashcardRatingActions and forwards callbacks', async () => {
    const user = userEvent.setup()
    const onToggleStar = vi.fn()
    const onToggleHide = vi.fn()
    const onReviewCard = vi.fn()

    render(
      <FlashcardStudyArea
        card={mockCard}
        cardKey="topic-1-11"
        isStarred={false}
        onToggleStar={onToggleStar}
        onToggleHide={onToggleHide}
        onReviewCard={onReviewCard}
      />
    )

    expect(screen.getByText('serendipity')).toBeInTheDocument()

    await user.click(screen.getByTitle(tr('learning:flashcard.actions.star')))
    expect(onToggleStar).toHaveBeenCalledTimes(1)

    await user.click(screen.getByTitle(tr('learning:flashcard.actions.markLearned')))
    expect(onToggleHide).toHaveBeenCalledTimes(1)

    await user.click(
      screen.getByText(tr('learning:flashcard.ratings.good')).closest('button')
    )
    expect(onReviewCard).toHaveBeenCalledWith(2, tr('learning:flashcard.ratings.good'))
  })

  it('disables card action buttons and rating buttons when isProcessing is true', () => {
    render(
      <FlashcardStudyArea
        card={mockCard}
        isProcessing={true}
        isStarred={true}
        onToggleStar={vi.fn()}
        onToggleHide={vi.fn()}
        onReviewCard={vi.fn()}
      />
    )

    expect(screen.getByTitle(tr('learning:flashcard.actions.unstar'))).toBeDisabled()
    expect(screen.getByTitle(tr('learning:flashcard.actions.markLearned'))).toBeDisabled()
    expect(
      screen.getByRole('button', { name: trRx('learning:flashcard.ratings.again') })
    ).toBeDisabled()
    expect(
      screen.getByRole('button', { name: trRx('learning:flashcard.ratings.easy') })
    ).toBeDisabled()
  })
})
