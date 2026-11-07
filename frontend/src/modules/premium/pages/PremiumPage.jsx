import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../context/AuthContext';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import { getPremiumPlansApi } from '../premiumApi';
import { getBestValuePlan } from '../utils/format';
import PremiumPlanCard from '../../../components/PremiumPlanCard/PremiumPlanCard';
import './PremiumPage.css';

function PremiumPage({ onNavigate }) {
  const { t } = useTranslation(['premium', 'common']);
  const { user } = useAuth();

  // Trạng thái danh sách gói Premium
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);

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

  const handleUpgrade = () => {
    if (onNavigate) onNavigate('/login');
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
        <div className="premium-page__grid">
          {plans.map((plan) => {
            const isFeatured = plan.id === bestValuePlan?.id;

            return (
              <PremiumPlanCard
                key={plan.id}
                plan={plan}
                isFeatured={isFeatured}
                popularLabel={isFeatured ? t('plans.bestValue') : undefined}
                buttonLabel={t('plan.upgrade')}
                onSelect={!user ? handleUpgrade : undefined}
              />
            );
          })}
        </div>
      )}
    </main>
  );
}

export default PremiumPage;
