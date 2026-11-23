import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { LayersIcon } from '../../components/icons/AdminIcons';
import styles from './PlaceholderAdminPage.module.css';

export default function PlaceholderAdminPage({ title, onNavigate }) {
  const { t } = useTranslation('admin');

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.iconBox}>
          <LayersIcon size={32} />
        </div>
        <h2 className={styles.title}>{title || t('dashboard.common.upcomingModule')}</h2>
        <p className={styles.desc}>{t('dashboard.common.upcomingDesc')}</p>
        <button type="button" className={styles.backBtn} onClick={() => onNavigate('/admin')}>
          {t('dashboard.common.backToDashboard')}
        </button>
      </div>
    </div>
  );
}

PlaceholderAdminPage.propTypes = {
  title: PropTypes.string,
  onNavigate: PropTypes.func.isRequired,
};
