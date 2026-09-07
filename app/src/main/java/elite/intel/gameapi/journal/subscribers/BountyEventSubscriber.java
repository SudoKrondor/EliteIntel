package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.BountyEvent;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.session.PlayerSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static elite.intel.util.StringUtls.localizedEvent;

@SuppressWarnings("unused")
public class BountyEventSubscriber {

    private static final Logger log = LogManager.getLogger(BountyEventSubscriber.class);
    private final PlayerSession playerSession = PlayerSession.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();

    @Subscribe
    public void onBountyEvent(BountyEvent event) {
        Thread.ofVirtual().start(() -> {
            ShipScanIdentity.fromRaw(event.getPilotName(), event.getTarget(), event.getVictimFaction())
                    .ifPresentOrElse(
                            identity -> playerSession.removeShipScan(identity.key()),
                            () -> log.debug(
                                    "Skipped targeted ship-scan eviction for Bounty event; unavailable raw identity component(s): {}",
                                    missingIdentityComponents(event)));

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

    private static List<String> missingIdentityComponents(BountyEvent event) {
        List<String> missing = new ArrayList<>(3);
        if (event.getPilotName() == null || event.getPilotName().isBlank()) missing.add("PilotName");
        if (event.getTarget() == null || event.getTarget().isBlank()) missing.add("Target");
        if (event.getVictimFaction() == null || event.getVictimFaction().isBlank()) missing.add("VictimFaction");
        return missing;
    }
}
