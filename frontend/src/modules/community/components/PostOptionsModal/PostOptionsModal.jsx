import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import './PostOptionsModal.css';

function PostOptionsModal({ isOpen, onClose, postId }) {
  const { t } = useTranslation();
  const [copyStatus, setCopyStatus] = useState('idle');

  useEffect(() => {
    if (!isOpen) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleCopyLink = async () => {
    setCopyStatus('idle');

    try {
      const postUrl = new URL(
        `/community/posts/${encodeURIComponent(postId)}`,
        window.location.origin
      ).toString();

      await navigator.clipboard.writeText(postUrl);
      setCopyStatus('copied');
    } catch {
      setCopyStatus('error');
    }
  };

  return createPortal(
    <div className="post-options-modal__overlay" onClick={onClose}>
      <div
        className="post-options-modal__container glass-capsule"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="post-options-modal__header">
          <h2 className="text-headline-sm">{t('community:postCard.postOptionsTitle')}</h2>
          <button type="button" className="btn-icon post-options-modal__close" onClick={onClose}>
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>
        <button type="button" className="btn-secondary post-options-modal__item" disabled>
          <span className="material-symbols-outlined">edit</span>
          <span>{t('community:postCard.editPost')}</span>
        </button>
        <button
          type="button"
          className="btn-secondary post-options-modal__item"
          onClick={handleCopyLink}
        >
          <span className="material-symbols-outlined">content_copy</span>
          <span>{t('community:postCard.copyPostLink')}</span>
        </button>
        {copyStatus === 'copied' && (
          <p className="post-options-modal__feedback">{t('community:postCard.postLinkCopied')}</p>
        )}
        {copyStatus === 'error' && (
          <p className="post-options-modal__feedback post-options-modal__feedback--error">
            {t('community:postCard.copyPostLinkFailed')}
          </p>
        )}
      </div>
    </div>,
    document.body
  );
}

export default PostOptionsModal;
