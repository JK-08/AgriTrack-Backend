package AgriTrackBackend.ATTENDANCE;

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
public interface DriverAttendanceRepository extends JpaRepository<DriverAttendance, Long> {

    Optional<DriverAttendance> findByDriverIdAndAttendanceDate(Long driverId, LocalDate attendanceDate);

    List<DriverAttendance> findByDriverIdAndAttendanceDateBetween(Long driverId, LocalDate from, LocalDate to);

    List<DriverAttendance> findByOwnerIdAndAttendanceDate(Long ownerId, LocalDate attendanceDate);

    @Query("SELECT a FROM DriverAttendance a WHERE a.ownerId = :ownerId " +
            "AND (:driverId IS NULL OR a.driverId = :driverId) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:from IS NULL OR a.attendanceDate >= :from) " +
            "AND (:to IS NULL OR a.attendanceDate <= :to)")
    Page<DriverAttendance> search(@Param("ownerId") Long ownerId,
                                   @Param("driverId") Long driverId,
                                   @Param("status") String status,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   Pageable pageable);
}
