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

function PremiumCheckoutModal({ plan, onClose, onSuccess, onNavigate }) {
  const { t, i18n } = useTranslation(['premium', 'common']);

  const [checkoutData, setCheckoutData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [checkoutError, setCheckoutError] = useState(null);
  const [paymentStatus, setPaymentStatus] = useState('PENDING'); // PENDING | SUCCESS | FAILED | REFUNDED
  const [qrLoadError, setQrLoadError] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  const pollingRef = useRef(null);
  const isMountedRef = useRef(true);

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
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [onClose]);

  const initializeCheckout = useCallback(async () => {
    if (!plan?.id) return;

    setIsLoading(true);
    setCheckoutError(null);
    setQrLoadError(false);
    setPaymentStatus('PENDING');
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }

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

  // Polling trạng thái giao dịch
  useEffect(() => {
    const transactionId = checkoutData?.transactionId;
    if (!transactionId || paymentStatus === 'SUCCESS' || paymentStatus === 'FAILED' || paymentStatus === 'REFUNDED') {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
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

    return () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, [checkoutData?.transactionId, paymentStatus, onSuccess]);

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
          {isLoading ? (
            <div className="premium-checkout-modal__loading">
              <span className="premium-checkout-modal__spinner" />
              <p className="text-body-md text-disabled">{t('checkout.statusPending')}</p>
            </div>
          ) : checkoutError ? (
            <EmptyState
              icon="error"
              isError
              description={checkoutError}
              actionLabel={t('common:retry')}
              onAction={initializeCheckout}
            />
          ) : paymentStatus === 'SUCCESS' ? (
            <div className="premium-checkout-modal__success glass-thumb">
              <span className="material-symbols-outlined premium-checkout-modal__success-icon">
                check_circle
              </span>
              <h3 className="text-headline-sm">{t('checkout.statusSuccess')}</h3>
              <p className="text-body-md text-disabled">{plan?.name} • {amountDisplay}</p>
              <button
                type="button"
                className="btn-primary premium-checkout-modal__success-btn"
                onClick={handleSuccessCta}
              >
                {t('checkout.successCta')}
              </button>
            </div>
          ) : paymentStatus === 'FAILED' || paymentStatus === 'REFUNDED' ? (
            <EmptyState
              icon="cancel"
              isError
              description={paymentStatus === 'REFUNDED' ? t('checkout.statusRefunded') : t('checkout.statusFailed')}
              actionLabel={t('common:retry')}
              onAction={initializeCheckout}
            />
          ) : (
            <div className="premium-checkout-modal__grid">
              <div className="premium-checkout-modal__qr-col">
                <div
                  className={`premium-checkout-modal__qr-wrapper ${
                    checkoutData?.qrCodeUrl && !qrLoadError
                      ? 'premium-checkout-modal__qr-wrapper--has-qr'
                      : 'premium-checkout-modal__qr-wrapper--fallback glass-card'
                  }`}
                >
                  {checkoutData?.qrCodeUrl && !qrLoadError ? (
                    <img
                      src={checkoutData.qrCodeUrl}
                      alt="VietQR Payment Code"
                      className="premium-checkout-modal__qr-img"
                      onError={() => setQrLoadError(true)}
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
                        onCopy={handleCopySuccess}
                        onError={handleCopyError}
                      />
                    </div>
                  </div>
                </div>

                <div className="premium-checkout-modal__transfer-details">
                  <h3 className="text-title-md text-high">
                    {t('checkout.transferInfo')}
                  </h3>

                  <div className="premium-checkout-modal__field glass-thumb premium-checkout-modal__field--highlight">
                    <div className="premium-checkout-modal__field-text">
                      <span className="text-label-sm text-body">{t('checkout.transferContent')}</span>
                      <span className="premium-checkout-modal__field-val premium-checkout-modal__field-val--ref">
                        {checkoutData?.providerRefCode}
                      </span>
                    </div>
                    <CopyButton
                      text={checkoutData?.providerRefCode}
                      onCopy={handleCopySuccess}
                      onError={handleCopyError}
                    />
                  </div>

                  <div className="premium-checkout-modal__field glass-thumb">
                    <div className="premium-checkout-modal__field-text">
                      <span className="text-label-sm text-body">{t('checkout.bankAccount')}</span>
                      <span className="premium-checkout-modal__field-val">
                        {checkoutData?.bankAccount}
                      </span>
                    </div>
                    <CopyButton
                      text={checkoutData?.bankAccount}
                      onCopy={handleCopySuccess}
                      onError={handleCopyError}
                    />
                  </div>

                  <div className="premium-checkout-modal__field glass-thumb">
                    <div className="premium-checkout-modal__field-text">
                      <span className="text-label-sm text-body">{t('checkout.accountName')}</span>
                      <span className="premium-checkout-modal__field-val">
                        {checkoutData?.bankAccountName}
                      </span>
                    </div>
                    <CopyButton
                      text={checkoutData?.bankAccountName}
                      onCopy={handleCopySuccess}
                      onError={handleCopyError}
                    />
                  </div>

                  <div className="premium-checkout-modal__field glass-thumb">
                    <div className="premium-checkout-modal__field-text">
                      <span className="text-label-sm text-body">{t('checkout.bankName')}</span>
                      <span className="premium-checkout-modal__field-val">
                        {checkoutData?.bankName}
                      </span>
                    </div>
                    <CopyButton
                      text={checkoutData?.bankName}
                      onCopy={handleCopySuccess}
                      onError={handleCopyError}
                    />
                  </div>

                  {checkoutData?.transactionId && (
                    <div className="premium-checkout-modal__transaction-id text-label-sm text-disabled">
                      {t('checkout.transactionId')}: #{checkoutData.transactionId}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
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