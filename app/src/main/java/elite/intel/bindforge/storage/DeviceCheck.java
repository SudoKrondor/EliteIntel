package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.bindforge.storage.DeviceFilesComparison.Verdict;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeInstallationsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * The check for the device files (storage.md rule 14, <em>Settled with Storage S9's sources</em>; Storage S9a): every
 * reachable installation's {@code DeviceMappings.xml} and {@code .buttonMap}s judged against the working copy by value
 * ({@link DeviceFilesComparison}), and - when one no longer matches - the reconcile first setup uses (rule 2), with
 * the working copy as one of the sides ({@link Found#reading}).
 * <p>
 * Nothing is written here: {@link BindingsCheck#settle} writes the device files the user settled on with the binds and
 * preset files, in one backup and one write. Touches the disk where it reads - not for the EDT.
 */
public final class DeviceCheck {

    private static final Logger log = LogManager.getLogger(DeviceCheck.class);

    /**
     * The working copy's side in a reconcile. Stored installation ids start at 1, so 0 is no installation's; it is used
     * only in the reading {@link Found#reading} makes.
     */
    public static final long WORKING_COPY_SIDE = 0L;

    /** How one installation stands against the working copy. */
    public enum State {
        MATCHES,
        DIFFERS,
        /** Its {@code DeviceMappings.xml} is gone - a reset; only putting the working copy back is offered. */
        RESET_MISSING,
        /** Its {@code DeviceMappings.xml} holds none of the user's elements - a reset. */
        RESET_FACTORY,
        /**
         * A file that is there could not be parsed. Putting the working copy back is offered: the backup holds its bytes
         * (Alan, 2026-10-31).
         */
        UNPARSEABLE,
        /**
         * A file could not be read at all. Named, and left as it is: the backup could not hold it (Alan, 2026-10-31).
         */
        UNREADABLE;

        /** Whether a game update probably reset it. */
        public boolean reset() {
            return this == RESET_MISSING || this == RESET_FACTORY;
        }

        /** Whether the check asks about it - everything but a match, and a file it could not read at all. */
        public boolean asked() {
            return this != MATCHES && this != UNREADABLE;
        }

        /**
         * Whether its files can be a side of the reconcile: there, readable, and holding a
         * {@code DeviceMappings.xml}.
         */
        public boolean side() {
            return this == DIFFERS || this == RESET_FACTORY;
        }

        private static State of(Verdict verdict) {
            return switch (verdict) {
                case MATCHES -> MATCHES;
                case DIFFERS -> DIFFERS;
                case RESET_MISSING -> RESET_MISSING;
                case RESET_FACTORY -> RESET_FACTORY;
            };
        }
    }

    /** Reads the device files of the installation at a root. */
    @FunctionalInterface
    public interface Reader {
        DeviceFiles read(Path root) throws IOException;
    }

    /** Judges every reachable installation against a working copy. */
    @FunctionalInterface
    public interface Source {
        /**
         * @throws IOException if the working copy's own device files cannot be parsed - BindForge's own file, so a
         *                     broken invariant rather than an installation to name
         */
        Found find(ConfigurationFiles workingCopy) throws IOException;
    }

    /**
     * One installation, judged.
     *
     * @param files   its device files as read - {@code null} when they could not be read
     * @param problem why a file could not be read or parsed; {@code null} otherwise
     */
    public record Judged(InstallationRow row, State state, DeviceFiles files, String problem) {
        public Judged {
            Objects.requireNonNull(row, "row");
            Objects.requireNonNull(state, "state");
            if (state != State.UNREADABLE) Objects.requireNonNull(files, "files");
        }

        public Path root() {
            return Path.of(row.rootPath());
        }

        /** Whether {@code other} is this installation, in the same state, holding the same files by value. */
        boolean sameAs(Judged other) {
            if (row.id() != other.row.id() || state != other.state) return false;
            if (files == null || other.files == null) return files == other.files;
            // WHY: a file that cannot be parsed has no values to compare - its bytes are all there is.
            if (state == State.UNPARSEABLE) return sameBytes(files, other.files);
            try {
                return DeviceFilesComparison.sameValues(files, other.files);
            } catch (IOException e) {
                // WHY: a file the judgement did not need to parse - a match leaves an installation's
                // DeviceMappings.xml unread when the working copy holds none - can fail here. What it holds is
                // unknown, so it counts as changed: the warning opens again rather than writing over it unseen.
                log.warn("Could not compare the device files of {} ({}) with the warning's, so they count as changed",
                        row.storefront(), row.rootPath(), e);
                return false;
            }
        }

        private static boolean sameBytes(DeviceFiles one, DeviceFiles other) {
            if (!Arrays.equals(one.deviceMappings(), other.deviceMappings())) return false;
            if (!one.buttonMaps().keySet().equals(other.buttonMaps().keySet())) return false;
            return one.buttonMaps().entrySet().stream()
                    .allMatch(map -> Arrays.equals(map.getValue(), other.buttonMaps().get(map.getKey())));
        }
    }

    /**
     * What the check found for the device files.
     *
     * @param checked       whether the device files were judged at all - APPLY's check does not judge them until
     *                      Storage S9b
     * @param installations every reachable installation, in the stored list's order
     */
    public record Found(boolean checked, List<Judged> installations) {

        /** The device files were not judged. */
        public static final Found NOT_CHECKED = new Found(false, List.of());

        public Found {
            installations = List.copyOf(installations);
        }

        /** Every installation that does not match. */
        public List<Judged> differing() {
            return installations.stream().filter(judged -> judged.state() != State.MATCHES).toList();
        }

        /** Whether every installation matches. */
        public boolean none() {
            return differing().isEmpty();
        }

        /** The installations the user is asked about. */
        public List<Judged> asked() {
            return installations.stream().filter(judged -> judged.state().asked()).toList();
        }

        /** The installations whose files can be a side of the reconcile. */
        public List<Judged> sides() {
            return installations.stream().filter(judged -> judged.state().side()).toList();
        }

        /** Whether any installation looks reset by a game update. */
        public boolean anyReset() {
            return installations.stream().anyMatch(judged -> judged.state().reset());
        }

        /** Whether {@code other} judged the same installations the same way, holding the same files by value. */
        public boolean sameAs(Found other) {
            if (checked != other.checked || installations.size() != other.installations.size()) return false;
            for (int i = 0; i < installations.size(); i++) {
                if (!installations.get(i).sameAs(other.installations.get(i))) return false;
            }
            return true;
        }

        /**
         * The reconcile, as first setup's reading (storage.md rule 14; alias-designer.md, <em>The {@code .buttonMap}
         * label merge</em>): the working copy first - its {@code DeviceMappings.xml} gives the reconciled file's
         * Frontier entries - then each installation that can be a side. The binds files are the working copy's; they
         * rank a device red when bindings use it.
         *
         * @param workingCopyFolder where the working copy is, for a message naming it
         * @param workingCopyName   the working copy's name on screen, in the user's language
         * @throws IOException if a side's files cannot be read for the plan - the message names it
         */
        public FirstSetupReading reading(ConfigurationFiles workingCopy, Path bindingsFolder, Path workingCopyFolder,
                                         String workingCopyName) throws IOException {
            List<InstallationFiles> sides = new ArrayList<>();
            sides.add(new InstallationFiles(WORKING_COPY_SIDE, workingCopyName, workingCopyFolder,
                    workingCopy.hasDeviceMappings() ? workingCopy.deviceMappings() : null, workingCopy.buttonMaps()));
            for (Judged side : sides()) {
                sides.add(new InstallationFiles(side.row().id(), side.row().storefront(), side.root(),
                        side.files().deviceMappings(), side.files().buttonMaps()));
            }
            return FirstSetupReading.of(bindingsFolder, workingCopy.bindings(), sides, List.of(),
                    FrontierStockDevices.getInstance(), FrontierStockButtonMaps.getInstance());
        }

        /**
         * Which installations the check's write leaves alone: one it could not read at all; and one it never read - not
         * reachable when it ran, back by the write - when it differs from the working copy (storage.md rule 1). Every
         * other installation it read gets what the user settled on.
         *
         * @param workingCopy the working copy as it was before the check
         */
        LiveCopies.LeaveAlone leavingAlone(DeviceFilesComparison workingCopy, Apply.Reader reader) {
            LiveCopies.LeaveAlone unseen = Apply.leavingAloneWhereDifferent(workingCopy, reader);
            return installation -> {
                Path root = normal(installation.root());
                for (Judged judged : installations) {
                    if (normal(judged.root()).equals(root)) return judged.state() == State.UNREADABLE;
                }
                return unseen.leaveAlone(installation);
            };
        }

        private static Path normal(Path path) {
            return path.toAbsolutePath().normalize();
        }
    }

    /**
     * The device files the user settled on: the working copy's own, or what the reconcile made (Storage S9R, the S9a
     * review - one type each, so a put-back carries no files that mean nothing).
     */
    public sealed interface Answer permits PutMineBack, Reconciled {

        /** Put mine back - the working copy's device files, as they are. */
        PutMineBack PUT_MINE_BACK = new PutMineBack();

        /**
         * The reconcile's result.
         *
         * @param reconciled what the reconcile made - only its device files are used; the binds files are settled
         *                   by the binds check, and {@code moves} rewritten in them at the write
         */
        public static Reconciled reconciled(ConfigurationFiles reconciled, Map<String, String> moves) {
            return new Reconciled(reconciled.hasDeviceMappings() ? reconciled.deviceMappings() : null,
                    reconciled.buttonMaps(), moves);
        }

        /** Each losing name, by the name its bindings move to - a merge's; none for a put-back. */
        Map<String, String> moves();
    }

    /** The working copy's device files, as they are. Its one instance is {@link Answer#PUT_MINE_BACK}. */
    public static final class PutMineBack implements Answer {
        private PutMineBack() {
        }

        @Override
        public Map<String, String> moves() {
            return Map.of();
        }

        @Override
        public String toString() {
            return "PutMineBack";
        }
    }

    /**
     * What the reconcile made.
     *
     * @param deviceMappings the reconciled {@code DeviceMappings.xml}, or {@code null} for none
     * @param buttonMaps     the reconciled {@code .buttonMap}s, by file name
     * @param moves          each losing name, by the name its bindings move to - a merge's
     */
    public record Reconciled(byte[] deviceMappings, Map<String, byte[]> buttonMaps, Map<String, String> moves)
            implements Answer {
        public Reconciled {
            deviceMappings = deviceMappings == null ? null : deviceMappings.clone();
            buttonMaps = Collections.unmodifiableMap(new TreeMap<>(buttonMaps));
            moves = Collections.unmodifiableMap(new TreeMap<>(moves));
        }

        @Override
        public byte[] deviceMappings() {
            return deviceMappings == null ? null : deviceMappings.clone();
        }
    }

    private DeviceCheck() {
    }

    /** Every installation in the stored list, read from its folder, against Frontier's names. */
    public static Source stored() {
        return workingCopy -> judge(workingCopy, BindForgeInstallationsManager.getInstance().findAll(),
                DeviceFiles::read, name -> FrontierStockDevices.getInstance().ships(name));
    }

    /**
     * Judges every reachable installation among {@code rows} ({@link BackupSources.Installation#reachable}). One that is
     * not reachable is left out, and judged when it comes back (storage.md rule 1). Touches the disk.
     *
     * @throws IOException if the working copy's own device files cannot be parsed
     */
    static Found judge(ConfigurationFiles workingCopy, List<InstallationRow> rows, Reader reader,
                       Predicate<String> ships) throws IOException {
        DeviceFilesComparison comparison = DeviceFilesComparison.of(workingCopy, ships);
        List<Judged> judged = new ArrayList<>();
        for (InstallationRow row : rows) {
            if (!BackupSources.reachable(row)) continue;
            judged.add(judge(comparison, row, reader));
        }
        return new Found(true, judged);
    }

    private static Judged judge(DeviceFilesComparison comparison, InstallationRow row, Reader reader) {
        DeviceFiles files;
        try {
            files = reader.read(Path.of(row.rootPath()));
        } catch (IOException e) {
            log.warn("Could not read the device files of {} ({}), so they are left as they are", row.storefront(),
                    row.rootPath(), e);
            return new Judged(row, State.UNREADABLE, null, e.getMessage());
        }
        try {
            return new Judged(row, State.of(comparison.judge(files)), files, null);
        } catch (IOException e) {
            log.warn("Could not parse the device files of {} ({})", row.storefront(), row.rootPath(), e);
            return new Judged(row, State.UNPARSEABLE, files, e.getMessage());
        }
    }

    /**
     * The set the check writes: {@code settled}'s binds and preset files - a merge's moves rewritten in them - with the
     * device files the user settled on.
     *
     * @param settled the working copy with the binds and preset files the check settled
     * @throws IOException if a move cannot be made in a binds file - the message names it
     */
    static ConfigurationFiles withDevices(ConfigurationFiles settled, Answer answer) throws IOException {
        return switch (answer) {
            case PutMineBack putBack -> settled;
            case Reconciled reconciled -> {
                Map<String, byte[]> bindings = new TreeMap<>(settled.bindings());
                FirstSetupFiles.moveBindings(bindings, reconciled.moves());
                yield new ConfigurationFiles(bindings, reconciled.deviceMappings(), reconciled.buttonMaps());
            }
        };
    }
}
