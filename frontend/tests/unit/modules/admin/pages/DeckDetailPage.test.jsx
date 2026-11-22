import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DeckDetailPage from '@/modules/admin/pages/DeckManagementPage/DeckDetailPage';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  fetchAdminDeckDetail: vi.fn(),
  fetchDeckTopics: vi.fn(),
  updateSystemDeck: vi.fn(),
  updateDeckStatus: vi.fn(),
  deleteUserDeck: vi.fn(),
  reorderDeckTopics: vi.fn(),
  createSystemTopic: vi.fn(),
  updateSystemTopic: vi.fn(),
  deleteSystemTopic: vi.fn(),
  fetchAdminTags: vi.fn(),
  fetchCefrLevels: vi.fn(),
}));

describe('DeckDetailPage component', () => {
  const mockNavigate = vi.fn();

  const systemDeck = {
    id: 1,
    title: 'Business English',
    slug: 'business-english',
    description: 'Corporate vocabulary',
    ownerType: 'SYSTEM',
    status: 'PUBLISHED',
    isPremium: true,
    topicCount: 2,
    cardCount: 20,
    tags: [{ id: 1, label: 'Business' }],
    cefrLevels: [{ id: 3, label: 'B2' }],
  };

  const userDeck = {
    id: 99,
    title: 'Violating Community Deck',
    slug: 'violating-community-deck',
    description: 'Bad terms',
    ownerType: 'USER',
    ownerId: 777,
    status: 'PUBLISHED',
    isPremium: false,
    topicCount: 1,
    cardCount: 5,
    tags: [],
    cefrLevels: [],
  };

  const sampleTopics = [
    { id: 10, name: 'Topic 1: Meetings', cardCount: 10, order: 1 },
    { id: 11, name: 'Topic 2: Negotiations', cardCount: 10, order: 2 },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    adminContentApi.fetchAdminTags.mockResolvedValue([]);
    adminContentApi.fetchCefrLevels.mockResolvedValue([]);
  });

  describe.each(LANGS)('i18n rendering and Deck detail operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders SYSTEM deck with edit and topic management controls', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);

      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Business English' })).toBeInTheDocument();
        expect(screen.getByText('Topic 1: Meetings')).toBeInTheDocument();
        expect(screen.getByText('Topic 2: Negotiations')).toBeInTheDocument();
      });

      // System deck has edit deck button and add topic button
      expect(
        screen.getByRole('button', {
          name: lng === 'vi' ? 'Chỉnh sửa thông tin' : 'Edit Deck',
        })
      ).toBeInTheDocument();

      expect(
        screen.getByRole('button', {
          name: lng === 'vi' ? 'Thêm chủ đề mới' : 'Add New Topic',
        })
      ).toBeInTheDocument();
    });

    it('renders USER deck in read-only mode with violation warning and delete button', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(userDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.deleteUserDeck.mockResolvedValueOnce({ success: true });

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="99" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Violating Community Deck' })).toBeInTheDocument();
      });

      // Warning banner present
      expect(screen.getByText(/777/)).toBeInTheDocument();

      // Edit buttons must NOT exist
      expect(
        screen.queryByRole('button', {
          name: lng === 'vi' ? 'Chỉnh sửa thông tin' : 'Edit Deck',
        })
      ).not.toBeInTheDocument();

      expect(
        screen.queryByRole('button', {
          name: lng === 'vi' ? 'Thêm chủ đề mới' : 'Add New Topic',
        })
      ).not.toBeInTheDocument();

      // Delete violation button is present
      const deleteViolationBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xóa vi phạm chính sách' : 'Delete for Policy Violation',
      });
      await user.click(deleteViolationBtn);

      // Submit delete modal with reason
      const reasonInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập lý do vi phạm (bắt buộc)...' : 'Enter reason for violation (required)...'
      );
      await user.type(reasonInput, 'Severe breach of community terms');

      const confirmBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xác nhận xóa vĩnh viễn' : 'Confirm Permanent Deletion',
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminContentApi.deleteUserDeck).toHaveBeenCalledWith('99', 'Severe breach of community terms');
        expect(mockNavigate).toHaveBeenCalledWith('/admin/decks');
      });
    });

    it('allows reordering topics in system deck', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.reorderDeckTopics.mockResolvedValueOnce({ success: true });

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Topic 1: Meetings')).toBeInTheDocument();
      });

      // Click move down on first topic
      const moveDownBtn = screen.getByRole('button', {
        name: /Di chuyển xuống Topic 1: Meetings|Move Down Topic 1: Meetings/i,
      });
      await user.click(moveDownBtn);

      await waitFor(() => {
        expect(adminContentApi.reorderDeckTopics).toHaveBeenCalledWith('1', [11, 10]);
      });
    });
  });
});
