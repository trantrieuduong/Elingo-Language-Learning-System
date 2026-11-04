import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Header from './components/Header/Header';
import Footer from './components/Footer/Footer';
import { useAuth } from './context/AuthContext';
import AccountVerificationPage from './modules/auth/pages/AccountVerificationPage';
import LandingPage from './modules/home/pages/LandingPage';
import LoginPage from './modules/auth/pages/LoginPage';
import ResetPasswordPage from './modules/auth/pages/ResetPasswordPage';
import SignupPage from './modules/auth/pages/SignupPage';
import DeckListPage from './modules/vocabulary/pages/DeckListPage/DeckListPage';
import TopicListPage from './modules/vocabulary/pages/TopicListPage/TopicListPage';
import FlashcardStudyPage from './modules/learning/flashcard/pages/FlashcardStudyPage';
import FlashcardReviewPage from './modules/learning/flashcard/pages/FlashcardReviewPage';
import LessonListPage from './modules/learning/lesson/pages/LessonListPage';
import DictationPage from './modules/learning/dictation/pages/DictationPage';
import ShadowingPage from './modules/learning/shadowing/pages/ShadowingPage';
import CommunityFeedPage from './modules/community/pages/CommunityFeedPage/CommunityFeedPage';
import PostGroupPage from './modules/community/pages/PostGroupPage/PostGroupPage';
import MyFeedPage from './modules/community/pages/MyFeedPage/MyFeedPage';
import PostDetailPage from './modules/community/pages/PostDetailPage/PostDetailPage';
import { BattleSocketProvider } from './modules/battle/context/BattleSocketContext';
import BattleLobbyPage from './modules/battle/pages/BattleLobbyPage';
import BattlePlayPage from './modules/battle/pages/BattlePlayPage';
import './App.css';

const PUBLIC_PATHS = ['/login', '/signup', '/account-verification', '/reset-password'];

// Các public route có thể xem cả khi chưa đăng nhập và khi đã đăng nhập (không redirect về /dashboard nếu đã đăng nhập)
const SHARED_PUBLIC_PATHS = ['/', '/decks', '/lessons'];

const PRIVATE_USER_PATHS = [
  '/dashboard',
  '/review',
  '/battle',
  '/speaking',
  '/community',
  '/community/groups',
  '/community/my-posts',
  '/progress',
  '/gamification',
  '/premium',
  '/premium/my-subscription',
  '/profile',
  '/notification',
];

