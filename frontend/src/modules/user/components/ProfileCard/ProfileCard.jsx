import { getFallbackAvatarUrl } from '../../../../utils/utils';
import './ProfileCard.css';

/**
 * ProfileCard - Component hiển thị thông tin hồ sơ người dùng
 * @param {Object} props
 * @param {Object} props.profile - Dữ liệu người dùng (id, username, fullName, avatarUrl)
 * @param {React.ReactNode} [props.actionMenu] - Nút/Menu tùy chọn (OptionsMenu)
 */
function ProfileCard({ profile, actionMenu }) {
  if (!profile) return null;

  const { username, fullName, avatarUrl } = profile;

  return (
    <div className="profile-card glass-card">
      <div className="profile-card__header">
        <div className="profile-card__avatar-wrapper">
          <img
            src={avatarUrl || getFallbackAvatarUrl(fullName)}
            alt={fullName}
            className="profile-card__avatar-img"
          />
        </div>

        {actionMenu && (
          <div className="profile-card__action">
            {actionMenu}
          </div>
        )}
      </div>

      <div className="profile-card__info">
        <h1 className="profile-card__fullname text-headline-md">
          {fullName}
        </h1>
        <p className="profile-card__username text-body-md text-disabled">
          @{username}
        </p>
      </div>
    </div>
  );
}

export default ProfileCard;
