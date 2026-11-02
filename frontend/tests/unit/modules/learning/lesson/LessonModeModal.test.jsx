import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LessonModeModal from '@/modules/learning/lesson/components/LessonModeModal/LessonModeModal';

describe('LessonModeModal component', () => {
  const mockLesson = {
    id: 1,
    title: 'Lesson 1: Greetings',
    slug: 'lesson-1-greetings',
    userProgress: {
      dictationStatus: 'COMPLETED',
      dictationProgressPct: 100,
      shadowingStatus: 'IN_PROGRESS',
      shadowingProgressPct: 60,
    },
  };

  it('does not render when isOpen is false or lesson is null', () => {
    const { container: c1 } = render(
      <LessonModeModal isOpen={false} lesson={mockLesson} />
    );
    expect(c1.firstChild).toBeNull();

    const { container: c2 } = render(
      <LessonModeModal isOpen={true} lesson={null} />
    );
    expect(c2.firstChild).toBeNull();
  });

  it('renders modal with mode cards and status labels', () => {
    render(<LessonModeModal isOpen={true} lesson={mockLesson} />);

    expect(screen.getByText('Lesson 1: Greetings')).toBeInTheDocument();
    expect(screen.getByText(/100%/)).toBeInTheDocument();
    expect(screen.getByText(/60%/)).toBeInTheDocument();
  });

  it('calls onSelectMode with dictation or shadowing when clicked', async () => {
    const user = userEvent.setup();
    const onSelectMode = vi.fn();
    const onClose = vi.fn();

    render(
      <LessonModeModal
        isOpen={true}
        lesson={mockLesson}
        onSelectMode={onSelectMode}
        onClose={onClose}
      />
    );

    // Click dictation card
    const dictationCard = screen.getByText('Dictation').closest('.lesson-mode-card');
    await user.click(dictationCard);
    expect(onSelectMode).toHaveBeenCalledWith('dictation', mockLesson);

    // Keyboard on shadowing card
    const shadowingCard = screen.getByText('Shadowing').closest('.lesson-mode-card');
    shadowingCard.focus();
    await user.keyboard('{Enter}');
    expect(onSelectMode).toHaveBeenCalledWith('shadowing', mockLesson);

    await user.keyboard(' ');
    expect(onSelectMode).toHaveBeenCalledTimes(3);
  });

  it('calls onClose when close button is clicked or Escape key is pressed', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(<LessonModeModal isOpen={true} lesson={mockLesson} onClose={onClose} />);

    const closeBtn = screen.getByRole('button');
    await user.click(closeBtn);
    expect(onClose).toHaveBeenCalledTimes(1);

    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it('handles not started progress state and IN_PROGRESS with non-number pct', () => {
    const unstartedLesson = {
      id: 2,
      title: 'New lesson',
      userProgress: null,
    };
    render(<LessonModeModal isOpen={true} lesson={unstartedLesson} />);
    expect(screen.getAllByText(/chưa học/i).length).toBeGreaterThan(0);

    const inProgressWithoutPct = {
      id: 3,
      title: 'In progress lesson',
      userProgress: {
        dictationStatus: 'IN_PROGRESS',
        dictationProgressPct: null,
      },
    };
    render(<LessonModeModal isOpen={true} lesson={inProgressWithoutPct} />);
    expect(screen.getByText(/0%/)).toBeInTheDocument();
  });

  it('handles overlay click and prevents close when modal dialog is clicked', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(<LessonModeModal isOpen={true} lesson={mockLesson} onClose={onClose} />);

    // Click modal container inside overlay -> stopPropagation
    const dialog = screen.getByRole('heading', { level: 2, name: /chọn chế độ học/i }).closest('.lesson-mode-modal');
    await user.click(dialog);
    expect(onClose).not.toHaveBeenCalled();

    // Click modal overlay
    const overlay = document.querySelector('.lesson-mode-modal-overlay');
    await user.click(overlay);
    expect(onClose).toHaveBeenCalled();
  });

  it('supports keyboard navigation (Enter and Space) on dictation card', async () => {
    const user = userEvent.setup();
    const onSelectMode = vi.fn();

    render(<LessonModeModal isOpen={true} lesson={mockLesson} onSelectMode={onSelectMode} />);

    const dictationCard = screen.getByText('Dictation').closest('.lesson-mode-card');
    dictationCard.focus();
    await user.keyboard('{Enter}');
    expect(onSelectMode).toHaveBeenCalledWith('dictation', mockLesson);

    await user.keyboard(' ');
    expect(onSelectMode).toHaveBeenCalledTimes(2);

    // Other keys do nothing
    await user.keyboard('{ArrowDown}');
    expect(onSelectMode).toHaveBeenCalledTimes(2);
  });
});

