import { useTranslation } from 'react-i18next';
import OptionsMenu from '../../../../../components/OptionsMenu/OptionsMenu';
import { formatDate, formatRelativeTime } from '../../../../../utils/utils';
import './PostCardHeader.css';

/**
 * @param {Object} author - Thông tin người đăng ({ fullName, username, avatarUrl, id })
 */
function PostCardHeader({
  author,
  group,
  createdAt,
  status,
  onAuthorClick,
  onGroupClick,
  onOptionsClick,
}) {
  const { t, i18n } = useTranslation();

  const formattedDate = formatDate(createdAt);
  const formattedCreatedAt =
    formattedDate === formatDate(new Date())
      ? formatRelativeTime(createdAt, i18n.resolvedLanguage || 'vi')
      : formattedDate || createdAt;

  const normalizedStatus = (status || 'PUBLISHED').toUpperCase();

  const renderStatus = () => {
    switch (normalizedStatus) {
      case 'PENDING':
        return (
          <span className="post-card-header__status post-card-header__status--pending">
            <span>{t('community:status.pending')}</span>
          </span>
        );
      case 'NEEDS_REVIEW':
        return (
          <span className="post-card-header__status post-card-header__status--review">
            <span>{t('community:status.needsReview')}</span>
          </span>
        );
      case 'REJECTED':
        return (
          <span className="post-card-header__status post-card-header__status--rejected">
            <span>{t('community:status.rejected')}</span>
          </span>
        );
      case 'PUBLISHED':
      default:
        return (
          <span className="post-card-header__status post-card-header__status--published">
            <span>{t('community:status.published')}</span>
          </span>
        );
    }
  };

  const authorDisplayName = author?.fullName || t('community:postCard.anonymousUser');

  return (
    <header className="post-card-header">
      <div className="post-card-header__author-group">
        <button type="button" onClick={onAuthorClick} className="post-card-header__avatar-btn">
          <img
            src={
              author?.avatarUrl ||
              `https://ui-avatars.com/api/?name=${encodeURIComponent(authorDisplayName)}`
            }
            alt={authorDisplayName}
            className="post-card-header__avatar"
          />
        </button>

        <div className="post-card-header__meta">
          <div className="post-card-header__names">
            <button type="button" onClick={onAuthorClick} className="post-card-header__name-btn">
              {authorDisplayName}
            </button>

            {group && (
              <>
                <span className="post-card-header__dot">•</span>
                <button
                  type="button"
                  onClick={onGroupClick}
                  className="post-card-header__group-pill"
                >
                  {group}
                </button>
              </>
            )}
          </div>

          {author?.username && <div className="post-card-header__username">@{author.username}</div>}

          <div className="post-card-header__submeta">
            <span>{formattedCreatedAt}</span>
            <span>·</span>
            {renderStatus()}
          </div>
        </div>
      </div>

      {onOptionsClick && <OptionsMenu onClick={onOptionsClick} />}
    </header>
  );
}

export default PostCardHeader;
