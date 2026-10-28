package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.ButtonMapLabels;
import elite.intel.bindforge.devicefiles.DeviceEntry;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlots;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.storage.SavedDeviceLabels;
import elite.intel.devices.DeviceService;
import elite.intel.devices.model.Device;
import elite.intel.devices.model.DeviceDescription;
import elite.intel.devices.model.DeviceIdentity;
import elite.intel.devices.model.GameAxis;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The controllers the capture dialog offers a tab for, under the name {@code .binds} calls each.
 * <p>
 * <strong>Every controller the user has, connected or not</strong> (Alan, 2026-10-15): the connected ones,
 * every one of the user's elements in the working copy, and every one the bindings name. A stick binding must not lose its tab because the
 * stick is asleep or unplugged - an unplugged controller's inputs are clicked, not pressed, and come from its saved
 * names and the inputs the bindings already use.
 * <p>
 * <strong>The name is the working copy's</strong> - what the installations hold now (Alan, 2026-10-15; read from the
 * working copy since Storage S4b, 2026-10-28): the first element in its {@code DeviceMappings.xml} claiming the
 * controller, as the game resolves it, and Frontier's when the working copy holds none. A device renamed on screen and
 * not yet applied is still under its old name in the working copy and in {@code .binds}, so a binding written under
 * that name moves with all the others at APPLY. Labels are the edits' ({@link SavedDeviceLabels}).
 * <p>
 * <strong>A connected controller with no name gets no capture</strong>: {@code .binds} would have to name it by
 * hardware id, which BindForge moves to a name at APPLY anyway. Its tab is shown and greyed, saying where to name it.
 */
public final class CaptureControllers {

    private static final Pattern BUTTON = Pattern.compile("Joy_(\\d+)");
    private static final Pattern HAT = Pattern.compile("Joy_POV(\\d)(?:Up|Down|Left|Right)");
    // A hardware id: .binds names an unnamed controller by VID+PID until BindForge moves it to a name.
    private static final Pattern HARDWARE_ID = Pattern.compile("[0-9A-Fa-f]{8}");

    /**
     * @param name    the controller's {@code .binds} name, or {@code null} when it has none yet
     * @param liveId  the Device Service's id while it is connected, else {@code -1}
     * @param product what the controller calls itself, for a tab with no name
     * @param axes    the game axis behind each SDL axis index; empty for a controller that is not connected
     * @param gamepad whether the controller is a gamepad, whose inputs are bound under the game's GamePad tokens
     */
    public record Controller(String name, int liveId, String product, int buttonCount, int hatCount,
                             List<GameAxis> axes, boolean gamepad) {

        public Controller {
            axes = List.copyOf(axes);
        }

        public boolean named() {
            return name != null;
        }

        public boolean connected() {
            return liveId >= 0;
        }

        /**
         * What its tab says.
         */
        public String tabTitle() {
            return named() ? name : product;
        }
    }

    private CaptureControllers() {
    }

    /**
     * Connected controllers first - named ones, then unnamed - then the ones only the working copy or the bindings know,
     * each group in name order.
     *
     * @param bindings the bindings being edited, for the controllers they name and the inputs they use
     * @param labels   the edits' names and labels, as {@link SavedDeviceLabels#load()} read them
     */
    public static List<Controller> all(Map<String, ReadOnlyBindingSlots> bindings, SavedDeviceLabels labels) {
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        List<Controller> controllers = new ArrayList<>(connected(labels));
        Set<String> present = new HashSet<>();
        controllers.stream().filter(Controller::named).forEach(c -> present.add(c.name().toLowerCase(Locale.ROOT)));

        Map<String, Set<String>> used = tokensByDevice(bindings);
        Set<String> known = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        labels.names().stream().filter(name -> !stock.ships(name)).forEach(known::add);
        known.addAll(used.keySet());

        List<Controller> absent = new ArrayList<>();
        for (String name : known) {
            if (present.contains(name.toLowerCase(Locale.ROOT))) continue;
            Set<String> tokens = new HashSet<>(labels.labelsOf(name).keySet());
            tokens.addAll(used.getOrDefault(name, Set.of()));
            int[] counts = counts(tokens);
            // Nothing saved and nothing bound: no input to offer, so no tab.
            if (counts[0] == 0 && counts[1] == 0) continue;
            absent.add(new Controller(name, -1, name, counts[0], counts[1], List.of(), false));
        }
        controllers.addAll(absent);
        return List.copyOf(controllers);
    }

