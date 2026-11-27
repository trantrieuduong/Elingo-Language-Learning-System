import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminFilterBar, {
  AdminFilterNumber,
  AdminFilterSearch,
  AdminFilterSelect,
} from '@/modules/admin/components/AdminFilterBar/AdminFilterBar';

const sortOptions = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'createdAt,asc', label: 'Oldest' },
];

describe('AdminFilterBar component', () => {
  it('renders child controls, date inputs, sort select, and triggers submit/reset', async () => {
    const user = userEvent.setup();
    const onKeywordChange = vi.fn();
    const onStatusChange = vi.fn();
    const onUserIdChange = vi.fn();
    const onFromChange = vi.fn();
    const onToChange = vi.fn();
    const onSortChange = vi.fn();
    const onApply = vi.fn((e) => e.preventDefault());
    const onReset = vi.fn();

    render(
      <AdminFilterBar
        fromLabel="From"
        fromValue="2026-11-01"
        onFromChange={onFromChange}
        toLabel="To"
        toValue="2026-11-20"
        onToChange={onToChange}
        sortValue="createdAt,desc"
        onSortChange={onSortChange}
        sortLabel="Sort"
        sortOptions={sortOptions}
        applyLabel="Apply"
        resetLabel="Reset"
        onApply={onApply}
        onReset={onReset}
      >
        <AdminFilterSearch
          value=""
          onChange={onKeywordChange}
          placeholder="Search keyword"
        />
        <AdminFilterSelect
          value=""
          onChange={onStatusChange}
          ariaLabel="Status"
          options={[
            { value: '', label: 'All' },
            { value: 'ACTIVE', label: 'Active' },
          ]}
        />
        <AdminFilterNumber
          value=""
          onChange={onUserIdChange}
          placeholder="User ID"
        />
      </AdminFilterBar>,
    );

    fireEvent.change(screen.getByPlaceholderText('Search keyword'), { target: { value: 'abc' } });
    expect(onKeywordChange).toHaveBeenCalledWith('abc');

    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'ACTIVE' } });
    expect(onStatusChange).toHaveBeenCalledWith('ACTIVE');

    fireEvent.change(screen.getByPlaceholderText('User ID'), { target: { value: '42' } });
    expect(onUserIdChange).toHaveBeenCalledWith('42');

    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-11-02' } });
    expect(onFromChange).toHaveBeenCalledWith('2026-11-02');

    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2026-11-25' } });
    expect(onToChange).toHaveBeenCalledWith('2026-11-25');

    fireEvent.change(screen.getByLabelText('Sort'), { target: { value: 'createdAt,asc' } });
    expect(onSortChange).toHaveBeenCalledWith('createdAt,asc');

    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(onApply).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: 'Reset' }));
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  it('renders filterError as text or banner based on errorVariant', () => {
    const baseProps = {
      fromLabel: 'From',
      fromValue: '',
      onFromChange: vi.fn(),
      toLabel: 'To',
      toValue: '',
      onToChange: vi.fn(),
      sortValue: 'createdAt,desc',
      onSortChange: vi.fn(),
      sortLabel: 'Sort',
      sortOptions,
      applyLabel: 'Apply',
      resetLabel: 'Reset',
      filterError: 'Invalid date range',
      onApply: vi.fn(),
      onReset: vi.fn(),
    };

    const { rerender } = render(
      <AdminFilterBar {...baseProps} errorVariant="text">
        <span>Filter</span>
      </AdminFilterBar>,
    );
    expect(screen.getByText('Invalid date range').tagName).toBe('P');

    rerender(
      <AdminFilterBar {...baseProps} errorVariant="banner">
        <span>Filter</span>
      </AdminFilterBar>,
    );
    expect(screen.getByText('Invalid date range').tagName).toBe('DIV');
  });
});
