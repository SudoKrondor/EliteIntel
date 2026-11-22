package elite.intel.bindforge.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** The game's two folders hang off one Windows user folder (linux-several-installations.md, rule 1). */
class GameUserFolderTest {

    @TempDir
    Path temp;

    @Test
    void theJournalFolderIsFoundFromTheBindingsFolder() {
        GameUserFolder user = new GameUserFolder(temp.resolve("pfx/drive_c/users/steamuser"));

        GameUserFolder found = GameUserFolder.holding(user.bindingsFolder()).orElseThrow();

        assertEquals(user, found);
        assertEquals(temp.resolve("pfx/drive_c/users/steamuser/Saved Games/Frontier Developments/Elite Dangerous"),
                found.journalFolder());
    }

    @Test
    void aFolderThatIsNotTheGamesBindingsFolderHasNoUserFolder() {
        assertEquals(Optional.empty(), GameUserFolder.holding(temp.resolve("my own bindings")));
        assertEquals(Optional.empty(), GameUserFolder.holding(temp.resolve("Options/Bindings")));
    }

    @Test
    void thePrefixUserThatHoldsTheGamesFoldersIsTaken() throws IOException {
        Path prefix = temp.resolve("Heroic/Prefixes/EliteDangerous");
        Files.createDirectories(prefix.resolve("drive_c/users/Public"));
        GameUserFolder player = new GameUserFolder(prefix.resolve("drive_c/users/krondor"));
        Files.createDirectories(player.journalFolder());

        assertEquals(Optional.of(player), GameUserFolder.inPrefix(prefix));
        assertTrue(GameUserFolder.isWinePrefix(prefix));
    }

    @Test
    void aPrefixTheGameHasNotRunInYetHoldsNoUserFolder() throws IOException {
        Path prefix = temp.resolve("prefix");
        Files.createDirectories(prefix.resolve("drive_c/users/krondor"));

        assertEquals(Optional.empty(), GameUserFolder.inPrefix(prefix));
        assertEquals(Optional.empty(), GameUserFolder.inPrefix(temp.resolve("not a prefix")));
        assertFalse(GameUserFolder.isWinePrefix(temp.resolve("not a prefix")));
    }
}
