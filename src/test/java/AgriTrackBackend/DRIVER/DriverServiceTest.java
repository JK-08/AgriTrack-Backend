package AgriTrackBackend.DRIVER;

import AgriTrackBackend.AUDIT.AuditAction;
import AgriTrackBackend.AUDIT.AuditService;
import AgriTrackBackend.COMMON.PageResponse;
import AgriTrackBackend.EXCEPTION.ForbiddenException;
import AgriTrackBackend.SECURITY.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers the pagination/search + audit + ownership-authorization pattern
 * applied to Driver in the "finish remaining modules" hardening pass.
 */
@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository repository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DriverService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId, String role) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "user" + userId + "@example.com", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void saveStampsOwnerFromCallerAndAuditsCreate() {
        loginAs(1L, "OWNER");
        Driver toSave = new Driver();
        when(repository.saveAndFlush(any(Driver.class))).thenAnswer(inv -> {
            Driver d = inv.getArgument(0);
            d.setDriverId(10L);
            return d;
        });

        Driver saved = service.save(toSave);

        assertThat(saved.getOwnerId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getIsAvailable()).isTrue();
        verify(auditService).log(eq(AuditAction.CREATE), eq("Driver"), eq(10L), eq(null), eq(saved));
    }

    @Test
    void searchPagedRejectsRequestForAnotherOwnersDrivers() {
        loginAs(1L, "OWNER");

        assertThatThrownBy(() -> service.searchPaged(2L, null, null, null, null, null, null, null))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void searchPagedDelegatesToRepositoryAndWrapsInPageResponse() {
        loginAs(1L, "OWNER");
        Driver d = new Driver();
        d.setDriverId(5L);
        d.setOwnerId(1L);
        Page<Driver> page = new PageImpl<>(List.of(d));
        when(repository.search(eq(1L), eq("john"), eq("ACTIVE"), eq(true), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<Driver> result = service.searchPaged(1L, "john", "ACTIVE", true, 0, 20, "createdAt", "desc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchPagedTreatsBlankSearchAndStatusAsNull() {
        loginAs(1L, "OWNER");
        when(repository.search(eq(1L), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.searchPaged(1L, "   ", "", null, null, null, null, null);

        verify(repository).search(eq(1L), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void updateRejectsNonOwnerAndDoesNotAudit() {
        loginAs(2L, "OWNER"); // not the driver's owner
        Driver existing = new Driver();
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        existing.setUserId(99L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(10L, new Driver()))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void updateByOwnerSnapshotsBeforeStateAndAuditsUpdate() {
        loginAs(1L, "OWNER");
        Driver existing = new Driver();
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        existing.setUserId(99L);
        existing.setStatus("ACTIVE");
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{\"status\":\"ACTIVE\"}");
        when(repository.saveAndFlush(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        Driver update = new Driver();
        update.setStatus("INACTIVE");
        Driver result = service.update(10L, update);

        assertThat(result.getStatus()).isEqualTo("INACTIVE");
        verify(auditService).logRaw(eq(AuditAction.UPDATE), eq("Driver"), eq(10L),
                eq("{\"status\":\"ACTIVE\"}"), eq(result));
    }

    @Test
    void setAvailabilityAuditsAsStatusChange() {
        loginAs(1L, "OWNER");
        Driver existing = new Driver();
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        existing.setUserId(99L);
        existing.setIsAvailable(true);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{\"isAvailable\":true}");
        when(repository.saveAndFlush(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setAvailability(10L, false);

        verify(auditService).logRaw(eq(AuditAction.STATUS_CHANGE), eq("Driver"), eq(10L), any(), any());
    }

    @Test
    void deleteByIdRejectsNonOwner() {
        loginAs(2L, "OWNER");
        Driver existing = new Driver();
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        existing.setUserId(99L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.deleteById(10L)).isInstanceOf(ForbiddenException.class);
        verify(repository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteByIdByOwnerAuditsDelete() {
        loginAs(1L, "OWNER");
        Driver existing = new Driver();
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        existing.setUserId(99L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn("{}");

        service.deleteById(10L);

        verify(repository).delete(existing);
        verify(auditService).logRaw(eq(AuditAction.DELETE), eq("Driver"), eq(10L), eq("{}"), eq(null));
    }

    @Test
    void getByIdRejectsStrangerWhoIsNeitherOwnerNorTheDriverThemselves() {
        loginAs(3L, "OWNER");
        Driver existing = new Driver();
        existing.setDriverId(10L);
        existing.setOwnerId(1L);
        existing.setUserId(99L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.getById(10L)).isInstanceOf(ForbiddenException.class);
    }
}
