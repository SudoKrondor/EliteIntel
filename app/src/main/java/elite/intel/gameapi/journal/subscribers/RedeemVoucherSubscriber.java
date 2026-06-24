package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.db.managers.BountyManager;
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

    @Subscribe
    public void onRedeemVoucherEvent(RedeemVoucherEvent event) {
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
