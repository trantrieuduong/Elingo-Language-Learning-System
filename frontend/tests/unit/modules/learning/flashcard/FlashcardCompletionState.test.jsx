import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import FlashcardCompletionState from '@/modules/learning/flashcard/components/FlashcardCompletionState/FlashcardCompletionState'
import { LANGS, tr, useLang } from '../../../../utils/i18n'

describe('FlashcardCompletionState', () => {
  describe.each(LANGS)('localized completion title (%s)', (lng) => {
    it('renders completion icon, localized title, and optional message', async () => {
      await useLang(lng)
      render(<FlashcardCompletionState message="Session finished!" />)

      expect(screen.getByText('task_alt')).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: tr('learning:flashcard.completeTitle') })).toBeInTheDocument()
      expect(screen.getByText('Session finished!')).toBeInTheDocument()
    })
  })

  it('does not render message paragraph when message is omitted', () => {
    const { container } = render(<FlashcardCompletionState />)
    expect(container.querySelector('.flashcard-completion-state__message')).toBeNull()
  })
})
