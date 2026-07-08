package AgriTrackBackend.CHAT;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatIdOrderByMessageIdAsc(Long chatId);

    @Query("SELECT m FROM Message m WHERE m.chatId = :chatId " +
            "AND (:search IS NULL OR LOWER(m.messageText) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:isRead IS NULL OR m.isRead = :isRead)")
    Page<Message> search(@Param("chatId") Long chatId,
                          @Param("search") String search,
                          @Param("isRead") Boolean isRead,
                          Pageable pageable);
}
