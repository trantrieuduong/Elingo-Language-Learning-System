import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminSidebar from '@/modules/admin/components/AdminSidebar/AdminSidebar';
import { useLang, LANGS } from '../../../../utils/i18n';

describe('AdminSidebar component', () => {
  const mockNavigate = vi.fn();
  const mockToggleCollapse = vi.fn();
  const mockExpand = vi.fn();
  const mockCloseMobile = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('Sidebar rendering and navigation [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders all backend modules in the navigation', () => {
      render(
        <AdminSidebar
          currentPath="/admin"
          onNavigate={mockNavigate}
          isCollapsed={false}
          onToggleCollapse={mockToggleCollapse}
          onExpand={mockExpand}
          isMobileOpen={false}
          onCloseMobile={mockCloseMobile}
        />
      );

      // Verify essential sections
      expect(
        screen.getByRole('button', {
          name: lng === 'vi' ? 'Bảng điều khiển' : 'Dashboard',
        })
      ).toBeInTheDocument();

      expect(
        screen.getByRole('button', {
          name: lng === 'vi' ? 'Nội dung và học tập' : 'Content & Learning',
        })
      ).toBeInTheDocument();

      expect(
        screen.getByRole('button', {
          name: lng === 'vi' ? 'Premium và thanh toán' : 'Premium & Billing',
        })
      ).toBeInTheDocument();

      expect(
        screen.getByRole('button', {
          name: lng === 'vi' ? 'Bài viết và cộng đồng' : 'Community & Posts',
        })
      ).toBeInTheDocument();
    });

    it('highlights active item matching currentPath', () => {
      render(
        <AdminSidebar
          currentPath="/admin"
          onNavigate={mockNavigate}
          isCollapsed={false}
          onToggleCollapse={mockToggleCollapse}
          onExpand={mockExpand}
          isMobileOpen={false}
          onCloseMobile={mockCloseMobile}
        />
      );

      const dashboardBtn = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Bảng điều khiển' : 'Dashboard', 'i'),
      });
      expect(dashboardBtn.className).toMatch(/active/);
    });

    it('toggles dropdown menu items when expanded', async () => {
      const user = userEvent.setup();

      render(
        <AdminSidebar
          currentPath="/admin"
          onNavigate={mockNavigate}
          isCollapsed={false}
          onToggleCollapse={mockToggleCollapse}
          onExpand={mockExpand}
          isMobileOpen={false}
          onCloseMobile={mockCloseMobile}
        />
      );

      const contentDropdown = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Nội dung và học tập' : 'Content & Learning', 'i'),
      });

      // Initially closed
      expect(
        screen.queryByRole('button', {
          name: new RegExp(lng === 'vi' ? 'Bộ từ' : 'Decks', 'i'),
        })
      ).not.toBeInTheDocument();

      // Click to open
      await user.click(contentDropdown);
      const decksSubItem = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Bộ từ' : 'Decks', 'i'),
      });
      expect(decksSubItem).toBeInTheDocument();

      // Also verify tags sub-item
      const tagsSubItem = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Quản lý chủ đề' : 'Tag Management', 'i'),
      });
      expect(tagsSubItem).toBeInTheDocument();

      // Click sub-item navigates
      await user.click(decksSubItem);
      expect(mockNavigate).toHaveBeenCalledWith('/admin/decks');
    });

    it('toggles system settings dropdown and navigates to config and audit log sub-items', async () => {
      const user = userEvent.setup();

      render(
        <AdminSidebar
          currentPath="/admin"
          onNavigate={mockNavigate}
          isCollapsed={false}
          onToggleCollapse={mockToggleCollapse}
          onExpand={mockExpand}
          isMobileOpen={false}
          onCloseMobile={mockCloseMobile}
        />
      );

      const settingsDropdown = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Cấu hình hệ thống' : 'System Settings', 'i'),
      });

      // Initially closed
      expect(
        screen.queryByRole('button', {
          name: new RegExp(lng === 'vi' ? 'Cấu hình$' : 'Configurations', 'i'),
        })
      ).not.toBeInTheDocument();

      // Click to open settings dropdown
      await user.click(settingsDropdown);

      const configSubItem = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Cấu hình$' : 'Configurations', 'i'),
      });
      const auditLogSubItem = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Nhật ký hoạt động' : 'Activity Log', 'i'),
      });

      expect(configSubItem).toBeInTheDocument();
      expect(auditLogSubItem).toBeInTheDocument();

      // Click config sub-item
      await user.click(configSubItem);
      expect(mockNavigate).toHaveBeenCalledWith('/admin/settings/configs');

      // Click audit log sub-item
      await user.click(auditLogSubItem);
      expect(mockNavigate).toHaveBeenCalledWith('/admin/settings/audit-logs');
    });

    it('highlights settings sub-item matching currentPath', () => {
      render(
        <AdminSidebar
          currentPath="/admin/settings/configs"
          onNavigate={mockNavigate}
          isCollapsed={false}
          onToggleCollapse={mockToggleCollapse}
          onExpand={mockExpand}
          isMobileOpen={false}
          onCloseMobile={mockCloseMobile}
        />
      );

      const settingsDropdown = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Cấu hình hệ thống' : 'System Settings', 'i'),
      });
      expect(settingsDropdown.className).toMatch(/active/);
    });

    it('automatically expands sidebar when clicking dropdown in collapsed state', async () => {
      const user = userEvent.setup();

      render(
        <AdminSidebar
          currentPath="/admin"
          onNavigate={mockNavigate}
          isCollapsed={true}
          onToggleCollapse={mockToggleCollapse}
          onExpand={mockExpand}
          isMobileOpen={false}
          onCloseMobile={mockCloseMobile}
        />
      );

      const contentTrigger = screen.getByTitle(
        lng === 'vi' ? 'Nội dung và học tập' : 'Content & Learning'
      );
      await user.click(contentTrigger);

      // Triggers onExpand to open sidebar
      expect(mockExpand).toHaveBeenCalledTimes(1);
    });

    it('triggers toggleCollapse when clicking footer button', async () => {
      const user = userEvent.setup();

      render(
        <AdminSidebar
          currentPath="/admin"
          onNavigate={mockNavigate}
          isCollapsed={false}
          onToggleCollapse={mockToggleCollapse}
          onExpand={mockExpand}
          isMobileOpen={false}
          onCloseMobile={mockCloseMobile}
        />
      );

      const collapseBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thu gọn menu' : 'Collapse sidebar',
      });
      await user.click(collapseBtn);

      expect(mockToggleCollapse).toHaveBeenCalledTimes(1);
    });
  });
});
