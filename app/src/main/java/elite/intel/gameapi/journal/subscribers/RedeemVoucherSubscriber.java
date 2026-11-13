package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.BountyManager;
import elite.intel.db.managers.CombatBondManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;

import java.util.Map;

/**
 * Reconciles bounty/mission state when vouchers are cashed in. The spoken payment
 * announcement is handled by {@code FinanceSubscriber}, the single home for
 * financial announcements.
 */
public class RedeemVoucherSubscriber {

    private final MissionManager missionManager = MissionManager.getInstance();
    private final BountyManager bountyManager = BountyManager.getInstance();
    private final CombatBondManager combatBonds = CombatBondManager.getInstance();

    @Subscribe
    public void onRedeemVoucherEvent(RedeemVoucherEvent event) {
        // Bonds are a war massacre's only record of its kills, so with any massacre open they are kept and flagged,
        // as bounties are. Any massacre rather than only a war one, because a war contract accepted before
        // MISSION_MASSACRE_CONFLICT existed is on file as a pirate one. With none, paid means gone.
        if (event.isCombatBond()) {
            if (missionManager.getMissions(missionManager.getMassacreMissionTypes()).isEmpty()) {
                combatBonds.clear();
            } else {
                combatBonds.markAllCashedIn();
            }
            return;
        }
        Map<Long, MissionDto> missions = missionManager.getMissions(
                missionManager.getPirateMissionTypes()
        );
        if (missions == null || missions.isEmpty()) {
            PlayerSession.getInstance().clearBounties();
        } else {
            // Missions still active: preserve bounty records for kill counting but
            // flag them as cashed-in so they are excluded from pending bounty totals.
            bountyManager.markAllCashedIn();
        }
    }
}
