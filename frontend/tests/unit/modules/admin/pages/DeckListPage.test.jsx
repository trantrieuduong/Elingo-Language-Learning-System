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
  fetchOrphanedCardStats: vi.fn(),
  fetchOrphanedCards: vi.fn(),
  cleanupOrphanedCards: vi.fn(),
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

    it('handles filter inputs for search, tag, status, tier and cefr', async () => {
      adminContentApi.fetchAdminDecks.mockResolvedValue({
        content: [sampleSystemDeck],
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

      // Type keyword
      const searchInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Tìm theo tên bộ từ hoặc mô tả...' : 'Search by deck title or description...'
      );
      await user.type(searchInput, 'Oxford');

      // Select tag
      const tagSelect = screen.getByDisplayValue(
        lng === 'vi' ? 'Tất cả chủ đề (Tag)' : 'All Tags'
      );
      await user.selectOptions(tagSelect, '1');

      // Select status
      const statusSelect = screen.getByDisplayValue(
        lng === 'vi' ? 'Tất cả trạng thái' : 'All Statuses'
      );
      await user.selectOptions(statusSelect, 'PUBLISHED');

      // Select tier
      const tierSelect = screen.getByDisplayValue(
        lng === 'vi' ? 'Tất cả phân hạng' : 'All Tiers'
      );
      await user.selectOptions(tierSelect, 'true');

      // Select CEFR
      const cefrSelect = screen.getByDisplayValue(
        lng === 'vi' ? 'Tất cả trình độ CEFR' : 'All CEFR Levels'
      );
      await user.selectOptions(cefrSelect, '2');

      await waitFor(() => {
        expect(adminContentApi.fetchAdminDecks).toHaveBeenCalledWith(
          expect.objectContaining({
            tagIds: [1],
            status: 'PUBLISHED',
            isPremium: true,
            cefrLevelIds: [2],
          })
        );
      });
    });

    it('handles quick deck status change modal', async () => {
      adminContentApi.fetchAdminDecks.mockResolvedValueOnce({
        content: [sampleSystemDeck],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      });
      adminContentApi.updateDeckStatus.mockResolvedValueOnce({
        ...sampleSystemDeck,
        status: 'ARCHIVED',
      });

      const user = userEvent.setup();
      const { container } = render(<DeckListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Oxford 3000')).toBeInTheDocument();
      });

      const statusBtn = screen.getByRole('button', {
        name: /Đổi trạng thái Oxford 3000|Change Status Oxford 3000/i,
      });
      await user.click(statusBtn);

      const select = container.querySelector('#deckStatusSelect');
      await user.selectOptions(select, 'ARCHIVED');

      const saveBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
      });
      await user.click(saveBtn);

      await waitFor(() => {
        expect(adminContentApi.updateDeckStatus).toHaveBeenCalledWith(1, 'ARCHIVED');
      });
    });

    it('navigates to deck detail on deck title or action button click', async () => {
      adminContentApi.fetchAdminDecks.mockResolvedValueOnce({
        content: [sampleSystemDeck],
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

      const titleLink = screen.getByText('Oxford 3000');
      await user.click(titleLink);

      expect(mockNavigate).toHaveBeenCalledWith('/admin/decks/1');
    });

    it('handles pagination navigation next and previous', async () => {
      adminContentApi.fetchAdminDecks.mockResolvedValueOnce({
        content: [sampleSystemDeck],
        page: 1,
        size: 20,
        totalElements: 30,
        totalPages: 2,
        isLast: false,
      });

      const user = userEvent.setup();
      render(<DeckListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Oxford 3000')).toBeInTheDocument();
      });

      const nextBtn = screen.getByRole('button', { name: /Next Page|Trang sau/i });
      expect(nextBtn).not.toBeDisabled();
      await user.click(nextBtn);

      await waitFor(() => {
        expect(adminContentApi.fetchAdminDecks).toHaveBeenCalledWith(
          expect.objectContaining({
            page: 2,
          })
        );
      });
    });

    it('renders error state and retries loading', async () => {
      adminContentApi.fetchAdminDecks.mockRejectedValueOnce(new Error('Network error'));

      const user = userEvent.setup();
      render(<DeckListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Thử lại|Retry/i })).toBeInTheDocument();
      });

      adminContentApi.fetchAdminDecks.mockResolvedValueOnce({
        content: [sampleSystemDeck],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      });

      const retryBtn = screen.getByRole('button', {
        name: /Thử lại|Retry/i,
      });
      await user.click(retryBtn);

      await waitFor(() => {
        expect(screen.getByText('Oxford 3000')).toBeInTheDocument();
      });
    });

    it('switches to Orphaned Cards tab and displays orphaned cards stats', async () => {
      adminContentApi.fetchAdminDecks.mockResolvedValueOnce({
        content: [sampleSystemDeck],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      });
      adminContentApi.fetchOrphanedCardStats.mockResolvedValueOnce({
        totalOrphanedCards: 12,
        safeToDeleteCards: 5,
        retainedInStudyCards: 7,
      });
      adminContentApi.fetchOrphanedCards.mockResolvedValueOnce({
        content: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 1,
        isLast: true,
      });

      const user = userEvent.setup();
      render(<DeckListPage onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Oxford 3000')).toBeInTheDocument();
      });

      const orphanedTabBtn = screen.getByRole('tab', {
        name: /Card mồ côi|Orphaned Cards/i,
      });
      await user.click(orphanedTabBtn);

      await waitFor(() => {
        expect(adminContentApi.fetchOrphanedCardStats).toHaveBeenCalledTimes(1);
        expect(adminContentApi.fetchOrphanedCards).toHaveBeenCalledWith({
          page: 1,
          size: 20,
          sort: 'id,desc',
        });
      });
    });
  });
});
