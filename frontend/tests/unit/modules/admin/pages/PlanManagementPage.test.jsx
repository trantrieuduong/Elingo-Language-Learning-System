import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PlanManagementPage from '@/modules/admin/pages/PlanManagementPage/PlanManagementPage';
import * as adminPremiumApi from '@/modules/admin/services/adminPremiumApi';
import { LANGS, useLang } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminPremiumApi');

const mockPlans = [
  {
    id: 1,
    name: 'Pro Monthly',
    price: 99000,
    currency: 'VND',
    durationDays: 30,
    isActive: true,
    createdAt: '2026-01-01T10:00:00',
    updatedAt: '2026-01-01T10:00:00',
  },
  {
    id: 2,
    name: 'Pro Yearly Legacy',
    price: 899000,
    currency: 'VND',
    durationDays: 365,
    isActive: false,
    createdAt: '2026-01-01T10:00:00',
    updatedAt: '2026-02-10T09:30:00',
  },
];

describe('PlanManagementPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminPremiumApi.getAllPlans.mockResolvedValue(mockPlans);
  });

  describe.each(LANGS)('PlanManagementPage [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders plans list and filters by status', async () => {
      const user = userEvent.setup();
      render(<PlanManagementPage onNavigate={vi.fn()} />);

      expect(await screen.findByText('Pro Monthly')).toBeInTheDocument();
      expect(screen.getByText('Pro Yearly Legacy')).toBeInTheDocument();

      const statusSelect = screen.getByRole('combobox');
      await user.selectOptions(statusSelect, 'INACTIVE');

      expect(screen.queryByText('Pro Monthly')).not.toBeInTheDocument();
      expect(screen.getByText('Pro Yearly Legacy')).toBeInTheDocument();
    });

    it('opens create plan modal and submits new plan', async () => {
      const user = userEvent.setup();
      adminPremiumApi.createPlan.mockResolvedValueOnce({
        id: 3,
        name: 'Pro Quarterly',
        price: 249000,
        currency: 'VND',
        durationDays: 90,
        isActive: false,
        updatedAt: '2026-11-25T18:30:00',
      });

      render(<PlanManagementPage onNavigate={vi.fn()} />);
      await screen.findByText('Pro Monthly');

      const createBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo gói mới' : 'Create Plan',
      });
      await user.click(createBtn);

      const dialog = screen.getByRole('dialog');
      const nameInput = within(dialog).getByLabelText(
        new RegExp(lng === 'vi' ? 'Tên gói thuê bao' : 'Plan Name', 'i')
      );
      const priceInput = within(dialog).getByLabelText(
        new RegExp(lng === 'vi' ? 'Giá cước' : 'Price', 'i')
      );
      await user.type(nameInput, 'Pro Quarterly');
      await user.type(priceInput, '249000');

      const submitBtn = within(dialog).getByRole('button', {
        name: lng === 'vi' ? 'Tạo gói' : 'Create Plan',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminPremiumApi.createPlan).toHaveBeenCalledWith({
          name: 'Pro Quarterly',
          price: 249000,
          currency: 'VND',
          durationDays: 30,
        });
      });

      expect(await screen.findByText('Pro Quarterly')).toBeInTheDocument();
    });

    it('toggles plan status via confirmation modal', async () => {
      const user = userEvent.setup();
      adminPremiumApi.enablePlan.mockResolvedValueOnce({
        ...mockPlans[1],
        isActive: true,
      });

      render(<PlanManagementPage onNavigate={vi.fn()} />);
      await screen.findByText('Pro Yearly Legacy');

      const enableBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Kích hoạt' : 'Enable',
      });
      await user.click(enableBtn);

      const confirmBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xác nhận kích hoạt' : 'Confirm Enable',
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminPremiumApi.enablePlan).toHaveBeenCalledWith(2);
      });
    });
  });
});
