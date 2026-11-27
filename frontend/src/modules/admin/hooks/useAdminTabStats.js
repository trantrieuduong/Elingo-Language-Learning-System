import { useEffect, useState } from 'react';

/**
 * Hook tải dữ liệu thống kê cho các tab Dashboard Admin, tự động xử lý hủy khi unmount.
 * @param {Function} fetcher - Hàm async trả về dữ liệu (thường dùng Promise.all)
 * @param {Array} deps - Danh sách dependency kích hoạt tải lại
 * @param {string} errorFallback - Thông báo lỗi mặc định
 */
export default function useAdminTabStats(fetcher, deps, errorFallback) {
  const [state, setState] = useState({
    loading: true,
    error: null,
    data: null,
  });

  useEffect(() => {
    let isMounted = true;
    setState((prev) => ({ ...prev, loading: true, error: null }));

    Promise.resolve()
      .then(() => fetcher())
      .then((data) => {
        if (isMounted) {
          setState({ loading: false, error: null, data });
        }
      })
      .catch((err) => {
        if (isMounted) {
          setState({
            loading: false,
            error: err?.message || errorFallback,
            data: null,
          });
        }
      });

    return () => {
      isMounted = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return state;
}
