import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UserManagementPage from '@/modules/admin/pages/UserManagementPage/UserManagementPage';
import * as adminUserApi from '@/modules/admin/services/adminUserApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, role: 'ADMIN', username: 'superadmin' },
  }),
}));

vi.mock('@/modules/admin/services/adminUserApi', () => ({
  searchUsers: vi.fn(),
  getUserById: vi.fn(),
  lockUser: vi.fn(),
  unlockUser: vi.fn(),
}));

describe('UserManagementPage component', () => {
  const mockNavigate = vi.fn();

  const sampleUsers = [
    {
      id: 101,
      username: 'learner_one',
      email: 'learner1@el.io',
      firstName: 'Van A',
      lastName: 'Nguyen',
      role: 'USER',
      isActive: true,
      isVerified: true,
      avatarUrl: null,
      authProviders: ['LOCAL'],
      createdAt: '2026-11-20T10:00:00',
      updatedAt: '2026-11-20T10:00:00',
    },
    {
      id: 102,
      username: 'locked_user',
      email: 'locked@el.io',
      firstName: 'Thi B',
      lastName: 'Tran',
      role: 'USER',
      isActive: false,
      isVerified: false,
      avatarUrl: null,
      authProviders: ['GOOGLE'],
      createdAt: '2026-11-19T09:00:00',
      updatedAt: '2026-11-20T11:00:00',
    },
    {
      id: 1,
      username: 'superadmin',
      email: 'admin@el.io',
      firstName: 'Admin',
      lastName: 'Super',
      role: 'ADMIN',
      isActive: true,
      isVerified: true,
      avatarUrl: null,
      authProviders: ['LOCAL'],
      createdAt: '2026-11-01T00:00:00',
      updatedAt: '2026-11-01T00:00:00',
    },
  ];

  const samplePage1 = {
    content: sampleUsers,
    page: 1,
    size: 20,
    totalElements: 3,
    totalPages: 1,
    isLast: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    adminUserApi.searchUsers.mockResolvedValue(samplePage1);
  });

  describe.each(LANGS)('i18n and UserManagementPage functionality [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders users list with user info and badges correctly', async () => {
      render(<UserManagementPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(/learner_one/)).toBeInTheDocument();
        expect(screen.getByText('learner1@el.io')).toBeInTheDocument();
        expect(screen.getByText(/locked_user/)).toBeInTheDocument();
        expect(screen.getByText('locked@el.io')).toBeInTheDocument();
        expect(screen.getByText(/superadmin/)).toBeInTheDocument();
      });

      // Total count in pagination bar
      expect(screen.getByText('3')).toBeInTheDocument();
    });

    it('applies filters when submitting filter form', async () => {
      const user = userEvent.setup();

      render(<UserManagementPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(/learner_one/)).toBeInTheDocument();
      });

      // Type search keyword
      const searchInputs = screen.getAllByRole('textbox');
      await user.type(searchInputs[0], 'learner');

      // Click Apply / Search button
      adminUserApi.searchUsers.mockResolvedValueOnce({
        content: [sampleUsers[0]],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      });

      const submitBtns = screen.getAllByRole('button');
      const searchSubmitBtn = submitBtns.find(
        (btn) => /Tìm kiếm|Search/i.test(btn.textContent)
      );
      if (searchSubmitBtn) {
        await user.click(searchSubmitBtn);
      }

      await waitFor(() => {
        expect(adminUserApi.searchUsers).toHaveBeenCalledWith(
          expect.objectContaining({
            keyword: 'learner',
            page: 1,
          })
        );
      });
    });

    it('resets filter fields when clicking reset button', async () => {
      const user = userEvent.setup();

      render(<UserManagementPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(/learner_one/)).toBeInTheDocument();
      });

      const resetBtns = screen.getAllByRole('button');
      const resetBtn = resetBtns.find((btn) => /Đặt lại|Reset/i.test(btn.textContent));

      if (resetBtn) {
        await user.click(resetBtn);
        await waitFor(() => {
          expect(adminUserApi.searchUsers).toHaveBeenCalledWith(
            expect.objectContaining({
              page: 1,
              size: 20,
            })
          );
        });
      }
    });

    it('opens user detail modal and closes it', async () => {
      const user = userEvent.setup();

      render(<UserManagementPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(/learner_one/)).toBeInTheDocument();
      });

      // Find details button for user 101
      const detailBtns = screen.getAllByTitle(/Chi tiết|Details/i);
      await user.click(detailBtns[0]);

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(within(screen.getByRole('dialog')).getByText('learner1@el.io')).toBeInTheDocument();
      });

      // Close modal via close button in dialog
      const dialog = screen.getByRole('dialog');
      const closeBtns = within(dialog).getAllByRole('button', { name: /Đóng|Close/i });
      await user.click(closeBtns[0]);

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    it('opens lock modal, submits reason and locks user account', async () => {
      const user = userEvent.setup();

      render(<UserManagementPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(/learner_one/)).toBeInTheDocument();
      });

      // Click lock button for active user (learner_one)
      const lockBtns = screen.getAllByTitle(/Khóa tài khoản|Lock Account/i);
      await user.click(lockBtns[0]);

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      const dialog = screen.getByRole('dialog');
      // Type reason into dialog textbox
      const reasonInput = within(dialog).getByRole('textbox');
      await user.type(reasonInput, 'Spamming content in group discussions');

      const lockedResponse = { ...sampleUsers[0], isActive: false };
      adminUserApi.lockUser.mockResolvedValueOnce(lockedResponse);

      const confirmBtn = within(dialog).getByRole('button', {
        name: /Xác nhận khóa|Confirm Lock/i,
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminUserApi.lockUser).toHaveBeenCalledWith(
          101,
          'Spamming content in group discussions'
        );
      });
    });

    it('opens unlock modal and unlocks user account', async () => {
      const user = userEvent.setup();

      render(<UserManagementPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(/locked_user/)).toBeInTheDocument();
      });

      // Click unlock button for locked user
      const unlockBtns = screen.getAllByTitle(/Mở khóa tài khoản|Unlock Account/i);
      await user.click(unlockBtns[0]);

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      const dialog = screen.getByRole('dialog');
      const unlockedResponse = { ...sampleUsers[1], isActive: true };
      adminUserApi.unlockUser.mockResolvedValueOnce(unlockedResponse);

      const confirmBtn = within(dialog).getByRole('button', {
        name: /Mở khóa|Unlock/i,
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminUserApi.unlockUser).toHaveBeenCalledWith(102);
      });
    });

    it('disables lock/unlock button for current admin or admin role users', async () => {
      render(<UserManagementPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText(/superadmin/)).toBeInTheDocument();
      });

      // Superadmin row: lock button must be disabled
      const superadminRow = screen.getByText(/superadmin/).closest('tr');
      const lockBtn = within(superadminRow).getByRole('button', {
        name: /Khóa tài khoản|Lock Account/i,
      });
      expect(lockBtn).toBeDisabled();
    });

    it('displays error state on API failure and retries successfully', async () => {
      adminUserApi.searchUsers.mockRejectedValueOnce(new Error('Network error'));
      const user = userEvent.setup();

      render(<UserManagementPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Thử lại|Retry/i })).toBeInTheDocument();
      });

      const retryBtn = screen.getByRole('button', { name: /Thử lại|Retry/i });
      await user.click(retryBtn);

      await waitFor(() => {
        expect(screen.getByText(/learner_one/)).toBeInTheDocument();
      });
    });
  });
});
