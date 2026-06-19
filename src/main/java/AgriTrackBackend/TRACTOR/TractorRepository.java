package AgriTrackBackend.TRACTOR;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TractorRepository extends JpaRepository<Tractor, Long> {

    List<Tractor> findByOwnerId(Long ownerId);

    List<Tractor> findByOwnerIdAndStatus(Long ownerId, String status);
}
