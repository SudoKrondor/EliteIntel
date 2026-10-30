package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaNarrator;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.ai.mouth.EventNarrator;
import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.BountyEvent;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.TTSFriendlyNumberConverter;
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
            if (!rewards.isEmpty())
                sb.append(localizedEvent("event.bounty.claimed", TTSFriendlyNumberConverter.formatCreditsForSpeech(bountyCollected)));
            playerSession.addBountyReward(event.getTotalReward());
            announceKill(event, sb.toString());
        });
    }

    /**
     * The kill-confirmed callout, preceded by VEGA's gloat when the commander turned that on.
     * <p>
     * The gloat goes first because it reads as the reaction and the confirmation as the record. Both are queued
     * on VEGA's event lane, which runs one thought at a time in order, so the confirmation waits for the gloat's
     * LLM round and is voiced after it - on the ordinary voice, since the mission-critical one would be spoken
     * at once, ahead of the gloat. A failed round only loses the gloat. With VEGA not running there is nothing
     * to gloat with, and the confirmation keeps its critical voice.
     */
    private static void announceKill(BountyEvent event, String confirmation) {
        VegaNarrator narrator = VegaRuntime.narrator();
        if (narrator == VegaNarrator.NO_OP || !GlobalSettingsManager.getInstance().getAiBountyKillGloat()) {
            EventNarrator.critical(confirmation);
            return;
        }
        gloat(narrator, event);
        narrator.announce(confirmation, false);
    }

    /**
     * VEGA gloating over the wreck, phrased fresh by the LLM each time.
     */
    private static void gloat(VegaNarrator narrator, BountyEvent event) {
        String ship = event.getTargetLocalised() != null && !event.getTargetLocalised().isBlank()
                ? event.getTargetLocalised()
                : event.getTarget();
        StringBuilder data = new StringBuilder("We destroyed a wanted ship");
        if (ship != null && !ship.isBlank()) data.append(". Ship: ").append(ship.replace('_', ' '));
        if (event.getPilotNameLocalised() != null && !event.getPilotNameLocalised().isBlank())
            data.append(". Pilot: ").append(event.getPilotNameLocalised());
        if (event.getVictimFaction() != null && !event.getVictimFaction().isBlank())
            data.append(". Faction: ").append(event.getVictimFaction());
        data.append('.');
        narrator.narrate(data.toString(),
                "Gloat over the defeated target with one short, sarcastic and vitriolic remark. "
                        + "Mock the dead pilot or their ship, never the commander. One sentence, no profanity. "
                        + "Do not repeat the bounty amount.");
    }

    private static List<String> missingIdentityComponents(BountyEvent event) {
        List<String> missing = new ArrayList<>(3);
        if (event.getPilotName() == null || event.getPilotName().isBlank()) missing.add("PilotName");
        if (event.getTarget() == null || event.getTarget().isBlank()) missing.add("Target");
        if (event.getVictimFaction() == null || event.getVictimFaction().isBlank()) missing.add("VictimFaction");
        return missing;
    }
}
