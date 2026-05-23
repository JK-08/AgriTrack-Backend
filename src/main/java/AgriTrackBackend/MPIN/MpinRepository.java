package AgriTrackBackend.MPIN;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MpinRepository extends JpaRepository<Mpin, Long> {

    Optional<Mpin> findByUserUserId(Long userId);

    Optional<Mpin> findByUserMobileNo(String mobileNo);
}