package elite.intel.ai.brain.i18n.ru;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;

import java.util.Set;

public class RussianAiActionAliases extends AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("проснись", "слушай", "слушай меня", "активируйся");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("слушай меня", "слушай");
    }
}
