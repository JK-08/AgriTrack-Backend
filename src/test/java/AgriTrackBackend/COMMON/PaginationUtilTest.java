package AgriTrackBackend.COMMON;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationUtilTest {

    @Test
    void appliesDefaultsWhenNothingSpecified() {
        Pageable p = PaginationUtil.build(null, null, null, null, "createdAt");
        assertThat(p.getPageNumber()).isEqualTo(0);
        assertThat(p.getPageSize()).isEqualTo(20);
        assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void capsPageSizeAtMax() {
        Pageable p = PaginationUtil.build(0, 5000, null, null, "createdAt");
        assertThat(p.getPageSize()).isEqualTo(PaginationUtil.MAX_SIZE);
    }

    @Test
    void negativePageFallsBackToZero() {
        Pageable p = PaginationUtil.build(-5, 10, null, null, "createdAt");
        assertThat(p.getPageNumber()).isEqualTo(0);
    }

    @Test
    void honorsExplicitSortFieldAndAscendingDirection() {
        Pageable p = PaginationUtil.build(1, 10, "name", "asc", "createdAt");
        assertThat(p.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(p.getPageNumber()).isEqualTo(1);
    }
}
