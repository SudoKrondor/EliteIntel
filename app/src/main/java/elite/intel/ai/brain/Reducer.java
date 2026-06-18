package elite.intel.ai.brain;
import elite.intel.ai.brain.actions.command.CommandIds;

import elite.intel.ai.brain.i18n.AiActionLocalizations;

import java.util.*;
import java.util.stream.Collectors;

import static elite.intel.ai.brain.actions.Queries.GENERAL_CONVERSATION;

public class Reducer {

    /// trash
    public final static List<String> trashSttWords = List.of(
            "--", "mm-hmm", "uh-huh", "hmm", "mm", "uh", "um", "ah", "oh", "huh", "eh",

            "yeah", "yep", "yup", "nope", "it", "an", "cool", "the",
            "okay", "ok", "got it", "alright", "alrighty", "sure", "right",
            "hello", "hi", "hey", "bye", "goodbye",
            "so", "well", "now", "anyway", "actually", "basically", "literally",
            "thanks", "thank you", "i'm sorry", "sorry", "excuse me", "pardon",
            "you know", "i see", "i mean", "of course", "no problem",
            "i got it", "don't i", "a ", "or ", "she can", "he can", "you can",
            "like they", "did you", "wh", "i'll", "like", "got a",
            "blow", "fuck", "shit", "just", "i "
    );


    /// George Carlin list
    private static final Set<String> STOP_WORDS = Set.of(
            /// George Carlin list
            "blow", "fuck", "shit", "piss", "cunt", "cock", "cocksucker", "motherfucker",

            /// Stop words
            "a", "an", "the", "to", "of", "in", "on", "at", "by", "for",
            "with", "and", "or", "is", "are", "am", "be", "do", "does",
            "what", "where", "how", "which", "any", "our", "my", "me",
            "we", "us", "i", "you", "it", "this", "that", "get", "have",
            "has", "can", "could", "would", "should", "not", "no", "up",
            "here", "there", "some", "much", "many"
    );


    /**
     * Reduces a given map of key-value pairs based on a normalized input string and a specified conversation mode flag.
     * Filters out entries in the map unless the keys contain significant words from the normalized input.
     * Ensures a fallback behavior for empty or irrelevant inputs based on the conversation mode.
     *
     * @param normalizedInput the input string that is normalized and used to filter the map; null or blank input defaults to returning the original map
     * @param full the original map of key-value pairs to be filtered
     * @param isConversationMode a flag indicating whether the method is running in conversation mode; determines fallback behavior for empty input
     * @return a reduced map containing only the entries whose keys match significant words from the normalized input;
     *         may return a map with fallback values if the input is empty or irrelevant
     */
    public static Map<String, String> reduce(
            String normalizedInput,
            Map<String, String> full,
            boolean isConversationMode
    ) {
        if (normalizedInput == null || normalizedInput.isBlank()) {
            return full;
        }

        // Preserve an exact alias match as a high-confidence candidate.
        // This must not replace semantic classification; it only prevents
        // the reducer from accidentally removing a valid action before the LLM sees it.
        String directAction = full.get(normalizedInput);
        if (directAction == null) {
            String lowerInput = normalizedInput.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : full.entrySet()) {
                List<String> phrases = AiActionLocalizations.splitPhraseGroup(entry.getKey());
                if (phrases.stream().anyMatch(p -> p.toLowerCase(Locale.ROOT).equals(lowerInput))) {
                    directAction = entry.getValue();
                    break;
                }
            }
        }

        // Use Unicode-aware tokenization.
        // "\\W+" is too ASCII-centric and does not work reliably with Cyrillic,
        // Ukrainian, German umlauts, and other non-English input.
        Set<String> inputWords = Arrays.stream(
                        normalizedInput
                                .toLowerCase(Locale.ROOT)
                                .split("[^\\p{L}\\p{N}_]+")
                )
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());

        Map<String, String> result = new LinkedHashMap<>();

        // Add all actions whose trigger phrases share meaningful words
        // with the normalized user input.
        for (Map.Entry<String, String> entry : full.entrySet()) {
            String trigger = entry.getKey();
            String action = entry.getValue();

            Set<String> triggerWords = Arrays.stream(
                            trigger
                                    .toLowerCase(Locale.ROOT)
                                    .split("[^\\p{L}\\p{N}_]+")
                    )
                    .filter(w -> w.length() > 2)
                    .filter(w -> !STOP_WORDS.contains(w))
                    .collect(Collectors.toSet());

            boolean hasOverlap = triggerWords.stream().anyMatch(inputWords::contains);

            if (hasOverlap) {
                result.put(trigger, action);
            }
        }

        // If the user input exactly matches an alias from the action map,
        // keep that action in the reduced candidate list.
        // It is still only a candidate; the LLM remains responsible for final intent selection.
        if (directAction != null) {
            result.put(normalizedInput, directAction);
        }

        // If no candidate survived reduction, fall back according to the current mode.
        if (result.isEmpty()) {
            if (isConversationMode) {
                result.put(GENERAL_CONVERSATION.getAction(), GENERAL_CONVERSATION.getAction());
            } else {
                result.put(CommandIds.IGNORE_NONSENSICAL_INPUT, CommandIds.IGNORE_NONSENSICAL_INPUT);
            }
        }

        return result;
    }

    public static String formatActions(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("ACTIONS (use ONLY these exact action names):\n\n");
        map.forEach((key, action) ->
                sb.append("  ").append(action).append(" ← ").append(key).append("\n"));
        return sb.toString();
    }
}
