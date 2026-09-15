package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.SupercruiseDestinationDropEvent;
import elite.intel.gameapi.signals.ConflictZoneIntensity;
import elite.intel.session.ConflictZone;
import elite.intel.session.PlayerSession;

/**
 * Keeps {@link ConflictZone} level with the journal: which system the commander is fighting a war
 * in, and when they have stopped. The twin of {@link ResourceSiteSubscriber}, with the same split
 * from the narrating drop subscriber and for the same reason - this is a single assignment that has
 * to have happened by the time the overlay polls, so it runs on the bus thread and owns nothing else.
 * <p>
 * A drop that is not into a conflict zone is ignored rather than treated as leaving one. Flying in
 * to cash in means dropping at a station, and that must not throw away the tally the commander is
 * flying there to collect on.
 */
public class ConflictZoneSubscriber {

    static final String VOUCHER_COMBAT_BOND = "CombatBond";

    private final ConflictZone conflictZone = ConflictZone.getInstance();
    private final PlayerSession playerSession = PlayerSession.getInstance();

    /**
     * WHY the system comes from the session rather than the event: a supercruise drop names the
     * destination and no system at all. The ship cannot drop into a zone in a system it is not in.
     */
    @Subscribe
    public void onSuperCruiseDrop(SupercruiseDestinationDropEvent event) {
        if (event == null) return;
        ConflictZoneIntensity intensity = ConflictZoneIntensity.fromSymbol(event.getTypeSymbol());
        if (intensity == null) return;

        conflictZone.droppedIn(playerSession.getLocationData().getSystemAddress(), intensity);
    }

    /**
     * Only redeeming combat bonds ends the fight. Cashing bounties or a codex voucher at the same
     * counter says nothing about the war.
     */
    @Subscribe
    public void onRedeemVoucher(RedeemVoucherEvent event) {
        if (event == null || !VOUCHER_COMBAT_BOND.equalsIgnoreCase(event.getType())) return;
        conflictZone.cashedIn();
    }
}
