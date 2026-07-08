package AgriTrackBackend.COMMON;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * One place that turns ?page&size&sortBy&sortDir query params into a
 * Pageable, so every controller applies the same defaults and the same
 * upper bound on page size (preventing a client from requesting size=100000
 * and defeating the point of pagination).
 */
public final class PaginationUtil {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PaginationUtil() {}

    public static Pageable build(Integer page, Integer size, String sortBy, String sortDir, String defaultSortBy) {
        int safePage = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        String field = (sortBy == null || sortBy.isBlank()) ? defaultSortBy : sortBy;
        Sort.Direction direction = (sortDir != null && sortDir.equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}
