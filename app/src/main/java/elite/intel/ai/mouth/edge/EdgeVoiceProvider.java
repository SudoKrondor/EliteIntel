package elite.intel.ai.mouth.edge;

import elite.intel.i18n.Language;

import java.util.Comparator;
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

    /**
     * Resolves the stored ship voice against what Edge currently offers, in three steps: the exact voice if it
     * is on offer for this language, otherwise a voice of the same gender in the right language, otherwise a
     * known-good voice for the language even when the voice list could not be fetched.
     * <p>
     * Gender is the commander's choice of voice and is preserved through every step: a ship set to a male
     * voice must not land on a female one in a language whose exact voice Edge does not offer, because the
     * same choice also tells VEGA prompt how to speak of itself.
     *
     * @param selectedName a logical {@link EdgeVoices} name, or a provider-native ShortName a commander stored
     *                     directly, or {@code null} for the default
     */
    EdgeVoice resolve(String selectedName, Language language) {
        EdgeVoices known = EdgeVoices.find(selectedName);
        EdgeVoices logical = known == null ? null : EdgeVoices.voiceOrDefault(selectedName);
        String desired = logical == null ? selectedName : logical.defaultShortName();

        List<EdgeVoice> voices = availableVoices;
        if (desired != null) {
            String requested = desired;
            EdgeVoice exact = voices.stream()
                    .filter(voice -> voice.shortName().equals(requested))
                    .filter(voice -> languageMatches(language, voice.locale()))
                    .findFirst()
                    .orElse(null);
            if (exact != null) {
                return exact;
            }
        }

        // The exact voice is not on offer for this language, so fall back to one of the same gender that is.
        EdgeVoices position = logical == null ? EdgeVoices.DEFAULT_VOICE : logical;
        boolean male = position.male();
        List<EdgeVoice> candidates = voices.stream()
                .filter(voice -> languageMatches(language, voice.locale()))
                .filter(voice -> voice.male() == male)
                .sorted(Comparator.comparing(EdgeVoice::shortName))
                .toList();
        if (!candidates.isEmpty()) {
            // WHY: index by the logical voice's ordinal rather than always taking the first candidate, so a
            // fleet whose ships carry different voices still sounds different in a language where none of the
            // exact voices exist. A name we do not recognise has no position of its own and takes the default
            // voice's, which is where every other unknown voice in the app lands.
            return candidates.get(Math.floorMod(position.ordinal(), candidates.size()));
        }
        String fallback = language == Language.EN
                ? position.defaultShortName()
                : localizedFallback(language, male);
        return fallbackVoice(fallback, locale(language), male);
    }

    private static EdgeVoice fallbackVoice(String shortName, String locale, boolean male) {
        return new EdgeVoice(
                EdgeSsml.protocolVoiceName(shortName), shortName, male ? "Male" : "Female", locale,
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

    /**
     * A voice of the requested gender known to exist for the language, used when the live voice list has not
     * arrived (or offers nothing matching). One name per gender per locale, all long-standing Edge Read Aloud
     * voices, so the fallback never depends on a roster this build cannot see.
     */
    private static String localizedFallback(Language language, boolean male) {
        return switch (language) {
            case EN -> male ? "en-US-AndrewMultilingualNeural" : "en-US-EmmaMultilingualNeural";
            case RU -> male ? "ru-RU-DmitryNeural" : "ru-RU-SvetlanaNeural";
            case UK -> male ? "uk-UA-OstapNeural" : "uk-UA-PolinaNeural";
            case DE -> male ? "de-DE-ConradNeural" : "de-DE-KatjaNeural";
            case FR -> male ? "fr-FR-HenriNeural" : "fr-FR-DeniseNeural";
            case ES -> male ? "es-ES-AlvaroNeural" : "es-ES-ElviraNeural";
            case IT -> male ? "it-IT-DiegoNeural" : "it-IT-ElsaNeural";
            case PT -> male ? "pt-PT-DuarteNeural" : "pt-PT-RaquelNeural";
            case PTBZ -> male ? "pt-BR-AntonioNeural" : "pt-BR-FranciscaNeural";
        };
    }
}
