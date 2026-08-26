package elite.intel.ai.hands;

import elite.intel.session.SystemSession;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the polling behind a WAIT_UNTIL step: it must return as soon as the game reports the expected
 * state (so a fast machine pays nothing), and it must give up at the deadline rather than stalling the
 * single input worker for a state that will never arrive.
 */
class InputSequenceExecutorTest {

    @Test
    void aConditionThatAlreadyHoldsCostsNoWait() {
        long start = System.currentTimeMillis();

        assertTrue(InputSequenceExecutor.awaitCondition(() -> true, 5000, 50));

        assertTrue(System.currentTimeMillis() - start < 1000, "an already-true condition must not sleep");
    }

    @Test
    void aConditionThatBecomesTrueReturnsWithoutSpendingTheWholeTimeout() {
        AtomicInteger polls = new AtomicInteger();

        assertTrue(InputSequenceExecutor.awaitCondition(() -> polls.incrementAndGet() >= 3, 5000, 10));

        assertEquals(3, polls.get());
    }

    @Test
    void aConditionThatNeverHoldsGivesUpAtTheDeadline() {
        long start = System.currentTimeMillis();

        assertFalse(InputSequenceExecutor.awaitCondition(() -> false, 200, 10));

        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 200, "gave up early after " + elapsed + "ms");
        assertTrue(elapsed < 2000, "overshot the deadline by too much: " + elapsed + "ms");
    }

    @Test
    void anInterruptedWaitStopsAndKeepsTheInterruptFlag() throws Exception {
        Thread waiter = new Thread(() -> {
            assertFalse(InputSequenceExecutor.awaitCondition(() -> false, 60000, 10));
            assertTrue(Thread.currentThread().isInterrupted(), "interrupt flag must survive for the executor's own check");
        });

        waiter.start();
        Thread.sleep(50);
        waiter.interrupt();
        waiter.join(5000);

        assertFalse(waiter.isAlive(), "an interrupted wait must not hold the input worker");
    }

    @Test
    void theSlowestPacingStillLeavesEveryPauseInsideItsWindow() {
        Random random = new Random(4242);
        int floor = SystemSession.KEY_INPUT_DELAY_MAX_MS;

        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int i = 0; i < 10_000; i++) {
            int delay = InputSequenceExecutor.postInputDelayMs(random, floor);
            lowest = Math.min(lowest, delay);
            highest = Math.max(highest, delay);
        }

        // The floor is what the commander picked - the spread only ever adds to it, so dragging the
        // slider towards SLOW can never hand the game a pause faster than the one it was set to.
        assertEquals(floor, lowest, "no pause may fall below the configured floor");
        assertEquals(floor + 101, highest, "the spread above the floor is unchanged from the shipped 100-300ms window");
    }

    @Test
    void theDefaultPacingKeepsTheShippedWindow() {
        Random random = new Random(7);

        for (int i = 0; i < 10_000; i++) {
            int delay = InputSequenceExecutor.postInputDelayMs(random, SystemSession.KEY_INPUT_DELAY_MIN_MS);
            assertTrue(delay >= 100 && delay <= 300, "default pacing drifted from the 100-300ms window: " + delay);
        }
    }
}
