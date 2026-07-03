package elite.intel.session;

import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.mouth.google.GoogleVoices;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Voice and personality are app-wide (single voice per TTS provider, one personality), persisted in
 * the seeded {@code game_session} row without any schema change: Kokoro voice in {@code aiVoice},
 * Google voice in {@code aiCadence} (repurposed), personality in {@code aiPersonality}. The two
 * provider voices are independent so switching TTS preserves each.
 */
class GlobalVoicePersonalityTest {

    @Test
    void providerVoicesPersistIndependently() {
        SystemSession session = SystemSession.getInstance();

        session.setKokoroVoice(KokoroVoices.NOVA);
        session.setGoogleVoice(GoogleVoices.RACHEL);

        assertEquals(KokoroVoices.NOVA, session.getKokoroVoice());
        assertEquals(GoogleVoices.RACHEL, session.getGoogleVoice());

        // Changing one provider's voice must not disturb the other's.
        session.setKokoroVoice(KokoroVoices.GEORGE);
        assertEquals(KokoroVoices.GEORGE, session.getKokoroVoice());
        assertEquals(GoogleVoices.RACHEL, session.getGoogleVoice());
    }

    @Test
    void personalityRoundTrips() {
        SystemSession session = SystemSession.getInstance();
        session.setAIPersonality(ShipPersonality.PROFESSIONAL);
        assertEquals(ShipPersonality.PROFESSIONAL, session.getAIPersonality());
        session.setAIPersonality(ShipPersonality.ROGUE);
        assertEquals(ShipPersonality.ROGUE, session.getAIPersonality());
    }
}
