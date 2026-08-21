package elite.intel.ai.ears;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * The one thing a sleeping companion still listens for.
 * <p>
 * Two shapes get past a closed Sleep/Wake gate, and nothing else does:
 * <ul>
 *   <li>a <b>pure wake phrase</b> — "wake up", "слушай" — passed through whole, so the model routes it to
 *       WAKEUP and the gate reopens;</li>
 *   <li>a <b>listen-prefixed order</b> — "listen, open the galaxy map" — passed through with the prefix
 *       removed, because that is an order to open the map, not an order to listen.</li>
 * </ul>
 * The prefix must be at the start and must be followed by real content. That is what stops "do not listen,
 * open the map" from talking its way past a gate the commander deliberately closed: the phrase contains a
 * listen word, but it does not begin with one.
 * <p>
 * Longest prefix first, so "listen up open galaxy map" is not first reduced by the shorter "listen" and left
 * with a stray "up".
 */
public final class WakeBypass {

    private final Set<String> wakePhrases;
    private final List<String> listenPrefixes;

    public WakeBypass(Set<String> wakePhrases, Collection<String> listenPrefixes) {
        this.wakePhrases = Set.copyOf(wakePhrases);
        List<String> longestFirst = new ArrayList<>(listenPrefixes);
        longestFirst.sort(Comparator.comparingInt(String::length).reversed());
        this.listenPrefixes = List.copyOf(longestFirst);
    }

    public WakeBypass(AiActionAliasProvider provider) {
        this(provider.wakeBypassPhrases(), provider.listenBypassPrefixes());
    }

    /**
     * The bypass for the commander's current language.
     */
    public static WakeBypass forCurrentLanguage() {
        return new WakeBypass(AiActionLocalizations.wakeBypassPhrases(),
                AiActionLocalizations.listenBypassPrefixes());
    }

    /**
     * What to route for {@code transcript}, or {@code null} when the gate holds and it is discarded.
     * A pure wake phrase comes back unchanged; a listen-prefixed order comes back without its prefix.
     */
    public @Nullable String admit(String transcript) {
        if (transcript == null || transcript.isBlank()) return null;
        String order = strippedOrder(transcript);
        if (order != null) return order;
        return isPureWakePhrase(transcript) ? transcript : null;
    }

    /**
     * True when the transcript may pass the gate at all, either shape.
     */
    public boolean passesGate(String transcript) {
        return admit(transcript) != null;
    }

    public boolean isPureWakePhrase(String transcript) {
        String lower = transcript.trim().toLowerCase(Locale.ROOT);
        for (String phrase : wakePhrases) {
            if (lower.equals(phrase.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /**
     * The order behind a listen prefix, or {@code null} when the transcript is not one — including when it is
     * a bare wake phrase, which is not an order and must reach the model whole.
     */
    public @Nullable String strippedOrder(String transcript) {
        String lower = transcript.toLowerCase(Locale.ROOT);
        for (String prefix : listenPrefixes) {
            String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
            if (lower.startsWith(lowerPrefix)
                    && lower.length() > lowerPrefix.length()
                    && Character.isWhitespace(lower.charAt(lowerPrefix.length()))) {
                String remainder = transcript.substring(prefix.length()).trim();
                if (!remainder.isBlank()) return remainder;
            }
        }
        return null;
    }
}
