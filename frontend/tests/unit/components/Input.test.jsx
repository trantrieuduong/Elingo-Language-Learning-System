import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Input from '@/components/Input/Input'
import { LANGS, tr, useLang } from '../../utils/i18n'

describe('Input Component', () => {
  it('should render label and input field', () => {
    render(<Input id="test-input" label="Username" value="" onChange={() => {}} />)
    expect(screen.getByLabelText('Username')).toBeInTheDocument()
  })

  it('should render error message when error prop is provided', () => {
    render(<Input id="test-input" label="Email" error="Invalid email" value="" onChange={() => {}} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Invalid email')
  })

  it('should render custom rightElement', () => {
    render(
      <Input id="test-input" label="Search" value="" onChange={() => {}} rightElement={<button>Search</button>} />
    )
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument()
  })

  describe.each(LANGS)('password toggle [%s]', (lng) => {
    beforeEach(() => useLang(lng))

    it('should toggle password visibility with a translated label', async () => {
      const user = userEvent.setup()
      render(<Input id="test-pass" label="Password" type="password" value="secret" onChange={() => {}} />)

      const input = screen.getByLabelText('Password')
      expect(input).toHaveAttribute('type', 'password')

      await user.click(screen.getByRole('button', { name: tr('common:showPassword') }))
      expect(input).toHaveAttribute('type', 'text')
      expect(screen.getByRole('button', { name: tr('common:hidePassword') })).toBeInTheDocument()
    })
  })
})
