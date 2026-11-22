import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import AdminLayout from './layouts/AdminLayout';
import DashboardPage from './pages/DashboardPage/DashboardPage';
import PlaceholderAdminPage from './pages/PlaceholderAdminPage/PlaceholderAdminPage';

export default function AdminRoutes({ currentPath, onNavigate }) {
  const { t } = useTranslation('admin');

  const renderPage = () => {
    switch (currentPath) {
      case '/admin':
      case '/admin/dashboard':
        return <DashboardPage onNavigate={onNavigate} />;

      // Module: Nội dung và học tập
      case '/admin/decks':
        return <PlaceholderAdminPage title={t('routes.decks')} onNavigate={onNavigate} />;
      case '/admin/lessons':
        return <PlaceholderAdminPage title={t('routes.lessons')} onNavigate={onNavigate} />;
      case '/admin/topics':
        return <PlaceholderAdminPage title={t('routes.topics')} onNavigate={onNavigate} />;

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
        return <PlaceholderAdminPage title={t('routes.users')} onNavigate={onNavigate} />;
      case '/admin/roles':
        return <PlaceholderAdminPage title={t('routes.roles')} onNavigate={onNavigate} />;

      // Module: Cấu hình hệ thống
      case '/admin/settings':
        return <PlaceholderAdminPage title={t('routes.settings')} onNavigate={onNavigate} />;

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
