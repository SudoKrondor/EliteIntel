package elite.intel.ai.mouth.edge;

import elite.intel.i18n.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void unavailableSelectionsUseDeterministicFemaleLocaleThenKnownFallback() {
        EdgeVoiceProvider provider = new EdgeVoiceProvider();
        provider.setAvailableVoices(List.of(
                voice("en-US-ZoeNeural", "Female", "en-US"),
                voice("en-US-AdamNeural", "Male", "en-US"),
                voice("de-DE-KatjaNeural", "Female", "de-DE")));

        assertEquals("en-US-ZoeNeural", provider.resolve(EdgeVoices.MARY.name(), Language.EN).shortName());
        assertEquals("en-US-ZoeNeural", provider.resolve(EdgeVoices.JAKE.name(), Language.EN).shortName());
        assertEquals("de-DE-KatjaNeural", provider.resolve(EdgeVoices.MARY.name(), Language.DE).shortName());

        provider.clear();
        assertEquals("ru-RU-SvetlanaNeural",
                provider.resolve(EdgeVoices.MARY.name(), Language.RU).shortName());
        assertEquals("ru-RU-SvetlanaNeural",
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
    void arbitraryPersistedShortNamesResolveWhenFemaleAndRejectMaleForMainVoice() {
        EdgeVoiceProvider provider = new EdgeVoiceProvider();
        provider.setAvailableVoices(List.of(
                voice("en-US-AvaNeural", "Female", "en-US"),
                voice("en-US-GuyNeural", "Male", "en-US")));

        assertEquals("en-US-AvaNeural", provider.resolve("en-US-AvaNeural", Language.EN).shortName());
        assertEquals("en-US-AvaNeural", provider.resolve("en-US-GuyNeural", Language.EN).shortName());
    }

    @Test
    void persistedGoogleAliasesDegradeDeterministicallyAndNativeShortNamesArePreserved() {
        assertEquals(EdgeVoices.JENNIFER.defaultShortName(),
                EdgeVoices.femaleShortNameOrDefault("JENNIFER"));
        assertEquals(EdgeVoices.DEFAULT_FEMALE.defaultShortName(),
                EdgeVoices.femaleShortNameOrDefault("JAKE"));
        assertEquals("en-US-AvaNeural", EdgeVoices.femaleShortNameOrDefault("en-US-AvaNeural"));
        assertEquals(EdgeVoices.DEFAULT_FEMALE.defaultShortName(),
                EdgeVoices.femaleShortNameOrDefault("not-a-voice"));
    }

    private static EdgeVoice voice(String shortName, String gender, String locale) {
        return new EdgeVoice(null, shortName, gender, locale, EdgeProtocolConstants.OUTPUT_FORMAT);
    }
}
