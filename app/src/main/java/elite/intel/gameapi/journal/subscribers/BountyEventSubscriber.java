package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.BountyEvent;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class BountyEventSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe
    public void onBountyEvent(BountyEvent event) {
        Thread.ofVirtual().start(() -> {
            playerSession.clearShipScans();

            BountyDto sessionData = new BountyDto();
            sessionData.setPilotName(event.getPilotName());
            sessionData.setVictimFaction(event.getVictimFaction());
            sessionData.setTarget(event.getTarget());
            sessionData.setTotalReward(event.getTotalReward());
            sessionData.setEarnedAt(event.getTimestamp());
            List<BountyDto.Reward> rewards = new ArrayList<>();
            for (BountyEvent.Reward reward : event.getRewards()) {
                BountyDto.Reward r = new BountyDto.Reward();
                r.setFaction(reward.getFaction());
                r.setReward(reward.getReward());
                rewards.add(r);
            }
            sessionData.setRewards(rewards);
            playerSession.addBounty(sessionData);

            StringBuilder sb = new StringBuilder();
            Set<String> targetFactions = missionManager.getTargetFactions(
                    missionManager.getPirateMissionTypes()
            );
            if (!targetFactions.isEmpty() && targetFactions.contains(event.getVictimFaction())) {
                sb.append(localizedEvent("event.bounty.missionKill")).append(" ");
            } else {
                sb.append(localizedEvent("event.bounty.killConfirmed")).append(" ");
            }

            long bountyCollected = rewards.stream().mapToLong(r -> r.getReward()).sum();
            if (!rewards.isEmpty()) sb.append(localizedEvent("event.bounty.claimed", bountyCollected));
            playerSession.addBountyReward(event.getTotalReward());
            EventNarrator.critical(sb.toString());
        });
    }
}
