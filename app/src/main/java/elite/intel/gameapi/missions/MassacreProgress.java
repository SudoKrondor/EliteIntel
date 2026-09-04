package elite.intel.gameapi.missions;

import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Progress across a stack of pirate-massacre missions.
 * <p>
 * Extracted from {@code AnalyzePirateMissionQuery} so the HUD overlay and the
 * voice query share one implementation. The query formats prose from this; the
 * overlay draws a progress bar from it.
 * <p>
 * Two rules make this more than a subtraction, and both are load-bearing:
 * <ul>
 *   <li><b>Stacked missions share kills.</b> Every mission in a stack targets
 *       the same faction, so one kill advances all of them at once. The kills
 *       needed for a stack is therefore NOT the sum of the individual counts -
 *       it is driven by the longest chain.</li>
 *   <li><b>Missions from the same provider queue.</b> A faction's second
 *       mission only starts once its first is done, so those DO add up.</li>
 * </ul>
 * A kill only counts toward missions accepted before it was earned, which is
 * what stops pre-existing bounties from completing a freshly taken mission.
 * <p>
 * <b>Bounties over-count, so they can never finish a mission.</b> The journal has
 * no per-kill mission counter: progress here is inferred from {@code Bounty}
 * events, and a bounty is not proof of mission credit - a kill someone else
 * landed the final blow on still pays a voucher, and not every ship flying a
 * faction's flag is one the contract counts. Inferred progress is therefore an
 * upper bound and is held one kill short of done. The one authoritative signal
 * the game gives is {@code MissionRedirected}, which fires when the objectives
 * are met and the mission is sent to its turn-in point; only that completes a
 * mission here, rolls its provider onto the next one, and lets the whole stack
 * read as complete.
 *
 * @param killsRemainingByFaction  kills left on each provider's ACTIVE mission
 * @param queueRemainingByFaction  kills left on everything a provider has given
 *                                 us - its active mission plus the ones queued
 *                                 behind it. This is what a provider still costs
 *                                 the commander, and unlike the stack total it
 *                                 DOES add up, because same-provider missions run
 *                                 one at a time
 * @param completedByFaction       missions each provider has had completed
 * @param killsRemaining           kills still to make to clear the whole stack
 * @param killsRequired            kills the whole stack needs from scratch,
 *                                 so {@code killsRequired - killsRemaining} is
 *                                 progress so far
 * @param targetFaction            the faction being hunted; null when idle
 */
