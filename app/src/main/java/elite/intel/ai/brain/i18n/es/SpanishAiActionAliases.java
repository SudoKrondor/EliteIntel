package elite.intel.ai.brain.i18n.es;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;

import java.util.Set;

public class SpanishAiActionAliases extends AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("despierta", "despiértate", "escucha", "escúchame");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("escúchame", "escucha");
    }
}
