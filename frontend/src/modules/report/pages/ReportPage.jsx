import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EmptyState from '../../../components/EmptyState/EmptyState';
import PageLoading from '../../../components/PageLoading/PageLoading';
import Pagination from '../../../components/Pagination/Pagination';
import Table from '../../../components/Table/Table';
import { formatDateTime, truncate } from '../../../utils/utils';
import ReportDetailModal from '../components/ReportDetailModal/ReportDetailModal';
import { getMyReportsApi } from '../reportApi';
import './ReportPage.css';

const PAGE_SIZE = 10;

/**
 * ReportPage - Trang xem danh sách các báo cáo của người dùng
 */
function ReportPage({ onNavigate }) {
  const { t, i18n } = useTranslation(['report', 'common']);

  // States dữ liệu
  const [reports, setReports] = useState([]);
  const [selectedReport, setSelectedReport] = useState(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  // States phân trang & loading
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, []);

  useEffect(() => {
    let isMounted = true;

    const fetchReports = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const res = await getMyReportsApi({ page: page, size: PAGE_SIZE });
        if (!isMounted) return;

        if (res && res.success) {
          const content = res.data.content;
          setReports(content);
          setTotalPages(res.data.totalPages);
        } else {
          setError(res?.message || t('report:errors.loadFailed'));
        }
      } catch (err) {
        if (!isMounted) return;
        setError(err?.response?.data?.message || t('report:errors.loadFailed'));
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    fetchReports();

    return () => {
      isMounted = false;
    };
  }, [page, retryCount, t]);

  const handleRetry = () => {
    setRetryCount((prev) => prev + 1);
  };

  const handlePageChange = (newPage) => {
    setPage(newPage);
    window.scrollTo({ top: 0 });
  };

  const handleOpenDetail = (report) => {
    setSelectedReport(report);
    setIsDetailModalOpen(true);
  };

  const handleCloseDetail = () => {
    setIsDetailModalOpen(false);
    setSelectedReport(null);
  };

  const renderStatusBadge = (status) => {
    const badgeClass =
      status === 'RESOLVED'
        ? 'badge--mint'
        : status === 'REJECTED'
          ? 'badge--error'
          : 'badge--lavender';

    return (
      <span className={`badge ${badgeClass}`}>
        {t(`report:status.${status}`)}
      </span>
    );
  };

  const locale = i18n?.language === 'vi' ? 'vi-VN' : 'en-US';

  const columns = [
    {
      key: 'targetType',
      title: t('report:table.targetType'),
      width: 15,
      render: (value) => (
        <span className="badge badge--default report-table__target-badge">
          {t(`report:targetTypes.${value}`)}
        </span>
      ),
    },
    {
      key: 'description',
      title: t('report:table.description'),
      width: 45,
      render: (value) => (
        <span className="report-table__description" title={value}>
          {truncate(value, 80)}
        </span>
      ),
    },
    {
      key: 'status',
      title: t('report:table.status'),
      width: 18,
      render: (value) => renderStatusBadge(value),
    },
    {
      key: 'createdAt',
      title: t('report:table.createdAt'),
      width: 14,
      render: (value) => (
        <span className="report-table__created-at">
          {formatDateTime(value, locale)}
        </span>
      ),
    },
    {
      key: 'actions',
      title: t('report:table.actions'),
      width: 8,
      align: 'center',
      render: (_, row) => (
        <div className="report-table__actions">
          <button
            type="button"
            className="btn-icon"
            onClick={() => handleOpenDetail(row)}
            title={t('report:viewDetails')}
          >
            <span className="material-symbols-outlined">visibility</span>
          </button>
        </div>
      ),
    },
  ];

  return (
    <main className="report-page">
      {isLoading ? (
        <PageLoading />
      ) : error ? (
        <EmptyState
          icon="error"
          isError={true}
          description={error}
          actionLabel={t('common:retry')}
          onAction={handleRetry}
        />
      ) : reports.length === 0 ? (
        <EmptyState
          icon="flag"
          title={t('report:emptyTitle')}
          description={t('report:emptyDescription')}
        />
      ) : (
        <>
          <div className="report-page__content-card glass-card">
            <Table
              columns={columns}
              data={reports}
              rowKey="id"
              className="report-table"
            />
          </div>

          {totalPages > 1 && (
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={handlePageChange}
            />
          )}
        </>
      )}

      <ReportDetailModal
        isOpen={isDetailModalOpen}
        onClose={handleCloseDetail}
        reportId={selectedReport?.id}
        initialReport={selectedReport}
      />
    </main>
  );
}

export default ReportPage;