package AgriTrackBackend.CHAT;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByOwnerIdOrderByLastMessageTimeDesc(Long ownerId);

    List<Chat> findByClientIdOrderByLastMessageTimeDesc(Long clientId);

    Optional<Chat> findByOwnerIdAndClientId(Long ownerId, Long clientId);
}
