import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import './Toast.css';

const TOAST_VARIANTS = {
  success: {
    className: 'toast--success',
    icon: 'check_circle',
  },
  error: {
    className: 'toast--error',
    icon: 'error',
  },
};

function Toast({ message, variant = 'success', onDismiss, duration = 3000 }) {
  const toastVariant = TOAST_VARIANTS[variant] || TOAST_VARIANTS.success;

  useEffect(() => {
    if (!message || !onDismiss) return undefined;

    const timer = setTimeout(onDismiss, duration);
    return () => clearTimeout(timer);
  }, [message, onDismiss, duration]);

  if (!message) return null;

  return createPortal(
    <div className={`toast ${toastVariant.className} glass-card`} role="alert">
      <span className="material-symbols-outlined toast__icon">{toastVariant.icon}</span>
      <span className="toast__message">{message}</span>
    </div>,
    document.body
  );
}

export default Toast;
