package elite.intel.ui.dialog;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A user's tick is stored under a notice's name (Storage S21), so the names are fixed: renaming one would show a
 * quieted dialog again, and two notices sharing a name would quiet each other.
 */
class QuietableNoticeTest {

    @Test
    void theNamesAreTheOnesStored() {
        Map<QuietableNotice, String> keys = Arrays.stream(QuietableNotice.values())
                .collect(Collectors.toMap(n -> n, QuietableNotice::key));

        assertEquals(Map.of(
                QuietableNotice.BINDFORGE_APPLY_CONFIRM, "bindforge.apply.confirm",
                QuietableNotice.BINDFORGE_APPLY_REPORT, "bindforge.apply.report",
                QuietableNotice.BINDFORGE_FIRST_TIME_STARTUP_REPORT, "bindforge.firstTimeStartup.report"), keys);
    }

    @Test
    void everyNameIsItsOwn() {
        long distinct = Arrays.stream(QuietableNotice.values()).map(QuietableNotice::key).distinct().count();

        assertEquals(QuietableNotice.values().length, distinct);
    }
}
