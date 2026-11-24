import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import BackButton from '../../../components/BackButton/BackButton';
import EmptyState from '../../../components/EmptyState/EmptyState';
import FormAlert from '../../../components/FormAlert/FormAlert';
import PageLoading from '../../../components/PageLoading/PageLoading';
import Pagination from '../../../components/Pagination/Pagination';
import SearchInput from '../../../components/SearchInput/SearchInput';
import { useSpeakingSocket } from '../context/SpeakingSocketContext';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { getSpeakingHistoryApi, getSpeakingHistorySummaryApi } from '../speakingApi';
import { formatDuration } from '../utils/formatDuration';
import './SpeakingHistoryPage.css';

const PAGE_SIZE = 10;

const getCannotRejoinReason = ({ activeRoomCode, item, t }) => {
  if (activeRoomCode && activeRoomCode !== item.roomCode) {
    return t('history.cannotRejoin.inOtherRoom');
  }

  if (!item.canRejoin) {
    return item.roomStatus === 'CLOSED'
      ? t('history.cannotRejoin.closed')
      : t('history.cannotRejoin.kicked');
  }

  return '';
};

const HistoryItem = ({ item, activeRoomCode, connection, i18n, onNavigate, t }) => {
  const cannotRejoinReason = getCannotRejoinReason({ activeRoomCode, item, t });
  const isCurrentRoom = activeRoomCode === item.roomCode;
  const isRejoinDisabled = Boolean(cannotRejoinReason) || connection !== 'connected';

  return (
    <article key={item.roomCode} className="speaking-history-item glass-card">
      <div className="speaking-history-item__left">
        <div className="speaking-history-item__icon-box glass-thumb">
          <span className="material-symbols-outlined">record_voice_over</span>
        </div>
        <div className="speaking-history-item__info">
          <div className="speaking-history-item__topic-line">
            <h2 className="text-title-sm speaking-history-item__topic">
              {item.topic || t('lobby.noTopic')}
            </h2>
            <div className="speaking-history-item__chips">
              <span>{t('lobby.capacityOption', { count: item.maxParticipants })}</span>
              <span>{t(`history.role.${item.role}`)}</span>
              <span className={`speaking-history-status speaking-history-status--${item.roomStatus}`}>
                {t(`history.status.${item.roomStatus}`)}
              </span>
            </div>
          </div>
          <div className="speaking-history-item__date-row">
            <span className="material-symbols-outlined">schedule</span>
            <span>{item.roomCode}</span>
            <span>{item.leftAt ? new Date(item.leftAt).toLocaleString(i18n.language) : ''}</span>
          </div>
        </div>
      </div>

      <div className="speaking-history-item__right">
        <div className="speaking-history-item__duration">
          <span>{formatDuration(item.durationSeconds)}</span>
          {item.visitCount > 1 && <small>{t('history.visits', { count: item.visitCount })}</small>}
        </div>
        <div className="speaking-history-item__action">
          <button
            type="button"
            className="btn-primary"
            disabled={isRejoinDisabled}
            aria-disabled={isRejoinDisabled}
            title={cannotRejoinReason || undefined}
            onClick={() => onNavigate(`/speaking/room/${item.roomCode}`)}
          >
            {isCurrentRoom ? t('history.backToRoom') : t('history.rejoin')}
          </button>
          {cannotRejoinReason && <small>{cannotRejoinReason}</small>}
        </div>
      </div>
    </article>
  );
};

const HistoryContent = ({
  activeRoomCode,
  connection,
  error,
  i18n,
  isFiltered,
  items,
  loading,
  onClearFilters,
  onNavigate,
  onRetry,
  page,
  setPage,
  t,
  totalPages,
}) => {
  if (loading) return <PageLoading />;

  if (error) {
    return (
      <div className="speaking-history-error glass-card">
        <FormAlert message={t('history.loadError')} />
        <button type="button" className="btn-secondary" onClick={onRetry}>
          {t('history.retry')}
        </button>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <EmptyState
        icon={isFiltered ? 'filter_alt_off' : 'history'}
        title={isFiltered ? t('history.emptyFiltered') : t('history.empty')}
        actionLabel={isFiltered ? t('lobby.clearFilters') : undefined}
        onAction={isFiltered ? onClearFilters : undefined}
      />
    );
  }

  return (
    <>
      <section className="speaking-history-list">
        {items.map((item) => (
          <HistoryItem
            key={item.roomCode}
            item={item}
            activeRoomCode={activeRoomCode}
            connection={connection}
            i18n={i18n}
            onNavigate={onNavigate}
            t={t}
          />
        ))}
      </section>
      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </>
  );
};

