package elite.intel.ai.brain.i18n.de;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;

import java.util.Set;

public class GermanAiActionAliases extends AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("wach auf", "hör zu", "hör mir zu", "aktiviere dich");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("hör mir zu", "hör zu");
    }
}
