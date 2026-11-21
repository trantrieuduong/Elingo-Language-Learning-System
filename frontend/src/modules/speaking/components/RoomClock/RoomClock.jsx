import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatDuration } from '../../utils/formatDuration';

const RoomClock = ({ joinedAtMs }) => {
  const { t } = useTranslation('speaking');
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    setNow(Date.now());
    if (joinedAtMs == null) return undefined;
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, [joinedAtMs]);

  if (joinedAtMs == null) return <span>{t('room.clock.waiting')}</span>;

  return (
    <span aria-label={t('room.clock.label')}>
      {formatDuration(
        (now - joinedAtMs) / 1000
      )}
    </span>
  );
};

export default RoomClock;
