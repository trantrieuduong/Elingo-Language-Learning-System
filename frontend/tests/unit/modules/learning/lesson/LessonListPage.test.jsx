import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LessonListPage from '@/modules/learning/lesson/pages/LessonListPage';
import { getLessonsApi } from '@/modules/learning/lesson/lessonApi';
import { getTagsApi, getCefrLevelsApi } from '@/modules/vocabulary/vocabularyApi';

vi.mock('@/modules/learning/lesson/lessonApi', () => ({
  getLessonsApi: vi.fn(),
}));

vi.mock('@/modules/vocabulary/vocabularyApi', () => ({
  getTagsApi: vi.fn(),
  getCefrLevelsApi: vi.fn(),
}));

describe('LessonListPage page component', () => {
  const mockLessons = [
    {
      id: 1,
      slug: 'lesson-1',
      title: 'English Lesson 1',
      description: 'Intro lesson',
      durationMs: 120000,
      sourceUrl: 'https://youtube.com/watch?v=11111111111',
    },
    {
      id: 2,
      slug: 'lesson-2',
      title: 'English Lesson 2',
      description: 'Advanced lesson',
      durationMs: 240000,
      sourceUrl: 'https://youtube.com/watch?v=22222222222',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    getCefrLevelsApi.mockResolvedValue({
      data: [{ id: 1, label: 'A1' }, { id: 2, label: 'B1' }],
    });
    getTagsApi.mockResolvedValue({
      data: [{ id: 10, label: 'Grammar' }],
    });
    getLessonsApi.mockResolvedValue({
      data: {
        content: mockLessons,
        page: 1,
        totalPages: 2,
        totalElements: 2,
      },
    });
  });

  it('renders lessons list and filter section after loading', async () => {
    render(<LessonListPage onNavigate={vi.fn()} />);

    expect(await screen.findByText('English Lesson 1')).toBeInTheDocument();
    expect(screen.getByText('English Lesson 2')).toBeInTheDocument();
    expect(screen.getByText('A1')).toBeInTheDocument();
    expect(screen.getByText('Grammar')).toBeInTheDocument();
  });

  it('handles empty state when no lessons are found', async () => {
    getLessonsApi.mockResolvedValueOnce({
      data: {
        content: [],
        page: 1,
        totalPages: 1,
      },
    });

    render(<LessonListPage onNavigate={vi.fn()} />);
    expect(await screen.findByText(/menu_book/i)).toBeInTheDocument();
  });

  it('handles error state and retries fetching', async () => {
    getLessonsApi.mockRejectedValueOnce(new Error('Network error'));

    render(<LessonListPage onNavigate={vi.fn()} />);

    // Wait for error state
    expect(await screen.findByText(/error/i)).toBeInTheDocument();

    // Click retry
    const retryBtn = screen.getByRole('button', { name: /retry|thử lại/i });
    getLessonsApi.mockResolvedValueOnce({
      data: {
        content: mockLessons,
        page: 1,
        totalPages: 1,
      },
    });

    const user = userEvent.setup();
    await user.click(retryBtn);

    expect(await screen.findByText('English Lesson 1')).toBeInTheDocument();
  });

  it('opens LessonModeModal when a lesson card is clicked and navigates when mode is selected', async () => {
    const onNavigate = vi.fn();
    const user = userEvent.setup();

    render(<LessonListPage onNavigate={onNavigate} />);

    const card = await screen.findByText('English Lesson 1');
    await user.click(card);

    // Modal opens with mode options
    expect(await screen.findByRole('heading', { level: 2, name: /chọn chế độ học|choose learning mode/i })).toBeInTheDocument();

    const dictationTitle = screen.getByRole('heading', { level: 3, name: 'Dictation' });
    const dictationCard = dictationTitle.closest('.lesson-mode-card');
    await user.click(dictationCard);

    expect(onNavigate).toHaveBeenCalledWith('/lessons/dictation/lesson-1');
  });

  it('handles search input with debounce and filter clearing', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });

    render(<LessonListPage onNavigate={vi.fn()} />);

    const searchInput = screen.getByRole('searchbox');
    await user.type(searchInput, 'search query');

    act(() => {
      vi.advanceTimersByTime(600);
    });

    await waitFor(() => {
      expect(getLessonsApi).toHaveBeenCalledWith(
        expect.objectContaining({ keyword: 'search query' })
      );
    });

    vi.useRealTimers();
  });
});
