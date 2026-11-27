import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  fetchRevenueTimeSeries,
  fetchRevenueBreakdown,
  fetchSubscriptionStats,
} from '../../../services/adminStatsApi';
import useAdminTabStats from '../../../hooks/useAdminTabStats';
import AdminStatsTabShell from '../components/AdminStatsTabShell';
import MetricCard from '../../../components/MetricCard/MetricCard';
import AreaTimeSeriesChart from '../../../components/VisualCharts/AreaTimeSeriesChart';
import CategoryBreakdown from '../../../components/VisualCharts/CategoryBreakdown';
import {
  CreditCardIcon,
  TrendingUpIcon,
  CheckCircleIcon,
  ActivityIcon,
} from '../../../components/icons/AdminIcons';
import styles from '../DashboardPage.module.css';

export default function RevenueStatsTab({ filter, reloadKey }) {
  const { t, i18n } = useTranslation('admin');
  const isEn = i18n.language && i18n.language.startsWith('en');

  const { loading, error, data } = useAdminTabStats(
    async () => {
      const [timeSeries, breakdown, subs] = await Promise.all([
        fetchRevenueTimeSeries(filter),
        fetchRevenueBreakdown(filter),
        fetchSubscriptionStats(),
      ]);
      return { timeSeries, breakdown, subs };
    },
    [filter, reloadKey],
    t('dashboard.common.errorLoading'),
  );

  if (loading || error) {
    return <AdminStatsTabShell loading={loading} error={error} />;
  }

  const { timeSeries: timeSeriesData, breakdown: breakdownData, subs: subStats } = data;

  // Format currency
  const formatCurrency = (amount = 0) => {
    if (isEn) {
      return `VND ${Number(amount).toLocaleString('en-US')}`;
    }
    return `${Number(amount).toLocaleString('vi-VN')} ₫`;
  };

  // Chuẩn hóa điểm biểu đồ doanh thu
  const points = (timeSeriesData?.points || []).map((pt) => ({
    date: pt.date,
    revenue: pt.revenue,
    cumulativeRevenue: pt.cumulativeRevenue,
    transactionCount: pt.transactionCount,
  }));

  // Chuẩn hóa phân bổ theo gói
  const planItems = (breakdownData?.byPlan || []).map((plan, idx) => ({
    label: plan.planName,
    count: plan.revenue,
    percentage: plan.percentage,
    color:
      idx === 0
        ? 'var(--adm-chart-primary)'
        : idx === 1
          ? 'var(--adm-chart-secondary)'
          : 'var(--adm-chart-tertiary)',
  }));

  // Chuẩn hóa tình trạng giao dịch
  const status = breakdownData?.statusBreakdown || {};
  const statusItems = [
    {
      label: t('dashboard.revenueTab.statusSuccess'),
      count: status.successCount || 0,
      color: 'var(--adm-success)',
    },
    {
      label: t('dashboard.revenueTab.statusPending'),
      count: status.pendingCount || 0,
      color: 'var(--adm-warning)',
    },
    {
      label: t('dashboard.revenueTab.statusFailed'),
      count: status.failedCount || 0,
      color: 'var(--adm-danger)',
    },
    {
      label: t('dashboard.revenueTab.statusRefunded'),
      count: status.refundedCount || 0,
      color: 'var(--adm-text-subtle)',
    },
  ];

  return (
    <div className={styles.tabContent}>
      {/* KPI Ribbon */}
      <div className={styles.kpiRow}>
        <MetricCard
          title={t('dashboard.revenueTab.totalRevenue')}
          value={formatCurrency(timeSeriesData?.totalRevenue || 0)}
          subvalue={t('dashboard.revenueTab.txCountSub', {
            count: timeSeriesData?.totalSuccessTransactions || 0,
          })}
          icon={CreditCardIcon}
          variant="primary"
        />
        <MetricCard
          title={t('dashboard.revenueTab.successRate')}
          value={`${status.successRate || 0}%`}
          subvalue={t('dashboard.revenueTab.ordersRatioSub', {
            count: status.successCount || 0,
            total: status.totalTransactions || 0,
          })}
          icon={CheckCircleIcon}
          variant="success"
        />
        <MetricCard
          title={t('dashboard.revenueTab.activeSubscriptions')}
          value={subStats?.totalActiveSubscriptions?.toLocaleString(isEn ? 'en-US' : 'vi-VN') || 0}
          subvalue={t('dashboard.revenueTab.premiumAccountsSub', {
            count: subStats?.totalPremiumUsers || 0,
          })}
          icon={TrendingUpIcon}
          variant="warning"
        />
        <MetricCard
          title={t('dashboard.revenueTab.penetrationRate')}
          value={`${subStats?.premiumRate || 0}%`}
          subvalue={t('dashboard.revenueTab.totalUsersCountSub', {
            count: subStats?.totalSystemUsers || 0,
          })}
          icon={ActivityIcon}
        />
      </div>

      {/* Main Grid (7:5) */}
      <div className={styles.visualGrid}>
        {/* Left Column: Revenue Over Time */}
        <div className={styles.chartCol}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>{t('dashboard.revenueTab.timeseriesTitle')}</h3>
            <p className={styles.sectionDesc}>{t('dashboard.revenueTab.timeseriesDesc')}</p>
          </div>
          <AreaTimeSeriesChart
            data={points}
            series={[
              {
                key: 'revenue',
                label: t('dashboard.revenueTab.dailyRevenue'),
                color: 'var(--adm-chart-primary)',
              },
              {
                key: 'cumulativeRevenue',
                label: t('dashboard.revenueTab.cumulative'),
                color: 'var(--adm-chart-secondary)',
              },
            ]}
            height={260}
            valueFormatter={(val) => formatCurrency(val)}
          />
        </div>

        {/* Right Column: Plans and Transaction Status */}
        <div className={styles.sideCol}>
          <CategoryBreakdown
            title={t('dashboard.revenueTab.breakdownByPlan')}
            items={planItems}
            total={timeSeriesData?.totalRevenue}
            totalLabel={t('dashboard.revenueTab.totalRevenue')}
          />

          <CategoryBreakdown
            title={t('dashboard.revenueTab.transactionStatus')}
            items={statusItems}
            total={status.totalTransactions}
            totalLabel={t('dashboard.revenueTab.totalTransactionsLabel')}
          />
        </div>
      </div>
    </div>
  );
}

RevenueStatsTab.propTypes = {
  filter: PropTypes.shape({
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
    granularity: PropTypes.string.isRequired,
  }).isRequired,
  reloadKey: PropTypes.number.isRequired,
};
