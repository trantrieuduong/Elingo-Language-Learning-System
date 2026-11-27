import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  fetchSocialStats,
  fetchModerationStats,
  fetchReportStats,
} from '../../../services/adminStatsApi';
import useAdminTabStats from '../../../hooks/useAdminTabStats';
import AdminStatsTabShell from '../components/AdminStatsTabShell';
import MetricCard from '../../../components/MetricCard/MetricCard';
import AreaTimeSeriesChart from '../../../components/VisualCharts/AreaTimeSeriesChart';
import CategoryBreakdown from '../../../components/VisualCharts/CategoryBreakdown';
import {
  MessageSquareIcon,
  ShieldCheckIcon,
  ClockIcon,
  AlertTriangleIcon,
} from '../../../components/icons/AdminIcons';
import styles from '../DashboardPage.module.css';

export default function CommunityStatsTab({ filter, reloadKey, onNavigate }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language && i18n.language.startsWith('en') ? 'en-US' : 'vi-VN';

  const { loading, error, data } = useAdminTabStats(
    async () => {
      const [social, moderation, reports] = await Promise.all([
        fetchSocialStats(filter),
        fetchModerationStats(filter),
        fetchReportStats(filter),
      ]);
      return { social, moderation, reports };
    },
    [filter, reloadKey],
    t('dashboard.common.errorLoading'),
  );

  if (loading || error) {
    return <AdminStatsTabShell loading={loading} error={error} />;
  }

  const { social: socialData, moderation: moderationData, reports: reportData } = data;

  // Chuẩn hóa điểm tương tác
  const engagementPoints = (socialData?.engagementSeries || []).map((pt) => ({
    date: pt.date,
    likeCount: pt.likeCount,
    commentCount: pt.commentCount,
    totalInteractions: pt.totalInteractions,
  }));

  // Chuẩn hóa nhãn AI
  const aiLabels = (moderationData?.aiLabelDistribution || []).map((ai) => ({
    label: ai.category,
    count: ai.count,
    percentage: ai.percentage,
    color:
      ai.category === 'CLEAN'
        ? 'var(--adm-success)'
        : ai.category === 'SPAM'
          ? 'var(--adm-warning)'
          : 'var(--adm-danger)',
  }));

  // Chuẩn hóa vi phạm theo đối tượng
  const targetItems = (reportData?.byTargetType || []).map((item) => ({
    label: item.category,
    count: item.count,
    percentage: item.percentage,
    color: 'var(--adm-chart-primary)',
  }));

  const backlog = moderationData?.backlog || {};
  const actions = moderationData?.actions || {};

  return (
    <div className={styles.tabContent}>
      {/* KPI Ribbon (5 thẻ cân đối) */}
      <div className={styles.kpiRow5}>
        <MetricCard
          title={t('dashboard.communityTab.totalPosts')}
          value={socialData?.totalPosts?.toLocaleString(locale) || 0}
          subvalue={t('dashboard.communityTab.likesAndCommentsSub', {
            likes: socialData?.totalLikes || 0,
            comments: socialData?.totalComments || 0,
          })}
          icon={MessageSquareIcon}
          variant="primary"
        />
        <MetricCard
          title={t('dashboard.communityTab.automationRate')}
          value={`${actions.automationRate || 0}%`}
          subvalue={t('dashboard.communityTab.autoApprovedSub', {
            count: actions.autoApproveCount || 0,
          })}
          icon={ShieldCheckIcon}
          variant="success"
        />
        <MetricCard
          title={t('dashboard.communityTab.avgReviewTime')}
          value={`${moderationData?.avgManualReviewSeconds || 0}s`}
          subvalue={t('dashboard.communityTab.manualApprovedSub', {
            count: actions.manualApproveCount || 0,
          })}
          icon={ClockIcon}
        />
        <MetricCard
          title={t('dashboard.communityTab.pendingModerationPosts')}
          value={backlog.totalBacklog || 0}
          subvalue={t('dashboard.communityTab.moderationBacklogSub', {
            needsReview: backlog.needsReviewCount || 0,
            pending: backlog.pendingCount || 0,
          })}
          icon={ShieldCheckIcon}
          variant={
            backlog.totalBacklog > 10
              ? 'danger'
              : backlog.totalBacklog > 0
                ? 'warning'
                : 'default'
          }
          href="/admin/posts"
          onClick={() => onNavigate && onNavigate('/admin/posts')}
        />
        <MetricCard
          title={t('dashboard.communityTab.pendingReportsTitle')}
          value={reportData?.pendingReports || 0}
          subvalue={t('dashboard.communityTab.totalReportsSub', {
            total: reportData?.totalReports || 0,
          })}
          icon={AlertTriangleIcon}
          variant={(reportData?.pendingReports || 0) > 0 ? 'warning' : 'default'}
          href="/admin/reports"
          onClick={() => onNavigate && onNavigate('/admin/reports')}
        />
      </div>

      {/* Main Grid */}
      <div className={styles.visualGrid}>
        {/* Left Column: Social Rhythm Chart */}
        <div className={styles.chartCol}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>{t('dashboard.communityTab.socialTrendTitle')}</h3>
            <p className={styles.sectionDesc}>{t('dashboard.communityTab.socialTrendDesc')}</p>
          </div>
          <AreaTimeSeriesChart
            data={engagementPoints}
            series={[
              {
                key: 'likeCount',
                label: t('dashboard.communityTab.likesLabel'),
                color: 'var(--adm-chart-primary)',
              },
              {
                key: 'commentCount',
                label: t('dashboard.communityTab.commentsLabel'),
                color: 'var(--adm-chart-secondary)',
              },
            ]}
            height={260}
          />
        </div>

        {/* Right Column: AI Labels and Reports */}
        <div className={styles.sideCol}>
          <CategoryBreakdown
            title={t('dashboard.communityTab.aiLabelTitle')}
            items={aiLabels}
            total={actions.totalActions}
            totalLabel={t('dashboard.communityTab.totalScanned')}
          />

          <CategoryBreakdown
            title={t('dashboard.communityTab.reportsByTarget')}
            items={targetItems}
            total={reportData?.totalReports}
            totalLabel={t('dashboard.communityTab.totalReportsLabel')}
          />
        </div>
      </div>
    </div>
  );
}

CommunityStatsTab.propTypes = {
  filter: PropTypes.shape({
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
    granularity: PropTypes.string.isRequired,
  }).isRequired,
  reloadKey: PropTypes.number.isRequired,
  onNavigate: PropTypes.func,
};
