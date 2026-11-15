import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PillRadioGroup from '@/modules/battle/components/PillRadioGroup/PillRadioGroup';

describe('PillRadioGroup component', () => {
  const options = [
    { key: 'ALL', labelKey: 'filter.all' },
    { key: 'WIN', labelKey: 'filter.win' },
    { key: 'LOSE', labelKey: 'filter.lose' },
  ];
  const t = (key) => `t(${key})`;

  it('renders the label and a radio for each option', () => {
    render(<PillRadioGroup label="Result" options={options} value="ALL" onChange={vi.fn()} t={t} />);

    expect(screen.getByText('Result:')).toBeInTheDocument();
    expect(screen.getByRole('radiogroup')).toBeInTheDocument();
    expect(screen.getAllByRole('radio')).toHaveLength(3);
    expect(screen.getByRole('radio', { name: 't(filter.win)' })).toBeInTheDocument();
  });

  it('marks only the selected option as checked and active', () => {
    render(<PillRadioGroup label="Result" options={options} value="WIN" onChange={vi.fn()} t={t} />);

    const win = screen.getByRole('radio', { name: 't(filter.win)' });
    const all = screen.getByRole('radio', { name: 't(filter.all)' });
    expect(win).toHaveAttribute('aria-checked', 'true');
    expect(win).toHaveClass('battle-filter-pill--active');
    expect(all).toHaveAttribute('aria-checked', 'false');
    expect(all).not.toHaveClass('battle-filter-pill--active');
  });

  it('calls onChange with the option key when clicked', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<PillRadioGroup label="Result" options={options} value="ALL" onChange={onChange} t={t} />);

    await user.click(screen.getByRole('radio', { name: 't(filter.lose)' }));

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith('LOSE');
  });

  it('renders no radio when there are no options', () => {
    render(<PillRadioGroup label="Result" options={[]} value="ALL" onChange={vi.fn()} t={t} />);

    expect(screen.queryAllByRole('radio')).toHaveLength(0);
  });
});
