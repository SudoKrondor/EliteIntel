package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.AiActionAliasProvider;

import java.util.Set;

public class FrenchAiActionAliases extends AiActionAliasProvider {

    @Override
    public Set<String> wakeBypassPhrases() {
        return Set.of("réveille-toi", "réveille toi", "reveille-toi", "reveille toi", "écoute", "ecoute", "ecoute commande vocale", "ecoute les commandes vocales");
    }

    @Override
    public Set<String> listenBypassPrefixes() {
        return Set.of("écoute-moi", "écoute moi", "ecoute-moi", "ecoute moi", "écoute", "ecoute");
    }
}
