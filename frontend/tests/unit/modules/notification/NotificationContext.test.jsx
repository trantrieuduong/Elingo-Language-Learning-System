import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import {
  NotificationProvider,
  useNotification,
} from '@/modules/notification/context/NotificationContext';
import { useAuth } from '@/context/AuthContext';
import * as notificationApi from '@/modules/notification/notificationApi';
import * as notificationSocket from '@/modules/notification/notificationSocket';

vi.mock('@/context/AuthContext');
vi.mock('@/modules/notification/notificationApi');
vi.mock('@/modules/notification/notificationSocket');

describe('NotificationContext', () => {
  const mockUser = { id: 1, username: 'testuser' };
  let socketHandler = null;
  const mockUnsubscribe = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({ user: mockUser });

    notificationSocket.subscribeNotifications.mockImplementation((handler) => {
      socketHandler = handler;
      return mockUnsubscribe;
    });

    notificationApi.getUnreadNotificationCountApi.mockResolvedValue({
      success: true,
      data: { count: 3 },
    });

    notificationApi.getNotificationsApi.mockResolvedValue({
      success: true,
      data: {
        content: [
          { id: 1, title: 'Notif 1', read: false },
          { id: 2, title: 'Notif 2', read: true },
        ],
        page: 1,
        isLast: false,
      },
    });

    notificationApi.markNotificationReadApi.mockResolvedValue({
      success: true,
    });

    notificationApi.markAllNotificationsReadApi.mockResolvedValue({
      success: true,
    });
  });

  it('automatically fetches unreadCount and subscribes to websocket when user logs in', async () => {
    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await waitFor(() => {
      expect(notificationApi.getUnreadNotificationCountApi).toHaveBeenCalled();
      expect(result.current.unreadCount).toBe(3);
      expect(notificationSocket.subscribeNotifications).toHaveBeenCalled();
    });
  });

  it('receives new notification via websocket (event notification:new), prepends to list, and increments unreadCount', async () => {
    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await waitFor(() => {
      expect(result.current.unreadCount).toBe(3);
    });

    // Send new websocket payload
    act(() => {
      socketHandler({
        event: 'notification:new',
        data: { id: 99, title: 'New Notification from WS', read: false },
      });
    });

    expect(result.current.notifications[0]).toEqual({
      id: 99,
      title: 'New Notification from WS',
      read: false,
    });
    expect(result.current.unreadCount).toBe(4);

    // Duplicate notification ID -> should be ignored
    act(() => {
      socketHandler({
        event: 'notification:new',
        data: { id: 99, title: 'Duplicate Notif', read: false },
      });
    });

    expect(result.current.notifications.length).toBe(1);
    expect(result.current.unreadCount).toBe(4);
  });

  it('loadInitialNotifications: loads the first page of notifications', async () => {
    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await act(async () => {
      await result.current.loadInitialNotifications();
    });

    expect(notificationApi.getNotificationsApi).toHaveBeenCalledWith({
      page: 1,
      size: 10,
    });
    expect(result.current.notifications).toHaveLength(2);
    expect(result.current.isLast).toBe(false);
    expect(result.current.isLoadingFirstPage).toBe(false);
  });

  it('loadInitialNotifications handles error: sets hasError to true', async () => {
    notificationApi.getNotificationsApi.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await act(async () => {
      await result.current.loadInitialNotifications();
    });

    expect(result.current.hasError).toBe(true);
    expect(result.current.isLoadingFirstPage).toBe(false);
  });

  it('loadMoreNotifications: loads the next page of notifications and appends them to the list', async () => {
    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await act(async () => {
      await result.current.loadInitialNotifications();
    });

    notificationApi.getNotificationsApi.mockResolvedValueOnce({
      success: true,
      data: {
        content: [
          { id: 3, title: 'Notif 3', read: false },
          { id: 4, title: 'Notif 4', read: true },
        ],
        page: 2,
        isLast: true,
      },
    });

    await act(async () => {
      await result.current.loadMoreNotifications();
    });

    expect(notificationApi.getNotificationsApi).toHaveBeenCalledWith({
      page: 2,
      size: 10,
    });
    expect(result.current.notifications).toHaveLength(4);
    expect(result.current.isLast).toBe(true);
    expect(result.current.isLoadingMore).toBe(false);
  });

  it('loadMoreNotifications handle API fail or server crash', async () => {
    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await act(async () => {
      await result.current.loadInitialNotifications();
    });

    notificationApi.getNotificationsApi.mockRejectedValueOnce(new Error('Server crashed'));

    let response;
    await act(async () => {
      response = await result.current.loadMoreNotifications();
    });

    expect(response).toEqual({ success: false });
    expect(result.current.hasError).toBe(true);
    expect(result.current.isLoadingMore).toBe(false);
  });

  it('markAsRead: marks single notification as read, updates item read state and decrements unreadCount', async () => {
    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await act(async () => {
      await result.current.loadInitialNotifications();
    });

    expect(result.current.unreadCount).toBe(3);
    const unreadItem = result.current.notifications.find((item) => item.id === 1);
    expect(unreadItem.read).toBe(false);

    await act(async () => {
      await result.current.markAsRead(1);
    });

    expect(notificationApi.markNotificationReadApi).toHaveBeenCalledWith(1);
    const updatedItem = result.current.notifications.find((item) => item.id === 1);
    expect(updatedItem.read).toBe(true);
    expect(result.current.unreadCount).toBe(2);
  });

  it('markAsRead does not call API when notification is already read', async () => {
    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await act(async () => {
      await result.current.loadInitialNotifications();
    });

    // Item 2 is already read
    await act(async () => {
      await result.current.markAsRead(2);
    });

    expect(notificationApi.markNotificationReadApi).not.toHaveBeenCalled();
  });

  it('markAllAsRead: marks all notifications as read, sets read=true for all items, and resets unreadCount to 0', async () => {
    const { result } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await act(async () => {
      await result.current.loadInitialNotifications();
    });

    await act(async () => {
      await result.current.markAllAsRead();
    });

    expect(notificationApi.markAllNotificationsReadApi).toHaveBeenCalledTimes(1);
    expect(result.current.notifications.every((item) => item.read === true)).toBe(true);
    expect(result.current.unreadCount).toBe(0);
  });

  it('resets state when user logs out or switches accounts', async () => {
    const { result, rerender } = renderHook(() => useNotification(), {
      wrapper: NotificationProvider,
    });

    await act(async () => {
      await result.current.loadInitialNotifications();
    });
    expect(result.current.notifications.length).toBeGreaterThan(0);

    // Logout: user = null
    useAuth.mockReturnValue({ user: null });
    rerender();

    expect(result.current.notifications).toEqual([]);
    expect(result.current.unreadCount).toBe(0);
    expect(result.current.isLast).toBe(true);
  });
});
