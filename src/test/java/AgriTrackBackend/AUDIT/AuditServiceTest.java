package AgriTrackBackend.AUDIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuditService is deliberately best-effort: a repository failure must never
 * propagate and break the business operation it's describing.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();
        org.springframework.test.util.ReflectionTestUtils.setField(auditService, "repository", repository);
    }

    @Test
    void logSerializesBeforeAndAfterAsJsonAndRecordsNullUserWhenUnauthenticated() {
        auditService.log(AuditAction.UPDATE, "Customer", 7L,
                Map.of("name", "Old"), Map.of("name", "New"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(saved.getEntityType()).isEqualTo("Customer");
        assertThat(saved.getEntityId()).isEqualTo("7");
        assertThat(saved.getBeforeValue()).contains("Old");
        assertThat(saved.getAfterValue()).contains("New");
        assertThat(saved.getUserId()).isNull(); // no authenticated CurrentUser in a plain unit test
    }

    @Test
    void logRawStoresPreSerializedBeforeSnapshotVerbatim() {
        auditService.logRaw(AuditAction.DELETE, "Tractor", 3L, "{\"status\":\"AVAILABLE\"}", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBeforeValue()).isEqualTo("{\"status\":\"AVAILABLE\"}");
        assertThat(captor.getValue().getAfterValue()).isNull();
    }

    @Test
    void neverThrowsWhenRepositorySaveFails() {
        when(repository.save(any())).thenThrow(new RuntimeException("DB down"));

        // must not propagate — the caller's business transaction should be unaffected
        auditService.log(AuditAction.CREATE, "Booking", 1L, null, null);
    }
}
