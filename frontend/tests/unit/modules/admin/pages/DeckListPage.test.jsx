import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DeckListPage from '@/modules/admin/pages/DeckManagementPage/DeckListPage';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  fetchAdminDecks: vi.fn(),
  createSystemDeck: vi.fn(),
  updateSystemDeck: vi.fn(),
  updateDeckStatus: vi.fn(),
  deleteUserDeck: vi.fn(),
  fetchAdminTags: vi.fn(),
  fetchCefrLevels: vi.fn(),
}));

describe('DeckListPage component', () => {
  const mockNavigate = vi.fn();

  const sampleSystemDeck = {
    id: 1,
    title: 'Oxford 3000',
    slug: 'oxford-3000',
    description: 'Core words',
    isPremium: false,
    status: 'PUBLISHED',
    ownerType: 'SYSTEM',
    topicCount: 5,
    cardCount: 100,
    tags: [{ id: 1, label: 'General' }],
    cefrLevels: [{ id: 2, label: 'B1' }],
  };

  const sampleUserDeck = {
    id: 99,
    title: 'Inappropriate Deck',
    slug: 'inappropriate-deck',
    description: 'User created violation',
    isPremium: false,
    status: 'PUBLISHED',
    ownerType: 'USER',
    ownerId: 888,
    topicCount: 1,
    cardCount: 10,
    tags: [],
    cefrLevels: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    adminContentApi.fetchAdminTags.mockResolvedValue([{ id: 1, label: 'General' }]);
    adminContentApi.fetchCefrLevels.mockResolvedValue([{ id: 2, label: 'B1' }]);
  });

  describe.each(LANGS)('i18n rendering and Deck filtering [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('defaults to SYSTEM decks and fetches with ownerType SYSTEM', async () => {
      adminContentApi.fetchAdminDecks.mockResolvedValueOnce({
        content: [sampleSystemDeck],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      });

      render(<DeckListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(adminContentApi.fetchAdminDecks).toHaveBeenCalledWith(
          expect.objectContaining({
            ownerType: 'SYSTEM',
          })
        );
        expect(screen.getByText('Oxford 3000')).toBeInTheDocument();
      });

      const pageHeading = lng === 'vi' ? 'Quản lý Bộ từ' : 'Vocabulary Decks Management';
      expect(screen.getByRole('heading', { level: 1, name: pageHeading })).toBeInTheDocument();
    });

    it('switches to USER ownerType filter and displays delete violation button', async () => {
      adminContentApi.fetchAdminDecks
        .mockResolvedValueOnce({
          content: [sampleSystemDeck],
          page: 1,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          isLast: true,
        })
        .mockResolvedValueOnce({
          content: [sampleUserDeck],
          page: 1,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          isLast: true,
        });

      const user = userEvent.setup();
      render(<DeckListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Oxford 3000')).toBeInTheDocument();
      });

      // Click user segment
      const userSegmentBtn = screen.getByRole('button', {
        name: /Người dùng|Học viên|Learner/i,
      });
      await user.click(userSegmentBtn);

      await waitFor(() => {
        expect(adminContentApi.fetchAdminDecks).toHaveBeenCalledWith(
          expect.objectContaining({
            ownerType: 'USER',
          })
        );
        expect(screen.getByText('Inappropriate Deck')).toBeInTheDocument();
      });

      // In USER decks, edit button must NOT be present
      expect(
        screen.queryByRole('button', {
          name: /Chỉnh sửa thông tin Inappropriate Deck|Edit Deck Inappropriate Deck/i,
        })
      ).not.toBeInTheDocument();

      // Delete violation button must be present
      const deleteViolationBtn = screen.getByRole('button', {
        name: /Xóa vi phạm chính sách Inappropriate Deck|Delete for Policy Violation Inappropriate Deck/i,
      });
      expect(deleteViolationBtn).toBeInTheDocument();
    });

    it('requires a reason to permanently delete user deck', async () => {
      adminContentApi.fetchAdminDecks.mockResolvedValueOnce({
        content: [sampleUserDeck],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      });
      adminContentApi.deleteUserDeck.mockResolvedValueOnce({ success: true });

      const user = userEvent.setup();
      render(<DeckListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Inappropriate Deck')).toBeInTheDocument();
      });

      const deleteBtn = screen.getByRole('button', {
        name: /Xóa vi phạm chính sách Inappropriate Deck|Delete for Policy Violation Inappropriate Deck/i,
      });
      await user.click(deleteBtn);

      // Modal open
      const reasonInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập lý do vi phạm (bắt buộc)...' : 'Enter reason for violation (required)...'
      );
      await user.type(reasonInput, 'Hate speech detected');

      const confirmBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xác nhận xóa vĩnh viễn' : 'Confirm Permanent Deletion',
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminContentApi.deleteUserDeck).toHaveBeenCalledWith(99, 'Hate speech detected');
      });
    });

    it('opens create system deck modal and submits payload', async () => {
      adminContentApi.fetchAdminDecks.mockResolvedValueOnce({
        content: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 1,
        isLast: true,
      });
      adminContentApi.createSystemDeck.mockResolvedValueOnce({ id: 10, title: 'TOEIC Core' });

      const user = userEvent.setup();
      render(<DeckListPage onNavigate={mockNavigate} />);

      const createBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo bộ từ hệ thống' : 'Create System Deck',
      });
      await user.click(createBtn);

      const titleInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Tên bộ từ' : 'Deck Title', 'i'));
      await user.type(titleInput, 'TOEIC Core');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo mới' : 'Create',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.createSystemDeck).toHaveBeenCalledWith(
          expect.objectContaining({
            title: 'TOEIC Core',
          })
        );
      });
    });
  });
});
