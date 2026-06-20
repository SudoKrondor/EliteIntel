package elite.intel.devices;

import elite.intel.devices.events.*;
import elite.intel.devices.model.Device;
import elite.intel.eventbus.DeviceBus;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.sdl.SDLInit.SDL_INIT_GAMEPAD;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_JOYSTICK;

/**
 * Polls SDL3 for joystick/HOTAS/gamepad/pedal input on a dedicated platform thread. Publishes
 * connect/disconnect, axis, and button events on DeviceBus. Read-only device
 * access - never writes to the game or any game file.
 *
 * Singleton shared infrastructure: StarVizion, BindForge, and push-to-talk all consume these
 * events rather than owning their own SDL3 context. Call start() once; stop() to shut down
 * cleanly.
 */
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);
    private static final int POLL_INTERVAL_MS = 16; // ~60 Hz
    private static final float AXIS_SCALE = 1.0f / 32767.0f;

    private static volatile DeviceService instance;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean available = new AtomicBoolean(false);
    private volatile Thread pollThread;

    // Written only from the poll thread; readable from any thread via CopyOnWriteArrayList.
    private final CopyOnWriteArrayList<Device> connectedDevices = new CopyOnWriteArrayList<>();

    // Open joystick handles - keyed by SDL JoystickID. Accessed only from pollThread.
    private final Map<Integer, Long> openHandles = new LinkedHashMap<>();
    private final Map<Integer, short[]> prevAxes = new HashMap<>();
    private final Map<Integer, boolean[]> prevButtons = new HashMap<>();
    private final Map<Integer, int[]> vidPidByDevice = new HashMap<>(); // [vendor, product]

    private DeviceService() {}

    public static DeviceService getInstance() {
        if (instance == null) {
            synchronized (DeviceService.class) {
                if (instance == null) instance = new DeviceService();
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------

    public boolean isAvailable() { return available.get(); }

    /**
     * Returns a snapshot of currently connected devices - safe to call from any thread.
     */
    public List<Device> getConnectedDevices() {
        return Collections.unmodifiableList(new ArrayList<>(connectedDevices));
    }

    // -------------------------------------------------------------------------

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        pollThread = Thread.ofPlatform().name("elite-intel-devices").start(this::pollLoop);
    }

    public void stop() {
        running.set(false);
        if (pollThread != null) {
            try { pollThread.join(3_000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // -------------------------------------------------------------------------

    private void pollLoop() {
        try {
            if (!initSdl()) return;
            available.set(true);
            DeviceBus.publish(new DeviceServiceStateEvent(true, null));

            Set<Integer> knownIds = new HashSet<>();
            boolean firstPoll = true;

            while (running.get()) {
                SDLEvents.SDL_PumpEvents();

                Set<Integer> currentIds = enumerateJoystickIds();
                if (firstPoll) {
                    log.debug("Device service initial joystick enumeration: {} device(s) found, ids={}", currentIds.size(), currentIds);
                    firstPoll = false;
                }

                for (int id : currentIds) {
                    if (knownIds.add(id)) onDeviceAdded(id);
                }

                Iterator<Integer> it = knownIds.iterator();
                while (it.hasNext()) {
                    int id = it.next();
                    if (!currentIds.contains(id)) {
                        onDeviceRemoved(id);
                        it.remove();
                    }
                }

                for (Map.Entry<Integer, Long> entry : openHandles.entrySet()) {
                    pollJoystick(entry.getKey(), entry.getValue());
                }

                //noinspection BusyWait
                Thread.sleep(POLL_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            closeAllHandles();
            SDLInit.SDL_Quit();
            available.set(false);
            log.info("Device service stopped");
        }
    }

    // -------------------------------------------------------------------------
    // LWJGL3 native library path setup
    // -------------------------------------------------------------------------

    /**
     * Redirect LWJGL3's SharedLibraryLoader away from %TEMP% and toward our own distribution
     * directory so the extracted DLLs are in a fixed, AV-trusted location.
     *
     * On Windows, loading from %TEMP% reliably fails when:
     *   - Antivirus holds an exclusive lock on newly-written files longer than LWJGL3's lock() wait
     *   - The username contains non-ASCII characters (JDK-8195129: LoadLibraryA can't handle them)
     *   - Group Policy / noexec mount prevents executing binaries from temp directories
     *
     * Both org.lwjgl.system.SharedLibraryExtractPath and org.lwjgl.librarypath are "Dynamic"
     * configuration properties in LWJGL3 - they are read on every .get() call, so setting them
     * here (before any LWJGL3 class is touched) is guaranteed to take effect.
     */
    private void configureLwjglNativePath() {
        try {
            URL src = DeviceService.class.getProtectionDomain().getCodeSource().getLocation();
            Path nativesDir = Paths.get(src.toURI()).getParent().resolve("native/lwjgl");
            if (!Files.isDirectory(nativesDir)) {
                log.debug("LWJGL3 native pre-extract dir not found ({}), using default temp extraction", nativesDir);
                return;
            }
            String abs = nativesDir.toAbsolutePath().toString();
            // Use pre-extracted dir as both the extract destination AND library search path.
            System.setProperty("org.lwjgl.system.SharedLibraryExtractPath", abs);
            String prev = System.getProperty("org.lwjgl.librarypath", "");
            System.setProperty("org.lwjgl.librarypath", prev.isEmpty() ? abs : abs + File.pathSeparator + prev);
            log.debug("LWJGL3 native path configured: {}", abs);
        } catch (Exception e) {
            log.debug("Could not configure LWJGL3 native path, falling back to temp extraction: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------

    private boolean initSdl() {
        // Must run before any LWJGL3 class is touched - both config properties are Dynamic.
        configureLwjglNativePath();
        try {
            boolean ok = SDLInit.SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD);
            if (!ok) {
                String err = SDLError.SDL_GetError();
                log.error("SDL_Init failed: {}", err);
                running.set(false);
                DeviceBus.publish(new DeviceServiceStateEvent(false, err));
                return false;
            }
            log.info("Device service SDL3 initialized");
            return true;
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError | NoClassDefFoundError e) {
            // NoClassDefFoundError is thrown on second access if Library's static init failed.
            log.error("SDL3 native libraries not available: {}", e.getMessage());
            running.set(false);
            DeviceBus.publish(new DeviceServiceStateEvent(false, e.getMessage()));
            return false;
        }
    }

    private Set<Integer> enumerateJoystickIds() {
        Set<Integer> ids = new HashSet<>();
        IntBuffer buf = SDLJoystick.SDL_GetJoysticks();
        if (buf != null) {
            while (buf.hasRemaining()) ids.add(buf.get());
        } else {
            log.error("SDL_GetJoysticks() returned null: {}", SDLError.SDL_GetError());
        }
        return ids;
    }

    private void onDeviceAdded(int id) {
        log.debug("Device enumeration found device id={}", id);
        long handle = SDLJoystick.SDL_OpenJoystick(id);
        if (handle == 0L) {
            log.error("SDL_OpenJoystick({}) failed: {}", id, SDLError.SDL_GetError());
            return;
        }
        openHandles.put(id, handle);

        int axes    = SDLJoystick.SDL_GetNumJoystickAxes(handle);
        int buttons = SDLJoystick.SDL_GetNumJoystickButtons(handle);
        String name = SDLJoystick.SDL_GetJoystickNameForID(id);
        if (name == null || name.isBlank()) name = "Joystick " + id;

        String usbPath = SDLJoystick.SDL_GetJoystickPathForID(id);
        if (usbPath == null) usbPath = "";
        String guid = readGuid(id);

        prevAxes.put(id, new short[Math.max(0, axes)]);
        prevButtons.put(id, new boolean[Math.max(0, buttons)]);

        Device device = new Device(id, name, axes, buttons, usbPath, guid);
        connectedDevices.add(device);
        log.info("Device connected: {} (id={}, axes={}, buttons={})", name, id, axes, buttons);
        DeviceBus.publish(new DeviceConnectedEvent(device));

        checkForDuplicate(id, device);
    }

    private void onDeviceRemoved(int id) {
        Long handle = openHandles.remove(id);
        if (handle != null) SDLJoystick.SDL_CloseJoystick(handle);
        prevAxes.remove(id);
        prevButtons.remove(id);
        vidPidByDevice.remove(id);
        connectedDevices.removeIf(d -> d.id() == id);
        log.info("Device disconnected: id={}", id);
        DeviceBus.publish(new DeviceDisconnectedEvent(id));
    }

    private void pollJoystick(int id, long handle) {
        short[] prev = prevAxes.get(id);
        boolean[] prevBtn = prevButtons.get(id);
        if (prev == null || prevBtn == null) return;

        for (int a = 0; a < prev.length; a++) {
            short raw = SDLJoystick.SDL_GetJoystickAxis(handle, a);
            if (raw != prev[a]) {
                prev[a] = raw;
                float normalized = Math.max(-1f, raw * AXIS_SCALE);
                DeviceBus.publish(new DeviceAxisEvent(id, a, normalized));
            }
        }

        for (int b = 0; b < prevBtn.length; b++) {
            boolean pressed = SDLJoystick.SDL_GetJoystickButton(handle, b);
            if (pressed != prevBtn[b]) {
                prevBtn[b] = pressed;
                DeviceBus.publish(new DeviceButtonEvent(id, b, pressed));
            }
        }
    }

    private void closeAllHandles() {
        for (long h : openHandles.values()) SDLJoystick.SDL_CloseJoystick(h);
        openHandles.clear();
        prevAxes.clear();
        prevButtons.clear();
        vidPidByDevice.clear();
        connectedDevices.clear();
    }

    // -------------------------------------------------------------------------
    // GUID / duplicate device detection
    // -------------------------------------------------------------------------

    /** Reads the 32-character hex GUID string for a device, or "" if unavailable. */
    private String readGuid(int id) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_GUID guid = SDL_GUID.malloc(stack);
            SDLJoystick.SDL_GetJoystickGUIDForID(id, guid);
            ByteBuffer out = stack.malloc(33);
            SDLGUID.SDL_GUIDToString(guid, out);
            byte[] hex = new byte[32];
            out.get(hex);
            return new String(hex, StandardCharsets.US_ASCII);
        } catch (Exception e) {
            log.debug("Could not read GUID for device {}: {}", id, e.getMessage());
            return "";
        }
    }

    /** Publishes DeviceDuplicateWarningEvent if the newly added device shares VID/PID with an already-connected device. */
    private void checkForDuplicate(int newId, Device newDevice) {
        int vendor = SDLJoystick.SDL_GetJoystickVendorForID(newId) & 0xFFFF;
        int product = SDLJoystick.SDL_GetJoystickProductForID(newId) & 0xFFFF;

        for (Map.Entry<Integer, int[]> entry : vidPidByDevice.entrySet()) {
            int[] vidPid = entry.getValue();
            if (vidPid[0] == vendor && vidPid[1] == product) {
                int existingId = entry.getKey();
                for (Device existing : connectedDevices) {
                    if (existing.id() == existingId) {
                        DeviceBus.publish(new DeviceDuplicateWarningEvent(existing, newDevice));
                        break;
                    }
                }
            }
        }

        vidPidByDevice.put(newId, new int[]{vendor, product});
    }
}
