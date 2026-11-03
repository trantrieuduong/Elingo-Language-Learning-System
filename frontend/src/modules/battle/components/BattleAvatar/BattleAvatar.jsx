import './BattleAvatar.css';

const BattleAvatar = ({ name = '', avatarUrl, size = 'md' }) => {
  const getInitials = (str) => {
    if (!str || typeof str !== 'string') return '?';
    const trimmed = str.trim();
    if (!trimmed) return '?';
    const parts = trimmed.split(/\s+/);
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  const sizeClass = `battle-avatar--${size}`;

  if (avatarUrl) {
    return (
      <img
        src={avatarUrl}
        alt={name || 'Avatar'}
        className={`battle-avatar ${sizeClass}`}
      />
    );
  }

  return (
    <div
      className={`battle-avatar ${sizeClass} battle-avatar--fallback`}
      aria-label={name || 'Avatar'}
    >
      <span className="battle-avatar__initials">{getInitials(name)}</span>
    </div>
  );
};

export default BattleAvatar;

