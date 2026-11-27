import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TransactionManagementPage from '@/modules/admin/pages/TransactionManagementPage/TransactionManagementPage';
import * as adminPremiumApi from '@/modules/admin/services/adminPremiumApi';
import { LANGS, useLang } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminPremiumApi');

const mockTransactionsPage = {
  content: [
    {
      id: 100,
      providerRefCode: 'ELGO8A9B1C2D3E',
      userId: 10,
      username: 'john_doe',
      userEmail: 'john@example.com',
      planId: 1,
      planName: 'Pro Monthly',
      providerCode: 'SEPAY',
      amount: 99000,
      currency: 'VND',
      status: 'PENDING',
      paidAt: null,
      createdAt: '2026-11-25T10:10:00',
    },
  ],
  page: 1,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  isLast: true,
};

describe('TransactionManagementPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminPremiumApi.getAllPlans.mockResolvedValue([{ id: 1, name: 'Pro Monthly' }]);
    adminPremiumApi.searchTransactions.mockResolvedValue(mockTransactionsPage);
  });

  describe.each(LANGS)('TransactionManagementPage [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders transactions table and pagination', async () => {
      render(<TransactionManagementPage onNavigate={vi.fn()} />);

      expect(await screen.findByText('ELGO8A9B1C2D3E')).toBeInTheDocument();
      expect(screen.getByText('john_doe')).toBeInTheDocument();
      expect(screen.getByText('Pro Monthly')).toBeInTheDocument();
    });

    it('resets page to 1 when applying filter', async () => {
      const user = userEvent.setup();
      render(<TransactionManagementPage onNavigate={vi.fn()} />);
      await screen.findByText('ELGO8A9B1C2D3E');

      const searchInput = screen.getByPlaceholderText(
        lng === 'vi'
          ? 'Tìm mã tham chiếu, username, email, họ tên...'
          : 'Search ref code, username, email, full name...'
      );
      await user.type(searchInput, 'ELGO');

      const applyBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tìm kiếm' : 'Search',
      });
      await user.click(applyBtn);

      await waitFor(() => {
        expect(adminPremiumApi.searchTransactions).toHaveBeenLastCalledWith(
          expect.objectContaining({
            keyword: 'ELGO',
            page: 1,
            size: 20,
          })
        );
      });
    });

    it('confirms pending transaction with mandatory reason', async () => {
      const user = userEvent.setup();
      adminPremiumApi.confirmTransaction.mockResolvedValueOnce({
        id: 100,
        status: 'SUCCESS',
        paidAt: '2026-11-25T18:45:00',
      });

      render(<TransactionManagementPage onNavigate={vi.fn()} />);
      await screen.findByText('ELGO8A9B1C2D3E');

      const confirmActionBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xác nhận thủ công' : 'Manual Confirm',
      });
      await user.click(confirmActionBtn);

      const reasonInput = screen.getByLabelText(
        new RegExp(lng === 'vi' ? 'Lý do thực hiện' : 'Administrative Reason', 'i')
      );
      await user.type(reasonInput, 'Verified bank transfer #FT01');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xác nhận thành công' : 'Confirm Payment',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminPremiumApi.confirmTransaction).toHaveBeenCalledWith(
          100,
          'Verified bank transfer #FT01'
        );
      });
    });
  });
});
