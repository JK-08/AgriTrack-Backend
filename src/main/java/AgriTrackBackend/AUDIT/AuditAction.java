package AgriTrackBackend.AUDIT;

public final class AuditAction {
    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String STATUS_CHANGE = "STATUS_CHANGE";
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String LOGOUT_ALL = "LOGOUT_ALL";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String MPIN_RESET = "MPIN_RESET";

    private AuditAction() {}
}
