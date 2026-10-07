package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.MasterDevice;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.install.GameInstallation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A generated {@code .buttonMap} for a connected controller that already has a name and has no file: one of the
 * writes outside Apply (spec, alias-designer.md, "At Elite-Intel startup").
 * <p>
 * <strong>It only ever creates.</strong> A {@code .buttonMap} already on disk - Frontier's {@code VPCPanel}, one
 * the user wrote, one generated last run - is never touched, and the write itself refuses to replace a file that
 * appears while it runs. Nothing is replaced, so nothing goes to Edit History.
 * <p>
 * <strong>It never names anything.</strong> The name is the first element in <em>that installation's</em>
 * {@code DeviceMappings.xml} claiming the controller's VID/PID, because the first match is the one the game uses
 * (domain doc §1.2d) and installations can disagree until first setup has run. A controller with no entry waits
 * for onboarding.
 * <p>
 * <strong>It leaves a device the master holds labels for alone.</strong> Apply writes that device's file from the
 * master, so a missing one is drift for the startup check to report and revert - generic labels written over the
 * gap would hide it. The master itself is never written here.
 * <p>
 * <strong>It leaves a device the game draws itself alone</strong> (Alan, 2026-10-06): an element carrying
 * {@code <SupportsIcons>}, and {@code <GamePad>}. Frontier's own readme says a label may be an icon token such as
 * {@code [x52b1]}, and these devices get the game's icons; {@code Button 3} would put text where an icon was. A
 * {@code GamePad} is bound by the game's own {@code GamePad_} tokens, so {@code Joy_} labels would never show.
 */
public final class ButtonMapGeneration {

    private static final Logger log = LogManager.getLogger(ButtonMapGeneration.class);

    private static final String GAME_PAD = "GamePad";
    private static final String SUPPORTS_ICONS = "SupportsIcons";

    /** A connected controller, as far as generation needs it. */
    public record Controller(String vid, String pid, int buttonCount, int axisCount) {
    }

    public enum Outcome {
        /** The file was written. */
        CREATED,
        /** A {@code .buttonMap} for the name is already there - before the write, or by the time it landed. */
        ALREADY_THERE,
        /** The installation has no entry for the controller, so it has no name yet. */
        NOT_NAMED,
        /** The master holds labels for the name; Apply owns its file. */
        MASTER_HAS_LABELS,
        /** The game draws this device's inputs itself - {@code <SupportsIcons>}, or {@code <GamePad>}. */
        GAME_DRAWS_IT,
        /** The controller reports no buttons and no axes, so there is nothing to label. */
        NOTHING_TO_LABEL,
        /** The installation's folder is not there. */
        SKIPPED_MISSING,
        /** Its {@code DeviceMappings.xml} could not be read, or the file could not be written. */
        FAILED
    }

    /**
     * @param deviceName the name the installation knows the controller by, or {@code null} when it has none
     * @param file       the {@code .buttonMap} that name means, or {@code null} exactly when there is no name or
     *                   the name cannot be a filename
     * @param reason     why it failed, and {@code null} otherwise
     */
    public record Result(Target target, String deviceName, Path file, Outcome outcome, String reason) {
    }

    /** The installation's element for the controller: its name, and whether it asks for the game's icons. */
    private record Named(String name, boolean gameDrawsIt) {
    }

    private final Supplier<List<Target>> targets;
    private final Supplier<Set<String>> masterLabelled;

    /** Generates into the stored installations, judged against the stored master. */
    public ButtonMapGeneration() {
        this(DeviceFilesPush::storedInstallations, ButtonMapGeneration::storedLabelledNames);
    }

    ButtonMapGeneration(Supplier<List<Target>> targets, Supplier<Set<String>> masterLabelled) {
        this.targets = targets;
        this.masterLabelled = masterLabelled;
    }

    /** Creates the controller's {@code .buttonMap} in every installation that names it and has none. */
    public List<Result> generateFor(Controller controller) {
        Set<String> labelled = masterLabelled.get().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<Result> results = new ArrayList<>();
        for (Target target : targets.get()) {
            Result result = generateIn(target, controller, labelled);
            if (result.outcome() == Outcome.CREATED) {
                log.info("Generated {} for {} ({})", result.file(), target.storefront(), target.controlSchemes());
            } else if (result.outcome() == Outcome.FAILED) {
                log.warn("Could not generate a .buttonMap for VID {} PID {} in {} ({}): {}", controller.vid(),
                        controller.pid(), target.storefront(), target.controlSchemes(), result.reason());
            }
            results.add(result);
        }
        return results;
    }

