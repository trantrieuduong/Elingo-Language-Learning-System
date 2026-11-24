import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ShadowingPage from '@/modules/learning/shadowing/pages/ShadowingPage';
import { getLessonDetailApi, getLessonSegmentsApi } from '@/modules/learning/lesson/lessonApi';
import { patchShadowingProgressApi } from '@/modules/learning/shadowing/shadowingApi';
import { uploadFileForVerification } from '@/services/uploadFile';
import { useWhisper } from '@/modules/learning/shadowing/whisper/useWhisper';
import { AudioRecorder } from '@/modules/learning/shadowing/utils/audioRecorder';
import { MODEL_OPTIONS } from '@/modules/learning/shadowing/whisper/modelConfig';

vi.mock('@/modules/learning/lesson/lessonApi', () => ({
  getLessonDetailApi: vi.fn(),
  getLessonSegmentsApi: vi.fn(),
}));

vi.mock('@/modules/learning/shadowing/shadowingApi', () => ({
  patchShadowingProgressApi: vi.fn(),
}));

vi.mock('@/services/uploadFile', () => ({
  uploadFileForVerification: vi.fn(),
}));

vi.mock('@/modules/learning/shadowing/whisper/useWhisper', () => ({
  useWhisper: vi.fn(),
}));

vi.mock('@/modules/learning/shadowing/utils/audioRecorder', () => ({
  AudioRecorder: vi.fn(),
}));

vi.mock('@/modules/learning/lesson/components/LessonYoutubePlayer/LessonYoutubePlayer', () => ({
  default: ({ title }) => <div data-testid="mock-yt-player">{title}</div>,
}));

