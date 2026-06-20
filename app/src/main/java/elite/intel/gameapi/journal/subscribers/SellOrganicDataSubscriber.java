package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import elite.intel.session.PlayerSession;

public class SellOrganicDataSubscriber {

    @Subscribe
    public void onSellOrganicDataEvent(SellOrganicDataEvent event) {
        PlayerSession playerSession = PlayerSession.getInstance();
        playerSession.clearBioSamples();
        CodexEntryManager codexEntryManager = CodexEntryManager.getInstance();
        codexEntryManager.clear();
        String instructions = """
                    We sold organic data and made credits.
                    Provide user with a sale summary. Start with total amount collected, then provide a breakdown by genus.
                """;
        GameEventBus.publish(new SensorDataEvent(event.toYaml(), instructions));
    }
}
