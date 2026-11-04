import { useCallback, useEffect, useRef, useState } from 'react';

function useScrollEnd(isEnabled = true) {
  const scrollContainerRef = useRef(null);
  const [isAtEnd, setIsAtEnd] = useState(false);

  const updateScrollPosition = useCallback(() => {
    const scrollContainer = scrollContainerRef.current;
    if (!scrollContainer) return;

    const nextIsAtEnd =
      scrollContainer.scrollHeight - scrollContainer.clientHeight - scrollContainer.scrollTop <= 1;
    setIsAtEnd((previousIsAtEnd) =>
      previousIsAtEnd === nextIsAtEnd ? previousIsAtEnd : nextIsAtEnd
    );
  }, []);

  useEffect(() => {
    if (!isEnabled) return undefined;

    const scrollContainer = scrollContainerRef.current;
    if (!scrollContainer) return undefined;

    const resizeObserver = new ResizeObserver(updateScrollPosition);
    resizeObserver.observe(scrollContainer);
    Array.from(scrollContainer.children).forEach((child) => resizeObserver.observe(child));

    const mutationObserver = new MutationObserver(() => {
      Array.from(scrollContainer.children).forEach((child) => resizeObserver.observe(child));
      updateScrollPosition();
    });
    mutationObserver.observe(scrollContainer, {
      childList: true,
      subtree: true,
      characterData: true,
    });

    updateScrollPosition();
    return () => {
      resizeObserver.disconnect();
      mutationObserver.disconnect();
    };
  }, [isEnabled, updateScrollPosition]);

  return {
    scrollContainerRef,
    isAtEnd,
    handleScroll: updateScrollPosition,
  };
}

export default useScrollEnd;
