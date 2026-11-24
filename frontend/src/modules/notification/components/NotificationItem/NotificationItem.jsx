import { useTranslation } from 'react-i18next';
import { formatRelativeTime } from '../../../../utils/utils';
import './NotificationItem.css';

const getNotificationIcon = (type) => {
  switch (type) {
    case 'POST_LIKED':
      return 'thumb_up';
    case 'POST_COMMENTED':
      return 'comment';
    case 'COMMENT_REPLIED':
      return 'reply';
    case 'BADGE_EARNED':
      return 'military_tech';
    case 'SUBSCRIPTION_ACTIVATED':
      return 'workspace_premium';
    case 'SUBSCRIPTION_EXPIRING':
      return 'timer';
    case 'REPORT_RESOLVED':
      return 'gavel';
    default:
      return 'notifications';
  }
};

function NotificationItem({ item, onSelect, onMarkAsRead }) {
  const { t, i18n } = useTranslation('notification');

  const currentLang = i18n.resolvedLanguage ?? 'vi';

  const title = t(`${item.type}.title`, {
    ...item.params,
    defaultValue: item.title,
  });

  let contentText = '';
  if (item.refType === 'POST') {
    const content = item.params?.content;
    if (content) {
      contentText = content;
    }
  } else if (item.refType === 'SUBSCRIPTION') {
    const planName = item.params.planName;
    const expiresAt = item.params.expiresAt;
    contentText = t(`${item.type}.content`, { planName, expiresAt });
  } else if (item.refType === 'BADGE') {
    const badgeName = item.params.badgeName;
    contentText = t(`${item.type}.content`, { badgeName });
  } else if (item.refType === 'REPORT') {
    const description = item.params.description;
    contentText = t('REPORT_RESOLVED.content', { description });
  }

  const relativeTime = formatRelativeTime(item.createdAt, currentLang);
  const icon = getNotificationIcon(item.type);

  return (
    <div
      className={`notification-item ${!item.read ? 'notification-item--unread' : ''}`}
      onClick={() => onSelect && onSelect(item)}
    >
      <div className="notification-item__icon-wrapper">
        <span className="material-symbols-outlined notification-item__icon">{icon}</span>
      </div>
      <div className="notification-item__content">
        <div className="notification-item__header">
          <span className="notification-item__title text-label-md">{title}</span>
          {!item.read && (
            <div className="notification-item__actions">
              <button
                type="button"
                className="btn-icon notification-item__mark-read-btn"
                title={t('markAsRead')}
                onClick={(e) => {
                  e.stopPropagation();
                  onMarkAsRead(item.id);
                }}
              >
                <span className="material-symbols-outlined">check_circle</span>
              </button>
            </div>
          )}
        </div>
        {contentText ? (
          <p className="notification-item__body text-body-sm">{contentText}</p>
        ) : null}
        {relativeTime && (
          <span className="notification-item__time text-body-xs">{relativeTime}</span>
        )}
      </div>
    </div>
  );
}

export default NotificationItem;
