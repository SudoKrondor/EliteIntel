package elite.intel.ai.brain.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiActionLocalizationsTest {

    @Test
    void splitPhraseGroupPreservesCommaInsideParameterTemplate() {
        List<String> phrases = AiActionLocalizations.splitPhraseGroup("navigate to coordinates {lat:X, lon:Y}");

        assertEquals(List.of("navigate to coordinates {lat:X, lon:Y}"), phrases);
    }

    @Test
    void splitPhraseGroupSplitsTopLevelCommaSeparatedAliases() {
        List<String> phrases = AiActionLocalizations.splitPhraseGroup(
                "navigate to coordinates {lat:X, lon:Y}, course to coordinates {lat:X, lon:Y}"
        );

        assertEquals(List.of(
                "navigate to coordinates {lat:X, lon:Y}",
                "course to coordinates {lat:X, lon:Y}"
        ), phrases);
    }
}
