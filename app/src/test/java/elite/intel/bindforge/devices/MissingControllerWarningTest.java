package elite.intel.bindforge.devices;

import org.junit.jupiter.api.Test;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The once-per-run gate: one warning per Elite-Intel process, spoken and logged, never used up while nothing can see
 * the hardware (Alan, 2026-10-11, A13).
 */
class MissingControllerWarningTest {

    private static final Path BINDINGS = Path.of("bindings");
    private static final List<MyDevice> LVWAP_UNPLUGGED =
            List.of(new MyDevice(null, "LVWAP", "3344", "83F4", false, "LVWAP", false));

    private final List<String> spoken = new ArrayList<>();
    private final List<String> logged = new ArrayList<>();

    @Test
    void aMissingControllerIsSpokenAndLoggedWithItsBindings() {
        MissingControllerWarning warning = warning(folder -> Map.of("LVWAP", 142));

        warning.judgeOnce(true, LVWAP_UNPLUGGED, BINDINGS);

        assertEquals(1, spoken.size());
        assertTrue(spoken.getFirst().contains("LVWAP"));
        assertEquals(List.of("Controller LVWAP is not connected - 142 bindings name it"), logged);
        assertFalse(warning.pending());
    }

    @Test
    void severalAreSpokenInOneLineAndLoggedOneEach() {
        MissingControllerWarning warning = warning(folder -> Map.of("LVWAP", 142, "RVWAP", 1));
        List<MyDevice> both = List.of(LVWAP_UNPLUGGED.getFirst(),
                new MyDevice(null, "RVWAP", "3344", "03F5", false, "RVWAP", false));

        warning.judgeOnce(true, both, BINDINGS);

        assertEquals(1, spoken.size());
        assertTrue(spoken.getFirst().contains("LVWAP, RVWAP"));
        assertEquals(List.of("Controller LVWAP is not connected - 142 bindings name it",
                "Controller RVWAP is not connected - 1 binding names it"), logged);
    }

    /** "Exactly one warning": the second opening of the screen says nothing, whatever is still missing. */
    @Test
    void itIsSaidOncePerRun() {
        MissingControllerWarning warning = warning(folder -> Map.of("LVWAP", 142));

        warning.judgeOnce(true, LVWAP_UNPLUGGED, BINDINGS);
        warning.judgeOnce(true, LVWAP_UNPLUGGED, BINDINGS);

        assertEquals(1, spoken.size());
    }

    /** Nothing missing still uses the warning up - a stick unplugged later is the MISSING column's to show. */
    @Test
    void nothingMissingSaysNothingAndUsesItUp() {
        MissingControllerWarning warning = warning(folder -> Map.of("LVWAP", 142));

        warning.judgeOnce(true, List.of(), BINDINGS);
        warning.judgeOnce(true, LVWAP_UNPLUGGED, BINDINGS);

        assertTrue(spoken.isEmpty());
        assertTrue(logged.isEmpty());
        assertFalse(warning.pending());
    }

    /**
     * Until the Device Service has listed the hardware - stopped, or running but not through its first look yet -
     * every controller reads absent. So nothing is said, and it waits.
     */
    @Test
    void hardwareNotYetListedSaysNothingAndKeepsItForLater() {
        MissingControllerWarning warning = warning(folder -> Map.of("LVWAP", 142));

        warning.judgeOnce(false, LVWAP_UNPLUGGED, BINDINGS);
        assertTrue(spoken.isEmpty());
        assertTrue(warning.pending());

        warning.judgeOnce(true, LVWAP_UNPLUGGED, BINDINGS);
        assertEquals(1, spoken.size());
    }

    /** Without the counts every unplugged entry would be announced as if it mattered. */
    @Test
    void unreadableBindingsSayNothingAndUseItUp() {
        MissingControllerWarning warning = warning(folder -> {
            throw new NoSuchFileException(folder.toString());
        });

        warning.judgeOnce(true, LVWAP_UNPLUGGED, BINDINGS);

        assertTrue(spoken.isEmpty());
        assertTrue(logged.isEmpty());
        assertFalse(warning.pending());
    }

    private MissingControllerWarning warning(MissingControllerWarning.ReferenceReader reader) {
        return new MissingControllerWarning(reader, List.of("T-Rudder"), spoken::add, logged::add);
    }
}
