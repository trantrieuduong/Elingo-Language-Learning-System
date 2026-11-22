import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DateRangeFilter from '@/modules/admin/components/DateRangeFilter/DateRangeFilter';
import { useLang, LANGS } from '../../../../utils/i18n';

describe('DateRangeFilter component', () => {
  const initialValue = {
    from: '2026-10-24',
    to: '2026-11-22',
    granularity: 'DAY',
  };

  const mockOnChange = vi.fn();
  const mockOnRefresh = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n and interactions [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('has "30 days" preset active by default', () => {
      render(
        <DateRangeFilter
          value={initialValue}
          onChange={mockOnChange}
          onRefresh={mockOnRefresh}
        />
      );

      const preset30Btn = screen.getByRole('button', {
        name: lng === 'vi' ? '30 ngày qua' : 'Last 30 days',
      });
      expect(preset30Btn.className).toMatch(/activePreset/);
    });

    it('changes preset and calls onChange when preset button is clicked', async () => {
      const user = userEvent.setup();
      render(
        <DateRangeFilter
          value={initialValue}
          onChange={mockOnChange}
          onRefresh={mockOnRefresh}
        />
      );

      const preset7Btn = screen.getByRole('button', {
        name: lng === 'vi' ? '7 ngày qua' : 'Last 7 days',
      });
      await user.click(preset7Btn);

      expect(mockOnChange).toHaveBeenCalledTimes(1);
      const callArg = mockOnChange.mock.calls[0][0];
      expect(callArg.granularity).toBe('DAY');
      expect(callArg.from).toBeDefined();
      expect(callArg.to).toBeDefined();
    });

    it('clears active preset when user edits input dates manually', () => {
      render(
        <DateRangeFilter
          value={initialValue}
          onChange={mockOnChange}
          onRefresh={mockOnRefresh}
        />
      );

      const fromInput = screen.getByLabelText(lng === 'vi' ? 'Từ ngày' : 'From');
      fireEvent.change(fromInput, { target: { value: '2026-01-01' } });

      const preset30Btn = screen.getByRole('button', {
        name: lng === 'vi' ? '30 ngày qua' : 'Last 30 days',
      });
      expect(preset30Btn.className).not.toMatch(/activePreset/);
    });

    it('switches granularity and triggers onChange', async () => {
      const user = userEvent.setup();
      render(
        <DateRangeFilter
          value={initialValue}
          onChange={mockOnChange}
          onRefresh={mockOnRefresh}
        />
      );

      const weeklyBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Theo tuần' : 'Weekly',
      });
      await user.click(weeklyBtn);

      expect(mockOnChange).toHaveBeenCalledWith({
        ...initialValue,
        granularity: 'WEEK',
      });
    });

    it('submits form when clicking apply button', async () => {
      const user = userEvent.setup();
      render(
        <DateRangeFilter
          value={initialValue}
          onChange={mockOnChange}
          onRefresh={mockOnRefresh}
        />
      );

      const applyBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Áp dụng' : 'Apply',
      });
      await user.click(applyBtn);

      expect(mockOnChange).toHaveBeenCalledTimes(1);
    });

    it('handles refresh button click and shows loading spin', async () => {
      const user = userEvent.setup();
      const { rerender } = render(
        <DateRangeFilter
          value={initialValue}
          onChange={vi.fn()}
          onRefresh={mockOnRefresh}
          isLoading={false}
        />
      );

      const refreshBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Làm mới dữ liệu' : 'Refresh Data',
      });

      await user.click(refreshBtn);
      expect(mockOnRefresh).toHaveBeenCalledTimes(1);

      // Rerender with loading state
      rerender(
        <DateRangeFilter
          value={initialValue}
          onChange={vi.fn()}
          onRefresh={mockOnRefresh}
          isLoading={true}
        />
      );

      expect(refreshBtn).toBeDisabled();
      const icon = refreshBtn.querySelector('svg');
      expect(icon.getAttribute('class')).toMatch(/spinning/);
    });
  });
});
