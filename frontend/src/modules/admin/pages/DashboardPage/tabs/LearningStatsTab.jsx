import React, { useState, useEffect, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  fetchLearningActivity,
  fetchDeckStats,
  fetchLessonStats,
} from '../../../services/adminStatsApi';
import MetricCard from '../../../components/MetricCard/MetricCard';
import StackedBarChart from '../../../components/VisualCharts/StackedBarChart';
import CategoryBreakdown from '../../../components/VisualCharts/CategoryBreakdown';
import DataTable from '../../../components/DataTable/DataTable';
import { AdminSkeletonCard, AdminErrorState } from '../../../components/Feedback/AdminFeedback';
import { BookOpenIcon, ActivityIcon, LayersIcon } from '../../../components/icons/AdminIcons';
import styles from '../DashboardPage.module.css';

export default function LearningStatsTab({ filter, reloadKey, onNavigate }) {
  const { t, i18n } = useTranslation('admin');
  const locale = i18n.language && i18n.language.startsWith('en') ? 'en-US' : 'vi-VN';

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [activityData, setActivityData] = useState(null);
  const [deckStats, setDeckStats] = useState(null);
  const [lessonStats, setLessonStats] = useState(null);

  // States chuyển tab nội bộ
  const [deckTab, setDeckTab] = useState('SYSTEM'); // 'SYSTEM' | 'USER'
  const [breakdownTab, setBreakdownTab] = useState('DECKS'); // 'DECKS' | 'LESSONS'

  useEffect(() => {
    let isMounted = true;
    const loadData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [activity, decks, lessons] = await Promise.all([
          fetchLearningActivity(filter),
          fetchDeckStats(10),
          fetchLessonStats(10),
        ]);
        if (!isMounted) return;
        setActivityData(activity);
        setDeckStats(decks);
        setLessonStats(lessons);
      } catch (err) {
        if (!isMounted) return;
        setError(err.message || t('dashboard.common.errorLoading'));
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadData();
    return () => {
      isMounted = false;
    };
  }, [filter, reloadKey, t]);

  // Helper render status badge
  const renderStatusBadge = (val) => {
    let bg = 'var(--adm-bg-surface-subtle)';
    let color = 'var(--adm-text-muted)';
    let label = val;

    if (val === 'PUBLISHED') {
      bg = 'var(--adm-success-subtle)';
      color = 'var(--adm-success)';
      label = t('dashboard.learningTab.statusPublished');
    } else if (val === 'DRAFT') {
      bg = 'var(--adm-warning-subtle)';
      color = 'var(--adm-warning)';
      label = t('dashboard.learningTab.statusDraft');
    } else if (val === 'ARCHIVED') {
      bg = 'var(--adm-bg-surface-subtle)';
      color = 'var(--adm-text-subtle)';
      label = t('dashboard.learningTab.statusArchived');
    }

    return (
      <span
        style={{
          fontSize: 11,
          fontWeight: 600,
          padding: '2px 6px',
          borderRadius: 'var(--adm-radius-full)',
          background: bg,
          color,
        }}
      >
        {label}
      </span>
    );
  };

  // Cột cho Top Decks
  const deckColumns = useMemo(
    () => [
      {
        title: t('dashboard.learningTab.deckTitle'),
        dataIndex: 'title',
        render: (val, record) => (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {record.coverImageUrl ? (
              <img
                src={record.coverImageUrl}
                alt={val}
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 'var(--adm-radius-sm)',
                  objectFit: 'cover',
                  cursor: onNavigate ? 'pointer' : 'default',
                }}
                onClick={() => {
                  if (onNavigate && record.deckId) {
                    onNavigate(`/admin/decks/${record.deckId}`);
                  }
                }}
              />
            ) : (
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 'var(--adm-radius-sm)',
                  background: 'var(--adm-bg-surface-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--adm-text-subtle)',
                  cursor: onNavigate ? 'pointer' : 'default',
                }}
                onClick={() => {
                  if (onNavigate && record.deckId) {
                    onNavigate(`/admin/decks/${record.deckId}`);
                  }
                }}
              >
                <BookOpenIcon size={16} />
              </div>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <a
                href={`/admin/decks/${record.deckId}`}
                style={{
                  fontWeight: 600,
                  color: 'var(--adm-text-main)',
                  textDecoration: 'none',
                  cursor: 'pointer',
                  transition: 'color var(--adm-transition-fast)',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--adm-accent)')}
                onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--adm-text-main)')}
                onClick={(e) => {
                  e.preventDefault();
                  if (onNavigate && record.deckId) {
                    onNavigate(`/admin/decks/${record.deckId}`);
                  }
                }}
              >
                {val}
              </a>
              {record.isPremium && (
                <span
                  style={{
                    fontSize: 10,
                    fontWeight: 700,
                    color: 'var(--adm-warning)',
                    textTransform: 'uppercase',
                  }}
                >
                  Premium
                </span>
              )}
            </div>
          </div>
        ),
      },
      {
        title: t('dashboard.learningTab.ownerType'),
        dataIndex: 'ownerType',
        render: (val) => (
          <span
            style={{
              fontSize: 11,
              fontWeight: 600,
              padding: '2px 6px',
              borderRadius: 'var(--adm-radius-full)',
              background:
                val === 'SYSTEM' ? 'var(--adm-primary-subtle)' : 'var(--adm-bg-surface-subtle)',
              color: val === 'SYSTEM' ? 'var(--adm-primary)' : 'var(--adm-text-muted)',
            }}
          >
            {val === 'SYSTEM' ? t('dashboard.learningTab.system') : t('dashboard.learningTab.user')}
          </span>
        ),
      },
      {
        title: t('dashboard.learningTab.statusHeader'),
        dataIndex: 'status',
        render: (val) => renderStatusBadge(val),
      },
      {
        title: t('dashboard.learningTab.learnersHeader'),
        dataIndex: 'learnerCount',
        align: 'right',
        render: (val) => (
          <strong style={{ fontFamily: 'var(--adm-font-display)' }}>
            {Number(val || 0).toLocaleString(locale)}
          </strong>
        ),
      },
    ],
    [t, locale, onNavigate]
  );

  // Cột cho Top Lessons
  const lessonColumns = useMemo(
    () => [
      {
        title: t('dashboard.learningTab.lessonTitle'),
        dataIndex: 'title',
        render: (val, record) => (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {record.thumbnailUrl ? (
              <img
                src={record.thumbnailUrl}
                alt={val}
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 'var(--adm-radius-sm)',
                  objectFit: 'cover',
                }}
              />
            ) : (
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 'var(--adm-radius-sm)',
                  background: 'var(--adm-bg-surface-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--adm-text-subtle)',
                }}
              >
                <LayersIcon size={16} />
              </div>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <span style={{ fontWeight: 600 }}>{val}</span>
              {record.isPremium && (
                <span
                  style={{
                    fontSize: 10,
                    fontWeight: 700,
                    color: 'var(--adm-warning)',
                    textTransform: 'uppercase',
                  }}
                >
                  Premium
                </span>
              )}
            </div>
          </div>
        ),
      },
      {
        title: t('dashboard.learningTab.statusHeader'),
        dataIndex: 'status',
        render: (val) => renderStatusBadge(val),
      },
      {
        title: t('dashboard.learningTab.learnCountHeader'),
        dataIndex: 'learnerCount',
        align: 'right',
        render: (val) => (
          <strong style={{ fontFamily: 'var(--adm-font-display)' }}>
            {Number(val || 0).toLocaleString(locale)}
          </strong>
        ),
      },
    ],
    [t, locale]
  );

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

  const totals = activityData?.totals || {};

  // Chuẩn bị dữ liệu cho Breakdown Decks
  const deckOwnerItems = (deckStats?.byOwnerType || []).map((ot) => ({
    label:
      ot.category === 'SYSTEM'
        ? t('dashboard.learningTab.systemAuthored')
        : t('dashboard.learningTab.userCreated'),
    count: ot.count,
    percentage: ot.percentage,
    color: ot.category === 'SYSTEM' ? 'var(--adm-chart-primary)' : 'var(--adm-chart-secondary)',
  }));

  const deckPremiumItems = (deckStats?.byPremium || []).map((bp) => ({
    label:
      bp.category === 'FREE'
        ? t('dashboard.learningTab.free')
        : t('dashboard.learningTab.premiumTier'),
    count: bp.count,
    percentage: bp.percentage,
    color: bp.category === 'FREE' ? 'var(--adm-success)' : 'var(--adm-warning)',
  }));

  const deckStatusItems = (deckStats?.byStatus || []).map((st) => ({
    label:
      st.category === 'PUBLISHED'
        ? t('dashboard.learningTab.statusPublished')
        : st.category === 'DRAFT'
          ? t('dashboard.learningTab.statusDraft')
          : t('dashboard.learningTab.statusArchived'),
    count: st.count,
    percentage: st.percentage,
    color:
      st.category === 'PUBLISHED'
        ? 'var(--adm-success)'
        : st.category === 'DRAFT'
          ? 'var(--adm-warning)'
          : 'var(--adm-text-disabled)',
  }));

  // Chuẩn bị dữ liệu cho Breakdown Lessons
  const lessonPremiumItems = (lessonStats?.byPremium || []).map((bp) => ({
    label:
      bp.category === 'FREE'
        ? t('dashboard.learningTab.free')
        : t('dashboard.learningTab.premiumTier'),
    count: bp.count,
    percentage: bp.percentage,
    color: bp.category === 'FREE' ? 'var(--adm-success)' : 'var(--adm-warning)',
  }));

  const lessonStatusItems = (lessonStats?.byStatus || []).map((st) => ({
    label:
      st.category === 'PUBLISHED'
        ? t('dashboard.learningTab.statusPublished')
        : st.category === 'DRAFT'
          ? t('dashboard.learningTab.statusDraft')
          : t('dashboard.learningTab.statusArchived'),
    count: st.count,
    percentage: st.percentage,
    color:
      st.category === 'PUBLISHED'
        ? 'var(--adm-success)'
        : st.category === 'DRAFT'
          ? 'var(--adm-warning)'
          : 'var(--adm-text-disabled)',
  }));

  const currentDecks =
    deckTab === 'SYSTEM' ? deckStats?.topSystemDecks || [] : deckStats?.topUserDecks || [];

  return (
    <div className={styles.tabContent}>
      {/* KPI Ribbon */}
      <div className={styles.kpiRow}>
        <MetricCard
          title={t('dashboard.learningTab.grandTotal')}
          value={Number(totals.grandTotal || 0).toLocaleString(locale)}
          subvalue={t('dashboard.learningTab.allSkills')}
          icon={ActivityIcon}
          variant="primary"
        />
        <MetricCard
          title={t('dashboard.learningTab.flashcardCount')}
          value={Number(totals.totalFlashcard || 0).toLocaleString(locale)}
          subvalue={t('dashboard.learningTab.flashcardSub')}
          icon={BookOpenIcon}
          variant="success"
        />
        <MetricCard
          title={t('dashboard.learningTab.dictationAndShadowing')}
          value={Number((totals.totalDictation || 0) + (totals.totalShadowing || 0)).toLocaleString(
            locale
          )}
          subvalue={`Dictation: ${totals.totalDictation || 0} · Shadowing: ${
            totals.totalShadowing || 0
          }`}
          icon={LayersIcon}
          variant="warning"
        />
        <MetricCard
          title={t('dashboard.learningTab.battleAndCheckin')}
          value={Number((totals.totalBattle || 0) + (totals.totalCheckIn || 0)).toLocaleString(
            locale
          )}
          subvalue={`Battle: ${totals.totalBattle || 0} · CheckIn: ${totals.totalCheckIn || 0}`}
          icon={ActivityIcon}
        />
      </div>

      {/* Main Grid */}
      <div className={styles.visualGrid}>
        {/* Left Column: Stacked Activity by Source */}
        <div className={styles.chartCol}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>{t('dashboard.learningTab.activityTrendTitle')}</h3>
            <p className={styles.sectionDesc}>{t('dashboard.learningTab.activityTrendDesc')}</p>
          </div>
          <StackedBarChart
            data={activityData?.points || []}
            keys={[
              {
                key: 'flashcard',
                label: 'Flashcard',
                color: 'var(--adm-chart-primary)',
              },
              {
                key: 'dictation',
                label: 'Dictation',
                color: 'var(--adm-chart-secondary)',
              },
              {
                key: 'shadowing',
                label: 'Shadowing',
                color: 'var(--adm-chart-tertiary)',
              },
              {
                key: 'battle',
                label: 'Battle',
                color: 'var(--adm-chart-quaternary)',
              },
              {
                key: 'checkIn',
                label: 'Check-in',
                color: 'var(--adm-chart-quinary)',
              },
            ]}
            height={260}
          />
        </div>

        {/* Right Column: Decks and Lessons composition with Segmented Switch */}
        <div className={styles.sideCol}>
          <div className={styles.sectionHeaderWithAction}>
            <div
              className={styles.segmentedControl}
              role="tablist"
              aria-label="Content breakdown switch"
              style={{ width: '100%' }}
            >
              <button
                type="button"
                role="tab"
                aria-selected={breakdownTab === 'DECKS'}
                className={`${styles.segmentedBtn} ${
                  breakdownTab === 'DECKS' ? styles.segmentedBtnActive : ''
                }`}
                style={{ flex: 1, justifyContent: 'center' }}
                onClick={() => setBreakdownTab('DECKS')}
              >
                {t('dashboard.learningTab.deckInventory')}
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={breakdownTab === 'LESSONS'}
                className={`${styles.segmentedBtn} ${
                  breakdownTab === 'LESSONS' ? styles.segmentedBtnActive : ''
                }`}
                style={{ flex: 1, justifyContent: 'center' }}
                onClick={() => setBreakdownTab('LESSONS')}
              >
                {t('dashboard.learningTab.lessonInventory')}
              </button>
            </div>
          </div>

          {breakdownTab === 'DECKS' ? (
            <>
              <CategoryBreakdown
                title={t('dashboard.learningTab.deckCompositionTitle')}
                items={deckOwnerItems}
                total={deckStats?.totalDecks}
                totalLabel={t('dashboard.learningTab.totalDecksLabel')}
              />

              <CategoryBreakdown
                title={t('dashboard.learningTab.freeVsPremiumTitle')}
                items={deckPremiumItems}
                total={deckStats?.totalDecks}
                totalLabel={t('dashboard.learningTab.totalDecksLabel')}
              />

              <CategoryBreakdown
                title={t('dashboard.learningTab.deckStatusTitle')}
                items={deckStatusItems}
                total={deckStats?.totalDecks}
                totalLabel={t('dashboard.learningTab.totalDecksLabel')}
              />
            </>
          ) : (
            <>
              <CategoryBreakdown
                title={t('dashboard.learningTab.lessonPremiumTitle')}
                items={lessonPremiumItems}
                total={lessonStats?.totalLessons}
                totalLabel={t('dashboard.learningTab.totalLessonsLabel')}
              />

              <CategoryBreakdown
                title={t('dashboard.learningTab.lessonStatusTitle')}
                items={lessonStatusItems}
                total={lessonStats?.totalLessons}
                totalLabel={t('dashboard.learningTab.totalLessonsLabel')}
              />
            </>
          )}
        </div>
      </div>

      {/* Tables Row: Top Decks & Top Lessons */}
      <div className={styles.tablesRow} style={{ marginTop: 24 }}>
        {/* Left Table: Decks with System vs User Segmented Switch */}
        <div className={styles.tableHalf}>
          <div className={styles.sectionHeaderWithAction}>
            <h3 className={styles.sectionTitle}>
              {deckTab === 'SYSTEM'
                ? t('dashboard.learningTab.topSystemDecks')
                : t('dashboard.learningTab.topUserDecks')}
            </h3>
            <div
              className={styles.segmentedControl}
              role="tablist"
              aria-label="Deck ownership switch"
            >
              <button
                type="button"
                role="tab"
                aria-selected={deckTab === 'SYSTEM'}
                className={`${styles.segmentedBtn} ${
                  deckTab === 'SYSTEM' ? styles.segmentedBtnActive : ''
                }`}
                onClick={() => setDeckTab('SYSTEM')}
              >
                {t('dashboard.learningTab.systemDecksTab')}
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={deckTab === 'USER'}
                className={`${styles.segmentedBtn} ${
                  deckTab === 'USER' ? styles.segmentedBtnActive : ''
                }`}
                onClick={() => setDeckTab('USER')}
              >
                {t('dashboard.learningTab.userDecksTab')}
              </button>
            </div>
          </div>
          <DataTable
            columns={deckColumns}
            data={currentDecks}
            keyField="deckId"
            emptyMessage={
              deckTab === 'SYSTEM'
                ? t('dashboard.learningTab.emptyDecks')
                : t('dashboard.learningTab.emptyUserDecks')
            }
          />
        </div>

        {/* Right Table: Lessons */}
        <div className={styles.tableHalf}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>{t('dashboard.learningTab.topLessons')}</h3>
          </div>
          <DataTable
            columns={lessonColumns}
            data={lessonStats?.topLessons || []}
            keyField="lessonId"
            emptyMessage={t('dashboard.learningTab.emptyLessons')}
          />
        </div>
      </div>
    </div>
  );
}

LearningStatsTab.propTypes = {
  filter: PropTypes.shape({
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
    granularity: PropTypes.string.isRequired,
  }).isRequired,
  reloadKey: PropTypes.number.isRequired,
  onNavigate: PropTypes.func,
};
