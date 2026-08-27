package elite.intel.ui.support;

import elite.intel.ai.hands.BindingsLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Collects everything needed to debug a commander's problem into one zip.
 * <p>
 * There is no telemetry and no server behind this app, so a bug report is
 * whatever the commander can be talked through attaching. Asking for five files
 * from three different directories - two of which the app chose for them and
 * they have no reason to know - loses most of the detail before it is ever
 * sent. One button that produces one file is the difference between a report
 * that can be diagnosed and one that cannot.
 * <p>
 * Every source is optional at collection time. A commander who never started
 * the game has no journal; one who has never opened the Controls screen has no
 * {@code .binds}; and the failure being reported may itself be the reason a
 * directory is unreadable. So a missing source is recorded and the bundle is
 * still written - a zip with three of four files diagnoses far more than an
 * abort with none. What was left out and why is written into the bundle itself
 * ({@link #INFO_ENTRY}), because the person opening the zip is not the person
 * who saw the message on screen, and "no journal in it" and "no journal at all"
 * are very different bug reports.
 */
public final class DiagnosticsBundle {

    private static final Logger log = LogManager.getLogger(DiagnosticsBundle.class);

    /**
     * Manifest naming the app version, the collection time, and anything that could not be collected.
     */
    public static final String INFO_ENTRY = "bundle-info.txt";

    /**
     * The in-app SYSTEM LOG transcript, which exists only in memory until now.
     */
    public static final String SESSION_LOG_ENTRY = "session.log";

    /**
     * The game's own live state files, which it keeps beside the journal and rewrites in place.
     * <p>
     * WHY they belong in a bundle: the journal is a record of what HAPPENED, and these are the only
     * statement of what IS - what is in the hold right now, what the market on the pad is selling, where the
     * plotted route goes. Half the questions a report raises are answered by comparing the two, and until
     * now every one of those had to be asked in a follow-up.
     * <p>
     * Absent files are ordinary rather than suspicious: {@code Backpack.json} and {@code ShipLocker.json}
     * belong to Odyssey, and the game writes {@code Market.json}, {@code Outfitting.json} and
     * {@code Shipyard.json} only once the commander has opened those screens.
     */
    static final List<String> GAME_STATE_FILES = List.of(
            "Backpack.json", "Cargo.json", "Market.json", "ModulesInfo.json", "NavRoute.json",
            "Outfitting.json", "ShipLocker.json", "Shipyard.json", "Status.json");

    /**
     * How much of any one file the bundle will carry, and so how much it will hold in memory at once.
     * <p>
     * 32 MB is well above a normal journal or a rolled 1 MB log, and well below the size at which reading one
     * whole would be felt on the heap. See {@link #readTail} for what happens to a file above it.
     */
    static final long MAX_SOURCE_BYTES = 32L * 1024 * 1024;

    private DiagnosticsBundle() {
    }

    /**
     * Where each part of the bundle comes from. Directories rather than files for the game's own data: which
     * journal and which {@code .binds} the app is actually reading is a question the commander cannot answer,
     * and getting it wrong is a large share of why reports are unusable.
     *
     * @param appVersion  reported in the manifest so a report names the build it came from
     * @param sessionLog  SYSTEM LOG transcript; blank when the commander cleared the panel
     * @param appLog      {@code logs/elite-intel.log}, resolved against the working directory; its newest
     *                    rolled sibling is taken as well, since the appender rolls on startup
     * @param journalDir  the configured journal directory; the newest {@code .log} in it is taken
     * @param bindingsDir the configured bindings directory; the active preset's file is taken
     */
    public record Sources(String appVersion, String sessionLog,
                          @Nullable Path appLog, @Nullable Path journalDir, @Nullable Path bindingsDir) {
    }

    /**
     * What the written bundle turned out to hold.
     * <p>
     * There is no "nothing was collected" case to report separately: the zip is created before any source is
     * read, so it always exists, and a bundle carrying only its manifest still tells whoever opens it which
     * paths this commander's app was looking at and why each one failed. That is a thin bug report rather
     * than no bug report, and calling it "not saved" would contradict the file on disk.
     *
     * @param included zip entry names actually written, never including the manifest
     * @param omitted  one human-readable line per source left out, with the reason
     */
    public record Result(List<String> included, List<String> omitted) {

        public Result {
            included = List.copyOf(included);
            omitted = List.copyOf(omitted);
        }
    }

    /**
     * Writes the bundle, collecting what it can.
     *
     * @throws IOException only if the zip itself cannot be written - a source that cannot be read is reported
     *                     in the {@link Result}, not thrown, since that is the case the bundle exists to capture
     */
    public static Result writeTo(Path zipFile, Sources sources) throws IOException {
        List<String> included = new ArrayList<>();
        List<String> omitted = new ArrayList<>();

        try (OutputStream out = Files.newOutputStream(zipFile);
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            if (sources.sessionLog() == null || sources.sessionLog().isBlank()) {
                omitted.add(SESSION_LOG_ENTRY + " - the in-app system log was empty");
            } else {
                writeEntry(zip, SESSION_LOG_ENTRY, sources.sessionLog().getBytes(StandardCharsets.UTF_8));
                included.add(SESSION_LOG_ENTRY);
            }

            copy(zip, "application log", appLog(sources.appLog()), included, omitted);
            copy(zip, "previous application log", previousAppLog(sources.appLog()), included, omitted);
            copy(zip, "journal", newestJournal(sources.journalDir()), included, omitted);
            copyGameState(zip, sources.journalDir(), included, omitted);
            copy(zip, "bindings", activeBindings(sources.bindingsDir()), included, omitted);

            writeEntry(zip, INFO_ENTRY, manifest(sources, included, omitted).getBytes(StandardCharsets.UTF_8));
        }
        return new Result(included, omitted);
    }

    /**
     * Adds one file, or records why it is absent. A source is a {@link Collected} rather than a {@code Path} so
     * that "there is no journal directory configured" and "the journal directory holds no files" stay distinct
     * in the report instead of collapsing into one null - and so that a source which is legitimately absent
     * ({@link Collected#absent()}) can stay silent rather than making a healthy bundle report as incomplete.
     */
    private static void copy(ZipOutputStream zip, String label, Collected source,
                             List<String> included, List<String> omitted) {
        if (source.path() == null) {
            if (!source.reason().isBlank()) omitted.add(label + " - " + source.reason());
            return;
        }
        String entryName = source.path().getFileName().toString();
        try {
            // Read fully before opening the entry: the app log is held open by log4j and the journal by the
            // game, so a read that fails must not leave a half-written entry behind in the zip.
            byte[] content = readTail(source.path(), MAX_SOURCE_BYTES);
            writeEntry(zip, entryName, content);
            included.add(entryName);
        } catch (IOException e) {
            log.warn("Diagnostics bundle: could not read {} ({}): {}", label, source.path(), e.toString());
            omitted.add(label + " - could not read " + source.path() + ": " + e);
        }
    }

    /**
     * Adds the game's live state files, and says in one line which of them the game had not written.
     * <p>
     * One line rather than nine: a Horizons commander who has never opened a shipyard is missing four of
     * these perfectly normally, and listing each would make a healthy bundle read as a broken one. Which
     * ones DID arrive is already answered by the manifest's included list.
     * <p>
     * Nothing is reported when there is no journal directory at all - the journal entry has already said so,
     * and saying it twice tells no one anything.
     * <p>
     * These are read exactly as they are found. The game rewrites {@code Status.json} several times a
     * second, so a bundle saved at the wrong instant can catch a half-written file; there is no lock to take
     * and a truncated snapshot still says more than no snapshot.
     */
    private static void copyGameState(ZipOutputStream zip, @Nullable Path journalDir,
                                      List<String> included, List<String> omitted) {
        if (journalDir == null || !Files.isDirectory(journalDir)) return;

        List<String> neverWritten = new ArrayList<>();
        for (String name : GAME_STATE_FILES) {
            Path file = journalDir.resolve(name);
            if (Files.isRegularFile(file)) {
                copy(zip, name, Collected.of(file), included, omitted);
            } else {
                neverWritten.add(name);
            }
        }
        if (!neverWritten.isEmpty()) {
            omitted.add("game state - not written by the game in " + journalDir + ": "
                    + String.join(", ", neverWritten));
        }
    }

    /**
     * The whole file, or its last {@code limit} bytes when it is larger than that.
     * <p>
     * A bound is needed because the bundle is collected at the worst possible moment - the commander is
     * already reporting a problem - and a session long enough to be worth reporting is exactly the one whose
     * journal has grown. Reading it whole is what keeps a failed read from leaving a half-written entry in
     * the zip, so the fix is to bound the read rather than to stream it.
     * <p>
     * The tail rather than the head, because every one of these files is append-only and the end is where
     * the failure is. The truncation is announced in the first line of the entry rather than in the manifest:
     * whoever opens the file needs to know they are looking at a fragment, and that is where they are
     * looking. The partial first line left by cutting mid-record is dropped with it.
     */
    static byte[] readTail(Path path, long limit) throws IOException {
        long size = Files.size(path);
        if (size <= limit) return Files.readAllBytes(path);

        byte[] buffer = new byte[(int) limit];
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            channel.position(size - limit);
            ByteBuffer target = ByteBuffer.wrap(buffer);
            while (target.hasRemaining() && channel.read(target) > 0) {
                // read until the buffer is full or the file ends under us
            }
        }
        int start = 0;
        while (start < buffer.length && buffer[start] != '\n') start++;   // drop the partial first record
        if (start < buffer.length) start++;

        String notice = String.format(
                "### Elite Intel: %s is %d MB; only its last %d MB are in this bundle ###%n",
                path.getFileName(), size / 1048576, limit / 1048576);
        byte[] head = notice.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[head.length + buffer.length - start];
        System.arraycopy(head, 0, content, 0, head.length);
        System.arraycopy(buffer, start, content, head.length, buffer.length - start);
        return content;
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private record Collected(@Nullable Path path, String reason) {
        static Collected of(Path path) {
            return new Collected(path, "");
        }

        /**
         * Not collected, and worth reporting: something the bundle wanted and could not get.
         */
        static Collected missing(String reason) {
            return new Collected(null, reason);
        }

        /**
         * Not collected, and that is normal - reported nowhere, so it cannot make a good bundle look broken.
         */
        static Collected absent() {
            return new Collected(null, "");
        }
    }

    private static Collected appLog(@Nullable Path appLog) {
        if (appLog == null) return Collected.missing("no application log path");
        if (!Files.isRegularFile(appLog)) return Collected.missing("not found at " + appLog.toAbsolutePath());
        return Collected.of(appLog);
    }

    /**
     * The newest rolled sibling of the application log, e.g. {@code elite-intel_2026-08-04_1.log}.
     * <p>
     * WHY this is worth a second file: the appender rolls on startup, so {@code elite-intel.log} only ever
     * holds the current session. A commander who hits a problem, restarts the app and then saves a bundle -
     * which is the ordinary sequence, since restarting is the first thing anyone tries - would otherwise send
     * a log that begins after the failure. The previous file is where the failure actually is.
     * <p>
     * Absent is not a failure here: on a first-ever run nothing has rolled yet, and a bundle carrying only
     * the current log is complete. Only an unreadable directory is reported.
     */
    private static Collected previousAppLog(@Nullable Path appLog) {
        if (appLog == null) return Collected.absent();
        Path dir = appLog.toAbsolutePath().getParent();
        if (dir == null || !Files.isDirectory(dir)) return Collected.absent();
        String stem = rolledPrefix(appLog);
        try (var files = Files.list(dir)) {
            return files
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(stem) && name.endsWith(".log");
                    })
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .map(Collected::of)
                    .orElseGet(Collected::absent);
        } catch (IOException e) {
            return Collected.missing("could not list " + dir + ": " + e);
        }
    }

    /**
     * {@code elite-intel.log} -> {@code elite-intel_}, the prefix log4j2's {@code filePattern} gives rolled files.
     */
    private static String rolledPrefix(Path appLog) {
        String name = appLog.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return (dot < 0 ? name : name.substring(0, dot)) + "_";
    }

    /**
     * The newest {@code .log} in the journal directory - the same choice {@code JournalParser} makes, so the
     * bundle carries the file the app was actually reading rather than one the commander picked by hand.
     */
    private static Collected newestJournal(@Nullable Path journalDir) {
        if (journalDir == null) return Collected.missing("no journal directory configured");
        if (!Files.isDirectory(journalDir)) return Collected.missing("not a directory: " + journalDir);
        try (var files = Files.list(journalDir)) {
            Optional<Path> newest = files
                    .filter(p -> p.toString().endsWith(".log"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
            return newest.map(Collected::of)
                    .orElseGet(() -> Collected.missing("no journal files in " + journalDir));
        } catch (IOException e) {
            return Collected.missing("could not list " + journalDir + ": " + e);
        }
    }

    /**
     * Delegates to {@link BindingsLoader} so the bundle carries the preset the app resolved, not a guess.
     */
    private static Collected activeBindings(@Nullable Path bindingsDir) {
        if (bindingsDir == null) return Collected.missing("no bindings directory configured");
        if (!Files.isDirectory(bindingsDir)) return Collected.missing("not a directory: " + bindingsDir);
        try {
            return Collected.of(new BindingsLoader().getLatestBindsFile(bindingsDir).toPath());
        } catch (IOException e) {
            return Collected.missing("could not resolve the active preset in " + bindingsDir + ": " + e);
        }
    }

    /**
     * The windowing system the app is actually running under, on the platforms that have more than one.
     * <p>
     * WHY this is worth four lines: an overlay that "moves about on its own" was eventually traced to a
     * compositor animating the window because of a hint the overlay set on itself - a bug that existed only
     * under one desktop, on one version of it, and could not be reproduced by anyone running anything else.
     * Establishing which desktop and which display server took several rounds of asking, and every one of
     * those rounds was a day. It is one line in a manifest.
     * <p>
     * Blank on Windows and macOS, where the answer is never in doubt and a line saying so is noise.
     */
    private static String desktop() {
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        String currentDesktop = System.getenv("XDG_CURRENT_DESKTOP");
        if (sessionType == null && currentDesktop == null) return "";

        // Both, when both are set: an app on a Wayland session still reaches X11 through Xwayland, and
        // "wayland session, X11 display :0" is a different situation from either half on its own.
        StringBuilder text = new StringBuilder("Desktop: ")
                .append(currentDesktop == null ? "unknown" : currentDesktop)
                .append(" (").append(sessionType == null ? "unknown" : sessionType).append(')');
        String x11 = System.getenv("DISPLAY");
        String wayland = System.getenv("WAYLAND_DISPLAY");
        if (x11 != null) text.append("  X11 display ").append(x11);
        if (wayland != null) text.append("  Wayland display ").append(wayland);
        return text.append('\n').toString();
    }

    /**
     * Every monitor, with its position in the desktop and its scale factor.
     * <p>
     * WHY: the HUD overlay is placed in coordinates that span all displays, so half of what can go wrong with
     * it is a property of the layout rather than of the app - a card that opens on the seam between two
     * screens, a gap above a shorter monitor that belongs to no display at all, a scale factor that makes
     * every coordinate fractional. None of that is visible in a log, and asking a commander to describe
     * their monitor arrangement gets prose rather than numbers.
     * <p>
     * Reported for every commander, not only the ones with two screens, because "one monitor" is itself the
     * answer to the first question asked of any layout-shaped report.
     */
    private static String displays() {
        try {
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            StringBuilder text = new StringBuilder("Displays: ").append(screens.length).append('\n');
            for (GraphicsDevice screen : screens) {
                GraphicsConfiguration config = screen.getDefaultConfiguration();
                Rectangle bounds = config.getBounds();
                AffineTransform scale = config.getDefaultTransform();
                text.append(String.format("  %-14s %dx%d at %+d%+d  scale %.2gx%.2g%n",
                        screen.getIDstring(), bounds.width, bounds.height, bounds.x, bounds.y,
                        scale.getScaleX(), scale.getScaleY()));
            }
            return text.toString();
        } catch (RuntimeException headlessOrDriverFailure) {
            // Never fatal: this is the one part of the manifest that asks the graphics stack a question, and
            // a bundle is most wanted precisely when that stack is the thing misbehaving.
            return "Displays: could not be queried (" + headlessOrDriverFailure + ")\n";
        }
    }

    private static String manifest(Sources sources, List<String> included, List<String> omitted) {
        StringBuilder text = new StringBuilder()
                .append("Elite Intel diagnostics bundle\n")
                .append("Created: ").append(java.time.ZonedDateTime.now()).append('\n')
                .append("App version: ").append(sources.appVersion()).append('\n')
                .append("OS: ").append(System.getProperty("os.name"))
                .append(' ').append(System.getProperty("os.version"))
                .append(" (").append(System.getProperty("os.arch")).append(")\n")
                .append("Java: ").append(System.getProperty("java.version")).append('\n')
                .append("Locale: ").append(java.util.Locale.getDefault())
                .append("  Timezone: ").append(java.time.ZoneId.systemDefault()).append('\n')
                .append(desktop())
                .append(displays())
                .append("\nIncluded:\n");
        if (included.isEmpty()) {
            text.append("  (nothing)\n");
        } else {
            included.forEach(name -> text.append("  ").append(name).append('\n'));
        }
        if (!omitted.isEmpty()) {
            text.append("\nNot included:\n");
            omitted.forEach(line -> text.append("  ").append(line).append('\n'));
        }
        // Stated rather than left to inference: no rolled log usually means a first-ever session, but from
        // the outside it is indistinguishable from the bundle having failed to pick one up.
        if (sources.appLog() != null && included.stream().noneMatch(n -> n.startsWith(rolledPrefix(sources.appLog())))) {
            text.append("\nNo previous (rolled) application log existed - the app logs only this session.\n");
        }
        return text.toString();
    }
}
