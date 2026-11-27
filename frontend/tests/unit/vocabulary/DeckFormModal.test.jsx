import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import DeckFormModal from '@/modules/vocabulary/components/DeckFormModal/DeckFormModal'
import { tr } from '../../utils/i18n'
import { MOCK_DECK } from '../../utils/mockData'

describe('DeckFormModal', () => {
  it('returns null when isOpen is false', () => {
    const { container } = render(<DeckFormModal isOpen={false} onClose={vi.fn()} onSubmit={vi.fn()} />)
    expect(container.firstChild).toBeNull()
    expect(document.querySelector('.deck-form-overlay')).toBeNull()
  })

  describe('Create mode', () => {
    it('renders create title, empty inputs, and submit button', () => {
      render(<DeckFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={vi.fn()} />)

      expect(screen.getByText(tr('learning:userDeck.form.createTitle'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))).toHaveValue('')
      expect(screen.getByLabelText(tr('learning:userDeck.form.descriptionLabel'))).toHaveValue('')
      expect(
        screen.getByRole('button', { name: new RegExp(tr('learning:userDeck.form.createDeck')) })
      ).toBeInTheDocument()
    })

    it('validates empty title, title > 50 chars and description > 255 chars', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn()
      render(<DeckFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      const submitBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:userDeck.form.createDeck')),
      })

      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:userDeck.form.titleRequired'))).toBeInTheDocument()
      expect(onSubmit).not.toHaveBeenCalled()

      const titleInput = screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))
      await user.type(titleInput, 'a'.repeat(51))
      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:userDeck.form.titleMaxLength'))).toBeInTheDocument()

      await user.clear(titleInput)
      await user.type(titleInput, 'Valid Title')
      const descInput = screen.getByLabelText(tr('learning:userDeck.form.descriptionLabel'))
      fireEvent.change(descInput, { target: { value: 'b'.repeat(256) } })
      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:userDeck.form.descriptionMaxLength'))).toBeInTheDocument()
      expect(onSubmit).not.toHaveBeenCalled()
    })

    it('clears field error on typing and calls onSubmit with trimmed values when valid', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn().mockResolvedValue({ success: true })
      render(<DeckFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      const submitBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:userDeck.form.createDeck')),
      })
      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:userDeck.form.titleRequired'))).toBeInTheDocument()

      const titleInput = screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))
      await user.type(titleInput, '  IELTS Vocab  ')
      expect(screen.queryByText(tr('learning:userDeck.form.titleRequired'))).not.toBeInTheDocument()

      const descInput = screen.getByLabelText(tr('learning:userDeck.form.descriptionLabel'))
      await user.type(descInput, '  Essential words  ')
      await user.click(submitBtn)

      expect(onSubmit).toHaveBeenCalledWith({
        title: 'IELTS Vocab',
        description: 'Essential words',
      })
    })

    it('calls onClose on cancel button click and Escape key when not loading', async () => {
      const user = userEvent.setup()
      const onClose = vi.fn()
      render(<DeckFormModal isOpen mode="create" onClose={onClose} onSubmit={vi.fn()} />)

      await user.click(screen.getByRole('button', { name: tr('common:cancel') }))
      expect(onClose).toHaveBeenCalledTimes(1)

      fireEvent.keyDown(document, { key: 'Escape' })
      expect(onClose).toHaveBeenCalledTimes(2)
    })

    it('does not call onClose on Escape key when isLoading is true', () => {
      const onClose = vi.fn()
      render(<DeckFormModal isOpen mode="create" isLoading onClose={onClose} onSubmit={vi.fn()} />)

      fireEvent.keyDown(document, { key: 'Escape' })
      expect(onClose).not.toHaveBeenCalled()
    })
  })

  describe('Edit mode', () => {
    it('pre-fills title and description from initialData and resets when reopened', () => {
      const { rerender } = render(
        <DeckFormModal
          isOpen
          mode="edit"
          initialData={MOCK_DECK}
          onClose={vi.fn()}
          onSubmit={vi.fn()}
        />
      )

      expect(screen.getByText(tr('learning:userDeck.form.editTitle'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))).toHaveValue(
        MOCK_DECK.title
      )
      expect(screen.getByLabelText(tr('learning:userDeck.form.descriptionLabel'))).toHaveValue(
        MOCK_DECK.description
      )
      expect(
        screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) })
      ).toBeInTheDocument()

      rerender(
        <DeckFormModal
          isOpen
          mode="edit"
          initialData={{ ...MOCK_DECK, title: 'Updated Title', description: '' }}
          onClose={vi.fn()}
          onSubmit={vi.fn()}
        />
      )

      expect(screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))).toHaveValue(
        'Updated Title'
      )
      expect(screen.getByLabelText(tr('learning:userDeck.form.descriptionLabel'))).toHaveValue('')
    })
  })

  describe('Server error & Loading state', () => {
    it('displays generalError or field errors from failed response', async () => {
      const user = userEvent.setup()
      const onSubmit = vi
        .fn()
        .mockResolvedValueOnce({ success: false, message: 'Server error' })
        .mockResolvedValueOnce({
          success: false,
          errors: [{ field: 'title', message: 'Title already exists' }],
        })

      render(<DeckFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      const titleInput = screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))
      await user.type(titleInput, 'Deck A')

      const submitBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:userDeck.form.createDeck')),
      })

      await user.click(submitBtn)
      expect(await screen.findByText('Server error')).toBeInTheDocument()

      await user.click(submitBtn)
      expect(await screen.findByText('Title already exists')).toBeInTheDocument()
      expect(screen.queryByText('Server error')).not.toBeInTheDocument()
    })

    it('disables inputs and buttons and shows saving state when isLoading is true', () => {
      render(<DeckFormModal isOpen mode="create" isLoading onClose={vi.fn()} onSubmit={vi.fn()} />)

      expect(screen.getByLabelText(tr('learning:userDeck.form.titleLabel'))).toBeDisabled()
      expect(screen.getByLabelText(tr('learning:userDeck.form.descriptionLabel'))).toBeDisabled()
      expect(screen.getByRole('button', { name: tr('common:cancel') })).toBeDisabled()
      expect(screen.getByText(tr('common:saving'))).toBeInTheDocument()
    })
  })
})
