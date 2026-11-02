import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LessonCard from '@/modules/learning/lesson/components/LessonCard/LessonCard';

describe('LessonCard component', () => {
  const mockLesson = {
    id: 1,
    title: 'Daily English Conversation',
    description: 'Learn simple everyday phrases.',
    durationMs: 150000, // 02:30
    sourceUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
    thumbnailUrl: 'https://example.com/custom-thumb.jpg',
    isPremium: true,
    cefrLevels: [{ id: 1, label: 'B1' }],
    tags: [{ id: 10, label: 'Speaking' }],
    userProgress: {
      dictationStatus: 'COMPLETED',
      dictationProgressPct: 100,
      shadowingStatus: 'IN_PROGRESS',
      shadowingProgressPct: 45.6,
    },
  };

  it('renders nothing when lesson is not provided', () => {
    const { container } = render(<LessonCard lesson={null} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders all lesson details, badges and progress properly', () => {
    const onClick = vi.fn();
    render(<LessonCard lesson={mockLesson} onClick={onClick} />);

    expect(screen.getByText('Daily English Conversation')).toBeInTheDocument();
    expect(screen.getByText('Learn simple everyday phrases.')).toBeInTheDocument();
    expect(screen.getByText('02:30')).toBeInTheDocument();
    expect(screen.getByText('B1')).toBeInTheDocument();
    expect(screen.getByText('Speaking')).toBeInTheDocument();
    expect(screen.getByText('check_circle')).toBeInTheDocument();
    expect(screen.getByText('46%')).toBeInTheDocument(); // rounded 45.6%

    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('src', 'https://example.com/custom-thumb.jpg');
    expect(img).toHaveAttribute('alt', 'Daily English Conversation');
  });

  it('falls back to youtube thumbnail when thumbnailUrl is missing', () => {
    const lessonWithoutThumb = {
      ...mockLesson,
      thumbnailUrl: null,
      sourceUrl: 'https://youtu.be/dQw4w9WgXcQ',
    };
    render(<LessonCard lesson={lessonWithoutThumb} />);
    const img = screen.getByRole('img');
    expect(img).toHaveAttribute(
      'src',
      'https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg'
    );
  });

  it('renders placeholder icon when both thumbnails are unavailable', () => {
    const lessonNoMedia = {
      ...mockLesson,
      thumbnailUrl: null,
      sourceUrl: null,
      isPremium: false,
      durationMs: 0,
      description: null,
    };
    render(<LessonCard lesson={lessonNoMedia} />);
    expect(screen.getByText('play_lesson')).toBeInTheDocument();
    expect(screen.queryByText('workspace_premium')).not.toBeInTheDocument();
  });

  it('triggers onClick on click or Enter/Space keyboard event', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<LessonCard lesson={mockLesson} onClick={onClick} />);

    const card = screen.getByRole('heading', { name: 'Daily English Conversation' }).closest('.lesson-card');
    await user.click(card);
    expect(onClick).toHaveBeenCalledWith(mockLesson);

    card.focus();
    await user.keyboard('{Enter}');
    expect(onClick).toHaveBeenCalledTimes(2);

    await user.keyboard(' ');
    expect(onClick).toHaveBeenCalledTimes(3);
  });
});
