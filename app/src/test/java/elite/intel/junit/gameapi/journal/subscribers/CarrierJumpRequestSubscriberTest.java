package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.CompanionNarrator;
import elite.intel.ai.brain.vega.CompanionRuntimeGraph;
import elite.intel.ai.brain.vega.CompanionRuntimeTestSupport;
import elite.intel.gameapi.journal.events.CarrierJumpRequestEvent;
import elite.intel.gameapi.journal.subscribers.CarrierJumpRequestSubscriber;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the carrier departure announcement against naming a destination the event never carried.
 *
 * <p>Vega announced "we're heading to Sol in 15" for a jump scheduled to Blua Eaec ZI-M c22-138, some
 * 7000 ly from Sol and nowhere on the route. The journal event carried {@code SystemName} but no
 * {@code Body}, and the subscriber built its line from {@code Body} alone, so the payload reached the LLM
 * with no destination in it while the instruction still said "State the destination". Ordered to state a
 * fact it had not been given, the model supplied the most probable Elite Dangerous system name.
 *
 * <p>The Body-less shape is rare - one of 48 requests across the commander's journals - which is why it read
 * as random banter. It comes from scheduling a jump off a typed system name, which is exactly what following
 * a stored fleet-carrier route does, so it recurs for that workflow.
 *
 * <p>Both journal lines below are real, taken verbatim from the commander's journals.
 */
class CarrierJumpRequestSubscriberTest {

    /**
     * The real event that triggered the hallucination: SystemName present, Body absent, BodyID set.
     */
    private static final String WITHOUT_BODY = """
            { "timestamp":"2026-07-20T05:47:33Z", "event":"CarrierJumpRequest", "CarrierType":"FleetCarrier",
              "CarrierID":3712500736, "SystemName":"Blua Eaec ZI-M c22-138", "SystemAddress":38004655070914,
              "BodyID":23, "DepartureTime":"%s" }
            """;

    /**
     * The common shape, where Body repeats the system with an arrival-point suffix.
     */
    private static final String WITH_BODY = """
            { "timestamp":"2026-07-20T01:40:05Z", "event":"CarrierJumpRequest", "CarrierType":"FleetCarrier",
              "CarrierID":3712500736, "SystemName":"Stuelou JZ-E b42-5", "Body":"Stuelou JZ-E b42-5 A",
              "SystemAddress":11575874370409, "BodyID":1, "DepartureTime":"%s" }
            """;

    private final CarrierJumpRequestSubscriber subscriber = new CarrierJumpRequestSubscriber();
    private final CapturingNarrator narrator = new CapturingNarrator();
    private CompanionRuntimeGraph runtimeGraph;

    @BeforeEach
    void installNarrator() {
        runtimeGraph = CompanionRuntimeTestSupport.installNarrator(narrator);
    }

    @AfterEach
    void clearNarrator() {
        CompanionRuntimeTestSupport.uninstall(runtimeGraph);
    }

    @Test
    void aBodylessRequestStillNamesTheDestinationSystem() throws Exception {
        narrate(WITHOUT_BODY);
        assertTrue(narrator.data.contains("Blua Eaec ZI-M c22-138"),
                () -> "the destination system must reach the LLM, but the payload was: " + narrator.data);
    }

    /**
     * The instruction and the payload have to agree. Telling the model to state a destination that is not in
     * front of it is what produced "Sol".
     */
    @Test
    void anInstructionNeverAsksForADestinationThePayloadLacks() throws Exception {
        narrate(WITHOUT_BODY);
        boolean named = narrator.data.contains("Blua Eaec ZI-M c22-138");
        assertTrue(named || !narrator.instructions.toLowerCase().contains("state the destination"),
                () -> "payload carries no destination yet the instruction demands one: " + narrator.instructions);
    }

    /**
     * The system, not the longer arrival body, is what the commander plots and thinks in.
     */
    @Test
    void theDestinationSystemIsAnnouncedRatherThanTheArrivalBody() throws Exception {
        narrate(WITH_BODY);
        assertTrue(narrator.data.contains("Stuelou JZ-E b42-5"), narrator.data);
        assertFalse(narrator.data.contains("Stuelou JZ-E b42-5 A"),
                () -> "the arrival-point suffix only lengthens an already procedural name: " + narrator.data);
    }

    @Test
    void theCountdownIsReported() throws Exception {
        narrate(WITH_BODY);
        assertTrue(narrator.data.matches(".*\\b1[45]\\b.*"),
                () -> "expected the ~15 minute countdown in: " + narrator.data);
    }

    /**
     * Runs the subscriber against a departure 15 minutes out and waits for its virtual thread.
     */
    private void narrate(String template) throws Exception {
        String departure = Instant.now().plus(Duration.ofMinutes(15)).plusSeconds(5)
                .truncatedTo(ChronoUnit.SECONDS).toString();
        JsonObject json = GsonFactory.getGson().fromJson(String.format(template, departure), JsonObject.class);
        subscriber.onCarrierJumpRequestEvent(new CarrierJumpRequestEvent(json));
        assertTrue(narrator.awaitNarration(5, TimeUnit.SECONDS), "subscriber never narrated");
    }

    /**
     * Captures what the subscriber hands the companion, across the subscriber's virtual thread.
     */
    private static final class CapturingNarrator implements CompanionNarrator {
        private final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        volatile String data;
        volatile String instructions;

        @Override
        public void filler(String text, boolean urgent) {
        }

        @Override
        public void narrate(String data, String instructions) {
            this.data = data;
            this.instructions = instructions;
            latch.countDown();
        }

        @Override
        public void announce(String phrase, boolean urgent) {
        }

        boolean awaitNarration(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }
}
