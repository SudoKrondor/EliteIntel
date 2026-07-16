package elite.intel.ai.brain.actions.command.builtin;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.hands.events.GameInputSequenceEvent;
import elite.intel.eventbus.GameControllerBus;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;
import elite.intel.util.StringUtls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression guard for hiding and blocking landing-gear deployment in supercruise. */
class DeployLandingGearCommandTest {

    private static final long SUPERCRUISE = 16L;
    private static final long IN_MAIN_SHIP = 16_777_216L;

    private final long savedFlags = Status.getInstance().getStatus().getFlags();
    private final long savedFlags2 = Status.getInstance().getStatus().getFlags2();
    private final DeployLandingGearCommand command = new DeployLandingGearCommand();
    private final InputCapture inputCapture = new InputCapture();

    @BeforeEach
    void registerInputCapture() {
        GameControllerBus.register(inputCapture);
    }

    @AfterEach
    void cleanUp() {
        GameControllerBus.unregister(inputCapture);
        setStatus(savedFlags, savedFlags2);
    }

    @Test
    void isHiddenInSupercruise() {
        setStatus(IN_MAIN_SHIP, 0L);
        assertTrue(command.isVisibleForLLM(Status.getInstance()));

        setStatus(IN_MAIN_SHIP | SUPERCRUISE, 0L);
        assertFalse(command.isVisibleForLLM(Status.getInstance()));
    }

    @Test
    void executionInSupercruiseDoesNotPublishLandingGearInput() {
        setStatus(IN_MAIN_SHIP | SUPERCRUISE, 0L);

        String outcome = command.execute(new JsonObject(), null);

        assertEquals(StringUtls.localizedLlm("handler.landingGear.cantDoThat"), outcome);
        assertTrue(inputCapture.events.isEmpty());
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
