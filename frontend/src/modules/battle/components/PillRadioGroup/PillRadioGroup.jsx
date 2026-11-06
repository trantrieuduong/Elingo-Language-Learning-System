const PillRadioGroup = ({ label, options, value, onChange, t }) => {
  return (
    <div className="battle-filter-group">
      <span className="battle-filter-group__label text-label-sm text-disabled">
        {label}:
      </span>
      <div className="battle-filter-pills" role="radiogroup">
        {options.map((option) => {
          const isSelected = value === option.key;
          return (
            <button
              key={option.key}
              type="button"
              role="radio"
              aria-checked={isSelected}
              aria-label={t(option.labelKey)}
              className={`battle-filter-pill ${
                isSelected ? 'battle-filter-pill--active' : ''
              }`}
              onClick={() => onChange(option.key)}
            >
              {t(option.labelKey)}
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default PillRadioGroup;

