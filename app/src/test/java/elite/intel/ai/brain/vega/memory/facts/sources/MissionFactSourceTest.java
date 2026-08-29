package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.gameapi.journal.events.dto.MissionDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MissionFactSourceTest {

    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    /**
     * Stored missions are JSON rows, and the DTO is built by deserialising one - the same way the DAO does.
     */
    private static MissionDto mission(String json) {
        return GsonFactory.getGson().fromJson(json, MissionDto.class);
    }

    private static MissionDto delivery() {
        return mission("""
                {"missionDescription":"Deliver gold to Abraham Lincoln","destinationSystem":"Sol",
                 "destinationStation":"Abraham Lincoln","commodityName":"Gold","count":45,
                 "expiry":"2026-08-28T15:30:00Z"}
                """);
    }

    @Test
    void namesTheContractItsDropAndItsDeadline() {
        assertEquals("current mission Deliver gold to Abraham Lincoln: to Sol, Abraham Lincoln, "
                        + "deliver 45 Gold, expires in 3h 30m",
                MissionFactSource.format(delivery(), 1, NOW));
    }

    @Test
    void addsTheStackSizeOnlyWhenMoreThanOneIsAccepted() {
        assertTrue(MissionFactSource.format(delivery(), 8, NOW).endsWith("8 missions accepted"));
        assertFalse(MissionFactSource.format(delivery(), 1, NOW).contains("accepted"));
    }

    @Test
    void reportsAKillContractByItsTargetRatherThanItsCargo() {
        MissionDto massacre = mission("""
                {"missionDescription":"Kill 12 pirates","destinationSystem":"Deciat","killCount":12,
                 "missionTargetFaction":"Deciat Blue Boys"}
                """);

        assertEquals("current mission Kill 12 pirates: to Deciat, kill 12 of Deciat Blue Boys",
                MissionFactSource.format(massacre, 1, NOW));
    }

    @Test
    void reportsPassengersWhenThatIsWhatTheContractCarries() {
        MissionDto tourist = mission("""
                {"missionDescription":"Sightseeing tour","destinationSystem":"Colonia","passengerCount":4}
                """);

        assertEquals("current mission Sightseeing tour: to Colonia, carry 4 passengers",
                MissionFactSource.format(tourist, 1, NOW));
    }

    /**
     * A donation is handed in at the board it was taken from, so the journal gives it no destination at all - the
     * line has to survive that rather than name a system it does not have.
     */
    @Test
    void survivesAMissionWithNowhereToFlyTo() {
        MissionDto donation = mission("""
                {"missionDescription":"Donate 500000 credits"}
                """);

        assertEquals("current mission Donate 500000 credits", MissionFactSource.format(donation, 1, NOW));
    }

    /**
     * A mission that never expires carries no expiry at all; an unparseable one is treated the same way.
     */
    @Test
    void saysNothingAboutADeadlineItDoesNotHave() {
        MissionDto open = delivery();
        open.setExpiry(null);
        assertFalse(MissionFactSource.format(open, 1, NOW).contains("expires"));

        MissionDto broken = delivery();
        broken.setExpiry("not a timestamp");
        assertFalse(MissionFactSource.format(broken, 1, NOW).contains("expires"));
    }

    @Test
    void saysAContractHasRunOutRatherThanCountingBackwards() {
        MissionDto lapsed = delivery();
        lapsed.setExpiry("2026-08-28T11:00:00Z");

        assertTrue(MissionFactSource.format(lapsed, 1, NOW).contains("expired"));
    }

    @Test
    void writesTheRemainingTimeInWholeUnits() {
        assertEquals("2d 3h", MissionFactSource.shortDuration(Duration.ofHours(51)));
        assertEquals("3h 30m", MissionFactSource.shortDuration(Duration.ofMinutes(210)));
        assertEquals("9m", MissionFactSource.shortDuration(Duration.ofMinutes(9)));
        // Under a minute still has time on it; rounding it to "0m" would read as expired.
        assertEquals("1m", MissionFactSource.shortDuration(Duration.ofSeconds(30)));
    }

    /**
     * The journal's own mission name is unbounded and localized. A long one must not push the destination and the
     * deadline off the line, nor carry the whole line past the shared fact cap.
     */
    @Test
    void aLongMissionNameIsShortenedSoTheActionablePartsSurvive() {
        MissionDto verbose = mission("""
                {"missionDescription":"Source 45 units of Hazardous Environment Combat Suits for the Imperial Navy
                 detachment stationed at the Jameson Memorial orbital facility","destinationSystem":"Sol",
                 "destinationStation":"Abraham Lincoln","expiry":"2026-08-28T15:30:00Z"}
                """.replace("\n", " "));

        String fact = MissionFactSource.format(verbose, 1, NOW);

        assertTrue(fact.length() <= FactLine.MAX_CHARS);
        assertTrue(fact.contains("..."), "the reader should be able to tell the name was cut");
        assertTrue(fact.contains("to Sol, Abraham Lincoln"));
        assertTrue(fact.contains("expires in 3h 30m"));
    }

    @Test
    void speaksOnEveryCommanderTurnRatherThanOnASubject() {
        assertTrue(new MissionFactSource().isAmbient());
    }
}
