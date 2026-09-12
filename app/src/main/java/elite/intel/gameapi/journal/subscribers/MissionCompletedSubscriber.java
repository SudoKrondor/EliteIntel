package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.SpokenAmounts;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.PirateMassacreContract;
import elite.intel.session.PlayerSession;

import static elite.intel.gameapi.MissionType.MISSION_PIRATE_MASSACRE;
import static elite.intel.gameapi.MissionType.MISSION_PIRATE_MASSACRE_WING;
import static elite.intel.util.StringUtls.removeNameEnding;

@SuppressWarnings("unused")
public class MissionCompletedSubscriber {
    private MissionManager missionManager = MissionManager.getInstance();
    private PlayerSession playerSession = PlayerSession.getInstance();
    private HuntingGroundManager huntingGrounds = HuntingGroundManager.getInstance();

    @Subscribe
    public void onMissionCompletedEvent(MissionCompletedEvent event) {
        Thread.ofVirtual().start(() -> {
            recordHuntingGroundVisit(event);

            MissionDto mission = playerSession.getMission(event.getMissionID());
            if (mission == null) {
                return; // no mission in session storage. just exit silently - nothing to do.
            }

            MissionType missionType = missionManager.getMissionType(removeNameEnding(event.getName()));
            if (MISSION_PIRATE_MASSACRE.equals(missionType) || MISSION_PIRATE_MASSACRE_WING.equals(missionType)) {
                playerSession.removeMission(event.getMissionID());
                String targetFaction = event.getTargetFaction();
                VegaRuntime.narrator().narrate("Notify: Mission against Faction \"" + targetFaction + "\" Completed: " + withSpokenReward(event),
                        "Notify user of a successful mission completion, provide detailed summary from the data received." + SpokenAmounts.RULE);
            } else {
                missionManager.remove(event.getMissionID());
                String missionDetails = event.getLocalisedName();
                VegaRuntime.narrator().narrate("Notify: Mission \"" + missionDetails + "\" Completed: " + withSpokenReward(event),
                        "Summarize key mission parameters, destination, reward, and fields relevant to the missiontype. Ignore unimportant fields such as timestamps, timeToLive, missionID etc" + SpokenAmounts.RULE);
            }
        });
    }

    /**
     * The event plus its reward as it should be said, so the model neither reads the digits nor rounds them
     * its own way (see {@link SpokenAmounts}).
     */
    private static String withSpokenReward(MissionCompletedEvent event) {
        return event + SpokenAmounts.yamlLine("reward", event.getReward());
    }

    /**
     * Marks the recorded contract done, which is what tells the pair search that this provider and
     * this hunting ground actually worked rather than merely being tried.
     * <p>
     * Runs whether or not the mission is still in the session log: a contract accepted before the app
     * was started is absent from the log but present in the ledger, and its completion is still worth
     * recording.
     */
    private void recordHuntingGroundVisit(MissionCompletedEvent event) {
        if (!PirateMassacreContract.isOne(event.getName(), event.getTargetType())) return;
        huntingGrounds.recordContractCompleted(event.getMissionID(), event.getTimestamp());
    }
}