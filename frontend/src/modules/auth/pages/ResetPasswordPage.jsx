import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import Input from '../../../components/Input/Input'
import { useAuth } from '../../../context/AuthContext'
import './AuthPages.css'

const initialForm = {
  email: '',
  otp: '',
  newPassword: '',
  confirmPassword: '',
}

const OTP_RESEND_COOLDOWN = Number(import.meta.env.VITE_OTP_RESEND_COOLDOWN) || 60

function ResetPasswordPage({ onNavigate }) {
  const { t } = useTranslation('auth')
  const { sendResetPasswordOtp, resetPassword } = useAuth()

  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [generalError, setGeneralError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  const [isOtpSent, setIsOtpSent] = useState(false)
  const [isSendingOtp, setIsSendingOtp] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [resendCooldown, setResendCooldown] = useState(0)

  const redirectTimerRef = useRef(null)

  useEffect(() => {
    if (resendCooldown <= 0) return undefined
    const timer = setTimeout(() => setResendCooldown((seconds) => seconds - 1), 1000)
    return () => clearTimeout(timer)
  }, [resendCooldown])

  useEffect(() => () => clearTimeout(redirectTimerRef.current), [])

  const handleChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }))
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: '' }))
    }
    if (generalError) setGeneralError('')
  }

  const validateEmail = () => {
    if (!form.email.trim()) {
      setErrors((prev) => ({ ...prev, email: t('emailRequired') }))
      return false
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setErrors((prev) => ({ ...prev, email: t('emailInvalid') }))
      return false
    }
    return true
  }

  const handleSendOtp = async () => {
    setGeneralError('')
    setSuccessMsg('')
    if (!validateEmail()) return

    setIsSendingOtp(true)
    const result = await sendResetPasswordOtp(form.email.trim())
    setIsSendingOtp(false)

    if (result.success) {
      setIsOtpSent(true)
      setSuccessMsg(result.message)
      setResendCooldown(OTP_RESEND_COOLDOWN)
    } else {
      // Xử lý lỗi field-specific
      if (result.code === 'EMAIL_NOT_EXISTED' || result.code === 'USER_NOT_FOUND') {
        setErrors((prev) => ({ ...prev, email: result.message }))
      } else {
        setGeneralError(result.message)
      }
    }
  }

  const validate = () => {
    const newErrors = {}

    if (!form.email.trim()) {
      newErrors.email = t('emailRequired')
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      newErrors.email = t('emailInvalid')
    }

    if (!form.otp.trim()) {
      newErrors.otp = t('resetOtpRequired')
    }

    if (!form.newPassword) {
      newErrors.newPassword = t('resetNewPasswordRequired')
    } else if (!getPasswordRequirements(form.newPassword).every((r) => r.isMet)) {
      newErrors.newPassword = t('passwordSignupInvalid')
    }

    if (form.newPassword !== form.confirmPassword) {
      newErrors.confirmPassword = t('resetConfirmPasswordMismatch')
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setGeneralError('')
    setSuccessMsg('')
    if (!validate()) return

    setIsSubmitting(true)
    const result = await resetPassword(form.email.trim(), form.otp.trim(), form.newPassword)
    setIsSubmitting(false)

    if (result.success) {
      setSuccessMsg(result.message)
      redirectTimerRef.current = setTimeout(() => {
        if (onNavigate) onNavigate('/login')
      }, 1200)
    } else {
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
        OTP_INVALID: 'otp',
        EMAIL_NOT_EXISTED: 'email',
        USER_NOT_FOUND: 'email',
        PASSWORD_INCORRECT: 'newPassword',
        NEW_PASSWORD_SAME_AS_OLD: 'newPassword',
      }

      const field = fieldErrorMap[result.code]
      if (field) {
        setErrors((prev) => ({ ...prev, [field]: result.message }))
      } else {
        setGeneralError(result.message)
      }
    }
  }

  const handleLoginClick = (e) => {
    e.preventDefault()
    if (onNavigate) onNavigate('/login')
  }

  const formatCooldown = () => {
    const minutes = Math.floor(resendCooldown / 60)
    const seconds = resendCooldown % 60
    return `${minutes}:${String(seconds).padStart(2, '0')}`
  }

  return (
    <main className="auth-page">
      <section className="auth-card glass-card">
        <span className="badge badge--soft-blue auth-card__badge">
          <span className="material-symbols-outlined">lock_reset</span>
          {t('resetBadge')}
        </span>
        <div className="auth-card__header">
          <h1>{t('resetTitle')}</h1>
          <p>{t('resetSubtitle')}</p>
        </div>

        {generalError && <div className="auth-alert auth-alert--error">{generalError}</div>}
        {successMsg && <div className="auth-alert auth-alert--success">{successMsg}</div>}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <Input
            id="reset-email"
            label={t('resetEmailLabel')}
            type="email"
            placeholder="you@example.com"
            value={form.email}
            onChange={(e) => handleChange('email', e.target.value)}
            error={errors.email}
            disabled={isSendingOtp || isSubmitting}
            autoFocus
            rightElement={
              <button
                type="button"
                className="auth-send-otp-inside-btn"
                onClick={handleSendOtp}
                disabled={isSendingOtp || isSubmitting || resendCooldown > 0}
              >
                <span className="material-symbols-outlined">send</span>
                <span>
                  {isSendingOtp
                    ? t('sendingOtp')
                    : resendCooldown > 0
                      ? t('resendOtpTimer', { time: formatCooldown() })
                      : isOtpSent
                        ? t('resendOtp')
                        : t('sendOtp')}
                </span>
              </button>
            }
          />

          <Input
            id="reset-otp"
            label={t('resetOtpLabel')}
            type="text"
            placeholder={t('resetOtpPlaceholder')}
            value={form.otp}
            onChange={(e) => handleChange('otp', e.target.value)}
            error={errors.otp}
            disabled={isSubmitting}
            inputMode="numeric"
            maxLength={6}
          />

          <div className="auth-password-field">
            <Input
              id="reset-new-password"
              label={t('resetNewPasswordLabel')}
              type="password"
              placeholder={t('resetNewPasswordPlaceholder')}
              value={form.newPassword}
              onChange={(e) => handleChange('newPassword', e.target.value)}
              error={errors.newPassword}
              disabled={isSubmitting}
            />
            <PasswordRequirements password={form.newPassword} t={t} />
          </div>

          <Input
            id="reset-confirm-password"
            label={t('resetConfirmPasswordLabel')}
            type="password"
            placeholder={t('resetConfirmPasswordPlaceholder')}
            value={form.confirmPassword}
            onChange={(e) => handleChange('confirmPassword', e.target.value)}
            error={errors.confirmPassword}
            disabled={isSubmitting}
          />

          <button type="submit" className="btn-primary auth-submit" disabled={isSubmitting}>
            {isSubmitting ? t('submittingReset') : t('submitReset')}
            {!isSubmitting && <span className="material-symbols-outlined">arrow_forward</span>}
          </button>
        </form>

        <p className="auth-card__footer">
          {t('backToLogin')}{' '}
          <a href="/login" onClick={handleLoginClick}>
            {t('loginLinkText')}
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

export default ResetPasswordPage
