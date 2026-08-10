package elite.intel.gameapi.search.edsm.dto.data;

import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These two classes are read from EDSM's JSON by Gson and written out as YAML by Jackson, and that
 * YAML is narration source: {@code StartJumpSubscriber} hands it to the companion on approach, so a
 * property name here is a word the commander hears. Both directions are pinned, because the two sets
 * of annotations are easy to change one without the other.
 */
class TrafficAndDeathsYamlTest {

    /**
     * The shape EDSM actually returns: total/week/day.
     */
    private static final String EDSM_JSON = """
            {"total":12345,"week":67,"day":3}
            """;

    @Test
    @DisplayName("traffic is spoken as words, and the daily count appears once")
    void trafficYamlIsSpeakable() {
        TrafficStats stats = GsonFactory.getGson().fromJson(EDSM_JSON, TrafficStats.class);
        String yaml = stats.toYaml();

        assertTrue(yaml.contains("this week: 67"), yaml);
        assertFalse(yaml.contains("thisWeek"), "run together, this is spoken as one word: " + yaml);
        assertTrue(yaml.contains("today: 3"), yaml);
        // The field is `today` but its getter is `getDay`, which Jackson used to publish as a second
        // property carrying the same number. Matched a line at a time because "today: 3" contains
        // "day: 3" — a substring check here passes whatever the code does.
        assertFalse(yaml.lines().anyMatch(line -> line.strip().equals("day: 3")),
                "the daily figure must not be sent twice: " + yaml);
    }

    @Test
    @DisplayName("deaths is spoken as words")
    void deathsYamlIsSpeakable() {
        DeathsStats stats = GsonFactory.getGson().fromJson(EDSM_JSON, DeathsStats.class);
        String yaml = stats.toYaml();

        assertTrue(yaml.contains("this week: 67"), yaml);
        assertFalse(yaml.contains("thisWeek"), yaml);
        assertTrue(yaml.contains("today: 3"), yaml);
    }

    @Test
    @DisplayName("naming the fields for speech did not break reading EDSM's JSON")
    void gsonStillParsesEdsmShape() {
        TrafficStats traffic = GsonFactory.getGson().fromJson(EDSM_JSON, TrafficStats.class);
        assertEquals(12345, traffic.getTotal());
        assertEquals(67, traffic.getThisWeek());
        assertEquals(3, traffic.getDay());

        DeathsStats deaths = GsonFactory.getGson().fromJson(EDSM_JSON, DeathsStats.class);
        assertEquals(12345, deaths.getTotal());
        assertEquals(67, deaths.getThisWeek());
        assertEquals(3, deaths.getToday());
    }
}
