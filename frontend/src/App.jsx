import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import Header from './components/Header/Header'
import Footer from './components/Footer/Footer'
import { useAuth } from './context/AuthContext'
import AccountVerificationPage from './modules/auth/pages/AccountVerificationPage'
import LandingPage from './modules/home/pages/LandingPage'
import LoginPage from './modules/auth/pages/LoginPage'
import ResetPasswordPage from './modules/auth/pages/ResetPasswordPage'
import SignupPage from './modules/auth/pages/SignupPage'
import DeckListPage from './modules/vocabulary/pages/DeckListPage/DeckListPage'
import TopicListPage from './modules/vocabulary/pages/TopicListPage/TopicListPage'
import UserCardManagementPage from './modules/vocabulary/pages/UserCardManagementPage/UserCardManagementPage'
import FlashcardStudyPage from './modules/learning/flashcard/pages/FlashcardStudyPage'
import FlashcardReviewPage from './modules/learning/flashcard/pages/FlashcardReviewPage'
import './App.css'

const PUBLIC_PATHS = ['/login', '/signup', '/account-verification', '/reset-password']

// Các public route có thể xem cả khi chưa đăng nhập và khi đã đăng nhập (không redirect về /dashboard nếu đã đăng nhập)
const SHARED_PUBLIC_PATHS = ['/', '/decks']

const PRIVATE_USER_PATHS = [
  '/dashboard',
  '/learning',
  '/review',
  '/battle',
  '/speaking',
  '/community',
  '/community/topics',
  '/community/my-posts',
  '/progress',
  '/gamification',
  '/premium',
  '/premium/my-subscription',
  '/profile',
  '/notification',
]

