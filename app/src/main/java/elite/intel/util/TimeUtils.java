package elite.intel.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtils {
    //public static final String ISO_8601 = "yyyy-MM-dd HH:mm:ss";

    // --------------------------------------------------------------
    // 1. The classic human-readable one (what you currently have)
    // --------------------------------------------------------------
    /** Classic local date-time pattern used in logs, DB dumps, filenames, etc. */
    public static final String LOCAL_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    /** Same as above but with milliseconds – very common in detailed logs */
    public static final String LOCAL_DATE_TIME_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";

    // --------------------------------------------------------------
    // 2. Proper ISO 8601 / RFC 3339 patterns (the ones you SHOULD use for APIs)
    // --------------------------------------------------------------
    public static final String ISO_INSTANT = "yyyy-MM-dd'T'HH:mm:ss'Z'";  // ← no millis, Z literal

    /** Most common real-world API format (with fractional seconds + Z) */
    public static final String ISO_OFFSET_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /** Fully compliant RFC 3339 / ISO 8601 with optional millis and proper offset support */
    public static final String RFC_3339 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String ISO_LOCAL_DATE = "yyyy-MM-dd";
    public static final String ISO_LOCAL_TIME = "HH:mm:ss";
    public static final String FILE_SAFE_DATE_TIME = "yyyy-MM-dd_HH-mm-ss";



    /**
     * Transforms date "yyyy-MM-dd HH:mm:ss" in to YHM
     *
     */
    public static String transformToYMDHtimeAgo(String dateAsString, String pattern) {
        // handle the 'T' separator only if needed for a space-separated pattern
        String value = dateAsString.substring(0, 19);
        if (!pattern.contains("'T'")) {
            value = value.replace('T', ' ');
        }
        LocalDateTime updatedDateTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(updatedDateTime, now);

        long years = duration.toDays() / 365;
        long days = duration.toDays() % 365;
        long months = days / 30;
        days = days % 30;
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(StringUtls.localizedEventPlural((int) years, "event.time.years")).append(", ");
        if (months > 0) sb.append(StringUtls.localizedEventPlural((int) months, "event.time.months")).append(", ");
        if (days > 0) sb.append(StringUtls.localizedEventPlural((int) days, "event.time.days")).append(", ");
        if (hours > 0) sb.append(StringUtls.localizedEventPlural((int) hours, "event.time.hours")).append(", ");
        sb.append(StringUtls.localizedEventPlural((int) minutes, "event.time.minutes"));
        return StringUtls.localizedEvent("event.time.ago", sb.toString());
    }
}
