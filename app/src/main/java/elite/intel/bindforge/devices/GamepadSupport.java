package elite.intel.bindforge.devices;

import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.GameAxis;

import java.util.List;
import java.util.Locale;

/**
 * Shared gamepad classification.
 */
public final class GamepadSupport {

    private GamepadSupport() {
    }

    public static boolean isGamepad(Device device) {
        return device != null && (device.gamepad() || isGamepadName(device.name()));
    }

    private static boolean isGamepadName(String name) {
        if (name == null) return false;
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("xbox") || normalized.contains("x-input")
                || normalized.contains("xinput") || normalized.contains("gamepad")
                || normalized.contains("game pad");
    }

    public static List<GameAxis> axes(int axisCount) {
        return DeviceDescription.gamepad(axisCount, 0).sdlAxes();
    }
}