function App() {
  const { user, loading } = useAuth()
  const [currentPath, setCurrentPath] = useState(window.location.pathname)
  const [verificationEmail, setVerificationEmail] = useState(() => window.history.state?.email ?? '')

  useEffect(() => {
    const handlePopState = (event) => {
      setCurrentPath(window.location.pathname)
      setVerificationEmail(event.state?.email ?? '')
    }
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  const flashcardStudyMatch = currentPath.match(/^\/decks\/([^/]+)$/)
  const deckTopicsMatch = currentPath.match(/^\/decks\/([^/]+)\/topics$/)
  const topicCardsMatch = currentPath.match(/^\/decks\/([^/]+)\/topics\/([^/]+)\/cards$/)

  useEffect(() => {
    if (loading) return

    const isAuthOnlyPath = PUBLIC_PATHS.includes(currentPath)
    const isAdmin = user?.role === 'ADMIN'
    const isAdminPath = currentPath.startsWith('/admin')
    const isPrivateUserPath = PRIVATE_USER_PATHS.some((path) => {
      return currentPath === path || currentPath.startsWith(`${path}/`)
    }) || !!flashcardStudyMatch // Trang học flashcard (/decks/:deckId) bắt buộc đăng nhập

    if (!user && (isAdminPath || isPrivateUserPath)) {
      navigate('/login')
      return
    }

    if (user && isAuthOnlyPath) {
      navigate(isAdmin ? '/admin' : '/dashboard')
      return
    }

    if (user && isAdminPath && !isAdmin) {
      navigate('/dashboard')
      return
    }

    if (user && isAdmin && !isAdminPath) {
      navigate('/admin')
    }
  }, [currentPath, user, loading])

  const navigate = (path, param) => {
    const stateObj = param && typeof param === 'object' ? param : {}
    window.history.pushState(stateObj, '', path)
    setCurrentPath(path)

    if (path === '/account-verification') {
      const email = typeof param === 'string' ? param : param?.email
      if (email) setVerificationEmail(email)
    }
  }

  if (loading) {
    return (
      <div className="app-loading-screen">
        <div className="app-loading-spinner" />
      </div>
    )
  }

  const isFlashcardPage = !!flashcardStudyMatch || currentPath === '/review'

  const renderAdminContent = () => {
    return (
      <main className="placeholder-page">
        <div className="placeholder-page__card glass-card">
          <span className="material-symbols-outlined placeholder-page__icon">admin_panel_settings</span>
          <h1 className="text-headline-md">Admin area</h1>
          <p className="text-body-md text-disabled">This module is outside the current implementation scope.</p>
        </div>
      </main>
    )
  }

  const renderContent = () => {
    if (flashcardStudyMatch) {
      return <FlashcardStudyPage deckId={flashcardStudyMatch[1]} onNavigate={navigate} />
    }

    if (deckTopicsMatch) {
      return <TopicListPage deckId={deckTopicsMatch[1]} onNavigate={navigate} />
    }

    if (topicCardsMatch) {
      return (
        <UserCardManagementPage
          deckId={topicCardsMatch[1]}
          topicId={topicCardsMatch[2]}
          onNavigate={navigate}
        />
      )
    }

    switch (currentPath) {
      case '/':
        return <LandingPage onNavigate={navigate} />
      case '/login':
        return <LoginPage onNavigate={navigate} />
      case '/signup':
        return <SignupPage onNavigate={navigate} />
      case '/reset-password':
        return <ResetPasswordPage onNavigate={navigate} />
      case '/account-verification':
        return <AccountVerificationPage email={verificationEmail} onNavigate={navigate} />
      case '/dashboard':
        return <PlaceholderPage title="Dashboard" path={currentPath} />
      case '/learning':
        return <PlaceholderPage title="Learning" path={currentPath} />
      case '/review':
        return <FlashcardReviewPage onNavigate={navigate} />
      case '/battle':
        return <PlaceholderPage title="Battle" path={currentPath} />
      case '/speaking':
        return <PlaceholderPage title="Speaking" path={currentPath} />
      case '/community':
        return <PlaceholderPage title="Community Feed" path={currentPath} />
      case '/community/topics':
        return <PlaceholderPage title="Community Topics" path={currentPath} />
      case '/community/my-posts':
        return <PlaceholderPage title="My Posts" path={currentPath} />
      case '/progress':
        return <PlaceholderPage title="Progress" path={currentPath} />
      case '/gamification':
        return <PlaceholderPage title="Gamification" path={currentPath} />
      case '/premium':
        return <PlaceholderPage title="Premium" path={currentPath} />
      case '/premium/my-subscription':
        return <PlaceholderPage title="My Subscription" path={currentPath} />
      case '/profile':
        return <PlaceholderPage title="Profile" path={currentPath} />
      case '/decks':
        return <DeckListPage onNavigate={navigate} />
      case '/notification':
        return <PlaceholderPage title="Notifications" path={currentPath} />
      default:
        return <NotFoundPage onNavigate={navigate} />
    }
  }

  if (currentPath.startsWith('/admin')) {
    return renderAdminContent()
  }

  return (
    <>
      <div className="ambient-bg" aria-hidden="true">
        <div className="ambient-bg__orb ambient-bg__orb--blue" />
        <div className="ambient-bg__orb ambient-bg__orb--lavender" />
        <div className="ambient-bg__orb ambient-bg__orb--mint" />
        <div className="ambient-bg__orb ambient-bg__orb--pink" />
        <div className="ambient-bg__orb ambient-bg__orb--ambient" />
      </div>
      {!isFlashcardPage && <Header onNavigate={navigate} currentPath={currentPath} />}
      <div
        className={`app-main ${deckTopicsMatch || topicCardsMatch ? 'app-main--vocabulary' : ''} ${topicCardsMatch ? 'app-main--card-management' : ''}`}
      >
        {renderContent()}
      </div>
      {!isFlashcardPage && <Footer onNavigate={navigate} />}
    </>
  )
}

function PlaceholderPage({ title, path }) {
  const { t } = useTranslation()
  return (
    <main className="placeholder-page">
      <div className="placeholder-page__card glass-card">
        <span className="material-symbols-outlined placeholder-page__icon">construction</span>
        <h1 className="text-headline-md">{title}</h1>
        <p className="text-body-md text-disabled">
          {t('underConstruction')} — Route: <code>{path}</code>
        </p>
      </div>
    </main>
  )
}

function NotFoundPage({ onNavigate }) {
  const { t } = useTranslation()
  return (
    <main className="placeholder-page">
      <div className="placeholder-page__card glass-card">
        <span className="material-symbols-outlined placeholder-page__icon">error</span>
        <h1 className="text-headline-md">{t('pageNotFound')}</h1>
        <button className="btn-primary" onClick={() => onNavigate('/')}>
          {t('backHome')}
        </button>
      </div>
    </main>
  )
}

export default App
