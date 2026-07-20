package elite.intel.gameapi.journal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.ScanEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Body-classification cases taken verbatim from a real journal (Stuelou AF-P d6-996), a binary system
 * whose inner planets sit above 1000 K. A surface-temperature heuristic used to promote those planets
 * to stars, so the system reported seven stars instead of two.
 */
class ScanBodyClassifierTest {

    @Test
    void aBarycentreOrbitingStarAtTheArrivalPointIsThePrimaryStar() {
        assertEquals(LocationDto.LocationType.PRIMARY_STAR, classify("""
                {"timestamp":"2026-07-20T01:44:59Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 A","BodyID":1,
                 "Parents":[{"Null":0}],"StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,
                 "DistanceFromArrivalLS":0.0,"StarType":"A","SurfaceTemperature":7879.0}
                """));
    }

    @Test
    void theSecondaryStarOfABinaryIsAStar() {
        assertEquals(LocationDto.LocationType.STAR, classify("""
                {"timestamp":"2026-07-20T01:45:00Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 B","BodyID":2,
                 "Parents":[{"Null":0}],"StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,
                 "DistanceFromArrivalLS":208121.801464,"StarType":"K","SurfaceTemperature":4905.0}
                """));
    }

    @Test
    void aPlanetHotterThan1000KIsStillAPlanet() {
        assertEquals(LocationDto.LocationType.PLANET, classify("""
                {"timestamp":"2026-07-20T01:46:27Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 A 2","BodyID":4,
                 "Parents":[{"Star":1},{"Null":0}],"StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,
                 "DistanceFromArrivalLS":419.356817,"PlanetClass":"High metal content body","SurfaceTemperature":1518.012329}
                """));
    }

    @Test
    void aPlanetOrbitingTheSecondaryStarIsAPlanetNotAStar() {
        assertEquals(LocationDto.LocationType.PLANET, classify("""
                {"timestamp":"2026-07-20T01:47:35Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 B 1","BodyID":28,
                 "Parents":[{"Star":2},{"Null":0}],"StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,
                 "DistanceFromArrivalLS":208114.825269,"PlanetClass":"Metal rich body","SurfaceTemperature":1559.665161}
                """));
    }

    @Test
    void aBodyOrbitingAPlanetIsAMoon() {
        assertEquals(LocationDto.LocationType.MOON, classify("""
                {"timestamp":"2026-07-20T01:46:19Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 A 2 a","BodyID":5,
                 "Parents":[{"Planet":4},{"Star":1},{"Null":0}],"StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,
                 "DistanceFromArrivalLS":421.008952,"PlanetClass":"Rocky body","SurfaceTemperature":425.916138}
                """));
    }

    @Test
    void aBeltClusterIsABeltCluster() {
        assertEquals(LocationDto.LocationType.BELT_CLUSTER, classify("""
                {"timestamp":"2026-07-20T01:46:59Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 B B Belt Cluster 1","BodyID":30,
                 "Parents":[{"Ring":29},{"Star":2},{"Null":0}],"StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,
                 "DistanceFromArrivalLS":208522.926502}
                """));
    }

    @Test
    void aBeltClusterIsRecognisedFromItsNameAloneWithoutParents() {
        assertEquals(LocationDto.LocationType.BELT_CLUSTER, classify("""
                {"timestamp":"2026-07-20T01:46:59Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 B B Belt Cluster 1","BodyID":30,
                 "StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,"DistanceFromArrivalLS":208522.926502}
                """));
    }

    @Test
    void aRingIsARingRatherThanTheMoonItsPlanetParentWouldImply() {
        assertEquals(LocationDto.LocationType.PLANETARY_RING, classify("""
                {"timestamp":"2026-07-20T01:46:58Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 B 1 A Ring","BodyID":29,
                 "Parents":[{"Planet":28},{"Star":2},{"Null":0}],"StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,
                 "DistanceFromArrivalLS":208114.825269}
                """));
    }

    @Test
    void aBodyWithNoStarTypeAndNoParentsIsUnclassified() {
        assertEquals(LocationDto.LocationType.UNCLASSIFIED, classify("""
                {"timestamp":"2026-07-20T01:46:58Z","event":"Scan","BodyName":"Stuelou AF-P d6-996 C","BodyID":99,
                 "StarSystem":"Stuelou AF-P d6-996","SystemAddress":34231400926907,"DistanceFromArrivalLS":12.5}
                """));
    }

    private static LocationDto.LocationType classify(String journalLine) {
        JsonObject json = JsonParser.parseString(journalLine).getAsJsonObject();
        return ScanBodyClassifier.classify(new ScanEvent(json));
    }
}