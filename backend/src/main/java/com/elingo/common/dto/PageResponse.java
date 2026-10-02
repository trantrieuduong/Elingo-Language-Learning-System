package com.elingo.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper
 *
 * @param <T>           element type
 * @param content       items in the current page
 * @param page          0-based current page index
 * @param size          number of items per page
 * @param totalElements total number of matching records
 * @param totalPages    total number of pages
 * @param last          whether this is the last page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last//sai quy tắc -> đúng là isLast 
) {// Constructor chính của record: tiện giả lập ở file test
    /**
     * Convenience factory that derives some built-in method from
     * org.springframework.data.domain.Page
     */
    public static <T> PageResponse<T> of(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }
    // static factory chuyển Page của Spring tầng Repository trả về sang PageResponse
    // do chỉ cần 1 số field từ Page của Spring (không dùng hết)
}
