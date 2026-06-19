package elite.intel.ai.brain.i18n.it;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;

import java.util.Set;

public class ItalianAiActionAliases extends AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of();
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of();
    }
}
