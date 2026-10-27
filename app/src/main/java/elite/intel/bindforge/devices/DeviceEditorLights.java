package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.ButtonMapLabels;
import elite.intel.devices.events.DeviceHatEvent;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which of the open device's inputs are active right now - the Device Editor's live highlighting
 * (alias-designer.md, <em>Fields and rules</em>, settled 2026-10-10).
 * <p>
 * <strong>A token is lit only while its input is active</strong>, with nothing lingering: a button while it is
 * held, a hat direction while it is pushed, an axis while it is more than 5% of its travel from where it rests -
 * so jitter never shows.
 * <p>
 * <strong>Where an axis rests is the first value seen after the dialog opened</strong> (Alan, 2026-10-10). The
 * Device Service reports an axis only when it moves, and it moves in raw steps, so the first report arrives within a
 * hair of where the axis sat - a throttle parked at half rests at half.
 * <p>
 * <strong>Axes are lit by which axis they are</strong>, through {@link DeviceDescription#axisAt}: SDL numbers axes
 * without gaps, so its axis 2 may be Z or RZ. Where the numbering is not matched, no axis is lit.
 * <p>
 * Pure and not thread-safe: the dialog feeds it on the EDT.
 */
public final class DeviceEditorLights {

    /** 5% of an axis's travel, which runs from -1 to 1. */
    public static final float REST_MARGIN = 0.1f;

    private final DeviceDescription description;
    private final boolean gamepad;
    private final Map<Integer, Float> rest = new HashMap<>();
    private final Set<String> lit = new LinkedHashSet<>();

    public DeviceEditorLights(DeviceDescription description, boolean gamepad) {
        this.description = description;
        this.gamepad = gamepad;
    }

    /** The tokens lit now. */
    public Set<String> lit() {
        return Set.copyOf(lit);
    }

    /** @return whether anything lit or went out */
    public boolean button(int sdlIndex, boolean pressed) {
        return set(ButtonMapLabels.buttonToken(sdlIndex + 1), pressed);
    }

    /** @return whether anything lit or went out */
    public boolean axis(int sdlIndex, float value) {
        Optional<GameAxis> axis = description.axisAt(sdlIndex);
        if (axis.isEmpty()) return false;
        float resting = rest.computeIfAbsent(sdlIndex, index -> gamepad ? gamepadRest(axis.get()) : value);
        return set(axis.get().token(), Math.abs(value - resting) > REST_MARGIN);
    }

    // WHY: a gamepad is known to rest its sticks at 0 and its triggers at -1, so no first-seen value is needed -
    // and one seen mid-pull would otherwise become the rest.
    private static float gamepadRest(GameAxis axis) {
        return ControllerCapture.gamepadTriggerToken(axis).isPresent() ? -1f : 0f;
    }

    /**
     * @param directions SDL's bits; an 8-way hat's diagonal lights two directions at once
     * @return whether anything lit or went out
     */
    public boolean hat(int sdlIndex, int directions) {
        List<String> tokens = DeviceEditorInputs.hatTokens(sdlIndex + 1);
        int[] bits = {DeviceHatEvent.UP, DeviceHatEvent.RIGHT, DeviceHatEvent.DOWN, DeviceHatEvent.LEFT};
        boolean changed = false;
        for (int i = 0; i < bits.length; i++) {
            changed |= set(tokens.get(i), (directions & bits[i]) != 0);
        }
        return changed;
    }

    /** Everything goes out - the controller was unplugged. Where the axes rest is forgotten too. */
    public void clear() {
        lit.clear();
        rest.clear();
    }

    private boolean set(String token, boolean on) {
        return on ? lit.add(token) : lit.remove(token);
    }
}
