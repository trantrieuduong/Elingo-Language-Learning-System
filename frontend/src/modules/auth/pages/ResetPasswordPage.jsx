import { useEffect, useRef, useState } from 'react'
import Input from '../../../components/Input/Input'
import { useAuth } from '../../../context/AuthContext'
import './AuthPages.css'

const initialForm = {
  email: '',
  otp: '',
  newPassword: '',
  confirmPassword: '',
}

const PASSWORD_REQUIREMENTS = [
  { key: 'length', label: 'Ít nhất 8 ký tự', isMet: (password) => password.length >= 8 },
  { key: 'case', label: 'Ít nhất có một chữ hoa và một chữ thường', isMet: (password) => /[A-Z]/.test(password) && /[a-z]/.test(password) },
  { key: 'number', label: 'Ít nhất có một số', isMet: (password) => /\d/.test(password) },
  { key: 'special', label: 'Ít nhất có một ký tự đặc biệt', isMet: (password) => /[^A-Za-z0-9]/.test(password) },
]

const OTP_RESEND_COOLDOWN = 60

function ResetPasswordPage({ onNavigate }) {
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
      setErrors((prev) => ({ ...prev, email: 'Email là bắt buộc.' }))
      return false
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setErrors((prev) => ({ ...prev, email: 'Email không đúng định dạng.' }))
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
      setSuccessMsg('Đã gửi mã OTP. Vui lòng kiểm tra email của bạn.')
      setResendCooldown(OTP_RESEND_COOLDOWN)
    } else {
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
      newErrors.email = 'Email là bắt buộc.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      newErrors.email = 'Email không đúng định dạng.'
    }

    if (!form.otp.trim()) {
      newErrors.otp = 'Mã OTP là bắt buộc.'
    }

    if (!form.newPassword) {
      newErrors.newPassword = 'Mật khẩu mới là bắt buộc.'
    } else if (!PASSWORD_REQUIREMENTS.every((requirement) => requirement.isMet(form.newPassword))) {
      newErrors.newPassword = 'Mật khẩu phải có ít nhất 8 ký tự và bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'
    }

    if (form.newPassword !== form.confirmPassword) {
      newErrors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
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
      setSuccessMsg('Đặt lại mật khẩu thành công.')
      redirectTimerRef.current = setTimeout(() => {
        if (onNavigate) onNavigate('/login')
      }, 1200)
    } else {
      if (result.code === 'OTP_INVALID') {
        setErrors((prev) => ({ ...prev, otp: result.message }))
      } else if (result.code === 'EMAIL_NOT_EXISTED' || result.code === 'USER_NOT_FOUND') {
        setErrors((prev) => ({ ...prev, email: result.message }))
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
          Khôi phục tài khoản
        </span>
        <div className="auth-card__header">
          <h1>Đặt lại mật khẩu</h1>
          <p>Nhập email đã đăng ký để nhận mã OTP và tạo mật khẩu mới.</p>
        </div>

        {generalError && <div className="auth-alert auth-alert--error">{generalError}</div>}
        {successMsg && <div className="auth-alert auth-alert--success">{successMsg}</div>}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <Input
            id="reset-email"
            label="Email đã đăng ký"
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
                    ? 'Đang gửi...'
                    : resendCooldown > 0
                      ? `Gửi lại (${formatCooldown()})`
                      : isOtpSent
                        ? 'Gửi lại mã OTP'
                        : 'Gửi mã OTP'}
                </span>
              </button>
            }
          />

          <Input
            id="reset-otp"
            label="Mã OTP"
            type="text"
            placeholder="Nhập mã OTP 6 chữ số"
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
              label="Mật khẩu mới"
              type="password"
              placeholder="Nhập mật khẩu mới"
              value={form.newPassword}
              onChange={(e) => handleChange('newPassword', e.target.value)}
              error={errors.newPassword}
              disabled={isSubmitting}
            />
            <PasswordRequirements password={form.newPassword} />
          </div>

          <Input
            id="reset-confirm-password"
            label="Xác nhận mật khẩu mới"
            type="password"
            placeholder="Nhập lại mật khẩu mới"
            value={form.confirmPassword}
            onChange={(e) => handleChange('confirmPassword', e.target.value)}
            error={errors.confirmPassword}
            disabled={isSubmitting}
          />

          <button type="submit" className="btn-primary auth-submit" disabled={isSubmitting}>
            {isSubmitting ? 'Đang xử lý...' : 'Xác nhận đặt lại mật khẩu'}
            {!isSubmitting && <span className="material-symbols-outlined">arrow_forward</span>}
          </button>
        </form>

        <p className="auth-card__footer">
          Quay lại{' '}
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

export default ResetPasswordPage
