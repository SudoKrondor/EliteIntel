package elite.intel.starvizion.input;

import elite.intel.gameapi.EventBusManager;
import elite.intel.starvizion.event.SvAxisStateEvent;
import elite.intel.starvizion.event.SvButtonStateEvent;
import elite.intel.starvizion.event.SvDeviceConnectedEvent;
import elite.intel.starvizion.event.SvDeviceDisconnectedEvent;
import elite.intel.starvizion.event.SvServiceStateEvent;
import elite.intel.starvizion.model.SvDevice;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLJoystick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.sdl.SDLInit.*;

/**
 * Polls SDL3 for joystick/HOTAS/gamepad/pedal input on a dedicated platform thread.
 * Publishes connect/disconnect and axis/button state events on the main EventBusManager.
 * Read-only device access — never writes to the game or any game file.
 *
 * Singleton. Call start() once; stop() to shut down cleanly.
 */
public class SdlInputService {

    private static final Logger log = LoggerFactory.getLogger(SdlInputService.class);
    private static final int POLL_INTERVAL_MS = 16; // ~60 Hz
    private static final float AXIS_SCALE = 1.0f / 32767.0f;

    private static volatile SdlInputService instance;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean available = new AtomicBoolean(false);
    private Thread sdlThread;

    // Written only from the SDL thread; readable from any thread via CopyOnWriteArrayList.
    private final CopyOnWriteArrayList<SvDevice> connectedDevices = new CopyOnWriteArrayList<>();

    // Open joystick handles — keyed by SDL JoystickID. Accessed only from sdlThread.
    private final Map<Integer, Long> openHandles = new LinkedHashMap<>();
    private final Map<Integer, short[]> prevAxes = new HashMap<>();
    private final Map<Integer, boolean[]> prevButtons = new HashMap<>();

    private SdlInputService() {}

    public static SdlInputService getInstance() {
        if (instance == null) {
            synchronized (SdlInputService.class) {
                if (instance == null) instance = new SdlInputService();
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------

    public boolean isAvailable() { return available.get(); }

    /** Returns a snapshot of currently connected devices — safe to call from any thread. */
    public List<SvDevice> getConnectedDevices() {
        return Collections.unmodifiableList(new ArrayList<>(connectedDevices));
    }

    // -------------------------------------------------------------------------

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        sdlThread = Thread.ofPlatform().name("starvizion-sdl").start(this::sdlLoop);
    }

    public void stop() {
        running.set(false);
        if (sdlThread != null) {
            try { sdlThread.join(3_000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // -------------------------------------------------------------------------

    private void sdlLoop() {
        try {
            if (!initSdl()) return;
            available.set(true);
            EventBusManager.publish(new SvServiceStateEvent(true, null));

            Set<Integer> knownIds = new HashSet<>();

            while (running.get()) {
                SDLEvents.SDL_PumpEvents();

                Set<Integer> currentIds = enumerateJoystickIds();

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
            log.info("StarVizion SDL3 service stopped");
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
     * configuration properties in LWJGL3 — they are read on every .get() call, so setting them
     * here (before any LWJGL3 class is touched) is guaranteed to take effect.
     */
    private void configureLwjglNativePath() {
        try {
            URL src = SdlInputService.class.getProtectionDomain().getCodeSource().getLocation();
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
        // Must run before any LWJGL3 class is touched — both config properties are Dynamic.
        configureLwjglNativePath();
        try {
            boolean ok = SDLInit.SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD);
            if (!ok) {
                String err = SDLError.SDL_GetError();
                log.error("SDL_Init failed: {}", err);
                running.set(false);
                EventBusManager.publish(new SvServiceStateEvent(false, err));
                return false;
            }
            log.info("StarVizion SDL3 initialized");
            return true;
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError | NoClassDefFoundError e) {
            // NoClassDefFoundError is thrown on second access if Library's static init failed.
            log.error("SDL3 native libraries not available: {}", e.getMessage());
            running.set(false);
            EventBusManager.publish(new SvServiceStateEvent(false, e.getMessage()));
            return false;
        }
    }

    private Set<Integer> enumerateJoystickIds() {
        Set<Integer> ids = new HashSet<>();
        IntBuffer buf = SDLJoystick.SDL_GetJoysticks();
        if (buf != null) {
            while (buf.hasRemaining()) ids.add(buf.get());
        }
        return ids;
    }

    private void onDeviceAdded(int id) {
        long handle = SDLJoystick.SDL_OpenJoystick(id);
        if (handle == 0L) {
            log.warn("SDL_OpenJoystick({}) failed: {}", id, SDLError.SDL_GetError());
            return;
        }
        openHandles.put(id, handle);

        int axes    = SDLJoystick.SDL_GetNumJoystickAxes(handle);
        int buttons = SDLJoystick.SDL_GetNumJoystickButtons(handle);
        String name = SDLJoystick.SDL_GetJoystickNameForID(id);
        if (name == null || name.isBlank()) name = "Joystick " + id;

        prevAxes.put(id, new short[Math.max(0, axes)]);
        prevButtons.put(id, new boolean[Math.max(0, buttons)]);

        SvDevice device = new SvDevice(id, name, axes, buttons);
        connectedDevices.add(device);
        log.info("StarVizion device connected: {} (id={}, axes={}, buttons={})", name, id, axes, buttons);
        EventBusManager.publish(new SvDeviceConnectedEvent(device));
    }

    private void onDeviceRemoved(int id) {
        Long handle = openHandles.remove(id);
        if (handle != null) SDLJoystick.SDL_CloseJoystick(handle);
        prevAxes.remove(id);
        prevButtons.remove(id);
        connectedDevices.removeIf(d -> d.id() == id);
        log.info("StarVizion device disconnected: id={}", id);
        EventBusManager.publish(new SvDeviceDisconnectedEvent(id));
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
                EventBusManager.publish(new SvAxisStateEvent(id, a, normalized));
            }
        }

        for (int b = 0; b < prevBtn.length; b++) {
            boolean pressed = SDLJoystick.SDL_GetJoystickButton(handle, b);
            if (pressed != prevBtn[b]) {
                prevBtn[b] = pressed;
                EventBusManager.publish(new SvButtonStateEvent(id, b, pressed));
            }
        }
    }

    private void closeAllHandles() {
        for (long h : openHandles.values()) SDLJoystick.SDL_CloseJoystick(h);
        openHandles.clear();
        prevAxes.clear();
        prevButtons.clear();
        connectedDevices.clear();
    }
}
