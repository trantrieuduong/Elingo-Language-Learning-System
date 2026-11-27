import { describe, it, expect, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import useAdminTabStats from '@/modules/admin/hooks/useAdminTabStats';

describe('useAdminTabStats', () => {
  it('loads data successfully and updates state', async () => {
    const mockData = { count: 42 };
    const fetcher = vi.fn().mockResolvedValueOnce(mockData);

    const { result } = renderHook(() => useAdminTabStats(fetcher, ['key-1'], 'Fallback error'));

    expect(result.current.loading).toBe(true);
    expect(result.current.error).toBeNull();

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.data).toEqual(mockData);
    expect(result.current.error).toBeNull();
  });

  it('sets error message from rejected error or fallback', async () => {
    const fetcherWithMessage = vi.fn().mockRejectedValueOnce(new Error('Network down'));
    const { result } = renderHook(() =>
      useAdminTabStats(fetcherWithMessage, ['k1'], 'Fallback error'),
    );

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.error).toBe('Network down');
    expect(result.current.data).toBeNull();

    const fetcherWithoutMessage = vi.fn().mockRejectedValueOnce({});
    const { result: fallbackResult } = renderHook(() =>
      useAdminTabStats(fetcherWithoutMessage, ['k2'], 'Fallback error'),
    );

    await waitFor(() => {
      expect(fallbackResult.current.loading).toBe(false);
    });
    expect(fallbackResult.current.error).toBe('Fallback error');
  });

  it('does not update state after unmount', async () => {
    let resolvePromise;
    const fetcher = vi.fn(
      () =>
        new Promise((resolve) => {
          resolvePromise = resolve;
        }),
    );

    const { unmount } = renderHook(() => useAdminTabStats(fetcher, ['k3'], 'Fallback error'));

    await waitFor(() => {
      expect(fetcher).toHaveBeenCalledTimes(1);
    });

    unmount();
    resolvePromise({ count: 10 });

    await Promise.resolve();
    expect(fetcher).toHaveBeenCalledTimes(1);
  });
});
