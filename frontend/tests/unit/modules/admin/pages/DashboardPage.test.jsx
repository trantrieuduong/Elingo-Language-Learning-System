import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DashboardPage from '@/modules/admin/pages/DashboardPage/DashboardPage';
import { useLang, LANGS } from '../../../../utils/i18n';

// Mock sub-tabs to isolate Dashboard container testing
vi.mock('@/modules/admin/pages/DashboardPage/tabs/UserStatsTab', () => ({
  default: ({ filter, reloadKey }) => (
    <div data-testid="user-tab" data-filter={JSON.stringify(filter)} data-reload={reloadKey}>
      User Tab Mock
    </div>
  ),
}));

vi.mock('@/modules/admin/pages/DashboardPage/tabs/RevenueStatsTab', () => ({
  default: ({ filter, reloadKey }) => (
    <div data-testid="revenue-tab" data-filter={JSON.stringify(filter)} data-reload={reloadKey}>
      Revenue Tab Mock
    </div>
  ),
}));

vi.mock('@/modules/admin/pages/DashboardPage/tabs/LearningStatsTab', () => ({
  default: ({ filter, reloadKey }) => (
    <div data-testid="learning-tab" data-filter={JSON.stringify(filter)} data-reload={reloadKey}>
      Learning Tab Mock
    </div>
  ),
}));

vi.mock('@/modules/admin/pages/DashboardPage/tabs/CommunityStatsTab', () => ({
  default: ({ filter, reloadKey, onNavigate }) => (
    <div data-testid="community-tab" data-filter={JSON.stringify(filter)} data-reload={reloadKey}>
      <button onClick={() => onNavigate && onNavigate('/admin/posts')}>Go to Posts</button>
      Community Tab Mock
    </div>
  ),
}));

describe('DashboardPage component', () => {
  const mockNavigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n, tab switching and filter propagation [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders dashboard title and description correctly', () => {
      render(<DashboardPage onNavigate={mockNavigate} />);

      const title = lng === 'vi' ? 'Bảng điều khiển thống kê' : 'Analytics Dashboard';
      expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument();
    });

    it('defaults to Users tab and displays UserStatsTab panel', () => {
      render(<DashboardPage onNavigate={mockNavigate} />);

      const usersTabBtn = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Người dùng' : 'Users',
      });
      expect(usersTabBtn).toHaveAttribute('aria-selected', 'true');
      expect(screen.getByTestId('user-tab')).toBeInTheDocument();
      expect(screen.queryByTestId('revenue-tab')).not.toBeInTheDocument();
    });

    it('switches between all 4 tabs when clicked', async () => {
      const user = userEvent.setup();
      render(<DashboardPage onNavigate={mockNavigate} />);

      // Switch to Revenue Tab
      const revenueTabBtn = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Doanh thu' : 'Revenue',
      });
      await user.click(revenueTabBtn);
      expect(revenueTabBtn).toHaveAttribute('aria-selected', 'true');
      expect(screen.getByTestId('revenue-tab')).toBeInTheDocument();
      expect(screen.queryByTestId('user-tab')).not.toBeInTheDocument();

      // Switch to Learning Tab
      const learningTabBtn = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Học tập' : 'Learning',
      });
      await user.click(learningTabBtn);
      expect(learningTabBtn).toHaveAttribute('aria-selected', 'true');
      expect(screen.getByTestId('learning-tab')).toBeInTheDocument();

      // Switch to Community Tab
      const communityTabBtn = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Cộng đồng' : 'Community',
      });
      await user.click(communityTabBtn);
      expect(communityTabBtn).toHaveAttribute('aria-selected', 'true');
      expect(screen.getByTestId('community-tab')).toBeInTheDocument();
    });

    it('passes down onNavigate to tabs correctly', async () => {
      const user = userEvent.setup();
      render(<DashboardPage onNavigate={mockNavigate} />);

      const communityTabBtn = screen.getByRole('tab', {
        name: lng === 'vi' ? 'Cộng đồng' : 'Community',
      });
      await user.click(communityTabBtn);

      const navBtn = screen.getByText('Go to Posts');
      await user.click(navBtn);
      expect(mockNavigate).toHaveBeenCalledWith('/admin/posts');
    });

    it('increments reloadKey when clicking refresh in DateRangeFilter', async () => {
      const user = userEvent.setup();
      render(<DashboardPage onNavigate={mockNavigate} />);

      const initialPanel = screen.getByTestId('user-tab');
      expect(initialPanel).toHaveAttribute('data-reload', '0');

      const refreshBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Làm mới dữ liệu' : 'Refresh Data',
      });
      await user.click(refreshBtn);

      const updatedPanel = screen.getByTestId('user-tab');
      expect(updatedPanel).toHaveAttribute('data-reload', '1');
    });
  });
});
