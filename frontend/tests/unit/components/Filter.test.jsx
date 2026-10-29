import { describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Filter from '@/components/Filter/Filter'

const cefrLevels = [
  { _id: 'a1', name: 'A1' },
  { _id: 'a2', name: 'A2' },
]

const tags = [
  { _id: 'travel', name: 'Du lịch' },
  { _id: 'food', name: 'Ẩm thực' },
]

describe('Filter component', () => {
  it('không hiển thị nhóm bộ lọc nào khi không có tùy chọn', () => {
    const { container } = render(<Filter />)

    expect(container.querySelector('.filter-bar')).toBeInTheDocument()
    expect(container.querySelectorAll('.filter-group')).toHaveLength(0)
  })

  it('hiển thị tùy chọn CEFR và chủ đề cùng trạng thái được chọn chính xác', () => {
    render(
      <Filter
        cefrLevels={cefrLevels}
        tags={tags}
        selectedCefrLevelIds={['a2']}
        onCefrChange={vi.fn()}
        onTagChange={vi.fn()}
      />
    )

    const levelGroup = screen.getByText('Cấp độ').closest('.filter-group')
    const tagGroup = screen.getByText('Chủ đề').closest('.filter-group')

    expect(levelGroup).not.toBeNull()
    expect(tagGroup).not.toBeNull()

    expect(within(levelGroup).getByRole('button', { name: 'A2' }))
      .toHaveClass('filter-pill--active')
    expect(within(levelGroup).getByRole('button', { name: 'A1' }))
      .not.toHaveClass('filter-pill--active')
    expect(within(levelGroup).getByRole('button', { name: 'Tất cả' }))
      .not.toHaveClass('filter-pill--active')

    expect(within(tagGroup).getByRole('button', { name: 'Du lịch' }))
      .not.toHaveClass('filter-pill--active')
    expect(within(tagGroup).getByRole('button', { name: 'Tất cả' }))
      .toHaveClass('filter-pill--active')
  })

  it('thêm và bỏ chọn một cấp độ CEFR', async () => {
    const user = userEvent.setup()
    const onCefrChange = vi.fn()
    const props = {
      cefrLevels,
      selectedCefrLevelIds: [],
      onCefrChange,
    }
    const { rerender } = render(<Filter {...props} />)

    await user.click(screen.getByRole('button', { name: 'A1' }))
    expect(onCefrChange).toHaveBeenLastCalledWith(['a1'])

    rerender(<Filter {...props} selectedCefrLevelIds={['a1']} />)

    await user.click(screen.getByRole('button', { name: 'A1' }))
    expect(onCefrChange).toHaveBeenLastCalledWith([])
  })

  it('thêm và bỏ chọn chủ đề mà vẫn giữ các lựa chọn khác', async () => {
    const user = userEvent.setup()
    const onTagChange = vi.fn()
    const props = {
      tags,
      selectedTagIds: ['travel'],
      onTagChange,
    }
    const { rerender } = render(<Filter {...props} />)

    await user.click(screen.getByRole('button', { name: 'Ẩm thực' }))
    expect(onTagChange).toHaveBeenLastCalledWith(['travel', 'food'])

    rerender(<Filter {...props} selectedTagIds={['travel', 'food']} />)

    await user.click(screen.getByRole('button', { name: 'Du lịch' }))
    expect(onTagChange).toHaveBeenLastCalledWith(['food'])
  })

  it('xóa lựa chọn của từng nhóm bộ lọc khi nhấn nút "Tất cả"', async () => {
    const user = userEvent.setup()
    const onCefrChange = vi.fn()
    const onTagChange = vi.fn()

    render(
      <Filter
        cefrLevels={cefrLevels}
        tags={tags}
        selectedCefrLevelIds={['a1']}
        selectedTagIds={['travel']}
        onCefrChange={onCefrChange}
        onTagChange={onTagChange}
      />
    )

    const levelGroup = screen.getByText('Cấp độ').closest('.filter-group')
    const tagGroup = screen.getByText('Chủ đề').closest('.filter-group')

    await user.click(within(levelGroup).getByRole('button', { name: 'Tất cả' }))
    await user.click(within(tagGroup).getByRole('button', { name: 'Tất cả' }))

    expect(onCefrChange).toHaveBeenCalledWith([])
    expect(onTagChange).toHaveBeenCalledWith([])
  })

  it('chỉ hiển thị nhóm bộ lọc có tùy chọn', () => {
    render(
      <Filter
        cefrLevels={cefrLevels}
        onCefrChange={vi.fn()}
        onTagChange={vi.fn()}
      />
    )

    expect(screen.getByText('Cấp độ')).toBeInTheDocument()
    expect(screen.queryByText('Chủ đề')).not.toBeInTheDocument()
  })
})