describe('ShadowingPage page component', () => {
  const mockLesson = {
    id: 20,
    title: 'Shadowing Practice Lesson',
    slug: 'shadowing-lesson',
    sourceUrl: 'https://youtube.com/watch?v=dQw4w9WgXcQ',
    shadowingPassScore: 60,
  };

  const mockSegments = [
    {
      id: 201,
      startMs: 0,
      endMs: 3000,
      transcriptNormalized: 'hello world',
      transcriptOriginal: 'Hello world!',
      userProgress: { shadowingAttemptCount: 0, shadowingBestScore: 0 },
    },
    {
      id: 202,
      startMs: 3000,
      endMs: 6000,
      transcriptNormalized: 'how are you',
      transcriptOriginal: 'How are you?',
      userProgress: { shadowingAttemptCount: 0, shadowingBestScore: 0 },
    },
  ];

  let mockRecorderInstance;
  let mockTranscribe;
  let mockLoadModel;

  beforeEach(() => {
    vi.clearAllMocks();

    URL.createObjectURL = vi.fn().mockReturnValue('blob:mock-audio-url');
    URL.revokeObjectURL = vi.fn();

    mockTranscribe = vi.fn().mockResolvedValue('hello world');
    mockLoadModel = vi.fn();

    useWhisper.mockReturnValue({
      status: 'ready',
      downloadProgress: 100,
      error: null,
      loadModel: mockLoadModel,
      transcribe: mockTranscribe,
    });

    mockRecorderInstance = {
      start: vi.fn().mockResolvedValue(),
      stop: vi.fn().mockResolvedValue({
        audio16k: new Float32Array([0.1, 0.2, -0.1]),
        durationSec: 2,
      }),
      cleanup: vi.fn(),
    };

    AudioRecorder.mockImplementation(function () {
      return mockRecorderInstance;
    });

    getLessonDetailApi.mockResolvedValue({ data: mockLesson });
    getLessonSegmentsApi.mockResolvedValue({ data: mockSegments });
    uploadFileForVerification.mockResolvedValue('verified-audio-key-123');
  });

  afterEach(() => {
    // Dọn giả lập thiết bị mobile (nếu test nào đó đã thêm)
    delete window.navigator.userAgentData;
    localStorage.removeItem('el_whisper_model');
    localStorage.removeItem('el_whisper_agreed');
    localStorage.removeItem('el_whisper_loaded');
  });

  it('renders loading state initially and then study card', async () => {
    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);

    expect(screen.getByText(/đang tải|loading/i)).toBeInTheDocument();

    expect(await screen.findByTestId('mock-yt-player')).toBeInTheDocument();
    expect(screen.getByText('Hello world!')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /bấm để nói|record/i })).toBeInTheDocument();
  });

  it('handles 404 error state', async () => {
    getLessonDetailApi.mockRejectedValueOnce({
      response: { status: 404, data: { code: 'LESSON_NOT_FOUND' } },
    });

    render(<ShadowingPage slug="not-found" onNavigate={vi.fn()} />);

    expect(await screen.findByText(/không tìm thấy|not found/i)).toBeInTheDocument();
  });

  it('handles empty segments state', async () => {
    getLessonSegmentsApi.mockResolvedValueOnce({ data: [] });

    render(<ShadowingPage slug="empty-lesson" onNavigate={vi.fn()} />);

    expect(await screen.findByText(/chưa có phân đoạn|no segments/i)).toBeInTheDocument();
  });

  it('records audio, runs whisper, uploads file, and submits shadowing score', async () => {
    const user = userEvent.setup();
    patchShadowingProgressApi.mockResolvedValue({
      data: {
        score: 100,
        passed: true,
        segmentProgress: { shadowingBestScore: 100, shadowingAttemptCount: 1 },
      },
    });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);

    await screen.findByTestId('mock-yt-player');

    // Click Record button to start: "Bấm để nói"
    const recordBtn = screen.getByRole('button', { name: /bấm để nói|record/i });
    await user.click(recordBtn);

    expect(mockRecorderInstance.start).toHaveBeenCalled();

    // Click Stop button to finish and evaluate
    const stopBtn = await screen.findByRole('button', { name: /đang ghi|stop/i });
    await user.click(stopBtn);

    expect(mockRecorderInstance.stop).toHaveBeenCalled();
    await waitFor(() => {
      expect(mockTranscribe).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(uploadFileForVerification).toHaveBeenCalled();
      expect(patchShadowingProgressApi).toHaveBeenCalledWith(
        20,
        201,
        expect.objectContaining({
          attemptCount: 1,
          score: 100,
          audioUrl: 'verified-audio-key-123',
        })
      );
    });

    // Score is displayed
    const scores = await screen.findAllByText(/100/);
    expect(scores.length).toBeGreaterThan(0);
  });

  it('moves between segments and shows lesson completed card', async () => {
    const user = userEvent.setup();

    // Mock segment 1 as already completed
    const completedSegments = [
      {
        ...mockSegments[0],
        userProgress: { shadowingAttemptCount: 1, shadowingBestScore: 90 },
      },
      {
        ...mockSegments[1],
        userProgress: { shadowingAttemptCount: 1, shadowingBestScore: 85 },
      },
    ];
    getLessonSegmentsApi.mockResolvedValue({ data: completedSegments });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);

    await screen.findByTestId('mock-yt-player');

    // Click Đoạn tiếp theo button to go to segment 2
    const nextBtn = screen.getByRole('button', { name: /đoạn tiếp theo|next/i });
    await user.click(nextBtn);

    // Now on segment 2 (last segment), button changes to "Hoàn thành bài"
    const finishBtn = screen.getByRole('button', { name: /hoàn thành bài|finish/i });
    await user.click(finishBtn);

    // Completed card appears
    expect(await screen.findByText(/chúc mừng bạn đã hoàn thành|congratulations/i)).toBeInTheDocument();
  });

  it('navigates back to /lessons on back button click', async () => {
    const onNavigate = vi.fn();
    const user = userEvent.setup();

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={onNavigate} />);

    await screen.findByTestId('mock-yt-player');
    const backBtn = screen.getByRole('button', { name: /quay lại|back/i });
    await user.click(backBtn);

    expect(onNavigate).toHaveBeenCalledWith('/lessons');
  });

  it('auto-loads the standard (base) model on PC without showing any model modal', async () => {
    useWhisper.mockReturnValue({
      status: 'idle',
      downloadProgress: 0,
      error: null,
      loadModel: mockLoadModel,
      transcribe: mockTranscribe,
    });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    // Tự tải ngay khi vào trang, không cần người dùng bấm gì
    await waitFor(() => {
      expect(mockLoadModel).toHaveBeenCalledWith(
        expect.objectContaining({ modelId: MODEL_OPTIONS.STANDARD, modelType: 'standard' })
      );
    });

    // Không còn modal hỏi / chọn model
    expect(screen.queryByRole('heading', { level: 2, name: /tải mô hình|download model/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
  });

  it('auto-loads the light (tiny) model on mobile devices', async () => {
    Object.defineProperty(window.navigator, 'userAgentData', {
      value: { mobile: true },
      configurable: true,
    });
    useWhisper.mockReturnValue({
      status: 'idle',
      downloadProgress: 0,
      error: null,
      loadModel: mockLoadModel,
      transcribe: mockTranscribe,
    });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    await waitFor(() => {
      expect(mockLoadModel).toHaveBeenCalledWith(
        expect.objectContaining({ modelId: MODEL_OPTIONS.LIGHT, modelType: 'light' })
      );
    });
    expect(mockLoadModel).not.toHaveBeenCalledWith(
      expect.objectContaining({ modelType: 'standard' })
    );
  });

  it('ignores legacy localStorage model choice and still picks the model by device', async () => {
    // Dữ liệu cũ do bản có modal để lại: người dùng từng chọn light trên PC
    localStorage.setItem('el_whisper_model', 'light');
    localStorage.setItem('el_whisper_agreed', 'true');
    useWhisper.mockReturnValue({
      status: 'idle',
      downloadProgress: 0,
      error: null,
      loadModel: mockLoadModel,
      transcribe: mockTranscribe,
    });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    await waitFor(() => {
      expect(mockLoadModel).toHaveBeenCalledWith(
        expect.objectContaining({ modelType: 'standard' })
      );
    });
    expect(mockLoadModel).not.toHaveBeenCalledWith(
      expect.objectContaining({ modelType: 'light' })
    );
  });

  it('retries loading the model when user clicks record while whisper is not ready', async () => {
    const user = userEvent.setup();
    localStorage.removeItem('el_whisper_loaded');
    useWhisper.mockReturnValue({
      status: 'idle',
      downloadProgress: 0,
      error: null,
      loadModel: mockLoadModel,
      transcribe: mockTranscribe,
    });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');
    await waitFor(() => expect(mockLoadModel).toHaveBeenCalled());
    mockLoadModel.mockClear();

    const recordBtn = screen.getByRole('button', { name: /bấm để nói|record/i });
    await user.click(recordBtn);

    // Bấm ghi âm khi model chưa sẵn sàng: thử tải lại, báo đang chuẩn bị, chưa ghi âm
    expect(mockLoadModel).toHaveBeenCalledTimes(1);
    expect(mockLoadModel).toHaveBeenCalledWith(
      expect.objectContaining({ modelType: 'standard' })
    );
    expect(await screen.findByText(/đang chuẩn bị mô hình/i)).toBeInTheDocument();
    expect(mockRecorderInstance.start).not.toHaveBeenCalled();

    // Không có modal nào xuất hiện
    expect(screen.queryByRole('heading', { level: 2, name: /tải mô hình|download model/i })).not.toBeInTheDocument();
  });

  it('triggers replay when replay button is clicked', async () => {
    const user = userEvent.setup();
    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);

    await screen.findByTestId('mock-yt-player');
    const replayBtn = screen.getByRole('button', { name: /nghe lại đoạn này|replay/i });
    await user.click(replayBtn);

    expect(replayBtn).toBeInTheDocument();
  });

  it('shows warning when recorded audio is too short (< 0.5s)', async () => {
    const user = userEvent.setup();
    mockRecorderInstance.stop.mockResolvedValueOnce({
      audio16k: new Float32Array([0.1]),
      durationSec: 0.2,
    });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    const recordBtn = screen.getByRole('button', { name: /bấm để nói|record/i });
    await user.click(recordBtn);

    const stopBtn = await screen.findByRole('button', { name: /đang ghi|stop/i });
    await user.click(stopBtn);

    expect(await screen.findByText(/chưa nghe rõ giọng đọc/i)).toBeInTheDocument();
    expect(mockTranscribe).not.toHaveBeenCalled();
  });

  it('handles whisper transcription error', async () => {
    const user = userEvent.setup();
    mockTranscribe.mockRejectedValueOnce(new Error('Whisper recognition failed'));

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    const recordBtn = screen.getByRole('button', { name: /bấm để nói|record/i });
    await user.click(recordBtn);

    const stopBtn = await screen.findByRole('button', { name: /đang ghi|stop/i });
    await user.click(stopBtn);

    expect(await screen.findByText('Whisper recognition failed')).toBeInTheDocument();
  });

  it('handles backend submission failure and resets file key on FILE_NOT_FOUND', async () => {
    const user = userEvent.setup();
    patchShadowingProgressApi.mockRejectedValueOnce({
      response: {
        data: {
          code: 'FILE_NOT_FOUND',
          message: 'Tập tin âm thanh không tìm thấy',
        },
      },
    });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    const recordBtn = screen.getByRole('button', { name: /bấm để nói|record/i });
    await user.click(recordBtn);

    const stopBtn = await screen.findByRole('button', { name: /đang ghi|stop/i });
    await user.click(stopBtn);

    expect(await screen.findByText('Tập tin âm thanh không tìm thấy')).toBeInTheDocument();
  });

  it('renders historical segment with penalty notice, audio element, and word accuracy breakdown', async () => {
    const historicalSegments = [
      {
        id: 201,
        startMs: 0,
        endMs: 3000,
        transcriptNormalized: 'hello world',
        transcriptOriginal: 'Hello world!',
        userProgress: {
          shadowingAttemptCount: 3,
          shadowingBestScore: 75,
          shadowingLatestAudioUrl: 'https://example.com/audio.wav',
          wordsAccuracy: [
            { word: 'hello', accuracy: 95 },
            { word: 'world', accuracy: 60 },
          ],
        },
      },
    ];

    getLessonSegmentsApi.mockResolvedValueOnce({ data: historicalSegments });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    // Best score is shown (multiple elements: in sidebar and in score display)
    const scores = await screen.findAllByText('75');
    expect(scores.length).toBeGreaterThan(0);

    // Word chips are rendered with accuracy.
    // Historical result is loaded in a useEffect after the segments are fetched,
    // so it must be awaited (findBy*) instead of read synchronously (getBy*).
    expect(await screen.findByText('Hello')).toBeInTheDocument();
    expect(screen.getByText('world!')).toBeInTheDocument();

    // Legend items
    expect(screen.getAllByText(/chính xác/i).length).toBeGreaterThan(0);
  });

  it('navigates to dictation mode from the completed card', async () => {
    const onNavigate = vi.fn();
    const user = userEvent.setup();

    const singleCompletedSegment = [
      {
        id: 201,
        startMs: 0,
        endMs: 3000,
        transcriptNormalized: 'hello world',
        transcriptOriginal: 'Hello world!',
        userProgress: {
          shadowingAttemptCount: 1,
          shadowingBestScore: 100,
        },
      },
    ];

    getLessonSegmentsApi.mockResolvedValueOnce({ data: singleCompletedSegment });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={onNavigate} />);
    await screen.findByTestId('mock-yt-player');

    // Since it's the last segment and already attempted, wait for finish button
    const finishBtn = await screen.findByRole('button', { name: /hoàn thành bài|finish/i });
    expect(finishBtn).toBeEnabled();
    await user.click(finishBtn);

    // Completion card is rendered
    expect(await screen.findByText(/chúc mừng bạn đã hoàn thành|congratulations/i)).toBeInTheDocument();

    // Click "Luyện Dictation"
    const dictationBtn = screen.getByRole('button', { name: /dictation/i });
    await user.click(dictationBtn);

    expect(onNavigate).toHaveBeenCalledWith('/lessons/dictation/shadowing-lesson');
  });

  it('displays whisper error message when whisper fails to load', async () => {
    let mockWhisperState = {
      status: 'idle',
      downloadProgress: 0,
      error: null,
      loadModel: mockLoadModel,
      transcribe: mockTranscribe,
    };

    useWhisper.mockImplementation(() => mockWhisperState);

    const { rerender } = render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    // Update whisper status to error
    mockWhisperState = {
      ...mockWhisperState,
      status: 'error',
      error: { message: 'Mô hình tải quá 120s' },
    };

    rerender(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);

    expect(await screen.findByText(/hết thời gian tải|120s/i)).toBeInTheDocument();
  });

  it('navigates to another segment when clicked in sidebar if unlocked', async () => {
    const user = userEvent.setup();
    const unlockedSegments = [
      {
        ...mockSegments[0],
        userProgress: { shadowingAttemptCount: 1, shadowingBestScore: 80 },
      },
      {
        ...mockSegments[1],
        userProgress: { shadowingAttemptCount: 0, shadowingBestScore: 0 },
      },
    ];

    getLessonSegmentsApi.mockResolvedValueOnce({ data: unlockedSegments });

    render(<ShadowingPage slug="shadowing-lesson" onNavigate={vi.fn()} />);
    await screen.findByTestId('mock-yt-player');

    // Click segment 1 in sidebar
    const segmentButtons = screen.getAllByRole('button');
    const segment01Btn = segmentButtons.find((btn) => btn.textContent.includes('01'));
    if (segment01Btn) {
      await user.click(segment01Btn);
    }

    const matches = screen.getAllByText('Hello world!');
    expect(matches.length).toBeGreaterThan(0);
  });
});
