package elite.intel.bindforge.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Everything one backup zip holds, read once, so what RESTORE puts back is exactly what was read (storage.md rule 10):
 * every {@code .binds} and {@code StartPreset.N.start} under {@code Bindings\}, and each installation's
 * {@code DeviceMappings.xml} and {@code .buttonMap}s under {@code Installations\<n>-<storefront>\ControlSchemes\}
 * (file-manager.md, <em>How the backup folder is laid out</em>).
 * <p>
 * <strong>Only a zip Elite-Intel made is read</strong> (Alan, 2026-10-25): one holding {@code backup-info.txt} and at
 * least one file in a backup's layout. Anything else is {@link NotABackupException}, and nothing is restored from it -
 * no restore may overwrite a file BindForge could not identify (file-manager.md). An entry outside the layout, or one
 * whose name is a path, is not part of the backup and is skipped. A Linux installation's own {@code Bindings\} is
 * skipped too: every binds read and write uses the one folder Elite-Intel is set to (storage brief, <em>Known
 * issues</em>). Touches the disk - not for the EDT.
 *
 * @param zip           the zip read
 * @param taken         when it was taken, as its {@code backup-info.txt} says, for the confirmation; {@code null} when
 *                      it does not say
 * @param bindings      every binds and preset file, by file name
 * @param installations each installation that has device files in it, in the backup's order
 */
