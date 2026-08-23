package elite.intel.session;

import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.VoiceGender;
import elite.intel.ai.mouth.edge.EdgeVoices;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Voice and personality are per-ship: each ship carries a single voice ({@code ship.voice}) and one
 * personality ({@code ship.personality}). {@link SystemSession} resolves both against the active ship
 * (the one named by the current loadout). A ship voice is the commander's pick, male or female, so the
 * getters resolve the stored name against the active TTS provider's enum and fall back to that provider's
 * default only when the stored name is not one of that provider's voices - which is what a TTS provider
 * switch leaves behind, since each engine names its voices differently.
 * <p>
 * {@link SystemSession#getVoiceGender()} reads the same per-ship voice, because the voice is also what tells
 * the companion prompt whether to speak of itself in feminine or masculine forms.
 */
class PerShipVoicePersonalityTest {

    /**
     * Makes {@code shipId} the active ship by seeding the current loadout.
     */
    private void makeActive(int shipId) {
        ShipLoadOutDto dto = new ShipLoadOutDto();
        dto.setShipId(shipId);
        ShipLoadoutManager.getInstance().clear();
        ShipLoadoutManager.getInstance().save(dto);
    }

    private ShipDao.Ship saveShip(int shipId, String voice, String personality) {
        ShipDao.Ship ship = new ShipDao.Ship();
        ship.setShipId(shipId);
        ship.setShipName("Test Ship " + shipId);
        ship.setShipIdentifier("cobramkiii");
        ship.setCargoCapacity(0);
        ship.setCommanderName("CMDR Test");
        ship.setVoice(voice);
        ship.setPersonality(personality);
        ShipManager.getInstance().saveShip(ship);
        return ship;
    }

    @Test
    void voiceAndPersonalityFollowTheActiveShip() {
        SystemSession session = SystemSession.getInstance();

        saveShip(101, KokoroVoices.NOVA.name(), ShipPersonality.PROFESSIONAL.name());
        saveShip(102, KokoroVoices.ALICE.name(), ShipPersonality.ROGUE.name());

        makeActive(101);
        assertEquals(KokoroVoices.NOVA, session.getKokoroVoice());
        assertEquals(ShipPersonality.PROFESSIONAL, session.getAIPersonality());

        // Switching the active ship switches the voice and personality with it.
        makeActive(102);
        assertEquals(KokoroVoices.ALICE, session.getKokoroVoice());
        assertEquals(ShipPersonality.ROGUE, session.getAIPersonality());
    }

    @Test
    void aMaleVoiceIsKeptAndReportedAsTheCompanionGender() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        try {
            session.setTtsProvider(TtsProvider.KOKORO);

            saveShip(301, KokoroVoices.GEORGE.name(), ShipPersonality.CASUAL.name());
            makeActive(301);
            assertEquals(KokoroVoices.GEORGE, session.getKokoroVoice());
            // The voice is the only thing picked; the prompt's self-reference follows it.
            assertEquals(VoiceGender.MALE, session.getVoiceGender());

            saveShip(302, KokoroVoices.NOVA.name(), ShipPersonality.CASUAL.name());
            makeActive(302);
            assertEquals(VoiceGender.FEMALE, session.getVoiceGender());

            // A ship carrying a voice this engine does not know takes the default voice, which is female.
            saveShip(303, GoogleVoices.JAKE.name(), ShipPersonality.CASUAL.name());
            makeActive(303);
            assertEquals(KokoroVoices.DEFAULT_VOICE, session.getKokoroVoice());
            assertEquals(VoiceGender.FEMALE, session.getVoiceGender());
        } finally {
            session.setTtsProvider(previousProvider);
        }
    }

    @Test
    void voiceGenderIsReadThroughTheActiveTtsProvider() {
        SystemSession session = SystemSession.getInstance();
        TtsProvider previousProvider = session.getTtsProvider();
        try {
            saveShip(311, GoogleVoices.STEVE.name(), ShipPersonality.CASUAL.name());
            makeActive(311);

            session.setTtsProvider(TtsProvider.GOOGLE);
            assertEquals(VoiceGender.MALE, session.getVoiceGender());

            // The same stored name is an Edge voice too (the enums are twins), so Edge hears the same gender.
            session.setTtsProvider(TtsProvider.EDGE);
            assertEquals(VoiceGender.MALE, session.getVoiceGender());

            // Kokoro has no voice by that name: it falls back to its default, and so does the gender.
            session.setTtsProvider(TtsProvider.KOKORO);
            assertEquals(VoiceGender.FEMALE, session.getVoiceGender());
        } finally {
            session.setTtsProvider(previousProvider);
        }
    }

    @Test
    void voiceInvalidForActiveProviderFallsBackToProviderDefault() {
        SystemSession session = SystemSession.getInstance();

        // A Kokoro voice name isn't a valid Google voice, so the Google getter falls back to its default.
        saveShip(201, KokoroVoices.NOVA.name(), ShipPersonality.CASUAL.name());
        makeActive(201);

        assertEquals(KokoroVoices.NOVA, session.getKokoroVoice());
        assertEquals(GoogleVoices.DEFAULT_VOICE, session.getGoogleVoice());
        assertEquals(EdgeVoices.DEFAULT_VOICE.defaultShortName(), session.getEdgeVoiceName());
    }

    @Test
    void edgeUsesTheExistingCloudVoiceNameWithoutNewShipState() {
        SystemSession session = SystemSession.getInstance();

        saveShip(401, EdgeVoices.JENNIFER.name(), ShipPersonality.CASUAL.name());
        makeActive(401);

        assertEquals(EdgeVoices.JENNIFER.defaultShortName(), session.getEdgeVoiceName());
    }

    @Test
    void edgePreservesAnArbitraryProviderShortNameInTheExistingShipField() {
        SystemSession session = SystemSession.getInstance();

        saveShip(402, "en-US-AvaNeural", ShipPersonality.CASUAL.name());
        makeActive(402);

        assertEquals("en-US-AvaNeural", session.getEdgeVoiceName());
    }
}