public record MassacreProgress(
        Map<String, Integer> killsRemainingByFaction,
        Map<String, Integer> queueRemainingByFaction,
        Map<String, Integer> completedByFaction,
        long killsRemaining,
        long killsRequired,
        String targetFaction) {

    /**
     * No active massacre missions.
     */
    public static final MassacreProgress NONE =
            new MassacreProgress(Map.of(), Map.of(), Map.of(), 0, 0, null);

    public boolean hasMissions() {
        return targetFaction != null;
    }

    /**
     * Kills already made toward the stack.
     */
    public long killsDone() {
        return Math.max(0, killsRequired - killsRemaining);
    }

    // -- computation ---------------------------------------------------------

    public static MassacreProgress compute(Collection<MissionDto> missions, Collection<BountyDto> bounties) {
        List<MissionDto> sorted = missions.stream()
                .filter(Objects::nonNull)
                .filter(m -> m.getMissionTargetFaction() != null)
                .sorted(Comparator.comparing(MassacreProgress::acceptedAt)
                        .thenComparingLong(MissionDto::getMissionId))
                .toList();
        if (sorted.isEmpty()) return NONE;

        String targetFaction = sorted.getFirst().getMissionTargetFaction();

        Map<String, List<MissionDto>> byProvider = new LinkedHashMap<>();
        for (MissionDto m : sorted) {
            byProvider.computeIfAbsent(m.getFaction(), k -> new ArrayList<>()).add(m);
        }

        // Pristine stack: what the whole run costs from zero kills. The head of
        // each provider's queue becomes its active mission and must be polled
        // off, or it is charged twice - once as active, once still pending.
        Map<String, Deque<MissionDto>> freshPending = pending(byProvider);
        Map<String, Integer> freshActive = new LinkedHashMap<>();
        for (String faction : freshPending.keySet()) {
            freshActive.put(faction,
                    Objects.requireNonNull(freshPending.get(faction).pollFirst()).getKillCount());
        }
        long required = simulateStackCost(byProvider, freshActive, freshPending);

        Map<String, Deque<MissionDto>> pendingByFaction = pending(byProvider);
        Map<String, MissionDto> activeMission = new LinkedHashMap<>();
        Map<String, Integer> activeRemaining = new LinkedHashMap<>();
        Map<String, Integer> completedByFaction = new LinkedHashMap<>();
        Map<String, Instant> activeSince = new LinkedHashMap<>();
        for (String faction : pendingByFaction.keySet()) {
            // Redirected missions are done on the game's own word. Draining them off the front of
            // the queue is what rolls a provider onto its next mission, and the last redirect is
            // when that next mission started counting - kills made before it belonged to its
            // predecessor, since same-provider missions run one at a time.
            Deque<MissionDto> queue = pendingByFaction.get(faction);
            int completed = 0;
            Instant since = Instant.EPOCH;
            MissionDto first = queue.pollFirst();
            while (first != null && first.isObjectivesComplete()) {
                completed++;
                since = redirectedAt(first);
                first = queue.pollFirst();
            }
            completedByFaction.put(faction, completed);
            activeSince.put(faction, since);
            if (first != null) {
                activeMission.put(faction, first);
                activeRemaining.put(faction, first.getKillCount());
            }
        }

        applyHistoricalKills(bounties, targetFaction, activeMission, activeRemaining, activeSince);

        Map<String, Integer> killsRemainingByFaction = new LinkedHashMap<>();
        Map<String, Integer> queueRemainingByFaction = new LinkedHashMap<>();
        for (String faction : byProvider.keySet()) {
            killsRemainingByFaction.put(faction, activeRemaining.getOrDefault(faction, 0));
            // What is left of this provider's queue: its active mission, plus every mission still
            // waiting behind it at full price. The drain loop above already polled the active one
            // off, so what remains in the deque is exactly the not-yet-started tail.
            int queued = pendingByFaction.get(faction).stream().mapToInt(MissionDto::getKillCount).sum();
            queueRemainingByFaction.put(faction, activeRemaining.getOrDefault(faction, 0) + queued);
        }

        long remaining = simulateStackCost(byProvider, new LinkedHashMap<>(activeRemaining), pendingByFaction);

        return new MassacreProgress(
                Collections.unmodifiableMap(killsRemainingByFaction),
                Collections.unmodifiableMap(queueRemainingByFaction),
                Collections.unmodifiableMap(completedByFaction),
                remaining, required, targetFaction);
    }

    /**
     * Applies bounties in chronological order to each provider's active mission.
     * A kill counts only toward a mission that was already accepted - and already
     * the provider's active one - when the bounty was earned.
     * <p>
     * The count stops one short of done: a bounty is evidence of a kill, not of
     * mission credit, so it can drive the bar forward but must never declare the
     * mission finished. Only {@code MissionRedirected} does that, and it has
     * already been applied by the caller.
     */
    private static void applyHistoricalKills(Collection<BountyDto> bounties, String targetFaction,
                                             Map<String, MissionDto> activeMission,
                                             Map<String, Integer> activeRemaining,
                                             Map<String, Instant> activeSince) {
        List<BountyDto> relevant = bounties.stream()
                .filter(Objects::nonNull)
                .filter(b -> targetFaction.equals(b.getVictimFaction()) && b.getPilotName() != null)
                .sorted(Comparator.comparing(MassacreProgress::earnedAt))
                .toList();

        for (BountyDto bounty : relevant) {
            Instant killTime = earnedAt(bounty);
            List<String> eligible = activeMission.entrySet().stream()
                    .filter(e -> e.getValue() != null
                            && acceptedAt(e.getValue()).compareTo(killTime) <= 0
                            // Strictly after: the kill that completes a mission belongs to it, not
                            // to its successor. Journal history is unambiguous - a 4-kill contract
                            // redirected on the bounty at 00:44:10 and its queued 8-kill successor
                            // then took exactly the eight bounties that came later.
                            && activeSince.getOrDefault(e.getKey(), Instant.EPOCH).compareTo(killTime) < 0)
                    .map(Map.Entry::getKey)
                    .toList();
            for (String faction : eligible) {
                int remaining = activeRemaining.get(faction);
                if (remaining > 1) activeRemaining.put(faction, remaining - 1);
            }
        }
    }

    /**
     * Kills needed to clear every remaining mission, given what each provider
     * has left. Because one kill advances all providers at once, this repeatedly
     * takes the smallest remaining count, charges it once, and rolls finished
     * providers onto their next mission.
     */
    private static long simulateStackCost(Map<String, List<MissionDto>> byProvider,
                                          Map<String, Integer> active,
                                          Map<String, Deque<MissionDto>> pendingSource) {
        Map<String, Deque<Integer>> pending = new LinkedHashMap<>();
        for (Map.Entry<String, Deque<MissionDto>> e : pendingSource.entrySet()) {
            Deque<Integer> counts = new LinkedList<>();
            for (MissionDto m : e.getValue()) counts.add(m.getKillCount());
            pending.put(e.getKey(), counts);
        }

        Map<String, Integer> simActive = new LinkedHashMap<>(active);
        long total = 0;
        while (!simActive.isEmpty()) {
            int min = simActive.values().stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
            total += min;
            for (String faction : new ArrayList<>(simActive.keySet())) {
                int left = simActive.get(faction) - min;
                if (left <= 0) {
                    simActive.remove(faction);
                    Deque<Integer> queue = pending.get(faction);
                    if (queue != null && !queue.isEmpty()) simActive.put(faction, queue.pollFirst());
                } else {
                    simActive.put(faction, left);
                }
            }
        }
        return total;
    }

    private static Map<String, Deque<MissionDto>> pending(Map<String, List<MissionDto>> byProvider) {
        Map<String, Deque<MissionDto>> out = new LinkedHashMap<>();
        byProvider.forEach((faction, list) -> out.put(faction, new LinkedList<>(list)));
        return out;
    }

    /**
     * Missing or unreadable accept time = pre-timestamp record; treated as epoch so all kills count.
     */
    private static Instant acceptedAt(MissionDto m) {
        return parseOr(m.getAcceptedAt(), Instant.EPOCH);
    }

    /**
     * When the provider's current mission started counting. Missing or unreadable = epoch, so no
     * kill is excluded on the strength of a timestamp we could not read.
     */
    private static Instant redirectedAt(MissionDto m) {
        return parseOr(m.getRedirectedAt(), Instant.EPOCH);
    }

    /**
     * Missing or unreadable earn time = pre-timestamp record; treated as MAX so it counts toward any mission.
     */
    private static Instant earnedAt(BountyDto b) {
        return parseOr(b.getEarnedAt(), Instant.MAX);
    }

    /**
     * Timestamps are ISO-8601 as the journal writes them, and the fallback for an absent one is
     * already defined per field, so an unreadable one takes the same route rather than throwing.
     * <p>
     * WHY this is not fail-fast: the HUD overlay recomputes this on a timer, so a single unparseable
     * row would otherwise blank the card and raise the same exception once a second for as long as
     * the row exists. {@code MissionObjectiveSource} treats its expiry field the same way.
     */
    private static Instant parseOr(String timestamp, Instant fallback) {
        if (timestamp == null) return fallback;
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
