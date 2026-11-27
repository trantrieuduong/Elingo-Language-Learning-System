import React from 'react';
import PropTypes from 'prop-types';
import { CloseIcon } from '../icons/AdminIcons';
import styles from './AdminModal.module.css';

/**
 * Khung modal chuẩn cho toàn bộ các trang quản trị Admin.
 */
export default function AdminModal({
  isOpen,
  onClose,
  title,
  subtitle,
  icon,
  wide = false,
  closeLabel = 'Close',
  closeDisabled = false,
  closeOnBackdrop = false,
  titleId,
  footer,
  children,
}) {
  if (!isOpen) return null;

  return (
    <div
      className={styles.overlay}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      onClick={closeOnBackdrop ? onClose : undefined}
    >
      <div
        className={`${styles.modal} ${wide ? styles.modalWide : ''}`}
        onClick={closeOnBackdrop ? (e) => e.stopPropagation() : undefined}
      >
        <div className={styles.modalHeader}>
          <div className={styles.headerTitleGroup}>
            {icon}
            <div>
              <h2 id={titleId} className={styles.modalTitle}>
                {title}
              </h2>
              {subtitle && <p className={styles.modalSubtitle}>{subtitle}</p>}
            </div>
          </div>
          <button
            type="button"
            className={styles.closeBtn}
            onClick={onClose}
            disabled={closeDisabled}
            aria-label={closeLabel}
          >
            <CloseIcon size={18} />
          </button>
        </div>

        {children}

        {footer && <div className={styles.modalFooter}>{footer}</div>}
      </div>
    </div>
  );
}

AdminModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  title: PropTypes.node.isRequired,
  subtitle: PropTypes.node,
  icon: PropTypes.node,
  wide: PropTypes.bool,
  closeLabel: PropTypes.string,
  closeDisabled: PropTypes.bool,
  closeOnBackdrop: PropTypes.bool,
  titleId: PropTypes.string,
  footer: PropTypes.node,
  children: PropTypes.node.isRequired,
};
