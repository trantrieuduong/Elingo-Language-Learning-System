package com.elingo.common.dto;

import java.util.List;
import java.util.function.Function;

/**
 * Generic cursor-based paginated response wrapper for infinite scroll / lazy loading.
 *
 * @param <T>        element type
 * @param items      items in the current slice
 * @param nextCursor cursor pointer for fetching the next slice (null if no more)
 * @param hasMore    whether more items exist after this slice
 */
public record CursorResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {
    public static <T> CursorResponse<T> of(List<T> items, int limit, Function<T, String> cursorExtractor) {
        if (items == null || items.isEmpty()) {
            return new CursorResponse<>(List.of(), null, false);
        }

        boolean hasMore = items.size() > limit;
        List<T> pagedItems = hasMore ? items.subList(0, limit) : items;
        String nextCursor = (hasMore && !pagedItems.isEmpty())
                ? cursorExtractor.apply(pagedItems.get(pagedItems.size() - 1))
                : null;

        return new CursorResponse<>(pagedItems, nextCursor, hasMore);
    }
}

