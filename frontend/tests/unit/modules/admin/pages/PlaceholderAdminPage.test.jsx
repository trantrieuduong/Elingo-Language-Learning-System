import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PlaceholderAdminPage from '@/modules/admin/pages/PlaceholderAdminPage/PlaceholderAdminPage';
import { useLang, LANGS } from '../../../../utils/i18n';

describe('PlaceholderAdminPage component', () => {
  const mockNavigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n and interaction [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('renders custom title when provided', () => {
      render(<PlaceholderAdminPage title="Quản lý từ vựng" onNavigate={mockNavigate} />);

      expect(screen.getByRole('heading', { level: 2, name: 'Quản lý từ vựng' })).toBeInTheDocument();
    });

    it('renders default upcoming title when title prop is missing', () => {
      render(<PlaceholderAdminPage onNavigate={mockNavigate} />);

      const defaultTitle =
        lng === 'vi'
          ? 'Mô-đun đang trong kế hoạch phát triển'
          : 'Module Under Development';
      expect(screen.getByRole('heading', { level: 2, name: defaultTitle })).toBeInTheDocument();
    });

    it('navigates back to /admin when clicking back button', async () => {
      const user = userEvent.setup();
      render(<PlaceholderAdminPage onNavigate={mockNavigate} />);

      const backBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Quay lại Bảng điều khiển' : 'Return to Dashboard',
      });
      await user.click(backBtn);

      expect(mockNavigate).toHaveBeenCalledWith('/admin');
    });
  });
});
