package elite.intel.db.util;

import elite.intel.ai.mouth.TransmissionAudio;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class TransmissionAudioSettingsTest {
    @AfterEach
    void reset() {
        SystemSession settings = SystemSession.getInstance();
        settings.setTransmissionTones(false);
        settings.setEnhancedRadioEffect(false);
        settings.setEffectsOnRadio(true);
        settings.setEffectsOnVegaAway(false);
        settings.setTransmissionToneVolume(35);
        settings.setSupertonicBoostPercent(0);
    }

    @Test
    void migrationDefaultsToEnhancedRadioOnly() throws Exception {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY)");
            handle.execute("INSERT INTO game_session (id) VALUES (1)");
            try (InputStream resource = getClass().getResourceAsStream("/db-migration/11003__transmission_audio_options.sql")) {
                assertNotNull(resource);
                for (String sql : new String(resource.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
                    if (!sql.isBlank()) handle.execute(sql);
                }
            }
            assertEquals(0, handle.createQuery("SELECT transmissionTones + effectsOnVegaAway + supertonicBoost FROM game_session").mapTo(Integer.class).one());
            assertEquals(0, handle.createQuery("SELECT effectsOnRadio FROM game_session").mapTo(Integer.class).one());
            assertEquals(0, handle.createQuery("SELECT enhancedRadioEffect FROM game_session").mapTo(Integer.class).one());
        }
    }

    @Test
    void optionsSurviveAnUnrelatedSettingsSave() {
        SystemSession settings = SystemSession.getInstance();
        settings.setTransmissionTones(true);
        settings.setEnhancedRadioEffect(true);
        settings.setEffectsOnRadio(false);
        settings.setEffectsOnVegaAway(true);
        settings.setTransmissionToneVolume(24);
        settings.setSupertonicBoostPercent(100);
        int volume = settings.getRadioVolume();
        settings.setRadioVolume(volume);
        assertTrue(settings.isTransmissionTones());
        assertTrue(settings.isEnhancedRadioEffect());
        assertFalse(settings.isEffectsOnRadio());
        assertTrue(settings.isEffectsOnVegaAway());
        assertEquals(24, settings.getTransmissionToneVolume());
        assertEquals(100, settings.getSupertonicBoostPercent());
    }

    @Test
    void tuningMigrationRetainsPreviousSupertonicChoice() throws Exception {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY, supertonicBoost INTEGER NOT NULL)");
            handle.execute("INSERT INTO game_session (id, supertonicBoost) VALUES (1, 1)");
            handle.execute("INSERT INTO game_session (id, supertonicBoost) VALUES (2, 0)");
            try (InputStream resource = getClass().getResourceAsStream("/db-migration/11004__audio_tuning_levels.sql")) {
                assertNotNull(resource);
                for (String sql : new String(resource.readAllBytes(), StandardCharsets.UTF_8)
                        .replaceAll("(?m)^--.*$", "").split(";")) {
                    if (!sql.isBlank()) handle.execute(sql);
                }
            }
            assertEquals(20, handle.createQuery("SELECT supertonicBoostPercent FROM game_session WHERE id = 1").mapTo(Integer.class).one());
            assertEquals(0, handle.createQuery("SELECT supertonicBoostPercent FROM game_session WHERE id = 2").mapTo(Integer.class).one());
            assertEquals(35, handle.createQuery("SELECT transmissionToneVolume FROM game_session WHERE id = 1").mapTo(Integer.class).one());
        }
    }

    @Test
    void optionalTreatmentFollowsRequestOriginAndPlayerLocation() {
        SystemSession settings = SystemSession.getInstance();
        Status status = Status.getInstance();
        GameEvents.StatusEvent originalStatus = status.getStatus();
        try {
            settings.setTransmissionTones(true);
            settings.setEnhancedRadioEffect(true);
            settings.setEffectsOnRadio(true);
            settings.setEffectsOnVegaAway(true);
            VocalisationRequestEvent radio = new VocalisationRequestEvent(
                    "radio", null, AiVoxResponseEvent.class, true, true, null);
            VocalisationRequestEvent ordinary = new VocalisationRequestEvent(
                    "ordinary", AiVoxResponseEvent.class, true);
            VocalisationRequestEvent vega = VocalisationRequestEvent.trackedVega(
                    "vega", "vega", AiVoxResponseEvent.class, true, new CompletableFuture<>());

            GameEvents.StatusEvent location = new GameEvents.StatusEvent();
            status.setStatus(location);
            assertEquals(new TransmissionAudio.Options(true, true), TransmissionAudio.forRequest(radio));
            assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(ordinary));
            assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(vega));

            location.setFlags2(1L); // On foot.
            status.setStatus(location);
            assertEquals(new TransmissionAudio.Options(true, true), TransmissionAudio.forRequest(vega));
            assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(ordinary));

            location.setFlags2(0);
            location.setFlags(1L << 26); // In an SRV.
            status.setStatus(location);
            assertEquals(new TransmissionAudio.Options(true, true), TransmissionAudio.forRequest(vega));

            settings.setEffectsOnRadio(false);
            assertEquals(TransmissionAudio.Options.NONE, TransmissionAudio.forRequest(radio));
        } finally {
            status.setStatus(originalStatus);
        }
    }
}
