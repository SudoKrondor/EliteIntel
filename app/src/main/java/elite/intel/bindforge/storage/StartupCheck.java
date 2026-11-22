package elite.intel.bindforge.storage;

import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The check when Elite-Intel starts, reading only (storage.md rule 15; Storage S8a): once first-time startup is
 * complete, the bindings folder is compared with the working copy as the check does, and if anything differs it says
 * so once - <em>"Your control files were changed outside BindForge. Open BindForge to check them."</em> Nothing is
 * written, no backup is made, and nothing is decided until the user opens BindForge. Said by {@link StartupNotice}, and
 * not while BindForge is read only (Storage S14c).
 * <p>
 * Every reachable installation's device files are compared too, since Storage S9a ({@link DeviceCheck}); one it cannot
 * read at all is logged, not said - the first click names it. Anything that stops it is logged and it says nothing: it
 * is a hint, and the check at the first BindForge click runs anyway.
 */
public final class StartupCheck {

    private static final Logger log = LogManager.getLogger(StartupCheck.class);

    /** Reads the working copy. */
    @FunctionalInterface
    interface Source {
        ConfigurationFiles read() throws IOException;
    }

    private final BooleanSupplier setUp;
    private final Source workingCopy;
    private final Supplier<Path> bindingsFolder;
    private final DeviceCheck.Source devices;

    /**
     * First-time startup's flag, the stored working copy, the bindings folder Elite-Intel is set to and the stored
     * installations.
     */
    public static StartupCheck stored() {
        return new StartupCheck(() -> !FirstTimeStartup.stored().needed(), () -> WorkingCopy.stored().read(),
                () -> PlayerSession.getInstance().getBindingsDir(), DeviceCheck.stored());
    }

    StartupCheck(BooleanSupplier setUp, Source workingCopy, Supplier<Path> bindingsFolder, DeviceCheck.Source devices) {
        this.setUp = setUp;
        this.workingCopy = workingCopy;
        this.bindingsFolder = bindingsFolder;
        this.devices = devices;
    }

    /**
     * Whether a live binds or preset file, or an installation's device files, differ from the working copy. On the
     * caller's thread; touches the disk.
     */
    boolean changedOutside() {
        try {
            if (!setUp.getAsBoolean()) return false;
            ConfigurationFiles files = workingCopy.read();
            Map<String, byte[]> live = BindingsCheck.readBindingsFolder(bindingsFolder.get());
            BindingsComparison.Differences differences =
                    BindingsComparison.compare(files.bindings(), live, BindingsComparison.Factory.NONE);
            List<DeviceCheck.Judged> installations = devices.find(files).asked();
            if (differences.none() && installations.isEmpty()) return false;
            log.info("Your control files were changed outside BindForge: {}; newer versions: {}; installations: {}",
                    differences.files().stream().map(BindingsComparison.FileDifference::file).toList(),
                    differences.newer().stream().map(BindingsComparison.NewerVersion::newer).toList(),
                    installations.stream().map(judged -> judged.row().storefront() + " (" + judged.row().rootPath()
                            + "): " + judged.state()).toList());
            return true;
        } catch (IOException | RuntimeException e) {
            // WHY: broad on purpose - a thread of its own at startup. A hint only; the check at the first BindForge
            // click reads the files again and says what stopped it.
            log.warn("The check when Elite-Intel starts could not compare the control files", e);
            return false;
        }
    }
}
