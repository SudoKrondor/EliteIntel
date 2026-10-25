package elite.intel.ui.widget;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The stepper tells its listeners only when the value changes - an arrow click or {@code setValue} - and clamps first.
 * Player Backups' guard against saving while it fills the controls depends on {@code setValue} firing (review,
 * 2026-10-24).
 */
class HudStepperTest {

    @Test
    void listenersHearOnlyAChangeAndTheClampedValue() {
        HudStepper stepper = new HudStepper(10, 1000, 10, 100);
        List<Integer> heard = new ArrayList<>();
        stepper.addChangeListener(e -> heard.add(stepper.getValue()));

        stepper.setValue(100);
        stepper.setValue(110);
        stepper.setValue(5000);
        stepper.setValue(1000);
        stepper.setValue(-3);

        assertEquals(List.of(110, 1000, 10), heard, "no event for the same value, and the clamped value is the one heard");
    }
}
