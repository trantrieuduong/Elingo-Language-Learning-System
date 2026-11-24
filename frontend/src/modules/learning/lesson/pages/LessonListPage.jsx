import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../../context/AuthContext';
import { getLessonsApi } from '../lessonApi';
import { getTagsApi, getCefrLevelsApi } from '../../../vocabulary/vocabularyApi';
import FilterSection from '../../../../components/FilterSection/FilterSection';
import Pagination from '../../../../components/Pagination/Pagination';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import Toast from '../../../../components/Toast/Toast';
import OptionsModal from '../../../../components/OptionsModal/OptionsModal';
import LessonCard from '../components/LessonCard/LessonCard';
import LessonModeModal from '../components/LessonModeModal/LessonModeModal';
import ReportModal from '../../../report/components/ReportModal/ReportModal';
import { useReportModal } from '../../../report/hooks/useReportModal';
import './LessonListPage.css';

// ── Helper Functions ──
function normalizeApiResponse(response, key = 'data') {
  const data = response?.[key] || response;
  return Array.isArray(data) ? data : [];
}

function transformToFilterOptions(items) {
  return items.map((item) => ({ _id: item.id, name: item.label }));
}

function normalizeLessonsResponse(response) {
  const pageData = response?.data || response;
  if (pageData && Array.isArray(pageData.content)) {
    return {
      lessons: pageData.content,
      totalPages: pageData.totalPages || 1,
    };
  }
  return { lessons: [], totalPages: 1 };
}

// ── Helper Components ──
function LessonsContent({ lessons, user, onCardClick, onOptionsClick }) {
  return (
    <div className="lessons-grid">
      {lessons.map((lesson) => (
        <LessonCard
          key={lesson.id || lesson.slug}
          lesson={lesson}
          onClick={onCardClick}
          onOptionsClick={user ? onOptionsClick : undefined}
        />
      ))}
    </div>
  );
}

function EmptyContent({ isFiltering, error, t, onRetry, onClearFilters }) {
  if (error) {
    return (
      <EmptyState
        icon="error"
        isError
        description={error}
        actionLabel={t('common:retry')}
        onAction={onRetry}
      />
    );
  }

  return (
    <EmptyState
      icon="menu_book"
      title={t('lessonList.emptyTitle')}
      description={isFiltering ? t('lessonList.emptyFilter') : t('lessonList.emptyDefault')}
      actionLabel={isFiltering ? t('common:filter.clearFilters') : undefined}
      onAction={isFiltering ? onClearFilters : undefined}
      actionVariant="primary"
    />
  );
}

