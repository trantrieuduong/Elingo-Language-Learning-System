import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LessonListPage from '@/modules/admin/pages/LessonManagementPage/LessonListPage';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import { useLang, LANGS, tr } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  fetchAdminLessons: vi.fn(),
  fetchAdminTags: vi.fn(),
  fetchCefrLevels: vi.fn(),
  createAdminLesson: vi.fn(),
  updateAdminLesson: vi.fn(),
  publishAdminLesson: vi.fn(),
  unpublishAdminLesson: vi.fn(),
  archiveAdminLesson: vi.fn(),
}));

vi.mock('@/services/uploadFile', () => ({
  uploadFileForVerification: vi.fn(),
}));

// YouTubePlayer thật sẽ nạp iframe API của YouTube (jsdom không chạy được),
// nên thay bằng stub có nút "mock-player-ready" để giả lập player đã lấy xong thời lượng video.
vi.mock(
  '@/modules/admin/pages/LessonManagementPage/components/YouTubePlayer',
  async () => {
    const React = (await import('react')).default;
    const MockYouTubePlayer = React.forwardRef(function MockYouTubePlayer(
      { videoId, onReady },
      _ref
    ) {
      return React.createElement(
        'div',
        { 'data-testid': 'youtube-player', 'data-video-id': videoId },
        React.createElement(
          'button',
          { type: 'button', onClick: () => onReady?.({ durationMs: 120000 }) },
          'mock-player-ready'
        )
      );
    });
    return { default: MockYouTubePlayer };
  }
);

// Helpers: lấy chuỗi i18n của namespace admin
const L = (key, options) => tr(`admin:content.lessons.${key}`, options);
const A = (key, options) => tr(`admin:${key}`, options);

// YouTube video id hợp lệ phải đúng 11 ký tự [A-Za-z0-9_-]
const VALID_URL = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';

const pageOf = (content, overrides = {}) => ({
  content,
  page: 1,
  size: 8,
  totalElements: content.length,
  totalPages: 1,
  isLast: true,
  ...overrides,
});

