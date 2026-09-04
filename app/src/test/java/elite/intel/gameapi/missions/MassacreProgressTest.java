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
    @DisplayName("only the game's redirect completes a stack, and then it reports full progress")
    void aRedirectedStackReportsFullProgress() {
        List<MissionDto> stack = List.of(
                redirected(mission(1, "Alpha", "Pirates", 3, "2026-07-01T00:00:00Z"), "2026-07-01T01:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(
                stack, bounties("Pirates", 3, "2026-07-01T01:00:00Z"));

        assertEquals(0, progress.killsRemaining());
        assertEquals(progress.killsRequired(), progress.killsDone());
        assertEquals(1, progress.completedByFaction().get("Alpha"));
    }

    /**
     * The bug this guards: a Bounty is not proof of mission credit - an assisted kill still pays a
     * voucher - so counting bounties overshoots. Twelve bounties against a twelve-kill contract
     * showed COMPLETE on the HUD while the game still wanted two more.
     */
    @Test
    @DisplayName("bounties alone never complete a mission, however many of them there are")
    void bountiesAloneCannotCompleteAMission() {
        List<MissionDto> stack = List.of(mission(1, "Alpha", "Pirates", 12, "2026-07-01T00:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(
                stack, bounties("Pirates", 20, "2026-07-01T01:00:00Z"));

        assertEquals(1, progress.killsRemaining(), "held one short of done until the game confirms");
        assertEquals(11, progress.killsDone());
        assertEquals(0, progress.completedByFaction().get("Alpha"));
    }

    @Test
    @DisplayName("a redirect completes the mission even with no bounties recorded against it")
    void aRedirectCompletesWithoutBounties() {
        List<MissionDto> stack = List.of(
                redirected(mission(1, "Alpha", "Pirates", 8, "2026-07-01T00:00:00Z"), "2026-07-01T04:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(stack, List.of());

        assertEquals(0, progress.killsRemaining());
        assertEquals(8, progress.killsDone());
    }

    @Test
    @DisplayName("the redirect rolls the provider onto its next mission, which starts from zero")
    void aRedirectRollsOverToTheQueuedMission() {
        // Alpha's first mission was confirmed done at 03:00; the five kills that finished it must
        // not also count toward the second, which only started counting when the first ended.
        List<MissionDto> stack = List.of(
                redirected(mission(1, "Alpha", "Pirates", 5, "2026-07-01T00:00:00Z"), "2026-07-01T03:00:00Z"),
                mission(2, "Alpha", "Pirates", 7, "2026-07-01T00:00:00Z"));
        List<BountyDto> kills = new ArrayList<>(bounties("Pirates", 5, "2026-07-01T01:00:00Z"));
        kills.addAll(bounties("Pirates", 2, "2026-07-01T05:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(stack, kills);

        assertEquals(12, progress.killsRequired());
        assertEquals(5, progress.killsRemainingByFaction().get("Alpha"), "7 needed, 2 made since the roll-over");
        assertEquals(1, progress.completedByFaction().get("Alpha"));
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

    /**
     * Replays real journal history: two Clan of LHS 1050 contracts from League of Seediansi
     * (4 kills and 8 kills, both accepted 2026-08-08T00:37) killed out back to back.
     * <p>
     * The history is worth pinning because it independently confirms both rules this class turns
     * on. The 4-kill mission redirected at 00:44:10, on the 4th bounty. The 8-kill mission then
     * redirected at 01:08:04, one second after the 12th - i.e. exactly eight kills AFTER its
     * predecessor finished, not eight after it was accepted. Same-provider missions really do
     * queue, a redirect really does land on the qualifying kill, and every bounty in that run
     * counted regardless of what the victim was.
     */
    @Test
    @DisplayName("a same-provider queue counts the second mission from the first one's redirect")
    void matchesRealJournalHistoryForAQueuedPair() {
        MissionDto first = redirected(
                mission(1, "League of Seediansi", "Clan of LHS 1050", 4, "2026-08-08T00:37:06Z"),
                "2026-08-08T00:44:10Z");
        MissionDto second = mission(2, "League of Seediansi", "Clan of LHS 1050", 8, "2026-08-08T00:37:24Z");

        // Every bounty of that run up to 00:53:49, when the game showed four kills left on the
        // second contract: four before the redirect, four after it.
        List<BountyDto> kills = killsAt("Clan of LHS 1050",
                "2026-08-08T00:42:24Z", "2026-08-08T00:43:37Z", "2026-08-08T00:43:56Z", "2026-08-08T00:44:10Z",
                "2026-08-08T00:45:37Z", "2026-08-08T00:48:22Z", "2026-08-08T00:50:58Z", "2026-08-08T00:53:49Z");

        MassacreProgress progress = MassacreProgress.compute(List.of(first, second), kills);

        assertEquals(12, progress.killsRequired());
        assertEquals(1, progress.completedByFaction().get("League of Seediansi"));
        assertEquals(4, progress.killsRemainingByFaction().get("League of Seediansi"),
                "the four kills that finished the first contract must not also count toward the second");
        assertEquals(4, progress.killsRemaining());
    }

    // -- what each provider still costs --------------------------------------

    /**
     * The board the overlay was built for. Three providers hunting the same pirates: A holding one
     * 81-kill contract, B two 40s taken from two of its own outposts, C a single 10.
     * <p>
     * The whole stack costs 81 - the longest chain - but that total says nothing about which board
     * to walk up to next, which is the decision in front of a commander building a stack. Per
     * provider it is 81, 80 and 10, and B's 80 is the point: its two contracts queue, so a second
     * mission from a provider already in the stack is paid for in full. That is how a run meant to
     * be 60 kills becomes 200.
     */
    @Test
    @DisplayName("each provider's queue is priced in full, even though the stack shares its kills")
    void everyProviderCarriesItsOwnQueueTotal() {
        MassacreProgress progress = MassacreProgress.compute(theBoard(), List.of());

        assertEquals(81, progress.killsRequired(), "the stack costs its longest chain");
        assertEquals(81, progress.queueRemainingByFaction().get("Faction A"));
        assertEquals(80, progress.queueRemainingByFaction().get("Faction B"), "two 40s queue");
        assertEquals(10, progress.queueRemainingByFaction().get("Faction C"));
    }

    /**
     * Ten kills advance every provider at once - that is what a stack IS - so each queue drops by
     * ten. C stops one short of zero rather than reading complete: a bounty is not proof of mission
     * credit, and only the game's own redirect finishes a contract.
     */
    @Test
    void oneKillAdvancesEveryProvidersQueue() {
        MassacreProgress progress = MassacreProgress.compute(
                theBoard(), bounties("Pirates", 10, "2026-07-01T01:00:00Z"));

        assertEquals(10, progress.killsDone());
        assertEquals(71, progress.queueRemainingByFaction().get("Faction A"));
        assertEquals(70, progress.queueRemainingByFaction().get("Faction B"),
                "its active 40 is down to 30 and the 40 behind it has not started");
        assertEquals(1, progress.queueRemainingByFaction().get("Faction C"),
                "held one short until the game redirects it");
    }

    /**
     * A provider's queued mission is not discounted when the one in front of it finishes: the
     * successor starts at its full count, and the queue total has to keep saying so.
     */
    @Test
    void aFinishedMissionLeavesItsSuccessorAtFullPrice() {
        List<MissionDto> stack = List.of(
                redirected(mission(1, "Alpha", "Pirates", 5, "2026-07-01T00:00:00Z"),
                        "2026-07-01T01:00:00Z"),
                mission(2, "Alpha", "Pirates", 7, "2026-07-01T00:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(stack, List.of());

        assertEquals(7, progress.queueRemainingByFaction().get("Alpha"),
                "the 5 is done; the 7 behind it has not been touched");
    }

    /**
     * Redirect every one of a provider's missions and it owes nothing - which is the only way a
     * provider reads as complete, since kills alone never get there.
     */
    @Test
    void aProviderWithNothingLeftOwesNothing() {
        List<MissionDto> stack = List.of(
                redirected(mission(1, "Alpha", "Pirates", 5, "2026-07-01T00:00:00Z"),
                        "2026-07-01T01:00:00Z"),
                mission(2, "Beta", "Pirates", 9, "2026-07-01T00:00:00Z"));

        MassacreProgress progress = MassacreProgress.compute(stack, List.of());

        assertEquals(0, progress.queueRemainingByFaction().get("Alpha"));
        assertEquals(9, progress.queueRemainingByFaction().get("Beta"));
    }

    /**
     * The card writes the providers in the order this map hands them back and rewrites itself when
     * that order changes, so acceptance order has to survive the computation.
     */
    @Test
    void providersComeBackInTheOrderTheirFirstContractWasAccepted() {
        assertEquals(List.of("Faction A", "Faction B", "Faction C"),
                List.copyOf(MassacreProgress.compute(theBoard(), List.of())
                        .queueRemainingByFaction().keySet()));
    }

    // -- fixtures ------------------------------------------------------------

    /**
     * Faction A: one 81. Faction B: two 40s from two of its outposts. Faction C: one 10.
     */
    private static List<MissionDto> theBoard() {
        return List.of(
                mission(1, "Faction A", "Pirates", 81, "2026-07-01T00:00:00Z"),
                mission(2, "Faction B", "Pirates", 40, "2026-07-01T00:01:00Z"),
                mission(3, "Faction B", "Pirates", 40, "2026-07-01T00:02:00Z"),
                mission(4, "Faction C", "Pirates", 10, "2026-07-01T00:03:00Z"));
    }


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

    /**
     * What {@code MissionRedirectedSubscriber} stamps on a mission when the game announces its
     * objectives are met - here, that the kills are done.
     */
    private static MissionDto redirected(MissionDto mission, String redirectedAt) {
        mission.setRedirectedAt(redirectedAt);
        return mission;
    }

    /**
     * Bounties at specific times, for replaying a real run rather than an evenly spaced one.
     */
    private static List<BountyDto> killsAt(String victimFaction, String... earnedAt) {
        List<BountyDto> out = new ArrayList<>();
        for (int i = 0; i < earnedAt.length; i++) {
            BountyDto b = new BountyDto();
            b.setVictimFaction(victimFaction);
            b.setPilotName("pilot-" + i);
            b.setEarnedAt(earnedAt[i]);
            b.setRewards(List.of());
            out.add(b);
        }
        return out;
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