const SpeakingHistoryPage = ({ onNavigate }) => {
  const { t, i18n } = useTranslation('speaking');
  const { connection, activeRoomCode, checkActiveRoom } = useSpeakingSocket();

  // States bộ lọc và phân trang
  const [keywordInput, setKeywordInput] = useState('');
  const keyword = useDebouncedValue(keywordInput, 400);
  const [maxParticipants, setMaxParticipants] = useState(null);
  const [page, setPage] = useState(1);

  // States dữ liệu trang
  const [items, setItems] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);
  const requestRef = useRef(0);

  useEffect(() => {
    if (connection === 'connected') {
      checkActiveRoom();
    }
  }, [connection, checkActiveRoom]);

  useEffect(() => {
    let isMounted = true;

    getSpeakingHistorySummaryApi()
      .then((res) => {
        if (isMounted && res?.data) {
          setSummary(res.data);
        }
      })
      .catch(() => {
        // Tổng kết không chặn danh sách lịch sử
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const fetchHistory = useCallback(async () => {
    const requestId = ++requestRef.current;
    setLoading(true);
    setError(false);

    try {
      const res = await getSpeakingHistoryApi({
        page,
        size: PAGE_SIZE,
        keyword,
        maxParticipants,
      });

      if (requestId !== requestRef.current) return;
      if (!res?.data) throw new Error('Invalid history response');

      setItems(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch {
      if (requestId === requestRef.current) {
        setError(true);
      }
    } finally {
      if (requestId === requestRef.current) {
        setLoading(false);
      }
    }
  }, [page, keyword, maxParticipants, retry]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  useEffect(() => {
    setPage(1);
  }, [keyword, maxParticipants]);

  const handleClearFilters = () => {
    setKeywordInput('');
    setMaxParticipants(null);
    setPage(1);
  };

  const isFiltered = Boolean(keyword.trim() || maxParticipants !== null);

  return (
    <main className="speaking-history-page">
      <div className="speaking-history-container">
        <div className="speaking-history-top-nav">
          <BackButton
            label={t('history.backToLobby')}
            onClick={() => onNavigate('/speaking')}
          />
        </div>

        <div className="speaking-history-top-row">
          <div className="speaking-history-header-area">
            <div className="speaking-history-title-row">
              <div className="speaking-history-title-icon-box glass-thumb">
                <span className="material-symbols-outlined speaking-history-title-icon">
                  history
                </span>
              </div>
              <div className="speaking-history-title-group">
                <h1 className="text-headline-md speaking-history-title">{t('history.title')}</h1>
                <p className="text-body-md speaking-history-subtitle">{t('history.subtitle')}</p>
              </div>
            </div>
          </div>

          {/* Thống kê tổng kết lịch sử */}
          {summary && (
            <section className="speaking-history-stats-card glass-card" aria-label={t('history.title')}>
              <div className="speaking-history-stats-grid">
                {[
                  [formatDuration(summary.totalSeconds), t('history.summary.totalTime')],
                  [summary.roomCount, t('history.summary.rooms')],
                  [summary.sessionCount, t('history.summary.sessions')],
                ].map(([value, label]) => (
                  <div key={label} className="speaking-history-stat-item">
                    <span className="speaking-history-stat-item__value">{value}</span>
                    <span className="speaking-history-stat-item__label">{label}</span>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>

        {/* Bộ lọc lịch sử */}
        <section className="speaking-history-filter-section glass-card">
          <div className="speaking-history-filter-group speaking-history-filter-group--search">
            <SearchInput
              id="speaking-history-search"
              placeholder={t('lobby.searchPlaceholder')}
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              onClear={() => setKeywordInput('')}
              clearLabel={t('lobby.clearFilters')}
            />
          </div>

          <div className="speaking-history-filter-group">
            <span className="speaking-history-filter-group__label">
              {t('lobby.capacityFilter')}:
            </span>
            <div className="speaking-history-filter-pills">
              {[null, 2, 3, 4].map((capacity) => (
                <button
                  key={capacity ?? 'all'}
                  type="button"
                  className={`filter-pill ${
                    maxParticipants === capacity ? 'filter-pill--active' : ''
                  }`}
                  onClick={() => setMaxParticipants(capacity)}
                >
                  {capacity === null
                    ? t('lobby.capacityAll')
                    : t('lobby.capacityOption', { count: capacity })}
                </button>
              ))}
            </div>
          </div>

          {isFiltered && (
            <button type="button" className="btn-clear-filter" onClick={handleClearFilters}>
              {t('lobby.clearFilters')}
            </button>
          )}
        </section>

        <HistoryContent
          activeRoomCode={activeRoomCode}
          connection={connection}
          error={error}
          i18n={i18n}
          isFiltered={isFiltered}
          items={items}
          loading={loading}
          onClearFilters={handleClearFilters}
          onNavigate={onNavigate}
          onRetry={() => setRetry((value) => value + 1)}
          page={page}
          setPage={setPage}
          t={t}
          totalPages={totalPages}
        />
      </div>
    </main>
  );
};

export default SpeakingHistoryPage;
