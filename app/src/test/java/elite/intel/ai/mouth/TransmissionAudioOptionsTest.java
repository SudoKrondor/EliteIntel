package elite.intel.ai.mouth;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which speech gets the optional treatment ({@link TransmissionAudio#forRequest}): radio traffic when that switch
 * is on, VEGA only on foot or in an SRV, and never anything else.
 */
class TransmissionAudioOptionsTest {

    private static final TransmissionAudio.Options ALL = new TransmissionAudio.Options(true, true);

    private final SystemSession settings = SystemSession.getInstance();
    private final Status status = Status.getInstance();
    private GameEvents.StatusEvent originalStatus;

    private final VocalisationRequestEvent radio = new VocalisationRequestEvent(
            "radio", null, AiVoxResponseEvent.class, true, true, null);
    private final VocalisationRequestEvent ordinary = new VocalisationRequestEvent(
            "ordinary", AiVoxResponseEvent.class, true);
    private final VocalisationRequestEvent vega = VocalisationRequestEvent.trackedVega(
            "vega", "vega", AiVoxResponseEvent.class, true, new CompletableFuture<>());

    @BeforeEach
    void everyEffectOn() {
        originalStatus = status.getStatus();
        settings.setTransmissionTones(true);
        settings.setEnhancedRadioEffect(true);
        settings.setEffectsOnRadio(true);
        settings.setEffectsOnVegaAway(true);
    }

    @AfterEach
    void restore() {
        status.setStatus(originalStatus);
        settings.setTransmissionTones(false);
        settings.setEnhancedRadioEffect(false);
        settings.setEffectsOnRadio(false);
        settings.setEffectsOnVegaAway(false);
    }

    @Test
    void aboardShipOnlyRadioTrafficIsTreated() {
        status.setStatus(new GameEvents.StatusEvent());

        assertEquals(ALL, TransmissionAudio.forRequest(radio));
        assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(ordinary));
        assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(vega));
    }

    @Test
    void vegaIsTreatedOnFootAndInAnSrv() {
        GameEvents.StatusEvent onFoot = new GameEvents.StatusEvent();
        onFoot.setFlags2(1L);
        status.setStatus(onFoot);
        assertEquals(ALL, TransmissionAudio.forRequest(vega));
        assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(ordinary));

        GameEvents.StatusEvent inSrv = new GameEvents.StatusEvent();
        inSrv.setFlags(1L << 26);
        status.setStatus(inSrv);
        assertEquals(ALL, TransmissionAudio.forRequest(vega));
    }

    @Test
    void theRadioSwitchOffLeavesRadioTrafficUntreated() {
        settings.setEffectsOnRadio(false);

        assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(radio));
    }
}
