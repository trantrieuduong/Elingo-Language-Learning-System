import React from 'react';
import PropTypes from 'prop-types';
import { AdminSkeletonCard, AdminErrorState } from '../../../components/Feedback/AdminFeedback';
import styles from '../DashboardPage.module.css';

/**
 * Khung hiển thị trạng thái loading/error dùng chung cho các tab thống kê Dashboard Admin.
 */
export default function AdminStatsTabShell({ loading, error, children }) {
  if (loading) {
    return (
      <div className={styles.loadingGrid}>
        <AdminSkeletonCard />
        <AdminSkeletonCard />
        <AdminSkeletonCard />
        <AdminSkeletonCard />
      </div>
    );
  }

  if (error) {
    return <AdminErrorState message={error} onRetry={() => {}} />;
  }

  return children;
}

AdminStatsTabShell.propTypes = {
  loading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  children: PropTypes.node,
};
