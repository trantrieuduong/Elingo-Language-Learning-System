import { useState } from 'react'
import Input from '../../../components/Input/Input'
import { useAuth } from '../../../context/AuthContext'
import './AuthPages.css'

const initialForm = {
  username: '',
  email: '',
  fullName: '',
  password: '',
  confirmPassword: '',
}

const PASSWORD_REQUIREMENTS = [
  { key: 'length', label: 'Ít nhất 8 ký tự', isMet: (password) => password.length >= 8 },
  { key: 'case', label: 'Ít nhất có một chữ hoa và một chữ thường', isMet: (password) => /[A-Z]/.test(password) && /[a-z]/.test(password) },
  { key: 'number', label: 'Ít nhất có một số', isMet: (password) => /\d/.test(password) },
  { key: 'special', label: 'Ít nhất có một ký tự đặc biệt', isMet: (password) => /[^A-Za-z0-9]/.test(password) },
]

const USERNAME_PATTERN = /^[A-Za-z0-9]{3,15}$/

const SIGNUP_ERROR_MESSAGES = {
  USERNAME_EXISTED: 'Username đã tồn tại.',
  EMAIL_EXISTED: 'Email đã tồn tại.',
}

const getSignupErrorMessage = (errors, fallbackMessage) => {
  const errorCode = errors?.[0]?.code
  return SIGNUP_ERROR_MESSAGES[errorCode] ?? errors?.[0]?.message ?? fallbackMessage
}

function SignupPage({ onNavigate }) {
  const { signup } = useAuth()
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [generalError, setGeneralError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const validate = () => {
    const newErrors = {}
    if (!form.fullName.trim()) newErrors.fullName = 'Họ và tên là bắt buộc.'
    if (!form.username.trim()) {
      newErrors.username = 'Username là bắt buộc.'
    } else if (!USERNAME_PATTERN.test(form.username)) {
      newErrors.username = 'Username gồm 3-15 chữ cái hoặc chữ số, không có khoảng trắng hay ký tự đặc biệt.'
    }
    if (!form.email.trim()) {
      newErrors.email = 'Email là bắt buộc.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      newErrors.email = 'Email không đúng định dạng.'
    }
    if (!form.password) {
      newErrors.password = 'Mật khẩu là bắt buộc.'
    } else if (!PASSWORD_REQUIREMENTS.every((requirement) => requirement.isMet(form.password))) {
      newErrors.password = 'Mật khẩu phải có ít nhất 8 ký tự và bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'
    }
    if (form.confirmPassword !== form.password) {
      newErrors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }))
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }))
    }
    if (generalError) setGeneralError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setGeneralError('')
    if (!validate()) return

    setIsSubmitting(true)
    const result = await signup({
      username: form.username.trim(),
      email: form.email.trim(),
      fullName: form.fullName.trim(),
      password: form.password,
    })
    setIsSubmitting(false)

    if (result.success) {
      if (onNavigate) onNavigate('/account-verification', { email: form.email.trim() })
      return
    }

    setGeneralError(getSignupErrorMessage(result.errors, result.message))
  }

  const handleLoginClick = (e) => {
    e.preventDefault()
    if (onNavigate) onNavigate('/login')
  }

  return (
    <main className="auth-page">
      <section className="auth-card auth-card--wide glass-card">
        <span className="badge badge--mint auth-card__badge">
          <span className="material-symbols-outlined">person_add</span>
          Bắt đầu cùng Elingo
        </span>
        <div className="auth-card__header">
          <h1>Tạo tài khoản Elingo</h1>
          <p>Thiết lập hồ sơ học viên và xác thực email để bắt đầu.</p>
        </div>

        {generalError && <div className="auth-alert auth-alert--error">{generalError}</div>}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="auth-form__grid">
            <Input
              id="signup-full-name"
              label="Họ và tên"
              type="text"
              placeholder="Nguyễn Văn A"
              value={form.fullName}
              onChange={(e) => handleChange('fullName', e.target.value)}
              error={errors.fullName}
              disabled={isSubmitting}
              autoFocus
            />
            <Input
              id="signup-username"
              label="Username"
              type="text"
              placeholder="ANguyen"
              value={form.username}
              onChange={(e) => handleChange('username', e.target.value)}
              error={errors.username}
              disabled={isSubmitting}
              maxLength={15}
            />
          </div>

          <Input
            id="signup-email"
            label="Email"
            type="email"
            placeholder="AN123@example.com"
            value={form.email}
            onChange={(e) => handleChange('email', e.target.value)}
            error={errors.email}
            disabled={isSubmitting}
          />

          <div className="auth-form__grid">
            <div className="auth-password-field">
              <Input
                id="signup-password"
                label="Mật khẩu"
                type="password"
                placeholder="Nhập mật khẩu"
                value={form.password}
                onChange={(e) => handleChange('password', e.target.value)}
                error={errors.password}
                disabled={isSubmitting}
              />
              <PasswordRequirements password={form.password} />
            </div>
            <Input
              id="signup-confirm-password"
              label="Xác nhận mật khẩu"
              type="password"
              placeholder="Nhập lại mật khẩu"
              value={form.confirmPassword}
              onChange={(e) => handleChange('confirmPassword', e.target.value)}
              error={errors.confirmPassword}
              disabled={isSubmitting}
            />
          </div>

          <button type="submit" className="btn-primary auth-submit" disabled={isSubmitting}>
            {isSubmitting ? 'Đang tạo tài khoản...' : 'Tạo tài khoản'}
            {!isSubmitting && <span className="material-symbols-outlined">arrow_forward</span>}
          </button>
        </form>

        <p className="auth-card__footer">
          Đã có tài khoản?{' '}
          <a href="/login" onClick={handleLoginClick}>
            Đăng nhập
          </a>
        </p>
      </section>
    </main>
  )
}

function PasswordRequirements({ password }) {
  const hasPasswordInput = password.length > 0

  return (
    <ul className="auth-password-requirements" aria-label="Yêu cầu mật khẩu">
      {PASSWORD_REQUIREMENTS.map((requirement) => {
        const isMet = requirement.isMet(password)
        const statusClass = !hasPasswordInput
          ? 'auth-password-requirements__item--neutral'
          : isMet
            ? 'auth-password-requirements__item--met'
            : 'auth-password-requirements__item--unmet'
        return (
          <li className={statusClass} key={requirement.key}>
            <span className="material-symbols-outlined">{isMet ? 'check_circle' : 'cancel'}</span>
            {requirement.label}
          </li>
        )
      })}
    </ul>
  )
}

export default SignupPage
