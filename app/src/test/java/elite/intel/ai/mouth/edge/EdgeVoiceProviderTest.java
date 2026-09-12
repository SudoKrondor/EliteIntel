package elite.intel.ai.mouth.edge;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EdgeVoiceProviderTest {
    @Test
    void parsesVoiceMetadataAndResolvesExactDynamicVoice() throws Exception {
        String json = """
                [{"Name":"Full Jenny","ShortName":"en-US-JennyNeural","Gender":"Female",
                  "Locale":"en-US","SuggestedCodec":"audio-24khz-48kbitrate-mono-mp3"}]
                """;
        List<EdgeVoice> voices = EdgeVoiceListParser.parse(json);
        EdgeVoiceProvider provider = new EdgeVoiceProvider();
        provider.setAvailableVoices(voices);

        assertEquals("en-US-JennyNeural",
                provider.resolve(EdgeVoices.JENNIFER.name(), Language.EN).shortName());
        assertEquals("Full Jenny", voices.getFirst().protocolName());
    }

    /**
     * The commander picks a voice, and its gender is part of that pick - it also decides how VEGA
     * speaks of itself. A selection Edge does not offer for the language therefore degrades to a voice of the
     * same gender, never across it, both against the live list and against the known-good fallbacks.
     */
    @Test
    void unavailableSelectionsUseDeterministicSameGenderLocaleThenKnownFallback() {
        EdgeVoiceProvider provider = new EdgeVoiceProvider();
        provider.setAvailableVoices(List.of(
                voice("en-US-ZoeNeural", "Female", "en-US"),
                voice("en-US-AdamNeural", "Male", "en-US"),
                voice("de-DE-KatjaNeural", "Female", "de-DE"),
                voice("de-DE-ConradNeural", "Male", "de-DE")));

        assertEquals("en-US-ZoeNeural", provider.resolve(EdgeVoices.MARY.name(), Language.EN).shortName());
        assertEquals("en-US-AdamNeural", provider.resolve(EdgeVoices.JAKE.name(), Language.EN).shortName());
        assertEquals("de-DE-KatjaNeural", provider.resolve(EdgeVoices.MARY.name(), Language.DE).shortName());
        assertEquals("de-DE-ConradNeural", provider.resolve(EdgeVoices.JAKE.name(), Language.DE).shortName());

        provider.clear();
        assertEquals("ru-RU-SvetlanaNeural",
                provider.resolve(EdgeVoices.MARY.name(), Language.RU).shortName());
        assertEquals("ru-RU-DmitryNeural",
                provider.resolve(EdgeVoices.JAKE.name(), Language.RU).shortName());
    }

    @Test
    void malformedOrEmptyVoiceListsFailClosed() {
        assertThrows(EdgeProtocolException.class, () -> EdgeVoiceListParser.parse("{}"));
        assertThrows(EdgeProtocolException.class, () -> EdgeVoiceListParser.parse("[]"));
        assertThrows(EdgeProtocolException.class,
                () -> EdgeVoiceListParser.parse("[{\"ShortName\":\"x\"}]"));
        assertThrows(EdgeProtocolException.class,
                () -> EdgeVoiceListParser.parse("[{\"ShortName\":[],\"Gender\":\"Female\",\"Locale\":\"en-US\"}]"));

        EdgeVoiceProvider provider = new EdgeVoiceProvider();
        assertThrows(IllegalArgumentException.class, () -> provider.setAvailableVoices(List.of()));
        assertFalse(provider.hasAvailableVoices());
    }

    @Test
    void arbitraryPersistedShortNamesResolveToThemselvesWhateverTheirGender() {
        EdgeVoiceProvider provider = new EdgeVoiceProvider();
        provider.setAvailableVoices(List.of(
                voice("en-US-AvaNeural", "Female", "en-US"),
                voice("en-US-GuyNeural", "Male", "en-US")));

        assertEquals("en-US-AvaNeural", provider.resolve("en-US-AvaNeural", Language.EN).shortName());
        // en-US-GuyNeural is JAKE's ShortName: a commander who stored it directly gets the voice they named.
        assertEquals("en-US-GuyNeural", provider.resolve("en-US-GuyNeural", Language.EN).shortName());
    }

    @Test
    void persistedGoogleAliasesMapToTheirOwnVoiceAndNativeShortNamesArePreserved() {
        assertEquals(EdgeVoices.JENNIFER.defaultShortName(),
                EdgeVoices.shortNameOrDefault("JENNIFER"));
        // A male selection keeps its own voice; only a name Edge cannot place falls back to the default.
        assertEquals(EdgeVoices.JAKE.defaultShortName(), EdgeVoices.shortNameOrDefault("JAKE"));
        assertEquals("en-US-AvaNeural", EdgeVoices.shortNameOrDefault("en-US-AvaNeural"));
        assertEquals(EdgeVoices.DEFAULT_VOICE.defaultShortName(),
                EdgeVoices.shortNameOrDefault("not-a-voice"));
    }

    private static EdgeVoice voice(String shortName, String gender, String locale) {
        return new EdgeVoice(null, shortName, gender, locale, EdgeProtocolConstants.OUTPUT_FORMAT);
    }
}
