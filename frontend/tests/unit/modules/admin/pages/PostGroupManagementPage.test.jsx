import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostGroupManagementPage from '@/modules/admin/pages/PostGroupManagementPage/PostGroupManagementPage';
import * as adminCommunityApi from '@/modules/admin/services/adminCommunityApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/services/apiClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock('@/modules/admin/services/adminCommunityApi', () => ({
  searchPostGroups: vi.fn(),
  createPostGroup: vi.fn(),
  updatePostGroup: vi.fn(),
  deletePostGroup: vi.fn(),
}));

describe('PostGroupManagementPage component', () => {
  const sampleGroups = {
    content: [
      {
        id: 1,
        groupName: 'English Grammar',
        description: 'Grammar discussions',
        postCount: 0,
        createdAt: '2026-03-01T08:00:00',
        updatedAt: '2026-03-02T09:00:00',
      },
    ],
    page: 1,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    isLast: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    adminCommunityApi.searchPostGroups.mockResolvedValue(sampleGroups);
    adminCommunityApi.createPostGroup.mockResolvedValue({
      id: 2,
      groupName: 'IELTS Writing',
      description: null,
      postCount: 0,
    });
    adminCommunityApi.updatePostGroup.mockResolvedValue({
      id: 1,
      groupName: 'English Grammar Updated',
      description: 'Grammar discussions',
      postCount: 0,
    });
    adminCommunityApi.deletePostGroup.mockResolvedValue(undefined);
  });

  describe.each(LANGS)('PostGroupManagementPage rendering and CRUD [%s]', (lng) => {
    beforeEach(() => {
      useLang(lng);
    });

    it('loads study groups and renders table row', async () => {
      render(<PostGroupManagementPage />);

      await waitFor(() => {
        expect(adminCommunityApi.searchPostGroups).toHaveBeenCalledWith(
          expect.objectContaining({ page: 1, size: 20, sort: 'createdAt,desc' })
        );
      });

      expect(await screen.findByText('English Grammar')).toBeInTheDocument();
      expect(screen.getByText('Grammar discussions')).toBeInTheDocument();
    });

    it('validates required groupName and creates a new group', async () => {
      const user = userEvent.setup();
      render(<PostGroupManagementPage />);

      await screen.findByText('English Grammar');
      const createBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo nhóm mới' : 'New Group',
      });
      await user.click(createBtn);

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo nhóm' : 'Create Group',
      });
      await user.click(submitBtn);

      expect(
        await screen.findByText(
          lng === 'vi' ? 'Vui lòng nhập tên nhóm học tập' : 'Please enter a group name'
        )
      ).toBeInTheDocument();
      expect(adminCommunityApi.createPostGroup).not.toHaveBeenCalled();

      const dialog = screen.getByRole('dialog');
      const nameInput = within(dialog).getByLabelText(
        new RegExp(lng === 'vi' ? 'Tên nhóm' : 'Group Name', 'i')
      );
      await user.type(nameInput, 'IELTS Writing');
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminCommunityApi.createPostGroup).toHaveBeenCalledWith({
          groupName: 'IELTS Writing',
          description: '',
        });
      });
    });

    it('opens edit modal and updates existing group', async () => {
      const user = userEvent.setup();
      render(<PostGroupManagementPage />);

      await screen.findByText('English Grammar');
      const editBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Chỉnh sửa' : 'Edit',
      });
      await user.click(editBtn);

      const dialog = screen.getByRole('dialog');
      const nameInput = within(dialog).getByLabelText(
        new RegExp(lng === 'vi' ? 'Tên nhóm' : 'Group Name', 'i')
      );
      await user.clear(nameInput);
      await user.type(nameInput, 'English Grammar Updated');

      const saveBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
      });
      await user.click(saveBtn);

      await waitFor(() => {
        expect(adminCommunityApi.updatePostGroup).toHaveBeenCalledWith(1, {
          groupName: 'English Grammar Updated',
          description: 'Grammar discussions',
        });
      });
    });

    it('opens delete modal and deletes empty group', async () => {
      const user = userEvent.setup();
      render(<PostGroupManagementPage />);

      await screen.findByText('English Grammar');
      const deleteBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xóa' : 'Delete',
      });
      await user.click(deleteBtn);

      const confirmBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xác nhận xóa' : 'Delete Group',
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminCommunityApi.deletePostGroup).toHaveBeenCalledWith(1);
      });
    });
  });
});
