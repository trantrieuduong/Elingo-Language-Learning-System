import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TagListPage from '@/modules/admin/pages/TagManagementPage/TagListPage';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  fetchAdminTags: vi.fn(),
  createAdminTag: vi.fn(),
  updateAdminTag: vi.fn(),
  deleteAdminTag: vi.fn(),
}));

describe('TagListPage component', () => {
  const mockNavigate = vi.fn();
  const sampleTags = [
    { id: 1, label: 'Grammar' },
    { id: 2, label: 'Travel & Tourism' },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n rendering and Tag management [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders tags list correctly', async () => {
      adminContentApi.fetchAdminTags.mockResolvedValueOnce(sampleTags);

      render(<TagListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Grammar')).toBeInTheDocument();
        expect(screen.getByText('Travel & Tourism')).toBeInTheDocument();
      });

      const title = lng === 'vi' ? 'Quản lý chủ đề (Tag)' : 'Tag Management';
      expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument();
    });

    it('filters tags by search keyword', async () => {
      adminContentApi.fetchAdminTags.mockResolvedValueOnce(sampleTags);
      const user = userEvent.setup();

      render(<TagListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Grammar')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Tìm kiếm theo tên thẻ...' : 'Search by tag name...'
      );
      await user.type(searchInput, 'Travel');

      expect(screen.queryByText('Grammar')).not.toBeInTheDocument();
      expect(screen.getByText('Travel & Tourism')).toBeInTheDocument();
    });

    it('creates a new tag successfully', async () => {
      adminContentApi.fetchAdminTags.mockResolvedValueOnce(sampleTags);
      adminContentApi.createAdminTag.mockResolvedValueOnce({ id: 3, label: 'Science' });
      const user = userEvent.setup();

      render(<TagListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Grammar')).toBeInTheDocument();
      });

      const createBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo chủ đề (Tag) mới' : 'Create New Tag',
      });
      await user.click(createBtn);

      const labelInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập tên thẻ chủ đề...' : 'Enter tag label...'
      );
      await user.type(labelInput, 'Science');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo mới' : 'Create',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.createAdminTag).toHaveBeenCalledWith({ label: 'Science' });
        expect(screen.getByText('Science')).toBeInTheDocument();
      });
    });

    it('edits an existing tag', async () => {
      adminContentApi.fetchAdminTags.mockResolvedValueOnce(sampleTags);
      adminContentApi.updateAdminTag.mockResolvedValueOnce({ id: 1, label: 'Advanced Grammar' });
      const user = userEvent.setup();

      render(<TagListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Grammar')).toBeInTheDocument();
      });

      const editBtn = screen.getByRole('button', {
        name: /Sửa thẻ Grammar|Edit Tag Grammar/i,
      });
      await user.click(editBtn);

      const labelInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập tên thẻ chủ đề...' : 'Enter tag label...'
      );
      await user.clear(labelInput);
      await user.type(labelInput, 'Advanced Grammar');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.updateAdminTag).toHaveBeenCalledWith(1, { label: 'Advanced Grammar' });
        expect(screen.getByText('Advanced Grammar')).toBeInTheDocument();
      });
    });

    it('deletes a tag when confirmed', async () => {
      adminContentApi.fetchAdminTags.mockResolvedValueOnce(sampleTags);
      adminContentApi.deleteAdminTag.mockResolvedValueOnce({ success: true });
      const user = userEvent.setup();

      render(<TagListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Grammar')).toBeInTheDocument();
      });

      const deleteBtns = screen.getAllByRole('button', {
        name: /Xóa thẻ Grammar|Delete Tag Grammar/i,
      });
      await user.click(deleteBtns[0]);

      // Confirm in modal - exactly matches modal action button
      const confirmBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xóa thẻ' : 'Delete Tag',
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminContentApi.deleteAdminTag).toHaveBeenCalledWith(1);
        expect(screen.queryByText('Grammar')).not.toBeInTheDocument();
      });
    });

    it('displays error message when tag deletion fails', async () => {
      adminContentApi.fetchAdminTags.mockResolvedValueOnce(sampleTags);
      adminContentApi.deleteAdminTag.mockRejectedValueOnce(new Error('Tag is in use'));
      const user = userEvent.setup();

      render(<TagListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Grammar')).toBeInTheDocument();
      });

      const deleteBtns = screen.getAllByRole('button', {
        name: /Xóa thẻ Grammar|Delete Tag Grammar/i,
      });
      await user.click(deleteBtns[0]);

      const confirmBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xóa thẻ' : 'Delete Tag',
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(
          screen.getByText(
            lng === 'vi'
              ? /Không thể xóa/i
              : /Cannot delete tag/i
          )
        ).toBeInTheDocument();
      });
    });
  });
});