public record BackupContents(Path zip, String taken, Map<String, byte[]> bindings,
                             List<InstallationFiles> installations) {

    private static final Logger log = LogManager.getLogger(BackupContents.class);

    /**
     * More than this, uncompressed, is not one of ours - a full backup is a few megabytes at most (file-manager.md,
     * <em>Backup Format</em>) - and reading it whole into memory could stop Elite-Intel.
     */
    static final long MAX_BYTES = 256L * 1024 * 1024;

    private static final char SEPARATOR = '/';
    private static final String TAKEN_LINE = "Taken: ";
    /** {@code Installations\1-STEAM: STEAM, D:\SteamLibrary}, either separator - the platform's when it was taken. */
    private static final Pattern INSTALLATION_LINE =
            Pattern.compile(Backups.INSTALLATIONS + "[\\\\/](\\d+-[^:]+): ([^,]+), (.+)");
    /** Bounded, so the number always fits - one of ours is never past a handful (review, 2026-10-25). */
    private static final Pattern INSTALLATION_FOLDER = Pattern.compile("(\\d{1,9})-(.+)");
    /** Characters Windows refuses in a file name - a name holding one is not one Elite-Intel wrote. */
    private static final String NOT_IN_A_NAME = "<>:\"|?*\\";

    public BackupContents {
        Objects.requireNonNull(zip, "zip");
        bindings = Collections.unmodifiableMap(new TreeMap<>(bindings));
        installations = List.copyOf(installations);
    }

    /** Thrown when a file is not a backup Elite-Intel made - not a zip, or no {@code backup-info.txt}, or empty. */
    public static final class NotABackupException extends IOException {
        NotABackupException(Path zip, String why) {
            super(zip.getFileName() + " is not an Elite-Intel backup: " + why);
        }
    }

    /**
     * Reads a backup zip - one from the list, or one the user browsed to.
     *
     * @throws NotABackupException if it is not a backup Elite-Intel made
     * @throws IOException         if it cannot be read
     */
    public static BackupContents read(Path zip) throws IOException {
        return read(zip, MAX_BYTES);
    }

    /** As {@link #read(Path)}, refusing a zip holding more than {@code maxBytes} once unpacked. */
    static BackupContents read(Path zip, long maxBytes) throws IOException {
        if (!Files.isRegularFile(zip)) throw new NotABackupException(zip, "not a file");
        try (ZipFile file = new ZipFile(zip.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry infoEntry = file.getEntry(Backups.INFO_FILE);
            if (infoEntry == null) throw new NotABackupException(zip, Backups.INFO_FILE + " is missing");
            Budget budget = new Budget(zip, maxBytes);
            String info = new String(bytesOf(file, infoEntry, budget), StandardCharsets.UTF_8);

            Map<String, byte[]> bindings = new TreeMap<>();
            Map<String, Found> installations = new TreeMap<>(BackupContents::byNumber);
            Enumeration<? extends ZipEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String[] parts = entry.getName().split(String.valueOf(SEPARATOR), -1);
                if (isBindingsEntry(parts)) {
                    bindings.put(parts[1], bytesOf(file, entry, budget));
                } else if (isDeviceMappingsEntry(parts)) {
                    installations.computeIfAbsent(parts[1], folder -> new Found()).deviceMappings = bytesOf(file, entry, budget);
                } else if (isButtonMapEntry(parts)) {
                    installations.computeIfAbsent(parts[1], folder -> new Found()).buttonMaps.put(parts[4],
                            bytesOf(file, entry, budget));
                } else if (!entry.getName().equals(Backups.INFO_FILE)) {
                    log.debug("Skipped {} in {}: not part of a backup's layout", entry.getName(), zip);
                }
            }
            if (bindings.isEmpty() && installations.isEmpty()) {
                throw new NotABackupException(zip, "it holds no game input configuration file");
            }
            return new BackupContents(zip, takenOf(info), bindings, installationsOf(installations, info));
        } catch (ZipException e) {
            throw new NotABackupException(zip, "not a zip");
        }
    }

    /** Whether it holds any binds or preset file - a backup without them is refused (storage.md rule 10). */
    public boolean hasBindings() {
        return !bindings.isEmpty();
    }

    /** Whether any installation's device files are in it - a backup without them is refused where installations are. */
    public boolean hasDeviceFiles() {
        return !installations.isEmpty();
    }

    // ---- the layout

    private static boolean isBindingsEntry(String[] parts) {
        return parts.length == 2 && parts[0].equals(Backups.BINDINGS) && plain(parts[1])
                && Backups.isBindingsFile(Path.of(parts[1]));
    }

    private static boolean isDeviceMappingsEntry(String[] parts) {
        return parts.length == 4 && isInstallationFolder(parts) && parts[2].equals(Backups.CONTROL_SCHEMES)
                && parts[3].equals(Backups.DEVICE_MAPPINGS);
    }

    private static boolean isButtonMapEntry(String[] parts) {
        return parts.length == 5 && isInstallationFolder(parts) && parts[2].equals(Backups.CONTROL_SCHEMES)
                && parts[3].equals(Backups.DEVICE_BUTTON_MAPS) && plain(parts[4])
                && parts[4].endsWith(Backups.BUTTON_MAP_SUFFIX) && parts[4].length() > Backups.BUTTON_MAP_SUFFIX.length();
    }

    private static boolean isInstallationFolder(String[] parts) {
        return parts[0].equals(Backups.INSTALLATIONS) && plain(parts[1])
                && INSTALLATION_FOLDER.matcher(parts[1]).matches();
    }

    // WHY: a name is written into a game folder, so one that is a path - "..", "C:x", a backslash - would write outside
    // it, and one Windows refuses would throw rather than be skipped (review, 2026-10-25). Never one Elite-Intel
    // wrote; skipped rather than trusted.
    private static boolean plain(String name) {
        if (name.isBlank() || name.equals(".") || name.equals("..")) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < ' ' || NOT_IN_A_NAME.indexOf(c) >= 0) return false;
        }
        return true;
    }

    // ---- what backup-info.txt says

    private static String takenOf(String info) {
        for (String line : info.split("\n")) {
            if (line.startsWith(TAKEN_LINE)) return line.substring(TAKEN_LINE.length()).trim();
        }
        return null;
    }

    /**
     * Each installation's files, labelled with the storefront and folder {@code backup-info.txt} records for it - or,
     * when it records none, the storefront in the zip's folder name and that name as its folder.
     */
    private static List<InstallationFiles> installationsOf(Map<String, Found> found, String info) {
        Map<String, String[]> recorded = new HashMap<>();
        for (String line : info.split("\n")) {
            Matcher matcher = INSTALLATION_LINE.matcher(line.trim());
            if (matcher.matches()) recorded.put(matcher.group(1), new String[]{matcher.group(2), matcher.group(3)});
        }
        List<InstallationFiles> installations = new ArrayList<>();
        found.forEach((folder, files) -> {
            // WHY: only folders this pattern matched were read, so it matches here too.
            Matcher name = INSTALLATION_FOLDER.matcher(folder);
            if (!name.matches()) return;
            String[] label = recorded.getOrDefault(folder, new String[]{name.group(2), folder});
            installations.add(new InstallationFiles(Long.parseLong(name.group(1)), label[0].trim(),
                    folderOf(label[1].trim(), folder), files.deviceMappings, files.buttonMaps));
        });
        return installations;
    }

    /** The folder recorded for an installation - a label for a person - or its name in the zip when that is no path. */
    private static Path folderOf(String recorded, String inZip) {
        try {
            return Path.of(recorded);
        } catch (InvalidPathException e) {
            return Path.of(inZip);
        }
    }

    private static int byNumber(String a, String b) {
        int byNumber = Long.compare(numberOf(a), numberOf(b));
        return byNumber != 0 ? byNumber : a.compareTo(b);
    }

    private static long numberOf(String folder) {
        Matcher name = INSTALLATION_FOLDER.matcher(folder);
        return name.matches() ? Long.parseLong(name.group(1)) : Long.MAX_VALUE;
    }

    // ---- reading

    private static byte[] bytesOf(ZipFile file, ZipEntry entry, Budget budget) throws IOException {
        try (InputStream in = file.getInputStream(entry)) {
            byte[] content = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, budget.left() + 1));
            budget.spend(content.length);
            return content;
        }
    }

    /** What is left of {@link #MAX_BYTES} - counted as read, never from the sizes the zip claims. */
    private static final class Budget {
        private final Path zip;
        private long left;

        Budget(Path zip, long maxBytes) {
            this.zip = zip;
            this.left = maxBytes;
        }

        long left() {
            return left;
        }

        void spend(long bytes) throws NotABackupException {
            left -= bytes;
            if (left < 0) throw new NotABackupException(zip, "it holds more than a backup can");
        }
    }

    /** One installation's files as they are found in the zip. */
    private static final class Found {
        byte[] deviceMappings;
        final Map<String, byte[]> buttonMaps = new TreeMap<>();
    }
}
