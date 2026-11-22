package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingInput;

import java.util.Objects;

/**
 * A deliberate edit of one axis element - its single {@code <Binding>}, {@code <Inverted>} and {@code <Deadzone>} -
 * made by the user. Clears the binding when {@link #axis()} is {@code null}; Inverted and Deadzone are written
 * either way. An axis takes no modifiers and no Hold.
 * <p>
 * For {@link BindingsWriter#assignAxis(byte[], AxisAssignment)} only, which nothing automatic may call.
 *
 * @param deadzone 0 to 1, written the way the game writes it: the float to eight decimals
 */
public record AxisAssignment(
        String bindingId,
        BindingInput axis,
        boolean inverted,
        float deadzone
) {
    public AxisAssignment {
        Objects.requireNonNull(bindingId, "bindingId");
        if (!(deadzone >= 0f && deadzone <= 1f)) {
            throw new IllegalArgumentException("Deadzone must be between 0 and 1, not " + deadzone);
        }
    }

    public boolean clearsSlot() {
        return axis == null;
    }
}
