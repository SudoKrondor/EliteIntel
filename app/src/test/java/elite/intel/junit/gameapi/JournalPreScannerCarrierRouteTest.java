package elite.intel.junit.gameapi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.JournalPreScanner;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A fleet carrier jumps on its own schedule, with or without the commander in the game. When it jumps
 * off its plotted route while the app is down, the arrival reaches the app only through the startup
 * replay: the live parser drops journal lines older than app start. Without a repair there, the stored
 * route keeps running from a system the carrier has left, and every leg it reports is unreachable.
 *
 * <p>The pre-scan itself makes no network calls, so the re-plot happens after the replay, once, from
 * the carrier's final position.
 */
class JournalPreScannerCarrierRouteTest {

    private static final String JOB_ID = "carrier-job-prescan";

    // Index 0 is the origin, which the client drops; 1 onward are the legs still to fly.
    private static final String REPLOTTED_ROUTE = """
            {
              "result": {
                "jumps": [
                  {"name": "Eephaik CX-V b31-9", "fuel_used": 0, "distance": 0.0,
                   "has_icy_ring": false, "is_system_pristine": false, "x": -12135.0, "y": -895.0, "z": 17545.0},
                  {"name": "Blua Eaec WW-E d11-32", "fuel_used": 102, "distance": 498.2,
                   "has_icy_ring": true, "is_system_pristine": false, "x": -11700.0, "y": -890.0, "z": 17800.0},
                  {"name": "Colonia", "fuel_used": 97, "distance": 474.0,
                   "has_icy_ring": true, "is_system_pristine": true, "x": -9530.5, "y": -910.28125, "z": 19808.125}
                ]
              }
            }
            """;

    // The client sends Accept-Encoding: gzip and does not decompress, so Jetty compression is off.
    @RegisterExtension
    WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().gzipDisabled(true))
            .build();

    private final PlayerSession session = PlayerSession.getInstance();

    @AfterEach
    void cleanup() {
        System.clearProperty("spansh.base.url");
        FleetCarrierRouteManager.getInstance().clear();
    }

    @Test
    @DisplayName("a carrier that left its route while the app was down has the route re-plotted from where it now is")
    void anOffRouteArrivalLearnedAtStartupReplotsTheRoute(@TempDir Path journalDir) throws IOException {
        System.setProperty("spansh.base.url", wm.baseUrl());
        stubJobAndResult(REPLOTTED_ROUTE);

        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        route.clear();
        session.setLastKnownCarrierLocation("Eephaik LY-R b47-6");
        session.setFleetCarrierData(carrierWithFuel());
        // Plotted while the carrier was somewhere else entirely.
        route.setFleetCarrierRoute(Map.of(1, leg("Dryooe Flyou GB-I c24-147"), 2, leg("Colonia")));

        writeJournal(journalDir, "Eephaik CX-V b31-9", 20299220533521L);
        JournalPreScanner.scan(journalDir);

        assertEquals("Eephaik CX-V b31-9", session.getCurrentFleetCarrierSystem(),
                "the replay has to have followed the carrier first");
        Map<Integer, CarrierJump> replotted = route.getFleetCarrierRoute();
        assertEquals(2, replotted.size(), "the route should be the freshly plotted one");
        assertEquals("Blua Eaec WW-E d11-32", replotted.get(1).getSystemName(),
                "leg 1 must be reachable from where the carrier actually is");
        assertNull(route.findByPrimaryStar("Dryooe Flyou GB-I c24-147"),
                "the abandoned route's legs must be gone");
    }

    @Test
    @DisplayName("a carrier still sitting where its route starts is left alone, and Spansh is not called")
    void aPositionReportDoesNotReplot(@TempDir Path journalDir) throws IOException {
        System.setProperty("spansh.base.url", wm.baseUrl());
        stubJobAndResult(REPLOTTED_ROUTE);

        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        route.clear();
        session.setLastKnownCarrierLocation("Eephaik CX-V b31-9");
        session.setFleetCarrierData(carrierWithFuel());
        route.setFleetCarrierRoute(Map.of(1, leg("Dryooe Flyou GB-I c24-147"), 2, leg("Colonia")));

        writeJournal(journalDir, "Eephaik CX-V b31-9", 20299220533521L);
        JournalPreScanner.scan(journalDir);

        assertEquals("Dryooe Flyou GB-I c24-147", route.getFleetCarrierRoute().get(1).getSystemName(),
                "the carrier has not moved, so its route still stands");
        wm.verify(0, postRequestedFor(urlEqualTo("/api/fleetcarrier/route")));
    }

    private static CarrierDataDto carrierWithFuel() {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setFuelLevel(545);
        carrier.setCargoCapacity(25000);
        carrier.setCargoSpaceUsed(13587);
        return carrier;
    }

    private static CarrierJump leg(String systemName) {
        CarrierJump jump = new CarrierJump();
        jump.setSystemName(systemName);
        jump.setFuelUsed(100);
        return jump;
    }

    /**
     * One CarrierLocation, which is all a carrier arrival the commander was not aboard for produces.
     */
    private static void writeJournal(Path journalDir, String system, long systemAddress) throws IOException {
        String line = """
                { "timestamp":"2026-07-30T04:24:10Z", "event":"CarrierLocation", "CarrierType":"FleetCarrier",\
                 "CarrierID":3712500736, "StarSystem":"%s", "SystemAddress":%d, "BodyID":0 }
                """.formatted(system, systemAddress);
        Files.writeString(journalDir.resolve("Journal.2026-07-30T042410.01.log"), line);
    }

    private void stubJobAndResult(String resultJson) {
        wm.stubFor(post(urlEqualTo("/api/fleetcarrier/route"))
                .willReturn(aResponse().withStatus(202).withBody("{\"job\":\"" + JOB_ID + "\"}")));
        wm.stubFor(get(urlEqualTo("/api/results/" + JOB_ID))
                .willReturn(okJson(resultJson)));
    }
}
