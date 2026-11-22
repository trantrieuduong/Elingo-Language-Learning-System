import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MetricCard from '@/modules/admin/components/MetricCard/MetricCard';
import { UsersIcon } from '@/modules/admin/components/icons/AdminIcons';

describe('MetricCard component', () => {
  it('renders title, value, subvalue and badge properly', () => {
    render(
      <MetricCard
        title="Tổng người dùng"
        value="1,250"
        subvalue="120 hoạt động"
        badge="KPI"
        icon={UsersIcon}
      />
    );

    expect(screen.getByText('Tổng người dùng')).toBeInTheDocument();
    expect(screen.getByText('1,250')).toBeInTheDocument();
    expect(screen.getByText('120 hoạt động')).toBeInTheDocument();
    expect(screen.getByText('KPI')).toBeInTheDocument();
  });

  it('renders positive and negative trends correctly', () => {
    const { rerender } = render(
      <MetricCard title="Doanh thu" value="100,000" trend={15.5} trendLabel="so với tháng trước" />
    );

    expect(screen.getByText('15.5%')).toBeInTheDocument();
    expect(screen.getByText('so với tháng trước')).toBeInTheDocument();

    // Rerender with negative trend
    rerender(
      <MetricCard title="Doanh thu" value="80,000" trend={-8.2} trendLabel="so với tháng trước" />
    );

    expect(screen.getByText('8.2%')).toBeInTheDocument();
  });

  it('renders variant border classes correctly', () => {
    const { container, rerender } = render(
      <MetricCard title="Cảnh báo" value="5" variant="warning" />
    );
    const card = container.querySelector('article');
    expect(card.className).toMatch(/warning/);

    rerender(<MetricCard title="Nguy hiểm" value="12" variant="danger" />);
    expect(card.className).toMatch(/danger/);

    rerender(<MetricCard title="Thành công" value="99" variant="success" />);
    expect(card.className).toMatch(/success/);
  });

  it('handles click and keyboard interaction when clickable', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();

    const { container } = render(
      <MetricCard
        title="Báo cáo vi phạm"
        value="4"
        href="/admin/reports"
        onClick={handleClick}
        linkAriaLabel="Xem danh sách báo cáo"
      />
    );

    const card = container.querySelector('article');
    expect(card).toHaveAttribute('role', 'link');
    expect(card).toHaveAttribute('tabindex', '0');
    expect(card).toHaveAttribute('aria-label', 'Xem danh sách báo cáo');

    // Click mouse
    await user.click(card);
    expect(handleClick).toHaveBeenCalledTimes(1);

    // Keyboard Enter
    fireEvent.keyDown(card, { key: 'Enter' });
    expect(handleClick).toHaveBeenCalledTimes(2);

    // Keyboard Space
    fireEvent.keyDown(card, { key: ' ' });
    expect(handleClick).toHaveBeenCalledTimes(3);
  });
});
