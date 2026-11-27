import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  fetchUserGrowth,
  fetchUserActivity,
  fetchUserBreakdown,
} from '../../../services/adminStatsApi';
import useAdminTabStats from '../../../hooks/useAdminTabStats';
import AdminStatsTabShell from '../components/AdminStatsTabShell';
import MetricCard from '../../../components/MetricCard/MetricCard';
import AreaTimeSeriesChart from '../../../components/VisualCharts/AreaTimeSeriesChart';
import CategoryBreakdown from '../../../components/VisualCharts/CategoryBreakdown';
import {
  UsersIcon,
  TrendingUpIcon,
  ActivityIcon,
  ShieldCheckIcon,
} from '../../../components/icons/AdminIcons';
import styles from '../DashboardPage.module.css';

export default function UserStatsTab({ filter, reloadKey }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language && i18n.language.startsWith('en') ? 'en-US' : 'vi-VN';

  const { loading, error, data } = useAdminTabStats(
    async () => {
      const [growth, activity, breakdown] = await Promise.all([
        fetchUserGrowth(filter),
        fetchUserActivity(filter),
        fetchUserBreakdown(),
      ]);
      return { growth, activity, breakdown };
    },
    [filter, reloadKey],
    t('dashboard.common.errorLoading'),
  );

  if (loading || error) {
    return <AdminStatsTabShell loading={loading} error={error} />;
  }

  const { growth: growthData, activity: activityData, breakdown: breakdownData } = data;

  // Chuẩn hóa dữ liệu cho biểu đồ tăng trưởng
  const growthPoints = (growthData?.newUsers || []).map((nu, idx) => {
    const cu = growthData?.cumulativeUsers?.[idx];
    return {
      date: nu.key,
      newUsers: nu.value,
      cumulative: cu ? cu.value : 0,
    };
  });

  // Chuẩn hóa dữ liệu DAU
  const dauPoints = (activityData?.dauTimeSeries || []).map((item) => ({
    date: item.key,
    dau: item.value,
  }));

  // Chuẩn hóa dữ liệu cấp độ học viên
  const levelItems = (breakdownData?.levelDistribution || []).map((lvl) => ({
    label: `${t('dashboard.userTab.levelPrefix')} ${lvl.key}`,
    count: lvl.value,
    color: 'var(--adm-chart-primary)',
  }));

  return (
    <div className={styles.tabContent}>
      {/* KPI Ribbon */}
      <div className={styles.kpiRow}>
        <MetricCard
          title={t('dashboard.userTab.totalUsers')}
          value={breakdownData?.totalUsers?.toLocaleString(locale) || 0}
          subvalue={t('dashboard.userTab.activeCountSub', {
            count: breakdownData?.activeCount || 0,
          })}
          icon={UsersIcon}
          variant="primary"
        />
        <MetricCard
          title={t('dashboard.userTab.verifiedRate')}
          value={`${breakdownData?.verifiedPercentage || 0}%`}
          subvalue={t('dashboard.userTab.verifiedCountSub', {
            count: breakdownData?.verifiedCount || 0,
          })}
          icon={ShieldCheckIcon}
          variant="success"
        />
        <MetricCard
          title={t('dashboard.userTab.premiumConversion')}
          value={`${breakdownData?.premiumPercentage || 0}%`}
          subvalue={t('dashboard.userTab.membersCountSub', {
            count: breakdownData?.premiumCount || 0,
          })}
          icon={TrendingUpIcon}
          variant="warning"
        />
        <MetricCard
          title={t('dashboard.userTab.stickiness')}
          value={`${activityData?.stickinessPercentage || 0}%`}
          subvalue={`DAU: ${activityData?.currentDau || 0} · MAU: ${activityData?.currentMau || 0}`}
          icon={ActivityIcon}
        />
      </div>

      {/* Main Grid: Asymmetric Layout (7:5) */}
      <div className={styles.visualGrid}>
        {/* Left Column: Growth Chart */}
        <div className={styles.chartCol}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>{t('dashboard.userTab.growthChartTitle')}</h3>
            <p className={styles.sectionDesc}>{t('dashboard.userTab.growthChartDesc')}</p>
          </div>
          <AreaTimeSeriesChart
            data={growthPoints}
            series={[
              {
                key: 'newUsers',
                label: t('dashboard.userTab.newUsers'),
                color: 'var(--adm-chart-primary)',
              },
              {
                key: 'cumulative',
                label: t('dashboard.userTab.cumulative'),
                color: 'var(--adm-chart-secondary)',
              },
            ]}
            height={240}
          />

          <div style={{ marginTop: 24 }}>
            <div className={styles.sectionHeader}>
              <h3 className={styles.sectionTitle}>{t('dashboard.userTab.activityChartTitle')}</h3>
              <p className={styles.sectionDesc}>{t('dashboard.userTab.activityChartDesc')}</p>
            </div>
            <AreaTimeSeriesChart
              data={dauPoints}
              series={[
                {
                  key: 'dau',
                  label: 'DAU',
                  color: 'var(--adm-chart-quaternary)',
                },
              ]}
              height={200}
            />
          </div>
        </div>

        {/* Right Column: Breakdown Cards */}
        <div className={styles.sideCol}>
          <CategoryBreakdown
            title={t('dashboard.userTab.levelDistribution')}
            items={levelItems}
            total={breakdownData?.totalUsers}
            totalLabel={t('dashboard.userTab.totalUsers')}
          />

          <CategoryBreakdown
            title={t('dashboard.userTab.accountStatusTitle')}
            items={[
              {
                label: t('dashboard.userTab.statusActive'),
                count: breakdownData?.activeCount || 0,
                percentage: breakdownData?.activePercentage || 0,
                color: 'var(--adm-success)',
              },
              {
                label: t('dashboard.userTab.statusUnverified'),
                count: (breakdownData?.totalUsers || 0) - (breakdownData?.verifiedCount || 0),
                percentage: 100 - (breakdownData?.verifiedPercentage || 100),
                color: 'var(--adm-warning)',
              },
            ]}
            total={breakdownData?.totalUsers}
          />
        </div>
      </div>
    </div>
  );
}

UserStatsTab.propTypes = {
  filter: PropTypes.shape({
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
    granularity: PropTypes.string.isRequired,
  }).isRequired,
  reloadKey: PropTypes.number.isRequired,
};
