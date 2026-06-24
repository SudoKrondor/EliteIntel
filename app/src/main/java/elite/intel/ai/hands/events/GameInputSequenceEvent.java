package elite.intel.ai.hands.events;

import java.util.Arrays;
import java.util.List;

/**
 * The public game input event API.
 * Every game input, including a single tap or key press, is represented as a sequence of {@link GameInputStep}s.
 */
public final class GameInputSequenceEvent {

    private final List<GameInputStep> steps;

    public GameInputSequenceEvent(List<GameInputStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        if (steps.stream().anyMatch(step -> step == null)) {
            throw new IllegalArgumentException("steps must not contain null");
        }
        this.steps = List.copyOf(steps);
    }

    public static GameInputSequenceEvent of(GameInputStep... steps) {
        return new GameInputSequenceEvent(Arrays.asList(steps));
    }

    public static GameInputSequenceEvent single(GameInputStep step) {
        return of(step);
    }

    public List<GameInputStep> getSteps() {
        return steps;
    }
}
