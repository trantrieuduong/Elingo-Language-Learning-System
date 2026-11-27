import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SegmentList from '@/modules/admin/pages/LessonManagementPage/components/SegmentList';
import { useLang, LANGS, tr } from '../../../../utils/i18n';

// Lấy text từ file locale thay vì hard-code -> không bị lệch khi đổi copy
const L = (key, options) => tr(`admin:content.lessons.${key}`, options);

describe('SegmentList component', () => {
  const mockOnSelect = vi.fn();
  const mockOnAdd = vi.fn();
  const mockOnToggleSelectMode = vi.fn();
  const mockOnToggleChecked = vi.fn();
  const mockOnToggleAll = vi.fn();
  const mockOnRequestBulkDelete = vi.fn();

  const sampleSegments = [
    {
      id: 1,
      startMs: 0,
      endMs: 60000,
      transcriptOriginal: 'Introduction to grammar',
      translation: 'Giới thiệu về ngữ pháp',
    },
    {
      id: 2,
      startMs: 60000,
      endMs: 120000,
      transcriptOriginal: 'Parts of speech overview',
      translation: 'Tổng quan về các loại từ',
    },
    {
      id: 3,
      startMs: 120000,
      endMs: 180000,
      transcriptOriginal: 'Common grammar rules',
      translation: 'Các quy tắc ngữ pháp phổ biến',
    },
  ];

  const defaultProps = {
    segments: sampleSegments,
    selectedKey: null,
    onSelect: mockOnSelect,
    onAdd: mockOnAdd,
    selectMode: false,
    onToggleSelectMode: mockOnToggleSelectMode,
    checkedIds: new Set(),
    onToggleChecked: mockOnToggleChecked,
    onToggleAll: mockOnToggleAll,
    onRequestBulkDelete: mockOnRequestBulkDelete,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n rendering and segment list operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders segment list with all segments', () => {
      render(<SegmentList {...defaultProps} />);

      expect(screen.getByText('Introduction to grammar')).toBeInTheDocument();
      expect(screen.getByText('Parts of speech overview')).toBeInTheDocument();
      expect(screen.getByText('Common grammar rules')).toBeInTheDocument();
    });

    it('displays segment count in header', () => {
      render(<SegmentList {...defaultProps} />);

      expect(screen.getByText('3')).toBeInTheDocument();
    });

    it('highlights selected segment', () => {
      render(<SegmentList {...defaultProps} selectedKey={2} />);

      const segmentButtons = screen.getAllByRole('button');
      const selectedSegment = segmentButtons.find(
        (btn) => btn.textContent.includes('Parts of speech overview')
      );
      expect(selectedSegment).toBeDefined();
      // toHaveClass không hỗ trợ RegExp; class CSS module có thể bị hash nên so khớp bằng regex trên className
      expect(selectedSegment.className).toMatch(/segmentItemSelected/);
    });

    it('calls onSelect when segment is clicked', async () => {
      const user = userEvent.setup();
      render(<SegmentList {...defaultProps} />);

      const segment = screen.getByText('Introduction to grammar');
      await user.click(segment);

      expect(mockOnSelect).toHaveBeenCalledWith(1);
    });

    it('calls onAdd when add button is clicked', async () => {
      const user = userEvent.setup();
      render(<SegmentList {...defaultProps} />);

      const addBtn = screen.getByRole('button', {
        name: L('addSegment'),
      });
      await user.click(addBtn);

      expect(mockOnAdd).toHaveBeenCalled();
    });

    it('displays select mode button when segments exist', () => {
      render(<SegmentList {...defaultProps} />);

      const selectBtn = screen.getByRole('button', {
        name: L('selectModeBtn'),
      });
      expect(selectBtn).toBeInTheDocument();
    });

    it('toggles select mode when button is clicked', async () => {
      const user = userEvent.setup();
      render(<SegmentList {...defaultProps} />);

      const selectBtn = screen.getByRole('button', {
        name: L('selectModeBtn'),
      });
      await user.click(selectBtn);

      expect(mockOnToggleSelectMode).toHaveBeenCalled();
    });

    it('shows bulk action bar in select mode', () => {
      render(<SegmentList {...defaultProps} selectMode={true} />);

      expect(
        screen.getByText(L('selectAll'))
      ).toBeInTheDocument();
    });

    it('shows checkboxes in select mode', () => {
      render(<SegmentList {...defaultProps} selectMode={true} />);

      const checkboxes = screen.getAllByRole('checkbox');
      // 1 for select all + 3 for segments
      expect(checkboxes.length).toBe(4);
    });

    it('calls onToggleChecked when segment is clicked in select mode', async () => {
      const user = userEvent.setup();
      render(<SegmentList {...defaultProps} selectMode={true} />);

      const segment = screen.getByText('Introduction to grammar');
      await user.click(segment);

      expect(mockOnToggleChecked).toHaveBeenCalledWith(1);
    });

    it('shows selected count in select mode', () => {
      const checkedIds = new Set([1, 2]);
      render(<SegmentList {...defaultProps} selectMode={true} checkedIds={checkedIds} />);

      expect(
        screen.getByText(L('selectedCount', { count: 2 }))
      ).toBeInTheDocument();
    });

    it('enables bulk delete button when items are selected', () => {
      const checkedIds = new Set([1, 2]);
      render(<SegmentList {...defaultProps} selectMode={true} checkedIds={checkedIds} />);

      const bulkDeleteBtn = screen.getByRole('button', {
        name: L('bulkDeleteBtn'),
      });
      expect(bulkDeleteBtn).not.toBeDisabled();
    });

    it('disables bulk delete button when no items are selected', () => {
      render(<SegmentList {...defaultProps} selectMode={true} />);

      const bulkDeleteBtn = screen.getByRole('button', {
        name: L('bulkDeleteBtn'),
      });
      expect(bulkDeleteBtn).toBeDisabled();
    });

    it('calls onRequestBulkDelete when bulk delete button is clicked', async () => {
      const user = userEvent.setup();
      const checkedIds = new Set([1, 2]);
      render(<SegmentList {...defaultProps} selectMode={true} checkedIds={checkedIds} />);

      const bulkDeleteBtn = screen.getByRole('button', {
        name: L('bulkDeleteBtn'),
      });
      await user.click(bulkDeleteBtn);

      expect(mockOnRequestBulkDelete).toHaveBeenCalled();
    });

    it('calls onToggleAll when select all checkbox is clicked', async () => {
      const user = userEvent.setup();
      render(<SegmentList {...defaultProps} selectMode={true} />);

      const selectAllCheckbox = screen.getByLabelText(
        L('selectAll')
      );
      await user.click(selectAllCheckbox);

      expect(mockOnToggleAll).toHaveBeenCalled();
    });

    it('checks select all checkbox when all items are selected', () => {
      const checkedIds = new Set([1, 2, 3]);
      render(<SegmentList {...defaultProps} selectMode={true} checkedIds={checkedIds} />);

      const selectAllCheckbox = screen.getByLabelText(
        L('selectAll')
      );
      expect(selectAllCheckbox).toBeChecked();
    });

    it('shows exit select mode button when in select mode', () => {
      render(<SegmentList {...defaultProps} selectMode={true} />);

      expect(
        screen.getByRole('button', {
          name: L('exitSelectMode'),
        })
      ).toBeInTheDocument();
    });

    it('disables add button in select mode', () => {
      render(<SegmentList {...defaultProps} selectMode={true} />);

      const addButtons = screen.getAllByRole('button', {
        name: L('addSegment'),
      });
      const headerAddBtn = addButtons[0];
      expect(headerAddBtn).toBeDisabled();
    });

    it('displays empty state when no segments', () => {
      render(<SegmentList {...defaultProps} segments={[]} />);

      expect(
        screen.getByText(L('noSegmentsTitle'))
      ).toBeInTheDocument();

      expect(
        screen.getByText(
          L('noSegmentsDesc')
        )
      ).toBeInTheDocument();
    });

    it('shows add buttons (header + empty state) when no segments', async () => {
      const user = userEvent.setup();
      render(<SegmentList {...defaultProps} segments={[]} />);

      // Có 2 nút: 1 ở header, 1 trong empty state
      const addButtons = screen.getAllByRole('button', { name: L('addSegment') });
      expect(addButtons).toHaveLength(2);

      await user.click(addButtons[1]);
      expect(mockOnAdd).toHaveBeenCalledTimes(1);
    });

    it('does not show select mode button when no segments', () => {
      render(<SegmentList {...defaultProps} segments={[]} />);

      expect(
        screen.queryByRole('button', {
          name: L('selectModeBtn'),
        })
      ).not.toBeInTheDocument();
    });

    it('displays segment time ranges correctly', () => {
      render(<SegmentList {...defaultProps} />);

      expect(screen.getByText('00:00 – 01:00')).toBeInTheDocument();
      expect(screen.getByText('01:00 – 02:00')).toBeInTheDocument();
      expect(screen.getByText('02:00 – 03:00')).toBeInTheDocument();
    });

    it('displays segment translations', () => {
      render(<SegmentList {...defaultProps} />);

      expect(screen.getByText('Giới thiệu về ngữ pháp')).toBeInTheDocument();
      expect(screen.getByText('Tổng quan về các loại từ')).toBeInTheDocument();
      expect(screen.getByText('Các quy tắc ngữ pháp phổ biến')).toBeInTheDocument();
    });

    it('displays segment numbers', () => {
      render(<SegmentList {...defaultProps} />);

      expect(screen.getByText(L('segmentNumber', { index: 1 }))).toBeInTheDocument();
      expect(screen.getByText(L('segmentNumber', { index: 2 }))).toBeInTheDocument();
      expect(screen.getByText(L('segmentNumber', { index: 3 }))).toBeInTheDocument();
    });

    it('highlights checked items in select mode', () => {
      const checkedIds = new Set([1]);
      render(<SegmentList {...defaultProps} selectMode={true} checkedIds={checkedIds} />);

      const segmentButtons = screen.getAllByRole('button');
      const checkedSegment = segmentButtons.find(
        (btn) => btn.textContent.includes('Introduction to grammar')
      );
      expect(checkedSegment).toBeDefined();
      expect(checkedSegment.className).toMatch(/segmentItemChecked/);
    });

    it('renders with single segment', () => {
      const singleSegment = [sampleSegments[0]];
      render(<SegmentList {...defaultProps} segments={singleSegment} />);

      expect(screen.getByText('Introduction to grammar')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument(); // Count badge
    });

    it('handles selectedKey as new', () => {
      render(<SegmentList {...defaultProps} selectedKey="new" />);

      // No segment should be highlighted
      const segmentButtons = screen.getAllByRole('button').filter(
        (btn) => btn.className.includes('segmentItem')
      );
      const selectedSegments = segmentButtons.filter(
        (btn) => btn.className.includes('segmentItemSelected')
      );
      expect(selectedSegments.length).toBe(0);
    });
  });
});
