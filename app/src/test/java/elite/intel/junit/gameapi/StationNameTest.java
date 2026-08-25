package elite.intel.junit.gameapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.dao.ConstructionSiteDao;
import elite.intel.gameapi.StationName;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.SupercruiseDestinationDropEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the one journal shape that made a UI symbol reach the HUD and the companion's mouth: a colonisation
 * ship names itself "$EXT_PANEL_ColonisationShip; Schroter's Progress", and the commander knows it as
 * "Schroter's Progress".
 */
class StationNameTest {

    @Test
    void peelsTheSymbolOffAColonisationShip() {
        assertEquals("Schroter's Progress",
                StationName.display("$EXT_PANEL_ColonisationShip; Schroter's Progress"));
    }

    @Test
    void spellsOutTheSymbolWhenItIsTheWholeName() {
        assertEquals("Colonisation Ship", StationName.display("$EXT_PANEL_ColonisationShip;"));
    }

    @Test
    void ordinaryStationNamesPassThroughUntouched() {
        assertEquals("Jameson Memorial", StationName.display("Jameson Memorial"));
        assertEquals("K7Q-BQL", StationName.display("K7Q-BQL"));
        // A name that happens to start with a dollar but is not the decorated shape is left alone.
        assertEquals("$100 Outpost", StationName.display("$100 Outpost"));
    }

    @Test
    void nullAndBlankSurvive() {
        assertNull(StationName.display(null));
        assertEquals("   ", StationName.display("   "));
    }

    /**
     * The journal line as the game actually wrote it, from the session that reported this: what the events
     * hand downstream is the name, not the symbol, so the HUD card and the memory line cannot show it.
     */
    @Test
    void dockingAtAColonisationShipCarriesTheNameDownstream() {
        DockedEvent docked = new DockedEvent(json("{ \"timestamp\":\"2026-08-24T20:54:36Z\", "
                + "\"event\":\"Docked\", "
                + "\"StationName\":\"$EXT_PANEL_ColonisationShip; Schroter's Progress\", "
                + "\"StationType\":\"SurfaceStation\", "
                + "\"StarSystem\":\"Hyades Sector MH-V c2-8\", "
                + "\"SystemAddress\":2283077046962, \"MarketID\":3962332162 }"));

        assertEquals("Schroter's Progress", docked.getStationName());
        assertEquals("docked at Schroter's Progress in Hyades Sector MH-V c2-8", docked.memorySummary());
    }

    @Test
    void droppingAtAColonisationShipCarriesTheName() {
        SupercruiseDestinationDropEvent drop = new SupercruiseDestinationDropEvent(
                json("{ \"timestamp\":\"2026-08-24T20:53:17Z\", \"event\":\"SupercruiseDestinationDrop\", "
                        + "\"Type\":\"$EXT_PANEL_ColonisationShip; Schroter's Progress\", "
                        + "\"Threat\":0, \"MarketID\":3962332162 }"));

        assertEquals("Schroter's Progress", drop.getType());
    }

    private static JsonObject json(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    /**
     * Rows written before the events were normalised still hold the decorated name, and that is what the
     * HUD card and "setting course for" were reading. They peel it off on the way out.
     */
    @Test
    void storedRowsPeelTheSymbolOnRead() {
        ConstructionSiteDao.Site site = new ConstructionSiteDao.Site();
        site.setStationName("$EXT_PANEL_ColonisationShip; Schroter's Progress");
        assertEquals("Schroter's Progress", site.getStationName());

        LocationDto location = new LocationDto(1L);
        location.setStationName("$EXT_PANEL_ColonisationShip; Schroter's Progress");
        assertEquals("Schroter's Progress", location.getStationName());
        assertTrue(location.isAtStation("Schroter's Progress"));
    }
}
