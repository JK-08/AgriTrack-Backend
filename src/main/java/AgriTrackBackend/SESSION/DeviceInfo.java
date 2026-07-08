package AgriTrackBackend.SESSION;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Device metadata pulled from request headers. All headers are optional so
 * older app builds that don't send them yet keep working — they just show
 * up as "unknown" in the sessions list.
 */
public record DeviceInfo(String deviceId, String deviceName, String platform, String appVersion) {

    public static DeviceInfo from(HttpServletRequest request) {
        return new DeviceInfo(
                header(request, "X-Device-Id", "unknown-device"),
                header(request, "X-Device-Name", "Unknown device"),
                header(request, "X-Platform", "unknown"),
                header(request, "X-App-Version", "unknown")
        );
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String header(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
