import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Input from '@/components/Input/Input'

describe('Input Component', () => {
  it('should render label and input field', () => {
    render(<Input id="test-input" label="Username" value="" onChange={() => {}} />)
    expect(screen.getByLabelText('Username')).toBeInTheDocument()
  })

  it('should render error message when error prop is provided', () => {
    render(<Input id="test-input" label="Email" error="Invalid email" value="" onChange={() => {}} />)
    expect(screen.getByText('Invalid email')).toBeInTheDocument()
  })

  it('should render custom rightElement', () => {
    render(
      <Input id="test-input" label="Search" value="" onChange={() => {}} rightElement={<button>Search</button>} />
    )
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument()
  })

  it('should toggle password visibility', async () => {
    const user = userEvent.setup()
    render(<Input id="test-pass" label="Password" type="password" value="secret" onChange={() => {}} />)

    const input = screen.getByLabelText('Password')
    expect(input).toHaveAttribute('type', 'password')

    const toggleButton = screen.getByRole('button', { name: 'visibility' })
    await user.click(toggleButton)
    expect(input).toHaveAttribute('type', 'text')
    expect(toggleButton).toHaveTextContent('visibility_off')
  })
})
