package elite.intel.ai.brain.i18n;

import java.util.Set;

public abstract class AiActionAliasProvider {

    public abstract Set<String> wakeBypassPhrases();

    /**
     * Listen-type prefixes that can be stripped before forwarding to the AI.
     * E.g. "listen open galaxy map" → "open galaxy map".
     * Pure wake phrases (no content follows) are NOT in this set.
     */
    public abstract Set<String> listenBypassPrefixes();
}
