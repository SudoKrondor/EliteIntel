package elite.intel.util;

import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.gameapi.i18n.EventsTextProvider;
import elite.intel.i18n.Language;
import elite.intel.session.PlayerSession;
import elite.intel.session.SystemSession;
import elite.intel.ui.i18n.MultiLingualTextProvider;

import javax.annotation.Nullable;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtls {

    private static final Pattern DISPLAY_INTEGER_PATTERN =
            Pattern.compile("(?<![\\p{L}\\p{N}])([+-]?\\d{4,})(?![\\p{L}\\p{N}])");

    public static String subtractString(String a, String b) {
        if (a == null || b == null) return "";
        return a.replace(b, "").replace("null", "").trim();
    }


    public static Integer getIntSafely(@Nullable String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Converts all characters to lower case and capitalizes the first character of each word in the string
     *
     * @param input String to process
     * @return Processed string with capitalized words or null if input is null or empty
     */
    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return null;

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)))
                        .append(words[i].substring(1));
            }
            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static String isFuelStarClause(String starClass) {
        if (starClass == null) return "";
        boolean isFuelStar = "KGBFOAM".contains(starClass);
        return " " + (isFuelStar ? localizedEvent("event.route.fuelAvailable") : localizedEvent("event.route.noFuel")) + " ";
    }

    private static int getHourOfDay() {
        return LocalDateTime.now().getHour();
    }

    public static String greeting(String playerName) {
        Language language = effectiveTtsLanguage();
        String spokenName = spokenNameOrCommander(playerName, language);

        int hour = getHourOfDay();
        String greetingKey = hour >= 5 && hour < 12
                ? "speech.greeting.morning"
                : hour >= 12 && hour < 18
                ? "speech.greeting.afternoon"
                : "speech.greeting.evening";

        return MultiLingualTextProvider.getText(language, greetingKey, spokenName);
    }


    //TODO: remove payer name from method signature once UI changes are in
    public static String shipIntroduction(String playerName, String shipName) {
        Language language = effectiveTtsLanguage();
        String spokenName = spokenNameOrCommander(playerName, language);
        String safeShipName = shipName == null || shipName.isBlank()
                ? MultiLingualTextProvider.getText(language, "speech.shipFallback")
                : shipName;
        return MultiLingualTextProvider.getText(
                language,
                "speech.shipIntroduction",
                spokenName,
                safeShipName,
                Ranks.getPlayerHonorific(
                        PlayerSession.getInstance().getRankAndProgressDto().getCombatRankEmpire(),
                        PlayerSession.getInstance().getRankAndProgressDto().getCombatRankFederation()
                )
        );
    }

    public static String localizedSpeech(String key, Object... args) {
        return MultiLingualTextProvider.getText(effectiveTtsLanguage(), key, args);
    }

    public static String localizedLlm(String key, Object... args) {
        return LlmTextProvider.getText(effectiveTtsLanguage(), key, args);
    }

    public static String localizedAiActionKeys(String action) {
        return AiActionAliasTextProvider.getText(SystemSession.getInstance().getLanguage(), action);
    }

    public static String localizedEvent(String key, Object... args) {
        return EventsTextProvider.getText(effectiveTtsLanguage(), key, args);
    }

    public static String localizedEventPlural(int count, String keyBase, Object... extraArgs) {
        Language lang = effectiveTtsLanguage();
        String suffix = pluralSuffix(lang, count);
        Object[] args = new Object[1 + extraArgs.length];
        args[0] = count;
        System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
        return EventsTextProvider.getText(lang, keyBase + suffix, args);
    }

    private static String pluralSuffix(Language lang, int count) {
        return switch (lang) {
            case RU, UK -> ruPlural(count);
            default -> count == 1 ? ".one" : ".many";
        };
    }

    private static String ruPlural(int count) {
        int mod100 = count % 100;
        int mod10 = count % 10;
        if (mod100 >= 11 && mod100 <= 19) return ".many";
        if (mod10 == 1) return ".one";
        if (mod10 >= 2 && mod10 <= 4) return ".few";
        return ".many";
    }

    public static String localizedSpeechLanguageName(Language language) {
        String key = switch (language) {
            case EN -> "language.english";
            case RU -> "language.russian";
            case UK -> "language.ukrainian";
            case DE -> "language.german";
            case FR -> "language.french";
            case ES -> "language.spanish";
            case PT -> "language.portuguese";
            case IT -> "language.italian";
        };
        return MultiLingualTextProvider.getText(effectiveTtsLanguage(), key);
    }

    private static Language effectiveTtsLanguage() {
        return AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
    }

    private static String spokenNameOrCommander(String playerName, Language language) {
        if (language == Language.EN) {
            return asciiTtsNameOrCommander(playerName);
        }
        if (playerName != null && !playerName.isBlank()) {
            return playerName;
        }
        return MultiLingualTextProvider.getText(language, "speech.commander");
    }

    private static String asciiTtsNameOrCommander(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "Commander";
        }
        String normalized = Normalizer.normalize(playerName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String ascii = normalized
                .replaceAll("[^\\x00-\\x7F]", "")
                .replaceAll("[^A-Za-z0-9 .'-]", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return ascii.isBlank() ? "Commander" : ascii;
    }


    /**
     * Converts a version string into a numeric format, preserving up to three version components
     * (e.g., major, minor, patch-build) while padding each component to four digits. The resulting
     * numeric representation is capped at 12 digits.
     *
     * @param version the version string to convert. If null, 0 is returned.
     * @return a long representing the numeric version, or 0 if input is null or cannot be processed.
     */
    public static long getNumericBuild(String version) {
        if (version == null) return 0L;
        // Remove non-digits and non-dots
        String cleaned = version.replaceAll("[^\\d.]", "");
        String[] parts = cleaned.split("\\.");
        // Take last up to 3 parts (major/minor/patch-build), pad to 4 digits each
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, parts.length - 3);
        for (int i = start; i < parts.length; i++) {
            sb.append(String.format("%4s", parts[i]).replace(' ', '0'));
        }
        // If less than 3, prepend zeros (e.g., 0172 -> 00000172)
        while (sb.length() < 12) {
            sb.insert(0, "0000");
        }
        return Long.parseLong(sb.substring(0, 12));
    }

    /**
     * Removes conversational filler/discourse openers the LLM tends to prepend despite the
     * "no filler" instruction in {@link elite.intel.ai.brain.ShipPersonality}. The prompt rule
     * only reduces frequency; this is the deterministic backstop applied to every TTS string.
     * Locale-aware: the filler lists live in {@link TtsFillerRules}, keyed on the current
     * session language.
     */
    public static String stripLeadingFillers(String input) {
        if (input == null) return "";
        return TtsFillerRules.stripLeading(input, SystemSession.getInstance().getLanguage());
    }

    public static String sanitizeTts(String input) {
        if (input == null) return "";
        // NFC first: fold any decomposed accents (e + combining acute) into single precomposed
        // letters so legitimate German/French/Russian/Ukrainian/Spanish characters survive the
        // \p{M} strip below. Precomposed letters (é, ü, ñ, Cyrillic й/ї) are category L, not M.
        return Normalizer.normalize(stripLeadingFillers(input), Normalizer.Form.NFC)
                .replaceAll("\\*{1,2}([^*\n]*?)\\*{1,2}", "$1") // **bold** / *italic* → plain
                .replaceAll("_([^_\n]*?)_", "$1")                // _italic_ → plain
                .replaceAll("~~([^~\n]*?)~~", "$1")              // ~~strikethrough~~ → plain
                .replaceAll("`{1,3}[^`\n]*`{1,3}", "")          // `code` / ```block``` → remove
                .replaceAll("(?m)^#{1,6}\\s*", "")              // # headings → remove marker
                .replaceAll("(?m)^>\\s?", "")                   // > blockquotes → remove marker
                .replace("\\n", " ").replace("\\r", " ")        // literal escape sequences from LLM
                .replaceAll("[\\r\\n]+", " ")                    // actual newline characters → space
                .replaceAll("(?<=\\S)-(?=\\S)", " ")             // "ninety-five" → "ninety five" (hyphen between chars)
                .replace("!", ". ")                             // espeak-ng stof crash on exclamatory sentences
                .replace("*", " ")                              // any stray asterisks
                .replace("`", "")                               // any stray backticks
                .replace("\"", "")
                .replace(". .", ".")
                .replace("[", "").replace("]", "")
                .replace("ETA", ". E.T.A.")
                .replace(":", " - ")
                // Join grouped digits so TTS reads "44 543" as one number instead of 44 and 543.
                .replaceAll("(?<=\\d)[ \\u00A0\\u202F](?=\\d{3}(?:[ \\u00A0\\u202F]\\d{3})*(?!\\d))", "")
                .replaceAll("[\\p{C}\\p{So}\\p{Sk}]+", " ")      // drop controls, emojis, and standalone symbols
                .replaceAll("\\p{M}+", "")                       // drop stray combining marks (e.g. IPA U+0329) NFC couldn't compose; precomposed accents are \p{L} and survive
                .replaceAll("\\.{2,}", " ")                     // "..." → space (espeak-ng stof crash on multi-dot sequences)
                .replaceAll("\\s{2,}", " ")                     // collapse repeated spaces
                .replace(", pilot", " " + PlayerSession.getInstance().getVariablePlayerName())
                .replace(", Commander", " " + PlayerSession.getInstance().getVariablePlayerName())
                .replace("Commander", " " + PlayerSession.getInstance().getVariablePlayerName())
                .trim();
    }

    /**
     * Groups long standalone integers for display without changing the text sent to TTS.
     * Alphanumeric identifiers are left untouched.
     */
    public static String formatNumbersForDisplay(String input) {
        if (input == null || input.isEmpty()) return input;

        Matcher matcher = DISPLAY_INTEGER_PATTERN.matcher(input);
        StringBuilder formatted = new StringBuilder(input.length());

        while (matcher.find()) {
            String value = matcher.group(1);
            int digitStart = value.charAt(0) == '+' || value.charAt(0) == '-' ? 1 : 0;
            String sign = value.substring(0, digitStart);
            String digits = value.substring(digitStart);

            StringBuilder grouped = new StringBuilder(value.length() + digits.length() / 3);
            grouped.append(sign);
            int firstGroupLength = digits.length() % 3;
            if (firstGroupLength == 0) firstGroupLength = 3;
            grouped.append(digits, 0, firstGroupLength);
            for (int index = firstGroupLength; index < digits.length(); index += 3) {
                grouped.append(' ').append(digits, index, index + 3);
            }

            matcher.appendReplacement(formatted, Matcher.quoteReplacement(grouped.toString()));
        }
        matcher.appendTail(formatted);
        return formatted.toString();
    }

    public static String normalizeVersion(String v) {
        if (v == null) return "";
        return v.replaceAll("[\\r\\n]+", "").trim();
    }

    /*
    Remove underscores and seperate Camelcase
    */
    public static String humanizeBindingName(String gameBinding) {
        return gameBinding
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ")
                .replace("HUD", "HUD ")
                .replaceAll("(?<=\\D)(?=\\d)", " ")
                .replaceAll("_", " ");
    }


    public static String toReadableModuleName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String withSpaces = input.replace('_', ' ');

        String[] words = withSpaces.split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }

        String result = sb.toString().trim();

        if (result.endsWith(" Fdl")) {
            result = result.substring(0, result.length() - 3) + "FDL";
        }

        return result;
    }

    public static String removeNameEnding(String missionName) {
        return missionName
                .replace("_name", "")       // Remove the _name
                .replaceAll("_\\d+$", "");  // Remove the _00x
    }


    public static String affirmative() {
        return localizedSpeech("speech.affirmative");
    }
}
