import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import UserCardItem from '@/modules/vocabulary/components/UserCardItem/UserCardItem'
import { MOCK_CARD, MOCK_STARRED_CARD } from '../../utils/mockData'
import { tr } from '../../utils/i18n'

describe('UserCardItem', () => {
  describe('Render', () => {
    it('displays card term and edit/delete buttons', () => {
      render(<UserCardItem card={MOCK_CARD} />)

      expect(screen.getByText(MOCK_CARD.term)).toBeInTheDocument()
      expect(screen.getByTitle(tr('common:edit'))).toBeInTheDocument()
      expect(screen.getByTitle(tr('common:delete'))).toBeInTheDocument()
    })

    it('shows star icon meta when card.state.starred is true and hides it when false', () => {
      const { container, rerender } = render(<UserCardItem card={MOCK_CARD} />)
      expect(container.querySelector('.user-card-item__star')).not.toBeInTheDocument()

      rerender(<UserCardItem card={MOCK_STARRED_CARD} />)
      expect(container.querySelector('.user-card-item__star')).toBeInTheDocument()
    })
  })

  describe('Interactions', () => {
    it('calls onEdit and onDelete with card when edit/delete buttons are clicked', async () => {
      const user = userEvent.setup()
      const onEdit = vi.fn()
      const onDelete = vi.fn()
      render(<UserCardItem card={MOCK_CARD} onEdit={onEdit} onDelete={onDelete} />)

      await user.click(screen.getByTitle(tr('common:edit')))
      expect(onEdit).toHaveBeenCalledWith(MOCK_CARD)

      await user.click(screen.getByTitle(tr('common:delete')))
      expect(onDelete).toHaveBeenCalledWith(MOCK_CARD)
    })

    it('calls onToggleStar with card when star button is clicked and disables it while processing', async () => {
      const user = userEvent.setup()
      const onToggleStar = vi.fn()
      const { container, rerender } = render(
        <UserCardItem card={MOCK_CARD} onToggleStar={onToggleStar} isStarProcessing={false} />
      )

      const starBtn = container.querySelector('.user-card-item__star-toggle')
      expect(starBtn).not.toBeDisabled()
      await user.click(starBtn)
      expect(onToggleStar).toHaveBeenCalledWith(MOCK_CARD)

      rerender(
        <UserCardItem card={MOCK_CARD} onToggleStar={onToggleStar} isStarProcessing={true} />
      )
      expect(container.querySelector('.user-card-item__star-toggle')).toBeDisabled()
    })
  })
})
