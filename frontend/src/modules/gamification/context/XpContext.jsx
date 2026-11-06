import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { subscribeXp } from '../xpSocket';
import Toast from '../../../components/Toast/Toast';

const XpContext = createContext({ lastXp: null });

export const XpProvider = ({ children }) => {
  const { t } = useTranslation('gamification');
  const [lastXp, setLastXp] = useState(null); // { amount, at }
  const [toastMessage, setToastMessage] = useState('');

  const handleDismiss = useCallback(() => setToastMessage(''), []);

  useEffect(() => {
    const unsubscribe = subscribeXp((payload) => {
      const amount = Number(payload?.xpGained);
      if (!Number.isFinite(amount) || amount <= 0) return;
      setLastXp({ amount, at: Date.now() });
      setToastMessage(t('xpToast', { xp: amount }));
    });

    return unsubscribe;
  }, [t]);

  return (
    <XpContext.Provider value={{ lastXp }}>
      {children}
      <Toast message={toastMessage} variant="success" onDismiss={handleDismiss} />
    </XpContext.Provider>
  );
};

export const useXp = () => useContext(XpContext);

