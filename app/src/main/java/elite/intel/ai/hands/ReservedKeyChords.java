package elite.intel.ai.hands;

import elite.intel.util.OsDetector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * OS-reserved keyboard chords that must never be assigned to a game control.
 * <p>
 * Some key combinations are intercepted by the operating system / desktop environment before the
 * game ever sees them, and triggering them mid-flight is destructive:
 * <ul>
 *   <li><b>Alt+F4</b> closes the focused window on every desktop OS - it would quit the game.</li>
 *   <li><b>Ctrl+Alt+F1..F12</b> on Linux switch virtual terminals and can drop the user to a TTY,
 *       yanking them out of the running game session.</li>
 * </ul>
 * A chord is matched on its full key-set (main key + modifiers), so the order of the keys and any
 * extra modifiers held alongside the dangerous combination do not let it slip through.
 * <p>
 * Used by the auto-assigner (to keep these out of the chord pool), the manual assign dialog (to
 * block the save and flag the chord), and the live keyboard map (to colour the keys as reserved).
 */
public final class ReservedKeyChords {

    private static final boolean IS_LINUX = OsDetector.getOs() == OsDetector.OS.LINUX;

    private static final Set<String> ALT_KEYS = Set.of("Key_LeftAlt", "Key_RightAlt");
    private static final Set<String> CTRL_KEYS = Set.of("Key_LeftControl", "Key_RightControl");
    private static final Set<String> FUNCTION_KEYS = Set.of(
            "Key_F1", "Key_F2", "Key_F3", "Key_F4", "Key_F5", "Key_F6",
            "Key_F7", "Key_F8", "Key_F9", "Key_F10", "Key_F11", "Key_F12");

    private ReservedKeyChords() {
    }

    /**
     * True when the chord formed by {@code mainKey} plus {@code modifierKeys} is reserved by the OS
     * and must not be assigned. {@code modifierKeys} are Elite key tokens (e.g. {@code Key_LeftAlt}).
     */
    public static boolean isReserved(String mainKey, Collection<String> modifierKeys) {
        Set<String> keys = new HashSet<>();
        if (mainKey != null && !mainKey.isBlank() && !"Key_".equals(mainKey)) {
            keys.add(mainKey);
        }
        if (modifierKeys != null) {
            for (String modifier : modifierKeys) {
                if (modifier != null && !modifier.isBlank()) {
                    keys.add(modifier);
                }
            }
        }
        return isReservedKeyset(keys);
    }

    /**
     * Key-set based core, so it can be tested without constructing chords.
     */
    static boolean isReservedKeyset(Set<String> keys) {
        return isReservedKeyset(keys, IS_LINUX);
    }

    /**
     * OS passed explicitly so the platform-specific rules are deterministically testable.
     */
    static boolean isReservedKeyset(Set<String> keys, boolean linux) {
        boolean hasAlt = keys.stream().anyMatch(ALT_KEYS::contains);
        boolean hasCtrl = keys.stream().anyMatch(CTRL_KEYS::contains);
        boolean hasFunctionKey = keys.stream().anyMatch(FUNCTION_KEYS::contains);

        // Alt+F4 closes the window on every desktop OS.
        if (hasAlt && keys.contains("Key_F4")) {
            return true;
        }
        // Ctrl+Alt+F1..F12 switch virtual terminals on Linux.
        return linux && hasCtrl && hasAlt && hasFunctionKey;
    }
}
