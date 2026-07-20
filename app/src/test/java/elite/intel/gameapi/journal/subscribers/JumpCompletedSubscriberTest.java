package elite.intel.gameapi.journal.subscribers;

import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.search.edsm.dto.data.BodyData;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * EDSM body payloads trimmed to the fields that decide where a body's class is recorded. EDSM puts a
 * star's class in spectralClass and a planet's in subType, so reading the wrong one stored stars as
 * planets whose type was a spectral code such as "M5".
 */
class JumpCompletedSubscriberTest {

    @Test
    void aStarsSpectralClassIsRecordedAsItsStarClass() {
        LocationDto body = apply("""
                {"bodyId":1,"name":"Sawait","type":"Star","subType":"M (Red dwarf) Star","spectralClass":"M6"}
                """);

        assertEquals("M6", body.getStarClass());
        assertNull(body.getPlanetClass(), "a star must not be given a planet class");
    }

    @Test
    void aPlanetsSubTypeIsRecordedAsItsPlanetClass() {
        LocationDto body = apply("""
                {"bodyId":4,"name":"Sawait 4","type":"Planet","subType":"High metal content world"}
                """);

        assertEquals("High metal content world", body.getPlanetClass());
        assertNull(body.getStarClass(), "a planet must not be given a star class");
    }

    /**
     * EDSM omits spectralClass on some stars. The body is still a star, so it must not fall through to
     * the planet branch and be labelled with its descriptive subType.
     */
    @Test
    void aStarWithoutASpectralClassIsStillNotTreatedAsAPlanet() {
        LocationDto body = apply("""
                {"bodyId":2,"name":"Sawait B","type":"Star","subType":"Black Hole"}
                """);

        assertNull(body.getPlanetClass(), "a star must not be given a planet class");
    }

    /**
     * The setters are write-once so the richer journal scan wins. EDSM data arrives on every jump and
     * must not overwrite a class already established from the commander's own scan.
     */
    @Test
    void anExistingPlanetClassFromAJournalScanIsNotOverwritten() {
        LocationDto body = new LocationDto(4L);
        body.setPlanetClass("High metal content body");

        JumpCompletedSubscriber.applyBodyClass(body, bodyData("""
                {"bodyId":4,"name":"Sawait 4","type":"Planet","subType":"High metal content world"}
                """));

        assertEquals("High metal content body", body.getPlanetClass());
    }

    @Test
    void aPlanetIsClassifiedFromItsDescriptiveSubType() {
        assertEquals(LocationDto.LocationType.PLANET, classify("""
                {"bodyId":4,"name":"Sawait 4","type":"Planet","subType":"High metal content world",
                 "distanceToArrival":419.35,"parents":[{"Star":1},{"Null":0}]}
                """));
    }

    @Test
    void theMainStarIsThePrimaryStar() {
        assertEquals(LocationDto.LocationType.PRIMARY_STAR, classify("""
                {"bodyId":1,"name":"Sawait","type":"Star","subType":"M (Red dwarf) Star","distanceToArrival":0.0}
                """));
    }

    @Test
    void aSecondaryStarIsAStar() {
        assertEquals(LocationDto.LocationType.STAR, classify("""
                {"bodyId":2,"name":"Sawait B","type":"Star","subType":"K (Yellow-Orange) Star","distanceToArrival":208121.8}
                """));
    }

    /**
     * EDSM has no moon type, so the parent chain is the only thing separating a moon from a planet.
     */
    @Test
    void aBodyOrbitingAPlanetIsAMoonNotAPlanet() {
        assertEquals(LocationDto.LocationType.MOON, classify("""
                {"bodyId":5,"name":"Sawait 4 a","type":"Planet","subType":"Rocky body",
                 "distanceToArrival":421.0,"parents":[{"Planet":4},{"Star":1},{"Null":0}]}
                """));
    }

    /**
     * A body EDSM cannot describe must yield null so the caller leaves the stored type alone. Persisting
     * the null is what wiped journal-classified bodies on every jump.
     */
    @Test
    void aBodyWithNoSubTypeIsNotClassified() {
        assertNull(classify("""
                {"bodyId":9,"name":"Sawait 9","type":"Planet","distanceToArrival":100.0}
                """));
    }

    private static LocationDto.LocationType classify(String edsmBodyJson) {
        return JumpCompletedSubscriber.classifyEdsmBody(bodyData(edsmBodyJson));
    }

    private static LocationDto apply(String edsmBodyJson) {
        LocationDto body = new LocationDto(4L);
        JumpCompletedSubscriber.applyBodyClass(body, bodyData(edsmBodyJson));
        return body;
    }

    private static BodyData bodyData(String edsmBodyJson) {
        return GsonFactory.getGson().fromJson(edsmBodyJson, BodyData.class);
    }
}
