package AgriTrackBackend.RATING;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByOwnerIdOrderByRatingIdDesc(Long ownerId);

    List<Rating> findByClientIdOrderByRatingIdDesc(Long clientId);

    @Query("SELECT r FROM Rating r WHERE r.ownerId = :ownerId " +
            "AND (:minValue IS NULL OR r.ratingValue >= :minValue) " +
            "AND (:search IS NULL OR LOWER(r.review) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Rating> search(@Param("ownerId") Long ownerId,
                         @Param("minValue") Integer minValue,
                         @Param("search") String search,
                         Pageable pageable);
}
