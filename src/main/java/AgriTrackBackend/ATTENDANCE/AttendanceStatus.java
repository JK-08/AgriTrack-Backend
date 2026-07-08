package AgriTrackBackend.ATTENDANCE;

public final class AttendanceStatus {
    public static final String PRESENT = "PRESENT";
    public static final String ABSENT = "ABSENT";
    public static final String LEAVE = "LEAVE";
    public static final String HALF_DAY = "HALF_DAY";

    public static final java.util.Set<String> ALL = java.util.Set.of(PRESENT, ABSENT, LEAVE, HALF_DAY);

    private AttendanceStatus() {}
}
