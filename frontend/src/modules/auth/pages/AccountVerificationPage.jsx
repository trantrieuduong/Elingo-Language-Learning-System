import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import Input from '../../../components/Input/Input'
import { useAuth } from '../../../context/AuthContext'
import './AuthPages.css'

const OTP_RESEND_COOLDOWN = Number(import.meta.env.VITE_OTP_RESEND_COOLDOWN) || 60

function AccountVerificationPage({ email: initialEmail = '', onNavigate }) {
  const { t } = useTranslation('auth')
  const { verifyAccount, resendVerificationOtp } = useAuth()
  const [otp, setOtp] = useState('')
  const [otpError, setOtpError] = useState('')
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isResending, setIsResending] = useState(false)
  const [resendCooldown, setResendCooldown] = useState(0)
  const redirectTimerRef = useRef(null)

  useEffect(() => {
    if (resendCooldown <= 0) return undefined
    const timer = setTimeout(() => setResendCooldown((seconds) => seconds - 1), 1000)
    return () => clearTimeout(timer)
  }, [resendCooldown])

  useEffect(() => () => clearTimeout(redirectTimerRef.current), [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccessMsg('')

    if (!otp.trim()) {
      setOtpError(t('otpRequired'))
      return
    }

    if (!initialEmail) {
      setError(t('verifyError'))
      return
    }

    setIsSubmitting(true)
    const result = await verifyAccount(initialEmail, otp.trim())
    setIsSubmitting(false)

    if (result.success) {
      setSuccessMsg(result.message)
      redirectTimerRef.current = setTimeout(() => {
        if (onNavigate) onNavigate('/login')
      }, 800)
      return
    }

    // Xử lý lỗi field-specific
    if (result.code === 'OTP_INVALID') {
      setOtpError(result.message)
    } else {
      setError(result.message)
    }
  }

  const handleResendOtp = async () => {
    if (!initialEmail || resendCooldown > 0) return

    setError('')
    setSuccessMsg('')
    setIsResending(true)
    const result = await resendVerificationOtp(initialEmail)
    setIsResending(false)

    if (result.success) {
      setSuccessMsg(result.message)
      setResendCooldown(OTP_RESEND_COOLDOWN)
      return
    }

    setError(result.message)
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
          <span className="material-symbols-outlined">mark_email_read</span>
          {t('verifyBadge')}
        </span>
        <div className="auth-card__header">
          <h1>{t('verifyTitle')}</h1>
          <p>{t('verifySubtitle')}</p>
        </div>

        {error && <div className="auth-alert auth-alert--error">{error}</div>}
        {successMsg && <div className="auth-alert auth-alert--success">{successMsg}</div>}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <Input
            id="verification-otp"
            label={t('otpLabel')}
            type="text"
            placeholder="123456"
            value={otp}
            onChange={(e) => {
              setOtp(e.target.value)
              if (otpError) setOtpError('')
              if (error) setError('')
            }}
            disabled={isSubmitting}
            error={otpError}
            inputMode="numeric"
            maxLength={6}
            autoFocus
          />

          <button type="submit" className="btn-primary auth-submit" disabled={isSubmitting || !initialEmail}>
            {isSubmitting ? t('submittingVerify') : t('submitVerify')}
            {!isSubmitting && <span className="material-symbols-outlined">check_circle</span>}
          </button>
        </form>

        <button
          type="button"
          className="auth-link-button"
          onClick={handleResendOtp}
          disabled={isResending || isSubmitting || resendCooldown > 0 || !initialEmail}
        >
          {isResending
            ? t('resendingCode')
            : resendCooldown > 0
              ? t('resendAfter', { time: formatCooldown() })
              : t('resendCode')}
        </button>
      </section>
    </main>
  )
}

export default AccountVerificationPage
