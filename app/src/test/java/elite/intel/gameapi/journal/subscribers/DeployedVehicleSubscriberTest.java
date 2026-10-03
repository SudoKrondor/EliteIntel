package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonParser;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.*;
import elite.intel.session.DeployedVehicle;
import elite.intel.session.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Nomad told apart from a wheeled SRV. Status.json cannot do it - in the Nomad it sets InSRV and nothing
 * else ({@code Flags 203423816}, {@code Flags2 0}, sampled 2026-10-02) - so the journal's vehicle symbol is
 * remembered and {@link Status#isInNomad()} narrows {@code isInSrv()} by it.
 * <p>
 * The launch and dock lines are the commander's own. The LoadGame line is trimmed from a support bundle.
 */
class DeployedVehicleSubscriberTest {

    /**
     * Live Status.json flags in the Nomad: ShieldsUp, HardpointsDeployed, HasLatLong, InSRV, HudAnalysis.
     */
    private static final long IN_NOMAD_FLAGS = 203423816L;
    /**
     * A Scarab parked in analysis mode - the same InSRV bit.
     */
    private static final long IN_SCARAB_FLAGS = 203427848L;
    /**
     * On foot on a planet: HasLatLong only, as far as Flags goes.
     */
    private static final long ON_FOOT_FLAGS = 1L << 21;
    /**
     * In the main ship, flying.
     */
    private static final long IN_SHIP_FLAGS = 1L << 24;

    private final DeployedVehicleSubscriber subscriber = new DeployedVehicleSubscriber();
    private long savedFlags;

    @BeforeEach
    void saveFlags() {
        savedFlags = Status.getInstance().getStatus().getFlags();
        DeployedVehicle.getInstance().reset();
    }

    @AfterEach
    void restore() {
        setFlags(savedFlags);
        DeployedVehicle.getInstance().reset();
    }

    @Test
    void launchVesselNamesTheNomad() {
        subscriber.onLaunchVessel(launchVessel());
        setFlags(IN_NOMAD_FLAGS);

        assertTrue(Status.getInstance().isInSrv(), "the game still reports the Nomad as an SRV");
        assertTrue(Status.getInstance().isInNomad());
    }

    @Test
    void aWheeledSrvIsNotTheNomad() {
        subscriber.onLaunchSRV(launchSrv("testbuggy", "SRV Scarab"));
        setFlags(IN_SCARAB_FLAGS);

        assertTrue(Status.getInstance().isInSrv());
        assertFalse(Status.getInstance().isInNomad());
    }

    @Test
    void launchingASrvAfterTheNomadReplacesIt() {
        subscriber.onLaunchVessel(launchVessel());
        subscriber.onDockSRV(dockSrv());
        subscriber.onLaunchSRV(launchSrv("mev_rhino", "SRV Rhino"));
        setFlags(IN_SCARAB_FLAGS);

        assertFalse(Status.getInstance().isInNomad());
    }

    @Test
    void stowingTheNomadForgetsIt() {
        subscriber.onLaunchVessel(launchVessel());
        subscriber.onDockSRV(dockSrv());
        setFlags(IN_NOMAD_FLAGS);

        assertFalse(Status.getInstance().isInNomad(), "nothing known falls back to plain SRV behaviour");
    }

    @Test
    void loggingInInsideTheNomadNamesIt() {
        subscriber.onLoadGame(loadGame("Lander01", "Nomad"));
        setFlags(IN_NOMAD_FLAGS);

        assertTrue(Status.getInstance().isInNomad(), "LoadGame spells it Lander01 - the test ignores case");
    }

    @Test
    void loggingInToAShipClearsAStaleNomad() {
        subscriber.onLaunchVessel(launchVessel());
        subscriber.onLoadGame(loadGame("Explorer_NX", "Caspian Explorer"));
        setFlags(IN_NOMAD_FLAGS);

        assertFalse(Status.getInstance().isInNomad());
    }

    @Test
    void theNomadOnRecordIsNotTheNomadOnceBackInTheShip() {
        subscriber.onLaunchVessel(launchVessel());
        setFlags(IN_SHIP_FLAGS);

        assertFalse(Status.getInstance().isInNomad(), "isInNomad is gated on the InSRV flag");
    }

    @Test
    void onFootAndBackIntoTheNomadKeepsIt() {
        subscriber.onLaunchVessel(launchVessel());
        setFlags(ON_FOOT_FLAGS);
        assertFalse(Status.getInstance().isInNomad(), "on foot is not in the Nomad");

        subscriber.onEmbark(embark(true, 29));
        setFlags(IN_NOMAD_FLAGS);
        assertTrue(Status.getInstance().isInNomad());
    }

    /**
     * 2026-07-11 in the commander's journal: LoadGame in the Artemis suit, then Embark SRV:true ID 29.
     */
    @Test
    void loggingInOnFootThenBoardingANomadLeftOutNamesItBySlot() {
        subscriber.onLaunchVessel(launchVessel());
        subscriber.onDockSRV(dockSrv());
        subscriber.onLoadGame(loadGame("ExplorationSuit_Class1", "Artemis Suit"));
        subscriber.onEmbark(embark(true, 29));
        setFlags(IN_NOMAD_FLAGS);

        assertTrue(Status.getInstance().isInNomad());
    }

    @Test
    void boardingAnUnknownSlotFallsBackToPlainSrv() {
        subscriber.onLoadGame(loadGame("ExplorationSuit_Class1", "Artemis Suit"));
        subscriber.onEmbark(embark(true, 29));
        setFlags(IN_NOMAD_FLAGS);

        assertFalse(Status.getInstance().isInNomad());
    }

    /**
     * Slot 35 has held a Scarab and, later, a Rhino in the commander's journals: the last one named wins.
     */
    @Test
    void aSlotNamesWhatWasLastLaunchedFromIt() {
        subscriber.onLaunchSRV(launchSrv("lander01", "Nomad", 35));
        subscriber.onDockSRV(dockSrv());
        subscriber.onLaunchSRV(launchSrv("mev_rhino", "SRV Rhino", 35));
        subscriber.onLoadGame(loadGame("ExplorationSuit_Class1", "Artemis Suit"));
        subscriber.onEmbark(embark(true, 35));
        setFlags(IN_SCARAB_FLAGS);

        assertFalse(Status.getInstance().isInNomad());
    }

    @Test
    void boardingTheShipSaysNothingAboutTheVehicle() {
        subscriber.onLaunchVessel(launchVessel());
        subscriber.onEmbark(embark(false, 29));
        setFlags(IN_NOMAD_FLAGS);

        assertTrue(Status.getInstance().isInNomad());
    }

    @Test
    void nothingKnownIsNotTheNomad() {
        setFlags(IN_NOMAD_FLAGS);

        assertFalse(Status.getInstance().isInNomad());
    }

    private static LaunchVesselEvent launchVessel() {
        String json = """
                { "timestamp":"2026-10-02T22:31:22Z", "event":"LaunchVessel", "VesselType":"lander01",
                  "VesselType_Localised":"Nomad", "Loadout":"galactic", "ID":29, "PlayerControlled":true }
                """;
        return new LaunchVesselEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static LaunchSRVEvent launchSrv(String type, String localised) {
        return launchSrv(type, localised, 23);
    }

    private static LaunchSRVEvent launchSrv(String type, String localised, int id) {
        String json = """
                { "timestamp":"2026-09-02T20:00:00Z", "event":"LaunchSRV", "SRVType":"%s",
                  "SRVType_Localised":"%s", "Loadout":"default", "ID":%d, "PlayerControlled":true }
                """.formatted(type, localised, id);
        return new LaunchSRVEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static DockSRVEvent dockSrv() {
        String json = """
                { "timestamp":"2026-09-18T22:15:35Z", "event":"DockSRV", "SRVType":"lander01",
                  "SRVType_Localised":"Nomad", "ID":29 }
                """;
        return new DockSRVEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static EmbarkEvent embark(boolean srv, int id) {
        String json = """
                { "timestamp":"2026-07-11T23:58:00Z", "event":"Embark", "SRV":%s, "Taxi":false, "Multicrew":false,
                  "ID":%d, "StarSystem":"HIP 90424", "SystemAddress":10326428075, "Body":"HIP 90424 A 5 b",
                  "BodyID":41, "OnStation":false, "OnPlanet":true }
                """.formatted(srv, id);
        return new EmbarkEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static LoadGameEvent loadGame(String ship, String localised) {
        String json = """
                { "timestamp":"2026-10-02T22:30:15Z", "event":"LoadGame", "FID":"F0000000", "Commander":"TEST",
                  "Horizons":true, "Odyssey":true, "Ship":"%s", "Ship_Localised":"%s", "ShipID":22,
                  "StartLanded":true, "GameMode":"Solo", "Credits":1000, "Loan":0 }
                """.formatted(ship, localised);
        return new LoadGameEvent(JsonParser.parseString(json).getAsJsonObject());
    }

    private static void setFlags(long flags) {
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        snapshot.setFlags(flags);
        Status.getInstance().setStatus(snapshot);
    }
}
