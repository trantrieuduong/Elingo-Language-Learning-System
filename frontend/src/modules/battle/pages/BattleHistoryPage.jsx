import { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { getBattleHistoryApi, getBattleStatsApi } from '../battleApi';
import { useAuth } from '../../../context/AuthContext';
import BattleAvatar from '../components/BattleAvatar/BattleAvatar';
import BackButton from '../../../components/BackButton/BackButton';
import PillRadioGroup from '../components/PillRadioGroup/PillRadioGroup';
import Pagination from '../../../components/Pagination/Pagination';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import './BattleHistoryPage.css';

const RESULT_FILTERS = [
  { key: 'ALL', labelKey: 'history.filter.all' },
  { key: 'WIN', labelKey: 'history.result.WIN' },
  { key: 'LOSE', labelKey: 'history.result.LOSE' },
  { key: 'DRAW', labelKey: 'history.result.DRAW' },
];

const MODE_FILTERS = [
  { key: 'ALL', labelKey: 'history.filter.all' },
  { key: 'MULTIPLE_CHOICE', labelKey: 'mode.multipleChoice' },
  { key: 'TYPING', labelKey: 'mode.typing' },
];

const MATCH_TYPE_FILTERS = [
  { key: 'ALL', labelKey: 'history.filter.all' },
  { key: 'RANDOM', labelKey: 'history.matchType.RANDOM' },
  { key: 'ROOM', labelKey: 'history.matchType.ROOM' },
];

const BattleHistoryPage = ({ onNavigate }) => {
  const { t, i18n } = useTranslation('battle');
  const { user } = useAuth();

  // Thống kê
  const [stats, setStats] = useState(null);

  // Bộ lọc
  const [resultFilter, setResultFilter] = useState('ALL');
  const [modeFilter, setModeFilter] = useState('ALL');
  const [matchTypeFilter, setMatchTypeFilter] = useState('ALL');

  // Danh sách & Phân trang
  const [historyList, setHistoryList] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);

  const topRef = useRef(null);

  // 1. Lấy dữ liệu thống kê một lần
  useEffect(() => {
    let isActive = true;
    const fetchStats = async () => {
      try {
        const res = await getBattleStatsApi();
        if (!isActive) return;
        if (res?.success && res?.data) {
          setStats(res.data);
        }
      } catch {
        // Lỗi stats không chặn danh sách (ẩn khối stats)
      }
    };
    fetchStats();
    return () => {
      isActive = false;
    };
  }, []);

  // 2. Lấy danh sách lịch sử khi đổi filter / page / retryCount
  useEffect(() => {
    let isActive = true;

    const fetchHistory = async () => {
      setLoading(true);
      setError(null);
      try {
        const params = {
          page,
          size: 10,
          result: resultFilter !== 'ALL' ? resultFilter : undefined,
          mode: modeFilter !== 'ALL' ? modeFilter : undefined,
          matchType: matchTypeFilter !== 'ALL' ? matchTypeFilter : undefined,
        };
        const res = await getBattleHistoryApi(params);
        if (!isActive) return;

        if (res?.success && res?.data) {
          setHistoryList(res.data.content || []);
          setTotalPages(res.data.totalPages || 1);
        } else {
          setError(t('history.loadError'));
        }
      } catch {
        if (!isActive) return;
        setError(t('history.loadError'));
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    };

    fetchHistory();

    return () => {
      isActive = false;
    };
  }, [page, resultFilter, modeFilter, matchTypeFilter, retryCount, t]);

  const handlePageChange = (newPage) => {
    setPage(newPage);
    if (topRef.current) {
      topRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const changeFilter = (setter) => (key) => {
    setter(key);
    setPage(1);
  };

  const handleClearFilters = () => {
    setResultFilter('ALL');
    setModeFilter('ALL');
    setMatchTypeFilter('ALL');
    setPage(1);
  };

  const isFiltered =
    resultFilter !== 'ALL' || modeFilter !== 'ALL' || matchTypeFilter !== 'ALL';

  return (
    <main className="battle-history-page" ref={topRef}>
      <div className="battle-history-container">
        {/* Nút quay lại */}
        <div className="battle-history-top-nav">
          <BackButton
            label={t('history.backToLobby')}
            onClick={() => onNavigate('/battle')}
          />
        </div>

        {/* Header & Thống kê */}
        <div className="battle-history-top-row">
          <div className="battle-history-header-area">
            <div className="battle-history-title-row">
              <div className="battle-history-title-icon-box glass-thumb">
                <span className="material-symbols-outlined battle-history-title-icon">
                  history
                </span>
              </div>
              <div className="battle-history-title-group">
                <h1 className="text-headline-md battle-history-title">
                  {t('history.title')}
                </h1>
                <p className="text-body-md text-disabled battle-history-subtitle">
                  {t('history.subtitle')}
                </p>
              </div>
            </div>
          </div>

          {/* 1. Thẻ thống kê */}
          {stats && (
            <div className="battle-stats-card glass-card">
                <div className="battle-stats-grid">
                  <div className="battle-stat-item">
                    <span className="battle-stat-item__value text-headline-sm">
                      {stats.totalMatches}
                    </span>
                    <span className="battle-stat-item__label text-label-sm text-disabled">
                      {t('history.stats.total')}
                    </span>
                  </div>
                  <div className="battle-stat-item battle-stat-item--win">
                    <span className="battle-stat-item__value text-headline-sm">
                      {stats.wins}
                    </span>
                    <span className="battle-stat-item__label text-label-sm text-disabled">
                      {t('history.stats.wins')}
                    </span>
                  </div>
                  <div className="battle-stat-item battle-stat-item--lose">
                    <span className="battle-stat-item__value text-headline-sm">
                      {stats.losses}
                    </span>
                    <span className="battle-stat-item__label text-label-sm text-disabled">
                      {t('history.stats.losses')}
                    </span>
                  </div>
                  <div className="battle-stat-item battle-stat-item--draw">
                    <span className="battle-stat-item__value text-headline-sm">
                      {stats.draws}
                    </span>
                    <span className="battle-stat-item__label text-label-sm text-disabled">
                      {t('history.stats.draws')}
                    </span>
                  </div>
                  <div className="battle-stat-item battle-stat-item--rate">
                    <span className="battle-stat-item__value text-headline-sm">
                      {Number(stats.winRate).toFixed(1)}%
                    </span>
                    <span className="battle-stat-item__label text-label-sm text-disabled">
                      {t('history.stats.winRate')}
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* 2. Bộ lọc */}
          <div className="battle-filter-section glass-card">
            <PillRadioGroup
              label={t('history.filter.result')}
              options={RESULT_FILTERS}
              value={resultFilter}
              onChange={changeFilter(setResultFilter)}
              t={t}
            />

            <PillRadioGroup
              label={t('history.filter.mode')}
              options={MODE_FILTERS}
              value={modeFilter}
              onChange={changeFilter(setModeFilter)}
              t={t}
            />

            <PillRadioGroup
              label={t('history.filter.matchType')}
              options={MATCH_TYPE_FILTERS}
              value={matchTypeFilter}
              onChange={changeFilter(setMatchTypeFilter)}
              t={t}
            />
          </div>

          {/* 3. Danh sách trận đấu */}
          {loading ? (
            <div className="battle-history-loading">
              <PageLoading />
            </div>
          ) : error ? (
            <div className="battle-history-error glass-card">
              <span className="material-symbols-outlined battle-history-error__icon">
                error
              </span>
              <p className="text-body-md text-error">{error}</p>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setRetryCount((prev) => prev + 1)}
              >
                {t('history.retry')}
              </button>
            </div>
          ) : historyList.length === 0 ? (
            <EmptyState
              icon="history"
              title={t('history.emptyTitle')}
              description={
                isFiltered
                  ? t('history.emptyFiltered')
                  : t('history.empty')
              }
              actionLabel={isFiltered ? t('history.clearFilters') : undefined}
              onAction={isFiltered ? handleClearFilters : undefined}
              actionVariant="primary"
            />
          ) : (
            <div className="battle-history-list">
              {historyList.map((item) => {
                const opponent = item.opponent;
                const opponentName =
                  opponent?.displayName || t('play.opponentFallback');
                const isWin = item.result === 'WIN';
                const isLose = item.result === 'LOSE';
                const isDraw = item.result === 'DRAW';

                const badgeClass = isWin
                  ? 'battle-history-badge--win'
                  : isLose
                    ? 'battle-history-badge--lose'
                    : 'battle-history-badge--draw';

                const dateText = item.finishedAt
                  ? new Date(item.finishedAt).toLocaleString(i18n.language)
                  : '';

                return (
                  <button
                    key={item.matchId}
                    type="button"
                    className="battle-history-item glass-card"
                    onClick={() => onNavigate(`/battle/history/${item.matchId}`)}
                    aria-label={`${t('history.title')} #${item.matchId}`}
                  >
                    <div className="battle-history-item__left">
                      <BattleAvatar
                        name={opponentName}
                        avatarUrl={opponent?.avatarUrl}
                        size="md"
                      />
                      <div className="battle-history-item__info">
                        <div className="battle-history-item__opponent-line">
                          <span className="text-title-sm battle-history-item__opponent-name">
                            {opponentName}
                          </span>
                          <div className="battle-history-item__chips">
                            <span className="battle-chip text-label-xs">
                              {t(
                                item.mode === 'MULTIPLE_CHOICE'
                                  ? 'mode.multipleChoice'
                                  : 'mode.typing'
                              )}
                            </span>
                            <span className="battle-chip text-label-xs">
                              {t(
                                item.matchType === 'ROOM'
                                  ? 'history.matchType.ROOM'
                                  : 'history.matchType.RANDOM'
                              )}
                            </span>
                          </div>
                        </div>
                        <div className="battle-history-item__date-row">
                          <span className="material-symbols-outlined battle-history-item__date-icon">
                            schedule
                          </span>
                          <span>{dateText}</span>
                        </div>
                      </div>
                    </div>

                    <div className="battle-history-item__right">
                      <div className="battle-history-item__score text-title-md">
                        <span className="battle-history-item__my-score">
                          {item.myScore}
                        </span>
                        <span className="text-disabled"> - </span>
                        <span className="battle-history-item__opp-score">
                          {item.opponentScore}
                        </span>
                      </div>
                      <span
                        className={`battle-history-badge text-label-sm ${badgeClass}`}
                      >
                        {t(`history.result.${item.result}`)}
                      </span>
                      <span className="material-symbols-outlined battle-history-item__arrow">
                        chevron_right
                      </span>
                    </div>
                  </button>
                );
              })}

              {/* Phân trang */}
              {totalPages > 1 && (
                <div className="battle-history-pagination">
                  <Pagination
                    currentPage={page}
                    totalPages={totalPages}
                    onPageChange={handlePageChange}
                  />
                </div>
              )}
            </div>
          )}
      </div>
    </main>
  );
};

export default BattleHistoryPage;

