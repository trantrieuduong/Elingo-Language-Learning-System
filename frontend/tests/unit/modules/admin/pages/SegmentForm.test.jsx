import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SegmentForm from '@/modules/admin/pages/LessonManagementPage/components/SegmentForm';
import { useLang, LANGS, tr, trRx } from '../../../../utils/i18n';

// Lấy text từ file locale thay vì hard-code -> không bị lệch khi đổi copy
const L = (key, options) => tr(`admin:content.lessons.${key}`, options);
const LRx = (key) => trRx(`admin:content.lessons.${key}`);

describe('SegmentForm component', () => {
  const mockOnSave = vi.fn();
  const mockOnDelete = vi.fn();
  const mockOnDirtyChange = vi.fn();
  const mockOnCancel = vi.fn();

  const sampleSegment = {
    id: 1,
    startMs: 0,
    endMs: 60000,
    transcriptOriginal: 'Introduction to grammar',
    translation: 'Giới thiệu về ngữ pháp',
  };

  const otherSegments = [
    {
      id: 2,
      startMs: 60000,
      endMs: 120000,
      transcriptOriginal: 'Parts of speech',
      translation: 'Các loại từ',
    },
  ];

  const defaultProps = {
    mode: null,
    segment: null,
    segments: [],
    durationMs: 600000, // 10 minutes
    defaultStartMs: 0,
    saving: false,
    error: null,
    onSave: mockOnSave,
    onDelete: mockOnDelete,
    onDirtyChange: mockOnDirtyChange,
    onCancel: mockOnCancel,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n rendering and segment form operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders hint card when mode is null', () => {
      render(<SegmentForm {...defaultProps} />);

      expect(
        screen.getByText(
          L('selectHint')
        )
      ).toBeInTheDocument();
    });

    it('renders new segment form with default values', () => {
      render(<SegmentForm {...defaultProps} mode="new" />);

      expect(
        screen.getByText(L('newSegmentTitle'))
      ).toBeInTheDocument();

      expect(screen.getByDisplayValue('00:00')).toBeInTheDocument();
      expect(screen.getByDisplayValue('00:05')).toBeInTheDocument();
    });

    it('renders edit form with segment data', () => {
      render(<SegmentForm {...defaultProps} mode="edit" segment={sampleSegment} />);

      expect(
        screen.getByText(L('editSegmentTitle'))
      ).toBeInTheDocument();

      expect(screen.getByDisplayValue('00:00')).toBeInTheDocument();
      expect(screen.getByDisplayValue('01:00')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Introduction to grammar')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Giới thiệu về ngữ pháp')).toBeInTheDocument();
    });

    it('shows delete button in edit mode', () => {
      render(<SegmentForm {...defaultProps} mode="edit" segment={sampleSegment} />);

      expect(
        screen.getByRole('button', {
          name: L('deleteSegmentBtn'),
        })
      ).toBeInTheDocument();
    });

    it('does not show delete button in new mode', () => {
      render(<SegmentForm {...defaultProps} mode="new" />);

      expect(
        screen.queryByRole('button', {
          name: L('deleteSegmentBtn'),
        })
      ).not.toBeInTheDocument();
    });

    it('calls onDirtyChange when form is modified', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const transcriptInput = screen.getByLabelText(
        LRx('transcriptLabel')
      );
      await user.type(transcriptInput, 'Test content');

      await waitFor(() => {
        expect(mockOnDirtyChange).toHaveBeenCalledWith(true);
      });
    });

    it('validates that start time must be less than end time', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const startInput = screen.getByLabelText(
        LRx('startTimeLabel')
      );
      const endInput = screen.getByLabelText(
        LRx('endTimeLabel')
      );

      await user.clear(startInput);
      await user.type(startInput, '0200');

      await user.clear(endInput);
      await user.type(endInput, '0100');

      await waitFor(() => {
        expect(
          screen.getByText(
            L('timeOrderError')
          )
        ).toBeInTheDocument();
      });
    });

    it('validates that end time does not exceed video duration', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" durationMs={300000} />); // 5 minutes

      const endInput = screen.getByLabelText(
        LRx('endTimeLabel')
      );

      await user.clear(endInput);
      await user.type(endInput, '1000'); // 10:00

      await waitFor(() => {
        expect(
          screen.getByText(
            L('timeExceedsError')
          )
        ).toBeInTheDocument();
      });
    });

    it('validates time overlap with existing segments', async () => {
      const user = userEvent.setup();
      render(
        <SegmentForm
          {...defaultProps}
          mode="new"
          segments={otherSegments}
        />
      );

      const startInput = screen.getByLabelText(
        LRx('startTimeLabel')
      );
      const endInput = screen.getByLabelText(
        LRx('endTimeLabel')
      );

      await user.clear(startInput);
      await user.type(startInput, '0100'); // 01:00

      await user.clear(endInput);
      await user.type(endInput, '0130'); // 01:30

      await waitFor(() => {
        expect(
          screen.getByText(
            L('timeOverlapError')
          )
        ).toBeInTheDocument();
      });
    });

    it('validates that transcript is required', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const translationInput = screen.getByLabelText(
        LRx('translationLabel')
      );
      await user.type(translationInput, 'Some translation');

      await waitFor(() => {
        expect(
          screen.getByText(
            L('transcriptRequired')
          )
        ).toBeInTheDocument();
      });
    });

    it('validates that translation is required', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const transcriptInput = screen.getByLabelText(
        LRx('transcriptLabel')
      );
      await user.type(transcriptInput, 'Some transcript');

      await waitFor(() => {
        expect(
          screen.getByText(
            L('translationRequired')
          )
        ).toBeInTheDocument();
      });
    });

    it('formats time input on blur', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const startInput = screen.getByLabelText(
        LRx('startTimeLabel')
      );

      await user.clear(startInput);
      await user.type(startInput, '130');
      await user.tab();

      await waitFor(() => {
        expect(startInput).toHaveValue('01:30');
      });
    });

    it('submits form with valid data', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const transcriptInput = screen.getByLabelText(
        LRx('transcriptLabel')
      );
      await user.type(transcriptInput, 'New transcript');

      const translationInput = screen.getByLabelText(
        LRx('translationLabel')
      );
      await user.type(translationInput, 'New translation');

      const saveBtn = screen.getByRole('button', {
        name: L('saveSegmentBtn'),
      });
      await user.click(saveBtn);

      await waitFor(() => {
        expect(mockOnSave).toHaveBeenCalledWith(
          expect.objectContaining({
            startMs: 0,
            endMs: 5000,
            transcriptOriginal: 'New transcript',
            translation: 'New translation',
          })
        );
      });
    });

    it('calls onDelete when delete button is clicked', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="edit" segment={sampleSegment} />);

      const deleteBtn = screen.getByRole('button', {
        name: L('deleteSegmentBtn'),
      });
      await user.click(deleteBtn);

      expect(mockOnDelete).toHaveBeenCalledWith(sampleSegment);
    });

    it('calls onCancel when cancel button is clicked', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const cancelBtn = screen.getByRole('button', {
        name: L('cancelBtn'),
      });
      await user.click(cancelBtn);

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('disables form controls when saving', () => {
      render(<SegmentForm {...defaultProps} mode="new" saving={true} />);

      const startInput = screen.getByLabelText(
        LRx('startTimeLabel')
      );
      expect(startInput).toBeDisabled();

      const saveBtn = screen.getByRole('button', {
        name: L('saveSegmentBtn'),
      });
      expect(saveBtn).toBeDisabled();
    });

    it('displays server error when provided', () => {
      render(
        <SegmentForm
          {...defaultProps}
          mode="new"
          error="Server error occurred"
        />
      );

      expect(screen.getByText('Server error occurred')).toBeInTheDocument();
    });

    it('disables save button when form is not dirty', () => {
      render(<SegmentForm {...defaultProps} mode="edit" segment={sampleSegment} />);

      const saveBtn = screen.getByRole('button', {
        name: L('saveSegmentBtn'),
      });
      expect(saveBtn).toBeDisabled();
    });

    it('disables save button when validation error exists', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const transcriptInput = screen.getByLabelText(
        LRx('transcriptLabel')
      );
      await user.type(transcriptInput, 'Test');

      // Translation is missing, should have validation error
      const saveBtn = screen.getByRole('button', {
        name: L('saveSegmentBtn'),
      });
      expect(saveBtn).toBeDisabled();
    });

    it('respects maxLength for transcript and translation', () => {
      render(<SegmentForm {...defaultProps} mode="new" />);

      const transcriptInput = screen.getByLabelText(
        LRx('transcriptLabel')
      );
      expect(transcriptInput).toHaveAttribute('maxlength', '2000');

      const translationInput = screen.getByLabelText(
        LRx('translationLabel')
      );
      expect(translationInput).toHaveAttribute('maxlength', '2000');
    });

    it('handles time input masking correctly', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const startInput = screen.getByLabelText(
        LRx('startTimeLabel')
      );

      await user.clear(startInput);
      await user.type(startInput, '1234');

      expect(startInput).toHaveValue('12:34');
    });

    it('clamps time values to duration on blur', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" durationMs={300000} />); // 5 minutes

      const endInput = screen.getByLabelText(
        LRx('endTimeLabel')
      );

      await user.clear(endInput);
      await user.type(endInput, '1000'); // 10:00
      await user.tab();

      await waitFor(() => {
        expect(endInput).toHaveValue('05:00'); // Clamped to 5 minutes
      });
    });

    it('resets form when mode changes', () => {
      const { rerender } = render(
        <SegmentForm {...defaultProps} mode="new" />
      );

      rerender(
        <SegmentForm
          {...defaultProps}
          mode="edit"
          segment={sampleSegment}
        />
      );

      expect(screen.getByDisplayValue('Introduction to grammar')).toBeInTheDocument();
    });

    it('validates invalid time format', async () => {
      const user = userEvent.setup();
      render(<SegmentForm {...defaultProps} mode="new" />);

      const startInput = screen.getByLabelText(
        LRx('startTimeLabel')
      );

      await user.clear(startInput);
      await user.type(startInput, '1299'); // 12:99 - invalid seconds

      await waitFor(() => {
        expect(
          screen.getByText(
            L('timeInvalid')
          )
        ).toBeInTheDocument();
      });
    });

    it('handles defaultStartMs for new segments', () => {
      render(
        <SegmentForm
          {...defaultProps}
          mode="new"
          defaultStartMs={120000}
        />
      );

      expect(screen.getByDisplayValue('02:00')).toBeInTheDocument();
    });
  });
});
