import { useState } from 'react';
import './FlipCard.css';

function FlipCard({ front, back, className = '', frontClassName = '', backClassName = '' }) {
  const [isFlipped, setIsFlipped] = useState(false);

  const handleKeyDown = (event) => {
    if (event.target !== event.currentTarget) return;

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      setIsFlipped((current) => !current);
    }
  };

  return (
    <div className={`flip-card ${className}`.trim()}>
      <div
        className={`flip-card__inner${isFlipped ? ' flip-card__inner--flipped' : ''}`}
        tabIndex={0}
        onClick={() => setIsFlipped((current) => !current)}
        onKeyDown={handleKeyDown}
      >
        <div className={`flip-card__face ${frontClassName}`.trim()}>{front}</div>
        <div className={`flip-card__face ${backClassName}`.trim()}>{back}</div>
      </div>
    </div>
  );
}

export default FlipCard;
