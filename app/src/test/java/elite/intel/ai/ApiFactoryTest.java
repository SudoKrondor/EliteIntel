package elite.intel.ai;

import elite.intel.ai.mouth.TtsProvider;
import elite.intel.ai.mouth.edge.EdgeTTSImpl;
import elite.intel.ai.mouth.google.GoogleTTSImpl;
import elite.intel.ai.mouth.kokoro.KokoroTTS;
import elite.intel.ai.mouth.supertonic.SupertonicTTS;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ApiFactoryTest {

    private static final String GOOGLE_KEY = "AIzaSy123456789012345678901234567890123";

    @Test
    void theStoredProviderSelectsTheMouth() {
        assertSame(EdgeTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.EDGE, null, Language.EN, Language.EN));
        assertSame(GoogleTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, GOOGLE_KEY, Language.EN, Language.EN));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.KOKORO, GOOGLE_KEY, Language.EN, Language.EN));
        assertSame(SupertonicTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.SUPERTONIC, GOOGLE_KEY, Language.EN, Language.EN));
    }

    /**
     * Edge is keyless, so a stored Google key must not pull the selection away from it.
     */
    @Test
    void edgeIsUnaffectedByAStoredGoogleKey() {
        assertSame(EdgeTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.EDGE, GOOGLE_KEY, Language.EN, Language.EN));
    }

    /**
     * Google without a usable key would start into silence, so the local engine stands in.
     */
    @Test
    void googleWithoutAUsableKeyFallsBackToKokoro() {
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, null, Language.EN, Language.EN));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, "", Language.EN, Language.EN));
        assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, "not-a-key", Language.EN, Language.EN));
    }

    /**
     * Kokoro cannot pronounce Cyrillic, so it is never the mouth for a Russian or Ukrainian commander - not as
     * a selection, and not as the stand-in for a keyless Google, which would swap a bill for silence. The other
     * local engine takes both cases: still local, still keyless.
     */
    @Test
    void kokoroNeverSpeaksForACyrillicCommander() {
        for (Language language : Language.values()) {
            if (!language.isCyrillicScript()) continue;
            assertSame(SupertonicTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.KOKORO, null, language, Language.EN),
                    "stored Kokoro under " + language);
            assertSame(SupertonicTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, "not-a-key", language, Language.EN),
                    "keyless Google under " + language);
            assertSame(EdgeTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.EDGE, null, language, Language.EN),
                    "a chosen Edge is kept under " + language);
            assertSame(GoogleTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, GOOGLE_KEY, language, Language.EN),
                    "Google speaks Cyrillic and stays selectable under " + language);
        }
    }

    /**
     * A Russian game client writes its radio chatter in Cyrillic, which needs Supertonic resident - and the
     * rule is that Supertonic is then the session's local engine, narration included, whatever language the
     * commander speaks to us. The cloud engines read every script and are kept.
     */
    @Test
    void aRussianGameClientMakesSupertonicTheLocalEngineForEveryCommander() {
        for (Language commander : Language.values()) {
            assertSame(SupertonicTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.KOKORO, null, commander, Language.RU),
                    "stored Kokoro, Russian client, commander speaking " + commander);
            assertSame(SupertonicTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, "not-a-key", commander, Language.RU),
                    "keyless Google, Russian client, commander speaking " + commander);
            assertSame(EdgeTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.EDGE, null, commander, Language.RU));
            assertSame(GoogleTTSImpl.getInstance(), ApiFactory.selectMouth(TtsProvider.GOOGLE, GOOGLE_KEY, commander, Language.RU));
        }
    }

    /**
     * And the client's language only ever withdraws Kokoro: a Latin-script commander on a Latin-script client
     * keeps every engine, whichever of the two the client is in.
     */
    @Test
    void aLatinScriptClientWithdrawsNothing() {
        for (Language client : Language.values()) {
            if (client.isCyrillicScript()) continue;
            assertSame(KokoroTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.KOKORO, null, Language.EN, client),
                    "stored Kokoro over a client writing " + client);
            assertSame(SupertonicTTS.getInstance(), ApiFactory.selectMouth(TtsProvider.SUPERTONIC, null, Language.EN, client));
        }
    }
}
