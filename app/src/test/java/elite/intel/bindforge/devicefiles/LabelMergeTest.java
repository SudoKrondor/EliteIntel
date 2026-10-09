package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.LabelMerge.Collision;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Labels merge at the label: only the inputs that disagree are rows, a label one side holds is taken, and one side
 * can answer the whole file (alias-designer.md, <em>{@code .buttonMap} files merge at the label</em>).
 */
class LabelMergeTest {

    /** The spec's merge table. */
    @Test
    void onlyTheInputsThatDisagreeAreRows() {
        LabelMerge merge = merge(
                "steam", Map.of("Joy_1", "LV MAIN TRIGGER", "Joy_XAxis", "LVWAP_XAXIS"),
                "epic", Map.of("Joy_1", "TRIGGER 1", "Joy_4", "PINKY PADDLE", "Joy_XAxis", "LVWAP_XAXIS"));

        assertEquals(List.of(new Collision("Joy_1", Map.of("steam", "LV MAIN TRIGGER", "epic", "TRIGGER 1"),
                List.of("LV MAIN TRIGGER", "TRIGGER 1"))), merge.collisions());
        assertEquals(Map.of("Joy_1", "TRIGGER 1", "Joy_4", "PINKY PADDLE", "Joy_XAxis", "LVWAP_XAXIS"),
                merge.merged(Map.of("Joy_1", "TRIGGER 1")), "Joy_4 taken, the axis identical");
    }

    @Test
    void twentyLabelsAndFiveGiveTwentyFive() {
        LabelMerge merge = merge("steam", Map.of("Joy_1", "A", "Joy_2", "B"), "epic", Map.of("Joy_3", "C"));

        assertTrue(merge.collisions().isEmpty());
        assertEquals(Map.of("Joy_1", "A", "Joy_2", "B", "Joy_3", "C"), merge.merged(Map.of()));
    }

    @Test
    void twoSidesHoldingOneLabelGiveOneChoiceAndASideWithoutOneIsNotAChoice() {
        Map<String, Map<String, String>> sides = new LinkedHashMap<>();
        sides.put("steam", Map.of("Joy_1", "TRIGGER"));
        sides.put("epic", Map.of("Joy_1", "FIRE"));
        sides.put("frontier", Map.of("Joy_1", "TRIGGER"));
        sides.put("oculus", Map.of("Joy_2", "PINKY"));
        Collision collision = new LabelMerge(sides).collisions().getFirst();

        assertEquals(List.of("TRIGGER", "FIRE"), collision.choices());
        assertEquals(List.of("steam", "epic", "frontier"), List.copyOf(collision.bySide().keySet()));
    }

    @Test
    void anAnswerMustBeALabelASideHolds() {
        LabelMerge merge = merge("steam", Map.of("Joy_1", "A"), "epic", Map.of("Joy_1", "B"));

        assertEquals(List.of("Joy_1"), merge.unanswered(Map.of()));
        assertEquals(List.of("Joy_1"), merge.unanswered(Map.of("Joy_1", "C")));
        assertEquals(List.of(), merge.unanswered(Map.of("Joy_1", "B")));
        assertThrows(IllegalStateException.class, () -> merge.merged(Map.of()));
    }

    @Test
    void oneSideAnswersEveryInputItHoldsALabelFor() {
        Map<String, Map<String, String>> sides = new LinkedHashMap<>();
        sides.put("steam", Map.of("Joy_1", "A"));
        sides.put("epic", Map.of("Joy_1", "B", "Joy_2", "B2"));
        sides.put("frontier", Map.of("Joy_2", "C2"));
        LabelMerge merge = new LabelMerge(sides);

        Map<String, String> answers = merge.answersFavouring("steam");

        assertEquals(Map.of("Joy_1", "A"), answers);
        assertEquals(List.of("Joy_2"), merge.unanswered(answers), "steam has no side on Joy_2");
        assertEquals(Map.of("Joy_1", "B", "Joy_2", "B2"), merge.answersFavouring("epic"));
    }

    @Test
    void aSideWithNoLabelsIsNotASide() {
        LabelMerge merge = merge("steam", Map.of(), "epic", Map.of("Joy_1", "A"));

        assertEquals(List.of("epic"), merge.sides());
    }

    private static LabelMerge merge(String first, Map<String, String> firstLabels,
                                    String second, Map<String, String> secondLabels) {
        Map<String, Map<String, String>> sides = new LinkedHashMap<>();
        sides.put(first, firstLabels);
        sides.put(second, secondLabels);
        return new LabelMerge(sides);
    }
}
