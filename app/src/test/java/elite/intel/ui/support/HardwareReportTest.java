package elite.intel.ui.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The report is read on a machine that is not the one it describes, so what matters is that every line
 * the JDK can always answer is present, and that a volume that cannot be asked about costs nothing.
 */
class HardwareReportTest {

    @Test
    void carriesTheLinesTheJdkAlwaysKnows(@TempDir Path tmp) {
        String report = HardwareReport.describe(tmp);

        assertTrue(report.contains("CPU: "), report);
        assertTrue(report.contains("Memory: "), report);
        assertTrue(report.contains("JVM heap: "), report);
        assertTrue(report.contains("GPU: "), report);
        assertTrue(report.contains("Disk " + tmp + ": "), report);
        assertTrue(report.endsWith("\n"), report);
    }

    @Test
    void aVolumeThatDoesNotExistIsSkippedWithoutComment(@TempDir Path tmp) {
        Path missing = tmp.resolve("never-created");

        String report = HardwareReport.describe(missing, null);

        assertFalse(report.contains(missing.toString()), report);
        assertFalse(report.contains("could not be queried"), report);
    }
}
