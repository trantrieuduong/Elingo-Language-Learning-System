import './OptionsMenu.css';

function OptionsMenu({ onClick, className = '' }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`btn-icon options-menu ${className}`.trim()}
    >
      <span className="material-symbols-outlined">more_horiz</span>
    </button>
  );
}

export default OptionsMenu;
