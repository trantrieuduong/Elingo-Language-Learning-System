import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FlashcardSessionLayout from '@/modules/learning/flashcard/components/FlashcardSessionLayout/FlashcardSessionLayout'
import { tr, trRx } from '../../../../utils/i18n'

describe('FlashcardSessionLayout', () => {
  it('renders back button, progress, sidebar, and main content', async () => {
    const user = userEvent.setup()
    const onBack = vi.fn()

    const { container } = render(
      <FlashcardSessionLayout
        backLabel={tr('learning:flashcard.backToDecks')}
        onBack={onBack}
        progress={<div data-testid="progress-slot">Progress</div>}
        sidebar={<div data-testid="sidebar-slot">Sidebar</div>}
        layoutClassName="custom-layout"
        sidebarClassName="custom-sidebar"
      >
        <div data-testid="main-slot">Main Content</div>
      </FlashcardSessionLayout>
    )

    expect(container.querySelector('.flashcard-layout')).toHaveClass('custom-layout')
    expect(container.querySelector('aside')).toHaveClass('custom-sidebar')
    expect(screen.getByTestId('progress-slot')).toBeInTheDocument()
    expect(screen.getByTestId('sidebar-slot')).toBeInTheDocument()
    expect(screen.getByTestId('main-slot')).toBeInTheDocument()

    await user.click(
      screen.getByRole('button', { name: trRx('learning:flashcard.backToDecks') })
    )
    expect(onBack).toHaveBeenCalledTimes(1)
  })

  it('uses default sidebarClassName when not provided', () => {
    const { container } = render(
      <FlashcardSessionLayout
        backLabel={tr('common:back')}
        onBack={vi.fn()}
        sidebar={<span>Topics</span>}
      >
        <span>Card</span>
      </FlashcardSessionLayout>
    )

    expect(container.querySelector('aside')).toHaveClass('flashcard-sidebar')
  })
})
