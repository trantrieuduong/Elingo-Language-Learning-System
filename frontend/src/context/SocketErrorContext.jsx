import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { subscribeTopic } from '../services/stompClient';
import Toast from '../components/Toast/Toast';

const SocketErrorContext = createContext(null);

export const SocketErrorProvider = ({ children }) => {
  const { t } = useTranslation('api');
  const [errorMessage, setErrorMessage] = useState('');

  const handleDismiss = useCallback(() => setErrorMessage(''), []);

  useEffect(() => {
    const unsubscribe = subscribeTopic('/user/queue/errors', (envelope) => {
      const errorData = envelope?.data ?? envelope;
      const code = errorData?.code;
      const fallbackMsg = errorData?.message || t('common.fallback');
      const displayMsg = code ? t(`error.${code}`, { defaultValue: fallbackMsg }) : fallbackMsg;
      setErrorMessage(displayMsg);
    });

    return unsubscribe;
  }, [t]);

  return (
    <SocketErrorContext.Provider value={{ setErrorMessage }}>
      {children}
      <Toast message={errorMessage} variant="error" onDismiss={handleDismiss} />
    </SocketErrorContext.Provider>
  );
};

export const useSocketError = () => useContext(SocketErrorContext);
