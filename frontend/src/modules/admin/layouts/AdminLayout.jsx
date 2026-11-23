import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import AdminSidebar from '../components/AdminSidebar/AdminSidebar';
import AdminHeader from '../components/AdminHeader/AdminHeader';
import '../styles/admin-theme.css';
import styles from './AdminLayout.module.css';

export default function AdminLayout({ currentPath, onNavigate, children }) {
  // Theme state: light or dark
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('admin_theme') || 'light';
  });

  // Sidebar collapse on desktop
  const [isCollapsed, setIsCollapsed] = useState(() => {
    return localStorage.getItem('admin_sidebar_collapsed') === 'true';
  });

  // Mobile drawer state
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  useEffect(() => {
    localStorage.setItem('admin_theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const toggleCollapse = () => {
    setIsCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('admin_sidebar_collapsed', String(next));
      return next;
    });
  };

  const expandSidebar = () => {
    setIsCollapsed(false);
    localStorage.setItem('admin_sidebar_collapsed', 'false');
  };

  return (
    <div className="admin-scope" data-theme={theme}>
      <div className={styles.layoutWrapper}>
        {/* Sidebar */}
        <AdminSidebar
          currentPath={currentPath}
          onNavigate={onNavigate}
          isCollapsed={isCollapsed}
          onToggleCollapse={toggleCollapse}
          onExpand={expandSidebar}
          isMobileOpen={isMobileOpen}
          onCloseMobile={() => setIsMobileOpen(false)}
        />

        {/* Main Content Area */}
        <div className={`${styles.mainArea} ${isCollapsed ? styles.mainAreaCollapsed : ''}`}>
          <AdminHeader
            currentPath={currentPath}
            theme={theme}
            onToggleTheme={toggleTheme}
            onOpenMobileSidebar={() => setIsMobileOpen(true)}
            onNavigate={onNavigate}
          />

          <main className={styles.contentBody}>{children}</main>
        </div>
      </div>
    </div>
  );
}

AdminLayout.propTypes = {
  currentPath: PropTypes.string.isRequired,
  onNavigate: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
};
