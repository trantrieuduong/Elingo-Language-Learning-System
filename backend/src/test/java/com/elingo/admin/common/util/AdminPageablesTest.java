package com.elingo.admin.common.util;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("AdminPageables")
class AdminPageablesTest {

    private static final Set<String> ALLOWED = Set.of("id", "createdAt", "title");

    @Nested
    @DisplayName("sanitize")
    class SanitizeTests {

        @Test
        @DisplayName("should return null when pageable is null")
        void shouldReturnNull_whenPageableIsNull() {
            Pageable result = AdminPageables.sanitize(null, ALLOWED);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return pageable when unsorted")
        void shouldReturnPageable_whenUnsorted() {
            Pageable unsorted = PageRequest.of(0, 10);
            Pageable result = AdminPageables.sanitize(unsorted, ALLOWED);
            assertThat(result).isSameAs(unsorted);
        }

        @Test
        @DisplayName("should return pageable when single sort field is allowed")
        void shouldReturnPageable_whenSingleSortFieldAllowed() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            Pageable result = AdminPageables.sanitize(pageable, ALLOWED);
            assertThat(result).isSameAs(pageable);
        }

        @Test
        @DisplayName("should return pageable when all sort fields are allowed")
        void shouldReturnPageable_whenAllSortFieldsAllowed() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("id").and(Sort.by("title")));
            Pageable result = AdminPageables.sanitize(pageable, ALLOWED);
            assertThat(result).isSameAs(pageable);
        }

        @Test
        @DisplayName("should throw INVALID_SORT when sort field is not allowed")
        void shouldThrowInvalidSort_whenSortFieldNotAllowed() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("passwordHash"));

            assertThatThrownBy(() -> AdminPageables.sanitize(pageable, ALLOWED))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_SORT));
        }

        @Test
        @DisplayName("should throw INVALID_SORT when any sort field in multi-sort is not allowed")
        void shouldThrowInvalidSort_whenAnySortFieldNotAllowed() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("title").and(Sort.by("secretToken")));

            assertThatThrownBy(() -> AdminPageables.sanitize(pageable, ALLOWED))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_SORT));
        }
    }
}
