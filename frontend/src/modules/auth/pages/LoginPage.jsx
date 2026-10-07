import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import Input from '../../../components/Input/Input'
import { useAuth } from '../../../context/AuthContext'
import './AuthPages.css'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID
let googleScriptPromise = null

const loadGoogleIdentityScript = (lang = 'vi') => {
  const scriptId = 'google-gsi-script'
  const currentScript = document.getElementById(scriptId)
  const targetSrc = `https://accounts.google.com/gsi/client?hl=${lang}`

  if (currentScript && currentScript.getAttribute('data-lang') === lang && window.google?.accounts?.id) {
    return Promise.resolve()
  }

  if (currentScript) {
    currentScript.remove()
    if (window.google?.accounts) {
      delete window.google.accounts
    }
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.id = scriptId
    script.src = targetSrc
    script.async = true
    script.defer = true
    script.setAttribute('data-lang', lang)
    script.onload = () => {
      setTimeout(resolve, 50)
    }
    script.onerror = () => reject(new Error('google_load_error'))
    document.head.appendChild(script)
  })
}

function LoginPage({ onNavigate }) {
  const { t, i18n } = useTranslation('auth')
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
    if (!username.trim()) newErrors.username = t('usernameRequired')
    if (!password) newErrors.password = t('passwordRequired')
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

    // Map code-specific errors: USER_NOT_FOUND vào field, còn lại vào general error
    if (result.code === 'USER_NOT_FOUND') {
      setErrors((prev) => ({ ...prev, username: result.message }))
    } else {
      setError(result.message)
    }
  }

  const handleSignupClick = (e) => {
    e.preventDefault()
    if (onNavigate) onNavigate('/signup')
  }

  const handleForgotPasswordClick = (e) => {
    e.preventDefault()
    if (onNavigate) onNavigate('/reset-password')
  }

  const handleGoogleCredential = useCallback(async (credentialResponse) => {
    if (!credentialResponse.credential) {
      if (isMountedRef.current) setError(t('googleCredentialError'))
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
  }, [loginWithGoogle, onNavigate, t])

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID || !googleButtonRef.current) return undefined

    let isActive = true
    let renderedWidth = 0
    let renderedLang = ''
    const currentLang = (i18n.resolvedLanguage || 'vi').startsWith('en') ? 'en' : 'vi'

    const renderGoogleButton = async () => {
      try {
        await loadGoogleIdentityScript(currentLang)
        if (!isActive || !googleButtonRef.current) return

        const buttonWidth = Math.floor(googleButtonRef.current.getBoundingClientRect().width)
        if (!buttonWidth || (buttonWidth === renderedWidth && renderedLang === currentLang)) return

        renderedWidth = buttonWidth
        renderedLang = currentLang
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
          locale: currentLang,
          width: buttonWidth,
        })
      } catch (googleError) {
        if (isActive) setError(t('googleServiceError'))
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
  }, [handleGoogleCredential, i18n.resolvedLanguage, t])

  return (
    <main className="auth-page">
      <section className="auth-card glass-card">
        <span className="badge badge--primary auth-card__badge">
          <span className="material-symbols-outlined">login</span>
          {t('badge')}
        </span>
        <div className="auth-card__header">
          <h1>{t('title')}</h1>
          <p>{t('subtitle')}</p>
        </div>

        {error && <div className="auth-alert auth-alert--error">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <Input
            id="login-username"
            label={t('usernameLabel')}
            type="text"
            placeholder={t('usernamePlaceholder')}
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
            label={t('passwordLabel')}
            type="password"
            placeholder={t('passwordPlaceholder')}
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
            {t('forgotPassword')}
          </a>

          <button type="submit" className="btn-primary auth-submit" disabled={isSubmitting}>
            {isSubmitting ? t('submitting') : t('submit')}
            {!isSubmitting && <span className="material-symbols-outlined">arrow_forward</span>}
          </button>
        </form>

        <div className="auth-divider" aria-hidden="true"><span>{t('dividerOr')}</span></div>
        {GOOGLE_CLIENT_ID ? (
          <div className={`auth-google-button ${isGoogleSubmitting ? 'auth-google-button--loading' : ''}`} ref={googleButtonRef} />
        ) : (
          <p className="auth-google-config-error">{t('googleNotConfigured')}</p>
        )}

        <p className="auth-card__footer">
          {t('noAccount')}{' '}
          <a href="/signup" onClick={handleSignupClick}>
            {t('signupNow')}
          </a>
        </p>
      </section>
    </main>
  )
}

export default LoginPage
