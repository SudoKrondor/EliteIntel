package elite.intel.gameapi.bookmarks;

import elite.intel.gameapi.bookmarks.LocationBookmarkFile.Candidate;
import elite.intel.gameapi.bookmarks.LocationBookmarkFile.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The export file: what goes out comes back as the same places, and an import never brings in a place twice.
 */
class LocationBookmarkFileTest {

    private final LocationBookmark camp =
            LocationBookmark.surface("Synuefe KU-F b44-4", "Synuefe KU-F b44-4 A 1", 12.5, -45.25)
                    .withDisplayName("Brain tree cluster");
    private final LocationBookmark engineer = LocationBookmark.planetaryPort("Eurybia", "Eurybia 1 a", "Demolition Unlimited");

    @Test
    void whatIsExportedComesBackAsTheSamePlaces() {
        List<Candidate> back = LocationBookmarkFile.parse(LocationBookmarkFile.toJson(List.of(camp.withId(4), engineer.withId(9))), List.of());

        assertEquals(List.of(camp, engineer), back.stream().map(Candidate::bookmark).toList(),
                "ids stay behind, names and coordinates travel");
        assertTrue(back.stream().allMatch(c -> c.status() == Status.NEW));
    }

    @Test
    void anUnrenamedBookmarkIsWrittenWithoutAName() {
        String json = LocationBookmarkFile.toJson(List.of(engineer));

        assertFalse(json.contains("displayName"), json);
        assertFalse(LocationBookmarkFile.parse(json, List.of()).getFirst().bookmark().isRenamed());
    }

    @Test
    void aPlaceAlreadyBookmarkedIsADuplicateWhateverItIsCalled() {
        String json = LocationBookmarkFile.toJson(List.of(camp, engineer));

        List<Candidate> candidates = LocationBookmarkFile.parse(json, List.of(engineer.withDisplayName("Liz Ryder").withId(3)));

        assertEquals(List.of(Status.NEW, Status.DUPLICATE), candidates.stream().map(Candidate::status).toList());
        assertFalse(candidates.get(1).importable());
    }

    @Test
    void aPlaceListedTwiceInTheFileComesInOnce() {
        String json = LocationBookmarkFile.toJson(List.of(engineer, engineer.withDisplayName("Again")));

        assertEquals(List.of(Status.NEW, Status.DUPLICATE),
                LocationBookmarkFile.parse(json, List.of()).stream().map(Candidate::status).toList());
    }

    @Test
    void anEntryThatIsNotAPlaceIsInvalidButStillShown() {
        String json = """
                {"format": 1, "bookmarks": [
                  {"kind": "SYSTEM"},
                  {"kind": "WORMHOLE", "starSystem": "Sol"},
                  {"kind": "STATION", "starSystem": "Sol"},
                  {"kind": "SURFACE", "starSystem": "Sol", "planetName": "Moon", "latitude": 91.0, "longitude": 0.0, "displayName": "Off the globe"},
                  {"kind": "SYSTEM", "starSystem": "Sol"}
                ]}
                """;

        List<Candidate> candidates = LocationBookmarkFile.parse(json, List.of());

        assertEquals(List.of(Status.INVALID, Status.INVALID, Status.INVALID, Status.INVALID, Status.NEW),
                candidates.stream().map(Candidate::status).toList());
        assertEquals("Off the globe", candidates.get(3).name());
        assertNull(candidates.get(3).bookmark());
    }

    @Test
    void somethingThatIsNotABookmarkFileIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> LocationBookmarkFile.parse("not json at all {", List.of()));
        assertThrows(IllegalArgumentException.class, () -> LocationBookmarkFile.parse("[]", List.of()));
        assertThrows(IllegalArgumentException.class, () -> LocationBookmarkFile.parse("{\"format\": 1}", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> LocationBookmarkFile.parse("{\"format\": 2, \"bookmarks\": []}", List.of()),
                "a newer app's file is not guessed at");
    }
}
