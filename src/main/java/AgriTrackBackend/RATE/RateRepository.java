package AgriTrackBackend.RATE;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateRepository extends JpaRepository<Rate, Long> {

    List<Rate> findByOwnerId(Long ownerId);

    List<Rate> findByOwnerIdAndIsActiveTrue(Long ownerId);
}
