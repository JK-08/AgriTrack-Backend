package AgriTrackBackend.NOTIFICATION;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPreferenceRequest {

    @NotBlank(message = "notificationType is required")
    @Pattern(regexp = "BOOKING|PAYMENT|DOCUMENT_EXPIRY|RATE_ALERT|MAINTENANCE|PAYROLL|CHAT|GENERAL",
            message = "notificationType must be one of: BOOKING, PAYMENT, DOCUMENT_EXPIRY, RATE_ALERT, MAINTENANCE, PAYROLL, CHAT, GENERAL")
    private String notificationType;

    private boolean pushEnabled = true;
    private boolean inAppEnabled = true;
    private boolean smsEnabled = false;
    private boolean emailEnabled = false;
}
