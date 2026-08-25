package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

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
}
