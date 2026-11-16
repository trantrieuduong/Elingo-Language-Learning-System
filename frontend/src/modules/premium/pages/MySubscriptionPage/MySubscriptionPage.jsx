import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import Pagination from '../../../../components/Pagination/Pagination';
import Table from '../../../../components/Table/Table';
import { formatPremiumPrice } from '../../../../utils/utils';
import { getMySubscriptionApi, getMyTransactionsApi } from '../../premiumApi';
import { formatDateTime } from '../../utils/format';
import './MySubscriptionPage.css';

function MySubscriptionPage({ onNavigate }) {
  const { t, i18n } = useTranslation(['premium', 'common']);

  // States thông tin gói Premium
  const [subscription, setSubscription] = useState(null);
  const [isSubLoading, setIsSubLoading] = useState(true);
  const [subError, setSubError] = useState(null);
  const [subRetryCount, setSubRetryCount] = useState(0);

  // States lịch sử giao dịch
  const [transactions, setTransactions] = useState([]);
  const [isTxLoading, setIsTxLoading] = useState(true);
  const [txError, setTxError] = useState(null);
  const [txRetryCount, setTxRetryCount] = useState(0);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, []);

  useEffect(() => {
    let isMounted = true;

    const fetchSubscription = async () => {
      setIsSubLoading(true);
      setSubError(null);
      try {
        const res = await getMySubscriptionApi();
        if (!isMounted) return;
        if (res.success && res.data) {
          setSubscription(res.data);
        } else {
          setSubError(res.message ?? t('common:loadError'));
        }
      } catch {
        if (!isMounted) return;
        setSubError(t('common:loadError'));
      } finally {
        if (isMounted) setIsSubLoading(false);
      }
    };

    fetchSubscription();

    return () => {
      isMounted = false;
    };
  }, [subRetryCount, t]);

  useEffect(() => {
    let isMounted = true;

    const fetchTransactions = async () => {
      setIsTxLoading(true);
      setTxError(null);
      try {
        const res = await getMyTransactionsApi({ page, size: 10 });
        if (!isMounted) return;
        if (res.success && res.data) {
          setTransactions(res.data.content);
          setTotalPages(res.data.totalPages);
        } else {
          setTxError(res.message ?? t('common:loadError'));
        }
      } catch {
        if (!isMounted) return;
        setTxError(t('common:loadError'));
      } finally {
        if (isMounted) setIsTxLoading(false);
      }
    };

    fetchTransactions();

    return () => {
      isMounted = false;
    };
  }, [page, txRetryCount, t]);

  const handleSubRetry = () => {
    setSubRetryCount((prev) => prev + 1);
  };

  const handleTxRetry = () => {
    setTxRetryCount((prev) => prev + 1);
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages && newPage !== page) {
      setPage(newPage);
    }
  };

  // Helper render badge trạng thái gói đăng ký
  const renderSubscriptionBadge = (status) => {
    const normalized = status.toUpperCase();
    if (normalized === 'ACTIVE') {
      return (
        <span className="badge badge--mint">
          <span className="material-symbols-outlined">check_circle</span>
          {t('mySubscription.statuses.active')}
        </span>
      );
    } else if (normalized === 'EXPIRED') {
      return (
        <span className="badge badge--pink">
          <span className="material-symbols-outlined">timer_off</span>
          {t('mySubscription.statuses.expired')}
        </span>
      );
    }
    return (
      <span className="badge badge--error">
        <span className="material-symbols-outlined">cancel</span>
        {t('mySubscription.statuses.cancelled')}
      </span>
    );
  };

  // Helper render badge trạng thái thanh toán
  const renderTxStatusBadge = (status) => {
    const normalized = status.toUpperCase();
    switch (normalized) {
      case 'SUCCESS':
        return <span className="badge badge--mint">{t('mySubscription.txStatuses.success')}</span>;
      case 'PENDING':
        return (
          <span className="badge badge--lavender">{t('mySubscription.txStatuses.pending')}</span>
        );
      case 'FAILED':
        return <span className="badge badge--error">{t('mySubscription.txStatuses.failed')}</span>;
      case 'REFUNDED':
        return (
          <span className="badge badge--soft-blue">{t('mySubscription.txStatuses.refunded')}</span>
        );
      default:
        return <span className="badge badge--default">{status}</span>;
    }
  };

  const locale = i18n?.language === 'vi' ? 'vi-VN' : 'en-US';

  const transactionColumns = [
    {
      key: 'planName',
      title: t('mySubscription.table.planName'),
      width: 25,
      render: (value) => <span className="my-sub-table__plan-name">{value}</span>,
    },
    {
      key: 'amount',
      title: t('mySubscription.table.amount'),
      width: 20,
      render: (value, row) => (
        <span className="my-sub-table__amount">
          {formatPremiumPrice(value, row.currency, locale)}
        </span>
      ),
    },
    {
      key: 'status',
      title: t('mySubscription.table.status'),
      width: 18,
      render: (value) => renderTxStatusBadge(value),
    },
    {
      key: 'paidAt',
      title: t('mySubscription.table.paidAt'),
      width: 20,
      render: (value, row) => formatDateTime(value || row.createdAt, locale),
    },
    {
      key: 'providerRefCode',
      title: t('mySubscription.table.refCode'),
      width: 17,
      render: (value) => <span className="my-sub-table__ref-code">{value}</span>,
    },
  ];

  return (
    <main className="my-sub-page">
      {/* Section: Thông tin gói Premium hiện tại */}
      <section className="my-sub-page__tx-section">
        <h2 className="text-headline-md my-sub-page__section-title">{t('mySubscription.title')}</h2>

        {isSubLoading ? (
          <PageLoading />
        ) : subError ? (
          <EmptyState
            icon="error"
            isError
            description={subError}
            actionLabel={t('common:retry')}
            onAction={handleSubRetry}
          />
        ) : !subscription?.planName ? (
          <EmptyState
            icon="workspace_premium"
            title={t('mySubscription.cardTitle')}
            description={t('mySubscription.noActiveSubscription')}
            actionLabel={t('mySubscription.explorePlans')}
            actionVariant="primary"
            onAction={() => {
              if (onNavigate) onNavigate('/premium');
            }}
          />
        ) : (
          <div className="my-sub-card glass-card">
            <div className="my-sub-card__header">
              <div className="my-sub-card__title-wrap">
                <div className="my-sub-card__icon-wrap glass-thumb">
                  <span className="material-symbols-outlined">
                    workspace_premium
                  </span>
                </div>
                <div>
                  <h2 className="text-headline-sm my-sub-card__title">
                    {subscription.planName}
                  </h2>
                  <p className="text-body-sm my-sub-card__label">{t('mySubscription.cardTitle')}</p>
                </div>
              </div>
                            <div className="my-sub-card__item">
                <span className="text-label-md my-sub-card__label">
                  {t('mySubscription.startAt')}
                </span>
                <span className="text-body-md my-sub-card__value">
                  {formatDateTime(subscription.startAt, locale)}
                </span>
              </div>

              <div className="my-sub-card__item">
                <span className="text-label-md my-sub-card__label">
                  {t('mySubscription.endAt')}
                </span>
                <span className="text-body-md my-sub-card__value">
                  {formatDateTime(subscription.endAt, locale)}
                </span>
              </div>

              <div className="my-sub-card__item">
                <span className="text-label-md my-sub-card__label">
                  {t('mySubscription.remainingDays')}
                </span>
                <span className="text-body-md my-sub-card__value my-sub-card__value--highlight">
                  {t('mySubscription.remainingDaysValue', { count: subscription.remainingDays })}
                </span>
              </div>
              <div className="my-sub-card__status">
                {renderSubscriptionBadge(subscription.status)}
              </div>
            </div>
          </div>
        )}
      </section>

      {/* Section: Lịch sử thanh toán */}
      <section className="my-sub-page__tx-section">
        <h2 className="text-headline-md my-sub-page__section-title">
          {t('mySubscription.transactionsTitle')}
        </h2>

        {isTxLoading ? (
          <PageLoading />
        ) : txError ? (
          <EmptyState
            icon="error"
            isError
            description={txError}
            actionLabel={t('common:retry')}
            onAction={handleTxRetry}
          />
        ) : transactions.length === 0 ? (
          <EmptyState icon="receipt_long" description={t('mySubscription.transactionsEmpty')} />
        ) : (
          <div className="my-sub-page__tx-card glass-card">
            <Table
              columns={transactionColumns}
              data={transactions}
              rowKey="id"
              className="my-sub-table"
            />
          </div>
        )}
        {totalPages > 1 && (
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={handlePageChange} />
        )}
      </section>
    </main>
  );
}

export default MySubscriptionPage;
