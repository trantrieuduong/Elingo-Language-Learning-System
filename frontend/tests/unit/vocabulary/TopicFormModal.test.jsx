import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import TopicFormModal from '@/modules/vocabulary/components/TopicFormModal/TopicFormModal'
import { tr } from '../../utils/i18n'
import { MOCK_TOPIC } from '../../utils/mockData'

describe('TopicFormModal', () => {
  it('returns null when isOpen is false', () => {
    const { container } = render(
      <TopicFormModal isOpen={false} onClose={vi.fn()} onSubmit={vi.fn()} />
    )
    expect(container.firstChild).toBeNull()
    expect(document.querySelector('.topic-form-overlay')).toBeNull()
  })

  describe('Create mode', () => {
    it('renders create heading and empty name input', () => {
      render(<TopicFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={vi.fn()} />)
      expect(screen.getByRole('heading', { name: tr('learning:topic.createTitle') })).toBeInTheDocument()
      expect(screen.getByLabelText(tr('learning:topic.labelName'))).toHaveValue('')
    })

    it('validates empty name and name > 50 characters', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn()
      render(<TopicFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      const submitBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:topic.submitCreate')),
      })

      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:topic.errorNameRequired'))).toBeInTheDocument()
      expect(onSubmit).not.toHaveBeenCalled()

      const nameInput = screen.getByLabelText(tr('learning:topic.labelName'))
      await user.type(nameInput, 'a'.repeat(51))
      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:topic.errorNameTooLong'))).toBeInTheDocument()
      expect(onSubmit).not.toHaveBeenCalled()
    })

    it('clears error on typing and calls onSubmit with trimmed name when valid', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn().mockResolvedValue({ success: true })
      render(<TopicFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      const submitBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:topic.submitCreate')),
      })
      await user.click(submitBtn)
      expect(screen.getByText(tr('learning:topic.errorNameRequired'))).toBeInTheDocument()

      const nameInput = screen.getByLabelText(tr('learning:topic.labelName'))
      await user.type(nameInput, '  Animals  ')
      expect(screen.queryByText(tr('learning:topic.errorNameRequired'))).not.toBeInTheDocument()

      await user.click(submitBtn)
      expect(onSubmit).toHaveBeenCalledWith({ name: 'Animals' })
    })

    it('calls onClose on cancel button click and Escape key', async () => {
      const user = userEvent.setup()
      const onClose = vi.fn()
      render(<TopicFormModal isOpen mode="create" onClose={onClose} onSubmit={vi.fn()} />)

      await user.click(screen.getByRole('button', { name: tr('common:cancel') }))
      expect(onClose).toHaveBeenCalledTimes(1)

      fireEvent.keyDown(document, { key: 'Escape' })
      expect(onClose).toHaveBeenCalledTimes(2)
    })
  })

  describe('Edit mode', () => {
    it('pre-fills name from topic and resets when topic changes', () => {
      const { rerender } = render(
        <TopicFormModal isOpen mode="edit" topic={MOCK_TOPIC} onClose={vi.fn()} onSubmit={vi.fn()} />
      )

      expect(screen.getByRole('heading', { name: tr('learning:topic.editTitle') })).toBeInTheDocument()
      expect(screen.getByLabelText(tr('learning:topic.labelName'))).toHaveValue(MOCK_TOPIC.name)

      rerender(
        <TopicFormModal
          isOpen
          mode="edit"
          topic={{ ...MOCK_TOPIC, name: 'Travel' }}
          onClose={vi.fn()}
          onSubmit={vi.fn()}
        />
      )
      expect(screen.getByLabelText(tr('learning:topic.labelName'))).toHaveValue('Travel')
    })
  })

  describe('Server error & isSubmitting', () => {
    it('displays generalError when onSubmit returns failure or throws', async () => {
      const user = userEvent.setup()
      const onSubmit = vi
        .fn()
        .mockResolvedValueOnce({ success: false, message: 'Topic already exists' })
        .mockRejectedValueOnce({ response: { data: { message: 'Connection error' } } })

      render(<TopicFormModal isOpen mode="create" onClose={vi.fn()} onSubmit={onSubmit} />)

      const nameInput = screen.getByLabelText(tr('learning:topic.labelName'))
      await user.type(nameInput, 'Food')
      const submitBtn = screen.getByRole('button', {
        name: new RegExp(tr('learning:topic.submitCreate')),
      })

      await user.click(submitBtn)
      expect(await screen.findByText('Topic already exists')).toBeInTheDocument()

      await user.click(submitBtn)
      expect(await screen.findByText('Connection error')).toBeInTheDocument()
    })

    it('disables input and buttons when isSubmitting is true', () => {
      render(<TopicFormModal isOpen mode="create" isSubmitting onClose={vi.fn()} onSubmit={vi.fn()} />)

      expect(screen.getByLabelText(tr('learning:topic.labelName'))).toBeDisabled()
      expect(screen.getByRole('button', { name: tr('common:cancel') })).toBeDisabled()
      expect(screen.getByText(tr('common:saving'))).toBeInTheDocument()
    })
  })
})
