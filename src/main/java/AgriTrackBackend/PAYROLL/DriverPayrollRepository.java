package AgriTrackBackend.PAYROLL;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverPayrollRepository extends JpaRepository<DriverPayroll, Long> {

    Optional<DriverPayroll> findByDriverIdAndPayrollMonth(Long driverId, String payrollMonth);

    List<DriverPayroll> findByOwnerIdAndPayrollMonth(Long ownerId, String payrollMonth);

    @Query("SELECT p FROM DriverPayroll p WHERE p.ownerId = :ownerId " +
            "AND (:driverId IS NULL OR p.driverId = :driverId) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:month IS NULL OR p.payrollMonth = :month)")
    Page<DriverPayroll> search(@Param("ownerId") Long ownerId,
                                @Param("driverId") Long driverId,
                                @Param("status") String status,
                                @Param("month") String month,
                                Pageable pageable);
}
