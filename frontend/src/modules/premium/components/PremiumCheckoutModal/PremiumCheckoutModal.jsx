import { useEffect, useState, useCallback, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import Toast from '../../../../components/Toast/Toast';
import { formatPremiumPrice } from '../../../../utils/utils';
import {
  createPremiumCheckoutApi,
  getPremiumTransactionStatusApi,
} from '../../premiumApi';
import CopyButton from '../../../../components/CopyButton/CopyButton';
import './PremiumCheckoutModal.css';

const POLLING_INTERVAL_MS = 5000;
const POLLING_TIMEOUT_MS = 10 * 60 * 1000; // 10 phút

function CheckoutQrSection({ checkoutData, qrLoadError, onQrError, t }) {
  const hasQr = checkoutData?.qrCodeUrl && !qrLoadError;
  const wrapperClass = hasQr
    ? 'premium-checkout-modal__qr-wrapper--has-qr'
    : 'premium-checkout-modal__qr-wrapper--fallback glass-card';

  return (
    <div className="premium-checkout-modal__qr-col">
      <div className={`premium-checkout-modal__qr-wrapper ${wrapperClass}`}>
        {hasQr ? (
          <img
            src={checkoutData.qrCodeUrl}
            alt="VietQR Payment Code"
            className="premium-checkout-modal__qr-img"
            onError={onQrError}
          />
        ) : (
          <div className="premium-checkout-modal__qr-fallback">
            <span className="material-symbols-outlined">qr_code_2</span>
            <p className="text-body-sm">{t('checkout.qrFallback')}</p>
          </div>
        )}
      </div>

      <div className="premium-checkout-modal__status-banner">
        <span className="premium-checkout-modal__spinner premium-checkout-modal__spinner--small" />
        <span className="text-body-sm text-mid">{t('checkout.statusPending')}</span>
      </div>
      <p className="premium-checkout-modal__hint text-label-sm text-disabled">
        {t('checkout.waitingHint')}
      </p>
    </div>
  );
}

function CheckoutInfoSection({
  plan,
  checkoutData,
  amountDisplay,
  onCopySuccess,
  onCopyError,
  t,
}) {
  const fields = [
    {
      label: t('checkout.transferContent'),
      value: checkoutData?.providerRefCode,
      isHighlight: true,
      isRef: true,
    },
    {
      label: t('checkout.bankAccount'),
      value: checkoutData?.bankAccount,
    },
    {
      label: t('checkout.accountName'),
      value: checkoutData?.bankAccountName,
    },
    {
      label: t('checkout.bankName'),
      value: checkoutData?.bankName,
    },
  ];

  return (
    <div className="premium-checkout-modal__info-col">
      <div className="premium-checkout-modal__summary-card glass-thumb">
        <div className="premium-checkout-modal__summary-row">
          <span className="text-body-sm text-disabled">{t('checkout.planName')}</span>
          <strong className="text-body-md text-high">{plan?.name}</strong>
        </div>
        <div className="premium-checkout-modal__summary-row">
          <span className="text-body-sm text-disabled">{t('checkout.amount')}</span>
          <div className="premium-checkout-modal__amount-group">
            <strong className="premium-checkout-modal__amount text-headline-sm">
              {amountDisplay}
            </strong>
            <CopyButton
              text={checkoutData?.amount}
              onCopy={onCopySuccess}
              onError={onCopyError}
            />
          </div>
        </div>
      </div>

      <div className="premium-checkout-modal__transfer-details">
        <h3 className="text-title-md text-high">{t('checkout.transferInfo')}</h3>

        {fields.map((f) => (
          <div
            key={f.label}
            className={`premium-checkout-modal__field glass-thumb ${
              f.isHighlight ? 'premium-checkout-modal__field--highlight' : ''
            }`}
          >
            <div className="premium-checkout-modal__field-text">
              <span className="text-label-sm text-body">{f.label}</span>
              <span
                className={`premium-checkout-modal__field-val ${
                  f.isRef ? 'premium-checkout-modal__field-val--ref' : ''
                }`}
              >
                {f.value}
              </span>
            </div>
            <CopyButton
              text={f.value}
              onCopy={onCopySuccess}
              onError={onCopyError}
            />
          </div>
        ))}

        {checkoutData?.transactionId && (
          <div className="premium-checkout-modal__transaction-id text-label-sm text-disabled">
            {t('checkout.transactionId')}: #{checkoutData.transactionId}
          </div>
        )}
      </div>
    </div>
  );
}

function CheckoutResultSection({ paymentStatus, plan, amountDisplay, onRetry, onSuccessCta, t }) {
  if (paymentStatus === 'SUCCESS') {
    return (
      <div className="premium-checkout-modal__success glass-thumb">
        <span className="material-symbols-outlined premium-checkout-modal__success-icon">
          check_circle
        </span>
        <h3 className="text-headline-sm">{t('checkout.statusSuccess')}</h3>
        <p className="text-body-md text-disabled">{plan?.name} • {amountDisplay}</p>
        <button
          type="button"
          className="btn-primary premium-checkout-modal__success-btn"
          onClick={onSuccessCta}
        >
          {t('checkout.successCta')}
        </button>
      </div>
    );
  }

  const isTimeout = paymentStatus === 'TIMEOUT';
  const isRefunded = paymentStatus === 'REFUNDED';
  const icon = isTimeout ? 'timer_off' : 'cancel';

  const description = isTimeout
    ? t('checkout.statusTimeout')
    : isRefunded
    ? t('checkout.statusRefunded')
    : t('checkout.statusFailed');

  return (
    <EmptyState
      icon={icon}
      isError
      description={description}
      actionLabel={t('common:retry')}
      onAction={onRetry}
    />
  );
}

function CheckoutModalContent({
  isLoading,
  checkoutError,
  paymentStatus,
  plan,
  checkoutData,
  amountDisplay,
  qrLoadError,
  onQrError,
  onRetry,
  onSuccessCta,
  onCopySuccess,
  onCopyError,
  t,
}) {
  if (isLoading) {
    return (
      <div className="premium-checkout-modal__loading">
        <span className="premium-checkout-modal__spinner" />
        <p className="text-body-md text-disabled">{t('checkout.statusPending')}</p>
      </div>
    );
  }

  if (checkoutError) {
    return (
      <EmptyState
        icon="error"
        isError
        description={checkoutError}
        actionLabel={t('common:retry')}
        onAction={onRetry}
      />
    );
  }

  if (paymentStatus !== 'PENDING') {
    return (
      <CheckoutResultSection
        paymentStatus={paymentStatus}
        plan={plan}
        amountDisplay={amountDisplay}
        onRetry={onRetry}
        onSuccessCta={onSuccessCta}
        t={t}
      />
    );
  }

  return (
    <div className="premium-checkout-modal__grid">
      <CheckoutQrSection
        checkoutData={checkoutData}
        qrLoadError={qrLoadError}
        onQrError={onQrError}
        t={t}
      />
      <CheckoutInfoSection
        plan={plan}
        checkoutData={checkoutData}
        amountDisplay={amountDisplay}
        onCopySuccess={onCopySuccess}
        onCopyError={onCopyError}
        t={t}
      />
    </div>
  );
}

function PremiumCheckoutModal({ plan, onClose, onSuccess, onNavigate }) {
  const { t, i18n } = useTranslation(['premium', 'common']);

  const [checkoutData, setCheckoutData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [checkoutError, setCheckoutError] = useState(null);
  const [paymentStatus, setPaymentStatus] = useState('PENDING'); // PENDING | SUCCESS | FAILED | REFUNDED | TIMEOUT
  const [qrLoadError, setQrLoadError] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  const pollingRef = useRef(null);
  const timeoutRef = useRef(null);
  const isMountedRef = useRef(true);

  const clearTimers = useCallback(() => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  // Xử lý phím ESC & khóa scroll
  useEffect(() => {
    isMountedRef.current = true;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        onClose?.();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      isMountedRef.current = false;
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = originalOverflow;
      clearTimers();
    };
  }, [onClose, clearTimers]);

  const initializeCheckout = useCallback(async () => {
    if (!plan?.id) return;

    setIsLoading(true);
    setCheckoutError(null);
    setQrLoadError(false);
    setPaymentStatus('PENDING');

    try {
      const res = await createPremiumCheckoutApi(plan.id);
      if (!isMountedRef.current) return;
      if (res.success && res.data) {
        setCheckoutData(res.data);
      } else {
        setCheckoutError(res.message ?? t('checkout.createError'));
      }
    } catch {
      if (!isMountedRef.current) return;
      setCheckoutError(t('checkout.createError'));
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  }, [plan?.id, t]);

  useEffect(() => {
    initializeCheckout();
  }, [initializeCheckout]);

  // Polling trạng thái giao dịch & Timeout
  useEffect(() => {
    const transactionId = checkoutData?.transactionId;
    const isTerminalStatus = paymentStatus !== 'PENDING';

    if (!transactionId || isTerminalStatus) {
      clearTimers();
      return;
    }

    const checkStatus = async () => {
      try {
        const res = await getPremiumTransactionStatusApi(transactionId);
        if (!isMountedRef.current) return;
        if (res.success && res.data) {
          const status = res.data.status;
          if (status && status !== 'PENDING') {
            setPaymentStatus(status);
            if (status === 'SUCCESS') {
              onSuccess?.(res.data);
            }
          }
        }
      } catch {
        // Lỗi polling được bỏ qua, sẽ thử lại ở lần kế tiếp
      }
    };

    pollingRef.current = setInterval(checkStatus, POLLING_INTERVAL_MS);

    timeoutRef.current = setTimeout(() => {
      if (!isMountedRef.current) return;
      clearTimers();
      setPaymentStatus('TIMEOUT');
    }, POLLING_TIMEOUT_MS);

    return () => {
      clearTimers();
    };
  }, [checkoutData?.transactionId, paymentStatus, onSuccess, clearTimers]);

  const amountDisplay = formatPremiumPrice(
    checkoutData?.amount,
    checkoutData?.currency,
    i18n.resolvedLanguage
  );

  const handleCopySuccess = () => {
    setToastMessage(t('checkout.copied'));
  };

  const handleCopyError = () => {
    setToastMessage(t('checkout.copyError'));
  };

  const handleSuccessCta = () => {
    onClose?.();
    if (onNavigate) {
      onNavigate('/premium/my-subscription');
    }
  };

  return createPortal(
    <div
      className="premium-checkout-modal-overlay"
      onClick={onClose}
    >
      <div
        className="premium-checkout-modal-container glass-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="premium-checkout-modal__header">
          <div>
            <h2 className="premium-checkout-modal__title text-headline-sm">
              {t('checkout.title')}
            </h2>
            <p className="premium-checkout-modal__subtitle text-body-md">
              {t('checkout.subtitle')}
            </p>
          </div>
          <button
            type="button"
            className="btn-icon premium-checkout-modal__close-btn"
            onClick={onClose}
            title={t('common:close')}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <div className="premium-checkout-modal__body">
          <CheckoutModalContent
            isLoading={isLoading}
            checkoutError={checkoutError}
            paymentStatus={paymentStatus}
            plan={plan}
            checkoutData={checkoutData}
            amountDisplay={amountDisplay}
            qrLoadError={qrLoadError}
            onQrError={() => setQrLoadError(true)}
            onRetry={initializeCheckout}
            onSuccessCta={handleSuccessCta}
            onCopySuccess={handleCopySuccess}
            onCopyError={handleCopyError}
            t={t}
          />
        </div>

        <Toast
          message={toastMessage}
          onDismiss={() => setToastMessage('')}
          duration={2000}
        />
      </div>
    </div>,
    document.body
  );
}

export default PremiumCheckoutModal;
