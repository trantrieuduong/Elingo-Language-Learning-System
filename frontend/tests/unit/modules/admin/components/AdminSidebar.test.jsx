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

      expect(
        screen.getByRole('button', {
          name: new RegExp(lng === 'vi' ? 'Báo cáo vi phạm' : 'Violation Reports', 'i'),
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
          name: new RegExp(lng === 'vi' ? 'Bộ thẻ từ vựng' : 'Vocabulary Decks', 'i'),
        })
      ).not.toBeInTheDocument();

      // Click to open
      await user.click(contentDropdown);
      const decksSubItem = screen.getByRole('button', {
        name: new RegExp(lng === 'vi' ? 'Bộ thẻ từ vựng' : 'Vocabulary Decks', 'i'),
      });
      expect(decksSubItem).toBeInTheDocument();

      // Click sub-item navigates
      await user.click(decksSubItem);
      expect(mockNavigate).toHaveBeenCalledWith('/admin/decks');
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
