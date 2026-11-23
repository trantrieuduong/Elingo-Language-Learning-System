import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from '@/services/apiClient';
import {
  fetchSystemConfigs,
  fetchConfigModules,
  updateSystemConfig,
} from '@/modules/admin/services/adminSystemConfigApi';

vi.mock('@/services/apiClient', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

describe('adminSystemConfigApi service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('fetchSystemConfigs', () => {
    it('calls GET /admin/system-configs without module param when omitted', async () => {
      const mockConfigs = [
        { id: 1, configKey: 'app.test.key', configValue: 'val' },
      ];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockConfigs } });

      const result = await fetchSystemConfigs();

      expect(apiClient.get).toHaveBeenCalledWith('/admin/system-configs', {
        params: {},
      });
      expect(result).toEqual(mockConfigs);
    });

    it('calls GET /admin/system-configs with trimmed module param when provided', async () => {
      const mockConfigs = [
        { id: 2, configKey: 'app.premium.prefix', module: 'PREMIUM' },
      ];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockConfigs } });

      const result = await fetchSystemConfigs('  PREMIUM  ');

      expect(apiClient.get).toHaveBeenCalledWith('/admin/system-configs', {
        params: { module: 'PREMIUM' },
      });
      expect(result).toEqual(mockConfigs);
    });

    it('returns empty array fallback when response data is undefined', async () => {
      apiClient.get.mockResolvedValueOnce({ data: {} });

      const result = await fetchSystemConfigs();

      expect(result).toEqual([]);
    });
  });

  describe('fetchConfigModules', () => {
    it('calls GET /admin/system-configs/config-module and returns module array', async () => {
      const mockModules = ['PREMIUM', 'COMMUNITY', 'GAMIFICATION'];
      apiClient.get.mockResolvedValueOnce({ data: { data: mockModules } });

      const result = await fetchConfigModules();

      expect(apiClient.get).toHaveBeenCalledWith('/admin/system-configs/config-module');
      expect(result).toEqual(mockModules);
    });

    it('returns empty array fallback when response is empty', async () => {
      apiClient.get.mockResolvedValueOnce({});

      const result = await fetchConfigModules();

      expect(result).toEqual([]);
    });
  });

  describe('updateSystemConfig', () => {
    it('calls PUT /admin/system-configs/{key} with encoded key and payload', async () => {
      const key = 'app.premium.bank-transfer.code-prefix';
      const payload = { value: 'ELGO_NEW', description: 'New description' };
      const mockResponse = { id: 1, configKey: key, configValue: 'ELGO_NEW' };

      apiClient.put.mockResolvedValueOnce({ data: { data: mockResponse } });

      const result = await updateSystemConfig(key, payload);

      expect(apiClient.put).toHaveBeenCalledWith(
        `/admin/system-configs/${encodeURIComponent(key)}`,
        payload
      );
      expect(result).toEqual(mockResponse);
    });
  });
});
