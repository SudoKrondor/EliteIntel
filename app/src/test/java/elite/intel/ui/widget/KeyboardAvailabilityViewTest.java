package elite.intel.ui.widget;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards {@link KeyboardAvailabilityView#distribute}'s core promise: the per-key pixel widths sum
 * exactly to the available width (so the last key is always flush to the row's right edge) and are
 * never negative, for the actual keyboard row weightings and across a range of widths.
 */
class KeyboardAvailabilityViewTest {

    // The real row weightings from KeyboardAvailabilityView.buildRows (each sums to 15 units).
    private static final double[] F_ROW =
            {1, 1, 1, 1, 1, 1, 0.5, 1, 1, 1, 1, 0.5, 1, 1, 1, 1};
    private static final double[] NUMBER_ROW =
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2};
    private static final double[] HOME_ROW =
            {1.75, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.25};
    private static final double[] SHIFT_ROW =
            {2.25, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2.75};
    private static final double[] BOTTOM_ROW = {1.5, 1.5, 9, 1.5, 1.5};

    @Test
    void widthsSumExactlyToAvailable() {
        double[][] rows = {F_ROW, NUMBER_ROW, HOME_ROW, SHIFT_ROW, BOTTOM_ROW};
        // A spread of widths, including the awkward odd/small ones where rounding would otherwise drift.
        for (int available : new int[]{1, 47, 100, 555, 556, 557, 560, 999, 1024}) {
            for (double[] row : rows) {
                int[] widths = KeyboardAvailabilityView.distribute(row, available);
                assertEquals(available, Arrays.stream(widths).sum(),
                        "row " + Arrays.toString(row) + " @ available=" + available);
            }
        }
    }

    @Test
    void widthsAreNeverNegative() {
        for (int available : new int[]{0, 1, 5, 560}) {
            int[] widths = KeyboardAvailabilityView.distribute(F_ROW, available);
            for (int w : widths) {
                assertTrue(w >= 0, "negative width for available=" + available + ": " + Arrays.toString(widths));
            }
        }
    }

    @Test
    void equalWeightsDifferByAtMostOnePixel() {
        int[] widths = KeyboardAvailabilityView.distribute(new double[]{1, 1, 1, 1, 1, 1, 1}, 100);
        int min = Arrays.stream(widths).min().orElseThrow();
        int max = Arrays.stream(widths).max().orElseThrow();
        assertTrue(max - min <= 1, "uneven split: " + Arrays.toString(widths));
        assertEquals(100, Arrays.stream(widths).sum());
    }

    @Test
    void singleCellTakesAllWidth() {
        assertEquals(560, KeyboardAvailabilityView.distribute(new double[]{2.25}, 560)[0]);
    }
}
