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
        LocalDateTime updatedDateTime = LocalDateTime.parse(dateAsString.substring(0, 19), DateTimeFormatter.ofPattern(pattern));
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(updatedDateTime, now);

        long years = duration.toDays() / 365;
        long days = duration.toDays() % 365;
        long months = days / 30;
        days = days % 30;
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        String timeAgo = (years > 0 ? years + " years, " : "") +
                (months > 0 ? months + " months, " : "") +
                (days > 0 ? days + " days, " : "") +
                (hours > 0 ? hours + " hours, " : "") +
                minutes + " minutes ago";
        return timeAgo;
    }
}
