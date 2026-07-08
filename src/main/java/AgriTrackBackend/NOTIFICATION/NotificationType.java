package AgriTrackBackend.NOTIFICATION;

import java.util.List;

/**
 * Curated set of notification types the app actually generates today.
 * Used to seed default preference rows and to validate preference updates.
 * Free-text notifications sent via /notification/create may still use other
 * values — those simply fall back to the default preferences (push+in-app
 * enabled, sms+email disabled) since no explicit row exists for them.
 */
public final class NotificationType {
    public static final String BOOKING = "BOOKING";
    public static final String PAYMENT = "PAYMENT";
    public static final String DOCUMENT_EXPIRY = "DOCUMENT_EXPIRY";
    public static final String RATE_ALERT = "RATE_ALERT";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String PAYROLL = "PAYROLL";
    public static final String CHAT = "CHAT";
    public static final String GENERAL = "GENERAL";

    public static final List<String> ALL = List.of(
            BOOKING, PAYMENT, DOCUMENT_EXPIRY, RATE_ALERT, MAINTENANCE, PAYROLL, CHAT, GENERAL);

    private NotificationType() {}
}
