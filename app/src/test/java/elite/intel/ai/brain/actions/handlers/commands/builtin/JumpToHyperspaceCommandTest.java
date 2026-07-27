package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.data.FsdTarget;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression guard: a plotted jump must reach the game, even when the commander's current system is not
 * a known location row (an unvisited/unscanned system leaves the looked-up location without a star name).
 */
class JumpToHyperspaceCommandTest {

    private static final long IN_MAIN_SHIP = 16_777_216L;

    private final long savedFlags = Status.getInstance().getStatus().getFlags();
    private final long savedFlags2 = Status.getInstance().getStatus().getFlags2();
    private final JumpToHyperspaceCommand command = new JumpToHyperspaceCommand();
    private final InputCapture inputCapture = new InputCapture();
    private FsdTarget savedFsdTarget;

    @BeforeEach
    void registerInputCapture() {
        savedFsdTarget = PlayerSession.getInstance().getFsdTarget();
        GameControllerBus.register(inputCapture);
    }

    @AfterEach
    void cleanUp() {
        GameControllerBus.unregister(inputCapture);
        if (savedFsdTarget != null) {
            PlayerSession.getInstance().setFsdTarget(savedFsdTarget);
        }
        setStatus(savedFlags, savedFlags2);
    }

    @Test
    void jumpsWhenCurrentLocationIsUnknown() {
        setStatus(IN_MAIN_SHIP, 0L);
        PlayerSession.getInstance().setFsdTarget(
                new FsdTarget("Los", "K", null, null, null, null, "Fuel star"));

        String outcome = command.execute(new JsonObject(), null);

        assertNull(outcome, "an executed jump reports no blocking outcome");
        assertFalse(inputCapture.events.isEmpty(), "the jump must reach the game as input");
    }

    private static void setStatus(long flags, long flags2) {
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        snapshot.setFlags(flags);
        snapshot.setFlags2(flags2);
        Status.getInstance().setStatus(snapshot);
    }

    private static final class InputCapture {
        private final List<GameInputSequenceEvent> events = new ArrayList<>();

        @Subscribe
        public void on(GameInputSequenceEvent event) {
            events.add(event);
        }
    }
}
