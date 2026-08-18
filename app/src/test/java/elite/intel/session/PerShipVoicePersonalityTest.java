package elite.intel.session;

import elite.intel.ai.brain.ShipPersonality;
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
 * (the one named by the current loadout). Ship voices are female-only, so the getters resolve the stored
 * name against the active TTS provider's enum and fall back to that provider's default female when the
 * stored voice name isn't a valid female voice there (including a legacy male voice from before the
 * female-only constraint).
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
    void legacyMaleVoiceResolvesToDefaultFemale() {
        SystemSession session = SystemSession.getInstance();

        // Ship voices are female-only; a ship still carrying a legacy male voice resolves to the default female.
        saveShip(301, KokoroVoices.GEORGE.name(), ShipPersonality.CASUAL.name());
        makeActive(301);

        assertEquals(KokoroVoices.DEFAULT_FEMALE, session.getKokoroVoice());
    }

    @Test
    void voiceInvalidForActiveProviderFallsBackToProviderDefault() {
        SystemSession session = SystemSession.getInstance();

        // A Kokoro voice name isn't a valid Google voice, so the Google getter falls back to its default female.
        saveShip(201, KokoroVoices.NOVA.name(), ShipPersonality.CASUAL.name());
        makeActive(201);

        assertEquals(KokoroVoices.NOVA, session.getKokoroVoice());
        assertEquals(GoogleVoices.DEFAULT_FEMALE, session.getGoogleVoice());
        assertEquals(EdgeVoices.DEFAULT_FEMALE.defaultShortName(), session.getEdgeVoiceName());
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
