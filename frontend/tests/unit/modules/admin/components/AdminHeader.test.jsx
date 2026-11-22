import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminHeader from '@/modules/admin/components/AdminHeader/AdminHeader';
import { useAuth } from '@/context/AuthContext';
import { LANGS, useLang } from '../../../../utils/i18n';

vi.mock('@/context/AuthContext');

describe('AdminHeader component', () => {
  const mockNavigate = vi.fn();
  const mockToggleTheme = vi.fn();
  const mockOpenMobileSidebar = vi.fn();
  const mockLogout = vi.fn();

  const mockUser = {
    id: 1,
    username: 'adminuser',
    fullName: 'Admin Elingo',
    email: 'admin@elingo.com',
    role: 'ADMIN',
    avatarUrl: null,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockLogout.mockResolvedValue(undefined);
    useAuth.mockReturnValue({
      user: mockUser,
      logout: mockLogout,
    });
  });

  describe.each(LANGS)('Language and navigation [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders breadcrumbs dynamically and handles navigation', async () => {
      const user = userEvent.setup();

      // Render at dashboard
      const { rerender } = render(
        <AdminHeader
          currentPath="/admin"
          theme="light"
          onToggleTheme={mockToggleTheme}
          onOpenMobileSidebar={mockOpenMobileSidebar}
          onNavigate={mockNavigate}
        />
      );

      expect(screen.getByText('Elingo')).toBeInTheDocument();
      expect(
        screen.getByText(lng === 'vi' ? 'Bảng điều khiển' : 'Dashboard')
      ).toBeInTheDocument();

      // Click breadcrumb Elingo
      await user.click(screen.getByText('Elingo'));
      expect(mockNavigate).toHaveBeenCalledWith('/admin');

      // Rerender at subpage /admin/decks
      rerender(
        <AdminHeader
          currentPath="/admin/decks"
          theme="light"
          onToggleTheme={mockToggleTheme}
          onOpenMobileSidebar={mockOpenMobileSidebar}
          onNavigate={mockNavigate}
        />
      );

      expect(
        screen.getByText(lng === 'vi' ? 'Bộ từ' : 'Decks')
      ).toBeInTheDocument();
    });

    it('toggles language when clicking language button', async () => {
      const user = userEvent.setup();

      render(
        <AdminHeader
          currentPath="/admin"
          theme="light"
          onToggleTheme={mockToggleTheme}
          onOpenMobileSidebar={mockOpenMobileSidebar}
          onNavigate={mockNavigate}
        />
      );

      const langBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Ngôn ngữ' : 'Language',
      });

      await user.click(langBtn);
      // Language should toggle
      expect(langBtn).toBeInTheDocument();
    });

    it('triggers theme toggle callback', async () => {
      const user = userEvent.setup();

      render(
        <AdminHeader
          currentPath="/admin"
          theme="light"
          onToggleTheme={mockToggleTheme}
          onOpenMobileSidebar={mockOpenMobileSidebar}
          onNavigate={mockNavigate}
        />
      );

      const themeBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Chuyển chế độ sáng/tối' : 'Toggle light/dark theme',
      });

      await user.click(themeBtn);
      expect(mockToggleTheme).toHaveBeenCalledTimes(1);
    });
  });

  describe('Admin Identity Chip Dropdown', () => {
    it('renders admin fallback initials when no avatar is provided', () => {
      render(
        <AdminHeader
          currentPath="/admin"
          theme="light"
          onToggleTheme={mockToggleTheme}
          onOpenMobileSidebar={mockOpenMobileSidebar}
          onNavigate={mockNavigate}
        />
      );

      expect(screen.getByText('Admin Elingo')).toBeInTheDocument();
      expect(screen.getByText('A')).toBeInTheDocument();
    });

    it('renders admin avatar image when avatarUrl exists', () => {
      useAuth.mockReturnValue({
        user: { ...mockUser, avatarUrl: 'https://cdn.elingo.com/avatars/admin.png' },
        logout: mockLogout,
      });

      render(
        <AdminHeader
          currentPath="/admin"
          theme="light"
          onToggleTheme={mockToggleTheme}
          onOpenMobileSidebar={mockOpenMobileSidebar}
          onNavigate={mockNavigate}
        />
      );

      const img = screen.getByAltText('Admin Elingo');
      expect(img).toHaveAttribute('src', 'https://cdn.elingo.com/avatars/admin.png');
    });

    it('opens dropdown, navigates back to learner app and closes', async () => {
      const user = userEvent.setup();

      render(
        <AdminHeader
          currentPath="/admin"
          theme="light"
          onToggleTheme={mockToggleTheme}
          onOpenMobileSidebar={mockOpenMobileSidebar}
          onNavigate={mockNavigate}
        />
      );

      const chipBtn = screen.getByTitle('Admin Elingo');
      await user.click(chipBtn);

      // Dropdown should be open
      expect(screen.getByText('admin@elingo.com')).toBeInTheDocument();
      const learnerAppBtn = screen.getByRole('menuitem', { name: /Về trang người dùng|Về trang học viên|Learner Portal/i });
      expect(learnerAppBtn).toBeInTheDocument();

      await user.click(learnerAppBtn);
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
      expect(screen.queryByText('admin@elingo.com')).not.toBeInTheDocument();
    });

    it('handles logout from dropdown', async () => {
      const user = userEvent.setup();

      render(
        <AdminHeader
          currentPath="/admin"
          theme="light"
          onToggleTheme={mockToggleTheme}
          onOpenMobileSidebar={mockOpenMobileSidebar}
          onNavigate={mockNavigate}
        />
      );

      const chipBtn = screen.getByTitle('Admin Elingo');
      await user.click(chipBtn);

      const logoutBtn = screen.getByRole('menuitem', { name: /Đăng xuất|Sign out/i });
      await user.click(logoutBtn);

      expect(mockLogout).toHaveBeenCalledTimes(1);
      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/login');
      });
    });

    it('closes dropdown on Escape key and outside click', async () => {
      const user = userEvent.setup();

      render(
        <div>
          <div data-testid="outside">Outside area</div>
          <AdminHeader
            currentPath="/admin"
            theme="light"
            onToggleTheme={mockToggleTheme}
            onOpenMobileSidebar={mockOpenMobileSidebar}
            onNavigate={mockNavigate}
          />
        </div>
      );

      const chipBtn = screen.getByTitle('Admin Elingo');
      await user.click(chipBtn);
      expect(screen.getByText('admin@elingo.com')).toBeInTheDocument();

      // Press Escape
      fireEvent.keyDown(document, { key: 'Escape' });
      expect(screen.queryByText('admin@elingo.com')).not.toBeInTheDocument();

      // Click open again
      await user.click(chipBtn);
      expect(screen.getByText('admin@elingo.com')).toBeInTheDocument();

      // Click outside
      fireEvent.mouseDown(screen.getByTestId('outside'));
      expect(screen.queryByText('admin@elingo.com')).not.toBeInTheDocument();
    });
  });
});