    private static Result generateIn(Target target, Controller controller, Set<String> labelled) {
        if (target.missing() || !Files.isDirectory(target.controlSchemes())) {
            return new Result(target, null, null, Outcome.SKIPPED_MISSING, null);
        }
        Optional<Named> named;
        try {
            named = nameOf(GameInstallation.deviceMappingsIn(target.controlSchemes()), controller);
        } catch (IOException e) {
            return new Result(target, null, null, Outcome.FAILED,
                    "could not read " + GameInstallation.DEVICE_MAPPINGS + ": " + e.getMessage());
        }
        if (named.isEmpty()) return new Result(target, null, null, Outcome.NOT_NAMED, null);

        String deviceName = named.get().name();
        Path file;
        try {
            file = GameInstallation.buttonMapIn(target.controlSchemes(), deviceName);
        } catch (InvalidPathException e) {
            // WHY: an XML tag may hold a colon, which no Windows filename can - the game cannot have a map for it either.
            return new Result(target, deviceName, null, Outcome.FAILED, "\"" + deviceName + "\" cannot be a filename");
        }
        if (named.get().gameDrawsIt()) return new Result(target, deviceName, file, Outcome.GAME_DRAWS_IT, null);
        if (labelled.contains(deviceName.toLowerCase(Locale.ROOT))) {
            return new Result(target, deviceName, file, Outcome.MASTER_HAS_LABELS, null);
        }
        if (Files.exists(file)) return new Result(target, deviceName, file, Outcome.ALREADY_THERE, null);

        Map<String, String> labels = ButtonMapLabels.generate(controller.buttonCount(), controller.axisCount());
        if (labels.isEmpty()) return new Result(target, deviceName, file, Outcome.NOTHING_TO_LABEL, null);
        try {
            boolean created = createNew(file, ButtonMapWriter.write(labels));
            return new Result(target, deviceName, file, created ? Outcome.CREATED : Outcome.ALREADY_THERE, null);
        } catch (IOException e) {
            return new Result(target, deviceName, file, Outcome.FAILED, e.getMessage());
        }
    }

    /** The first element claiming the controller's VID/PID, primary or {@code <Alternative>}. */
    private static Optional<Named> nameOf(Path deviceMappings, Controller controller) throws IOException {
        if (!Files.isRegularFile(deviceMappings)) return Optional.empty();
        byte[] xml = Files.readAllBytes(deviceMappings);
        DeviceEntry.HardwareId hardware = new DeviceEntry.HardwareId(controller.vid(), controller.pid());
        Optional<String> name = DeviceMappingsParser.parse(new ByteArrayInputStream(xml), deviceMappings.toString())
                .stream()
                .filter(entry -> entry.hardware().contains(hardware))
                .map(DeviceEntry::name)
                .findFirst();
        if (name.isEmpty()) return Optional.empty();
        return Optional.of(new Named(name.get(), GAME_PAD.equals(name.get()) || asksForIcons(xml, name.get())));
    }

    /**
     * Whether the first element of that name carries {@code <SupportsIcons>} anywhere inside it - Frontier puts one
     * inside an {@code <Alternative>} too, on {@code <SaitekX52>}.
     */
    private static boolean asksForIcons(byte[] xml, String name) throws IOException {
        NodeList children = DeviceFileXml.parse(xml).getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element && element.getTagName().equals(name)) {
                return element.getElementsByTagName(SUPPORTS_ICONS).getLength() > 0;
            }
        }
        return false;
    }

    /**
     * Writes a file that must not already exist: a flushed temp file beside it, then published under the target's
     * name by a step that refuses to replace. A reader sees no file or the whole file, and a file that appeared in
     * the meantime is kept.
     * <p>
     * Not {@code AtomicFiles.write}, which renames <em>over</em> the target - right for Apply, wrong for a write
     * whose one promise is that it never overwrites.
     *
     * @return {@code false} when the target was already there, in which case nothing was written
     */
    static boolean createNew(Path target, byte[] content) throws IOException {
        Path folder = target.getParent();
        Files.createDirectories(folder);
        Path temp = Files.createTempFile(folder, "." + target.getFileName(), ".tmp");
        IOException failure = null;
        try {
            writeFlushed(temp, content);
            return publish(temp, target);
        } catch (IOException e) {
            failure = e;
            throw e;
        } finally {
            deleteTemp(temp, failure);
        }
    }

    // WHY: a hard link is created by link(2) on Linux and CreateHardLink on Windows, and both fail atomically when
    // the target exists - a plain move only does that on Windows, while the JDK's Linux move checks and then
    // renames, which replaces. Some volumes (FAT, some shares) refuse hard links; there a plain move is used,
    // which refuses to replace on Windows and narrows the window to a check-then-rename on Linux.
    private static boolean publish(Path temp, Path target) throws IOException {
        try {
            Files.createLink(target, temp);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        } catch (IOException | UnsupportedOperationException linkRefused) {
            log.debug("Hard link refused for {}, falling back to a plain move: {}", target, linkRefused.getMessage());
        }
        try {
            Files.move(temp, target);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        }
    }

    private static void writeFlushed(Path file, byte[] content) throws IOException {
        Files.write(file, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    /**
     * Removes the temp file without letting its failure replace the outcome: attached to the real failure when
     * there is one, logged otherwise - the file was published, or was already there, either way.
     */
    private static void deleteTemp(Path temp, IOException failure) {
        try {
            Files.deleteIfExists(temp);
        } catch (IOException cleanupFailure) {
            if (failure != null) {
                failure.addSuppressed(cleanupFailure);
            } else {
                log.warn("Could not remove the temp file {}: {}", temp, cleanupFailure.getMessage());
            }
        }
    }

    private static Set<String> storedLabelledNames() {
        return DeviceFilesPush.storedMaster().stream()
                .filter(device -> !device.labels().isEmpty())
                .map(MasterDevice::name)
                .collect(Collectors.toSet());
    }
}
