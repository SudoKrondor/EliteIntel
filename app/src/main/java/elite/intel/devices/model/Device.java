package elite.intel.devices.model;

/**
 * A connected USB gaming device (joystick, HOTAS, gamepad, or pedal set), as reported by SDL3.
 */
public record Device(
        int id,           // SDL3 instance ID — session-scoped, reassigned on reconnect
        String name,      // SDL3 device name
        int axisCount,    // number of analog axes
        int buttonCount,  // number of buttons
        String usbPath,   // USB port path from SDL_GetJoystickPathForID — for duplicate VID/PID detection
        String guid,      // SDL3 GUID — used for VID/PID extraction and .binds file correlation
        boolean gamepad   // SDL's semantic gamepad classification, independent of the display name
) {

    @Override
    public String toString() {
        return name;
    }
}
