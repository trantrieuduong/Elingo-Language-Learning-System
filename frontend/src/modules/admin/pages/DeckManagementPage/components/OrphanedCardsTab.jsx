import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import MetricCard from '../../../components/MetricCard/MetricCard';
import DataTable from '../../../components/DataTable/DataTable';
import AdminPagination from '../../../components/AdminPagination/AdminPagination';
import {
  AdminErrorState,
  AdminSkeletonCard,
} from '../../../components/Feedback/AdminFeedback';
import {
  LayersIcon,
  TrashIcon,
  ShieldCheckIcon,
  RefreshIcon,
  CheckCircleIcon,
  AlertTriangleIcon,
  CloseIcon,
  Volume2Icon,
} from '../../../components/icons/AdminIcons';
import {
  fetchOrphanedCardStats,
  fetchOrphanedCards,
  cleanupOrphanedCards,
} from '../../../services/adminContentApi';
import styles from './OrphanedCardsTab.module.css';

const PAGE_SIZE = 20;

export default function OrphanedCardsTab() {
  const { t } = useTranslation('admin');

  const [stats, setStats] = useState({
    totalOrphanedCards: 0,
    safeToDeleteCards: 0,
    retainedInStudyCards: 0,
  });
  const [statsLoading, setStatsLoading] = useState(true);

  const [cardsData, setCardsData] = useState({
    content: [],
    page: 1,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 1,
    isLast: true,
  });
  const [page, setPage] = useState(1);
  const [cardsLoading, setCardsLoading] = useState(true);
  const [error, setError] = useState(null);

  const [cleanupModal, setCleanupModal] = useState({
    isOpen: false,
    submitting: false,
    error: null,
  });
  const [cleanupResult, setCleanupResult] = useState(null);

  const loadStats = useCallback(async () => {
    try {
      setStatsLoading(true);
      const data = await fetchOrphanedCardStats();
      if (data) {
        setStats({
          totalOrphanedCards: data.totalOrphanedCards ?? 0,
          safeToDeleteCards: data.safeToDeleteCards ?? 0,
          retainedInStudyCards: data.retainedInStudyCards ?? 0,
        });
      }
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || t('feedback.errorDesc'));
    } finally {
      setStatsLoading(false);
    }
  }, [t]);

  const loadCards = useCallback(
    async (targetPage = page) => {
      try {
        setCardsLoading(true);
        setError(null);
        const data = await fetchOrphanedCards({
          page: targetPage,
          size: PAGE_SIZE,
          sort: 'id,desc',
        });
        if (data) {
          setCardsData(data);
        }
      } catch (err) {
        setError(err?.response?.data?.message || err?.message || t('feedback.errorDesc'));
      } finally {
        setCardsLoading(false);
      }
    },
    [page, t]
  );

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    loadCards(page);
  }, [loadCards, page]);

  const handleRefresh = () => {
    setError(null);
    loadStats();
    loadCards(page);
  };

  const handleOpenCleanupModal = () => {
    setCleanupModal({
      isOpen: true,
      submitting: false,
      error: null,
    });
  };

  const handleCloseCleanupModal = () => {
    if (cleanupModal.submitting) return;
    setCleanupModal({
      isOpen: false,
      submitting: false,
      error: null,
    });
  };

  const handleConfirmCleanup = async () => {
    try {
      setCleanupModal((prev) => ({ ...prev, submitting: true, error: null }));
      const result = await cleanupOrphanedCards();
      setCleanupModal({ isOpen: false, submitting: false, error: null });
      if (result) {
        setCleanupResult(result);
      }
      setPage(1);
      await Promise.all([loadStats(), loadCards(1)]);
    } catch (err) {
      setCleanupModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  const columns = useMemo(
    () => [
      {
        title: t('content.orphanedCards.colId'),
        dataIndex: 'id',
        key: 'id',
        width: '90px',
        render: (id) => <span className={styles.idCell}>#{id}</span>,
      },
      {
        title: t('content.orphanedCards.colTerm'),
        key: 'term',
        render: (_, record) => {
          const firstPhonetic = record.phonetics?.find((p) => p?.text)?.text;
          return (
            <div className={styles.termCell}>
              <div className={styles.termTopRow}>
                <span className={styles.termText}>{record.term}</span>
                {record.pos && <span className={styles.posBadge}>{record.pos}</span>}
              </div>
              {firstPhonetic && <span className={styles.phoneticText}>{firstPhonetic}</span>}
            </div>
          );
        },
      },
      {
        title: t('content.orphanedCards.colTranslation'),
        key: 'translation',
        render: (_, record) => (
          <div className={styles.translationCell}>
            <span className={styles.translationText}>{record.translation || '—'}</span>
            {record.explanationVi && (
              <span className={styles.explanationText} title={record.explanationVi}>
                {record.explanationVi}
              </span>
            )}
          </div>
        ),
      },
      {
        title: t('content.orphanedCards.colMedia'),
        key: 'media',
        width: '200px',
        render: (_, record) => {
          const audioCount =
            record.phonetics?.filter((p) => Boolean(p?.audioUrl)).length || 0;
          const hasImage = Boolean(record.imageUrl);

          if (!hasImage && audioCount === 0) {
            return <span className={styles.noMedia}>{t('content.orphanedCards.noMedia')}</span>;
          }

          return (
            <div className={styles.mediaCell}>
              {hasImage && (
                <span className={styles.mediaBadge}>
                  {t('content.orphanedCards.hasImage')}
                </span>
              )}
              {audioCount > 0 && (
                <span className={styles.mediaBadge}>
                  <Volume2Icon size={12} />
                  {t('content.orphanedCards.hasAudio', { count: audioCount })}
                </span>
              )}
            </div>
          );
        },
      },
    ],
    [t]
  );

  const safeCount = stats.safeToDeleteCards ?? 0;

  return (
    <div className={styles.tabContainer}>
      {/* KPI Statistics Row */}
      <div className={styles.kpiGrid}>
        {statsLoading ? (
          <>
            <AdminSkeletonCard />
            <AdminSkeletonCard />
            <AdminSkeletonCard />
          </>
        ) : (
          <>
            <MetricCard
              title={t('content.orphanedCards.statsTotal')}
              value={stats.totalOrphanedCards}
              subvalue={t('content.orphanedCards.statsTotalSub')}
              icon={LayersIcon}
              variant="default"
            />
            <MetricCard
              title={t('content.orphanedCards.statsSafe')}
              value={stats.safeToDeleteCards}
              subvalue={t('content.orphanedCards.statsSafeSub')}
              icon={TrashIcon}
              variant={safeCount > 0 ? 'warning' : 'default'}
            />
            <MetricCard
              title={t('content.orphanedCards.statsRetained')}
              value={stats.retainedInStudyCards}
              subvalue={t('content.orphanedCards.statsRetainedSub')}
              icon={ShieldCheckIcon}
              variant="primary"
            />
          </>
        )}
      </div>

      {/* Action Bar */}
      <div className={styles.actionCard}>
        <div className={styles.actionInfo}>
          <h3 className={styles.actionTitle}>{t('content.orphanedCards.cleanupHintTitle')}</h3>
          <p className={styles.actionDesc}>{t('content.orphanedCards.cleanupHintDesc')}</p>
        </div>
        <div className={styles.actionButtons}>
          <button
            type="button"
            className={styles.secondaryBtn}
            onClick={handleRefresh}
            disabled={statsLoading || cardsLoading || cleanupModal.submitting}
          >
            <RefreshIcon size={15} />
            <span>{t('content.orphanedCards.refreshBtn')}</span>
          </button>
          <button
            type="button"
            className={styles.dangerBtn}
            onClick={handleOpenCleanupModal}
            disabled={statsLoading || cleanupModal.submitting || safeCount === 0}
          >
            <TrashIcon size={15} />
            <span>
              {cleanupModal.submitting
                ? t('content.orphanedCards.cleaningBtn')
                : t('content.orphanedCards.cleanupBtn')}
            </span>
          </button>
        </div>
      </div>

      {/* Cleanup Outcome Banner */}
      {cleanupResult && (
        <div className={styles.successBanner} role="status">
          <div className={styles.successBannerContent}>
            <CheckCircleIcon size={18} className={styles.successIcon} />
            <div>
              <div className={styles.successTitle}>
                {t('content.orphanedCards.cleanupSuccessTitle')}
              </div>
              <div className={styles.successDesc}>
                {t('content.orphanedCards.cleanupSuccessDetail', {
                  scannedCount: cleanupResult.scannedCount ?? 0,
                  deletedCardCount: cleanupResult.deletedCardCount ?? 0,
                  deletedFileCount: cleanupResult.deletedFileCount ?? 0,
                })}
              </div>
            </div>
          </div>
          <button
            type="button"
            className={styles.dismissBtn}
            onClick={() => setCleanupResult(null)}
            aria-label={t('content.orphanedCards.cancelBtn')}
          >
            <CloseIcon size={16} />
          </button>
        </div>
      )}

      {/* Error State */}
      {error && (
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={handleRefresh}
        />
      )}

      {/* Table & Pagination */}
      {!error && (
        <div className={styles.tableCard}>
          <DataTable
            borderless
            columns={columns}
            data={cardsData.content}
            keyField="id"
            isLoading={cardsLoading}
            emptyMessage={t('content.orphanedCards.emptyList')}
          />

          {!cardsLoading && cardsData.totalElements > 0 && (
            <AdminPagination
              page={page}
              totalPages={cardsData.totalPages}
              totalElements={cardsData.totalElements}
              isLast={Boolean(cardsData.isLast ?? cardsData.last)}
              onPageChange={setPage}
              unitLabel={t('content.orphanedCards.unitLabel')}
              isLoading={cardsLoading}
            />
          )}
        </div>
      )}

      {/* Confirm Cleanup Modal */}
      {cleanupModal.isOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseCleanupModal}>
          <div
            className={styles.modalCard}
            role="dialog"
            aria-modal="true"
            aria-labelledby="orphanedCleanupModalTitle"
            onClick={(e) => e.stopPropagation()}
          >
            <div className={styles.modalHeader}>
              <div className={styles.modalTitleGroup}>
                <AlertTriangleIcon size={18} className={styles.modalWarnIcon} />
                <h3 id="orphanedCleanupModalTitle" className={styles.modalTitle}>
                  {t('content.orphanedCards.confirmModalTitle')}
                </h3>
              </div>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={handleCloseCleanupModal}
                disabled={cleanupModal.submitting}
                aria-label={t('content.orphanedCards.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>

            <div className={styles.modalBody}>
              {cleanupModal.error && (
                <p className={styles.errorMessage}>{cleanupModal.error}</p>
              )}
              <p className={styles.modalDesc}>
                {t('content.orphanedCards.confirmModalDesc', {
                  safeCount,
                  retainedCount: stats.retainedInStudyCards ?? 0,
                })}
              </p>
            </div>

            <div className={styles.modalFooter}>
              <button
                type="button"
                className={styles.cancelBtn}
                onClick={handleCloseCleanupModal}
                disabled={cleanupModal.submitting}
              >
                {t('content.orphanedCards.cancelBtn')}
              </button>
              <button
                type="button"
                className={styles.dangerBtn}
                onClick={handleConfirmCleanup}
                disabled={cleanupModal.submitting}
              >
                {cleanupModal.submitting
                  ? t('content.orphanedCards.cleaningBtn')
                  : t('content.orphanedCards.confirmCleanupBtn', { count: safeCount })}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