function App() {
  const { user, loading } = useAuth();
  const [currentPath, setCurrentPath] = useState(window.location.pathname);
  const [verificationEmail, setVerificationEmail] = useState(
    () => window.history.state?.email ?? ''
  );

  useEffect(() => {
    const handlePopState = (event) => {
      setCurrentPath(window.location.pathname);
      setVerificationEmail(event.state?.email ?? '');
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const flashcardStudyMatch = currentPath.match(/^\/decks\/([a-z0-9]+(?:-[a-z0-9]+)*)$/);
  const dictationMatch = currentPath.match(/^\/lessons\/dictation\/([^/]+)$/);
  const shadowingMatch = currentPath.match(/^\/lessons\/shadowing\/([^/]+)$/);
  const battlePlayMatch = currentPath.match(/^\/battle\/play\/(\d+)$/);
  const deckListMatch = currentPath === '/decks';
  const deckTopicsMatch = currentPath.match(/^\/decks\/([^/]+)\/topics$/);
  const postDetailMatch = currentPath.match(/^\/community\/posts\/(\d+)$/);
  const isPrivateUserPath =
    PRIVATE_USER_PATHS.some((path) => {
      return currentPath === path || currentPath.startsWith(`${path}/`);
    }) ||
    !!flashcardStudyMatch ||
    !!deckTopicsMatch ||
    !!dictationMatch ||
    !!shadowingMatch;

  useEffect(() => {
    if (loading) return;

    const isAuthOnlyPath = PUBLIC_PATHS.includes(currentPath);
    const isAdmin = user?.role === 'ADMIN';
    const isAdminPath = currentPath.startsWith('/admin');

    if (!user && (isAdminPath || isPrivateUserPath)) {
      navigate('/login');
      return;
    }

    if (user && isAuthOnlyPath) {
      navigate(isAdmin ? '/admin' : '/dashboard');
      return;
    }

    if (user && isAdminPath && !isAdmin) {
      navigate('/dashboard');
      return;
    }

    if (user && isAdmin && !isAdminPath) {
      navigate('/admin');
    }
  }, [currentPath, user, loading, isPrivateUserPath]);

  const navigate = (path, param) => {
    if (path === -1) {
      window.history.back();
      return;
    }

    const stateObj = param && typeof param === 'object' ? param : {};
    window.history.pushState({ ...stateObj, previousPath: window.location.pathname }, '', path);
    setCurrentPath(window.location.pathname);

    if (path === '/account-verification') {
      const email = typeof param === 'string' ? param : param?.email;
      if (email) setVerificationEmail(email);
    }
  };

  if (loading) {
    return (
      <div className="app-loading-screen">
        <div className="app-loading-spinner" />
      </div>
    );
  }

  const renderAdminContent = () => {
    return (
      <main className="placeholder-page">
        <div className="placeholder-page__card glass-card">
          <span className="material-symbols-outlined placeholder-page__icon">
            admin_panel_settings
          </span>
          <h1 className="text-headline-md">Admin area</h1>
          <p className="text-body-md text-disabled">
            This module is outside the current implementation scope.
          </p>
        </div>
      </main>
    );
  };

  const renderBattle = (page) => <BattleSocketProvider>{page}</BattleSocketProvider>;

  const renderContent = () => {
    if (postDetailMatch) {
      return (
        <PostDetailPage
          key={currentPath}
          postId={postDetailMatch[1]}
          canGoBack={['/community', '/community/my-posts'].includes(
            window.history.state?.previousPath
          )}
          onNavigate={navigate}
        />
        )}
    if (battlePlayMatch) {
      return renderBattle(
        <BattlePlayPage matchId={battlePlayMatch[1]} onNavigate={navigate} />
      );
    }

    if (flashcardStudyMatch) {
      return (
        <FlashcardStudyPage
          deckSlug={flashcardStudyMatch[1]}
          returnTo={window.history.state?.returnTo}
          onNavigate={navigate}
        />
      );
    }

    if (deckTopicsMatch) {
      return (
        <TopicListPage deckId={decodeURIComponent(deckTopicsMatch[1])} onNavigate={navigate} />
      );
    }

    if (dictationMatch) {
      return <DictationPage slug={decodeURIComponent(dictationMatch[1])} onNavigate={navigate} />;
    }

    if (shadowingMatch) {
      return <ShadowingPage slug={decodeURIComponent(shadowingMatch[1])} onNavigate={navigate} />;
    }

    switch (currentPath) {
      case '/':
        return <LandingPage onNavigate={navigate} />;
      case '/login':
        return <LoginPage onNavigate={navigate} />;
      case '/signup':
        return <SignupPage onNavigate={navigate} />;
      case '/reset-password':
        return <ResetPasswordPage onNavigate={navigate} />;
      case '/account-verification':
        return <AccountVerificationPage email={verificationEmail} onNavigate={navigate} />;
      case '/dashboard':
        return <PlaceholderPage title="Dashboard" path={currentPath} />;
      case '/lessons':
        return <LessonListPage onNavigate={navigate} />;
      case '/review':
        return <FlashcardReviewPage onNavigate={navigate} />;
      case '/battle':
        return renderBattle(<BattleLobbyPage onNavigate={navigate} />);
      case '/speaking':
        return <PlaceholderPage title="Speaking" path={currentPath} />;
      case '/community':
        return (
          <CommunityFeedPage
            initialSelectedGroupId={window.history.state?.selectedGroupId}
            onNavigate={navigate}
          />
        );
      case '/community/my-posts':
        return <MyFeedPage onNavigate={navigate} />;
      case '/community/groups':
        return <PostGroupPage onNavigate={navigate} />;

      case '/progress':
        return <PlaceholderPage title="Progress" path={currentPath} />;
      case '/gamification':
        return <PlaceholderPage title="Gamification" path={currentPath} />;
      case '/premium':
        return <PlaceholderPage title="Premium" path={currentPath} />;
      case '/premium/my-subscription':
        return <PlaceholderPage title="My Subscription" path={currentPath} />;
      case '/profile':
        return <PlaceholderPage title="Profile" path={currentPath} />;
      case '/decks':
        return <DeckListPage onNavigate={navigate} />;
      case '/notification':
        return <PlaceholderPage title="Notifications" path={currentPath} />;
      default:
        return <NotFoundPage onNavigate={navigate} />;
    }
  };

  if (currentPath.startsWith('/admin')) {
    return renderAdminContent();
  }

  return (
    <>
      <div className="ambient-bg">
        <div className="ambient-bg__orb ambient-bg__orb--blue" />
        <div className="ambient-bg__orb ambient-bg__orb--lavender" />
        <div className="ambient-bg__orb ambient-bg__orb--mint" />
        <div className="ambient-bg__orb ambient-bg__orb--pink" />
        <div className="ambient-bg__orb ambient-bg__orb--ambient" />
      </div>
      <Header onNavigate={navigate} currentPath={currentPath} />
      <div className={'app-main'}>{renderContent()}</div>
      <Footer onNavigate={navigate} />
    </>
  );
}

function PlaceholderPage({ title, path }) {
  const { t } = useTranslation();
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
  );
}

function NotFoundPage({ onNavigate }) {
  const { t } = useTranslation();
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
  );
}

export default App;
