package AgriTrackBackend.EXPENSE;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByOwnerIdOrderByExpenseDateDesc(Long ownerId);

    List<Expense> findByOwnerIdAndExpenseDateBetween(Long ownerId, LocalDate from, LocalDate to);

    @Query("SELECT e FROM Expense e WHERE e.ownerId = :ownerId " +
            "AND (:search IS NULL OR LOWER(e.vendor) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:tractorId IS NULL OR e.tractorId = :tractorId) " +
            "AND (:from IS NULL OR e.expenseDate >= :from) " +
            "AND (:to IS NULL OR e.expenseDate <= :to)")
    Page<Expense> search(@Param("ownerId") Long ownerId,
                          @Param("search") String search,
                          @Param("category") String category,
                          @Param("tractorId") Long tractorId,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          Pageable pageable);
}
