import { useState } from 'react';
import './SpeakingAvatar.css';

const SpeakingAvatar = ({ name = '', src = '', size = 40, className = '' }) => {
  const [imgError, setImgError] = useState(false);

  const initial = (name || '').trim().charAt(0).toUpperCase() || '?';

  return (
    <div
      className={`speaking-avatar ${className}`.trim()}
      style={{ width: `${size}px`, height: `${size}px`, minWidth: `${size}px` }}
      aria-hidden="true"
    >
      {src && !imgError ? (
        <img
          src={src}
          alt={name}
          className="speaking-avatar__img"
          onError={() => setImgError(true)}
        />
      ) : (
        <span className="speaking-avatar__initial">{initial}</span>
      )}
    </div>
  );
};

export default SpeakingAvatar;
