import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DateRangeFilter from '../../components/DateRangeFilter/DateRangeFilter';
import UserStatsTab from './tabs/UserStatsTab';
import RevenueStatsTab from './tabs/RevenueStatsTab';
import LearningStatsTab from './tabs/LearningStatsTab';
import CommunityStatsTab from './tabs/CommunityStatsTab';
import {
  UsersIcon,
  CreditCardIcon,
  BookOpenIcon,
  MessageSquareIcon,
} from '../../components/icons/AdminIcons';
import styles from './DashboardPage.module.css';

/**
 * Format Date to YYYY-MM-DD
 */
const formatDate = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

export default function DashboardPage({ onNavigate }) {
  const { t } = useTranslation('admin');

  // Mặc định 30 ngày gần nhất
  const [filter, setFilter] = useState(() => {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 29);
    return {
      from: formatDate(start),
      to: formatDate(end),
      granularity: 'DAY',
    };
  });

  const [activeTab, setActiveTab] = useState('users');
  const [reloadKey, setReloadKey] = useState(0);

  const handleRefresh = () => {
    setReloadKey((prev) => prev + 1);
  };

  const tabs = [
    {
      id: 'users',
      label: t('dashboard.tabs.users'),
      icon: UsersIcon,
    },
    {
      id: 'revenue',
      label: t('dashboard.tabs.revenue'),
      icon: CreditCardIcon,
    },
    {
      id: 'learning',
      label: t('dashboard.tabs.learning'),
      icon: BookOpenIcon,
    },
    {
      id: 'community',
      label: t('dashboard.tabs.community'),
      icon: MessageSquareIcon,
    },
  ];

  return (
    <div className={styles.dashboardContainer}>
      {/* Top Header & Title */}
      <div className={styles.pageHeader}>
        <div>
          <h1 className={styles.pageTitle}>{t('dashboard.title')}</h1>
          <p className={styles.pageSubtitle}>{t('dashboard.subtitle')}</p>
        </div>

        {/* Global Date Filter */}
        <DateRangeFilter value={filter} onChange={setFilter} onRefresh={handleRefresh} />
      </div>

      {/* Tabs Navigation */}
      <div className={styles.tabNavRow} role="tablist">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              role="tab"
              aria-selected={isActive}
              type="button"
              className={`${styles.tabBtn} ${isActive ? styles.tabBtnActive : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <Icon size={16} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Tab Panels */}
      <div className={styles.tabPanelWrapper}>
        {activeTab === 'users' && (
          <UserStatsTab filter={filter} reloadKey={reloadKey} onNavigate={onNavigate} />
        )}
        {activeTab === 'revenue' && (
          <RevenueStatsTab filter={filter} reloadKey={reloadKey} onNavigate={onNavigate} />
        )}
        {activeTab === 'learning' && (
          <LearningStatsTab filter={filter} reloadKey={reloadKey} onNavigate={onNavigate} />
        )}
        {activeTab === 'community' && (
          <CommunityStatsTab filter={filter} reloadKey={reloadKey} onNavigate={onNavigate} />
        )}
      </div>
    </div>
  );
}

DashboardPage.propTypes = {
  onNavigate: PropTypes.func,
};
