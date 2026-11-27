import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import TopicCard from '@/modules/vocabulary/components/TopicCard/TopicCard'
import { MOCK_TOPIC } from '../../utils/mockData'
import { tr } from '../../utils/i18n'

describe('TopicCard', () => {
  describe('Basic rendering', () => {
    it('displays topic name', () => {
      render(<TopicCard topic={MOCK_TOPIC} />)
      expect(screen.getByText(MOCK_TOPIC.name)).toBeInTheDocument()
    })

    it('hides edit/delete buttons and active class when isSelected is false', () => {
      const { container } = render(<TopicCard topic={MOCK_TOPIC} isSelected={false} />)
      expect(container.querySelector('.topic-card')).not.toHaveClass('topic-card--active')
      expect(screen.queryByTitle(tr('learning:topic.editTopic'))).not.toBeInTheDocument()
      expect(screen.queryByTitle(tr('learning:topic.deleteTopic'))).not.toBeInTheDocument()
    })

    it('shows edit/delete buttons and active class when isSelected is true', () => {
      const { container } = render(<TopicCard topic={MOCK_TOPIC} isSelected />)
      expect(container.querySelector('.topic-card')).toHaveClass('topic-card--active')
      expect(screen.getByTitle(tr('learning:topic.editTopic'))).toBeInTheDocument()
      expect(screen.getByTitle(tr('learning:topic.deleteTopic'))).toBeInTheDocument()
    })
  })

  describe('Interactions', () => {
    it('calls onSelect with topic when topic name is clicked', async () => {
      const user = userEvent.setup()
      const onSelect = vi.fn()
      render(<TopicCard topic={MOCK_TOPIC} onSelect={onSelect} />)

      await user.click(screen.getByText(MOCK_TOPIC.name))
      expect(onSelect).toHaveBeenCalledWith(MOCK_TOPIC)
    })

    it('calls onEdit and onDelete with topic when action buttons are clicked', async () => {
      const user = userEvent.setup()
      const onEdit = vi.fn()
      const onDelete = vi.fn()
      render(<TopicCard topic={MOCK_TOPIC} isSelected onEdit={onEdit} onDelete={onDelete} />)

      await user.click(screen.getByTitle(tr('learning:topic.editTopic')))
      expect(onEdit).toHaveBeenCalledWith(MOCK_TOPIC)

      await user.click(screen.getByTitle(tr('learning:topic.deleteTopic')))
      expect(onDelete).toHaveBeenCalledWith(MOCK_TOPIC)
    })

    it('does not crash when onSelect, onEdit, or onDelete are undefined', async () => {
      const user = userEvent.setup()
      render(<TopicCard topic={MOCK_TOPIC} isSelected />)

      await user.click(screen.getByText(MOCK_TOPIC.name))
      await user.click(screen.getByTitle(tr('learning:topic.editTopic')))
      await user.click(screen.getByTitle(tr('learning:topic.deleteTopic')))
    })
  })
})
