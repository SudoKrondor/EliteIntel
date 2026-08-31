package elite.intel.ai.brain.actions.handlers.commands.builtin;

import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.commands.IntelCommand;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.db.util.Database;
import elite.intel.i18n.Language;
import elite.intel.session.Status;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The voice side of the jukebox: six commands, no more.
 *
 * <p>Six is a deliberate ceiling. The reflex resolver dispatches only when one action beats the next by a
 * clear margin, so a family of near-identical commands defeats it and sends every utterance to the local
 * model - the failure that turned ten carrier queries into three. Each of these says something the others
 * do not.
 */
class JukeboxCommandsTest {

    private static final Set<String> JUKEBOX_COMMANDS = Set.of(
            PlayMusicCommand.ID,
            PauseMusicCommand.ID,
            NextMusicTrackCommand.ID,
            PreviousMusicTrackCommand.ID,
            ShuffleMusicCommand.ID,
            PlayMusicTrackByNameCommand.ID);

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
        CommandRegistry.getInstance().load();
    }

    @BeforeEach
    void emptyLibrary() {
        JukeboxManager.getInstance().clear();
    }

    @Test
    void everyJukeboxCommandIsRegistered() {
        for (String id : JUKEBOX_COMMANDS) {
            assertTrue(CommandRegistry.getInstance().find(id).isPresent(), id + " is not registered");
        }
    }

    @Test
    void everyJukeboxCommandExplainsItselfToTheModel() {
        for (String id : JUKEBOX_COMMANDS) {
            String description = command(id).llmDescription();
            assertFalse(description.isBlank(), id + " has no description for the model");
            assertTrue(description.toLowerCase(Locale.ROOT).contains("music")
                            || description.toLowerCase(Locale.ROOT).contains("track"),
                    id + " never says it is about music, so the model cannot tell it from a ship command");
        }
    }

    @ParameterizedTest(name = "{0} can work the jukebox by voice")
    @EnumSource(Language.class)
    void everyJukeboxCommandHasPhrasesInEveryLanguage(Language language) {
        elite.intel.session.SystemSession.getInstance().setLanguage(language);
        try {
            for (String id : JUKEBOX_COMMANDS) {
                List<String> phrases = AiActionLocalizations.phrasesForAction(id);
                assertFalse(phrases.isEmpty(),
                        id + " cannot be spoken in " + language + " - it has no alias phrases");
            }
        } finally {
            elite.intel.session.SystemSession.getInstance().setLanguage(Language.EN);
        }
    }

    /**
     * These stay offered even with an empty playlist.
     * <p>
     * The tempting alternative - hiding them until a folder is chosen - makes the companion answer "play
     * music" with a claim that it has no such function, which is untrue and leaves the commander no wiser.
     * Answering "there is no music in your playlist yet" names the actual problem. It also keeps the set of
     * offered tools independent of the library's contents, which a frozen composition snapshot depends on.
     */
    @Test
    void theCommandsAreOfferedEvenBeforeAnyMusicIsLoaded() {
        Status status = Status.getInstance();
        assertTrue(JukeboxManager.getInstance().playlist().isEmpty(), "this test needs an empty library");

        for (String id : JUKEBOX_COMMANDS) {
            assertTrue(command(id).isVisibleForLLM(status),
                    id + " vanishes when the playlist is empty, so the companion would deny it exists");
        }
    }

    @Test
    void askingForMusicWithAnEmptyPlaylistSaysSoRatherThanFailingSilently() {
        assertTrue(JukeboxManager.getInstance().playlist().isEmpty(), "this test needs an empty library");

        for (String id : List.of(PlayMusicCommand.ID, NextMusicTrackCommand.ID,
                PreviousMusicTrackCommand.ID, PlayMusicTrackByNameCommand.ID)) {
            com.google.gson.JsonObject params = new com.google.gson.JsonObject();
            params.addProperty("key", "anything");
            String spoken = command(id).execute(params, "");
            assertNotNull(spoken, id + " said nothing at all with an empty playlist");
            assertFalse(spoken.isBlank(), id + " said nothing at all with an empty playlist");
        }
    }

    private static IntelCommand command(String id) {
        return CommandRegistry.getInstance().find(id)
                .orElseThrow(() -> new AssertionError(id + " is not registered"));
    }

    @Test
    void shuffleCanBeSwitchedBothWaysRatherThanOnlyToggled() {
        // A toggle would mean "whatever it is not right now", which is only right by luck when the
        // commander cannot see the setting. BooleanToggleReflexTest guards the phrasing in every locale.
        IntelCommand shuffle = command(ShuffleMusicCommand.ID);
        assertTrue(shuffle.parameters().stream().anyMatch(p -> p.getName().equals("state")),
                "shuffle has to be told which way to go");
    }

    @Test
    void namingATrackIsARequiredArgumentSoItNeverStealsPlainPlayMusic() {
        IntelCommand byName = command(PlayMusicTrackByNameCommand.ID);
        assertTrue(byName.parameters().stream().anyMatch(p -> p.getName().equals("key") && p.isRequired()),
                "without a required title this would compete with play_music for a bare phrase");
    }
}
