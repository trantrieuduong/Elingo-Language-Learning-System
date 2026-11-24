import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import NotificationPanel from '@/modules/notification/components/NotificationPanel/NotificationPanel';
import { useNotification } from '@/modules/notification/context/NotificationContext';
import { MOCK_NOTIFICATION_CONTEXT_DEFAULT } from '../../../utils/mockData';
import { tr, LANGS, useLang } from '../../../utils/i18n';

vi.mock('@/modules/notification/context/NotificationContext');

describe('NotificationPanel', () => {
  const mockMarkAsRead = vi.fn();
  const mockMarkAllAsRead = vi.fn();
  const mockLoadInitialNotifications = vi.fn();
  const mockLoadMoreNotifications = vi.fn();

  const setupMockNotification = (overrides = {}) => {
    useNotification.mockReturnValue({
      ...MOCK_NOTIFICATION_CONTEXT_DEFAULT,
      markAsRead: mockMarkAsRead,
      markAllAsRead: mockMarkAllAsRead,
      loadInitialNotifications: mockLoadInitialNotifications,
      loadMoreNotifications: mockLoadMoreNotifications,
      ...overrides,
    });
  };

  beforeEach(() => {
    vi.clearAllMocks();
    setupMockNotification();
  });

  describe.each(LANGS)('i18n header and empty/loading text in %s', (lng) => {
    it('renders panel title and empty state text when there are no notifications', async () => {
      await useLang(lng);
      setupMockNotification({
        notifications: [],
        unreadCount: 0,
      });

      render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);

      expect(screen.getByText(tr('notification:title'))).toBeInTheDocument();
      expect(screen.getByText(tr('notification:empty'))).toBeInTheDocument();
      expect(screen.getByText(tr('notification:emptyDesc'))).toBeInTheDocument();
    });
  });

  it('renders unread count badge and "Mark all as read" button when unreadCount > 0', async () => {
    await useLang('vi');
    setupMockNotification({
      unreadCount: 5,
      notifications: [
        {
          id: 1,
          type: 'POST_LIKED',
          refType: 'POST',
          refId: 10,
          title: 'Post Liked',
          params: { actor: 'User A', content: 'Post body' },
          read: false,
          createdAt: new Date().toISOString(),
        },
      ],
    });

    render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);

    expect(screen.getByText('5')).toBeInTheDocument();

    const markAllBtn = screen.getByRole('button', {
      name: tr('notification:markAllAsRead'),
    });
    expect(markAllBtn).toBeInTheDocument();

    fireEvent.click(markAllBtn);
    expect(mockMarkAllAsRead).toHaveBeenCalledTimes(1);
  });

  it('renders "99+" badge when unreadCount > 99', () => {
    setupMockNotification({
      unreadCount: 120,
      notifications: [],
    });

    render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);
    expect(screen.getByText('99+')).toBeInTheDocument();
  });

  it('renders loading spinner when isLoadingFirstPage is true and notifications list is empty', () => {
    setupMockNotification({
      isLoadingFirstPage: true,
      notifications: [],
    });

    const { container } = render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);
    expect(container.querySelector('.notification-panel__loading')).toBeInTheDocument();
  });

  it('renders error state and retry button when hasError is true and notifications list is empty', async () => {
    await useLang('vi');
    setupMockNotification({
      hasError: true,
      notifications: [],
    });

    render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);

    expect(screen.getByText(tr('notification:loadErrorTitle'))).toBeInTheDocument();
    const retryBtn = screen.getByRole('button', { name: tr('common:retry') });
    expect(retryBtn).toBeInTheDocument();

    fireEvent.click(retryBtn);
    expect(mockLoadInitialNotifications).toHaveBeenCalledTimes(1);
  });

  describe('Loading more notifications (loadMoreNotifications)', () => {
    it('renders "Load more" button when isLast is false and triggers loadMoreNotifications on click', async () => {
      await useLang('vi');
      setupMockNotification({
        isLast: false,
        isLoadingMore: false,
        notifications: [
          {
            id: 1,
            type: 'POST_LIKED',
            refType: 'POST',
            refId: 10,
            title: 'Item 1',
            params: { actor: 'User 1' },
            read: true,
            createdAt: new Date().toISOString(),
          },
        ],
      });

      render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);

      const loadMoreBtn = screen.getByRole('button', {
        name: tr('notification:loadMore'),
      });
      expect(loadMoreBtn).toBeInTheDocument();
      expect(loadMoreBtn).not.toBeDisabled();

      fireEvent.click(loadMoreBtn);
      expect(mockLoadMoreNotifications).toHaveBeenCalledTimes(1);
    });

    it('disables load more button and displays loading text when isLoadingMore is true', async () => {
      await useLang('vi');
      setupMockNotification({
        isLast: false,
        isLoadingMore: true,
        notifications: [
          {
            id: 1,
            type: 'POST_LIKED',
            refType: 'POST',
            refId: 10,
            title: 'Item 1',
            params: { actor: 'User 1' },
            read: true,
            createdAt: new Date().toISOString(),
          },
        ],
      });

      render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);

      const loadingBtn = screen.getByRole('button', {
        name: tr('notification:loading'),
      });
      expect(loadingBtn).toBeInTheDocument();
      expect(loadingBtn).toBeDisabled();
    });

    it('does not render "Load more" button when isLast is true', () => {
      setupMockNotification({
        isLast: true,
        notifications: [
          {
            id: 1,
            type: 'POST_LIKED',
            refType: 'POST',
            refId: 10,
            title: 'Item 1',
            params: { actor: 'User 1' },
            read: true,
            createdAt: new Date().toISOString(),
          },
        ],
      });

      render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);
      expect(document.querySelector('.notification-panel__load-more-btn')).toBeNull();
    });

    it('show Toast alert UNKNOWN_ERROR when clicking load more when server crack', async () => {
      await useLang('vi');
      mockLoadMoreNotifications.mockResolvedValueOnce({ success: false });

      setupMockNotification({
        isLast: false,
        isLoadingMore: false,
        notifications: [
          {
            id: 1,
            type: 'POST_LIKED',
            refType: 'POST',
            refId: 10,
            title: 'Item 1',
            params: { actor: 'User 1' },
            read: true,
            createdAt: new Date().toISOString(),
          },
        ],
      });

      render(<NotificationPanel onClose={vi.fn()} onNavigate={vi.fn()} />);

      const loadMoreBtn = screen.getByRole('button', {
        name: tr('notification:loadMore'),
      });

      fireEvent.click(loadMoreBtn);

      await waitFor(() => {
        expect(screen.getByText(tr('api:common.UNKNOWN_ERROR'))).toBeInTheDocument();
      });
    });
  });

  describe('Item click navigation and mark as read', () => {
    it('marks item as read, closes panel, and navigates to community post when unread POST item is clicked', async () => {
      const mockClose = vi.fn();
      const mockNavigate = vi.fn();
      const unreadPostItem = {
        id: 42,
        type: 'POST_LIKED',
        refType: 'POST',
        refId: 999,
        title: 'Post Liked',
        params: { actor: 'User A', content: 'Post content' },
        read: false,
        createdAt: new Date().toISOString(),
      };

      setupMockNotification({
        notifications: [unreadPostItem],
      });

      const { container } = render(
        <NotificationPanel onClose={mockClose} onNavigate={mockNavigate} />
      );

      const itemEl = container.querySelector('.notification-item');
      fireEvent.click(itemEl);

      await waitFor(() => {
        expect(mockMarkAsRead).toHaveBeenCalledWith(42);
        expect(mockClose).toHaveBeenCalledTimes(1);
        expect(mockNavigate).toHaveBeenCalledWith('/community/posts/999');
      });
    });

    it('navigates to /premium/my-subscription when clicking SUBSCRIPTION item', async () => {
      const mockClose = vi.fn();
      const mockNavigate = vi.fn();
      const subItem = {
        id: 43,
        type: 'SUBSCRIPTION_ACTIVATED',
        refType: 'SUBSCRIPTION',
        refId: null,
        title: 'Sub',
        params: { planName: 'VIP', expiresAt: '2026-12-31' },
        read: true,
        createdAt: new Date().toISOString(),
      };

      setupMockNotification({
        notifications: [subItem],
      });

      const { container } = render(
        <NotificationPanel onClose={mockClose} onNavigate={mockNavigate} />
      );

      fireEvent.click(container.querySelector('.notification-item'));

      await waitFor(() => {
        expect(mockMarkAsRead).not.toHaveBeenCalled();
        expect(mockClose).toHaveBeenCalledTimes(1);
        expect(mockNavigate).toHaveBeenCalledWith('/premium/my-subscription');
      });
    });

    it('navigates to /progress when clicking BADGE item', async () => {
      const mockNavigate = vi.fn();
      const badgeItem = {
        id: 44,
        type: 'BADGE_EARNED',
        refType: 'BADGE',
        refId: null,
        title: 'Badge',
        params: { badgeName: 'Hero' },
        read: true,
        createdAt: new Date().toISOString(),
      };

      setupMockNotification({
        notifications: [badgeItem],
      });

      const { container } = render(
        <NotificationPanel onClose={vi.fn()} onNavigate={mockNavigate} />
      );

      fireEvent.click(container.querySelector('.notification-item'));

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/progress');
      });
    });

    it('navigates to /reports when clicking REPORT item', async () => {
      const mockNavigate = vi.fn();
      const reportItem = {
        id: 45,
        type: 'REPORT_RESOLVED',
        refType: 'REPORT',
        refId: null,
        title: 'Report',
        params: {},
        read: true,
        createdAt: new Date().toISOString(),
      };

      setupMockNotification({
        notifications: [reportItem],
      });

      const { container } = render(
        <NotificationPanel onClose={vi.fn()} onNavigate={mockNavigate} />
      );

      fireEvent.click(container.querySelector('.notification-item'));

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/reports');
      });
    });
  });
});
