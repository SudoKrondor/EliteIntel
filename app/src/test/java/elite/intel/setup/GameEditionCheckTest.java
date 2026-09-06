package elite.intel.setup;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The app is built for Odyssey; on Horizons the on-foot half of it simply does not exist. The journal header
 * is the only place that says which one is running, so these tests pin what is read out of it and when the
 * commander is interrupted over it.
 * <p>
 * The header lines below are verbatim journal output, Odyssey and Horizons alike.
 */
class GameEditionCheckTest {

    private static final String ODYSSEY_HEADER = "{ \"timestamp\":\"2026-09-06T15:52:14Z\", \"event\":\"Fileheader\","
            + " \"part\":1, \"language\":\"English/UK\", \"Odyssey\":true, \"gameversion\":\"4.4.1.1\","
            + " \"build\":\"r332841/r0 \" }";
    private static final String HORIZONS_HEADER = "{ \"timestamp\":\"2026-09-06T15:52:14Z\", \"event\":\"Fileheader\","
            + " \"part\":1, \"language\":\"English/UK\", \"Odyssey\":false, \"gameversion\":\"4.4.1.1\","
            + " \"build\":\"r332841/r0 \" }";

    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    // ── reading the flag ─────────────────────────────────────────────────────

    @Test
    void theHeaderStatesTheEdition() {
        assertEquals(Optional.of(true), GameEditionCheck.odysseyFlag(ODYSSEY_HEADER));
        assertEquals(Optional.of(false), GameEditionCheck.odysseyFlag(HORIZONS_HEADER));
    }

    /**
     * The game writes a byte-order mark, and the live parser strips it before parsing. This reads the same
     * bytes off disk itself, so it has to strip it too.
     */
    @Test
    void aByteOrderMarkDoesNotHideTheEdition() {
        assertEquals(Optional.of(true), GameEditionCheck.odysseyFlag("﻿" + ODYSSEY_HEADER));
    }

    /**
     * Unknown is not Horizons. Nothing here is evidence of the edition, and a warning that fires on a
     * truncated or unrecognised line would be a false alarm on a perfectly good Odyssey install.
     */
    @Test
    void anythingThatDoesNotStateTheFlagIsUnknown() {
        assertEquals(Optional.empty(), GameEditionCheck.odysseyFlag(null));
        assertEquals(Optional.empty(), GameEditionCheck.odysseyFlag(""));
        assertEquals(Optional.empty(), GameEditionCheck.odysseyFlag("not json at all"));
        assertEquals(Optional.empty(), GameEditionCheck.odysseyFlag("{ \"event\":\"Fileheader\", "), "truncated");
        assertEquals(Optional.empty(),
                GameEditionCheck.odysseyFlag("{ \"timestamp\":\"2026-09-06T15:52:14Z\", \"event\":\"Fileheader\","
                        + " \"part\":1, \"language\":\"English/UK\", \"gameversion\":\"3.7.0.0\" }"),
                "a header from before the flag existed");
        assertEquals(Optional.empty(),
                GameEditionCheck.odysseyFlag("{ \"timestamp\":\"2026-09-06T15:58:22Z\", \"event\":\"Commander\","
                        + " \"FID\":\"F1\", \"Name\":\"JAMESON\" }"),
                "some other event on the first line");
    }

    // ── what the commander hears ─────────────────────────────────────────────

    @Test
    void aHorizonsSessionIsWarnedAbout(@TempDir Path dir) throws IOException {
        journal(dir, "Journal.2026-09-06T085218.01.log", HORIZONS_HEADER);

        assertEquals(List.of(text("speech.notOdyssey")), capture(() -> checkIn(dir).check()));
    }

    @Test
    void anOdysseySessionIsLeftAlone(@TempDir Path dir) throws IOException {
        journal(dir, "Journal.2026-09-06T085218.01.log", ODYSSEY_HEADER);

        assertTrue(capture(() -> checkIn(dir).check()).isEmpty());
    }

    /**
     * No journal to read is not evidence of Horizons - and a commander whose journal folder is wrong is told
     * so by {@link SetupCheck}, which can say something useful about it.
     */
    @Test
    void nothingToReadSaysNothing(@TempDir Path dir) {
        assertTrue(capture(() -> checkIn(dir).check()).isEmpty(), "empty folder");
        assertTrue(capture(() -> checkIn(dir.resolve("nope")).check()).isEmpty(), "missing folder");
        assertTrue(capture(() -> checkIn(null).check()).isEmpty(), "no folder configured");
    }

