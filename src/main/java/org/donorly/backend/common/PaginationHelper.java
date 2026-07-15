package org.donorly.backend.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Shared page-request construction: clamps the page index to >= 0 and the page size
 * to 1..200 so a client cannot request an unbounded result set.
 */
public final class PaginationHelper {

    public static final int MAX_PAGE_SIZE = 200;

    private PaginationHelper() {
    }

    public static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    public static PageRequest pageRequest(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), clampSize(size), sort);
    }

    /** Most list endpoints sort newest-first. */
    public static PageRequest newestFirst(int page, int size) {
        return pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
