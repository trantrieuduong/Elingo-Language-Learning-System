import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FlashcardRatingActions from '@/modules/learning/flashcard/components/FlashcardRatingActions/FlashcardRatingActions'
import { LANGS, tr, useLang } from '../../../../utils/i18n'

describe('FlashcardRatingActions', () => {
  describe.each(LANGS)('localized rating buttons (%s)', (lng) => {
    it('renders all 4 SM-2 rating buttons and calls onReviewCard with grade and label', async () => {
      await useLang(lng)
      const user = userEvent.setup()
      const onReviewCard = vi.fn()

      render(<FlashcardRatingActions onReviewCard={onReviewCard} />)

      const expected = [
        { grade: 0, key: 'again' },
        { grade: 1, key: 'hard' },
        { grade: 2, key: 'good' },
        { grade: 3, key: 'easy' },
      ]

      for (const item of expected) {
        const btn = screen.getByText(tr(`learning:flashcard.ratings.${item.key}`)).closest('button')
        await user.click(btn)
        expect(onReviewCard).toHaveBeenCalledWith(
          item.grade,
          tr(`learning:flashcard.ratings.${item.key}`)
        )
      }

      expect(onReviewCard).toHaveBeenCalledTimes(4)
    })
  })

  it('disables all rating buttons when isProcessing is true', () => {
    render(<FlashcardRatingActions onReviewCard={vi.fn()} isProcessing={true} />)

    const buttons = screen.getAllByRole('button')
    expect(buttons).toHaveLength(4)
    buttons.forEach((btn) => expect(btn).toBeDisabled())
  })
})
