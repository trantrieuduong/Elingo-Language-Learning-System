import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  getSpeakingRoomsApi,
  getSpeakingRoomApi,
  getIceConfigApi,
  getSpeakingHistoryApi,
  getSpeakingHistorySummaryApi,
  getBansApi,
} from '@/modules/speaking/speakingApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('speaking/speakingApi.js', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // getSpeakingRoomsApi và getSpeakingHistoryApi dùng chung quy tắc build query
  describe.each([
    ['getSpeakingRoomsApi', getSpeakingRoomsApi, '/speaking-rooms'],
    ['getSpeakingHistoryApi', getSpeakingHistoryApi, '/speaking-rooms/history'],
  ])('%s', (_name, apiFn, url) => {
    it(`calls GET ${url} with default page and size`, async () => {
      apiClient.get.mockResolvedValue({
        data: { success: true, data: { content: [], totalPages: 1 } },
      });

      const result = await apiFn();

      expect(apiClient.get).toHaveBeenCalledWith(url, { params: { page: 1, size: 10 } });
      expect(result).toEqual({ success: true, data: { content: [], totalPages: 1 } });
    });

    it('sends page and size when provided', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await apiFn({ page: 3, size: 5 });

      expect(apiClient.get).toHaveBeenCalledWith(url, { params: { page: 3, size: 5 } });
    });

    it('sends a trimmed keyword when provided', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await apiFn({ keyword: '  travel  ' });

      expect(apiClient.get).toHaveBeenCalledWith(url, {
        params: { page: 1, size: 10, keyword: 'travel' },
      });
    });

    it.each([[''], ['   '], [null], [undefined]])('omits the keyword when it is %j', async (keyword) => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await apiFn({ keyword });

      const { params } = apiClient.get.mock.calls[0][1];
      expect(params).toEqual({ page: 1, size: 10 });
      expect(params).not.toHaveProperty('keyword');
    });

    it('sends maxParticipants when provided', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await apiFn({ maxParticipants: 3 });

      expect(apiClient.get).toHaveBeenCalledWith(url, {
        params: { page: 1, size: 10, maxParticipants: 3 },
      });
    });

    it.each([[null], [undefined], [0]])('omits maxParticipants when it is %j', async (value) => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await apiFn({ maxParticipants: value });

      const { params } = apiClient.get.mock.calls[0][1];
      expect(params).not.toHaveProperty('maxParticipants');
    });

    it('sends every filter together', async () => {
      apiClient.get.mockResolvedValue({ data: { success: true } });

      await apiFn({ page: 2, size: 20, keyword: 'ielts', maxParticipants: 4 });

      expect(apiClient.get).toHaveBeenCalledWith(url, {
        params: { page: 2, size: 20, keyword: 'ielts', maxParticipants: 4 },
      });
    });

    it('propagates errors from apiClient', async () => {
      apiClient.get.mockRejectedValue(new Error('Network error'));

      await expect(apiFn()).rejects.toThrow('Network error');
    });
  });

  describe('getSpeakingRoomApi', () => {
    it('calls GET /speaking-rooms/{roomCode} and returns response data', async () => {
      apiClient.get.mockResolvedValue({
        data: { success: true, data: { roomCode: 'ABC234', topic: 'Travel' } },
      });

      const result = await getSpeakingRoomApi('ABC234');

      expect(apiClient.get).toHaveBeenCalledWith('/speaking-rooms/ABC234');
      expect(result).toEqual({ success: true, data: { roomCode: 'ABC234', topic: 'Travel' } });
    });

    it('propagates errors from apiClient', async () => {
      apiClient.get.mockRejectedValue(new Error('Not found'));

      await expect(getSpeakingRoomApi('ABC234')).rejects.toThrow('Not found');
    });
  });

  describe('getIceConfigApi', () => {
    it('calls GET /speaking-rooms/ice-config and returns response data', async () => {
      const payload = {
        success: true,
        data: { iceServers: [{ urls: 'stun:stun.example.org:3478' }] },
      };
      apiClient.get.mockResolvedValue({ data: payload });

      const result = await getIceConfigApi();

      expect(apiClient.get).toHaveBeenCalledWith('/speaking-rooms/ice-config');
      expect(result).toEqual(payload);
    });

    it('propagates errors from apiClient', async () => {
      apiClient.get.mockRejectedValue(new Error('Server error'));

      await expect(getIceConfigApi()).rejects.toThrow('Server error');
    });
  });

  describe('getSpeakingHistorySummaryApi', () => {
    it('calls GET /speaking-rooms/history/summary and returns response data', async () => {
      const payload = {
        success: true,
        data: { totalSeconds: 3600, roomCount: 4, sessionCount: 6 },
      };
      apiClient.get.mockResolvedValue({ data: payload });

      const result = await getSpeakingHistorySummaryApi();

      expect(apiClient.get).toHaveBeenCalledWith('/speaking-rooms/history/summary');
      expect(result).toEqual(payload);
    });

    it('propagates errors from apiClient', async () => {
      apiClient.get.mockRejectedValue(new Error('Server error'));

      await expect(getSpeakingHistorySummaryApi()).rejects.toThrow('Server error');
    });
  });

  describe('getBansApi', () => {
    it('calls GET /speaking-rooms/{roomCode}/bans and returns response data', async () => {
      const payload = { success: true, data: [{ userId: 7, displayName: 'Eve' }] };
      apiClient.get.mockResolvedValue({ data: payload });

      const result = await getBansApi('ABC234');

      expect(apiClient.get).toHaveBeenCalledWith('/speaking-rooms/ABC234/bans');
      expect(result).toEqual(payload);
    });

    it('propagates errors from apiClient', async () => {
      apiClient.get.mockRejectedValue(new Error('Forbidden'));

      await expect(getBansApi('ABC234')).rejects.toThrow('Forbidden');
    });
  });
});
