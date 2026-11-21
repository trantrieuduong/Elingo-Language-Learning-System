import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { formatDateTime } from '../../../../utils/utils';
import { getMyReportApi } from '../../reportApi';
import './ReportDetailModal.css';

/**
 * ReportDetailModal - Modal chỉ đọc xem chi tiết báo cáo vi phạm
 *
 * Pattern: stale-while-revalidate
 *   - Hiển thị ngay `initialReport` từ danh sách
 *   - Fetch data mới nhất theo `reportId` để cập nhật lại
 * 
 * @param {number|string} reportId - ID để fetch fresh data
 * @param {Object} initialReport - Data ban đầu từ list để hiển thị ngay
 */
function ReportDetailModal({ isOpen, onClose, reportId, initialReport }) {
  const { t, i18n } = useTranslation(['report', 'common']);
  const [report, setReport] = useState(initialReport);

  useEffect(() => {
    if (!isOpen || !reportId) {
      setReport(null);
      return;
    }

    if (initialReport) {
      setReport(initialReport);
    }

    let isMounted = true;

    const fetchReport = async () => {
      try {
        const res = await getMyReportApi(reportId);
        if (!isMounted) return;
        if (res && res.success) {
          setReport(res.data);
        }
      } catch {
        // Giữ nguyên data từ initialReport nếu fetch nền thất bại
      }
    };

    fetchReport();

    return () => {
      isMounted = false;
    };
  }, [isOpen, reportId, initialReport]);

  // Khóa scroll body và hỗ trợ bấm phím ESC để đóng modal
  useEffect(() => {
    if (!isOpen) return undefined;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose?.();
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

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
  const images = report?.imageUrls || [];

  return createPortal(
    <div className="report-detail-modal__overlay">
      <div
        className="report-detail-modal__container glass-card"
        role="dialog"
      >
        <header className="report-detail-modal__header">
          <h2 className="text-headline-sm">
            <span className="material-symbols-outlined report-detail-modal__header-icon">
              info
            </span>
            <span>{t('report:detailModalTitle')}</span>
          </h2>
          <button
            type="button"
            className="btn-icon report-detail-modal__close"
            onClick={onClose}
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </header>

        <div className="report-detail-modal__body">
          {report ? (
            <div className="report-detail-modal__grid">
                <div className="report-detail-modal__field">
                  <span className="report-detail-modal__label">{t('report:fields.targetType')}</span>
                  <div className="report-detail-modal__value">
                    <span className="badge badge--default">
                      {t(`report:targetTypes.${report.targetType}`)}
                    </span>
                  </div>
                </div>

                <div className="report-detail-modal__field">
                  <span className="report-detail-modal__label">{t('report:fields.status')}</span>
                  <div className="report-detail-modal__value">
                    {renderStatusBadge(report.status)}
                  </div>
                </div>

                <div className="report-detail-modal__field">
                  <span className="report-detail-modal__label">{t('report:fields.createdAt')}</span>
                  <div className="report-detail-modal__value">
                    {formatDateTime(report.createdAt, locale)}
                  </div>
                </div>

                <div className="report-detail-modal__field">
                  <span className="report-detail-modal__label">{t('report:fields.resolvedAt')}</span>
                  <div className="report-detail-modal__value">
                    {formatDateTime(report.resolvedAt, locale) || '—'}
                  </div>
                </div>

                <div className="report-detail-modal__field report-detail-modal__field--full">
                  <span className="report-detail-modal__label">{t('report:fields.description')}</span>
                  <div className="report-detail-modal__text-box">
                    {report.description}
                  </div>
                </div>

                <div className="report-detail-modal__field report-detail-modal__field--full">
                  <span className="report-detail-modal__label">{t('report:fields.image')}</span>
                  {images.length > 0 ? (
                    <div className="report-detail-modal__images">
                      {images.map((imgUrl, index) => (
                        <a
                          key={`${imgUrl}-${index}`}
                          href={imgUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="report-detail-modal__image-link"
                        >
                          <img
                            src={imgUrl}
                            alt={`${t('report:fields.image')} ${index + 1}`}
                            className="report-detail-modal__image-thumb"
                          />
                        </a>
                      ))}
                    </div>
                  ) : (
                    <p className="text-body-md text-disabled">{t('report:noImages')}</p>
                  )}
                </div>

                <div className="report-detail-modal__field report-detail-modal__field--full">
                  <span className="report-detail-modal__label">{t('report:fields.resolutionNote')}</span>
                  <div className="report-detail-modal__text-box report-detail-modal__resolution-box">
                    {report.resolutionNote ? (
                      report.resolutionNote
                    ) : (
                      <span className="text-disabled">{t('report:noResolutionNote')}</span>
                    )}
                  </div>
                </div>
              </div>
          ) : null}
        </div>

        <footer className="report-detail-modal__footer">
          <button type="button" className="btn-secondary" onClick={onClose}>
            {t('common:close')}
          </button>
        </footer>
      </div>
    </div>,
    document.body
  );
}

export default ReportDetailModal;
