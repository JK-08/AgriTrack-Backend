package AgriTrackBackend.ONBOARDING;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {

    @Query("SELECT o FROM Onboarding o WHERE " +
            "(:search IS NULL OR LOWER(o.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(o.subtitle) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Onboarding> search(@Param("search") String search, Pageable pageable);
}
