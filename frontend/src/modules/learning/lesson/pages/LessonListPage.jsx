import { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { getLessonsApi } from '../lessonApi';
import { getTagsApi, getCefrLevelsApi } from '../../../vocabulary/vocabularyApi';
import FilterSection from '../../../../components/FilterSection/FilterSection';
import Pagination from '../../../../components/Pagination/Pagination';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import LessonCard from '../components/LessonCard/LessonCard';
import LessonModeModal from '../components/LessonModeModal/LessonModeModal';
import './LessonListPage.css';

function LessonListPage({ onNavigate }) {
  const { t } = useTranslation(['learning', 'common', 'api']);

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

        const cefrList = Array.isArray(cefrRes?.data)
          ? cefrRes.data
          : Array.isArray(cefrRes)
            ? cefrRes
            : [];
        const tagsList = Array.isArray(tagsRes?.data)
          ? tagsRes.data
          : Array.isArray(tagsRes)
            ? tagsRes
            : [];

        if (cefrList.length > 0) {
          // Filter component yêu cầu {_id, name}
          setCefrLevels(cefrList.map((item) => ({ _id: item.id, name: item.label })));
        }

        if (tagsList.length > 0) {
          setTags(tagsList.map((item) => ({ _id: item.id, name: item.label })));
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

        // BE trả { success: true, data: { content, totalPages, ... } }
        const pageData = res?.data || res;
        if (pageData && Array.isArray(pageData.content)) {
          setLessons(pageData.content);
          setTotalPages(pageData.totalPages || 1);
        } else {
          setLessons([]);
          setTotalPages(1);
        }
      } catch {
        if (!isMounted || currentRequestId !== activeRequestIdRef.current) return;
        const msg = t('common:loadError');
        setError(msg);
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
    onNavigate?.(`/learning/${mode}/${slug}`);
  };

  const isFiltering = !!(
    debouncedQuery ||
    selectedCefrLevelIds.length > 0 ||
    selectedTagIds.length > 0
  );

  return (
    <main className="lessons-container">
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
      ) : error ? (
        <EmptyState
          icon="error"
          isError
          description={error}
          actionLabel={t('common:retry')}
          onAction={() => setRetryCount((prev) => prev + 1)}
        />
      ) : lessons.length === 0 ? (
        <EmptyState
          icon="menu_book"
          title={t('lessonList.emptyTitle')}
          description={isFiltering ? t('lessonList.emptyFilter') : t('lessonList.emptyDefault')}
          actionLabel={isFiltering ? t('common:filter.clearFilters') : undefined}
          onAction={isFiltering ? handleClearFilters : undefined}
          actionVariant="primary"
        />
      ) : (
        <>
          <div className="lessons-grid">
            {lessons.map((lesson) => (
              <LessonCard
                key={lesson.id || lesson.slug}
                lesson={lesson}
                onClick={setSelectedLesson}
              />
            ))}
          </div>

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
    </main>
  );
}

export default LessonListPage;
