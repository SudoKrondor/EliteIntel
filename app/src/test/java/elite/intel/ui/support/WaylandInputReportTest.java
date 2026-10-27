package elite.intel.ui.support;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The report answers two questions a commander once had to answer by hand: which versions, and whether
 * Xwayland hands key presses to the compositor. Both are read from text the programs print and the bytes
 * of the binary, so those readings are what is pinned here.
 */
class WaylandInputReportTest {

    @Test
    void readsTheVersionEachProgramPrints() {
        assertEquals(Optional.of("6.5.3"), WaylandInputReport.parseVersion("kwin 6.5.3\n"));
        assertEquals(Optional.of("24.1.10"), WaylandInputReport.parseVersion(
                "The X.Org Foundation Xwayland Version 24.1.10 (12401010)\nX Protocol Version 11, Revision 0\n"));
        assertEquals(Optional.empty(), WaylandInputReport.parseVersion("command not found"));
    }

    @Test
    void findsLibeiAmongTheLibrariesABinaryLoads() {
        byte[] linked = "\0libXau.so.6\0libei.so.1\0libc.so.6\0".getBytes(StandardCharsets.US_ASCII);
        byte[] unlinked = "\0libXau.so.6\0libepoxy.so.0\0libc.so.6\0".getBytes(StandardCharsets.US_ASCII);

        assertTrue(WaylandInputReport.linksLibei(linked));
        assertFalse(WaylandInputReport.linksLibei(unlinked));
    }
}
