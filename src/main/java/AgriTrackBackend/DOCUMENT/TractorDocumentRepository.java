package AgriTrackBackend.DOCUMENT;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TractorDocumentRepository extends JpaRepository<TractorDocument, Long> {

    List<TractorDocument> findByTractorIdOrderByDocumentTypeAsc(Long tractorId);

    Optional<TractorDocument> findByTractorIdAndDocumentType(Long tractorId, String documentType);

    List<TractorDocument> findByOwnerIdAndExpiryDateBetweenAndReminderSentFalse(
            Long ownerId, LocalDate from, LocalDate to);

    // used by the scheduled reminder job across all owners
    List<TractorDocument> findByExpiryDateBetweenAndReminderSentFalse(LocalDate from, LocalDate to);

    @Query("SELECT d FROM TractorDocument d WHERE d.ownerId = :ownerId " +
            "AND (:documentType IS NULL OR d.documentType = :documentType) " +
            "AND (:tractorId IS NULL OR d.tractorId = :tractorId) " +
            "AND (:expiringBefore IS NULL OR d.expiryDate <= :expiringBefore)")
    Page<TractorDocument> search(@Param("ownerId") Long ownerId,
                                  @Param("documentType") String documentType,
                                  @Param("tractorId") Long tractorId,
                                  @Param("expiringBefore") LocalDate expiringBefore,
                                  Pageable pageable);
}
