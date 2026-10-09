package elite.intel.ui.overlay;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A card called up by voice sits over the ranking until it is closed or left alone too long.
 */
class TemporaryHudCardTest {

    private final AtomicLong now = new AtomicLong(1_000);
    private final TemporaryHudCard slot = new TemporaryHudCard(now::get);
    private final HudObjective list = new HudObjective("list", "LIST", null, List.of(), HudObjective.PRIORITY_AMBIENT);
    private final HudObjectiveSource source = () -> Optional.of(list);

    @Test
    void anOpenCardIsShown() {
        slot.open(source);

        assertEquals(Optional.of(list), slot.current());
    }

    @Test
    void itGoesAwayByItselfAfterTwoIdleMinutes() {
        slot.open(source);
        now.addAndGet(TemporaryHudCard.IDLE_LIMIT.toMillis() - 1);
        assertTrue(slot.current().isPresent());

        now.addAndGet(1);
        assertTrue(slot.current().isEmpty());
        assertFalse(slot.isShowing(source));
    }

    @Test
    void usingItRestartsTheClock() {
        slot.open(source);
        now.addAndGet(TemporaryHudCard.IDLE_LIMIT.toMillis() - 1);
        slot.touch();
        now.addAndGet(TemporaryHudCard.IDLE_LIMIT.toMillis() - 1);

        assertTrue(slot.isShowing(source));
    }

    @Test
    void anotherCardsCloseLeavesThisOneUp() {
        slot.open(source);
        slot.close(Optional::empty);

        assertTrue(slot.isShowing(source));
        slot.close(source);
        assertTrue(slot.current().isEmpty());
    }

    @Test
    void anEmptySourceLetsTheRankedCardThrough() {
        slot.open(Optional::empty);

        assertTrue(slot.current().isEmpty());
    }
}
