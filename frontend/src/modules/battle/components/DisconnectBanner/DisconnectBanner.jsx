import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import './DisconnectBanner.css';

const DisconnectBanner = ({ opponentName, graceMs, at }) => {
  const { t } = useTranslation('battle');

  const calculateSecondsLeft = () => {
    if (!at || !graceMs) return 0;
    const elapsed = Date.now() - at;
    return Math.max(0, Math.ceil((graceMs - elapsed) / 1000));
  };

  const [secondsLeft, setSecondsLeft] = useState(calculateSecondsLeft);

  useEffect(() => {
    setSecondsLeft(calculateSecondsLeft());

    const timer = setInterval(() => {
      const left = calculateSecondsLeft();
      setSecondsLeft(left);
      if (left <= 0) {
        clearInterval(timer);
      }
    }, 250);

    return () => clearInterval(timer);
  }, [at, graceMs]);

  const name = opponentName || t('play.opponentFallback');

  return (
    <div className="battle-disconnect-banner glass-card" role="status">
      <span className="material-symbols-outlined battle-disconnect-banner__icon">
        wifi_off
      </span>
      <div className="battle-disconnect-banner__content text-body-md">
        {secondsLeft > 0 ? (
          <span>
            {t('play.opponentDisconnected', {
              name,
              seconds: secondsLeft,
            })}
          </span>
        ) : (
          <span>{t('play.opponentDisconnectedConfirm')}</span>
        )}
      </div>
    </div>
  );
};

export default DisconnectBanner;

