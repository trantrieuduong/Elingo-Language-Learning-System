import React, { useState } from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LessonFilterBar from '@/modules/admin/pages/LessonManagementPage/components/LessonFilterBar';
import { useLang, LANGS, tr } from '../../../../utils/i18n';

// Helper: lấy chuỗi i18n của namespace admin -> content.lessons.*
const L = (key) => tr(`admin:content.lessons.${key}`);

describe('LessonFilterBar component', () => {
  const mockOnKeywordChange = vi.fn();
  const mockOnStatusChange = vi.fn();
  const mockOnPremiumChange = vi.fn();
  const mockOnTagChange = vi.fn();
  const mockOnCefrChange = vi.fn();
  const mockOnApply = vi.fn();
  const mockOnReset = vi.fn();

  const defaultProps = {
    keyword: '',
    onKeywordChange: mockOnKeywordChange,
    status: '',
    onStatusChange: mockOnStatusChange,
    isPremium: '',
    onPremiumChange: mockOnPremiumChange,
    tagId: '',
    onTagChange: mockOnTagChange,
    cefrId: '',
    onCefrChange: mockOnCefrChange,
    tags: [
      { id: 1, label: 'Grammar' },
      { id: 2, label: 'Vocabulary' },
    ],
    cefrLevels: [
      { id: 1, label: 'A1' },
      { id: 2, label: 'B1' },
      { id: 3, label: 'B2' },
    ],
    onApply: mockOnApply,
    onReset: mockOnReset,
    loading: false,
  };

  // Input là controlled (value do cha giữ) nên cần wrapper có state
  // để gõ nhiều ký tự thì giá trị mới được cộng dồn.
  function StatefulFilterBar() {
    const [keyword, setKeyword] = useState('');
    return (
      <LessonFilterBar
        {...defaultProps}
        keyword={keyword}
        onKeywordChange={(value) => {
          setKeyword(value);
          mockOnKeywordChange(value);
        }}
      />
    );
  }

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n rendering and filter operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders all filter inputs', () => {
      render(<LessonFilterBar {...defaultProps} />);

      expect(screen.getByPlaceholderText(L('searchPlaceholder'))).toBeInTheDocument();
      expect(screen.getByLabelText(L('statusFilter'))).toHaveDisplayValue(L('allStatuses'));
      expect(screen.getByLabelText(L('premiumFilter'))).toHaveDisplayValue(L('allTiers'));
      expect(screen.getByLabelText(L('tagFilter'))).toHaveDisplayValue(L('allTags'));
      expect(screen.getByLabelText(L('cefrFilter'))).toHaveDisplayValue(L('allCefrLevels'));
    });

    it('calls onKeywordChange when typing in search input', async () => {
      const user = userEvent.setup();
      render(<StatefulFilterBar />);

      const searchInput = screen.getByPlaceholderText(L('searchPlaceholder'));
      await user.type(searchInput, 'grammar');

      expect(mockOnKeywordChange).toHaveBeenCalledTimes(7);
      expect(mockOnKeywordChange).toHaveBeenLastCalledWith('grammar');
      expect(searchInput).toHaveValue('grammar');
    });

    it('calls onApply when Enter key is pressed in search input', async () => {
      const user = userEvent.setup();
      render(<LessonFilterBar {...defaultProps} />);

      const searchInput = screen.getByPlaceholderText(L('searchPlaceholder'));
      await user.type(searchInput, 'test{Enter}');

      expect(mockOnApply).toHaveBeenCalledTimes(1);
    });

    it('does not call onApply for other keys', async () => {
      const user = userEvent.setup();
      render(<LessonFilterBar {...defaultProps} />);

      await user.type(screen.getByPlaceholderText(L('searchPlaceholder')), 'abc');

      expect(mockOnApply).not.toHaveBeenCalled();
    });

    it('calls onStatusChange when status select changes', async () => {
      const user = userEvent.setup();
      render(<LessonFilterBar {...defaultProps} />);

      await user.selectOptions(screen.getByLabelText(L('statusFilter')), 'PUBLISHED');

      expect(mockOnStatusChange).toHaveBeenCalledWith('PUBLISHED');
    });

    it('calls onPremiumChange when tier select changes', async () => {
      const user = userEvent.setup();
      render(<LessonFilterBar {...defaultProps} />);

      await user.selectOptions(screen.getByLabelText(L('premiumFilter')), 'true');

      expect(mockOnPremiumChange).toHaveBeenCalledWith('true');
    });

    it('calls onTagChange when tag select changes', async () => {
      const user = userEvent.setup();
      render(<LessonFilterBar {...defaultProps} />);

      await user.selectOptions(screen.getByLabelText(L('tagFilter')), '1');

      expect(mockOnTagChange).toHaveBeenCalledWith('1');
    });

    it('calls onCefrChange when CEFR select changes', async () => {
      const user = userEvent.setup();
      render(<LessonFilterBar {...defaultProps} />);

      await user.selectOptions(screen.getByLabelText(L('cefrFilter')), '2');

      expect(mockOnCefrChange).toHaveBeenCalledWith('2');
    });

    it('calls onReset when reset button is clicked', async () => {
      const user = userEvent.setup();
      render(<LessonFilterBar {...defaultProps} />);

      await user.click(screen.getByRole('button', { name: L('clearFilters') }));

      expect(mockOnReset).toHaveBeenCalledTimes(1);
    });

    it('disables reset button when loading', () => {
      render(<LessonFilterBar {...defaultProps} loading={true} />);

      expect(screen.getByRole('button', { name: L('clearFilters') })).toBeDisabled();
    });

    it('renders all tag options correctly', () => {
      render(<LessonFilterBar {...defaultProps} />);

      const tagSelect = screen.getByLabelText(L('tagFilter'));
      expect(within(tagSelect).getByRole('option', { name: '#Grammar' })).toBeInTheDocument();
      expect(within(tagSelect).getByRole('option', { name: '#Vocabulary' })).toBeInTheDocument();
    });

    it('renders all CEFR level options correctly', () => {
      render(<LessonFilterBar {...defaultProps} />);

      const cefrSelect = screen.getByLabelText(L('cefrFilter'));
      ['A1', 'B1', 'B2'].forEach((label) => {
        expect(within(cefrSelect).getByRole('option', { name: label })).toBeInTheDocument();
      });
    });

    it('renders all status options correctly', () => {
      render(<LessonFilterBar {...defaultProps} />);

      const statusSelect = screen.getByLabelText(L('statusFilter'));
      [L('statusDraft'), L('statusPublished'), L('statusArchived')].forEach((label) => {
        expect(within(statusSelect).getByRole('option', { name: label })).toBeInTheDocument();
      });
    });

    it('renders tier options correctly', () => {
      render(<LessonFilterBar {...defaultProps} />);

      const tierSelect = screen.getByLabelText(L('premiumFilter'));
      expect(within(tierSelect).getByRole('option', { name: L('freeTier') })).toBeInTheDocument();
      expect(within(tierSelect).getByRole('option', { name: L('premiumTier') })).toBeInTheDocument();
    });

    it('handles empty tags array gracefully', () => {
      render(<LessonFilterBar {...defaultProps} tags={[]} />);

      const tagSelect = screen.getByLabelText(L('tagFilter'));
      expect(within(tagSelect).getAllByRole('option')).toHaveLength(1);
      expect(tagSelect).toHaveDisplayValue(L('allTags'));
    });

    it('handles empty CEFR levels array gracefully', () => {
      render(<LessonFilterBar {...defaultProps} cefrLevels={[]} />);

      const cefrSelect = screen.getByLabelText(L('cefrFilter'));
      expect(within(cefrSelect).getAllByRole('option')).toHaveLength(1);
      expect(cefrSelect).toHaveDisplayValue(L('allCefrLevels'));
    });

    it('displays current filter values correctly', () => {
      render(
        <LessonFilterBar
          {...defaultProps}
          keyword="test keyword"
          status="PUBLISHED"
          isPremium="true"
          tagId="1"
          cefrId="2"
        />
      );

      expect(screen.getByPlaceholderText(L('searchPlaceholder'))).toHaveValue('test keyword');
      expect(screen.getByLabelText(L('statusFilter'))).toHaveValue('PUBLISHED');
      expect(screen.getByLabelText(L('premiumFilter'))).toHaveValue('true');
      expect(screen.getByLabelText(L('tagFilter'))).toHaveValue('1');
      expect(screen.getByLabelText(L('cefrFilter'))).toHaveValue('2');
    });
  });
});
