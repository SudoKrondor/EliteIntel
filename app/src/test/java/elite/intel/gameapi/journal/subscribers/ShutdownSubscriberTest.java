package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.ai.mouth.TransmissionAudio;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.ShutdownEvent;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Once the game has closed, the last in-game reading must stop deciding anything - above all, VEGA must not keep
 * sounding like a helmet radio because the commander was on foot when they quit.
 */
class ShutdownSubscriberTest {

    private final Status status = Status.getInstance();
    private final SystemSession settings = SystemSession.getInstance();
    private final GameEvents.StatusEvent originalStatus = status.getStatus();

    @AfterEach
    void restore() {
        status.setStatus(originalStatus);
        settings.setTransmissionTones(false);
        settings.setEffectsOnVegaAway(false);
    }

    @Test
    void closingTheGameClearsTheLastInGameState() {
        GameEvents.StatusEvent onFoot = new GameEvents.StatusEvent();
        onFoot.setFlags2(1L);
        onFoot.setBalance(1_000_000L);
        status.setStatus(onFoot);
        assertTrue(status.isOnFoot());

        new ShutdownSubscriber().onShutdown(shutdown());

        assertFalse(status.isOnFoot());
        assertEquals(0, status.getStatus().getFlags());
        assertEquals(1_000_000L, status.getStatus().getBalance(), "only the in-game state is cleared");
    }

    @Test
    void vegaLosesTheAwayEffectOnceTheGameHasClosed() {
        settings.setTransmissionTones(true);
        settings.setEffectsOnVegaAway(true);
        GameEvents.StatusEvent inSrv = new GameEvents.StatusEvent();
        inSrv.setFlags(1L << 26);
        status.setStatus(inSrv);
        VocalisationRequestEvent vega = VocalisationRequestEvent.trackedVega(
                "vega", "vega", AiVoxResponseEvent.class, true, new CompletableFuture<>());
        assertTrue(TransmissionAudio.forRequest(vega).tones());

        new ShutdownSubscriber().onShutdown(shutdown());

        assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(vega));
    }

    private static ShutdownEvent shutdown() {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2026-09-30T19:00:00Z");
        json.addProperty("event", "Shutdown");
        return new ShutdownEvent(json);
    }
}
