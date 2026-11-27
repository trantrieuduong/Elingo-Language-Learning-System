import React, { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { AdminErrorState, AdminSkeletonCard } from '../../components/Feedback/AdminFeedback';
import {
  ArrowLeftIcon,
  BookOpenIcon,
  CheckCircleIcon,
  CloseIcon,
  CrownIcon,
  EditIcon,
  ExternalLinkIcon,
  RefreshIcon,
} from '../../components/icons/AdminIcons';
import {
  createLessonSegment,
  deleteLessonSegments,
  fetchAdminLessonDetail,
  fetchAdminTags,
  fetchCefrLevels,
  fetchLessonSegments,
  updateLessonSegment,
} from '../../services/adminContentApi';
import { extractVideoId } from './utils/youtube';
import { formatMs } from './utils/timeMask';
import YouTubePlayer from './components/YouTubePlayer';
import SegmentList from './components/SegmentList';
import SegmentForm from './components/SegmentForm';
import LessonFormModal from './components/LessonFormModal';
import LessonStatusModal from './components/LessonStatusModal';
import styles from './LessonDetailPage.module.css';

export default function LessonDetailPage({ lessonId, onNavigate }) {
  const { t } = useTranslation('admin');
  const playerRef = useRef(null);

  const [lesson, setLesson] = useState(null);
  const [segments, setSegments] = useState([]);
  const [tags, setTags] = useState([]);
  const [cefrLevels, setCefrLevels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);

  // Segment editor state
  const [selectedKey, setSelectedKey] = useState(null); // null | 'new' | segmentId
  const [isFormDirty, setIsFormDirty] = useState(false);
  const [savingSegment, setSavingSegment] = useState(false);
  const [segmentError, setSegmentError] = useState(null);
  const [playerState, setPlayerState] = useState(null);

  // Multi-select state
  const [selectMode, setSelectMode] = useState(false);
  const [checkedIds, setCheckedIds] = useState(new Set());

  // Modals
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    segment: null,
    submitting: false,
    error: null,
  });
  const [bulkDeleteModal, setBulkDeleteModal] = useState({
    isOpen: false,
    submitting: false,
    error: null,
  });
  const [discardModal, setDiscardModal] = useState({
    isOpen: false,
    action: null, // () => void
  });

  const reloadAll = useCallback(async () => {
    try {
      setError(null);
      const [lessonRes, segmentRes] = await Promise.all([
        fetchAdminLessonDetail(lessonId),
        fetchLessonSegments(lessonId),
      ]);
      setLesson(lessonRes || null);
      setSegments(segmentRes || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || t('feedback.errorDesc'));
    } finally {
      setLoading(false);
    }
  }, [lessonId, t]);

  useEffect(() => {
    setLoading(true);
    setSelectedKey(null);
    setSelectMode(false);
    setCheckedIds(new Set());
    reloadAll();
  }, [reloadAll]);

  useEffect(() => {
    Promise.all([fetchAdminTags(), fetchCefrLevels()])
      .then(([tagsData, cefrData]) => {
        setTags(tagsData || []);
        setCefrLevels(cefrData || []);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!toastMessage) return undefined;
    const timer = setTimeout(() => setToastMessage(null), 4000);
    return () => clearTimeout(timer);
  }, [toastMessage]);

  // Đồng bộ phát -> chọn đoạn (300ms)
  useEffect(() => {
    if (playerState !== 1 || isFormDirty || selectedKey === 'new' || selectMode) {
      return undefined;
    }
    const timer = setInterval(() => {
      const currentMs = (playerRef.current?.getCurrentTime() || 0) * 1000;
      const current = segments.find((s) => currentMs >= s.startMs && currentMs < s.endMs);
      if (current && current.id !== selectedKey) {
        setSelectedKey(current.id);
      }
    }, 300);
    return () => clearInterval(timer);
  }, [playerState, isFormDirty, selectedKey, selectMode, segments]);

  // Escape key closes open confirmation modals
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key !== 'Escape') return;
      if (discardModal.isOpen) {
        setDiscardModal({ isOpen: false, action: null });
      } else if (deleteModal.isOpen && !deleteModal.submitting) {
        setDeleteModal({ isOpen: false, segment: null, submitting: false, error: null });
      } else if (bulkDeleteModal.isOpen && !bulkDeleteModal.submitting) {
        setBulkDeleteModal({ isOpen: false, submitting: false, error: null });
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [discardModal.isOpen, deleteModal.isOpen, deleteModal.submitting, bulkDeleteModal.isOpen, bulkDeleteModal.submitting]);

  const applySelect = (key) => {
    setSelectedKey(key);
    setIsFormDirty(false);
    setSegmentError(null);
    if (key !== 'new' && key !== null) {
      const seg = segments.find((s) => s.id === key);
      if (seg) playerRef.current?.seekTo(seg.startMs / 1000);
    }
  };

  const requestSelect = (key) => {
    if (key === selectedKey) return;
    if (isFormDirty) {
      setDiscardModal({ isOpen: true, action: () => applySelect(key) });
      return;
    }
    applySelect(key);
  };

  const handleToggleSelectMode = () => {
    if (!selectMode && isFormDirty) {
      setDiscardModal({
        isOpen: true,
        action: () => {
          setIsFormDirty(false);
          setSelectedKey(null);
          setSegmentError(null);
          setSelectMode(true);
          setCheckedIds(new Set());
        },
      });
      return;
    }
    setSelectMode((prev) => {
      const next = !prev;
      if (!next) setCheckedIds(new Set());
      else {
        setSelectedKey(null);
        setSegmentError(null);
      }
      return next;
    });
  };

  const handleToggleChecked = (id) => {
    setCheckedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleToggleAll = () => {
    if (checkedIds.size === segments.length) {
      setCheckedIds(new Set());
    } else {
      setCheckedIds(new Set(segments.map((s) => s.id)));
    }
  };

  const handleSaveSegment = async (values) => {
    try {
      setSavingSegment(true);
      setSegmentError(null);
      const saved =
        selectedKey === 'new'
          ? await createLessonSegment({ lessonId: Number(lessonId), ...values })
          : await updateLessonSegment(selectedKey, values);
      await reloadAll();
      setSelectedKey(saved.id);
      setIsFormDirty(false);
      setToastMessage(t('content.lessons.segmentSaveSuccess'));
    } catch (err) {
      setSegmentError(err?.response?.data?.message || err?.message || t('feedback.errorDesc'));
    } finally {
      setSavingSegment(false);
    }
  };

  const handleConfirmDeleteSingle = async () => {
    if (!deleteModal.segment) return;
    try {
      setDeleteModal((prev) => ({ ...prev, submitting: true, error: null }));
      await deleteLessonSegments(Number(lessonId), [deleteModal.segment.id]);
      setDeleteModal({ isOpen: false, segment: null, submitting: false, error: null });
      setSelectedKey(null);
      setIsFormDirty(false);
      await reloadAll();
      setToastMessage(t('content.lessons.segmentDeleteSuccess'));
    } catch (err) {
      setDeleteModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  const handleConfirmBulkDelete = async () => {
    if (checkedIds.size === 0) return;
    const count = checkedIds.size;
    try {
      setBulkDeleteModal((prev) => ({ ...prev, submitting: true, error: null }));
      await deleteLessonSegments(Number(lessonId), [...checkedIds]);
      setBulkDeleteModal({ isOpen: false, submitting: false, error: null });
      setSelectMode(false);
      setCheckedIds(new Set());
      setSelectedKey(null);
      await reloadAll();
      setToastMessage(t('content.lessons.bulkDeleteSuccess', { count }));
    } catch (err) {
      setBulkDeleteModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  if (loading) {
    return (
      <div className={styles.pageContainer}>
        <AdminSkeletonCard />
        <AdminSkeletonCard />
      </div>
    );
  }

  if (error || !lesson) {
    return (
      <div className={styles.pageContainer}>
        <button
          type="button"
          className={styles.backBtn}
          onClick={() => onNavigate && onNavigate('/admin/lessons')}
        >
          <ArrowLeftIcon size={16} />
          <span>{t('content.lessons.backToLessons')}</span>
        </button>
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error || t('feedback.errorDesc')}
          onRetry={reloadAll}
        />
      </div>
    );
  }

  const videoId = extractVideoId(lesson.sourceUrl);
  const selectedSegment =
    selectedKey && selectedKey !== 'new'
      ? segments.find((s) => s.id === selectedKey) || null
      : null;
  const formMode = selectMode
    ? null
    : selectedKey === 'new'
    ? 'new'
    : selectedSegment
    ? 'edit'
    : null;
  const lastEndMs = segments.length > 0 ? segments[segments.length - 1].endMs : 0;

  const statusBadgeClass =
    lesson.status === 'PUBLISHED'
      ? styles.badgePublished
      : lesson.status === 'ARCHIVED'
      ? styles.badgeArchived
      : styles.badgeDraft;
  const statusLabel =
    lesson.status === 'PUBLISHED'
      ? t('content.lessons.statusPublished')
      : lesson.status === 'ARCHIVED'
      ? t('content.lessons.statusArchived')
      : t('content.lessons.statusDraft');

  return (
    <div className={styles.pageContainer}>
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

      {/* Header */}
      <div className={styles.pageHeader}>
        <button
          type="button"
          className={styles.backBtn}
          onClick={() => onNavigate && onNavigate('/admin/lessons')}
        >
          <ArrowLeftIcon size={16} />
          <span>{t('content.lessons.backToLessons')}</span>
        </button>

        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.secondaryBtn}
            onClick={() => setIsEditModalOpen(true)}
          >
            <EditIcon size={15} />
            <span>{t('content.lessons.editLessonBtn')}</span>
          </button>
          <button
            type="button"
            className={styles.secondaryBtn}
            onClick={() => setIsStatusModalOpen(true)}
          >
            <RefreshIcon size={15} />
            <span>{t('content.lessons.changeStatusBtn')}</span>
          </button>
        </div>
      </div>

      {/* Overview Card (giống DeckDetailPage) */}
      <div className={styles.overviewCard}>
        <div className={styles.overviewThumbWrap}>
          {lesson.thumbnailUrl ? (
            <img
              src={lesson.thumbnailUrl}
              alt={lesson.title}
              className={styles.overviewThumb}
            />
          ) : (
            <div className={styles.overviewThumbPlaceholder}>
              <BookOpenIcon size={28} />
            </div>
          )}
          <a
            href={lesson.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className={styles.sourceLink}
          >
            <span>{t('content.lessons.sourceLink')}</span>
            <ExternalLinkIcon size={13} />
          </a>
        </div>

        <div className={styles.overviewInfo}>
          <h1 className={styles.title}>{lesson.title}</h1>
          <span className={styles.slug}>{lesson.slug}</span>
          {lesson.description && (
            <p className={styles.description}>{lesson.description}</p>
          )}

          <div className={styles.pillsRow}>
            <span className={`${styles.badge} ${statusBadgeClass}`}>{statusLabel}</span>
            <span
              className={`${styles.badge} ${
                lesson.isPremium ? styles.badgePremium : styles.badgeFree
              }`}
            >
              {lesson.isPremium ? (
                <>
                  <CrownIcon size={12} />
                  {t('content.lessons.premiumTier')}
                </>
              ) : (
                t('content.lessons.freeTier')
              )}
            </span>
            {lesson.durationMs ? (
              <span className={styles.metaPill}>{formatMs(lesson.durationMs)}</span>
            ) : null}
            <span className={styles.metaPill}>
              {t('content.lessons.segmentsCount', { count: segments.length })}
            </span>
            {lesson.cefrLevels?.map((c) => (
              <span key={c.id} className={styles.cefrPill}>
                {c.label}
              </span>
            ))}
            {lesson.tags?.map((tg) => (
              <span key={tg.id} className={styles.tagPill}>
                #{tg.label}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Two-column Workspace */}
      <div className={styles.workspaceGrid}>
        <div className={styles.leftColumn}>
          <SegmentList
            segments={segments}
            selectedKey={selectedKey}
            onSelect={(id) => requestSelect(id)}
            onAdd={() => requestSelect('new')}
            selectMode={selectMode}
            onToggleSelectMode={handleToggleSelectMode}
            checkedIds={checkedIds}
            onToggleChecked={handleToggleChecked}
            onToggleAll={handleToggleAll}
            onRequestBulkDelete={() =>
              setBulkDeleteModal({ isOpen: true, submitting: false, error: null })
            }
          />
        </div>

        <div className={styles.rightColumn}>
          {videoId ? (
            <YouTubePlayer
              ref={playerRef}
              key={videoId}
              videoId={videoId}
              onStateChange={setPlayerState}
            />
          ) : (
            <div className={styles.videoErrorCard}>
              <p className={styles.videoErrorText}>{t('content.lessons.videoUnavailable')}</p>
            </div>
          )}

          <SegmentForm
            mode={formMode}
            segment={selectedSegment}
            segments={segments}
            durationMs={lesson.durationMs}
            defaultStartMs={lastEndMs}
            saving={savingSegment}
            error={segmentError}
            onSave={handleSaveSegment}
            onDelete={(seg) =>
              setDeleteModal({ isOpen: true, segment: seg, submitting: false, error: null })
            }
            onDirtyChange={setIsFormDirty}
            onCancel={() => requestSelect(null)}
          />
        </div>
      </div>

      {/* Edit Lesson Modal */}
      <LessonFormModal
        isOpen={isEditModalOpen}
        mode="edit"
        initialData={lesson}
        tags={tags}
        cefrLevels={cefrLevels}
        onClose={() => setIsEditModalOpen(false)}
        onSuccess={() => {
          setIsEditModalOpen(false);
          setToastMessage(t('content.lessons.lessonUpdateSuccess'));
          reloadAll();
        }}
      />

      {/* Status Modal */}
      <LessonStatusModal
        isOpen={isStatusModalOpen}
        lesson={lesson}
        onClose={() => setIsStatusModalOpen(false)}
        onSuccess={() => {
          setIsStatusModalOpen(false);
          setToastMessage(t('content.lessons.statusUpdateSuccess'));
          reloadAll();
        }}
      />

      {/* Delete Single Segment Modal */}
      {deleteModal.isOpen && (
        <div
          className={styles.modalBackdrop}
          onClick={() =>
            !deleteModal.submitting &&
            setDeleteModal({ isOpen: false, segment: null, submitting: false, error: null })
          }
        >
          <div
            className={styles.modalCard}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
          >
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>{t('content.lessons.deleteSegmentTitle')}</h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={() =>
                  setDeleteModal({ isOpen: false, segment: null, submitting: false, error: null })
                }
                disabled={deleteModal.submitting}
                aria-label={t('content.lessons.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <div className={styles.modalBody}>
              {deleteModal.error && <p className={styles.errorMessage}>{deleteModal.error}</p>}
              <p className={styles.modalDesc}>{t('content.lessons.deleteSegmentDesc')}</p>
            </div>
            <div className={styles.modalFooter}>
              <button
                type="button"
                className={styles.cancelBtn}
                onClick={() =>
                  setDeleteModal({ isOpen: false, segment: null, submitting: false, error: null })
                }
                disabled={deleteModal.submitting}
              >
                {t('content.lessons.cancelBtn')}
              </button>
              <button
                type="button"
                className={styles.dangerBtn}
                onClick={handleConfirmDeleteSingle}
                disabled={deleteModal.submitting}
              >
                {t('content.lessons.deleteSegmentBtn')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Bulk Delete Segments Modal */}
      {bulkDeleteModal.isOpen && (
        <div
          className={styles.modalBackdrop}
          onClick={() =>
            !bulkDeleteModal.submitting &&
            setBulkDeleteModal({ isOpen: false, submitting: false, error: null })
          }
        >
          <div
            className={styles.modalCard}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
          >
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>
                {t('content.lessons.bulkDeleteTitle', { count: checkedIds.size })}
              </h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={() =>
                  setBulkDeleteModal({ isOpen: false, submitting: false, error: null })
                }
                disabled={bulkDeleteModal.submitting}
                aria-label={t('content.lessons.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <div className={styles.modalBody}>
              {bulkDeleteModal.error && (
                <p className={styles.errorMessage}>{bulkDeleteModal.error}</p>
              )}
              <p className={styles.modalDesc}>{t('content.lessons.bulkDeleteDesc')}</p>
            </div>
            <div className={styles.modalFooter}>
              <button
                type="button"
                className={styles.cancelBtn}
                onClick={() =>
                  setBulkDeleteModal({ isOpen: false, submitting: false, error: null })
                }
                disabled={bulkDeleteModal.submitting}
              >
                {t('content.lessons.cancelBtn')}
              </button>
              <button
                type="button"
                className={styles.dangerBtn}
                onClick={handleConfirmBulkDelete}
                disabled={bulkDeleteModal.submitting}
              >
                {t('content.lessons.bulkDeleteBtn')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Discard Unsaved Changes Modal */}
      {discardModal.isOpen && (
        <div
          className={styles.modalBackdrop}
          onClick={() => setDiscardModal({ isOpen: false, action: null })}
        >
          <div
            className={styles.modalCard}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
          >
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>{t('content.lessons.discardTitle')}</h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={() => setDiscardModal({ isOpen: false, action: null })}
                aria-label={t('content.lessons.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <div className={styles.modalBody}>
              <p className={styles.modalDesc}>{t('content.lessons.discardDesc')}</p>
            </div>
            <div className={styles.modalFooter}>
              <button
                type="button"
                className={styles.cancelBtn}
                onClick={() => setDiscardModal({ isOpen: false, action: null })}
              >
                {t('content.lessons.keepEditingBtn')}
              </button>
              <button
                type="button"
                className={styles.dangerBtn}
                onClick={() => {
                  const act = discardModal.action;
                  setDiscardModal({ isOpen: false, action: null });
                  act?.();
                }}
              >
                {t('content.lessons.discardBtn')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

LessonDetailPage.propTypes = {
  lessonId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  onNavigate: PropTypes.func,
};
