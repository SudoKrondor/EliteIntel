package elite.intel.ai.brain.actions.handlers.query;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.RegisterQuery;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.handlers.query.struct.AiDataStruct;
import elite.intel.db.managers.MissionManager;
import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.session.PlayerSession;
import elite.intel.util.yaml.ToYamlConvertable;
import elite.intel.util.yaml.YamlFactory;

import java.time.Instant;
import java.util.*;

@RegisterQuery
public class AnalyzePirateMissionQueryCommand extends BaseQueryAnalyzer implements IntelQuery {
    public static final String ID = "query_pirate_mission";


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

    private String computeKillsRemaining(Map<Long, MissionDto> missions, Set<BountyDto> bounties) {
        // Sort all missions by acceptedAt then missionId (oldest first)
        List<MissionDto> sortedMissions = missions.values().stream()
                .filter(m -> m.getMissionTargetFaction() != null)
                .sorted(Comparator.comparing(this::missionAcceptedAt)
                        .thenComparingLong(MissionDto::getMissionId))
                .toList();

        if (sortedMissions.isEmpty()) {
            return "no missions available";
        }

        String targetFaction = sortedMissions.getFirst().getMissionTargetFaction();

        // Group missions by provider faction, preserving chronological order within each faction
        Map<String, List<MissionDto>> missionsByFaction = new LinkedHashMap<>();
        for (MissionDto m : sortedMissions) {
            missionsByFaction.computeIfAbsent(m.getFaction(), k -> new ArrayList<>()).add(m);
        }

        // Per-faction deques of pending missions (already chronologically ordered)
        Map<String, Deque<MissionDto>> pendingByFaction = new LinkedHashMap<>();
        for (Map.Entry<String, List<MissionDto>> e : missionsByFaction.entrySet()) {
            pendingByFaction.put(e.getKey(), new LinkedList<>(e.getValue()));
        }

        // Initialize: first mission per faction becomes active
        Map<String, MissionDto> activeMission = new LinkedHashMap<>();
        Map<String, Integer> activeRemaining = new LinkedHashMap<>();
        Map<String, Integer> completedByFaction = new LinkedHashMap<>();
        for (String faction : pendingByFaction.keySet()) {
            MissionDto first = Objects.requireNonNull(pendingByFaction.get(faction).pollFirst());
            activeMission.put(faction, first);
            activeRemaining.put(faction, first.getKillCount());
            completedByFaction.put(faction, 0);
        }

        // Sort bounties for target faction by earnedAt (oldest first)
        // null earnedAt → Instant.MAX so old records always count toward any mission
        List<BountyDto> targetBounties = bounties.stream()
                .filter(b -> targetFaction.equals(b.getVictimFaction()) && b.getPilotName() != null)
                .sorted(Comparator.comparing(this::bountyEarnedAt))
                .toList();

        // Phase 1: Apply historical kills in chronological order.
        // Each kill applies only to missions whose acceptedAt <= kill's earnedAt.
        // This correctly handles pre-mission bounties and mixed-age stacks.
        for (BountyDto bounty : targetBounties) {
            Instant killTime = bountyEarnedAt(bounty);
            List<String> eligible = activeMission.entrySet().stream()
                    .filter(e -> e.getValue() != null && missionAcceptedAt(e.getValue()).compareTo(killTime) <= 0)
                    .map(Map.Entry::getKey)
                    .toList();
            for (String faction : eligible) {
                int remaining = activeRemaining.get(faction) - 1;
                if (remaining <= 0) {
                    completedByFaction.merge(faction, 1, Integer::sum);
                    Deque<MissionDto> pending = pendingByFaction.get(faction);
                    if (pending != null && !pending.isEmpty()) {
                        MissionDto next = pending.pollFirst();
                        activeMission.put(faction, next);
                        activeRemaining.put(faction, next.getKillCount());
                    } else {
                        activeMission.remove(faction);
                        activeRemaining.remove(faction);
                    }
                } else {
                    activeRemaining.put(faction, remaining);
                }
            }
        }

        // Current kills-remaining per faction for display
        Map<String, Integer> killsRemainingByFaction = new HashMap<>();
        for (String faction : missionsByFaction.keySet()) {
            killsRemainingByFaction.put(faction, activeRemaining.getOrDefault(faction, 0));
        }

        // Phase 2: Simulate future kills needed to finish all remaining missions.
        // All active missions at this point are already accepted, so no timestamp filtering needed.
        Map<String, Integer> simActive = new LinkedHashMap<>(activeRemaining);
        Map<String, Deque<Integer>> simPending = new LinkedHashMap<>();
        for (Map.Entry<String, Deque<MissionDto>> e : pendingByFaction.entrySet()) {
            Deque<Integer> counts = new LinkedList<>();
            for (MissionDto m : e.getValue()) counts.add(m.getKillCount());
            simPending.put(e.getKey(), counts);
        }

        long totalKillsRemaining = 0;
        while (!simActive.isEmpty()) {
            int minRemaining = simActive.values().stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
            totalKillsRemaining += minRemaining;
            List<String> factionsToProcess = new ArrayList<>(simActive.keySet());
            for (String faction : factionsToProcess) {
                int newRemaining = simActive.get(faction) - minRemaining;
                if (newRemaining <= 0) {
                    simActive.remove(faction);
                    Deque<Integer> pending = simPending.get(faction);
                    if (pending != null && !pending.isEmpty()) {
                        simActive.put(faction, pending.pollFirst());
                    }
                } else {
                    simActive.put(faction, newRemaining);
                }
            }
        }

        // Build response string
        List<String> factionSummaries = new ArrayList<>();
        List<String> sortedFactions = new ArrayList<>(missionsByFaction.keySet());
        sortedFactions.sort(String::compareTo);
        for (String faction : sortedFactions) {
            int completed = completedByFaction.getOrDefault(faction, 0);
            int killsRemaining = killsRemainingByFaction.getOrDefault(faction, 0);
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
            sb.append(totalKillsRemaining).append(" kills remain to complete the assignment. Summary: ");
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