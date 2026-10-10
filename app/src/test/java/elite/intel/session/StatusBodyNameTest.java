package elite.intel.session;

import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A stored Status.json reading keeps the body its latitude and longitude are on.
 * <p>
 * The field case: landed on Poluskapura A 1, "bookmark this" saved the star system, because the reading came
 * back with the coordinates and no body - so there was no planet to put them on.
 */
class StatusBodyNameTest {

    private final Status status = Status.getInstance();
    private GameEvents.StatusEvent saved;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void remember() {
        saved = status.getStatus();
    }

    @AfterEach
    void restore() {
        status.setStatus(saved);
    }

    @Test
    void theBodyComesBackWithTheCoordinates() {
        GameEvents.StatusEvent landed = status.getStatus();
        landed.setLatitude(-31.264311);
        landed.setLongitude(100.654610);
        landed.setBodyName("Poluskapura A 1");
        status.setStatus(landed);

        GameEvents.StatusEvent read = status.getStatus();
        assertEquals("Poluskapura A 1", read.getBodyName());
        assertEquals(-31.264311, read.getLatitude(), 1e-9);
    }
}
