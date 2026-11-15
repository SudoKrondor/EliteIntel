package elite.intel.ai.hands;

import elite.intel.bindforge.io.KeyBindingsParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Taps one lone Control key when the services start, so a desktop that asks before letting the app press keys asks
 * then, while the commander is at the app, and not at the first voice command in game.
 * <p>
 * KWin and Mutter, on an Xwayland built with libei, hold the first XTEST key from each X11 client until the commander
 * allows it, and KWin forgets the answer when the app closes. Without this tap the dialog opened over the game on the
 * first command of every session, and that command was lost. One tap per process is enough: the permission belongs to
 * the display connection, which lives as long as the app. A Control key on its own does nothing in game or on the
 * desktop, unless the commander bound it bare to a control, so a bound one is passed over.
 */
final class InputPermissionPrimer {
    private static final Logger log = LogManager.getLogger(InputPermissionPrimer.class);

    record Candidate(String frontierKey, int keyCode) {
    }

    static final List<Candidate> CANDIDATES = List.of(
            new Candidate("Key_LeftControl", KeyProcessor.KEY_LEFTCONTROL),
            new Candidate("Key_RightControl", KeyProcessor.KEY_RIGHTCONTROL));

    private static final AtomicBoolean primed = new AtomicBoolean();

    private InputPermissionPrimer() {
    }

    /**
     * Taps the first candidate the commander's binds leave free, once per process, where the key path can be gated.
     *
     * @param bindingSlots every keyboard binding of the active binds file, or {@code null} when none was read
     */
    static void prime(KeyProcessor keyProcessor, Map<String, KeyBindingsParser.BindingSlots> bindingSlots) {
        if (!keyProcessor.inputMayAskPermission() || !primed.compareAndSet(false, true)) {
            return;
        }
        Optional<Candidate> key = freeKey(bindingSlots);
        if (key.isEmpty()) {
            log.warn("Both Control keys are bound on their own; not priming the desktop's input permission");
            return;
        }
        log.info("Tapping {} so the desktop asks for input permission now", key.get().frontierKey());
        keyProcessor.pressKey(key.get().keyCode());
    }

    /**
     * The first candidate no slot binds as a bare key. A chord that only uses it as a modifier does not fire on a
     * lone tap, so it does not count.
     */
    static Optional<Candidate> freeKey(Map<String, KeyBindingsParser.BindingSlots> bindingSlots) {
        return CANDIDATES.stream()
                .filter(candidate -> bindingSlots == null || bindingSlots.values().stream()
                        .noneMatch(slots -> bindsBare(slots.primary(), candidate) || bindsBare(slots.secondary(), candidate)))
                .findFirst();
    }

    private static boolean bindsBare(KeyBindingsParser.KeyBinding binding, Candidate candidate) {
        return binding != null
                && candidate.frontierKey().equalsIgnoreCase(binding.key)
                && (binding.modifiers == null || binding.modifiers.length == 0);
    }
}
