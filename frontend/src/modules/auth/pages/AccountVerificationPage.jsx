import { useEffect, useRef, useState } from 'react'
import Input from '../../../components/Input/Input'
import { useAuth } from '../../../context/AuthContext'
import './AuthPages.css'

const OTP_RESEND_COOLDOWN = 60

function AccountVerificationPage({ email: initialEmail = '', onNavigate }) {
  const { verifyAccount, resendVerificationOtp } = useAuth()
  const [otp, setOtp] = useState('')
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

    if (!initialEmail || !otp.trim()) {
      setError('Vui lòng nhập mã xác thực gồm 6 chữ số.')
      return
    }

    setIsSubmitting(true)
    const result = await verifyAccount(initialEmail, otp.trim())
    setIsSubmitting(false)

    if (result.success) {
      setSuccessMsg('Tài khoản đã được xác thực. Bạn có thể đăng nhập ngay.')
      redirectTimerRef.current = setTimeout(() => {
        if (onNavigate) onNavigate('/login')
      }, 800)
      return
    }

    setError(result.message)
  }

  const handleResendOtp = async () => {
    if (!initialEmail || resendCooldown > 0) return

    setError('')
    setSuccessMsg('')
    setIsResending(true)
    const result = await resendVerificationOtp(initialEmail)
    setIsResending(false)

    if (result.success) {
      setSuccessMsg('Mã xác thực mới đã được gửi đến email của bạn.')
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
          Xác thực email
        </span>
        <div className="auth-card__header">
          <h1>Xác thực tài khoản</h1>
          <p>Nhập mã gồm 6 chữ số đã được gửi đến địa chỉ email của bạn.</p>
        </div>

        {error && <div className="auth-alert auth-alert--error">{error}</div>}
        {successMsg && <div className="auth-alert auth-alert--success">{successMsg}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <Input
            id="verification-email"
            label="Email"
            type="email"
            value={initialEmail}
            disabled
          />
          <Input
            id="verification-otp"
            label="Mã xác thực"
            type="text"
            placeholder="123456"
            value={otp}
            onChange={(e) => {
              setOtp(e.target.value)
              if (error) setError('')
            }}
            disabled={isSubmitting}
            inputMode="numeric"
            maxLength={6}
            autoFocus
          />

          <button type="submit" className="btn-primary auth-submit" disabled={isSubmitting || !initialEmail}>
            {isSubmitting ? 'Đang xác thực...' : 'Xác thực tài khoản'}
            {!isSubmitting && <span className="material-symbols-outlined">check_circle</span>}
          </button>
        </form>

        <button
          type="button"
          className="auth-link-button"
          onClick={handleResendOtp}
          disabled={isResending || isSubmitting || resendCooldown > 0 || !initialEmail}
        >
          {isResending ? 'Đang gửi mã...' : resendCooldown > 0 ? `Gửi lại mã sau ${formatCooldown()}` : 'Gửi lại mã xác thực'}
        </button>
      </section>
    </main>
  )
}

export default AccountVerificationPage
