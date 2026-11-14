import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../../context/AuthContext';
import EmptyState from '../../../../components/EmptyState/EmptyState';
import PageLoading from '../../../../components/PageLoading/PageLoading';
import { getPremiumPlansApi } from '../../premiumApi';
import { getBestValuePlan } from '../../utils/format';
import PremiumPlanCard from '../../../../components/PremiumPlanCard/PremiumPlanCard';
import PremiumCheckoutModal from '../../components/PremiumCheckoutModal/PremiumCheckoutModal';
import './PremiumListPage.css';

function PremiumListPage({ onNavigate }) {
  const { t } = useTranslation(['premium', 'common']);
  const { user } = useAuth();

  // Trạng thái danh sách gói Premium
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);

  // Modal checkout
  const [selectedPlanForCheckout, setSelectedPlanForCheckout] = useState(null);

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, []);

  useEffect(() => {
    let isMounted = true;

    const fetchPlans = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await getPremiumPlansApi();
        if (!isMounted) return;

        const list = response?.data || response;
        setPlans(Array.isArray(list) ? list : []);
      } catch {
        if (!isMounted) return;
        setError(t('common:loadError'));
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchPlans();

    return () => {
      isMounted = false;
    };
  }, [retryCount, t]);

  const handleRetry = () => {
    setRetryCount((count) => count + 1);
  };

  const handleSelectPlan = (plan) => {
    if (!user) {
      if (onNavigate) onNavigate('/login');
      return;
    }
    setSelectedPlanForCheckout(plan);
  };

  const bestValuePlan = getBestValuePlan(plans);

  return (
    <main className="premium-page">
      {loading ? (
        <PageLoading />
      ) : error ? (
        <EmptyState
          icon="error"
          isError
          description={error}
          actionLabel={t('common:retry')}
          onAction={handleRetry}
        />
      ) : plans.length === 0 ? (
        <EmptyState
          icon="workspace_premium"
          title={t('plans.emptyTitle')}
          description={t('plans.emptyDescription')}
        />
      ) : (
        <>
          <div className="premium-page__notice glass-card" role="note">
            <span className="material-symbols-outlined premium-page__notice-icon">
              info
            </span>
            <span className="premium-page__notice-text text-body-md">
              {t('plans.accumulationNote')}
            </span>
          </div>

          <div className="premium-page__grid">
            {plans.map((plan) => {
              const isFeatured = plan.id === bestValuePlan?.id;

              return (
                <PremiumPlanCard
                  key={plan.id}
                  plan={plan}
                  isFeatured={isFeatured}
                  popularLabel={isFeatured ? t('plans.bestValue') : undefined}
                  buttonLabel={user ? t('plan.upgrade') : undefined}
                  onSelect={user ? () => handleSelectPlan(plan) : undefined}
                />
              );
            })}
          </div>
        </>
      )}

      {selectedPlanForCheckout && (
        <PremiumCheckoutModal
          plan={selectedPlanForCheckout}
          onClose={() => setSelectedPlanForCheckout(null)}
          onNavigate={onNavigate}
        />
      )}
    </main>
  );
}

export default PremiumListPage;
