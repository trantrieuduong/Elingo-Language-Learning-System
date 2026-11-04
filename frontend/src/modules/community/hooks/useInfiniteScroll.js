import { useCallback, useEffect, useRef, useState } from 'react';

function useInfiniteScroll({
  cursor,
  fetchPage,
  hasMore,
  isLoadingFirstPage,
  onPageLoaded,
  requestGenerationRef,
}) {
  const sentinelRef = useRef(null);
  const callbacksRef = useRef({ fetchPage, onPageLoaded });
  const isLoadingMoreRef = useRef(false);
  const isMountedRef = useRef(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  useEffect(() => {
    callbacksRef.current = { fetchPage, onPageLoaded };
  }, [fetchPage, onPageLoaded]);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const resetLoadingMore = useCallback(() => {
    isLoadingMoreRef.current = false;
    setIsLoadingMore(false);
  }, []);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver((entries) => {
      const [target] = entries;
      if (!target?.isIntersecting || !hasMore || isLoadingMoreRef.current || isLoadingFirstPage) {
        return;
      }

      const requestGeneration = requestGenerationRef.current;
      isLoadingMoreRef.current = true;
      setIsLoadingMore(true);

      Promise.resolve()
        .then(() => callbacksRef.current.fetchPage(cursor))
        .then((pageData) => {
          if (isMountedRef.current && requestGeneration === requestGenerationRef.current) {
            callbacksRef.current.onPageLoaded(pageData);
          }
        })
        .catch(() => {})
        .finally(() => {
          if (isMountedRef.current && requestGeneration === requestGenerationRef.current) {
            isLoadingMoreRef.current = false;
            setIsLoadingMore(false);
          }
        });
    });

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [cursor, hasMore, isLoadingFirstPage, requestGenerationRef]);

  return { isLoadingMore, resetLoadingMore, sentinelRef };
}

export default useInfiniteScroll;
