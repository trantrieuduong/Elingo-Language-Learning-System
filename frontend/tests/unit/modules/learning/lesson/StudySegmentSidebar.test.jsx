import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import StudySegmentSidebar from '@/modules/learning/lesson/components/StudySegmentSidebar/StudySegmentSidebar';

describe('StudySegmentSidebar component', () => {
  const mockSegments = [
    {
      id: 1,
      startMs: 0,
      endMs: 3000,
      transcriptNormalized: 'hello world',
      userProgress: {
        dictationAttemptCount: 1,
        dictationBestScore: 85,
        shadowingAttemptCount: 1,
        shadowingBestScore: 50,
      },
    },
    {
      id: 2,
      startMs: 3000,
      endMs: 6000,
      transcriptNormalized: 'how are you',
      userProgress: {
        dictationAttemptCount: 0,
        dictationBestScore: 0,
        shadowingAttemptCount: 0,
        shadowingBestScore: 0,
      },
    },
    {
      id: 3,
      startMs: 6000,
      endMs: 9000,
      transcriptNormalized: 'nice to meet you',
      userProgress: null,
    },
  ];

  it('renders nothing when segments is empty or null', () => {
    const { container: c1 } = render(<StudySegmentSidebar segments={[]} />);
    expect(c1.firstChild).toBeNull();

    const { container: c2 } = render(<StudySegmentSidebar segments={null} />);
    expect(c2.firstChild).toBeNull();
  });

  it('renders progress percentage and segments list in dictation mode', () => {
    render(
      <StudySegmentSidebar
        segments={mockSegments}
        currentIndex={0}
        mode="dictation"
        passScore={60}
      />
    );

    // 1 of 3 completed -> 33%
    expect(screen.getByText('33%')).toBeInTheDocument();
    expect(screen.getByText('01')).toBeInTheDocument();
    expect(screen.getByText('02')).toBeInTheDocument();
    expect(screen.getByText('03')).toBeInTheDocument();
    expect(screen.getByText('85')).toBeInTheDocument();
  });

  it('renders correctly in shadowing mode with custom passScore', () => {
    render(
      <StudySegmentSidebar
        segments={mockSegments}
        currentIndex={1}
        mode="shadowing"
        passScore={60}
      />
    );

    expect(screen.getByText('50')).toBeInTheDocument();
  });

  it('allows clicking unlocked segments but prevents locked segments', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();

    render(
      <StudySegmentSidebar
        segments={mockSegments}
        currentIndex={0}
        mode="dictation"
        onSelect={onSelect}
      />
    );

    // Segment 1 (attempted) and Segment 2 (first incomplete) are selectable
    const segmentButtons = screen.getAllByRole('button');
    await user.click(segmentButtons[1]); // Segment 2
    expect(onSelect).toHaveBeenCalledWith(1);

    // Segment 3 is locked (index > firstIncompleteIdx)
    await user.click(segmentButtons[2]); // Segment 3
    expect(onSelect).toHaveBeenCalledTimes(1); // Not called for locked segment
  });
});
