package elite.intel.ai.brain.inference.mistral;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gate that keeps the companion inside Mistral's one-request-per-second free tier. The interval here is
 * deliberately tiny: what is under test is the spacing and the escape hatch, not the shipped 1.1 s value.
 */
class SendRateGateTest {

    private static final long INTERVAL_MILLIS = 120;

    @Test
    void firstSlotIsGrantedWithoutWaiting() {
        SendRateGate gate = new SendRateGate(INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

        long startedNanos = System.nanoTime();
        assertTrue(gate.awaitSlot());

        assertTrue(elapsedMillis(startedNanos) < INTERVAL_MILLIS,
                "an idle gate must not delay the first send");
    }

    @Test
    void consecutiveSlotsAreHeldAnIntervalApart() {
        SendRateGate gate = new SendRateGate(INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

        assertTrue(gate.awaitSlot());
        long secondStartedNanos = System.nanoTime();
        assertTrue(gate.awaitSlot());
        long secondWaitedMillis = elapsedMillis(secondStartedNanos);

        long thirdStartedNanos = System.nanoTime();
        assertTrue(gate.awaitSlot());

        assertTrue(secondWaitedMillis >= INTERVAL_MILLIS - 5,
                "second send waited only " + secondWaitedMillis + " ms");
        assertTrue(elapsedMillis(thirdStartedNanos) >= INTERVAL_MILLIS - 5,
                "the gate must space every send, not just the second one");
    }

    /**
     * A cancelled thought must leave the queue at once, and must not spend the slot it never used.
     */
    @Test
    void interruptedWaitReleasesTheCallerWithoutClaimingTheSlot() throws Exception {
        SendRateGate gate = new SendRateGate(10, TimeUnit.SECONDS);
        assertTrue(gate.awaitSlot()); // the next slot is now ten seconds out

        CountDownLatch waiting = new CountDownLatch(1);
        AtomicBoolean claimed = new AtomicBoolean(true);
        AtomicBoolean interruptFlagKept = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            waiting.countDown();
            claimed.set(gate.awaitSlot());
            interruptFlagKept.set(Thread.currentThread().isInterrupted());
        }, "send-rate-gate-test");
        waiter.setDaemon(true);

        waiter.start();
        assertTrue(waiting.await(2, TimeUnit.SECONDS));
        Thread.sleep(50); // let the waiter reach the timed wait rather than racing the interrupt
        waiter.interrupt();
        waiter.join(2_000);

        assertFalse(waiter.isAlive(), "an interrupt must release the waiter well inside the interval");
        assertFalse(claimed.get(), "an interrupted caller must not believe it holds a send slot");
        assertTrue(interruptFlagKept.get(), "the interrupt must stay visible to the owning thought");
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
}
