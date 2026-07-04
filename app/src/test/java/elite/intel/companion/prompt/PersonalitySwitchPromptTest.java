package elite.intel.companion.prompt;

import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that switching the commander-chosen AI personality (the active ship's personality, read back via
 * {@link SystemSession#getAIPersonality()}) actually reaches the rendered COMMANDER prompt:
 * {@code {personalityClause}} is filled from the CURRENT personality on
 * every render (no stale in-memory field, no cached static prefix), so a switch shows up on the next turn. If
 * the companion "feels" unchanged after a switch, this test passing pins the cause to the model or the persona,
 * not to the switch failing to reach the prompt.
 */
class PersonalitySwitchPromptTest {

    private final CompanionSystemPromptPart prompt = new CompanionSystemPromptPart();
    private ShipPersonality original;

    private String render() {
        return prompt.staticRules(ThoughtSource.COMMANDER);
    }

    /**
     * Personality is per-ship; write it onto the active ship, which is what {@link SystemSession#getAIPersonality()} reads.
     */
    private static void setActiveShipPersonality(ShipPersonality personality) {
        ShipDao.Ship ship = ShipManager.getInstance().getShip();
        ship.setPersonality(personality.name());
        ShipManager.getInstance().saveShip(ship);
    }

    @BeforeEach
    void remember() {
        // Personality is per-ship, so getAIPersonality operates on the active ship. Seed one and make it
        // active so the switch round-trips through the session the same way it does in production.
        int shipId = 9001;
        ShipDao.Ship ship = new ShipDao.Ship();
        ship.setShipId(shipId);
        ship.setShipName("Personality Test Ship");
        ship.setShipIdentifier("cobramkiii");
        ship.setCargoCapacity(0);
        ship.setVoice(KokoroVoices.BELLA.name());
        ship.setPersonality(ShipPersonality.CASUAL.name());
        ship.setCommanderName("CMDR Test");
        ShipManager.getInstance().saveShip(ship);
        ShipLoadOutDto dto = new ShipLoadOutDto();
        dto.setShipId(shipId);
        ShipLoadoutManager.getInstance().clear();
        ShipLoadoutManager.getInstance().save(dto);

        original = SystemSession.getInstance().getAIPersonality();
    }

    @AfterEach
    void restore() {
        setActiveShipPersonality(original);
    }

    @Test
    void switchingPersonalityChangesTheRenderedClause() {
        setActiveShipPersonality(ShipPersonality.PROFESSIONAL);
        String professional = render();
        assertTrue(professional.contains(ShipPersonality.PROFESSIONAL.getPersonalityClause()),
                "PROFESSIONAL clause must be in the prompt after switching to it");

        setActiveShipPersonality(ShipPersonality.ROGUE);
        String rogue = render();
        assertTrue(rogue.contains(ShipPersonality.ROGUE.getPersonalityClause()),
                "ROGUE clause must be in the prompt after switching to it");

        // The switch actually changed the prompt (not just the stored DB value)...
        assertNotEquals(professional, rogue, "switching personality must change the rendered prompt");
        // ...and the previous personality's clause is gone (no stale caching of the static prefix).
        assertTrue(!rogue.contains("military professional"),
                "the ROGUE prompt must not still carry the PROFESSIONAL clause");
    }
}
