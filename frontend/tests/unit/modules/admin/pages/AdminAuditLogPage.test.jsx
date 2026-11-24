import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminAuditLogPage from '@/modules/admin/pages/AdminAuditLogPage/AdminAuditLogPage';
import * as adminAuditApi from '@/modules/admin/services/adminAuditApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminAuditApi', () => ({
  fetchAuditLogs: vi.fn(),
  fetchAuditActions: vi.fn(),
  fetchAuditTargetTypes: vi.fn(),
}));

describe('AdminAuditLogPage component', () => {
  const mockNavigate = vi.fn();

  const sampleActions = ['CREATE', 'UPDATE', 'DELETE', 'LOGIN'];
  const sampleTargetTypes = ['USER', 'SYSTEM_CONFIG', 'PAYMENT'];

  const samplePage1 = {
    content: [
      {
        id: 101,
        adminId: 99,
        action: 'UPDATE',
        targetType: 'SYSTEM_CONFIG',
        targetId: 'app.premium.bank-transfer.code-prefix',
        detail: '{"oldValue":"ELGO","newValue":"ELGO_V2"}',
        ipAddress: '192.168.1.10',
        createdAt: '2026-11-20T10:30:00',
      },
      {
        id: 102,
        adminId: 42,
        action: 'DELETE',
        targetType: 'USER',
        targetId: '888',
        detail: 'Deleted inactive test user account',
        ipAddress: '192.168.1.20',
        createdAt: '2026-11-20T11:15:00',
      },
    ],
    page: 1,
    size: 20,
    totalElements: 25,
    totalPages: 2,
    isLast: false,
  };

  const samplePage2 = {
    content: [
      {
        id: 103,
        adminId: 99,
        action: 'CREATE',
        targetType: 'SYSTEM_CONFIG',
        targetId: 'app.gamification.rules.daily-streak.bonus',
        detail: '{"value":"50"}',
        ipAddress: '192.168.1.30',
        createdAt: '2026-11-19T09:00:00',
      },
    ],
    page: 2,
    size: 20,
    totalElements: 25,
    totalPages: 2,
    isLast: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    adminAuditApi.fetchAuditActions.mockResolvedValue(sampleActions);
    adminAuditApi.fetchAuditTargetTypes.mockResolvedValue(sampleTargetTypes);
  });

  describe.each(LANGS)('i18n and AdminAuditLogPage functionality [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders audit logs list with headers and data rows correctly', async () => {
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage1);

      render(<AdminAuditLogPage onNavigate={mockNavigate} />);

      const pageTitle = lng === 'vi' ? 'Nhật ký hoạt động Quản trị' : 'Admin Activity Log';
      expect(screen.getByRole('heading', { level: 1, name: pageTitle })).toBeInTheDocument();

      await waitFor(() => {
        expect(screen.getByText('192.168.1.10')).toBeInTheDocument();
        expect(screen.getByText('192.168.1.20')).toBeInTheDocument();
      });

      // Check admin labels and details
      expect(screen.getByText(/Admin #\s*99/)).toBeInTheDocument();
      expect(screen.getByText(/Admin #\s*42/)).toBeInTheDocument();
      expect(screen.getByText('Deleted inactive test user account')).toBeInTheDocument();

      // Check pagination info
      expect(screen.getByText('25')).toBeInTheDocument();
      expect(screen.getByText(/1\/2/)).toBeInTheDocument();
    });

    it('applies filters when submitting filter form', async () => {
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage1);
      const user = userEvent.setup();

      render(<AdminAuditLogPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('192.168.1.10')).toBeInTheDocument();
      });

      // Select action filter
      const actionSelect = screen.getByLabelText(
        lng === 'vi' ? 'Hành động' : 'Action'
      );
      await user.selectOptions(actionSelect, 'UPDATE');

      // Select target type filter
      const targetTypeSelect = screen.getByLabelText(
        lng === 'vi' ? 'Loại đối tượng' : 'Target Type'
      );
      await user.selectOptions(targetTypeSelect, 'SYSTEM_CONFIG');

      // Enter admin ID
      const adminIdInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập Admin ID...' : 'Enter Admin ID...'
      );
      await user.type(adminIdInput, '99');

      // Enter target ID
      const targetIdInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập Target ID...' : 'Enter Target ID...'
      );
      await user.type(targetIdInput, 'app.premium');

      // Submit filter
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage1);
      const applyBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Áp dụng' : 'Apply',
      });
      await user.click(applyBtn);

      await waitFor(() => {
        expect(adminAuditApi.fetchAuditLogs).toHaveBeenCalledWith(
          expect.objectContaining({
            page: 1,
            size: 20,
            action: 'UPDATE',
            targetType: 'SYSTEM_CONFIG',
            adminId: 99,
            targetId: 'app.premium',
          })
        );
      });
    });

    it('resets filter fields when clicking reset button', async () => {
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage1);
      const user = userEvent.setup();

      render(<AdminAuditLogPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('192.168.1.10')).toBeInTheDocument();
      });

      // Type some filter values
      const adminIdInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập Admin ID...' : 'Enter Admin ID...'
      );
      await user.type(adminIdInput, '99');
      expect(adminIdInput.value).toBe('99');

      // Click reset
      const resetBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Đặt lại' : 'Reset',
      });
      await user.click(resetBtn);

      expect(adminIdInput.value).toBe('');
    });

    it('navigates through pages using pagination controls', async () => {
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage1);
      const user = userEvent.setup();

      render(<AdminAuditLogPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('192.168.1.10')).toBeInTheDocument();
      });

      // Click next page
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage2);
      const nextBtn = screen.getByRole('button', { name: /Next Page|Trang sau/i });
      await user.click(nextBtn);

      await waitFor(() => {
        expect(adminAuditApi.fetchAuditLogs).toHaveBeenCalledWith(
          expect.objectContaining({
            page: 2,
            size: 20,
          })
        );
        expect(screen.getByText('192.168.1.30')).toBeInTheDocument();
      });

      // Click previous page
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage1);
      const prevBtn = screen.getByRole('button', { name: /Previous Page|Trang trước/i });
      await user.click(prevBtn);

      await waitFor(() => {
        expect(adminAuditApi.fetchAuditLogs).toHaveBeenCalledWith(
          expect.objectContaining({
            page: 1,
            size: 20,
          })
        );
        expect(screen.getByText('192.168.1.10')).toBeInTheDocument();
      });
    });

    it('opens detail modal and closes it', async () => {
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage1);
      const user = userEvent.setup();

      render(<AdminAuditLogPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('192.168.1.10')).toBeInTheDocument();
      });

      // Click first view detail button
      const detailBtns = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Chi tiết' : 'Details',
      });
      await user.click(detailBtns[0]);

      // Modal dialog should be visible
      const modal = await screen.findByRole('dialog');
      expect(modal).toBeInTheDocument();

      const modalTitle = lng === 'vi' ? 'Chi tiết nhật ký hoạt động' : 'Activity Log Detail';
      expect(within(modal).getByRole('heading', { level: 2, name: modalTitle })).toBeInTheDocument();
      expect(within(modal).getByText('#101')).toBeInTheDocument();
      expect(within(modal).getByText('{"oldValue":"ELGO","newValue":"ELGO_V2"}')).toBeInTheDocument();

      // Close modal
      const closeBtns = within(modal).getAllByRole('button', {
        name: lng === 'vi' ? 'Đóng' : 'Close',
      });
      await user.click(closeBtns[0]);

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    it('displays error state on API failure and retries successfully', async () => {
      adminAuditApi.fetchAuditLogs.mockRejectedValueOnce(new Error('Audit logs fetch failed'));
      const user = userEvent.setup();

      render(<AdminAuditLogPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Audit logs fetch failed')).toBeInTheDocument();
      });

      // Retry
      adminAuditApi.fetchAuditLogs.mockResolvedValueOnce(samplePage1);
      const retryBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thử lại' : 'Retry',
      });
      await user.click(retryBtn);

      await waitFor(() => {
        expect(screen.getByText('192.168.1.10')).toBeInTheDocument();
      });
    });
  });
});
