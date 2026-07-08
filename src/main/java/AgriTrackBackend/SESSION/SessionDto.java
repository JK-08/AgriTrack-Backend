package AgriTrackBackend.SESSION;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Read-only view of a session — never exposes the refresh token hash. */
@Getter
@Setter
public class SessionDto {
    private Long id;
    private String deviceId;
    private String deviceName;
    private String platform;
    private String appVersion;
    private String ipAddress;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private LocalDateTime expiresAt;
    private boolean revoked;

    public static SessionDto from(UserSession s) {
        SessionDto dto = new SessionDto();
        dto.setId(s.getId());
        dto.setDeviceId(s.getDeviceId());
        dto.setDeviceName(s.getDeviceName());
        dto.setPlatform(s.getPlatform());
        dto.setAppVersion(s.getAppVersion());
        dto.setIpAddress(s.getIpAddress());
        dto.setLoginTime(s.getLoginTime());
        dto.setLastActivity(s.getLastActivity());
        dto.setExpiresAt(s.getExpiresAt());
        dto.setRevoked(Boolean.TRUE.equals(s.getRevoked()));
        return dto;
    }
}
