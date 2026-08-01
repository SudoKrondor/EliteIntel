package elite.intel.gameapi.missions;

import elite.intel.gameapi.journal.events.dto.BountyDto;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers what the extraction ADDED on top of the behaviour the characterization
 * tests pin: killsRequired and killsDone, which the HUD progress bar needs and
 * the voice query never used.
 * <p>
 * The bar is current/max, so getting killsRequired wrong shows a confidently
 * wrong "4 of 12" with nothing else to catch it.
 */
class MassacreProgressTest {

    @Test
    void noMissionsMeansNothingToShow() {
        MassacreProgress progress = MassacreProgress.compute(List.of(), List.of());

        assertFalse(progress.hasMissions());
        assertEquals(0, progress.killsRequired());
        assertNull(progress.targetFaction());
    }

    @Test
    @DisplayName("a fresh stack requires the longest chain, not the sum of its missions")
    void requiredIsDrivenByTheLongestChainNotTheSum() {
        // One kill advances all three providers, so 10+8+6 costs 10, not 24.
        List<MissionDto> stack = List.of(
                mission(1, "Alpha", "Pirates", 10, "2026-07-01T00:00:00Z"),
                mission(2, "Beta", "Pirates", 8, "2026-07-01T00:00:00Z"),
                mission(3, "Gamma", "Pirates", 6, "2026-07-01T00:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(stack, List.of());

        assertEquals(10, progress.killsRequired());
        assertEquals(10, progress.killsRemaining());
        assertEquals(0, progress.killsDone(), "nothing killed yet");
    }

    @Test
    @DisplayName("two missions from one provider queue, so their counts add up")
    void sameProviderMissionsAddUp() {
        List<MissionDto> stack = List.of(
                mission(1, "Alpha", "Pirates", 5, "2026-07-01T00:00:00Z"),
                mission(2, "Alpha", "Pirates", 7, "2026-07-01T00:00:00Z"));

        assertEquals(12, MassacreProgress.compute(stack, List.of()).killsRequired());
    }

    @Test
    @DisplayName("killsDone tracks kills already made and the bar never exceeds its max")
    void progressAdvancesWithKills() {
        List<MissionDto> stack = List.of(mission(1, "Alpha", "Pirates", 10, "2026-07-01T00:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(
                stack, bounties("Pirates", 4, "2026-07-01T01:00:00Z"));

        assertEquals(10, progress.killsRequired());
        assertEquals(6, progress.killsRemaining());
        assertEquals(4, progress.killsDone());
        assertTrue(progress.killsDone() <= progress.killsRequired());
    }

    @Test
    @DisplayName("required stays fixed as kills come in, so the bar's max does not move under it")
    void requiredDoesNotShrinkAsProgressIsMade() {
        List<MissionDto> stack = List.of(
                mission(1, "Alpha", "Pirates", 10, "2026-07-01T00:00:00Z"),
                mission(2, "Beta", "Pirates", 8, "2026-07-01T00:00:00Z"));

        long fresh = MassacreProgress.compute(stack, List.of()).killsRequired();
        long partway = MassacreProgress.compute(
                stack, bounties("Pirates", 5, "2026-07-01T01:00:00Z")).killsRequired();

        assertEquals(fresh, partway, "a moving max would make the bar jump backwards");
    }

    @Test
    void aCompletedStackReportsFullProgress() {
        List<MissionDto> stack = List.of(mission(1, "Alpha", "Pirates", 3, "2026-07-01T00:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(
                stack, bounties("Pirates", 3, "2026-07-01T01:00:00Z"));

        assertEquals(0, progress.killsRemaining());
        assertEquals(progress.killsRequired(), progress.killsDone());
    }

    /**
     * The overlay recomputes this every second, so a row it cannot read must degrade to the
     * documented default rather than throw. Throwing would blank the card and raise the same
     * exception once a second for as long as the row exists.
     */
    @Test
    void anUnreadableTimestampFallsBackInsteadOfThrowing() {
        List<MissionDto> stack = List.of(mission(1, "Alpha", "Pirates", 4, "not-a-timestamp"));
        List<BountyDto> kills = bounties("Pirates", 1, "2026-07-01T01:00:00Z");
        kills.getFirst().setEarnedAt("also-not-a-timestamp");

        MassacreProgress progress = MassacreProgress.compute(stack, kills);

        assertTrue(progress.hasMissions());
        assertEquals(4, progress.killsRequired());
        // Unreadable accept time sorts as epoch and unreadable earn time as MAX, so the kill counts.
        assertEquals(1, progress.killsDone());
    }

    // -- fixtures ------------------------------------------------------------

    private static MissionDto mission(long id, String provider, String target, int kills, String acceptedAt) {
        String json = "{"
                + "\"missionId\":" + id + ","
                + "\"faction\":\"" + provider + "\","
                + "\"missionTargetFaction\":\"" + target + "\","
                + "\"killCount\":" + kills + ","
                + "\"acceptedAt\":\"" + acceptedAt + "\","
                + "\"reward\":0}";
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
