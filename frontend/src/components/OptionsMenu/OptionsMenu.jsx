import './OptionsMenu.css';

function OptionsMenu({ onClick, ariaLabel = 'More options', className = '' }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={ariaLabel}
      className={`btn-icon options-menu ${className}`.trim()}
    >
      <span className="material-symbols-outlined">more_horiz</span>
    </button>
  );
}

export default OptionsMenu;
