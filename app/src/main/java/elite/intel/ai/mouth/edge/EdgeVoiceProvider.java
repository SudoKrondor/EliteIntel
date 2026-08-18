package elite.intel.ai.mouth.edge;

import elite.intel.i18n.Language;

import java.util.List;

/** Resolves the per-ship voice model against the voices currently offered by Edge Read Aloud. */
final class EdgeVoiceProvider {
    private volatile List<EdgeVoice> availableVoices = List.of();

    void setAvailableVoices(List<EdgeVoice> voices) {
        if (voices == null || voices.isEmpty()) {
            throw new IllegalArgumentException("Edge available voices must not be empty");
        }
        availableVoices = List.copyOf(voices);
    }

    boolean hasAvailableVoices() {
        return !availableVoices.isEmpty();
    }

    void clear() {
        availableVoices = List.of();
    }

    EdgeVoice resolve(String selectedName, Language language) {
        EdgeVoices mapped = EdgeVoices.find(selectedName);
        String desired = mapped == null ? selectedName : mapped.defaultShortName();
        if (mapped != null && mapped.male()) {
            mapped = EdgeVoices.DEFAULT_FEMALE;
            desired = mapped.defaultShortName();
        }
        List<EdgeVoice> voices = availableVoices;
        if (desired != null) {
            String requested = desired;
            EdgeVoice exact = voices.stream()
                    .filter(voice -> voice.shortName().equals(requested))
                    .filter(voice -> languageMatches(language, voice.locale()))
                    .filter(voice -> !voice.male())
                    .findFirst()
                    .orElse(null);
            if (exact != null) {
                return exact;
            }
        }

        List<EdgeVoice> candidates = voices.stream()
                .filter(voice -> languageMatches(language, voice.locale()))
                .filter(voice -> !voice.male())
                .sorted((left, right) -> left.shortName().compareTo(right.shortName()))
                .toList();
        if (!candidates.isEmpty()) {
            int selection = mapped == null || selectedName == null ? 0 : mapped.ordinal();
            if (mapped == null && selectedName != null) {
                selection = selectedName.hashCode();
            }
            return candidates.get(Math.floorMod(selection, candidates.size()));
        }
        String fallback = language == Language.EN
                ? (mapped == null ? EdgeVoices.DEFAULT_FEMALE.defaultShortName() : mapped.defaultShortName())
                : localizedFallback(language);
        return fallbackVoice(fallback, locale(language));
    }

    private static EdgeVoice fallbackVoice(String shortName, String locale) {
        return new EdgeVoice(
                EdgeSsml.protocolVoiceName(shortName), shortName, "Female", locale,
                EdgeProtocolConstants.OUTPUT_FORMAT);
    }

    private static boolean languageMatches(Language language, String locale) {
        return language == Language.EN
                ? locale.regionMatches(true, 0, "en-", 0, 3)
                : locale.equalsIgnoreCase(locale(language));
    }

    private static String locale(Language language) {
        return switch (language) {
            case EN -> "en-US";
            case RU -> "ru-RU";
            case UK -> "uk-UA";
            case DE -> "de-DE";
            case FR -> "fr-FR";
            case ES -> "es-ES";
            case IT -> "it-IT";
            case PT -> "pt-PT";
            case PTBZ -> "pt-BR";
        };
    }

    private static String localizedFallback(Language language) {
        return switch (language) {
            case EN -> "en-US-EmmaMultilingualNeural";
            case RU -> "ru-RU-SvetlanaNeural";
            case UK -> "uk-UA-PolinaNeural";
            case DE -> "de-DE-KatjaNeural";
            case FR -> "fr-FR-DeniseNeural";
            case ES -> "es-ES-ElviraNeural";
            case IT -> "it-IT-ElsaNeural";
            case PT -> "pt-PT-RaquelNeural";
            case PTBZ -> "pt-BR-FranciscaNeural";
        };
    }
}
