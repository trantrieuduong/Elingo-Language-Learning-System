import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useReportModal } from '../../../../src/modules/report/hooks/useReportModal';

describe('useReportModal hook', () => {
  it('initializes with default closed state', () => {
    const { result } = renderHook(() => useReportModal());

    expect(result.current.isReportOpen).toBe(false);
    expect(result.current.targetType).toBeNull();
    expect(result.current.targetId).toBeNull();
    expect(result.current.targetLabel).toBe('');
  });

  it('updates state when openReport is called', () => {
    const { result } = renderHook(() => useReportModal());

    act(() => {
      result.current.openReport({
        targetType: 'POST',
        targetId: 101,
        targetLabel: 'Bài viết số 101',
      });
    });

    expect(result.current.isReportOpen).toBe(true);
    expect(result.current.targetType).toBe('POST');
    expect(result.current.targetId).toBe(101);
    expect(result.current.targetLabel).toBe('Bài viết số 101');
  });

  it('closes modal when closeReport is called', () => {
    const { result } = renderHook(() => useReportModal());

    act(() => {
      result.current.openReport({
        targetType: 'USER',
        targetId: 303,
        targetLabel: 'Người dùng vi phạm',
      });
    });

    expect(result.current.isReportOpen).toBe(true);

    act(() => {
      result.current.closeReport();
    });

    expect(result.current.isReportOpen).toBe(false);
  });
});