    /**
     * The same file the live parser follows: the newest one. A commander who played Horizons once must not be
     * warned about the Odyssey session they are in now.
     */
    @Test
    void theNewestJournalIsTheOneThatCounts(@TempDir Path dir) throws IOException {
        Path old = journal(dir, "Journal.2026-09-01T101010.01.log", HORIZONS_HEADER);
        Path current = journal(dir, "Journal.2026-09-06T085218.01.log", ODYSSEY_HEADER);
        Files.setLastModifiedTime(old, FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(current, FileTime.fromMillis(2_000_000));

        assertTrue(capture(() -> checkIn(dir).check()).isEmpty());
    }

    /**
     * Said once, not on every service restart. It is the same broken situation, and the commander already
     * heard it.
     */
    @Test
    void theWarningDoesNotNag(@TempDir Path dir) throws IOException {
        journal(dir, "Journal.2026-09-06T085218.01.log", HORIZONS_HEADER);
        GameEditionCheck check = checkIn(dir);

        assertEquals(1, capture(check::check).size());
        assertTrue(capture(check::check).isEmpty(), "second start must not repeat it");
    }

    /**
     * An Odyssey session re-arms it: the commander has moved between editions before, and may again.
     */
    @Test
    void anOdysseySessionRearmsTheWarning(@TempDir Path dir) throws IOException {
        journal(dir, "Journal.2026-09-06T085218.01.log", HORIZONS_HEADER);
        GameEditionCheck check = checkIn(dir);

        assertEquals(1, capture(check::check).size());
        check.onGameSessionStarted(true);

        assertEquals(1, capture(check::check).size(), "a later Horizons session has to be warned about again");
    }

    /**
     * The live path, for a game launched after the app: the header arrives on the bus instead of off disk.
     */
    @Test
    void aGameLaunchedAfterTheAppIsJudgedFromTheBus(@TempDir Path emptyDir) {
        GameEditionCheck check = checkIn(emptyDir);

        assertEquals(List.of(text("speech.notOdyssey")), capture(() -> check.onGameSessionStarted(false)));
        assertTrue(capture(() -> check.onGameSessionStarted(true)).isEmpty());
    }

    @Test
    void theWarningIsSpokenUninterruptibly(@TempDir Path dir) throws IOException {
        journal(dir, "Journal.2026-09-06T085218.01.log", HORIZONS_HEADER);

        List<VocalisationRequestEvent> events = captureEvents(() -> checkIn(dir).check());

        assertFalse(events.isEmpty());
        events.forEach(event -> assertFalse(event.canBeInterrupted()));
    }

    // ── the commander has to be able to understand it ────────────────────────

    @Test
    void theWarningIsTranslatedInEveryLanguage() {
        String english = MultiLingualTextProvider.getText(Language.EN, "speech.notOdyssey");
        assertFalse(english.isBlank());
        assertNotEquals("speech.notOdyssey", english, "missing from the base bundle");

        for (Language language : Language.values()) {
            String text = MultiLingualTextProvider.getText(language, "speech.notOdyssey");
            assertNotEquals("speech.notOdyssey", text, language + " has no speech.notOdyssey");
            assertFalse(text.isBlank(), language.toString());
            assertTrue(text.contains("Odyssey"), language + " does not name the edition it needs");
            if (language != Language.EN) {
                assertNotEquals(english, text, language + " left it in English");
            }
        }
    }

    private static GameEditionCheck checkIn(Path journalDir) {
        return new GameEditionCheck(() -> journalDir);
    }

    private static Path journal(Path dir, String name, String header) throws IOException {
        return Files.writeString(dir.resolve(name), header + "\n");
    }

    private static String text(String key) {
        return MultiLingualTextProvider.getText(Language.EN, key);
    }

    private static List<String> capture(Runnable action) {
        return captureEvents(action).stream().map(VocalisationRequestEvent::getText).toList();
    }

    private static List<VocalisationRequestEvent> captureEvents(Runnable action) {
        List<VocalisationRequestEvent> heard = new ArrayList<>();
        Object listener = new Object() {
            @Subscribe
            public void onVocalisation(VocalisationRequestEvent event) {
                heard.add(event);
            }
        };
        GameEventBus.register(listener);
        try {
            action.run();
        } finally {
            GameEventBus.unregister(listener);
        }
        return heard;
    }
}
