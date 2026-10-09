package elite.intel.bindforge.devicefiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One device's {@code .buttonMap} labels from several sides, merged at the label. Pure.
 * <p>
 * Settled 2026-09-23 - alias-designer.md, <em>{@code .buttonMap} files merge at the label</em>:
 * <ul>
 *     <li>Two label sets combine: twenty labels on one side and five on another give twenty-five.</li>
 *     <li><strong>Only the inputs that disagree are rows</strong>, one each. Identical labels are not listed, and a
 *     label only one side holds is taken without a question.</li>
 *     <li>Overwrite stays available for the whole file - {@link #answersFavouring(String)}.</li>
 * </ul>
 * A side is named by the caller: an installation's id at first setup. The same view serves a later re-merge, where
 * the master is one of the sides.
 */
public final class LabelMerge {

    /**
     * One input the sides label differently.
     *
     * @param input   the {@code .buttonMap} token, {@code Joy_1}
     * @param bySide  each side's label, for the sides holding one, in side order
     * @param choices the distinct labels, in side order - two sides holding the same label give one choice
     */
    public record Collision(String input, Map<String, String> bySide, List<String> choices) {
        public Collision {
            Objects.requireNonNull(input, "input");
            bySide = Collections.unmodifiableMap(new LinkedHashMap<>(bySide));
            choices = List.copyOf(choices);
        }
    }

    private final Map<String, Map<String, String>> sides;
    private final List<Collision> collisions;

    /**
     * @param sides each side's labels, input to label, in the order they are to be shown. A side with no labels
     *              may be passed and is ignored
     */
    public LabelMerge(Map<String, Map<String, String>> sides) {
        Map<String, Map<String, String>> copied = new LinkedHashMap<>();
        sides.forEach((side, labels) -> {
            if (!labels.isEmpty()) copied.put(side, Map.copyOf(labels));
        });
        this.sides = Collections.unmodifiableMap(copied);
        this.collisions = findCollisions(this.sides);
    }

    /** The sides holding any label, in order. */
    public List<String> sides() {
        return List.copyOf(sides.keySet());
    }

    /** The inputs that disagree, by input. Empty when there is nothing to ask. */
    public List<Collision> collisions() {
        return collisions;
    }

    /**
     * The whole-file route - <em>use Steam's labels for this device</em>: that side's label for every input it
     * holds one for. An input where the side has none is left unanswered.
     *
     * @return input to label
     */
    public Map<String, String> answersFavouring(String side) {
        Map<String, String> labels = sides.getOrDefault(side, Map.of());
        Map<String, String> answers = new LinkedHashMap<>();
        for (Collision collision : collisions) {
            String label = labels.get(collision.input());
            if (label != null) answers.put(collision.input(), label);
        }
        return answers;
    }

    /**
     * The inputs still needing an answer: unanswered, or answered with a label no side holds for that input.
     *
     * @param answers input to the chosen label
     */
    public List<String> unanswered(Map<String, String> answers) {
        List<String> missing = new ArrayList<>();
        for (Collision collision : collisions) {
            // WHY: the null check first - an immutable list's contains(null) throws rather than answering false.
            String answer = answers.get(collision.input());
            if (answer == null || !collision.choices().contains(answer)) missing.add(collision.input());
        }
        return missing;
    }

    /**
     * Every side's labels combined, each disagreeing input taking its answer.
     *
     * @param answers input to the chosen label
     * @throws IllegalStateException if an input is still unanswered - see {@link #unanswered(Map)}
     */
    public Map<String, String> merged(Map<String, String> answers) {
        List<String> missing = unanswered(answers);
        if (!missing.isEmpty()) throw new IllegalStateException("labels are not answered: " + missing);
        Map<String, String> merged = new TreeMap<>();
        sides.values().forEach(labels -> labels.forEach(merged::putIfAbsent));
        for (Collision collision : collisions) merged.put(collision.input(), answers.get(collision.input()));
        return merged;
    }

    private static List<Collision> findCollisions(Map<String, Map<String, String>> sides) {
        Map<String, Map<String, String>> byInput = new TreeMap<>();
        sides.forEach((side, labels) -> labels.forEach((input, label) ->
                byInput.computeIfAbsent(input, i -> new LinkedHashMap<>()).put(side, label)));
        List<Collision> found = new ArrayList<>();
        byInput.forEach((input, bySide) -> {
            List<String> choices = List.copyOf(new LinkedHashSet<>(bySide.values()));
            if (choices.size() > 1) found.add(new Collision(input, bySide, choices));
        });
        return List.copyOf(found);
    }
}
