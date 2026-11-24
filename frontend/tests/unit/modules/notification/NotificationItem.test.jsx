import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import NotificationItem from '@/modules/notification/components/NotificationItem/NotificationItem';
import { tr, LANGS, useLang } from '../../../utils/i18n';

describe('NotificationItem', () => {
  const baseItem = {
    id: 101,
    type: 'POST_LIKED',
    refType: 'POST',
    refId: 1,
    title: 'Default title',
    params: {
      actor: 'John Doe',
      content: 'Hello world post',
    },
    read: false,
    createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
  };

  describe.each(LANGS)('i18n text and title/time rendering in %s', (lng) => {
    it('renders translated title, content, and relative timestamp', async () => {
      await useLang(lng);
      render(
        <NotificationItem
          item={baseItem}
          onSelect={vi.fn()}
          onMarkAsRead={vi.fn()}
        />
      );

      const expectedTitle = tr('notification:POST_LIKED.title', { actor: 'John Doe' });
      expect(screen.getByText(expectedTitle)).toBeInTheDocument();
      expect(screen.getByText('Hello world post')).toBeInTheDocument();

      const timeEl = document.querySelector('.notification-item__time');
      expect(timeEl).toBeInTheDocument();
      expect(timeEl.textContent.trim().length).toBeGreaterThan(0);
    });
  });

  it('renders fallback title (defaultValue) when translation key is not found', () => {
    const itemWithCustomType = {
      ...baseItem,
      type: 'UNKNOWN_TYPE',
      title: 'Custom fallback title',
    };

    render(
      <NotificationItem
        item={itemWithCustomType}
        onSelect={vi.fn()}
        onMarkAsRead={vi.fn()}
      />
    );

    expect(screen.getByText('Custom fallback title')).toBeInTheDocument();
  });

  describe('Content rendering by refType', () => {
    it('renders content when refType is SUBSCRIPTION', async () => {
      await useLang('vi');
      const item = {
        id: 102,
        type: 'SUBSCRIPTION_ACTIVATED',
        refType: 'SUBSCRIPTION',
        refId: null,
        title: 'Premium',
        params: {
          planName: 'Pro 1 Month',
          expiresAt: '2026-12-31',
        },
        read: true,
        createdAt: new Date().toISOString(),
      };

      render(<NotificationItem item={item} onSelect={vi.fn()} onMarkAsRead={vi.fn()} />);

      const expectedContent = tr('notification:SUBSCRIPTION_ACTIVATED.content', {
        planName: 'Pro 1 Month',
        expiresAt: '2026-12-31',
      });
      expect(screen.getByText(expectedContent)).toBeInTheDocument();
    });

    it('renders content when refType is BADGE', async () => {
      await useLang('vi');
      const item = {
        id: 103,
        type: 'BADGE_EARNED',
        refType: 'BADGE',
        refId: null,
        title: 'Badge',
        params: {
          badgeName: 'Top Learner',
        },
        read: true,
        createdAt: new Date().toISOString(),
      };

      render(<NotificationItem item={item} onSelect={vi.fn()} onMarkAsRead={vi.fn()} />);

      const expectedContent = tr('notification:BADGE_EARNED.content', {
        badgeName: 'Top Learner',
      });
      expect(screen.getByText(expectedContent)).toBeInTheDocument();
    });

    it('renders content when refType is REPORT', async () => {
      await useLang('vi');
      const item = {
        id: 104,
        type: 'REPORT_RESOLVED',
        refType: 'REPORT',
        refId: null,
        title: 'Report Title',
        params: {
          description: 'Report description',
        },
        read: true,
        createdAt: new Date().toISOString(),
      };

      render(
        <NotificationItem item={item} onSelect={vi.fn()} onMarkAsRead={vi.fn()} />
      );

      const expectedContent = tr('notification:REPORT_RESOLVED.content', {
        description: 'Report description'
      });
      expect(screen.getByText(expectedContent)).toBeInTheDocument();
    });
  });

  describe('Icon mapping by notification type', () => {
    const iconCases = [
      { type: 'POST_LIKED', icon: 'thumb_up' },
      { type: 'POST_COMMENTED', icon: 'comment' },
      { type: 'COMMENT_REPLIED', icon: 'reply' },
      { type: 'BADGE_EARNED', icon: 'military_tech' },
      { type: 'SUBSCRIPTION_ACTIVATED', icon: 'workspace_premium' },
      { type: 'SUBSCRIPTION_EXPIRING', icon: 'timer' },
      { type: 'REPORT_RESOLVED', icon: 'gavel' },
      { type: 'OTHER_TYPE', icon: 'notifications' },
    ];

    it.each(iconCases)('renders icon "$icon" for type $type', ({ type, icon }) => {
      const item = {
        ...baseItem,
        type,
      };
      const { container } = render(
        <NotificationItem item={item} onSelect={vi.fn()} onMarkAsRead={vi.fn()} />
      );

      const iconEl = container.querySelector('.notification-item__icon');
      expect(iconEl).toHaveTextContent(icon);
    });
  });

  describe('Mark as read and detail selection interactions', () => {
    it('renders unread class and mark-as-read button when unread', () => {
      const onMarkAsRead = vi.fn();
      const onSelect = vi.fn();

      const { container } = render(
        <NotificationItem
          item={{ ...baseItem, read: false }}
          onSelect={onSelect}
          onMarkAsRead={onMarkAsRead}
        />
      );

      expect(container.firstChild).toHaveClass('notification-item--unread');

      const markReadBtn = container.querySelector('.notification-item__mark-read-btn');
      expect(markReadBtn).toBeInTheDocument();

      fireEvent.click(markReadBtn);
      expect(onMarkAsRead).toHaveBeenCalledTimes(1);
      expect(onMarkAsRead).toHaveBeenCalledWith(baseItem.id);
      expect(onSelect).not.toHaveBeenCalled();
    });

    it('does not render unread class or mark-as-read button when already read', () => {
      const { container } = render(
        <NotificationItem
          item={{ ...baseItem, read: true }}
          onSelect={vi.fn()}
          onMarkAsRead={vi.fn()}
        />
      );

      expect(container.firstChild).not.toHaveClass('notification-item--unread');
      expect(container.querySelector('.notification-item__mark-read-btn')).toBeNull();
    });

    it('triggers onSelect(item) when clicking on the item body', () => {
      const onSelect = vi.fn();

      const { container } = render(
        <NotificationItem
          item={baseItem}
          onSelect={onSelect}
          onMarkAsRead={vi.fn()}
        />
      );

      fireEvent.click(container.firstChild);
      expect(onSelect).toHaveBeenCalledTimes(1);
      expect(onSelect).toHaveBeenCalledWith(baseItem);
    });
  });
});
