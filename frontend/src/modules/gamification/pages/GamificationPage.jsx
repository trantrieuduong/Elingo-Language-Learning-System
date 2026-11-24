import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Tabs from '../../../components/Tabs/Tabs';
import GamificationOverviewTab from '../components/GamificationOverviewTab/GamificationOverviewTab';
import GamificationLevelsTab from '../components/GamificationLevelsTab/GamificationLevelsTab';
import GamificationLeaderboardTab from '../components/GamificationLeaderboardTab/GamificationLeaderboardTab';
import './GamificationPage.css';

/**
 * GamificationPage — Trang tổng hợp Gamification gồm 3 tab:
 * 1. Overview (Tổng quan thứ hạng, level, streak, huy hiệu)
 * 2. Levels (Cấp độ & Lộ trình)
 * 3. Leaderboard (Bảng xếp hạng realtime & lịch sử)
 *
 * @param {Function} [onNavigate] - Navigate function từ App.jsx
 */
function GamificationPage({ onNavigate: _onNavigate, userId }) {
  const { t } = useTranslation('gamification');
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'levels' | 'leaderboard'

  return (
    <div className="gamification-page container">
      {/* Tab Navigation Bar */}
      <Tabs
        tabs={[
          {
            key: 'overview',
            label: t('tabs.overview'),
            icon: 'dashboard',
          },
          {
            key: 'levels',
            label: t('tabs.levels'),
            icon: 'stairs',
          },
          {
            key: 'leaderboard',
            label: t('tabs.leaderboard'),
            icon: 'leaderboard',
          },
        ]}
        activeTab={activeTab}
        onChange={(tabKey) => setActiveTab(tabKey)}
        className="gamification-page__tabs"
      />

      {/* Tab Content */}
      <main className="gamification-page__content">
        {activeTab === 'overview' && <GamificationOverviewTab userId={userId} />}
        {activeTab === 'levels' && <GamificationLevelsTab userId={userId} />}
        {activeTab === 'leaderboard' && <GamificationLeaderboardTab />}
      </main>
    </div>
  );
}

export default GamificationPage;
