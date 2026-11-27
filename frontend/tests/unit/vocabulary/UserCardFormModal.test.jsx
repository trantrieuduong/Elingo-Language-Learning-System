import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import UserCardFormModal from '@/modules/vocabulary/components/UserCardFormModal/UserCardFormModal'
import { MOCK_CARD } from '../../utils/mockData'
import { tr } from '../../utils/i18n'

describe('UserCardFormModal', () => {
  it('returns null when isOpen is false', () => {
    const { container } = render(
      <UserCardFormModal isOpen={false} onClose={vi.fn()} onSubmit={vi.fn()} />
    )
    expect(container.firstChild).toBeNull()
    expect(document.querySelector('.user-card-form-overlay')).toBeNull()
  })

  describe('Create mode', () => {
    it('renders create title and all fields empty initially', () => {
      render(<UserCardFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={vi.fn()} />)

      expect(screen.getByText(tr('learning:userCards.createTitle'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('learning:userCards.termLabel'))).toHaveValue('')
      expect(screen.getByLabelText(tr('learning:userCards.translation'))).toHaveValue('')
      expect(screen.getByLabelText(tr('learning:userCards.partOfSpeech'))).toHaveValue('')
      expect(screen.getByLabelText(tr('learning:userCards.explanationVi'))).toHaveValue('')
      expect(screen.getByLabelText(tr('learning:userCards.explanationEn'))).toHaveValue('')
      expect(screen.getByLabelText(tr('learning:userCards.examplesVi'))).toHaveValue('')
      expect(screen.getByLabelText(tr('learning:userCards.examplesEn'))).toHaveValue('')
    })

    it('validates required fields and max length constraints', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn()
      render(<UserCardFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      const submitBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:userCards.createCard')),
      })

      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:userCards.termRequired'))).toBeInTheDocument()
      expect(screen.getByText(tr('learning:userCards.translationRequired'))).toBeInTheDocument()
      expect(onSubmit).not.toHaveBeenCalled()

      fireEvent.change(screen.getByLabelText(tr('learning:userCards.termLabel')), {
        target: { value: 'a'.repeat(101) },
      })
      fireEvent.change(screen.getByLabelText(tr('learning:userCards.translation')), {
        target: { value: 'b'.repeat(501) },
      })
      fireEvent.change(screen.getByLabelText(tr('learning:userCards.explanationVi')), {
        target: { value: 'c'.repeat(256) },
      })
      fireEvent.change(screen.getByLabelText(tr('learning:userCards.examplesEn')), {
        target: { value: 'd'.repeat(256) },
      })

      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:userCards.termTooLong'))).toBeInTheDocument()
      expect(screen.getByText(tr('learning:userCards.translationTooLong'))).toBeInTheDocument()
      expect(screen.getByText(tr('learning:userCards.explanationTooLong'))).toBeInTheDocument()
      expect(screen.getByText(tr('learning:userCards.exampleTooLong'))).toBeInTheDocument()
      expect(onSubmit).not.toHaveBeenCalled()
    })

    it('clears errors on typing and submits trimmed payload with null or selected pos', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn().mockResolvedValue({ success: true })
      render(<UserCardFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      const submitBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:userCards.createCard')),
      })

      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:userCards.termRequired'))).toBeInTheDocument()

      await user.type(screen.getByLabelText(tr('learning:userCards.termLabel')), '  accomplish  ')
      expect(screen.queryByText(tr('learning:userCards.termRequired'))).not.toBeInTheDocument()

      await user.type(screen.getByLabelText(tr('learning:userCards.translation')), '  hoàn thành  ')
      expect(
        screen.queryByText(tr('learning:userCards.translationRequired'))
      ).not.toBeInTheDocument()

      await user.click(submitBtn)
      expect(onSubmit).toHaveBeenCalledWith({
        term: 'accomplish',
        pos: null,
        translation: 'hoàn thành',
        explanationVi: null,
        explanationEn: null,
        examplesVi: null,
        examplesEn: null,
      })

      await user.selectOptions(screen.getByLabelText(tr('learning:userCards.partOfSpeech')), 'verb')
      await user.click(submitBtn)
      expect(onSubmit).toHaveBeenLastCalledWith(
        expect.objectContaining({
          term: 'accomplish',
          pos: 'verb',
          translation: 'hoàn thành',
        })
      )
    })

    it('calls onClose on cancel button click and Escape key when not submitting', async () => {
      const user = userEvent.setup()
      const onClose = vi.fn()
      render(<UserCardFormModal isOpen mode="create" onClose={onClose} onSubmit={vi.fn()} />)

      await user.click(screen.getByRole('button', { name: tr('common:cancel') }))
      expect(onClose).toHaveBeenCalledTimes(1)

      fireEvent.keyDown(document, { key: 'Escape' })
      expect(onClose).toHaveBeenCalledTimes(2)
    })
  })

  describe('Edit mode', () => {
    it('pre-fills all fields from card prop and shows saveChanges button', () => {
      const richCard = {
        ...MOCK_CARD,
        explanationVi: 'Lời chào',
        explanationEn: 'A greeting',
        examplesVi: 'Xin chào bạn',
        examplesEn: 'Hello friend',
      }
      render(
        <UserCardFormModal isOpen mode="edit" card={richCard} onClose={vi.fn()} onSubmit={vi.fn()} />
      )

      expect(screen.getByText(tr('learning:userCards.editTitle'))).toBeInTheDocument()
      expect(screen.getByLabelText(tr('learning:userCards.termLabel'))).toHaveValue(richCard.term)
      expect(screen.getByLabelText(tr('learning:userCards.partOfSpeech'))).toHaveValue(richCard.pos)
      expect(screen.getByLabelText(tr('learning:userCards.translation'))).toHaveValue(
        richCard.translation
      )
      expect(screen.getByLabelText(tr('learning:userCards.explanationVi'))).toHaveValue(
        richCard.explanationVi
      )
      expect(screen.getByLabelText(tr('learning:userCards.explanationEn'))).toHaveValue(
        richCard.explanationEn
      )
      expect(screen.getByLabelText(tr('learning:userCards.examplesVi'))).toHaveValue(
        richCard.examplesVi
      )
      expect(screen.getByLabelText(tr('learning:userCards.examplesEn'))).toHaveValue(
        richCard.examplesEn
      )
      expect(
        screen.getByRole('button', { name: new RegExp(tr('common:saveChanges')) })
      ).toBeInTheDocument()
    })
  })

  describe('Server error & isSubmitting', () => {
    it('displays generalError when onSubmit returns failure', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn().mockResolvedValue({ success: false, message: 'Term already exists' })
      render(<UserCardFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      await user.type(screen.getByLabelText(tr('learning:userCards.termLabel')), 'hello')
      await user.type(screen.getByLabelText(tr('learning:userCards.translation')), 'xin chào')
      await user.click(
        screen.getByRole('button', { name: new RegExp(tr('learning:userCards.createCard')) })
      )

      expect(await screen.findByText('Term already exists')).toBeInTheDocument()
    })

    it('disables all inputs and shows saving text when isSubmitting is true', () => {
      render(
        <UserCardFormModal isOpen mode="create" isSubmitting onClose={vi.fn()} onSubmit={vi.fn()} />
      )

      expect(screen.getByLabelText(tr('learning:userCards.termLabel'))).toBeDisabled()
      expect(screen.getByLabelText(tr('learning:userCards.translation'))).toBeDisabled()
      expect(screen.getByLabelText(tr('learning:userCards.partOfSpeech'))).toBeDisabled()
      expect(screen.getByRole('button', { name: tr('common:cancel') })).toBeDisabled()
      expect(screen.getByText(tr('common:saving'))).toBeInTheDocument()
    })
  })
})
