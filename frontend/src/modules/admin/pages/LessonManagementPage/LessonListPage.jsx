import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import AdminPagination from '../../components/AdminPagination/AdminPagination';
import {
  BookOpenIcon,
  RefreshIcon,
  CrownIcon,
  CheckCircleIcon,
  PlusIcon,
  EditIcon,
  EyeIcon,
} from '../../components/icons/AdminIcons';
import {
  fetchAdminLessons,
  fetchAdminTags,
  fetchCefrLevels,
} from '../../services/adminContentApi';
import LessonFilterBar from './components/LessonFilterBar';
import LessonStatusModal from './components/LessonStatusModal';
import LessonFormModal from './components/LessonFormModal';
import styles from './LessonListPage.module.css';

const PAGE_SIZE = 8;

const emptyPageData = {
  content: [],
  page: 1,
  size: PAGE_SIZE,
  totalElements: 0,
  totalPages: 1,
  isLast: true,
};

export default function LessonListPage({ onNavigate }) {
  const { t } = useTranslation('admin');

  // Filter state
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [isPremium, setIsPremium] = useState('');
  const [tagId, setTagId] = useState('');
  const [cefrId, setCefrId] = useState('');
  const [page, setPage] = useState(1);

  // Catalogs
  const [tags, setTags] = useState([]);
  const [cefrLevels, setCefrLevels] = useState([]);

  // Data & UI state
  const [lessonData, setLessonData] = useState(emptyPageData);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);
  const [statusModal, setStatusModal] = useState({ isOpen: false, lesson: null });
  const [formModal, setFormModal] = useState({
    isOpen: false,
    mode: 'create',
    initialData: null,
  });

  // Load tags & CEFR levels once on mount
  useEffect(() => {
    const loadCatalogs = async () => {
      try {
        const [tagsData, cefrData] = await Promise.all([
          fetchAdminTags(),
          fetchCefrLevels(),
        ]);
        setTags(tagsData || []);
        setCefrLevels(cefrData || []);
      } catch {
        // Silently tolerate catalog failure
      }
    };
    loadCatalogs();
  }, []);

  // Load lessons
  const loadLessons = useCallback(
    async (isManualRefresh = false) => {
      try {
        if (isManualRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }
        setError(null);

        const res = await fetchAdminLessons({
          keyword: keyword.trim() || undefined,
          status: status || undefined,
          isPremium: isPremium === '' ? undefined : isPremium === 'true',
          tagIds: tagId ? [Number(tagId)] : undefined,
          cefrLevelIds: cefrId ? [Number(cefrId)] : undefined,
          page,
          size: PAGE_SIZE,
          sort: 'createdAt,desc',
        });

        setLessonData(res || emptyPageData);
      } catch (err) {
        setError(err?.response?.data?.message || err?.message || t('feedback.errorDesc'));
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [keyword, status, isPremium, tagId, cefrId, page, t]
  );

  useEffect(() => {
    loadLessons();
  }, [loadLessons]);

  // Auto-dismiss toast after 4s
  useEffect(() => {
    if (!toastMessage) return undefined;
    const timer = setTimeout(() => setToastMessage(null), 4000);
    return () => clearTimeout(timer);
  }, [toastMessage]);

  const handleApplyFilter = () => {
    setPage(1);
  };

  const handleResetFilter = () => {
    setKeyword('');
    setStatus('');
    setIsPremium('');
    setTagId('');
    setCefrId('');
    setPage(1);
  };

  const handleStatusChange = (val) => {
    setStatus(val);
    setPage(1);
  };

  const handlePremiumChange = (val) => {
    setIsPremium(val);
    setPage(1);
  };

  const handleTagChange = (val) => {
    setTagId(val);
    setPage(1);
  };

  const handleCefrChange = (val) => {
    setCefrId(val);
    setPage(1);
  };

  const handleStatusSuccess = () => {
    setStatusModal({ isOpen: false, lesson: null });
    setToastMessage(t('content.lessons.statusUpdateSuccess'));
    loadLessons();
  };

  const handleOpenCreate = () => {
    setFormModal({ isOpen: true, mode: 'create', initialData: null });
  };

  const handleOpenEdit = (lesson) => {
    setFormModal({ isOpen: true, mode: 'edit', initialData: lesson });
  };

  const handleFormSuccess = () => {
    const isEdit = formModal.mode === 'edit';
    setFormModal({ isOpen: false, mode: 'create', initialData: null });
    setToastMessage(
      isEdit ? t('content.lessons.lessonUpdateSuccess') : t('content.lessons.lessonCreateSuccess')
    );
    if (!isEdit && page !== 1) {
      setPage(1);
    } else {
      loadLessons();
    }
  };

  const columns = useMemo(
    () => [
      {
        key: 'lesson',
        title: t('content.lessons.colLesson'),
        render: (_val, record) => (
          <div className={styles.lessonCell}>
            {record.thumbnailUrl ? (
              <img
                src={record.thumbnailUrl}
                alt={record.title}
                loading="lazy"
                className={styles.lessonThumb}
              />
            ) : (
              <div
                className={styles.lessonThumbPlaceholder}
                title={t('content.lessons.noThumbnail')}
              >
                <BookOpenIcon size={18} />
              </div>
            )}
            <div className={styles.lessonInfo}>
              <a
                href={`/admin/lessons/${record.id}`}
                className={styles.lessonTitle}
                title={record.title}
                onClick={(e) => {
                  e.preventDefault();
                  if (onNavigate) onNavigate(`/admin/lessons/${record.id}`);
                }}
              >
                {record.title}
              </a>
              <span className={styles.lessonSlug}>{record.slug}</span>
              {record.description && (
                <span className={styles.lessonDesc} title={record.description}>
                  {record.description}
                </span>
              )}
            </div>
          </div>
        ),
      },
      {
        key: 'status',
        title: t('content.lessons.colStatus'),
        dataIndex: 'status',
        width: '120px',
        render: (val) => {
          let badgeClass = styles.badgeDraft;
          let label = t('content.lessons.statusDraft');
          if (val === 'PUBLISHED') {
            badgeClass = styles.badgePublished;
            label = t('content.lessons.statusPublished');
          } else if (val === 'ARCHIVED') {
            badgeClass = styles.badgeArchived;
            label = t('content.lessons.statusArchived');
          }
          return <span className={`${styles.badge} ${badgeClass}`}>{label}</span>;
        },
      },
      {
        key: 'isPremium',
        title: t('content.lessons.colAccess'),
        dataIndex: 'isPremium',
        width: '100px',
        render: (val) => (
          <span className={`${styles.badge} ${val ? styles.badgePremium : styles.badgeFree}`}>
            {val ? (
              <>
                <CrownIcon size={12} />
                {t('content.lessons.premiumTier')}
              </>
            ) : (
              t('content.lessons.freeTier')
            )}
          </span>
        ),
      },
      {
        key: 'cefrLevels',
        title: t('content.lessons.colLevel'),
        dataIndex: 'cefrLevels',
        width: '110px',
        render: (levels) => (
          <div className={styles.pillsGroup}>
            {levels?.length > 0
              ? levels.map((cefr) => (
                  <span key={cefr.id} className={styles.cefrPill}>
                    {cefr.label}
                  </span>
                ))
              : '—'}
          </div>
        ),
      },
      {
        key: 'tags',
        title: t('content.lessons.colTags'),
        dataIndex: 'tags',
        width: '150px',
        render: (tagList) => {
          if (!tagList || tagList.length === 0) return '—';
          const visible = tagList.slice(0, 3);
          const extraCount = tagList.length - visible.length;
          return (
            <div className={styles.pillsGroup}>
              {visible.map((tag) => (
                <span key={tag.id} className={styles.tagPill}>
                  #{tag.label}
                </span>
              ))}
              {extraCount > 0 && (
                <span className={styles.tagPill}>+{extraCount}</span>
              )}
            </div>
          );
        },
      },
      {
        key: 'segmentCount',
        title: t('content.lessons.colSegments'),
        dataIndex: 'segmentCount',
        width: '130px',
        render: (val) => (
          <span className={styles.segmentCount}>
            {t('content.lessons.segmentsCount', { count: val ?? 0 })}
          </span>
        ),
      },
      {
        key: 'updatedAt',
        title: t('content.lessons.colUpdated'),
        dataIndex: 'updatedAt',
        width: '110px',
        render: (val) => {
          if (!val) return '—';
          return (
            <span className={styles.dateCell} title={val.replace('T', ' ')}>
              {val.substring(0, 10)}
            </span>
          );
        },
      },
      {
        key: 'actions',
        title: t('content.lessons.colActions'),
        width: '140px',
        align: 'right',
        render: (_val, record) => (
          <div className={styles.actionsCell}>
            <button
              type="button"
              className={styles.actionBtn}
              onClick={() => onNavigate && onNavigate(`/admin/lessons/${record.id}`)}
              title={t('content.lessons.viewDetail')}
              aria-label={`${t('content.lessons.viewDetail')} ${record.title}`}
            >
              <EyeIcon size={15} />
            </button>
            <button
              type="button"
              className={styles.actionBtn}
              onClick={() => handleOpenEdit(record)}
              title={t('content.lessons.editLessonBtn')}
              aria-label={`${t('content.lessons.editLessonBtn')} ${record.title}`}
            >
              <EditIcon size={15} />
            </button>
            <button
              type="button"
              className={styles.actionBtn}
              onClick={() => setStatusModal({ isOpen: true, lesson: record })}
              title={t('content.lessons.changeStatusBtn')}
              aria-label={`${t('content.lessons.changeStatusBtn')} ${record.title}`}
            >
              <RefreshIcon size={15} />
            </button>
          </div>
        ),
      },
    ],
    [t]
  );

  return (
    <div className={styles.pageContainer}>
      {/* Toast Feedback */}
      {toastMessage && (
        <div className={styles.toast} role="status">
          <CheckCircleIcon size={16} className={styles.toastIcon} />
          <span>{toastMessage}</span>
          <button
            type="button"
            className={styles.toastCloseBtn}
            onClick={() => setToastMessage(null)}
            aria-label={t('common.close')}
          >
            ×
          </button>
        </div>
      )}

      {/* Page Header */}
      <div className={styles.pageHeader}>
        <div className={styles.headerInfo}>
          <div className={styles.titleRow}>
            <BookOpenIcon size={24} className={styles.headerIcon} />
            <h1 className={styles.title}>{t('content.lessons.title')}</h1>
          </div>
          <p className={styles.subtitle}>{t('content.lessons.subtitle')}</p>
        </div>

        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => loadLessons(true)}
            disabled={loading || refreshing}
            title={t('common.refresh')}
          >
            <RefreshIcon
              size={16}
              className={`${styles.refreshIcon} ${refreshing ? styles.spinning : ''}`}
            />
            <span>{t('common.refresh')}</span>
          </button>
          <button
            type="button"
            className={styles.primaryBtn}
            onClick={handleOpenCreate}
          >
            <PlusIcon size={16} />
            <span>{t('content.lessons.createLesson')}</span>
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <LessonFilterBar
        keyword={keyword}
        onKeywordChange={setKeyword}
        status={status}
        onStatusChange={handleStatusChange}
        isPremium={isPremium}
        onPremiumChange={handlePremiumChange}
        tagId={tagId}
        onTagChange={handleTagChange}
        cefrId={cefrId}
        onCefrChange={handleCefrChange}
        tags={tags}
        cefrLevels={cefrLevels}
        onApply={handleApplyFilter}
        onReset={handleResetFilter}
        loading={loading || refreshing}
      />

      {/* Error State */}
      {error && (
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={() => loadLessons(false)}
        />
      )}

      {/* Table & Pagination */}
      {!error && (
        <div className={styles.tableCard}>
          <DataTable
            borderless
            columns={columns}
            data={lessonData.content}
            keyField="id"
            isLoading={loading}
            emptyMessage={t('content.lessons.emptyDesc')}
          />

          {!loading && lessonData.totalElements > 0 && (
            <AdminPagination
              page={page}
              totalPages={lessonData.totalPages}
              totalElements={lessonData.totalElements}
              isLast={Boolean(lessonData.isLast ?? lessonData.last)}
              onPageChange={setPage}
              unitLabel={t('content.lessons.unitLabel')}
              isLoading={loading}
            />
          )}
        </div>
      )}

      {/* Status Modal */}
      <LessonStatusModal
        isOpen={statusModal.isOpen}
        lesson={statusModal.lesson}
        onClose={() => setStatusModal({ isOpen: false, lesson: null })}
        onSuccess={handleStatusSuccess}
      />

      {/* Create / Edit Modal */}
      <LessonFormModal
        isOpen={formModal.isOpen}
        mode={formModal.mode}
        initialData={formModal.initialData}
        tags={tags}
        cefrLevels={cefrLevels}
        onClose={() => setFormModal({ isOpen: false, mode: 'create', initialData: null })}
        onSuccess={handleFormSuccess}
      />
    </div>
  );
}

LessonListPage.propTypes = {
  onNavigate: PropTypes.func,
};
