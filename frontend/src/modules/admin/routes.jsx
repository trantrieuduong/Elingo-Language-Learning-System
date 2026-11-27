import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminLayout from './layouts/AdminLayout';
import DashboardPage from './pages/DashboardPage/DashboardPage';
import PlaceholderAdminPage from './pages/PlaceholderAdminPage/PlaceholderAdminPage';
import DeckListPage from './pages/DeckManagementPage/DeckListPage';
import DeckDetailPage from './pages/DeckManagementPage/DeckDetailPage';
import LessonListPage from './pages/LessonManagementPage/LessonListPage';
import LessonDetailPage from './pages/LessonManagementPage/LessonDetailPage';
import TopicDetailPage from './pages/TopicManagementPage/TopicDetailPage';
import TagListPage from './pages/TagManagementPage/TagListPage';
import SystemConfigPage from './pages/SystemConfigPage/SystemConfigPage';
import AdminAuditLogPage from './pages/AdminAuditLogPage/AdminAuditLogPage';
import UserManagementPage from './pages/UserManagementPage/UserManagementPage';
import PlanManagementPage from './pages/PlanManagementPage/PlanManagementPage';
import TransactionManagementPage from './pages/TransactionManagementPage/TransactionManagementPage';
import WebhookLogPage from './pages/WebhookLogPage/WebhookLogPage';
import PostModerationPage from './pages/PostModerationPage/PostModerationPage';
import ReportManagementPage from './pages/ReportManagementPage/ReportManagementPage';
import PostGroupManagementPage from './pages/PostGroupManagementPage/PostGroupManagementPage';

export default function AdminRoutes({ currentPath, onNavigate }) {
  const { t } = useTranslation('admin');

  const renderPage = () => {
    const [pathname, queryString] = currentPath.split('?');
    const searchParams = new URLSearchParams(queryString || '');

    // 1. Nested topic detail: /admin/decks/{deck_id}/topics/{topic_id}
    const topicDetailMatch = pathname.match(/^\/admin\/decks\/(\d+)\/topics\/(\d+)$/);
    if (topicDetailMatch) {
      return (
        <TopicDetailPage
          deckId={topicDetailMatch[1]}
          topicId={topicDetailMatch[2]}
          onNavigate={onNavigate}
        />
      );
    }

    // 2. Deck topics subpath fallback: /admin/decks/{deck_id}/topics -> /admin/decks/{deck_id}
    const deckTopicsMatch = pathname.match(/^\/admin\/decks\/(\d+)\/topics\/?$/);
    if (deckTopicsMatch) {
      return <DeckDetailPage deckId={deckTopicsMatch[1]} onNavigate={onNavigate} />;
    }

    // 3. Deck detail: /admin/decks/{deck_id}
    const deckDetailMatch = pathname.match(/^\/admin\/decks\/(\d+)$/);
    if (deckDetailMatch) {
      return <DeckDetailPage deckId={deckDetailMatch[1]} onNavigate={onNavigate} />;
    }

    // 4. Lesson detail: /admin/lessons/{lesson_id}
    const lessonDetailMatch = pathname.match(/^\/admin\/lessons\/(\d+)$/);
    if (lessonDetailMatch) {
      return <LessonDetailPage lessonId={lessonDetailMatch[1]} onNavigate={onNavigate} />;
    }

    switch (pathname) {
      case '/admin':
      case '/admin/dashboard':
        return <DashboardPage onNavigate={onNavigate} />;

      // Module: Nội dung và học tập
      case '/admin/decks':
        return <DeckListPage onNavigate={onNavigate} />;
      case '/admin/lessons':
        return <LessonListPage onNavigate={onNavigate} />;
      case '/admin/tags':
        return <TagListPage onNavigate={onNavigate} />;

      // Module: Premium và thanh toán
      case '/admin/plans':
        return <PlanManagementPage onNavigate={onNavigate} />;
      case '/admin/transactions':
        return <TransactionManagementPage onNavigate={onNavigate} />;
      case '/admin/webhook-logs':
        return (
          <WebhookLogPage
            initialTransactionId={searchParams.get('transactionId') || ''}
            onNavigate={onNavigate}
          />
        );

      // Module: Bài viết và cộng đồng
      case '/admin/posts':
        return (
          <PostModerationPage
            initialPostId={searchParams.get('postId') || ''}
            onNavigate={onNavigate}
          />
        );
      case '/admin/comments':
        return <PlaceholderAdminPage title={t('routes.comments')} onNavigate={onNavigate} />;
      case '/admin/groups':
        return <PostGroupManagementPage onNavigate={onNavigate} />;

      // Module: Báo cáo
      case '/admin/reports':
        return <ReportManagementPage onNavigate={onNavigate} />;

      // Module: Người dùng
      case '/admin/users':
        return <UserManagementPage onNavigate={onNavigate} />;
      case '/admin/roles':
        return <PlaceholderAdminPage title={t('routes.roles')} onNavigate={onNavigate} />;

      // Module: Cấu hình hệ thống
      case '/admin/settings':
      case '/admin/settings/configs':
        return <SystemConfigPage onNavigate={onNavigate} />;
      case '/admin/settings/audit-logs':
        return <AdminAuditLogPage onNavigate={onNavigate} />;

      default:
        return <DashboardPage onNavigate={onNavigate} />;
    }
  };

  return (
    <AdminLayout currentPath={currentPath} onNavigate={onNavigate}>
      {renderPage()}
    </AdminLayout>
  );
}

AdminRoutes.propTypes = {
  currentPath: PropTypes.string.isRequired,
  onNavigate: PropTypes.func.isRequired,
};
