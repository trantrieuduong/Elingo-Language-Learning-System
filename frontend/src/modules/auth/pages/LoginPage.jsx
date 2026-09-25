import { useCallback, useEffect, useRef, useState } from 'react'
import Input from '../../../components/Input/Input'
import { useAuth } from '../../../context/AuthContext'
import './AuthPages.css'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID
let googleScriptPromise = null

const loadGoogleIdentityScript = () => {
  if (window.google?.accounts?.id) return Promise.resolve()
  if (googleScriptPromise) return googleScriptPromise

  googleScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.defer = true
    script.onload = resolve
    script.onerror = () => reject(new Error('Không thể tải dịch vụ đăng nhập Google.'))
    document.head.appendChild(script)
  })

  return googleScriptPromise
}

function LoginPage({ onNavigate }) {
  const { login, loginWithGoogle } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isGoogleSubmitting, setIsGoogleSubmitting] = useState(false)
  const isMountedRef = useRef(true)
  const googleButtonRef = useRef(null)

  useEffect(() => () => {
    isMountedRef.current = false
  }, [])

  const validate = () => {
    const newErrors = {}

    if (!username.trim()) newErrors.username = 'Email hoặc Username là bắt buộc.'
    if (!password) newErrors.password = 'Mật khẩu là bắt buộc.'

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!validate()) return

    setIsSubmitting(true)
    const result = await login(username.trim(), password)
    setIsSubmitting(false)

    if (result.success) {
      if (onNavigate) {
        onNavigate(result.user?.role === 'ADMIN' ? '/admin' : '/dashboard')
      }
      return
    }

    if (result.notVerified) {
      if (onNavigate) onNavigate('/account-verification', { email: username.trim() })
      return
    }

    setError(result.message)
  }

  const handleSignupClick = (e) => {
    e.preventDefault()
    if (onNavigate) onNavigate('/signup')
  }

  const handleForgotPasswordClick = (e) => {
    e.preventDefault()
  }

  const handleGoogleCredential = useCallback(async (credentialResponse) => {
    if (!credentialResponse.credential) {
      if (isMountedRef.current) setError('Không nhận được thông tin đăng nhập từ Google.')
      return
    }

    setIsGoogleSubmitting(true)
    const result = await loginWithGoogle(credentialResponse.credential)
    if (!isMountedRef.current) return
    setIsGoogleSubmitting(false)

    if (result.success) {
      if (onNavigate) onNavigate(result.user?.role === 'ADMIN' ? '/admin' : '/dashboard')
      return
    }

    setError(result.message)
  }, [loginWithGoogle, onNavigate])

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID || !googleButtonRef.current) return undefined

    let isActive = true
    let renderedWidth = 0

    const renderGoogleButton = async () => {
      try {
        await loadGoogleIdentityScript()
        if (!isActive || !googleButtonRef.current) return

        const buttonWidth = Math.floor(googleButtonRef.current.getBoundingClientRect().width)
        if (!buttonWidth || buttonWidth === renderedWidth) return

        renderedWidth = buttonWidth
        googleButtonRef.current.replaceChildren()

        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: handleGoogleCredential,
          auto_select: false,
          cancel_on_tap_outside: true,
        })
        window.google.accounts.id.renderButton(googleButtonRef.current, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: 'signin_with',
          shape: 'pill',
          locale: 'vi',
          width: buttonWidth,
        })
      } catch (googleError) {
        if (isActive) setError(googleError.message || 'Không thể tải đăng nhập Google. Vui lòng thử lại.')
      }
    }

    renderGoogleButton()
    const resizeObserver = new ResizeObserver(() => {
      renderGoogleButton()
    })
    resizeObserver.observe(googleButtonRef.current)

    return () => {
      isActive = false
      resizeObserver.disconnect()
    }
  }, [handleGoogleCredential])

  return (
    <main className="auth-page">
      <section className="auth-card glass-card">
        <span className="badge badge--primary auth-card__badge">
          <span className="material-symbols-outlined">login</span>
          Chào mừng bạn trở lại
        </span>
        <div className="auth-card__header">
          <h1>Đăng nhập Elingo</h1>
          <p>Tiếp tục bài học, ôn tập và luyện nói của bạn.</p>
        </div>

        {error && <div className="auth-alert auth-alert--error">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <Input
            id="login-username"
            label="Email hoặc Username"
            type="text"
            placeholder="you@example.com"
            value={username}
            onChange={(e) => {
              setUsername(e.target.value)
              if (errors.username) setErrors((prev) => ({ ...prev, username: '' }))
              if (error) setError('')
            }}
            error={errors.username}
            disabled={isSubmitting}
            autoFocus
          />

          <Input
            id="login-password"
            label="Mật khẩu"
            type="password"
            placeholder="Nhập mật khẩu"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              if (errors.password) setErrors((prev) => ({ ...prev, password: '' }))
              if (error) setError('')
            }}
            error={errors.password}
            disabled={isSubmitting}
          />

          <a className="auth-forgot-password" href="#forgot-password" onClick={handleForgotPasswordClick}>
            Quên mật khẩu?
          </a>

          <button type="submit" className="btn-primary auth-submit" disabled={isSubmitting}>
            {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
            {!isSubmitting && <span className="material-symbols-outlined">arrow_forward</span>}
          </button>
        </form>

        <div className="auth-divider" aria-hidden="true"><span>hoặc</span></div>
        {GOOGLE_CLIENT_ID ? (
          <div className={`auth-google-button ${isGoogleSubmitting ? 'auth-google-button--loading' : ''}`} ref={googleButtonRef} />
        ) : (
          <p className="auth-google-config-error">Google Sign-In chưa được cấu hình.</p>
        )}

        <p className="auth-card__footer">
          Chưa có tài khoản Elingo?{' '}
          <a href="/signup" onClick={handleSignupClick}>
            Đăng ký ngay
          </a>
        </p>
      </section>
    </main>
  )
}

export default LoginPage
