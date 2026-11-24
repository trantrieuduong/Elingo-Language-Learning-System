import { createContext, useContext, useState, useEffect, useRef } from 'react';
import { useAuth } from '../../../context/AuthContext';
import {
  getNotificationsApi,
  getUnreadNotificationCountApi,
  markNotificationReadApi,
  markAllNotificationsReadApi,
} from '../notificationApi';
import { subscribeNotifications } from '../notificationSocket';

const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
  const { user } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [page, setPage] = useState(1);
  const [isLast, setIsLast] = useState(true);
  const [isLoadingFirstPage, setIsLoadingFirstPage] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasError, setHasError] = useState(false);
  const notificationIdsRef = useRef(new Set());
  const markingReadIdsRef = useRef(new Set());

  // Reset state khi đổi tài khoản (user id thay đổi hoặc logout)
  useEffect(() => {
    notificationIdsRef.current.clear();
    markingReadIdsRef.current.clear();
    setNotifications([]);
    setUnreadCount(0);
    setPage(1);
    setIsLast(true);
    setHasError(false);
    setIsLoadingFirstPage(false);
    setIsLoadingMore(false);
  }, [user?.id]);

  // Fetch unread count ban đầu khi user đăng nhập
  const fetchUnreadCount = async () => {
    if (!user) return;
    try {
      const res = await getUnreadNotificationCountApi();
      if (res?.success && res.data) {
        setUnreadCount(res.data.count);
      } else {
        console.error(
          'Failed to fetch unread notification count:',
          res?.message ?? 'Invalid API response'
        );
      }
    } catch (err) {
      console.error('Failed to fetch unread notification count:', err);
    }
  };

  useEffect(() => {
    if (user) {
      fetchUnreadCount();
    }
  }, [user]);

  // Subscribe websocket
  useEffect(() => {
    if (!user) return;

    const unsubscribe = subscribeNotifications((envelope) => {
      const event = envelope?.event;
      if (event && event !== 'notification:new') return;

      const payload = envelope?.data;
      if (!payload || !payload.id) return;
      if (notificationIdsRef.current.has(payload.id)) return;

      notificationIdsRef.current.add(payload.id);
      setNotifications((prev) => [payload, ...prev]);
      setUnreadCount((prev) => prev + 1);
    });

    return unsubscribe;
  }, [user]);

  const loadInitialNotifications = async () => {
    if (!user) return;
    setIsLoadingFirstPage(true);
    setHasError(false);
    try {
      const res = await getNotificationsApi({ page: 1, size: 10 });
      if (res?.success && res.data) {
        const items = res.data.content;
        notificationIdsRef.current = new Set(items.map((item) => item.id));
        setNotifications(items);
        setPage(res.data.page);
        setIsLast(res.data.isLast);
      } else {
        setHasError(true);
      }
    } catch (err) {
      console.error('Failed to fetch notifications:', err);
      setHasError(true);
    } finally {
      setIsLoadingFirstPage(false);
    }
  };

  const loadMoreNotifications = async () => {
    if (!user || isLoadingMore || isLast) return { success: false };
    setIsLoadingMore(true);
    setHasError(false);
    const nextPage = page + 1;
    try {
      const res = await getNotificationsApi({ page: nextPage, size: 10 });
      if (res?.success && res.data) {
        const newItems = res.data.content.filter(
          (item) => !notificationIdsRef.current.has(item.id)
        );
        newItems.forEach((item) => notificationIdsRef.current.add(item.id));
        setNotifications((prev) => [...prev, ...newItems]);
        setPage(res.data.page);
        setIsLast(res.data.isLast);
        return { success: true };
      } else {
        setHasError(true);
        return { success: false };
      }
    } catch (err) {
      console.error('Failed to fetch more notifications:', err);
      setHasError(true);
      return { success: false };
    } finally {
      setIsLoadingMore(false);
    }
  };

  const markAsRead = async (id) => {
    const notification = notifications.find((item) => item.id === id);
    if (!notification || notification.read || markingReadIdsRef.current.has(id)) {
      return { success: true };
    }

    markingReadIdsRef.current.add(id);
    try {
      const res = await markNotificationReadApi(id);
      if (res?.success) {
        setNotifications((prev) =>
          prev.map((item) => (item.id === id ? { ...item, read: true } : item))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
        return { success: true };
      } else {
        console.error(
          'Failed to mark notification as read:',
          res?.message ?? 'Invalid API response'
        );
        return { success: false, message: res?.message };
      }
    } catch (err) {
      console.error('Failed to mark notification as read:', err);
      fetchUnreadCount();
      return { success: false, error: err };
    } finally {
      markingReadIdsRef.current.delete(id);
      // chỉ để theo dõi request đang chạy. Nếu API lỗi, thao tác sau vẫn được thử lại
    }
  };

  const markAllAsRead = async () => {
    try {
      const res = await markAllNotificationsReadApi();
      if (res?.success) {
        setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
        setUnreadCount(0);
        return { success: true };
      } else {
        console.error(
          'Failed to mark all notifications as read:',
          res?.message ?? 'Invalid API response'
        );
        return { success: false, message: res?.message };
      }
    } catch (err) {
      console.error('Failed to mark all notifications as read:', err);
      fetchUnreadCount();
      return { success: false, error: err };
    }
  };

  return (
    <NotificationContext.Provider
      value={{
        unreadCount,
        notifications,
        isLoadingFirstPage,
        isLoadingMore,
        isLast,
        hasError,
        fetchUnreadCount,
        loadInitialNotifications,
        loadMoreNotifications,
        markAsRead,
        markAllAsRead,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotification = () => useContext(NotificationContext);
