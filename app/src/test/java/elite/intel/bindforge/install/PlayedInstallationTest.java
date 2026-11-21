package elite.intel.bindforge.install;

import com.google.common.eventbus.Subscribe;
import elite.intel.bindforge.install.PlayedInstallation.Choice;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.dao.PlayerDao;
import elite.intel.db.managers.BindForgeSettingsManager;
import elite.intel.db.util.Database;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.JournalFiles;
import elite.intel.session.PlayerSession;
import elite.intel.ui.event.GameFoldersChangedEvent;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The installations that can be played, and the rule that follows the game between them
 * (linux-several-installations.md, rules 2-3).
 */
class PlayedInstallationTest {

    private static final long YESTERDAY = 20261120180000L;

    @TempDir
    Path temp;

    private Choice steam;
    private Choice epic;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    // WHY: the suite shares one in-memory database - the folders and the time of the choice go back as they were.
    @AfterEach
    void forgetTheChoice() {
        storeFolders(null, null);
        BindForgeSettingsManager.getInstance().setGameFoldersChosenAt(0);
    }

    @BeforeEach
    void twoInstallations() {
        steam = new Choice("STEAM", temp.resolve("steam"), new GameUserFolder(temp.resolve("steam-pfx/users/steamuser")));
        epic = new Choice("EPIC", temp.resolve("epic"), new GameUserFolder(temp.resolve("epic-pfx/users/krondor")));
    }

    @Test
    void theGameStartingInAnotherInstallationAfterTheChoiceIsFollowed() {
        Map<Choice, JournalFiles.StartStamp> newest = Map.of(steam, stamp(20261120190000L), epic, stamp(20261121090000L));

        assertEquals(Optional.of(epic), PlayedInstallation.toFollow(newest, Optional.of(steam), YESTERDAY));
    }

    @Test
    void theInstallationAlreadyPlayedIsNotChosenAgain() {
        Map<Choice, JournalFiles.StartStamp> newest = Map.of(steam, stamp(20261121090000L), epic, stamp(20261120190000L));

        assertEquals(Optional.empty(), PlayedInstallation.toFollow(newest, Optional.of(steam), YESTERDAY));
    }

    @Test
    void aJournalOlderThanTheUsersPickDoesNotUndoIt() {
        Map<Choice, JournalFiles.StartStamp> newest = Map.of(steam, stamp(20261120170000L), epic, stamp(20261120120000L));

        assertEquals(Optional.empty(), PlayedInstallation.toFollow(newest, Optional.of(epic), YESTERDAY),
                "the app restarted with the game off keeps the installation the user picked");
    }

    @Test
    void neverChosenTakesTheNewestJournal() {
        Map<Choice, JournalFiles.StartStamp> newest = Map.of(steam, stamp(20250101000000L), epic, stamp(20240101000000L));

        assertEquals(Optional.of(steam), PlayedInstallation.toFollow(newest, Optional.empty(), 0));
    }

    @Test
    void noJournalAnywhereFollowsNothing() {
        assertEquals(Optional.empty(), PlayedInstallation.toFollow(Map.of(), Optional.of(steam), 0));
    }

    @Test
    void eachInstallationsNewestJournalIsReadFromItsNameAndAFolderWithNoneIsLeftOut() throws IOException {
        Files.createDirectories(steam.journalFolder());
        Files.createFile(steam.journalFolder().resolve("Journal.2026-11-20T190000.01.log"));
        Files.createFile(steam.journalFolder().resolve("Journal.2026-11-21T080000.01.log"));
        Files.createFile(steam.journalFolder().resolve("Status.json"));

        Map<Choice, JournalFiles.StartStamp> newest = PlayedInstallation.newestJournals(List.of(steam, epic));

        assertEquals(Map.of(steam, stamp(20261121080000L)), newest);
    }

