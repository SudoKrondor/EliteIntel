package elite.intel.bindforge.install;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeSettingsManager;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.JournalFiles;
import elite.intel.session.DirectorySetting;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.GameFoldersChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The installation being played: the one whose bindings and journal folders Elite-Intel reads
 * (linux-several-installations.md, rules 2-3). On Linux each installation keeps its own in its Wine prefix; on Windows
 * every installation shares one, so there is never more than one choice there, and nothing to choose.
 */
public final class PlayedInstallation {

    private static final Logger log = LogManager.getLogger(PlayedInstallation.class);

    /** The form of a journal name's start stamp, which the time of a choice is stored in. */
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private PlayedInstallation() {
    }

    /**
     * One installation that can be played: its own bindings folder, and the journal folder beside it.
     *
     * @param storefront a label, as the stored list records it
     * @param root       the installation folder
     * @param userFolder the Windows user folder holding its bindings and journal folders
     */
    public record Choice(String storefront, Path root, GameUserFolder userFolder) {
        public Choice {
            Objects.requireNonNull(storefront, "storefront");
            Objects.requireNonNull(root, "root");
            Objects.requireNonNull(userFolder, "userFolder");
        }

        public Path bindingsFolder() {
            return userFolder.bindingsFolder();
        }

        public Path journalFolder() {
            return userFolder.journalFolder();
        }
    }

    /**
     * Every installation that can be played, one per bindings folder, in the stored list's order: detection runs and is
     * recorded, as every BindForge read does. Fewer than two means there is nothing to choose. Touches the disk and the
     * database - not for the EDT.
     */
    public static List<Choice> choices() {
        GameInstallationProvider provider =
                GameInstallationProvider.forThisPlatform(PlayerSession.getInstance().getBindingsDir());
        List<GameInstallation> detected = provider.findInstallations();
        return choicesOf(new InstallationRegistry(provider).rescan(detected), detected);
    }

    /**
     * The reachable rows whose own bindings folder is there and is the game's bindings folder of some user folder, once
     * per folder by real path - two routes to one folder are one choice.
     */
    static List<Choice> choicesOf(List<InstallationRow> rows, List<GameInstallation> detected) {
        Set<Path> seen = new HashSet<>();
        List<Choice> choices = new ArrayList<>();
        for (InstallationRow row : rows) {
            Path root = Path.of(row.rootPath());
            if (!GameInstallation.reachable(root, row.missing())) continue;
            Path own = InstallationRegistry.ownBindingsFolder(row, detected);
            if (own == null || !Files.isDirectory(own)) continue;
            Optional<GameUserFolder> user = GameUserFolder.holding(own);
            if (user.isEmpty() || !seen.add(realPath(own))) continue;
            choices.add(new Choice(row.storefront(), root, user.get()));
        }
        return List.copyOf(choices);
    }

    /** The choice whose bindings folder Elite-Intel is set to now, if any. Touches the disk. */
    public static Optional<Choice> current(List<Choice> choices) {
        return currentOf(choices, PlayerSession.getInstance().getBindingsDir());
    }

    static Optional<Choice> currentOf(List<Choice> choices, Path bindingsFolder) {
        Path setTo = realPath(bindingsFolder);
        return choices.stream().filter(choice -> realPath(choice.bindingsFolder()).equals(setTo)).findFirst();
    }

    /**
     * Makes {@code choice} the installation being played: Elite-Intel's bindings and journal folders become its own, the
     * time of the choice is stored, and {@link GameFoldersChangedEvent} tells what reads them. The user's pick and the
     * follower's alike (rule 2), one at a time ({@link #follow}). Touches the database.
     *
     * @return false when a folder is not one that can be stored - nothing changed, and the log says which
     */
    public static synchronized boolean choose(Choice choice) {
        // WHY: both checked before either is stored, so the two folders never point at different installations.
        Optional<String> journal = DirectorySetting.sanitize(choice.journalFolder().toString());
        Optional<String> bindings = DirectorySetting.sanitize(choice.bindingsFolder().toString());
        if (journal.isEmpty() || bindings.isEmpty()) {
            log.warn("Could not set the folders of {} ({}) - journal folder {}, bindings folder {}; the installation "
                    + "being played stays as it was", choice.storefront(), choice.root(), choice.journalFolder(),
                    choice.bindingsFolder());
            return false;
        }
        PlayerSession session = PlayerSession.getInstance();
        if (!session.setJournalPath(journal.get()) || !session.setBindingsDir(bindings.get())) {
            // WHY: both were just accepted by the same check, so a refusal now means the folder went in between -
            // a fault worth an error, and the pair may disagree until the next choice.
            log.error("A folder of {} ({}) was refused after it was checked: journal folder {}, bindings folder {}",
                    choice.storefront(), choice.root(), session.getJournalPath(), session.getBindingsDir());
            return false;
        }
        BindForgeSettingsManager.getInstance().setGameFoldersChosenAt(now());
        log.info("Playing {} ({}): bindings folder {}, journal folder {}", choice.storefront(), choice.root(),
                choice.bindingsFolder(), choice.journalFolder());
        UiBus.publish(new GameFoldersChangedEvent());
        return true;
    }

    /**
     * One look of the follower (rule 3): decides with {@link #toFollow} and chooses what it decides, under the same lock
     * as {@link #choose} - so a pick the user makes meanwhile is never undone by a decision taken before it.
     *
     * @return the choice made, if one was
     */
    public static synchronized Optional<Choice> follow(List<Choice> choices) {
        Optional<Choice> next = toFollow(newestJournals(choices), current(choices),
                BindForgeSettingsManager.getInstance().getGameFoldersChosenAt());
        return next.filter(PlayedInstallation::choose);
    }

    /**
     * The rule the follower applies (rule 3): the installation with the newest journal of all, when it is not the one
     * being played and its journal was opened after the current choice was made - the game started there. Empty
     * otherwise.
     *
     * @param newest   each choice's newest journal, by the start stamp in its name; a choice with none is left out
     * @param chosenAt when the current choice was made, {@code yyyyMMddHHmmss} local time, or 0 for never
     */
    static Optional<Choice> toFollow(Map<Choice, JournalFiles.StartStamp> newest, Optional<Choice> current,
                                     long chosenAt) {
        return newest.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(entry -> current.map(choice -> !choice.equals(entry.getKey())).orElse(true))
                .filter(entry -> entry.getValue().opened() > chosenAt)
                .map(Map.Entry::getKey);
    }

    /** Each choice's newest journal by the stamp in its name; one whose folder holds none, or cannot be listed, is left out. */
    static Map<Choice, JournalFiles.StartStamp> newestJournals(List<Choice> choices) {
        Map<Choice, JournalFiles.StartStamp> newest = new HashMap<>();
        for (Choice choice : choices) {
            if (!Files.isDirectory(choice.journalFolder())) continue;
            try {
                JournalFiles.newest(choice.journalFolder())
                        .flatMap(file -> JournalFiles.startStamp(file.getFileName().toString()))
                        .ifPresent(stamp -> newest.put(choice, stamp));
            } catch (IOException e) {
                // WHY: a transient the next look retries - one folder that cannot be listed now must not stop the
                // others being followed.
                log.debug("Could not list {} for its newest journal: {}", choice.journalFolder(), e.getMessage());
            }
        }
        return newest;
    }

    private static long now() {
        return Long.parseLong(LocalDateTime.now().format(STAMP));
    }

    private static Path realPath(Path folder) {
        try {
            return folder.toRealPath();
        } catch (IOException e) {
            return folder.toAbsolutePath().normalize();
        }
    }
}
