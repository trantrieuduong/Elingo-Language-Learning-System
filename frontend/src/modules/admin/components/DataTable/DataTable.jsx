import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import styles from './DataTable.module.css';

export default function DataTable({
  columns = [],
  data = [],
  keyField = 'id',
  emptyMessage,
  isLoading = false,
  borderless = false,
}) {
  const { t } = useTranslation('admin');
  const resolvedEmptyMessage = emptyMessage || t('feedback.emptyList');

  if (isLoading) {
    return (
      <div className={`${styles.tableWrapper} ${borderless ? styles.borderless : ''}`}>
        <div className={styles.loadingContainer}>
          <div className={styles.loadingBar} />
          <span>{t('feedback.loadingList')}</span>
        </div>
      </div>
    );
  }

  return (
    <div className={`${styles.tableWrapper} ${borderless ? styles.borderless : ''}`}>
      <table className={styles.table}>
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col.key || col.dataIndex}
                className={`${styles.th} ${col.align === 'right' ? styles.alignRight : ''}`}
                style={{ width: col.width }}
              >
                {col.title}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className={styles.emptyCell}>
                {resolvedEmptyMessage}
              </td>
            </tr>
          ) : (
            data.map((record, rowIndex) => (
              <tr key={record[keyField] || rowIndex} className={styles.tr}>
                {columns.map((col) => {
                  const val = col.dataIndex ? record[col.dataIndex] : undefined;
                  const content = col.render ? col.render(val, record, rowIndex) : val;
                  return (
                    <td
                      key={col.key || col.dataIndex}
                      className={`${styles.td} ${col.align === 'right' ? styles.alignRight : ''}`}
                    >
                      {content !== undefined && content !== null ? content : '—'}
                    </td>
                  );
                })}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

DataTable.propTypes = {
  columns: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string,
      dataIndex: PropTypes.string,
      title: PropTypes.node.isRequired,
      width: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
      align: PropTypes.oneOf(['left', 'center', 'right']),
      render: PropTypes.func,
    })
  ).isRequired,
  data: PropTypes.arrayOf(PropTypes.object),
  keyField: PropTypes.string,
  emptyMessage: PropTypes.string,
  isLoading: PropTypes.bool,
  borderless: PropTypes.bool,
};
