import React, { useState, useEffect, useCallback, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  RefreshIcon,
  SearchIcon,
  EditIcon,
  ShieldCheckIcon,
} from '../../components/icons/AdminIcons';
import {
  fetchSystemConfigs,
  fetchConfigModules,
} from '../../services/adminSystemConfigApi';
import ConfigEditModal from './components/ConfigEditModal';
import styles from './SystemConfigPage.module.css';

export default function SystemConfigPage({ onNavigate: _onNavigate }) {
  const { t } = useTranslation('admin');

  // Filter state
  const [selectedModule, setSelectedModule] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Data & loading state
  const [configs, setConfigs] = useState([]);
  const [modules, setModules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Edit Modal state
  const [editModal, setEditModal] = useState({
    isOpen: false,
    config: null,
  });

  // Load modules list on mount
  useEffect(() => {
    const loadModules = async () => {
      try {
        const moduleList = await fetchConfigModules();
        setModules(moduleList || []);
      } catch {
        // Silently tolerate module fetch failure
      }
    };
    loadModules();
  }, []);

  // Load configs
  const loadConfigs = useCallback(
    async (isManualRefresh = false) => {
      try {
        if (isManualRefresh) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }
        setError(null);

        const data = await fetchSystemConfigs(selectedModule);
        setConfigs(data || []);
      } catch (err) {
        setError(err.message || t('feedback.errorDesc'));
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [selectedModule, t]
  );

  useEffect(() => {
    loadConfigs(false);
  }, [loadConfigs]);

  // Client-side quick search filter
  const filteredConfigs = useMemo(() => {
    if (!searchQuery.trim()) {
      return configs;
    }
    const q = searchQuery.toLowerCase().trim();
    return configs.filter(
      (c) =>
        (c.configKey && c.configKey.toLowerCase().includes(q)) ||
        (c.description && c.description.toLowerCase().includes(q))
    );
  }, [configs, searchQuery]);

  // Statistics counters
  const totalCount = configs.length;
  const secretCount = useMemo(
    () => configs.filter((c) => Boolean(c.isSecret)).length,
    [configs]
  );

  const handleOpenEdit = (config) => {
    setEditModal({
      isOpen: true,
      config,
    });
  };

  const handleCloseEdit = () => {
    setEditModal({
      isOpen: false,
      config: null,
    });
  };

  const handleEditSuccess = (updatedConfig) => {
    setConfigs((prev) =>
      prev.map((c) => (c.configKey === updatedConfig.configKey ? updatedConfig : c))
    );
    handleCloseEdit();
  };

  // Table columns definition
  const columns = useMemo(
    () => [
      {
        key: 'configKey',
        dataIndex: 'configKey',
        title: t('systemConfig.key'),
        width: '23%',
        render: (key) => (
          <div className={styles.keyCell}>
            <span className={styles.keyText} title={key}>
              {key}
            </span>
          </div>
        ),
      },
      {
        key: 'module',
        dataIndex: 'module',
        title: t('systemConfig.module'),
        width: '11%',
        render: (mod) => {
          let modClass = styles.moduleOther;
          if (mod === 'PREMIUM') modClass = styles.modulePremium;
          else if (mod === 'COMMUNITY') modClass = styles.moduleCommunity;
          else if (mod === 'GAMIFICATION') modClass = styles.moduleGamification;

          return <span className={`${styles.moduleBadge} ${modClass}`}>{mod}</span>;
        },
      },
      {
        key: 'valueType',
        dataIndex: 'valueType',
        title: t('systemConfig.typeAndSecurity'),
        width: '11%',
        render: (type, record) => (
          <div className={styles.typeCell}>
            <span className={styles.typeBadge}>{type}</span>
            {record.isSecret && (
              <span className={styles.secretBadge} title={t('systemConfig.secret')}>
                <ShieldCheckIcon size={13} />
                <span>{t('systemConfig.secret')}</span>
              </span>
            )}
          </div>
        ),
      },
      {
        key: 'description',
        dataIndex: 'description',
        title: t('systemConfig.description'),
        width: '22%',
        render: (desc) => (
          <div className={styles.descCell} title={desc || ''}>
            {desc ? (
              <span className={styles.descText}>{desc}</span>
            ) : (
              <span className={styles.emptyText}>—</span>
            )}
          </div>
        ),
      },
      {
        key: 'configValue',
        dataIndex: 'configValue',
        title: t('systemConfig.value'),
        width: '28%',
        render: (val, record) => {
          if (record.isSecret) {
            return (
              <span className={styles.maskedValue} title={t('systemConfig.secret')}>
                ********
              </span>
            );
          }
          const displayVal = val || '(empty)';
          return (
            <code className={styles.valueCode} title={displayVal}>
              {displayVal}
            </code>
          );
        },
      },
      {
        key: 'actions',
        title: t('common.actions'),
        width: '5%',
        align: 'right',
        render: (_, record) => (
          <button
            type="button"
            className={styles.actionBtn}
            onClick={() => handleOpenEdit(record)}
            title={t('systemConfig.editModalTitle')}
            aria-label={t('systemConfig.editModalTitle')}
          >
            <EditIcon size={15} />
          </button>
        ),
      },
    ],
    [t]
  );

  return (
    <div className={styles.container}>
      {/* Header */}
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <h1 className={styles.title}>{t('systemConfig.title')}</h1>
          <p className={styles.subtitle}>{t('systemConfig.subtitle')}</p>
        </div>
        <div className={styles.headerActions}>
          <button
            type="button"
            className={styles.refreshBtn}
            onClick={() => loadConfigs(true)}
            disabled={loading || refreshing}
            title={t('common.refresh')}
            aria-label={t('common.refresh')}
          >
            <RefreshIcon
              size={16}
              className={refreshing ? styles.spinning : ''}
            />
            <span>{t('common.refresh')}</span>
          </button>
        </div>
      </header>

      {/* KPI Stat Chips */}
      <div className={styles.statChipsRow}>
        <div className={styles.statChip}>
          <span className={styles.statLabel}>{t('systemConfig.totalConfigs')}</span>
          <span className={styles.statValue}>{totalCount}</span>
        </div>
        <div className={styles.statChip}>
          <span className={styles.statLabel}>{t('systemConfig.secretConfigs')}</span>
          <span className={`${styles.statValue} ${styles.statValueSecret}`}>
            {secretCount}
          </span>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className={styles.toolbar}>
        {/* Module Filter */}
        <div className={styles.filterGroup}>
          <select
            className={styles.select}
            value={selectedModule}
            onChange={(e) => setSelectedModule(e.target.value)}
            aria-label={t('systemConfig.module')}
          >
            <option value="">{t('systemConfig.allModules')}</option>
            {modules.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </div>

        {/* Quick Search */}
        <div className={styles.searchGroup}>
          <div className={styles.searchBox}>
            <SearchIcon size={16} className={styles.searchIcon} />
            <input
              type="text"
              className={styles.searchInput}
              placeholder={t('systemConfig.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>
      </div>

      {/* Content Area */}
      {error ? (
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={() => loadConfigs(false)}
        />
      ) : (
        <div className={styles.tableCard}>
          <DataTable
            columns={columns}
            data={filteredConfigs}
            keyField="configKey"
            isLoading={loading}
            emptyMessage={t('feedback.emptyList')}
          />
        </div>
      )}

      {/* Edit Modal */}
      {editModal.isOpen && editModal.config && (
        <ConfigEditModal
          config={editModal.config}
          onClose={handleCloseEdit}
          onSuccess={handleEditSuccess}
        />
      )}
    </div>
  );
}

SystemConfigPage.propTypes = {
  onNavigate: PropTypes.func,
};
