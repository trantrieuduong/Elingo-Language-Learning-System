import { useState } from 'react'
import { useTranslation } from 'react-i18next'
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

const USERNAME_PATTERN = /^[A-Za-z0-9]{3,15}$/

function SignupPage({ onNavigate }) {
  const { t } = useTranslation('auth')
  const { signup } = useAuth()
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [generalError, setGeneralError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const validate = () => {
    const newErrors = {}
    if (!form.fullName.trim()) newErrors.fullName = t('fullNameRequired')
    if (!form.username.trim()) {
      newErrors.username = t('usernameSignupRequired')
    } else if (!USERNAME_PATTERN.test(form.username)) {
      newErrors.username = t('usernameSignupInvalid')
    }
    if (!form.email.trim()) {
      newErrors.email = t('emailRequired')
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      newErrors.email = t('emailInvalid')
    }
    if (!form.password) {
      newErrors.password = t('passwordSignupRequired')
    } else if (!getPasswordRequirements(form.password).every((r) => r.isMet)) {
      newErrors.password = t('passwordSignupInvalid')
    }
    if (form.confirmPassword !== form.password) {
      newErrors.confirmPassword = t('confirmPasswordMismatch')
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

    // Xử lý validation errors từ backend
    if (result.errors && Array.isArray(result.errors)) {
      const newErrors = {}
      result.errors.forEach((err) => {
        if (err.field && err.message) {
          newErrors[err.field] = err.message
        }
      })
      if (Object.keys(newErrors).length > 0) {
        setErrors((prev) => ({ ...prev, ...newErrors }))
        return
      }
    }

    // Map code-specific errors vào field tương ứng
    const fieldErrorMap = {
      USERNAME_EXISTED: 'username',
      EMAIL_EXISTED: 'email',
    }

    const field = fieldErrorMap[result.code]
    if (field) {
      setErrors((prev) => ({ ...prev, [field]: result.message }))
    } else {
      setGeneralError(result.message)
    }
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
          {t('signupBadge')}
        </span>
        <div className="auth-card__header">
          <h1>{t('signupTitle')}</h1>
          <p>{t('signupSubtitle')}</p>
        </div>

        {generalError && <div className="auth-alert auth-alert--error">{generalError}</div>}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="auth-form__grid">
            <Input
              id="signup-full-name"
              label={t('fullNameLabel')}
              type="text"
              placeholder={t('fullNamePlaceholder')}
              value={form.fullName}
              onChange={(e) => handleChange('fullName', e.target.value)}
              error={errors.fullName}
              disabled={isSubmitting}
              autoFocus
            />
            <Input
              id="signup-username"
              label={t('usernameSignupLabel')}
              type="text"
              placeholder={t('usernameSignupPlaceholder')}
              value={form.username}
              onChange={(e) => handleChange('username', e.target.value)}
              error={errors.username}
              disabled={isSubmitting}
              maxLength={15}
            />
          </div>

          <Input
            id="signup-email"
            label={t('emailLabel')}
            type="email"
            placeholder={t('emailPlaceholder')}
            value={form.email}
            onChange={(e) => handleChange('email', e.target.value)}
            error={errors.email}
            disabled={isSubmitting}
          />

          <div className="auth-form__grid">
            <div className="auth-password-field">
              <Input
                id="signup-password"
                label={t('passwordSignupLabel')}
                type="password"
                placeholder={t('passwordSignupPlaceholder')}
                value={form.password}
                onChange={(e) => handleChange('password', e.target.value)}
                error={errors.password}
                disabled={isSubmitting}
              />
              <PasswordRequirements password={form.password} t={t} />
            </div>
            <Input
              id="signup-confirm-password"
              label={t('confirmPasswordLabel')}
              type="password"
              placeholder={t('confirmPasswordPlaceholder')}
              value={form.confirmPassword}
              onChange={(e) => handleChange('confirmPassword', e.target.value)}
              error={errors.confirmPassword}
              disabled={isSubmitting}
            />
          </div>

          <button type="submit" className="btn-primary auth-submit" disabled={isSubmitting}>
            {isSubmitting ? t('submittingSignup') : t('submitSignup')}
            {!isSubmitting && <span className="material-symbols-outlined">arrow_forward</span>}
          </button>
        </form>

        <p className="auth-card__footer">
          {t('hasAccount')}{' '}
          <a href="/login" onClick={handleLoginClick}>
            {t('loginLink')}
          </a>
        </p>
      </section>
    </main>
  )
}

function getPasswordRequirements(password) {
  return [
    { key: 'length', isMet: password.length >= 8 },
    { key: 'case', isMet: /[A-Z]/.test(password) && /[a-z]/.test(password) },
    { key: 'number', isMet: /\d/.test(password) },
    { key: 'special', isMet: /[^A-Za-z0-9]/.test(password) },
  ]
}

function PasswordRequirements({ password, t }) {
  const hasPasswordInput = password.length > 0
  const requirements = getPasswordRequirements(password)

  return (
    <ul className="auth-password-requirements" aria-label={t('passwordRequirements.title')}>
      {requirements.map((req) => {
        const statusClass = !hasPasswordInput
          ? 'auth-password-requirements__item--neutral'
          : req.isMet
            ? 'auth-password-requirements__item--met'
            : 'auth-password-requirements__item--unmet'
        return (
          <li className={statusClass} key={req.key}>
            <span className="material-symbols-outlined">{req.isMet ? 'check_circle' : 'cancel'}</span>
            {t(`passwordRequirements.${req.key}`)}
          </li>
        )
      })}
    </ul>
  )
}

export default SignupPage