function LessonListPage({ onNavigate }) {
  const { t } = useTranslation(['learning', 'common', 'api', 'report']);
  const { user } = useAuth();

  // State của Report modal
  const { isReportOpen, targetType, targetId, targetLabel, openReport, closeReport } =
    useReportModal();
  const [targetLessonForOptions, setTargetLessonForOptions] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [toastVariant, setToastVariant] = useState('success');
  const handleToastDismiss = useCallback(() => setToastMessage(''), []);
  const showToast = useCallback((message, variant = 'success') => {
    setToastVariant(variant);
    setToastMessage(message);
  }, []);

  // Dữ liệu danh sách bài học và phân trang
  const [lessons, setLessons] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  // Dữ liệu metadata bộ lọc
  const [cefrLevels, setCefrLevels] = useState([]);
  const [tags, setTags] = useState([]);

  // State bộ lọc và tìm kiếm
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [selectedCefrLevelIds, setSelectedCefrLevelIds] = useState([]);
  const [selectedTagIds, setSelectedTagIds] = useState([]);

  // State trạng thái tải & lỗi
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);

  // State bài học được chọn để mở modal
  const [selectedLesson, setSelectedLesson] = useState(null);

  // Ref chống race condition khi tìm kiếm hoặc đổi trang liên tục
  const activeRequestIdRef = useRef(0);

  // Tải danh sách CEFR levels và Tags khi mount
  useEffect(() => {
    let isMounted = true;

    const fetchMetadata = async () => {
      try {
        const [cefrRes, tagsRes] = await Promise.all([getCefrLevelsApi(), getTagsApi()]);

        if (!isMounted) return;

        const cefrList = normalizeApiResponse(cefrRes);
        const tagsList = normalizeApiResponse(tagsRes);

        if (cefrList.length > 0) {
          setCefrLevels(transformToFilterOptions(cefrList));
        }

        if (tagsList.length > 0) {
          setTags(transformToFilterOptions(tagsList));
        }
      } catch {
        // Lỗi metadata không chặn việc hiển thị bài học, tiếp tục bình thường
      }
    };

    fetchMetadata();

    return () => {
      isMounted = false;
    };
  }, []);

  // Debounce tìm kiếm 500ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery.trim());
    }, 500);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Đổi từ khóa hoặc bộ lọc -> reset về trang 1
  useEffect(() => {
    setPage(1);
  }, [debouncedQuery, selectedCefrLevelIds, selectedTagIds]);

  // Tải danh sách bài học mỗi khi query, filter, page hoặc retryCount thay đổi
  useEffect(() => {
    let isMounted = true;
    const currentRequestId = ++activeRequestIdRef.current;

    const fetchLessons = async () => {
      setLoading(true);
      setError(null);

      try {
        const res = await getLessonsApi({
          keyword: debouncedQuery || undefined,
          cefrLevelIds: selectedCefrLevelIds,
          tagIds: selectedTagIds,
          page,
        });

        if (!isMounted || currentRequestId !== activeRequestIdRef.current) return;

        const { lessons: fetchedLessons, totalPages: fetchedTotalPages } = 
          normalizeLessonsResponse(res);
        
        setLessons(fetchedLessons);
        setTotalPages(fetchedTotalPages);
      } catch {
        if (!isMounted || currentRequestId !== activeRequestIdRef.current) return;
        setError(t('common:loadError'));
      } finally {
        if (isMounted && currentRequestId === activeRequestIdRef.current) {
          setLoading(false);
        }
      }
    };

    fetchLessons();

    return () => {
      isMounted = false;
    };
  }, [debouncedQuery, selectedCefrLevelIds, selectedTagIds, page, retryCount, t]);

  // Xử lý chuyển trang và cuộn mượt lên đầu danh sách
  const handlePageChange = (newPage) => {
    setPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Xóa toàn bộ bộ lọc
  const handleClearFilters = () => {
    setSearchQuery('');
    setDebouncedQuery('');
    setSelectedCefrLevelIds([]);
    setSelectedTagIds([]);
    setPage(1);
  };

  // Điều hướng sang chế độ học được chọn trong modal
  const handleSelectMode = (mode, lesson) => {
    if (!lesson) return;
    const slug = encodeURIComponent(lesson.slug);
    setSelectedLesson(null);
    onNavigate?.(`/lessons/${mode}/${slug}`);
  };

  const isFiltering = !!(
    debouncedQuery ||
    selectedCefrLevelIds.length > 0 ||
    selectedTagIds.length > 0
  );

  const lessonOptions = useMemo(() => {
    if (!targetLessonForOptions || !user) return [];
    
    return [
      {
        id: 'report-lesson',
        label: t('report:report'),
        icon: 'flag',
        onSelect: () => {
          openReport({
            targetType: 'LESSON',
            targetId: targetLessonForOptions.id,
            targetLabel: targetLessonForOptions.title,
          });
        },
      },
    ];
  }, [targetLessonForOptions, user, t, openReport]);

  const hasLessons = lessons.length > 0;

  return (
    <main className="lessons-container">
      <Toast message={toastMessage} variant={toastVariant} onDismiss={handleToastDismiss} />

      {/* Filter Section */}
      <FilterSection
        searchQuery={searchQuery}
        onSearchChange={(e) => setSearchQuery(e.target.value)}
        searchPlaceholder={t('lessonList.searchPlaceholder')}
        cefrLevels={cefrLevels}
        tags={tags}
        selectedCefrLevelIds={selectedCefrLevelIds}
        selectedTagIds={selectedTagIds}
        onCefrChange={setSelectedCefrLevelIds}
        onTagChange={setSelectedTagIds}
        hasActiveFilters={isFiltering}
        onClearFilters={handleClearFilters}
      />

      {/* Nội dung danh sách bài học */}
      {loading ? (
        <PageLoading />
      ) : error || !hasLessons ? (
        <EmptyContent
          isFiltering={isFiltering}
          error={error}
          t={t}
          onRetry={() => setRetryCount((prev) => prev + 1)}
          onClearFilters={handleClearFilters}
        />
      ) : (
        <>
          <LessonsContent
            lessons={lessons}
            user={user}
            onCardClick={setSelectedLesson}
            onOptionsClick={setTargetLessonForOptions}
          />

          {/* Phân trang */}
          <div className="lessons-pagination-wrapper">
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={handlePageChange}
            />
          </div>
        </>
      )}

      {/* Modal chọn chế độ Dictation / Shadowing */}
      <LessonModeModal
        isOpen={!!selectedLesson}
        lesson={selectedLesson}
        onSelectMode={handleSelectMode}
        onClose={() => setSelectedLesson(null)}
      />

      <OptionsModal
        isOpen={!!targetLessonForOptions}
        title={t("lessonList.lessonOptions")}
        options={lessonOptions}
        onClose={() => setTargetLessonForOptions(null)}
      />

      <ReportModal
        isOpen={isReportOpen}
        targetType={targetType}
        targetId={targetId}
        targetLabel={targetLabel}
        onClose={closeReport}
        onSuccess={() => showToast(t('report:successMessage'))}
      />
    </main>
  );
}

export default LessonListPage;
