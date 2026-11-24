import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNotification } from '../../context/NotificationContext';
import NotificationItem from '../NotificationItem/NotificationItem';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import Toast from '../../../../components/Toast/Toast';
import './NotificationPanel.css';

function NotificationPanel({ onClose, onNavigate }) {
  const { t } = useTranslation(['notification', 'common', 'api']);
  const [toastMessage, setToastMessage] = useState('');
  const {
    notifications,
    unreadCount,
    isLoadingFirstPage,
    isLoadingMore,
    isLast,
    hasError,
    loadMoreNotifications,
    loadInitialNotifications,
    markAsRead,
    markAllAsRead,
  } = useNotification();

  const handleItemClick = async (item) => {
    if (!item.read) {
      const res = await markAsRead(item.id);
      if (res && res.success === false) {
        setToastMessage(t('api:common.UNKNOWN_ERROR'));
      }
    }

    if (onClose) onClose();

    if (!onNavigate) return;

    if (item.refType === 'POST' && item.refId) {
      onNavigate(`/community/posts/${item.refId}`);
    } else if (item.refType === 'SUBSCRIPTION') {
      onNavigate('/premium/my-subscription');
    } else if (item.refType === 'BADGE') {
      onNavigate('/progress');
    } else if (item.refType === 'REPORT') {
      onNavigate('/reports');
    }
  };

  const handleMarkAsRead = async (id) => {
    const res = await markAsRead(id);
    if (res && res.success === false) {
      setToastMessage(t('api:common.UNKNOWN_ERROR'));
    }
  };

  const handleMarkAllRead = async () => {
    const res = await markAllAsRead();
    if (res && res.success === false) {
      setToastMessage(t('api:common.UNKNOWN_ERROR'));
    }
  };

  const handleLoadMore = async () => {
    const res = await loadMoreNotifications();
    if (res && res.success === false) {
      setToastMessage(t('api:common.UNKNOWN_ERROR'));
    }
  };

  return (
    <>
      <div className="notification-panel glass-card">
      <div className="notification-panel__header">
        <div className="notification-panel__title-group">
          <span className="text-title-md notification-panel__title">{t('title')}</span>
          {unreadCount > 0 && (
            <span className="badge badge--primary notification-panel__badge">
              {unreadCount > 99 ? '99+' : unreadCount}
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            type="button"
            className="notification-panel__read-all-btn"
            onClick={handleMarkAllRead}
          >
            {t('markAllAsRead')}
          </button>
        )}
      </div>

      <div className="notification-panel__list">
        {isLoadingFirstPage && notifications.length === 0 ? (
          <div className="notification-panel__loading">
            <PageLoading />
          </div>
        ) : hasError && notifications.length === 0 ? (
          <div className="notification-panel__error">
            <EmptyState
              icon="error"
              isError
              title={t('loadErrorTitle')}
              description={t('common:loadError')}
              actionLabel={t('common:retry')}
              onAction={loadInitialNotifications}
              variant="compact"
            />
          </div>
        ) : notifications.length === 0 ? (
          <div className="notification-panel__empty">
            <EmptyState
              icon="notifications_off"
              title={t('empty')}
              description={t('emptyDesc')}
              variant="compact"
            />
          </div>
        ) : (
          <>
            {notifications.map((item) => (
              <NotificationItem
                key={item.id}
                item={item}
                onSelect={handleItemClick}
                onMarkAsRead={handleMarkAsRead}
              />
            ))}

            {!isLast && (
              <div className="notification-panel__footer">
                <button
                  type="button"
                  className="btn-secondary notification-panel__load-more-btn"
                  onClick={handleLoadMore}
                  disabled={isLoadingMore}
                >
                  {isLoadingMore ? t('loading') : t('loadMore')}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>

    {toastMessage && (
      <Toast
        message={toastMessage}
        variant="error"
        onDismiss={() => setToastMessage('')}
      />
    )}
  </>
  );
}

export default NotificationPanel;
