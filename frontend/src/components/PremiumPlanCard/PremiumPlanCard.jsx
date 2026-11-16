import { useTranslation } from 'react-i18next';
import { formatPremiumPrice, getDurationParts } from '../../utils/utils';
import './PremiumPlanCard.css';

function PremiumPlanCard({
  plan,
  onSelect,
  isFeatured = false,
  popularLabel,
  buttonLabel,
  features,
}) {
  const { t, i18n } = useTranslation(['premium']);

  if (!plan) return null;

  const { unit, count } = getDurationParts(plan.durationDays);
  const planPeriod = `/ ${t(`plan.duration.${unit}`, { count })}`;
  const planPrice = formatPremiumPrice(plan.price, plan.currency, i18n.resolvedLanguage);
  const hasDetails = Boolean(
    plan.description || (Array.isArray(features) && features.length)
  );

  return (
    <article
      className={`premium-plan-card ${
        isFeatured ? 'premium-plan-card--featured glass-pro glass-sheen' : 'glass-card'
      } ${popularLabel ? 'premium-plan-card--has-popular' : ''} ${
        buttonLabel ? 'premium-plan-card--has-button' : ''
      } ${
        hasDetails ? '' : 'premium-plan-card--compact'
      }`}
    >
      {popularLabel && (
        <span className="premium-plan-card__popular glass-thumb">{popularLabel}</span>
      )}
      <div className="premium-plan-card__content">
        <h2 className="premium-plan-card__name text-headline-sm">{plan.name}</h2>
        <div className="premium-plan-card__price">
          <strong className="premium-plan-card__price-value--paid">{planPrice}</strong>
          <span>{planPeriod}</span>
        </div>
        {plan.description && <p className="premium-plan-card__description">{plan.description}</p>}
        {Array.isArray(features) && features.length > 0 && (
          <ul className="premium-plan-card__features">
            {features.map((feature) => (
              <li key={feature}>
                <span className="material-symbols-outlined">
                  {isFeatured ? 'check_circle' : 'check'}
                </span>
                {feature}
              </li>
            ))}
          </ul>
        )}
      </div>
      {buttonLabel && (
        <button
          type="button"
          className={`premium-plan-card__select ${isFeatured ? 'btn-primary' : 'btn-secondary'}`}
          disabled={!onSelect}
          onClick={() => onSelect?.(plan)}
        >
          {buttonLabel}
        </button>
      )}
    </article>
  );
}

export default PremiumPlanCard;
