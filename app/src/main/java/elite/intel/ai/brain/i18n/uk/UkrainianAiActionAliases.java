package elite.intel.ai.brain.i18n.uk;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;

import java.util.Set;

public class UkrainianAiActionAliases extends AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("прокинься", "слухай", "слухай мене", "активуйся");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("слухай мене", "слухай");
    }
}
