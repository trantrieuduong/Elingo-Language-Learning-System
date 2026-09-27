import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Input from '@/components/Input/Input'

describe('Input Component', () => {
  it('should render label and input field', () => {
    render(<Input id="test-input" label="Username" value="" onChange={() => {}} />)

    expect(screen.getByLabelText(/username/i)).toBeInTheDocument()
  })

  it('should render error message when error prop is provided', () => {
    render(<Input id="test-input" label="Email" error="Email không hợp lệ" value="" onChange={() => {}} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Email không hợp lệ')
  })

  it('should toggle password visibility on clicking eye icon', async () => {
    const user = userEvent.setup()
    render(<Input id="test-pass" label="Mật khẩu" type="password" value="secret" onChange={() => {}} />)

    const input = screen.getByLabelText(/^mật khẩu$/i)
    expect(input).toHaveAttribute('type', 'password')

    const toggleBtn = screen.getByRole('button', { name: /hiện mật khẩu/i })
    await user.click(toggleBtn)

    expect(input).toHaveAttribute('type', 'text')
  })

  it('should render custom rightElement', () => {
    render(
      <Input
        id="test-input"
        label="Search"
        value=""
        onChange={() => {}}
        rightElement={<button>Search</button>}
      />
    )

    expect(screen.getByRole('button', { name: /search/i })).toBeInTheDocument()
  })
})
