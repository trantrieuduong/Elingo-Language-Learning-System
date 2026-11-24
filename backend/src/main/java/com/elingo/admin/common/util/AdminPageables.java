package com.elingo.admin.common.util;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class AdminPageables {

    private AdminPageables() {}

    /**
     * Chỉ cho sort theo các thuộc tính trong allowed; ngoài danh sách thì trả về lỗi 400.
     */
    public static Pageable sanitize(Pageable pageable, Set<String> allowed) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return pageable;
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new AppException(AppError.INVALID_SORT);
            }
        }
        return pageable;
    }
}
