package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.gameapi.journal.events.ProspectedAsteroidEvent;
import elite.intel.session.PlayerSession;

import java.util.Set;

import static elite.intel.util.StringUtls.capitalizeWords;
import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class ProspectorSubscriber {

    @Subscribe
    public void onProspectedAsteroidEvent(ProspectedAsteroidEvent event) {
        Thread.ofVirtual().start(() -> {
            PlayerSession playerSession = PlayerSession.getInstance();
            if (!playerSession.isMiningAnnouncementOn()) return;

            String phrase = prospectorPhrase(event, playerSession.getMiningTargets());
            if (phrase.isEmpty()) return;

            // A core is worth cutting in for: it is rare, and the rock drifts out of range while the
            // companion finishes whatever it was saying.
            CompanionRuntime.narrator().announce(phrase, event.isCore());
        });
    }

    /**
     * What the prospector hit is worth saying, or an empty string when it is worth nothing.
     * <p>
     * Two independent reasons to speak, and a core is the one that does not depend on what the
     * commander is mining for: core asteroids are rare, they have to be cracked rather than mined,
     * and passing one by unremarked because its surface minerals were off the target list is a
     * worse miss than one more line of chatter. A rock can be both, and then both are said.
     */
    static String prospectorPhrase(ProspectedAsteroidEvent event, Set<String> miningTargets) {
        StringBuilder sb = new StringBuilder();

        if (event.isCore()) {
            sb.append(localizedEvent("event.mining.motherlodeDetected", spokenMotherlode(event.getMotherlodeMaterial())));
        }

        if (event.getMaterials() != null) {
            for (ProspectedAsteroidEvent.Material material : event.getMaterials()) {
                if (material == null) continue;
                if (material.getName() == null || material.getName().isEmpty()) continue;

                String prospectedMaterial = capitalizeWords(material.getName());
                if (miningTargets.contains(prospectedMaterial)) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(localizedEvent("event.mining.prospectorDetected",
                            String.format("%.2f", material.getProportion()), material.getName()));
                    break;
                }
            }
        }

        return sb.toString();
    }

    /**
     * {@code MotherlodeMaterial} is a bare FDev symbol with no {@code _Localised} sibling, so the
     * spoken name has to be looked up. A symbol the commodities table does not know still gets
     * announced - the core matters more than the label - with the run-together symbol split back
     * into words so it is not read out as one.
     */
    private static String spokenMotherlode(String symbol) {
        String localized = FuzzySearch.localizedCommodityNameForSymbol(symbol);
        return localized != null ? localized : symbol.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }
}
