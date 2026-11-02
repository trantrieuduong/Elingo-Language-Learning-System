import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DictationPage from '@/modules/learning/dictation/pages/DictationPage';
import { getLessonDetailApi, getLessonSegmentsApi } from '@/modules/learning/lesson/lessonApi';
import { patchDictationProgressApi } from '@/modules/learning/dictation/dictationApi';

vi.mock('@/modules/learning/lesson/lessonApi', () => ({
  getLessonDetailApi: vi.fn(),
  getLessonSegmentsApi: vi.fn(),
}));

vi.mock('@/modules/learning/dictation/dictationApi', () => ({
  patchDictationProgressApi: vi.fn(),
}));

// Mock LessonYoutubePlayer to avoid external YT dependency in page test
vi.mock('@/modules/learning/lesson/components/LessonYoutubePlayer/LessonYoutubePlayer', () => ({
  default: ({ title }) => <div data-testid="mock-yt-player">{title}</div>,
}));

describe('DictationPage page component', () => {
  const mockLesson = {
    id: 10,
    title: 'Daily English Conversation',
    slug: 'daily-english',
    sourceUrl: 'https://youtube.com/watch?v=dQw4w9WgXcQ',
    dictationPassScore: 70,
  };

  const mockSegments = [
    {
      id: 101,
      startMs: 0,
      endMs: 2500,
      transcriptNormalized: 'hello world',
      userProgress: { dictationAttemptCount: 0, dictationBestScore: 0 },
    },
    {
      id: 102,
      startMs: 2500,
      endMs: 5000,
      transcriptNormalized: 'good morning',
      userProgress: { dictationAttemptCount: 0, dictationBestScore: 0 },
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    getLessonDetailApi.mockResolvedValue({ data: mockLesson });
    getLessonSegmentsApi.mockResolvedValue({ data: mockSegments });
  });

  it('renders loading state initially and then study view with segments', async () => {
    render(<DictationPage slug="daily-english" onNavigate={vi.fn()} />);

    expect(screen.getByText(/đang tải|loading/i)).toBeInTheDocument();

    expect(await screen.findByTestId('mock-yt-player')).toBeInTheDocument();
    expect(screen.getByText('Daily English Conversation')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });

  it('handles error state for 404 / lesson not found', async () => {
    getLessonDetailApi.mockRejectedValueOnce({
      response: { status: 404, data: { code: 'LESSON_NOT_FOUND' } },
    });

    render(<DictationPage slug="non-existent" onNavigate={vi.fn()} />);

    expect(await screen.findByText(/không tìm thấy|not found/i)).toBeInTheDocument();
  });

  it('handles premium required error state', async () => {
    getLessonDetailApi.mockRejectedValueOnce({
      response: { status: 403, data: { code: 'PREMIUM_REQUIRED' } },
    });

    render(<DictationPage slug="premium-lesson" onNavigate={vi.fn()} />);

    expect(await screen.findByText(/gói thành viên|premium/i)).toBeInTheDocument();
  });

  it('handles empty segments state', async () => {
    getLessonSegmentsApi.mockResolvedValueOnce({ data: [] });

    render(<DictationPage slug="empty-lesson" onNavigate={vi.fn()} />);

    expect(await screen.findByText(/chưa có phân đoạn|no segments/i)).toBeInTheDocument();
  });

  it('allows user to type matching words, reveal words with hint, and submit segment', async () => {
    const user = userEvent.setup();
    patchDictationProgressApi.mockResolvedValue({
      data: {
        score: 80,
        passed: true,
        segmentProgress: { dictationBestScore: 80, dictationAttemptCount: 1 },
      },
    });

    render(<DictationPage slug="daily-english" onNavigate={vi.fn()} />);

    const textarea = await screen.findByRole('textbox');
    // Type 'hello'
    await user.type(textarea, 'hello');

    // Click reveal eye button for 2nd word 'world'
    const revealBtns = screen.getAllByTitle(/gợi ý từ này|reveal/i);
    await user.click(revealBtns[0]);

    // Now allDone is true (hello = correct, world = revealed)
    const submitBtn = screen.getByRole('button', { name: /hoàn thành đoạn|complete/i });
    expect(submitBtn).not.toBeDisabled();

    await user.click(submitBtn);

    expect(patchDictationProgressApi).toHaveBeenCalledWith(10, 101, {
      attemptCount: 1,
      hintUsedCount: 1,
    });

    // Check result view rendered
    const scoreElements = await screen.findAllByText(/80/);
    expect(scoreElements.length).toBeGreaterThan(0);
  });

  it('supports retry segment and moving to next segment until lesson completion', async () => {
    const user = userEvent.setup();
    patchDictationProgressApi.mockResolvedValue({
      data: {
        score: 90,
        passed: true,
        segmentProgress: { dictationBestScore: 90, dictationAttemptCount: 1 },
      },
    });

    render(<DictationPage slug="daily-english" onNavigate={vi.fn()} />);

    const textarea = await screen.findByRole('textbox');
    // Click Show all remaining
    const showAllBtn = screen.getByRole('button', { name: /hiện tất cả|show all/i });
    await user.click(showAllBtn);

    const submitBtn = screen.getByRole('button', { name: /hoàn thành đoạn|complete/i });
    await user.click(submitBtn);

    // In result view, click retry
    const retryBtn = await screen.findByRole('button', { name: /làm lại đoạn này|retry/i });
    await user.click(retryBtn);

    // Back to textarea
    expect(screen.getByRole('textbox')).toBeInTheDocument();

    // Show all and submit again
    await user.click(screen.getByRole('button', { name: /hiện tất cả|show all/i }));
    await user.click(screen.getByRole('button', { name: /hoàn thành đoạn|complete/i }));

    // Now click Next segment
    const nextBtn = await screen.findByRole('button', { name: /tiếp theo|next/i });
    await user.click(nextBtn);

    // Second segment (index 1)
    expect(screen.getByRole('textbox')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /hiện tất cả|show all/i }));
    await user.click(screen.getByRole('button', { name: /hoàn thành đoạn|complete/i }));

    // Finish lesson button on last segment: "Hoàn thành bài"
    const finishBtn = await screen.findByRole('button', { name: /hoàn thành bài|finish/i });
    await user.click(finishBtn);

    // Lesson completion card is visible
    expect(await screen.findByText(/chúc mừng bạn đã hoàn thành|congratulations/i)).toBeInTheDocument();
  });

  it('navigates back to lessons list when back button is clicked', async () => {
    const onNavigate = vi.fn();
    const user = userEvent.setup();

    render(<DictationPage slug="daily-english" onNavigate={onNavigate} />);

    await screen.findByTestId('mock-yt-player');
    const backBtn = screen.getByRole('button', { name: /quay lại|back/i });
    await user.click(backBtn);

    expect(onNavigate).toHaveBeenCalledWith('/lessons');
  });
});
