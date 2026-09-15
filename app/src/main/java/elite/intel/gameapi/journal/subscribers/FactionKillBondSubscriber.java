package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.CombatBondManager;
import elite.intel.gameapi.journal.events.FactionKillBondEvent;
import elite.intel.session.PlayerSession;

/**
 * Files each combat bond in the ledger the conflict-zone card reads.
 * <p>
 * Silent on purpose, unlike {@link BountyEventSubscriber}: kills in a conflict zone come every few
 * seconds and the card shows the running tally, so a spoken line per kill would talk over the fight.
 */
@SuppressWarnings("unused")
public class FactionKillBondSubscriber {

    private final CombatBondManager combatBonds = CombatBondManager.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onFactionKillBond(FactionKillBondEvent event) {
        if (event == null) return;
        Thread.ofVirtual().start(() -> combatBonds.record(
                playerSession.getLocationData().getSystemAddress(),
                preferPlain(event.getAwardingFaction(), event.getAwardingFactionLocalised()),
                preferPlain(event.getVictimFaction(), event.getVictimFactionLocalised()),
                event.getReward(),
                event.getTimestamp()
        ));
    }

    /**
     * A faction that is really a symbol ({@code $faction_PilotsFederation;}) reads better by its
     * localised name; an ordinary faction name has no localised twin and is used as is.
     */
    private static String preferPlain(String name, String localised) {
        if (name != null && name.startsWith("$") && localised != null && !localised.isBlank()) return localised;
        return name;
    }
}
