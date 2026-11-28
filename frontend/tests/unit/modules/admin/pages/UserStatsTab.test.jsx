import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import UserStatsTab from '@/modules/admin/pages/DashboardPage/tabs/UserStatsTab';
import {
  fetchUserGrowth,
  fetchUserActivity,
  fetchUserBreakdown,
} from '@/modules/admin/services/adminStatsApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminStatsApi', () => ({
  fetchUserGrowth: vi.fn(),
  fetchUserActivity: vi.fn(),
  fetchUserBreakdown: vi.fn(),
}));

const sampleFilter = {
  from: '2026-10-30',
  to: '2026-11-28',
  granularity: 'DAY',
};

const mockGrowthData = {
  newUsers: [{ key: '2026-11-28', value: 3 }],
  cumulativeUsers: [{ key: '2026-11-28', value: 9 }],
};

const mockActivityData = {
  stickinessPercentage: 33.33,
  currentDau: 1,
  currentMau: 3,
  dauTimeSeries: [{ key: '2026-11-28', value: 1 }],
};

const mockBreakdownData = {
  totalUsers: 9,
  activeCount: 8,
  activePercentage: 88.89,
  verifiedCount: 7,
  verifiedPercentage: 77.78,
  premiumCount: 1,
  premiumPercentage: 11.11,
  levelDistribution: [
    { key: '1', value: 2 },
    { key: '2', value: 1 },
  ],
};

describe('UserStatsTab component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchUserGrowth.mockResolvedValue(mockGrowthData);
    fetchUserActivity.mockResolvedValue(mockActivityData);
  });

  describe.each(LANGS)('i18n and account status independent bars [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders two independent progress bars for active and unverified accounts without 100% stacked distortion', async () => {
      fetchUserBreakdown.mockResolvedValue(mockBreakdownData);

      render(<UserStatsTab filter={sampleFilter} reloadKey={0} />);

      const activeLabel = lng === 'vi' ? 'Hoạt động bình thường' : 'Active Accounts';
      const unverifiedLabel = lng === 'vi' ? 'Chưa xác thực email' : 'Unverified Email';

      await waitFor(() => {
        expect(screen.getByText(activeLabel)).toBeInTheDocument();
      });

      const statusHeading = screen.getByRole('heading', {
        name: lng === 'vi' ? 'Tình trạng tài khoản' : 'Account Status',
      });
      const statusCard = statusHeading.closest('div').parentElement;
      const scoped = within(statusCard);

      expect(scoped.getByText(activeLabel)).toBeInTheDocument();
      expect(scoped.getByText(unverifiedLabel)).toBeInTheDocument();
      expect(scoped.getByText('8')).toBeInTheDocument();
      expect(scoped.getByText('2')).toBeInTheDocument();
      expect(scoped.getByText('(88.9%)')).toBeInTheDocument();
      expect(scoped.getByText('(22.2%)')).toBeInTheDocument();

      const progressBars = screen.getAllByRole('progressbar');
      expect(progressBars).toHaveLength(2);

      const activeBar = screen.getByRole('progressbar', { name: activeLabel });
      const unverifiedBar = screen.getByRole('progressbar', { name: unverifiedLabel });

      expect(activeBar).toHaveAttribute('aria-valuenow', '88.9');
      expect(activeBar.firstElementChild).toHaveStyle({ width: '88.89%' });

      expect(unverifiedBar).toHaveAttribute('aria-valuenow', '22.2');
      expect(unverifiedBar.firstElementChild).toHaveStyle({ width: '22.22%' });
    });

    it('computes 100% unverified when verifiedPercentage is 0 and totalUsers > 0', async () => {
      fetchUserBreakdown.mockResolvedValue({
        ...mockBreakdownData,
        totalUsers: 10,
        activeCount: 10,
        activePercentage: 100,
        verifiedCount: 0,
        verifiedPercentage: 0,
      });

      render(<UserStatsTab filter={sampleFilter} reloadKey={0} />);

      const unverifiedLabel = lng === 'vi' ? 'Chưa xác thực email' : 'Unverified Email';

      await waitFor(() => {
        expect(screen.getByRole('progressbar', { name: unverifiedLabel })).toBeInTheDocument();
      });

      const unverifiedBar = screen.getByRole('progressbar', { name: unverifiedLabel });
      expect(unverifiedBar).toHaveAttribute('aria-valuenow', '100');
      expect(unverifiedBar.firstElementChild).toHaveStyle({ width: '100%' });
    });

    it('handles totalUsers = 0 safely without NaN or 100% unverified', async () => {
      fetchUserBreakdown.mockResolvedValue({
        totalUsers: 0,
        activeCount: 0,
        activePercentage: 0,
        verifiedCount: 0,
        verifiedPercentage: 0,
        premiumCount: 0,
        premiumPercentage: 0,
        levelDistribution: [],
      });

      render(<UserStatsTab filter={sampleFilter} reloadKey={0} />);

      const activeLabel = lng === 'vi' ? 'Hoạt động bình thường' : 'Active Accounts';
      const unverifiedLabel = lng === 'vi' ? 'Chưa xác thực email' : 'Unverified Email';

      await waitFor(() => {
        expect(screen.getByRole('progressbar', { name: activeLabel })).toBeInTheDocument();
      });

      expect(screen.getByRole('progressbar', { name: activeLabel })).toHaveAttribute(
        'aria-valuenow',
        '0',
      );
      expect(screen.getByRole('progressbar', { name: unverifiedLabel })).toHaveAttribute(
        'aria-valuenow',
        '0',
      );
    });
  });
});
