import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';
import isoWeek from 'dayjs/plugin/isoWeek';
import { getRealtimeLeaderboardApi, getHistoricalLeaderboardApi } from '../../gamificationApi';
import Table from '../../../../components/Table/Table';
import Pagination from '../../../../components/Pagination/Pagination';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import Tabs from '../../../../components/Tabs/Tabs';
import './GamificationLeaderboardTab.css';

dayjs.extend(isoWeek);

const DEFAULT_PAGE_SIZE = 20;

const formatIsoWeekKey = (date) => {
  const d = dayjs(date);
  return `${d.isoWeekYear()}-W${String(d.isoWeek()).padStart(2, '0')}`;
};

function GamificationLeaderboardTab() {
  const { t } = useTranslation('gamification');

  const [mode, setMode] = useState('realtime'); // 'realtime' | 'history'
  const [periodType, setPeriodType] = useState('WEEKLY'); // 'WEEKLY' | 'MONTHLY'
  const [periodKey, setPeriodKey] = useState('');
  const [page, setPage] = useState(1);

  // Data states
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [leaderboardData, setLeaderboardData] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPeriodKey, setCurrentPeriodKey] = useState('');

  // Helper to generate sample period keys for quick history picker
  const getSuggestedPeriodKeys = () => {
    const now = dayjs();

    if (periodType === 'MONTHLY') {
      const keys = [];
      for (let i = 1; i <= 6; i++) {
        keys.push(now.subtract(i, 'month').format('YYYY-MM'));
      }
      return keys;
    }

    // Weekly hints using ISO-8601 week year and week number
    const keys = [];
    for (let i = 1; i <= 6; i++) {
      keys.push(formatIsoWeekKey(now.subtract(i, 'week')));
    }
    return keys;
  };

  const fetchLeaderboard = async () => {
    setLoading(true);
    setError('');
    try {
      if (mode === 'realtime') {
        const res = await getRealtimeLeaderboardApi({
          periodType,
          page,
          size: DEFAULT_PAGE_SIZE,
        });

        if (res?.success && res.data) {
          const lb = res.data.leaderboard;
          setLeaderboardData(lb?.content || []);
          setTotalPages(lb?.totalPages || 1);
          setCurrentPeriodKey(res.data.periodKey || '');
        }
      } else {
        // Historical
        if (!periodKey) {
          setLoading(false);
          return;
        }

        const res = await getHistoricalLeaderboardApi({
          periodType,
          periodKey,
          page,
          size: DEFAULT_PAGE_SIZE,
        });

        if (res?.success && res.data) {
          setLeaderboardData(res.data.content || []);
          setTotalPages(res.data.totalPages || 1);
        }
      }
    } catch {
      setError(t('common.loadError'));
    } finally {
      setLoading(false);
    }
  };

  // Reset page khi chuyển tab, periodType, hoặc periodKey
  useEffect(() => {
    setPage(1);
  }, [mode, periodType, periodKey]);

  useEffect(() => {
    // Nếu chuyển sang history và chưa có periodKey, set default là gợi ý đầu tiên
    if (mode === 'history' && !periodKey) {
      const suggested = getSuggestedPeriodKeys();
      if (suggested.length > 0) {
        setPeriodKey(suggested[0]);
        return;
      }
    }
    fetchLeaderboard();
  }, [mode, periodType, periodKey, page]);

  const columns = [
    {
      key: 'rank',
      title: t('leaderboard.columns.rank'),
      width: 15,
      align: 'center',
      render: (value) => {
        let medalModifier = '';
        if (value === 1) medalModifier = 'leaderboard-rank-medal--gold';
        else if (value === 2) medalModifier = 'leaderboard-rank-medal--silver';
        else if (value === 3) medalModifier = 'leaderboard-rank-medal--bronze';

        return (
          <div className="leaderboard-rank-cell">
            {medalModifier ? (
              <span
                className={`material-symbols-outlined leaderboard-rank-medal ${medalModifier}`}
                title={`#${value}`}
                aria-label={`Rank ${value}`}
              >
                workspace_premium
              </span>
            ) : (
              <span className="leaderboard-rank-number">#{value}</span>
            )}
          </div>
        );
      },
    },
    {
      key: 'username',
      title: t('leaderboard.columns.user'),
      width: 55,
      align: 'left',
      render: (value, row) => (
        <div className="leaderboard-user-cell">
          {row.avatarUrl ? (
            <img src={row.avatarUrl} alt={value} className="leaderboard-avatar" />
          ) : (
            <div className="leaderboard-avatar-fallback">
              <span className="material-symbols-outlined">person</span>
            </div>
          )}
          <span className="leaderboard-username">{value || t('leaderboard.anonymousUser')}</span>
        </div>
      ),
    },
    {
      key: 'totalXp',
      title: t('leaderboard.columns.xp'),
      width: 30,
      align: 'right',
      render: (value) => <span className="leaderboard-xp-cell">{value?.toLocaleString()} XP</span>,
    },
  ];

  const modeTabs = [
    { key: 'realtime', label: t('leaderboard.realtimeTab'), icon: 'bolt' },
    { key: 'history', label: t('leaderboard.historyTab'), icon: 'history' },
  ];

  const periodTabs = [
    { key: 'WEEKLY', label: t('leaderboard.weekly') },
    { key: 'MONTHLY', label: t('leaderboard.monthly') },
  ];

  const handlePeriodTypeChange = (nextPeriodType) => {
    setPeriodType(nextPeriodType);
    if (mode === 'history') setPeriodKey('');
  };

  return (
    <div className="gamification-leaderboard">
      {/* Header and Controls */}
      <div className="gamification-leaderboard__controls glass-card">
        <Tabs
          tabs={modeTabs}
          activeTab={mode}
          onChange={setMode}
          className="gamification-leaderboard__mode-tabs"
        />

        {/* Period Selector (Weekly vs Monthly) */}
        <div className="gamification-leaderboard__period-group">
          <Tabs
            tabs={periodTabs}
            activeTab={periodType}
            onChange={handlePeriodTypeChange}
            className="gamification-leaderboard__period-tabs"
          />

          {/* Historical Period Dropdown or Input */}
          {mode === 'history' && (
            <div className="gamification-leaderboard__history-selector">
              <label htmlFor="periodKeySelect" className="text-label-sm text-disabled">
                {t('leaderboard.selectPeriod')}:
              </label>
              <select
                id="periodKeySelect"
                className="gamification-leaderboard__select"
                value={periodKey}
                onChange={(e) => setPeriodKey(e.target.value)}
              >
                {getSuggestedPeriodKeys().map((k) => (
                  <option key={k} value={k}>
                    {k}
                  </option>
                ))}
              </select>
            </div>
          )}

          {mode === 'realtime' && currentPeriodKey && (
            <span className="badge badge--soft-blue">
              {t('leaderboard.periodKey', { key: currentPeriodKey })}
            </span>
          )}
        </div>
      </div>

      {/* Leaderboard Table / Loading / Empty */}
      {loading ? (
        <PageLoading />
      ) : error ? (
        <EmptyState
          icon="error"
          isError={true}
          description={error}
          actionLabel={t('common.retry')}
          onAction={fetchLeaderboard}
        />
      ) : leaderboardData.length === 0 ? (
        <EmptyState
          icon="leaderboard"
          title={t('leaderboard.emptyTitle')}
          description={t('leaderboard.emptyDesc')}
        />
      ) : (
        <div className="gamification-leaderboard__table-card glass-card">
          <Table
            columns={columns}
            data={leaderboardData}
            rowKey={(row) => `${row.userId}-${row.rank}`}
            className="gamification-leaderboard-table"
          />

          <div className="gamification-leaderboard__pagination">
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={(p) => setPage(p)}
            />
          </div>
        </div>
      )}
    </div>
  );
}

export default GamificationLeaderboardTab;
