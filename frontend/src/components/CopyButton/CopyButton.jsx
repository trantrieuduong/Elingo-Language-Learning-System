import { useTranslation } from 'react-i18next';
import './CopyButton.css';

function CopyButton({ text, onCopy, onError, className = '' }) {
  const { t } = useTranslation('common');

  const handleClick = async () => {
    if (text === null || text === undefined || text === '') return;
    try {
      await navigator.clipboard.writeText(String(text));
      onCopy?.(text);
    } catch (err) {
      onError?.(err);
    }
  };

  return (
    <button
      type="button"
      className={`btn-icon copy-btn ${className}`.trim()}
      onClick={handleClick}
      title={t('copy')}
    >
      <span className="material-symbols-outlined">content_copy</span>
    </button>
  );
}

export default CopyButton;