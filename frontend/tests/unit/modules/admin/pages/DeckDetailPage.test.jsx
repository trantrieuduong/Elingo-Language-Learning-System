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

    it('creates a new topic in system deck', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.createSystemTopic.mockResolvedValueOnce({
        id: 12,
        name: 'Topic 3: Contracts',
        cardCount: 0,
        order: 3,
      });

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Topic 1: Meetings')).toBeInTheDocument();
      });

      const addTopicBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thêm chủ đề mới' : 'Add New Topic',
      });
      await user.click(addTopicBtn);

      const nameInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập tên chủ đề bài học...' : 'Enter topic name...'
      );
      await user.type(nameInput, 'Topic 3: Contracts');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo mới' : 'Create',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.createSystemTopic).toHaveBeenCalledWith({
          deckId: '1',
          name: 'Topic 3: Contracts',
        });
        expect(screen.getByText('Topic 3: Contracts')).toBeInTheDocument();
      });
    });

    it('edits an existing topic in system deck', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.updateSystemTopic.mockResolvedValueOnce({
        id: 10,
        name: 'Topic 1: Team Meetings',
        cardCount: 10,
        order: 1,
      });

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Topic 1: Meetings')).toBeInTheDocument();
      });

      const editBtn = screen.getByRole('button', {
        name: /Đổi tên chủ đề Topic 1: Meetings|Rename Topic Topic 1: Meetings/i,
      });
      await user.click(editBtn);

      const nameInput = screen.getByPlaceholderText(
        lng === 'vi' ? 'Nhập tên chủ đề bài học...' : 'Enter topic name...'
      );
      await user.clear(nameInput);
      await user.type(nameInput, 'Topic 1: Team Meetings');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.updateSystemTopic).toHaveBeenCalledWith(10, {
          name: 'Topic 1: Team Meetings',
        });
        expect(screen.getByText('Topic 1: Team Meetings')).toBeInTheDocument();
      });
    });

    it('deletes a topic from system deck', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.deleteSystemTopic.mockResolvedValueOnce({ success: true });

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Topic 1: Meetings')).toBeInTheDocument();
      });

      const deleteBtns = screen.getAllByRole('button', {
        name: /Xóa chủ đề Topic 1: Meetings|Delete Topic Topic 1: Meetings/i,
      });
      await user.click(deleteBtns[0]);

      const confirmBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xóa chủ đề' : 'Delete Topic',
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminContentApi.deleteSystemTopic).toHaveBeenCalledWith(10);
        expect(screen.queryByText('Topic 1: Meetings')).not.toBeInTheDocument();
      });
    });

    it('changes deck status via quick status modal', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.updateDeckStatus.mockResolvedValueOnce({
        ...systemDeck,
        status: 'ARCHIVED',
      });

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Business English' })).toBeInTheDocument();
      });

      const changeStatusBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Đổi trạng thái' : 'Change Status',
      });
      await user.click(changeStatusBtn);

      const statusSelect = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Trạng thái' : 'Status', 'i'));
      await user.selectOptions(statusSelect, 'ARCHIVED');

      const saveBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
      });
      await user.click(saveBtn);

      await waitFor(() => {
        expect(adminContentApi.updateDeckStatus).toHaveBeenCalledWith('1', 'ARCHIVED');
      });
    });

    it('navigates to topic cards when view cards button or title link is clicked', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Topic 1: Meetings')).toBeInTheDocument();
      });

      const topicLink = screen.getByText('Topic 1: Meetings');
      await user.click(topicLink);

      expect(mockNavigate).toHaveBeenCalledWith('/admin/decks/1/topics/10');
    });

    it('renders error state and retries on retry button click', async () => {
      adminContentApi.fetchAdminDeckDetail.mockRejectedValueOnce(new Error('Deck not found'));
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce([]);

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Thử lại|Retry/i })).toBeInTheDocument();
      });

      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);

      const retryBtn = screen.getByRole('button', {
        name: /Thử lại|Retry/i,
      });
      await user.click(retryBtn);

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Business English' })).toBeInTheDocument();
      });
    });

    it('allows moving topic up in system deck', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.reorderDeckTopics.mockResolvedValueOnce({ success: true });

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Topic 2: Negotiations')).toBeInTheDocument();
      });

      // Click move up on second topic
      const moveUpBtn = screen.getByRole('button', {
        name: /Di chuyển lên Topic 2: Negotiations|Move Up Topic 2: Negotiations/i,
      });
      await user.click(moveUpBtn);

      await waitFor(() => {
        expect(adminContentApi.reorderDeckTopics).toHaveBeenCalledWith('1', [11, 10]);
      });
    });

    it('cancels topic modal and delete topic modal', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);

      const user = userEvent.setup();
      render(<DeckDetailPage deckId="1" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('Topic 1: Meetings')).toBeInTheDocument();
      });

      // Open Add Topic
      const addTopicBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thêm chủ đề mới' : 'Add New Topic',
      });
      await user.click(addTopicBtn);

      // Cancel Add Topic
      const cancelTopicBtns = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Hủy bỏ' : 'Cancel',
      });
      await user.click(cancelTopicBtns[0]);

      // Open Delete Topic
      const deleteBtns = screen.getAllByRole('button', {
        name: /Xóa chủ đề Topic 1: Meetings|Delete Topic Topic 1: Meetings/i,
      });
      await user.click(deleteBtns[0]);

      // Cancel Delete Topic
      const cancelDeleteBtns = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Hủy bỏ' : 'Cancel',
      });
      await user.click(cancelDeleteBtns[0]);
    });
  });
});