    @Test
    void theChoicesAreTheReachableInstallationsWithABindingsFolderOfTheirOwnOncePerFolder() throws IOException {
        Path steamRoot = installation("steam");
        Path secondRoute = installation("steam-again");
        Path frontierRoot = installation("frontier");
        Path gone = temp.resolve("gone");
        GameUserFolder steamUser = new GameUserFolder(temp.resolve("pfx/drive_c/users/steamuser"));
        Files.createDirectories(steamUser.bindingsFolder());
        List<GameInstallation> detected = List.of(
                detected(steamRoot, steamUser.bindingsFolder()),
                detected(secondRoute, steamUser.bindingsFolder()),
                detected(gone, steamUser.bindingsFolder()));
        List<InstallationRow> rows = List.of(row(1, "STEAM", steamRoot), row(2, "STEAM", secondRoute),
                row(3, "FRONTIER", frontierRoot), row(4, "STEAM", gone));

        List<Choice> choices = PlayedInstallation.choicesOf(rows, detected);

        assertEquals(List.of(new Choice("STEAM", steamRoot, steamUser)), choices,
                "two routes to one folder are one choice; no folder of its own, or no installation, is none");
    }

    @Test
    void theCurrentChoiceIsTheOneTheBindingsFolderIsSetTo() {
        assertEquals(Optional.of(epic), PlayedInstallation.currentOf(List.of(steam, epic), epic.bindingsFolder()));
        assertEquals(Optional.empty(), PlayedInstallation.currentOf(List.of(steam, epic), temp.resolve("elsewhere")));
    }

    @Test
    void choosingSetsBothFoldersStampsTheTimeAndTellsWhatReadsThem() throws IOException {
        Files.createDirectories(epic.bindingsFolder());
        Files.createDirectories(epic.journalFolder());
        AtomicInteger told = new AtomicInteger();
        Object listener = new Object() {
            @Subscribe
            public void onGameFoldersChanged(GameFoldersChangedEvent event) {
                told.incrementAndGet();
            }
        };
        UiBus.register(listener);
        try {
            assertTrue(PlayedInstallation.choose(epic));
        } finally {
            UiBus.unregister(listener);
        }

        assertEquals(epic.bindingsFolder().toRealPath(), PlayerSession.getInstance().getBindingsDir().toRealPath());
        assertEquals(epic.journalFolder().toRealPath(), PlayerSession.getInstance().getJournalPath().toRealPath());
        assertTrue(BindForgeSettingsManager.getInstance().getGameFoldersChosenAt() > YESTERDAY,
                "stored in the journal stamp's form, so a journal opened later wins");
        assertEquals(1, told.get());
    }

    @Test
    void aFolderThatCannotBeSetChangesNeitherFolder() throws IOException {
        Files.createDirectories(steam.bindingsFolder());
        Files.createDirectories(steam.journalFolder());
        assertTrue(PlayedInstallation.choose(steam));
        long chosenAt = BindForgeSettingsManager.getInstance().getGameFoldersChosenAt();
        Files.createDirectories(epic.journalFolder());

        assertFalse(PlayedInstallation.choose(epic), "its bindings folder is not there");

        assertEquals(steam.bindingsFolder().toRealPath(), PlayerSession.getInstance().getBindingsDir().toRealPath());
        assertEquals(steam.journalFolder().toRealPath(), PlayerSession.getInstance().getJournalPath().toRealPath(),
                "the two folders never point at different installations");
        assertEquals(chosenAt, BindForgeSettingsManager.getInstance().getGameFoldersChosenAt());
    }

    private static void storeFolders(String journal, String bindings) {
        Database.withDao(PlayerDao.class, dao -> {
            PlayerDao.Player player = dao.get();
            player.setJournalDirectory(journal);
            player.setBindingsDirectory(bindings);
            dao.save(player);
            return null;
        });
    }

    private Path installation(String name) throws IOException {
        Path root = temp.resolve(name);
        Files.createDirectories(GameInstallation.controlSchemesUnder(root));
        return root;
    }

    private static GameInstallation detected(Path root, Path bindings) {
        return new GameInstallation(Storefront.STEAM, root, GameInstallation.controlSchemesUnder(root), bindings);
    }

    private static InstallationRow row(long id, String storefront, Path root) {
        return new InstallationRow(id, storefront, root.toString(), false, false);
    }

    private static JournalFiles.StartStamp stamp(long opened) {
        return new JournalFiles.StartStamp(opened, 1);
    }
}
