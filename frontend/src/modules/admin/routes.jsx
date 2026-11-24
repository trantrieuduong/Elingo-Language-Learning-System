import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminLayout from './layouts/AdminLayout';
import DashboardPage from './pages/DashboardPage/DashboardPage';
import PlaceholderAdminPage from './pages/PlaceholderAdminPage/PlaceholderAdminPage';
import DeckListPage from './pages/DeckManagementPage/DeckListPage';
import DeckDetailPage from './pages/DeckManagementPage/DeckDetailPage';
import TopicDetailPage from './pages/TopicManagementPage/TopicDetailPage';
import TagListPage from './pages/TagManagementPage/TagListPage';
import SystemConfigPage from './pages/SystemConfigPage/SystemConfigPage';
import AdminAuditLogPage from './pages/AdminAuditLogPage/AdminAuditLogPage';
import UserManagementPage from './pages/UserManagementPage/UserManagementPage';

export default function AdminRoutes({ currentPath, onNavigate }) {
  const { t } = useTranslation('admin');

  const renderPage = () => {
    // 1. Nested topic detail: /admin/decks/{deck_id}/topics/{topic_id}
    const topicDetailMatch = currentPath.match(/^\/admin\/decks\/(\d+)\/topics\/(\d+)$/);
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
    const deckTopicsMatch = currentPath.match(/^\/admin\/decks\/(\d+)\/topics\/?$/);
    if (deckTopicsMatch) {
      return <DeckDetailPage deckId={deckTopicsMatch[1]} onNavigate={onNavigate} />;
    }

    // 3. Deck detail: /admin/decks/{deck_id}
    const deckDetailMatch = currentPath.match(/^\/admin\/decks\/(\d+)$/);
    if (deckDetailMatch) {
      return <DeckDetailPage deckId={deckDetailMatch[1]} onNavigate={onNavigate} />;
    }

    switch (currentPath) {
      case '/admin':
      case '/admin/dashboard':
        return <DashboardPage onNavigate={onNavigate} />;

      // Module: Nội dung và học tập
      case '/admin/decks':
        return <DeckListPage onNavigate={onNavigate} />;
      case '/admin/tags':
        return <TagListPage onNavigate={onNavigate} />;

      // Module: Premium và thanh toán
      case '/admin/plans':
        return <PlaceholderAdminPage title={t('routes.plans')} onNavigate={onNavigate} />;
      case '/admin/transactions':
        return <PlaceholderAdminPage title={t('routes.transactions')} onNavigate={onNavigate} />;

      // Module: Bài viết và cộng đồng
      case '/admin/posts':
        return <PlaceholderAdminPage title={t('routes.posts')} onNavigate={onNavigate} />;
      case '/admin/comments':
        return <PlaceholderAdminPage title={t('routes.comments')} onNavigate={onNavigate} />;
      case '/admin/groups':
        return <PlaceholderAdminPage title={t('routes.groups')} onNavigate={onNavigate} />;

      // Module: Báo cáo
      case '/admin/reports':
        return <PlaceholderAdminPage title={t('routes.reports')} onNavigate={onNavigate} />;

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
