package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.subscribers.events.MiningAnnouncementEvent;
import elite.intel.gameapi.EventBusManager;
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
            boolean foundTargetMaterial = false;
            StringBuilder sb = new StringBuilder();
            PlayerSession playerSession = PlayerSession.getInstance();

            for (ProspectedAsteroidEvent.Material material : event.getMaterials()) {
                if (material == null) continue;
                if (material.getName() == null || material.getName().isEmpty()) continue;

                String prospectedMaterial = capitalizeWords(material.getName());
                Set<String> miningTargets = playerSession.getMiningTargets();

                if (miningTargets.contains(prospectedMaterial)) {
                    foundTargetMaterial = true;
                    double proportion = material.getProportion();
                    sb.append(localizedEvent("event.mining.prospectorDetected", String.format("%.2f", proportion), material.getName()));
                    break;
                }
            }

            if (foundTargetMaterial) {
                EventBusManager.publish(new MiningAnnouncementEvent(sb.toString()));
            }
        });
    }
}
