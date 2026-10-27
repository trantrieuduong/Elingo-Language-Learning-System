import './EmptyState.css';

function EmptyState({
  icon = 'inbox',
  title,
  description,
  isError = false,
  actionLabel,
  onAction,
  actionVariant = 'secondary',
  className = '',
}) {
  const btnClass = actionVariant === 'primary' ? 'btn-primary' : 'btn-secondary';
  const iconStateClass = isError
    ? 'empty-state__icon--error text-error'
    : 'empty-state__icon--default text-disabled';
  const descClass = isError ? 'text-error' : 'text-disabled';

  return (
    <div className={`empty-state glass-card ${className}`.trim()}>
      {icon && (
        <span className={`material-symbols-outlined empty-state__icon ${iconStateClass}`}>
          {icon}
        </span>
      )}

      {title && <h2 className="text-headline-sm empty-state__title">{title}</h2>}

      {description && (
        <p className={`text-body-lg empty-state__description ${descClass}`}>{description}</p>
      )}

      {actionLabel && onAction && (
        <div className="empty-state__actions">
          <button type="button" className={btnClass} onClick={onAction}>
            {actionLabel}
          </button>
        </div>
      )}
    </div>
  );
}

export default EmptyState;
