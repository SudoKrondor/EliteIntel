package elite.intel.ai.brain.i18n.en;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;

import java.util.Set;

public class EnglishAiActionAliases extends AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("wake", "wake up", "listen", "listen up");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("listen up", "listen");
    }
}
