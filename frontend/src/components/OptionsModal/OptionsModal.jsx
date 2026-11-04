import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import './OptionsModal.css';

function OptionsModal({ isOpen, title, options = [], onClose }) {
  useEffect(() => {
    if (!isOpen) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleOptionClick = (option) => {
    option.onSelect?.();
    onClose?.();
  };

  return createPortal(
    <div className="options-modal__overlay" onClick={onClose}>
      <div
        className="options-modal__container glass-capsule"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="options-modal__header">
          <h2 className="text-headline-sm">{title}</h2>
          <button
            type="button"
            className="btn-icon options-modal__close"
            onClick={onClose}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>
        {options.map((option) => (
          <button
            key={option.id}
            type="button"
            className={`btn-secondary options-modal__item${option.variant ? ` options-modal__item--${option.variant}` : ''}`}
            onClick={() => handleOptionClick(option)}
            disabled={option.disabled}
          >
            {option.icon && <span className="material-symbols-outlined">{option.icon}</span>}
            <span>{option.label}</span>
          </button>
        ))}
      </div>
    </div>,
    document.body
  );
}

export default OptionsModal;
