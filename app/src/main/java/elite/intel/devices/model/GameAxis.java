package elite.intel.devices.model;

/**
 * The eight joystick axes Elite Dangerous names, in its order: {@code Joy_XAxis} through {@code Joy_VAxis}.
 * <p>
 * They are DirectInput's eight axis slots - X, Y, Z, Rx, Ry, Rz and its two sliders, which the game calls U and V.
 * A controller axis with none of these slots is one the game cannot see.
 */
public enum GameAxis {
    X, Y, Z, RX, RY, RZ, U, V;

    /** The token {@code .binds} and {@code .buttonMap} use for this axis - {@code Joy_RZAxis}. */
    public String token() {
        return "Joy_" + name() + "Axis";
    }
}
