import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TopicDetailPage from '@/modules/admin/pages/TopicManagementPage/TopicDetailPage';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import { useLang, LANGS } from '../../../../utils/i18n';

vi.mock('@/modules/admin/services/adminContentApi', () => ({
  fetchAdminDeckDetail: vi.fn(),
  fetchDeckTopics: vi.fn(),
  fetchTopicCards: vi.fn(),
  reorderTopicCards: vi.fn(),
  createSystemCard: vi.fn(),
  updateSystemCard: vi.fn(),
  deleteSystemCard: vi.fn(),
}));

describe('TopicDetailPage component', () => {
  const mockNavigate = vi.fn();

  const systemDeck = {
    id: 1,
    title: 'Oxford 3000',
    ownerType: 'SYSTEM',
  };

  const userDeck = {
    id: 99,
    title: 'User Deck',
    ownerType: 'USER',
  };

  const sampleTopics = [
    { id: 5, name: 'Unit 1: Introductions' },
  ];

  const sampleCards = [
    {
      id: 101,
      term: 'perseverance',
      pos: 'noun',
      translation: 'sự kiên trì',
      explanationVi: 'Tính kiên trì bền bỉ',
      examplesEn: 'Success requires perseverance.',
      phonetics: [
        { text: '/ˌpɜː.sɪˈvɪə.rəns/', audioUrl: 'https://cdn.example.com/audio.mp3', locale: 'UK' },
      ],
    },
    {
      id: 102,
      term: 'resilience',
      pos: 'noun',
      translation: 'khả năng phục hồi',
      phonetics: [],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe.each(LANGS)('i18n rendering and Cards operations [%s]', (lng) => {
    beforeEach(async () => {
      await useLang(lng);
    });

    it('renders topic cards and allows system card operations', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);

      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Unit 1: Introductions' })).toBeInTheDocument();
        expect(screen.getByText('perseverance')).toBeInTheDocument();
        expect(screen.getByText('sự kiên trì')).toBeInTheDocument();
      });

      // System deck has add card button
      const addCardBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thêm thẻ từ vựng' : 'Add New Card',
      });
      expect(addCardBtn).toBeInTheDocument();
    });

    it('renders read-only mode for user deck topic cards', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(userDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);

      render(<TopicDetailPage deckId="99" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { level: 1, name: 'Unit 1: Introductions' })).toBeInTheDocument();
      });

      // No add card button
      expect(
        screen.queryByRole('button', {
          name: lng === 'vi' ? 'Thêm thẻ từ vựng' : 'Add New Card',
        })
      ).not.toBeInTheDocument();

      // Table indicates Read-only
      expect(screen.getAllByText('Read-only')).toHaveLength(2);
    });

    it('allows reordering cards in system topic', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);
      adminContentApi.reorderTopicCards.mockResolvedValueOnce({ success: true });

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('perseverance')).toBeInTheDocument();
      });

      // Move perseverance down
      const moveDownBtn = screen.getByRole('button', {
        name: /Di chuyển xuống perseverance|Move Down perseverance/i,
      });
      await user.click(moveDownBtn);

      await waitFor(() => {
        expect(adminContentApi.reorderTopicCards).toHaveBeenCalledWith('5', [102, 101]);
      });
    });

    it('creates a new system card with term and translation', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);
      adminContentApi.createSystemCard.mockResolvedValueOnce({
        id: 103,
        term: 'ambition',
        pos: 'noun',
        translation: 'sự tham vọng',
        phonetics: [],
      });

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('perseverance')).toBeInTheDocument();
      });

      const addBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thêm thẻ từ vựng' : 'Add New Card',
      });
      await user.click(addBtn);

      // Modal open - fill required fields
      const termInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Thuật ngữ' : 'Term', 'i'));
      await user.type(termInput, 'ambition');

      const transInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Nghĩa tiếng Việt' : 'Translation', 'i'));
      await user.type(transInput, 'sự tham vọng');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Tạo mới' : 'Create',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.createSystemCard).toHaveBeenCalledWith(
          expect.objectContaining({
            topicId: 5,
            term: 'ambition',
            translation: 'sự tham vọng',
          })
        );
      });
    });

    it('deletes a card when confirmed', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);
      adminContentApi.deleteSystemCard.mockResolvedValueOnce({ success: true });

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('perseverance')).toBeInTheDocument();
      });

      const deleteBtns = screen.getAllByRole('button', {
        name: /Bạn có chắc chắn muốn xóa thẻ từ vựng này\? perseverance|Are you sure you want to delete this card\? perseverance/i,
      });
      await user.click(deleteBtns[0]);

      // Confirm in delete modal
      const confirmBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Xác nhận xóa vĩnh viễn' : 'Confirm Permanent Deletion',
      });
      await user.click(confirmBtn);

      await waitFor(() => {
        expect(adminContentApi.deleteSystemCard).toHaveBeenCalledWith(101);
      });
    });
  });
});