describe('LessonListPage component', () => {
  const mockNavigate = vi.fn();

  const sampleLesson = {
    id: 1,
    title: 'English Grammar Basics',
    slug: 'english-grammar-basics',
    description: 'Learn fundamental grammar rules',
    sourceUrl: VALID_URL,
    durationMs: 600000,
    thumbnailUrl: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
    isPremium: false,
    status: 'PUBLISHED',
    segmentCount: 5,
    tags: [{ id: 1, label: 'Grammar' }],
    cefrLevels: [{ id: 2, label: 'B1' }],
    updatedAt: '2024-01-15T10:30:00',
  };

  const sampleDraftLesson = {
    id: 2,
    title: 'Advanced Vocabulary',
    slug: 'advanced-vocabulary',
    description: 'Expand your vocabulary',
    sourceUrl: 'https://www.youtube.com/watch?v=xyz78901234',
    durationMs: 480000,
    thumbnailUrl: null,
    isPremium: true,
    status: 'DRAFT',
    segmentCount: 0,
    tags: [],
    cefrLevels: [{ id: 4, label: 'C1' }],
    updatedAt: '2024-01-10T09:15:00',
  };

  const lastParams = () => adminContentApi.fetchAdminLessons.mock.calls.at(-1)[0];
  const renderPage = () => render(<LessonListPage onNavigate={mockNavigate} />);
  const waitForSampleLesson = () => screen.findByText('English Grammar Basics');

  beforeEach(() => {
    vi.clearAllMocks();
    Object.values(adminContentApi).forEach((fn) => fn.mockReset());

    adminContentApi.fetchAdminLessons.mockResolvedValue(pageOf([sampleLesson]));
    adminContentApi.fetchAdminTags.mockResolvedValue([
      { id: 1, label: 'Grammar' },
      { id: 2, label: 'Vocabulary' },
    ]);
    adminContentApi.fetchCefrLevels.mockResolvedValue([
      { id: 1, label: 'A1' },
      { id: 2, label: 'B1' },
      { id: 3, label: 'B2' },
      { id: 4, label: 'C1' },
    ]);
  });

  describe.each(LANGS)('i18n rendering and Lesson list operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders lessons list correctly', async () => {
      adminContentApi.fetchAdminLessons.mockResolvedValue(
        pageOf([sampleLesson, sampleDraftLesson])
      );

      renderPage();

      expect(await screen.findByText('English Grammar Basics')).toBeInTheDocument();
      expect(screen.getByText('Advanced Vocabulary')).toBeInTheDocument();
      expect(screen.getByRole('heading', { level: 1, name: L('title') })).toBeInTheDocument();

      const table = within(screen.getByRole('table'));
      expect(table.getByText(L('segmentsCount', { count: 5 }))).toBeInTheDocument();
      expect(table.getByText('2024-01-15')).toBeInTheDocument();
      expect(table.getByText('B1')).toBeInTheDocument();
      expect(table.getByText('#Grammar')).toBeInTheDocument();
    });

    it('requests the first page with default params on mount', async () => {
      renderPage();
      await waitForSampleLesson();

      expect(adminContentApi.fetchAdminLessons).toHaveBeenCalled();
      expect(lastParams()).toEqual(
        expect.objectContaining({ page: 1, size: 8, sort: 'createdAt,desc' })
      );
      expect(lastParams().keyword).toBeUndefined();
      expect(lastParams().status).toBeUndefined();
      expect(lastParams().isPremium).toBeUndefined();
    });

    it('shows loading state while fetching lessons', async () => {
      adminContentApi.fetchAdminLessons.mockImplementationOnce(() => new Promise(() => {}));

      renderPage();

      expect(await screen.findByText(A('feedback.loadingList'))).toBeInTheDocument();
    });

    it('filters lessons by search keyword', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.type(screen.getByPlaceholderText(L('searchPlaceholder')), 'Grammar');

      await waitFor(() => {
        expect(adminContentApi.fetchAdminLessons).toHaveBeenCalledWith(
          expect.objectContaining({ keyword: 'Grammar' })
        );
      });
    });

    it('filters lessons by status', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.selectOptions(screen.getByLabelText(L('statusFilter')), 'PUBLISHED');

      await waitFor(() => {
        expect(adminContentApi.fetchAdminLessons).toHaveBeenCalledWith(
          expect.objectContaining({ status: 'PUBLISHED', page: 1 })
        );
      });
    });

    it('filters lessons by premium tier', async () => {
      adminContentApi.fetchAdminLessons.mockResolvedValue(pageOf([sampleDraftLesson]));

      const user = userEvent.setup();
      renderPage();
      await screen.findByText('Advanced Vocabulary');

      const tierSelect = screen.getByLabelText(L('premiumFilter'));

      await user.selectOptions(tierSelect, 'true');
      await waitFor(() => expect(lastParams().isPremium).toBe(true));

      await user.selectOptions(tierSelect, 'false');
      await waitFor(() => expect(lastParams().isPremium).toBe(false));
    });

    it('filters lessons by tag', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();
      // Danh sách tag được nạp bất đồng bộ, phải chờ option xuất hiện mới chọn được
      await screen.findByRole('option', { name: '#Vocabulary' });

      await user.selectOptions(screen.getByLabelText(L('tagFilter')), '2');

      await waitFor(() => {
        expect(adminContentApi.fetchAdminLessons).toHaveBeenCalledWith(
          expect.objectContaining({ tagIds: [2] })
        );
      });
    });

    it('filters lessons by CEFR level', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();
      await screen.findByRole('option', { name: 'B2' });

      await user.selectOptions(screen.getByLabelText(L('cefrFilter')), '3');

      await waitFor(() => {
        expect(adminContentApi.fetchAdminLessons).toHaveBeenCalledWith(
          expect.objectContaining({ cefrLevelIds: [3] })
        );
      });
    });

    it('resets all filters when reset button is clicked', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      const searchInput = screen.getByPlaceholderText(L('searchPlaceholder'));
      await user.type(searchInput, 'Test');
      await user.selectOptions(screen.getByLabelText(L('statusFilter')), 'DRAFT');
      await waitFor(() => expect(lastParams().status).toBe('DRAFT'));

      // Nút xóa bộ lọc bị khóa trong lúc đang tải
      const resetBtn = screen.getByRole('button', { name: L('clearFilters') });
      await waitFor(() => expect(resetBtn).toBeEnabled());
      await user.click(resetBtn);

      expect(searchInput).toHaveValue('');
      expect(screen.getByLabelText(L('statusFilter'))).toHaveValue('');
      await waitFor(() => {
        expect(lastParams().keyword).toBeUndefined();
        expect(lastParams().status).toBeUndefined();
        expect(lastParams().page).toBe(1);
      });
    });

    it('creates a lesson from the create modal', async () => {
      adminContentApi.createAdminLesson.mockResolvedValueOnce({
        id: 10,
        title: 'New Lesson',
        status: 'DRAFT',
      });

      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.click(screen.getByRole('button', { name: L('createLesson') }));

      const dialog = within(screen.getByRole('dialog'));
      expect(dialog.getByRole('heading', { name: L('createModalTitle') })).toBeInTheDocument();

      await user.type(dialog.getByLabelText(`${L('lessonTitleLabel')} *`), 'New Lesson');
      await user.type(dialog.getByLabelText(`${L('sourceUrlLabel')} *`), VALID_URL);
      await user.click(dialog.getByText('mock-player-ready'));

      const callsBeforeSubmit = adminContentApi.fetchAdminLessons.mock.calls.length;
      await user.click(dialog.getByRole('button', { name: L('createBtn') }));

      await waitFor(() => {
        expect(adminContentApi.createAdminLesson).toHaveBeenCalledWith(
          expect.objectContaining({
            title: 'New Lesson',
            sourceUrl: VALID_URL,
            durationMs: 120000,
          })
        );
      });

      expect(await screen.findByText(L('lessonCreateSuccess'))).toBeInTheDocument();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      await waitFor(() => {
        expect(adminContentApi.fetchAdminLessons.mock.calls.length).toBeGreaterThan(
          callsBeforeSubmit
        );
      });
    });

    it('opens edit modal with existing data and updates the lesson', async () => {
      adminContentApi.updateAdminLesson.mockResolvedValueOnce({
        ...sampleLesson,
        title: 'Renamed Lesson',
      });

      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.click(
        screen.getByRole('button', { name: `${L('editLessonBtn')} English Grammar Basics` })
      );

      const dialog = within(screen.getByRole('dialog'));
      expect(dialog.getByRole('heading', { name: L('editModalTitle') })).toBeInTheDocument();

      const titleInput = dialog.getByLabelText(`${L('lessonTitleLabel')} *`);
      expect(titleInput).toHaveValue('English Grammar Basics');

      await user.clear(titleInput);
      await user.type(titleInput, 'Renamed Lesson');
      await user.click(dialog.getByRole('button', { name: L('saveBtn') }));

      await waitFor(() => {
        expect(adminContentApi.updateAdminLesson).toHaveBeenCalledWith(
          1,
          expect.objectContaining({ title: 'Renamed Lesson' })
        );
      });
      expect(await screen.findByText(L('lessonUpdateSuccess'))).toBeInTheDocument();
    });

    it('opens status modal and archives the lesson', async () => {
      adminContentApi.archiveAdminLesson.mockResolvedValueOnce({
        ...sampleLesson,
        status: 'ARCHIVED',
      });

      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.click(
        screen.getByRole('button', { name: `${L('changeStatusBtn')} English Grammar Basics` })
      );

      // Trong dialog cũng có label "Trạng thái" nên phải giới hạn phạm vi tìm trong dialog
      const dialog = within(screen.getByRole('dialog'));
      await user.selectOptions(dialog.getByLabelText(L('statusFilter')), 'ARCHIVED');
      await user.click(dialog.getByRole('button', { name: L('saveBtn') }));

      await waitFor(() => {
        expect(adminContentApi.archiveAdminLesson).toHaveBeenCalledWith(1);
      });
      expect(await screen.findByText(L('statusUpdateSuccess'))).toBeInTheDocument();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('closes toast when its close button is clicked', async () => {
      adminContentApi.archiveAdminLesson.mockResolvedValueOnce({
        ...sampleLesson,
        status: 'ARCHIVED',
      });

      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.click(
        screen.getByRole('button', { name: `${L('changeStatusBtn')} English Grammar Basics` })
      );
      const dialog = within(screen.getByRole('dialog'));
      await user.selectOptions(dialog.getByLabelText(L('statusFilter')), 'ARCHIVED');
      await user.click(dialog.getByRole('button', { name: L('saveBtn') }));

      await screen.findByText(L('statusUpdateSuccess'));
      await user.click(screen.getByRole('button', { name: A('common.close') }));

      expect(screen.queryByText(L('statusUpdateSuccess'))).not.toBeInTheDocument();
    });

    it('navigates to lesson detail on lesson title click', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.click(screen.getByRole('link', { name: 'English Grammar Basics' }));

      expect(mockNavigate).toHaveBeenCalledWith('/admin/lessons/1');
    });

    it('navigates to lesson detail on view detail button click', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.click(
        screen.getByRole('button', { name: `${L('viewDetail')} English Grammar Basics` })
      );

      expect(mockNavigate).toHaveBeenCalledWith('/admin/lessons/1');
    });

    it('handles pagination navigation', async () => {
      adminContentApi.fetchAdminLessons.mockResolvedValue(
        pageOf([sampleLesson], { totalElements: 20, totalPages: 3, isLast: false })
      );

      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      expect(screen.getByRole('button', { name: A('common.prevPage') })).toBeDisabled();

      const nextBtn = screen.getByRole('button', { name: A('common.nextPage') });
      expect(nextBtn).toBeEnabled();
      await user.click(nextBtn);

      await waitFor(() => {
        expect(adminContentApi.fetchAdminLessons).toHaveBeenCalledWith(
          expect.objectContaining({ page: 2 })
        );
      });
    });

    it('renders error state and retries loading', async () => {
      adminContentApi.fetchAdminLessons.mockRejectedValueOnce(new Error('Network error'));

      const user = userEvent.setup();
      renderPage();

      expect(await screen.findByText('Network error')).toBeInTheDocument();
      expect(screen.queryByRole('table')).not.toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: A('feedback.retry') }));

      expect(await screen.findByText('English Grammar Basics')).toBeInTheDocument();
      expect(screen.queryByText('Network error')).not.toBeInTheDocument();
    });

    it('displays empty state when no lessons exist', async () => {
      adminContentApi.fetchAdminLessons.mockResolvedValue(pageOf([]));

      renderPage();

      expect(await screen.findByText(L('emptyDesc'))).toBeInTheDocument();
    });

    it('refreshes lesson list when refresh button is clicked', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      const callsBefore = adminContentApi.fetchAdminLessons.mock.calls.length;
      await user.click(screen.getByRole('button', { name: A('common.refresh') }));

      await waitFor(() => {
        expect(adminContentApi.fetchAdminLessons).toHaveBeenCalledTimes(callsBefore + 1);
      });
    });

    it('displays correct status and tier badges', async () => {
      const archivedLesson = {
        ...sampleLesson,
        id: 3,
        title: 'Archived Lesson',
        status: 'ARCHIVED',
      };
      adminContentApi.fetchAdminLessons.mockResolvedValue(
        pageOf([sampleLesson, sampleDraftLesson, archivedLesson])
      );

      renderPage();
      await screen.findByText('Archived Lesson');

      // Phải tìm trong bảng: bộ lọc cũng có <option> trùng chữ (Published/Draft/Archived/Premium...)
      const table = within(screen.getByRole('table'));
      expect(table.getByText(L('statusPublished'))).toBeInTheDocument();
      expect(table.getByText(L('statusDraft'))).toBeInTheDocument();
      expect(table.getByText(L('statusArchived'))).toBeInTheDocument();
      expect(table.getByText(L('premiumTier'))).toBeInTheDocument();
      expect(table.getAllByText(L('freeTier'))).toHaveLength(2);
    });

    it('shows thumbnail placeholder when lesson has no thumbnail', async () => {
      adminContentApi.fetchAdminLessons.mockResolvedValue(pageOf([sampleDraftLesson]));

      renderPage();
      await screen.findByText('Advanced Vocabulary');

      expect(screen.getByTitle(L('noThumbnail'))).toBeInTheDocument();
      expect(screen.queryByRole('img')).not.toBeInTheDocument();
    });

    it('collapses extra tags into a +N pill', async () => {
      const manyTagsLesson = {
        ...sampleLesson,
        tags: [
          { id: 1, label: 'Grammar' },
          { id: 2, label: 'Vocabulary' },
          { id: 3, label: 'Idioms' },
          { id: 4, label: 'Slang' },
          { id: 5, label: 'Business' },
        ],
      };
      adminContentApi.fetchAdminLessons.mockResolvedValue(pageOf([manyTagsLesson]));

      renderPage();
      await waitForSampleLesson();

      const table = within(screen.getByRole('table'));
      expect(table.getByText('#Idioms')).toBeInTheDocument();
      expect(table.queryByText('#Slang')).not.toBeInTheDocument();
      expect(table.getByText('+2')).toBeInTheDocument();
    });

    it('cancels lesson form modal', async () => {
      const user = userEvent.setup();
      renderPage();
      await waitForSampleLesson();

      await user.click(screen.getByRole('button', { name: L('createLesson') }));

      const dialog = within(screen.getByRole('dialog'));
      // Tìm theo text để không khớp nhầm nút X (cùng accessible name "Hủy"/"Cancel")
      await user.click(dialog.getByText(L('cancelBtn')));

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });
});
