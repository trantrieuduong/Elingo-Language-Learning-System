import { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { getLessonsApi } from '../lessonApi'
import { getTagsApi, getCefrLevelsApi } from '../../../vocabulary/vocabularyApi'
import Filter from '../../../../components/Filter/Filter'
import Pagination from '../../../../components/Pagination/Pagination'
import PageError from '../../../../components/PageError/PageError'
import Input from '../../../../components/Input/Input'
import LessonCard from '../components/LessonCard/LessonCard'
import LessonModeModal from '../components/LessonModeModal/LessonModeModal'
import './LessonListPage.css'

function LessonListPage({ onNavigate }) {
  const { t } = useTranslation(['learning', 'api'])

  // Dữ liệu danh sách bài học và phân trang
  const [lessons, setLessons] = useState([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  // Dữ liệu metadata bộ lọc
  const [cefrLevels, setCefrLevels] = useState([])
  const [tags, setTags] = useState([])

  // State bộ lọc và tìm kiếm
  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [selectedCefrCode, setSelectedCefrCode] = useState(null)
  const [selectedTagCode, setSelectedTagCode] = useState(null)

  // State trạng thái tải & lỗi
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [retryCount, setRetryCount] = useState(0)

  // State bài học được chọn để mở modal
  const [selectedLesson, setSelectedLesson] = useState(null)

  // Ref chống race condition khi tìm kiếm hoặc đổi trang liên tục
  const activeRequestIdRef = useRef(0)

  // Tải danh sách CEFR levels và Tags khi mount
  useEffect(() => {
    let isMounted = true

    const fetchMetadata = async () => {
      try {
        const [cefrRes, tagsRes] = await Promise.all([
          getCefrLevelsApi(),
          getTagsApi(),
        ])

        const cefrList = Array.isArray(cefrRes?.data) ? cefrRes.data : Array.isArray(cefrRes) ? cefrRes : []
        const tagsList = Array.isArray(tagsRes?.data) ? tagsRes.data : Array.isArray(tagsRes) ? tagsRes : []

        if (cefrList.length > 0) {
          // Filter component yêu cầu {_id, name}
          setCefrLevels(cefrList.map((item) => ({ _id: item.code, name: item.label || item.code })))
        }

        if (tagsList.length > 0) {
          setTags(tagsList.map((item) => ({ _id: item.code, name: item.label || item.code })))
        }
      } catch {
        // Lỗi metadata không chặn việc hiển thị bài học, tiếp tục bình thường
      }
    }

    fetchMetadata()

    return () => {
      isMounted = false
    }
  }, [])

  // Debounce tìm kiếm 500ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery.trim())
    }, 500)

    return () => clearTimeout(timer)
  }, [searchQuery])

  // Đổi từ khóa hoặc bộ lọc -> reset về trang 1
  useEffect(() => {
    setPage(1)
  }, [debouncedQuery, selectedCefrCode, selectedTagCode])

  // Tải danh sách bài học mỗi khi query, filter, page hoặc retryCount thay đổi
  useEffect(() => {
    let isMounted = true
    const currentRequestId = ++activeRequestIdRef.current

    const fetchLessons = async () => {
      setLoading(true)
      setError(null)

      try {
        const res = await getLessonsApi({
          keyword: debouncedQuery || undefined,
          cefrCode: selectedCefrCode || undefined,
          tagCode: selectedTagCode || undefined,
          page,
        })

        if (!isMounted || currentRequestId !== activeRequestIdRef.current) return

        // BE trả { success: true, data: { content, totalPages, ... } }
        const pageData = res?.data || res
        if (pageData && Array.isArray(pageData.content)) {
          setLessons(pageData.content)
          setTotalPages(pageData.totalPages || 1)
        } else {
          setLessons([])
          setTotalPages(1)
        }
      } catch (err) {
        if (!isMounted || currentRequestId !== activeRequestIdRef.current) return
        const msg = err.response?.data?.message || t('lessonList.loadError')
        setError(msg)
      } finally {
        if (isMounted && currentRequestId === activeRequestIdRef.current) {
          setLoading(false)
        }
      }
    }

    fetchLessons()

    return () => {
      isMounted = false
    }
  }, [debouncedQuery, selectedCefrCode, selectedTagCode, page, retryCount, t])

  // Xử lý chuyển trang và cuộn mượt lên đầu danh sách
  const handlePageChange = (newPage) => {
    setPage(newPage)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Xóa toàn bộ bộ lọc
  const handleClearFilters = () => {
    setSearchQuery('')
    setDebouncedQuery('')
    setSelectedCefrCode(null)
    setSelectedTagCode(null)
    setPage(1)
  }

  // Điều hướng sang chế độ học được chọn trong modal
  const handleSelectMode = (mode, lesson) => {
    if (!lesson) return
    const slug = encodeURIComponent(lesson.slug)
    setSelectedLesson(null)
    onNavigate?.(`/learning/${mode}/${slug}`)
  }

  const isFiltering = !!(debouncedQuery || selectedCefrCode || selectedTagCode)

  return (
    <main className="lessons-container">

      {/* Filter Section */}
      <section className="lessons-filters glass-well">
        <div className="lessons-filters__top">
          <div className="lessons-filters__search">
            <Input
              id="search-lessons"
              placeholder={t('lessonList.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              rightElement={<span className="material-symbols-outlined">search</span>}
            />
          </div>
          {isFiltering && (
            <button
              type="button"
              className="btn-clear-filter"
              onClick={handleClearFilters}
            >
              <span className="material-symbols-outlined" aria-hidden="true">filter_alt_off</span>
              {t('lessonList.clearFilters')}
            </button>
          )}
        </div>

        <Filter
          cefrLevels={cefrLevels}
          tags={tags}
          selectedCefrLevelId={selectedCefrCode}
          selectedTagId={selectedTagCode}
          onCefrChange={setSelectedCefrCode}
          onTagChange={setSelectedTagCode}
        />
      </section>

      {/* Nội dung danh sách bài học */}
      {loading ? (
        <div className="lessons-loading">
          <div className="app-loading-spinner" aria-hidden="true" />
          <p className="text-body-md text-disabled">{t('lessonList.loading')}</p>
        </div>
      ) : error ? (
        <div className="lessons-error-wrapper">
          <PageError
            error={error}
            onAction={() => setRetryCount((prev) => prev + 1)}
            actionLabel={t('lessonList.retry')}
          />
        </div>
      ) : lessons.length === 0 ? (
        <div className="lessons-empty glass-card">
          <span className="material-symbols-outlined lessons-empty__icon" aria-hidden="true">
            menu_book
          </span>
          <h2 className="text-headline-sm lessons-empty__title">{t('lessonList.emptyTitle')}</h2>
          <p className="text-body-md text-disabled lessons-empty__desc">
            {isFiltering ? t('lessonList.emptyFilter') : t('lessonList.emptyDefault')}
          </p>
          {isFiltering && (
            <button
              type="button"
              className="btn-primary"
              onClick={handleClearFilters}
            >
              {t('lessonList.clearFilters')}
            </button>
          )}
        </div>
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
  )
}

export default LessonListPage
