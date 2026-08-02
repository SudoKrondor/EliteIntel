package elite.intel.ai.brain.actions.handlers.queries;

import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterization tests: these pin the CURRENT behaviour of the massacre
 * kill simulation before it is extracted for reuse by the HUD overlay. They are
 * written against the existing implementation and must keep passing, unchanged,
 * afterwards - that is the whole point of them.
 * <p>
 * The rule that makes this non-trivial: every stacked mission targets the same
 * faction, so one kill counts toward all of them at once. Total kills needed is
 * therefore NOT the sum of the individual kill counts.
 */
class MassacreKillsCharacterizationTest {

    private final AnalyzePirateMissionQuery query = new AnalyzePirateMissionQuery();

    @Test
    void noMissionsAtAll() {
        assertEquals("no missions available", compute(List.of(), List.of()));
    }

    @Test
    @DisplayName("a single untouched mission needs its full kill count")
    void singleMissionNoKills() {
        MissionDto m = mission(1, "Providers", "Pirates", 10, "2026-07-01T00:00:00Z");

        assertEquals("10 kills remain to complete the assignment. Summary: Providers 10 Kills remaining",
                compute(List.of(m), List.of()));
    }

    @Test
    @DisplayName("kills earned after acceptance count against the mission")
    void killsApplyChronologically() {
        MissionDto m = mission(1, "Providers", "Pirates", 10, "2026-07-01T00:00:00Z");
        List<BountyDto> kills = bounties("Pirates", 4, "2026-07-01T01:00:00Z");

        assertEquals("6 kills remain to complete the assignment. Summary: Providers 6 Kills remaining",
                compute(List.of(m), kills));
    }

    @Test
    @DisplayName("kills earned BEFORE the mission was accepted do not count")
    void killsBeforeAcceptanceAreIgnored() {
        MissionDto m = mission(1, "Providers", "Pirates", 10, "2026-07-01T12:00:00Z");
        List<BountyDto> early = bounties("Pirates", 4, "2026-07-01T06:00:00Z");

        assertEquals("10 kills remain to complete the assignment. Summary: Providers 10 Kills remaining",
                compute(List.of(m), early));
    }

    @Test
    @DisplayName("one kill advances every faction's mission at once, so the total is not the sum")
    void stackedMissionsShareKills() {
        // 10 + 8 + 6 individually, but a single kill counts for all three:
        // the run finishes when the largest is done.
        MissionDto a = mission(1, "Alpha", "Pirates", 10, "2026-07-01T00:00:00Z");
        MissionDto b = mission(2, "Beta", "Pirates", 8, "2026-07-01T00:00:00Z");
        MissionDto c = mission(3, "Gamma", "Pirates", 6, "2026-07-01T00:00:00Z");

        assertEquals("10 kills remain to complete the assignment. "
                        + "Summary: Alpha 10 Kills remaining. Beta 8 Kills remaining. Gamma 6 Kills remaining",
                compute(List.of(a, b, c), List.of()));
    }

    @Test
    @DisplayName("two missions from the same faction run one after the other, not together")
    void sameFactionMissionsQueue() {
        // Same provider: the second only starts once the first is finished, so
        // the totals add up rather than overlapping.
        MissionDto first = mission(1, "Alpha", "Pirates", 5, "2026-07-01T00:00:00Z");
        MissionDto second = mission(2, "Alpha", "Pirates", 7, "2026-07-01T02:00:00Z");

        assertEquals("12 kills remain to complete the assignment. Summary: Alpha 5 Kills remaining",
                compute(List.of(first, second), List.of()));
    }

    @Test
    @DisplayName("finishing a mission rolls the faction onto its next one and reports the completion")
    void completedMissionRollsOver() {
        MissionDto first = mission(1, "Alpha", "Pirates", 5, "2026-07-01T00:00:00Z");
        MissionDto second = mission(2, "Alpha", "Pirates", 7, "2026-07-01T00:00:00Z");
        List<BountyDto> kills = bounties("Pirates", 5, "2026-07-01T03:00:00Z");

        assertEquals("7 kills remain to complete the assignment. "
                        + "Summary: Alpha 7 Kills remaining, one mission completed",
                compute(List.of(first, second), kills));
    }

    @Test
    @DisplayName("kills on a different faction are irrelevant")
    void killsOnAnotherFactionDoNotCount() {
        MissionDto m = mission(1, "Providers", "Pirates", 10, "2026-07-01T00:00:00Z");
        List<BountyDto> wrongTarget = bounties("Smugglers", 6, "2026-07-01T01:00:00Z");

        assertEquals("10 kills remain to complete the assignment. Summary: Providers 10 Kills remaining",
                compute(List.of(m), wrongTarget));
    }

    @Test
    @DisplayName("a mission with no accept timestamp accepts historical kills")
    void legacyMissionWithoutTimestamp() {
        MissionDto m = mission(1, "Providers", "Pirates", 10, null);
        List<BountyDto> kills = bounties("Pirates", 3, "2020-01-01T00:00:00Z");

        assertEquals("7 kills remain to complete the assignment. Summary: Providers 7 Kills remaining",
                compute(List.of(m), kills));
    }

    // -- fixtures ------------------------------------------------------------

    private String compute(List<MissionDto> missions, List<BountyDto> bounties) {
        Map<Long, MissionDto> byId = new LinkedHashMap<>();
        for (MissionDto m : missions) byId.put(m.getMissionId(), m);
        return query.computeKillsRemaining(byId, new LinkedHashSet<>(bounties));
    }

    /**
     * MissionDto only constructs from a journal event and exposes no setters for
     * these fields, but it round-trips through JSON in the DB - so JSON is the
     * supported way to build one, and it exercises the same mapping production
     * uses.
     */
    private static MissionDto mission(long id, String provider, String target, int kills, String acceptedAt) {
        String accepted = acceptedAt == null ? "" : "\"acceptedAt\":\"" + acceptedAt + "\",";
        String json = "{"
                + "\"missionId\":" + id + ","
                + "\"faction\":\"" + provider + "\","
                + "\"missionTargetFaction\":\"" + target + "\","
                + "\"killCount\":" + kills + ","
                + accepted
                + "\"reward\":0"
                + "}";
        return GsonFactory.getGson().fromJson(json, MissionDto.class);
    }

    private static List<BountyDto> bounties(String victimFaction, int count, String earnedAt) {
        List<BountyDto> out = new ArrayList<>();
        Instant base = Instant.parse(earnedAt);
        for (int i = 0; i < count; i++) {
            BountyDto b = new BountyDto();
            b.setVictimFaction(victimFaction);
            b.setPilotName("pilot-" + i);
            b.setEarnedAt(base.plusSeconds(i).toString());
            b.setRewards(List.of());
            out.add(b);
        }
        return out;
    }
}