    /**
     * Every connected controller, named ones first, each group in name order. Empty when the service is down.
     */
    public static List<Controller> connected() {
        return connected(SavedDeviceLabels.load());
    }

    private static List<Controller> connected(SavedDeviceLabels labels) {
        DeviceService service = DeviceService.getInstance();
        if (!service.isAvailable()) return List.of();
        FrontierStockDevices stock = FrontierStockDevices.getInstance();
        List<Controller> controllers = new ArrayList<>();
        for (Device device : service.getConnectedDevices()) {
            Optional<DeviceIdentity> identity = DeviceIdentities.of(device);
            String name = identity.map(id -> nameOf(id, labels, stock)).orElse(null);
            DeviceDescription description = service.description(device.id()).orElse(DeviceDescription.axesUnknown(0));
            boolean gamepad = GamepadSupport.isGamepad(device);
            List<GameAxis> axes = axesFor(gamepad, device.axisCount(), description);
            controllers.add(new Controller(name, device.id(), device.name(), device.buttonCount(),
                    description.hatCount(), axes, gamepad));
        }
        controllers.sort(Comparator.comparing((Controller c) -> !c.named())
                .thenComparing(Controller::tabTitle, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(controllers);
    }

    static List<GameAxis> axesFor(boolean gamepad, int axisCount, DeviceDescription description) {
        if (gamepad) return GamepadSupport.axes(axisCount);
        if (description.agreesWith(axisCount)) return description.sdlAxes();
        return ButtonMapLabels.axesToLabel(axisCount, description);
    }

    /**
     * The {@code .binds} name of one connected controller, as {@link #connected()} names it - for a caller that needs
     * one controller, not all of them (review, 2026-10-17).
     *
     * @param labels the edits, as {@link SavedDeviceLabels#load()} read them - off the EDT
     * @return empty when the controller has no name yet, or its GUID could not be read
     */
    public static Optional<String> nameOf(Device device, SavedDeviceLabels labels) {
        return DeviceIdentities.of(device).map(id -> nameOf(id, labels, FrontierStockDevices.getInstance()));
    }

    /**
     * How many buttons and hats an unplugged controller is shown with: up to the highest numbered one its saved
     * names or its bindings mention. Pure.
     *
     * @return {buttons, hats}
     */
    static int[] counts(Collection<String> tokens) {
        int buttons = 0;
        int hats = 0;
        for (String token : tokens) {
            Matcher button = BUTTON.matcher(token);
            if (button.matches()) buttons = Math.max(buttons, Integer.parseInt(button.group(1)));
            Matcher hat = HAT.matcher(token);
            if (hat.matches()) hats = Math.max(hats, Integer.parseInt(hat.group(1)));
        }
        return new int[]{buttons, hats};
    }

    /**
     * The controller tokens each named device is used with in the bindings - main inputs and modifiers.
     */
    private static Map<String, Set<String>> tokensByDevice(Map<String, ReadOnlyBindingSlots> bindings) {
        Map<String, Set<String>> byDevice = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (bindings == null) return byDevice;
        for (ReadOnlyBindingSlots slots : bindings.values()) {
            if (slots == null) continue;
            for (ReadOnlyBindingSlot slot : new ReadOnlyBindingSlot[]{slots.primary(), slots.secondary()}) {
                if (!KeyBindingsParser.isAssigned(slot)) continue;
                note(byDevice, slot.device(), slot.key());
                slot.bindingModifiers().forEach(m -> note(byDevice, m.device(), m.key()));
            }
        }
        return byDevice;
    }

    private static void note(Map<String, Set<String>> byDevice, String device, String token) {
        if (!BindingInput.isDeviceName(device) || HARDWARE_ID.matcher(device).matches() || token == null) return;
        byDevice.computeIfAbsent(device, d -> new HashSet<>()).add(token);
    }

    private static String nameOf(DeviceIdentity identity, SavedDeviceLabels labels, FrontierStockDevices stock) {
        return labels.nameOf(identity.vid(), identity.pid())
                .or(() -> stock.covering(identity.vid(), identity.pid()).map(DeviceEntry::name))
                .orElse(null);
    }
}
