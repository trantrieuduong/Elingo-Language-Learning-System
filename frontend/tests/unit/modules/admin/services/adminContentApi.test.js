import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  fetchCefrLevels,
  fetchAdminDecks,
  fetchAdminDeckDetail,
  createSystemDeck,
  updateSystemDeck,
  updateDeckStatus,
  deleteUserDeck,
  fetchDeckTopics,
  reorderDeckTopics,
  createSystemTopic,
  updateSystemTopic,
  deleteSystemTopic,
  fetchTopicCards,
  reorderTopicCards,
  fetchCardDetail,
  createSystemCard,
  updateSystemCard,
  deleteSystemCard,
  fetchAdminTags,
  fetchAdminTagDetail,
  createAdminTag,
  updateAdminTag,
  deleteAdminTag,
  fetchOrphanedCardStats,
  fetchOrphanedCards,
  cleanupOrphanedCards,
} from '@/modules/admin/services/adminContentApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('adminContentApi service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('CEFR Levels API', () => {
    it('fetchCefrLevels calls /cefr-levels and returns data', async () => {
      const mockLevels = [{ id: 1, label: 'A1' }, { id: 2, label: 'B1' }];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockLevels } });

      const result = await fetchCefrLevels();
      expect(apiClient.get).toHaveBeenCalledWith('/cefr-levels');
      expect(result).toEqual(mockLevels);
    });
  });

  describe('Decks API', () => {
    it('fetchAdminDecks builds query params correctly', async () => {
      const mockData = { content: [], totalElements: 0 };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockData } });

      const params = {
        keyword: ' Oxford ',
        ownerType: 'SYSTEM',
        status: 'PUBLISHED',
        isPremium: false,
        tagIds: [1, 2],
        cefrLevelIds: [3],
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
      };

      const result = await fetchAdminDecks(params);

      expect(apiClient.get).toHaveBeenCalledWith('/admin/decks', {
        params: {
          keyword: 'Oxford',
          ownerType: 'SYSTEM',
          status: 'PUBLISHED',
          isPremium: false,
          tagIds: [1, 2],
          cefrLevelIds: [3],
          page: 0,
          size: 20,
          sort: 'createdAt,desc',
        },
      });
      expect(result).toEqual(mockData);
    });

    it('fetchAdminDeckDetail calls GET /admin/decks/:id', async () => {
      const mockDeck = { id: 10, title: 'IELTS Core' };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockDeck } });

      const result = await fetchAdminDeckDetail(10);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/decks/10');
      expect(result).toEqual(mockDeck);
    });

    it('createSystemDeck posts payload to /admin/decks', async () => {
      const payload = { title: 'TOEIC 600', isPremium: true };
      apiClient.post.mockResolvedValueOnce({ data: { data: { id: 11, ...payload } } });

      const result = await createSystemDeck(payload);
      expect(apiClient.post).toHaveBeenCalledWith('/admin/decks', payload);
      expect(result.id).toBe(11);
    });

    it('updateSystemDeck puts payload to /admin/decks/:id', async () => {
      const payload = { title: 'TOEIC 700' };
      apiClient.put.mockResolvedValueOnce({ data: { data: { id: 11, ...payload } } });

      const result = await updateSystemDeck(11, payload);
      expect(apiClient.put).toHaveBeenCalledWith('/admin/decks/11', payload);
      expect(result.title).toBe('TOEIC 700');
    });

    it('updateDeckStatus patches status to /admin/decks/:id/status', async () => {
      apiClient.patch.mockResolvedValueOnce({ data: { data: { id: 11, status: 'ARCHIVED' } } });

      const result = await updateDeckStatus(11, 'ARCHIVED');
      expect(apiClient.patch).toHaveBeenCalledWith('/admin/decks/11/status', { status: 'ARCHIVED' });
      expect(result.status).toBe('ARCHIVED');
    });

    it('deleteUserDeck calls DELETE with reason param', async () => {
      apiClient.delete.mockResolvedValueOnce({ data: { success: true } });

      const result = await deleteUserDeck(99, 'Spam content');
      expect(apiClient.delete).toHaveBeenCalledWith('/admin/decks/99', {
        params: { reason: 'Spam content' },
      });
      expect(result.success).toBe(true);
    });

    it('fetchDeckTopics calls GET /admin/decks/:id/topics', async () => {
      const mockTopics = [{ id: 1, name: 'Unit 1' }];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockTopics } });

      const result = await fetchDeckTopics(10);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/decks/10/topics');
      expect(result).toEqual(mockTopics);
    });

    it('reorderDeckTopics patches orderedIds', async () => {
      apiClient.patch.mockResolvedValueOnce({ data: { success: true } });

      const result = await reorderDeckTopics(10, [2, 1]);
      expect(apiClient.patch).toHaveBeenCalledWith('/admin/decks/10/topics/orders', {
        orderedIds: [2, 1],
      });
      expect(result.success).toBe(true);
    });
  });

  describe('Topics API', () => {
    it('createSystemTopic posts payload', async () => {
      const payload = { deckId: 10, name: 'Travel' };
      apiClient.post.mockResolvedValueOnce({ data: { data: { id: 5, ...payload } } });

      const result = await createSystemTopic(payload);
      expect(apiClient.post).toHaveBeenCalledWith('/admin/topics', payload);
      expect(result.id).toBe(5);
    });

    it('updateSystemTopic puts payload', async () => {
      const payload = { name: 'Travel & Tourism' };
      apiClient.put.mockResolvedValueOnce({ data: { data: { id: 5, ...payload } } });

      const result = await updateSystemTopic(5, payload);
      expect(apiClient.put).toHaveBeenCalledWith('/admin/topics/5', payload);
      expect(result.name).toBe('Travel & Tourism');
    });

    it('deleteSystemTopic deletes topic by id', async () => {
      apiClient.delete.mockResolvedValueOnce({ data: { success: true } });

      const result = await deleteSystemTopic(5);
      expect(apiClient.delete).toHaveBeenCalledWith('/admin/topics/5');
      expect(result.success).toBe(true);
    });

    it('fetchTopicCards calls GET /admin/topics/:id/cards', async () => {
      const mockCards = [{ id: 100, term: 'hello' }];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockCards } });

      const result = await fetchTopicCards(5);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/topics/5/cards');
      expect(result).toEqual(mockCards);
    });

    it('reorderTopicCards patches orderedIds to /admin/topics/:id/cards/orders', async () => {
      apiClient.patch.mockResolvedValueOnce({ data: { success: true } });

      const result = await reorderTopicCards(5, [102, 101]);
      expect(apiClient.patch).toHaveBeenCalledWith('/admin/topics/5/cards/orders', {
        orderedIds: [102, 101],
      });
      expect(result.success).toBe(true);
    });
  });

  describe('Cards API', () => {
    it('fetchCardDetail calls GET /admin/cards/:id', async () => {
      const mockCard = { id: 101, term: 'world' };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockCard } });

      const result = await fetchCardDetail(101);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/cards/101');
      expect(result).toEqual(mockCard);
    });

    it('createSystemCard posts payload to /admin/cards', async () => {
      const payload = { topicId: 5, term: 'apple', translation: 'quả táo' };
      apiClient.post.mockResolvedValueOnce({ data: { data: { id: 200, ...payload } } });

      const result = await createSystemCard(payload);
      expect(apiClient.post).toHaveBeenCalledWith('/admin/cards', payload);
      expect(result.id).toBe(200);
    });

    it('updateSystemCard puts payload to /admin/cards/:id', async () => {
      const payload = { term: 'apple', translation: 'trái táo' };
      apiClient.put.mockResolvedValueOnce({ data: { data: { id: 200, ...payload } } });

      const result = await updateSystemCard(200, payload);
      expect(apiClient.put).toHaveBeenCalledWith('/admin/cards/200', payload);
      expect(result.translation).toBe('trái táo');
    });

    it('deleteSystemCard deletes card by id', async () => {
      apiClient.delete.mockResolvedValueOnce({ data: { success: true } });

      const result = await deleteSystemCard(200);
      expect(apiClient.delete).toHaveBeenCalledWith('/admin/cards/200');
      expect(result.success).toBe(true);
    });
  });

  describe('Tags API', () => {
    it('fetchAdminTags calls GET /admin/tags', async () => {
      const mockTags = [{ id: 1, label: 'Grammar' }];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockTags } });

      const result = await fetchAdminTags();
      expect(apiClient.get).toHaveBeenCalledWith('/admin/tags');
      expect(result).toEqual(mockTags);
    });

    it('fetchAdminTagDetail calls GET /admin/tags/:id', async () => {
      const mockTag = { id: 1, label: 'Grammar' };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockTag } });

      const result = await fetchAdminTagDetail(1);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/tags/1');
      expect(result).toEqual(mockTag);
    });

    it('createAdminTag posts payload to /admin/tags', async () => {
      const payload = { label: 'Business' };
      apiClient.post.mockResolvedValueOnce({ data: { data: { id: 2, ...payload } } });

      const result = await createAdminTag(payload);
      expect(apiClient.post).toHaveBeenCalledWith('/admin/tags', payload);
      expect(result.label).toBe('Business');
    });

    it('updateAdminTag puts payload to /admin/tags/:id', async () => {
      const payload = { label: 'Business & Finance' };
      apiClient.put.mockResolvedValueOnce({ data: { data: { id: 2, ...payload } } });

      const result = await updateAdminTag(2, payload);
      expect(apiClient.put).toHaveBeenCalledWith('/admin/tags/2', payload);
      expect(result.label).toBe('Business & Finance');
    });

    it('deleteAdminTag deletes tag by id', async () => {
      apiClient.delete.mockResolvedValueOnce({ data: { success: true } });

      const result = await deleteAdminTag(2);
      expect(apiClient.delete).toHaveBeenCalledWith('/admin/tags/2');
      expect(result.success).toBe(true);
    });
  });

  describe('Orphaned Cards API', () => {
    it('fetchOrphanedCardStats calls GET /admin/orphaned-cards/stats', async () => {
      const mockStats = {
        totalOrphanedCards: 120,
        safeToDeleteCards: 45,
        retainedInStudyCards: 75,
      };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockStats } });

      const result = await fetchOrphanedCardStats();
      expect(apiClient.get).toHaveBeenCalledWith('/admin/orphaned-cards/stats');
      expect(result).toEqual(mockStats);
    });

    it('fetchOrphanedCards calls GET /admin/orphaned-cards with 1-indexed pagination params', async () => {
      const mockPage = {
        content: [{ id: 505, term: 'obsolete' }],
        page: 2,
        size: 20,
        totalElements: 25,
        totalPages: 2,
        isLast: true,
      };
      apiClient.get.mockResolvedValueOnce({ data: { data: mockPage } });

      const result = await fetchOrphanedCards({ page: 2, size: 20, sort: 'id,desc' });
      expect(apiClient.get).toHaveBeenCalledWith('/admin/orphaned-cards', {
        params: { page: 2, size: 20, sort: 'id,desc' },
      });
      expect(result).toEqual(mockPage);
    });

    it('cleanupOrphanedCards calls POST /admin/orphaned-cards/cleanup', async () => {
      const mockCleanup = {
        scannedCount: 45,
        deletedCardCount: 45,
        deletedFileCount: 38,
      };
      apiClient.post.mockResolvedValueOnce({ data: { data: mockCleanup } });

      const result = await cleanupOrphanedCards();
      expect(apiClient.post).toHaveBeenCalledWith('/admin/orphaned-cards/cleanup');
      expect(result).toEqual(mockCleanup);
    });
  });
});
