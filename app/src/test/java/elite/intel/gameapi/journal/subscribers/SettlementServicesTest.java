package elite.intel.gameapi.journal.subscribers;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * What the model is told about a port's services. Beaufoy Horizons is the reported one: 28 services in the
 * journal, fifteen of them read out in one sentence.
 */
class SettlementServicesTest {

    /** Beaufoy Horizons' {@code StationServices}, verbatim from the journal. */
    private static final List<String> BEAUFOY_HORIZONS = List.of(
            "dock", "autodock", "blackmarket", "commodities", "contacts", "exploration", "missions",
            "outfitting", "crewlounge", "rearm", "refuel", "repair", "tuning", "engineer", "missionsgenerated",
            "flightcontroller", "stationoperations", "powerplay", "searchrescue", "stationMenu", "livery",
            "socialspace", "bartender", "vistagenomics", "pioneersupplies", "apexinterstellar",
            "frontlinesolutions", "registeringcolonisation");

    @BeforeEach
    @AfterEach
    void speakEnglish() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    @Test
    void aBusyPortIsDescribedAsHavingManyRatherThanListed() {
        assertEquals("Many services, including Outfitting, Black Market, Universal Cartographics.",
                SettlementServices.describe(BEAUFOY_HORIZONS));
    }

    @Test
    void noJournalSymbolReachesTheModel() {
        String described = SettlementServices.describe(BEAUFOY_HORIZONS);
        for (String symbol : List.of("missionsgenerated", "stationMenu", "crewlounge", "dock", "contacts")) {
            assertFalse(described.contains(symbol), symbol + " leaked: " + described);
        }
    }

    @Test
    void aSmallPortNamesWhatItHas() {
        assertEquals("Services include Shipyard, Material Trader.",
                SettlementServices.describe(List.of("dock", "refuel", "materialtrader", "shipyard")));
    }

    @Test
    void aDockAndARefuelPadAreNotNews() {
        assertEquals("", SettlementServices.describe(List.of("dock", "refuel", "repair", "contacts")));
        assertEquals("", SettlementServices.describe(null));
    }

    @Test
    void theJournalsCasingDoesNotMatter() {
        assertEquals(List.of("techBroker"), SettlementServices.notable(List.of("techBroker")));
    }
}
