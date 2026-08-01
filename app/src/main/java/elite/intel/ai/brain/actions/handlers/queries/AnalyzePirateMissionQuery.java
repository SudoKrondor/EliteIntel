package elite.intel.ai.brain.actions.handlers.queries;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.queries.struct.AiDataStruct;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.gameapi.missions.MassacreProgress;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.time.Instant;
import java.util.*;

@RegisterQuery
public class AnalyzePirateMissionQuery extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_pirate_mission";

    @Override
    public String llmDescription() {
        return "Report progress on active pirate-massacre (kill) missions: kills remaining per faction and total, and the total combined reward.";
    }


    @Override public String id() { return ID; }


    private final PlayerSession session = PlayerSession.getInstance();
    private final MissionManager missionManager = MissionManager.getInstance();

    @Override
    public JsonObject handle(String action, JsonObject params, String originalUserInput) {
        Map<Long, MissionDto> missions = missionManager.getMissions(
                missionManager.getPirateMissionTypes()
        );
        Set<BountyDto> bounties = session.getBounties();
        String remainingKills = computeKillsRemaining(missions, bounties);
        String missionProfit = computeMissionProfit(missions, bounties);
        String instructions = """
                Answer the user's question about active pirate kill missions.
                
                Data fields:
                - totalMissionKillsLeft: pre-computed kills remaining per faction and total, formatted as a summary string
                - totalMissionProfit: pre-computed total credit reward from missions and bounties combined
                
                Rules:
                - All values are pre-computed. Do not recalculate.
                - If asked about kills remaining: report totalMissionKillsLeft directly.
                - If asked about profit or reward: report totalMissionProfit directly.
                - Otherwise: report both fields as a brief summary.
                """;
        return process(new AiDataStruct(instructions, new DataDto(remainingKills, missionProfit)), originalUserInput);
    }

    /**
     * Formats the shared {@link MassacreProgress} computation as prose for the
     * LLM. The simulation itself lives in MassacreProgress so the HUD overlay
     * reports the same numbers; only the wording belongs here.
     */
    // Package-private for the characterization tests that pin this wording.
    String computeKillsRemaining(Map<Long, MissionDto> missions, Set<BountyDto> bounties) {
        MassacreProgress progress = MassacreProgress.compute(missions.values(), bounties);
        if (!progress.hasMissions()) {
            return "no missions available";
        }

        List<String> factionSummaries = new ArrayList<>();
        List<String> sortedFactions = new ArrayList<>(progress.killsRemainingByFaction().keySet());
        sortedFactions.sort(String::compareTo);
        for (String faction : sortedFactions) {
            int completed = progress.completedByFaction().getOrDefault(faction, 0);
            int killsRemaining = progress.killsRemainingByFaction().getOrDefault(faction, 0);
            StringBuilder summary = new StringBuilder();
            summary.append(faction).append(" ").append(killsRemaining).append(" Kills remaining");
            if (completed > 0) {
                summary.append(", ").append(completed == 1 ? "one" : completed).append(" mission");
                if (completed > 1) summary.append("s");
                summary.append(" completed");
            }
            factionSummaries.add(summary.toString());
        }

        String summary = String.join(". ", factionSummaries);
        StringBuilder sb = new StringBuilder();
        if (!factionSummaries.isEmpty()) {
            sb.append(progress.killsRemaining()).append(" kills remain to complete the assignment. Summary: ");
        }
        sb.append(summary);
        return sb.toString();
    }

    private Instant missionAcceptedAt(MissionDto m) {
        String ts = m.getAcceptedAt();
        // null = old record with no timestamp; treat as epoch so all bounties count (backward compat)
        return ts != null ? Instant.parse(ts) : Instant.EPOCH;
    }

    private Instant bountyEarnedAt(BountyDto b) {
        String ts = b.getEarnedAt();
        // null = old record with no timestamp; treat as MAX so it counts toward any mission (backward compat)
        return ts != null ? Instant.parse(ts) : Instant.MAX;
    }

    private String computeMissionProfit(Map<Long, MissionDto> missionsMap, Set<BountyDto> bounties) {

        Collection<MissionDto> missions = missionsMap.values();
        long missionReward = 0;
        for (MissionDto mission : missions) {
            missionReward += mission.getReward();
        }

        long bountyReward = 0;
        for (BountyDto bounty : bounties) {
            bountyReward += bounty.getRewards().stream().mapToLong(BountyDto.Reward::getReward).sum();
        }
        return "Total mission profit:" + (missionReward + bountyReward);
    }

    record DataDto(String totalMissionKillsLeft, String totalMissionProfit) implements ToYamlConvertable {
        @Override public String toYaml() {
            return YamlFactory.toYaml(this);
        }
    }
}