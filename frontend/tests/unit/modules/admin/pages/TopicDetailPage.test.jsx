import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TopicDetailPage from '@/modules/admin/pages/TopicManagementPage/TopicDetailPage';
import * as adminContentApi from '@/modules/admin/services/adminContentApi';
import * as uploadFileService from '@/services/uploadFile';
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

vi.mock('@/services/uploadFile', () => ({
  uploadFileForVerification: vi.fn(),
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

    it('edits an existing system card', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);
      adminContentApi.updateSystemCard.mockResolvedValueOnce({
        ...sampleCards[0],
        translation: 'sự kiên trì bền bỉ',
      });

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('perseverance')).toBeInTheDocument();
      });

      const editBtns = screen.getAllByRole('button', {
        name: /Sửa thẻ từ vựng perseverance|Edit Card perseverance/i,
      });
      await user.click(editBtns[0]);

      const transInput = screen.getByLabelText(new RegExp(lng === 'vi' ? 'Nghĩa tiếng Việt' : 'Translation', 'i'));
      await user.clear(transInput);
      await user.type(transInput, 'sự kiên trì bền bỉ');

      const submitBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Lưu thay đổi' : 'Save Changes',
      });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(adminContentApi.updateSystemCard).toHaveBeenCalledWith(
          101,
          expect.objectContaining({
            translation: 'sự kiên trì bền bỉ',
          })
        );
      });
    });

    it('plays audio when play button is clicked', async () => {
      const playMock = vi.fn().mockResolvedValue(undefined);
      window.Audio = vi.fn().mockImplementation(function (url) {
        this.src = url;
        this.play = playMock;
        this.pause = vi.fn();
      });

      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('perseverance')).toBeInTheDocument();
      });

      const playBtns = screen.getAllByRole('button', {
        name: /Nghe phát âm perseverance|Listen perseverance/i,
      });
      await user.click(playBtns[0]);

      expect(window.Audio).toHaveBeenCalledWith('https://cdn.example.com/audio.mp3');
      expect(playMock).toHaveBeenCalled();
    });

    it('renders error state and retries on failure', async () => {
      adminContentApi.fetchAdminDeckDetail.mockRejectedValueOnce(new Error('Failed to load topic cards'));
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce([]);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce([]);

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Thử lại|Retry/i })).toBeInTheDocument();
      });

      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);

      const retryBtn = screen.getByRole('button', {
        name: /Thử lại|Retry/i,
      });
      await user.click(retryBtn);

      await waitFor(() => {
        expect(screen.getByText('perseverance')).toBeInTheDocument();
      });
    });
    it('allows moving card up in system topic', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);
      adminContentApi.reorderTopicCards.mockResolvedValueOnce({ success: true });

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('resilience')).toBeInTheDocument();
      });

      const moveUpBtn = screen.getByRole('button', {
        name: /Di chuyển lên resilience|Move Up resilience/i,
      });
      await user.click(moveUpBtn);

      await waitFor(() => {
        expect(adminContentApi.reorderTopicCards).toHaveBeenCalledWith('5', [102, 101]);
      });
    });

    it('handles image and phonetic audio uploads and removal in card modal', async () => {
      uploadFileService.uploadFileForVerification
        .mockResolvedValueOnce('uploads/card-img.png')
        .mockResolvedValueOnce('uploads/audio-uk.mp3')
        .mockResolvedValueOnce('uploads/audio-us.mp3');

      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('perseverance')).toBeInTheDocument();
      });

      const addBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Thêm thẻ từ vựng' : 'Add New Card',
      });
      await user.click(addBtn);

      // 1. Upload invalid image -> displays upload image error
      const imgInput = document.querySelector('input[type="file"][accept="image/*"]');
      const textFile = new File(['text'], 'test.txt', { type: 'text/plain' });
      fireEvent.change(imgInput, { target: { files: [textFile] } });
      expect(screen.getByText(lng === 'vi' ? 'Tải ảnh lên thất bại' : 'Failed to upload image')).toBeInTheDocument();

      // 2. Upload valid image
      const validImg = new File(['img'], 'card.png', { type: 'image/png' });
      fireEvent.change(imgInput, { target: { files: [validImg] } });
      await waitFor(() => {
        expect(uploadFileService.uploadFileForVerification).toHaveBeenCalledWith(validImg, 'image/png');
      });

      // Remove image
      const removeImgBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Gỡ ảnh' : 'Remove image',
      });
      await user.click(removeImgBtn);

      // 3. Audio UK: invalid then valid
      const audioInputs = document.querySelectorAll('input[type="file"][accept="audio/*"]');
      const audioUkInput = audioInputs[0];
      fireEvent.change(audioUkInput, { target: { files: [textFile] } });
      expect(screen.getByText(lng === 'vi' ? 'Tải âm thanh lên thất bại' : 'Failed to upload audio')).toBeInTheDocument();

      const validUkAudio = new File(['audio'], 'uk.mp3', { type: 'audio/mpeg' });
      fireEvent.change(audioUkInput, { target: { files: [validUkAudio] } });
      await waitFor(() => {
        expect(uploadFileService.uploadFileForVerification).toHaveBeenCalledWith(validUkAudio, 'audio/mpeg');
      });

      // Remove UK audio
      const removeUkBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Gỡ' : 'Remove',
      });
      await user.click(removeUkBtn);

      // 4. Audio US: valid
      const audioUsInput = audioInputs[1];
      const validUsAudio = new File(['audio'], 'us.mp3', { type: 'audio/mpeg' });
      fireEvent.change(audioUsInput, { target: { files: [validUsAudio] } });
      await waitFor(() => {
        expect(uploadFileService.uploadFileForVerification).toHaveBeenCalledWith(validUsAudio, 'audio/mpeg');
      });

      // Remove US audio
      const removeUsBtn = screen.getByRole('button', {
        name: lng === 'vi' ? 'Gỡ' : 'Remove',
      });
      await user.click(removeUsBtn);

      // Close modal
      const cancelModalBtns = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Hủy bỏ' : 'Cancel',
      });
      await user.click(cancelModalBtns[0]);
    });

    it('cancels card deletion when cancel button is clicked', async () => {
      adminContentApi.fetchAdminDeckDetail.mockResolvedValueOnce(systemDeck);
      adminContentApi.fetchDeckTopics.mockResolvedValueOnce(sampleTopics);
      adminContentApi.fetchTopicCards.mockResolvedValueOnce(sampleCards);

      const user = userEvent.setup();
      render(<TopicDetailPage deckId="1" topicId="5" onNavigate={mockNavigate} />);

      await waitFor(() => {
        expect(screen.getByText('perseverance')).toBeInTheDocument();
      });

      const deleteBtns = screen.getAllByRole('button', {
        name: /Bạn có chắc chắn muốn xóa thẻ từ vựng này\? perseverance|Are you sure you want to delete this card\? perseverance/i,
      });
      await user.click(deleteBtns[0]);

      const cancelBtns = screen.getAllByRole('button', {
        name: lng === 'vi' ? 'Hủy bỏ' : 'Cancel',
      });
      await user.click(cancelBtns[0]);

      expect(screen.queryByRole('button', {
        name: lng === 'vi' ? 'Xác nhận xóa vĩnh viễn' : 'Confirm Permanent Deletion',
      })).not.toBeInTheDocument();
    });
  });
});
