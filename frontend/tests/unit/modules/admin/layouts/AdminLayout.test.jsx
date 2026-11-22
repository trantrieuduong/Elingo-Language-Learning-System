import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminLayout from '@/modules/admin/layouts/AdminLayout';
import { useAuth } from '@/context/AuthContext';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/context/AuthContext');

describe('AdminLayout component', () => {
  const mockNavigate = vi.fn();
  const mockUser = {
    id: 1,
    username: 'adminuser',
    fullName: 'Admin System',
    role: 'ADMIN',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuth.mockReturnValue({
      user: mockUser,
      logout: vi.fn(),
    });
  });

  describe.each(LANGS)('Layout behavior and responsiveness [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders children inside layout body', () => {
      render(
        <AdminLayout currentPath="/admin" onNavigate={mockNavigate}>
          <div data-testid="test-child">Child Content</div>
        </AdminLayout>
      );

      expect(screen.getByTestId('test-child')).toBeInTheDocument();
      expect(screen.getByText('Child Content')).toBeInTheDocument();
    });

    it('toggles theme and persists to localStorage', async () => {
      const user = userEvent.setup();
      const { container } = render(
        <AdminLayout currentPath="/admin" onNavigate={mockNavigate}>
          <div>Child Content</div>
        </AdminLayout>
      );

      const scopeEl = container.querySelector('.admin-scope');
      expect(scopeEl).toHaveAttribute('data-theme', 'light');

      // Click theme toggle button in header
      const themeBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Chuyển chế độ sáng/tối' : 'Toggle light/dark theme',
      });
      await user.click(themeBtn);

      expect(scopeEl).toHaveAttribute('data-theme', 'dark');
      expect(localStorage.getItem('admin_theme')).toBe('dark');

      // Click again switches back to light
      await user.click(themeBtn);
      expect(scopeEl).toHaveAttribute('data-theme', 'light');
      expect(localStorage.getItem('admin_theme')).toBe('light');
    });

    it('toggles sidebar collapse state and persists to localStorage', async () => {
      const user = userEvent.setup();
      render(
        <AdminLayout currentPath="/admin" onNavigate={mockNavigate}>
          <div>Child Content</div>
        </AdminLayout>
      );

      const collapseBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thu gọn menu' : 'Collapse sidebar',
      });
      await user.click(collapseBtn);

      expect(localStorage.getItem('admin_sidebar_collapsed')).toBe('true');

      // When collapsed, the button aria-label becomes expand
      const expandBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Mở rộng menu' : 'Expand sidebar',
      });
      await user.click(expandBtn);

      expect(localStorage.getItem('admin_sidebar_collapsed')).toBe('false');
    });
  });
});
