package elite.intel.junit.prompt;


import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.ai.brain.commons.HandlerDispatchedEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ws.WebSocketBroadcaster;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NaturalSpeechIntegrationTestDE {


    private static final int LLM_WAIT_MS = 3000;
    private static final int LLM_POLL_MS = 100;

    private HandlerCapture capture;

    @BeforeAll
    void bootstrap() throws InterruptedException {
        SystemSession systemSession = SystemSession.getInstance();
        systemSession.setConversationalMode(false);
        systemSession.setLanguage(Language.DE);
        HeadlessBootstrap.start();
        WebSocketBroadcaster.getInstance().start();
        capture = new HandlerCapture();
        Thread.sleep(2000);
        GameEventBus.publish(new SensorDataEvent("ping - connection check", "Acknowledge connection"));
        Thread.sleep(4000);
    }

    @AfterAll
    void teardown() {
        HeadlessBootstrap.stop();
    }

    // -------------------------------------------------------------------------
    // Core tester
    // -------------------------------------------------------------------------

    private void assertRouted(String input, String expectedAction) throws InterruptedException {
        capture.reset();
        GameEventBus.publish(new UserInputEvent(input));

        HandlerDispatchedEvent event = waitForDispatch(expectedAction);
        assertNotNull(event,
                "No handler dispatched for input: \"" + input + "\"");
        assertEquals(expectedAction, event.getAction(),
                "Input: \"" + input + "\" → got \"" + event.getAction()
                        + "\" but expected \"" + expectedAction + "\"");
    }

    private HandlerDispatchedEvent waitForDispatch(String expectedAction) throws InterruptedException {
        long deadline = System.currentTimeMillis() + LLM_WAIT_MS;
        HandlerDispatchedEvent event = null;
        while (System.currentTimeMillis() < deadline) {
            event = capture.getLastEvent();
            if (event != null && expectedAction.equals(event.getAction())) {
                return event;
            }
            Thread.sleep(LLM_POLL_MS);
        }
        return event;
    }

    // =========================================================================
    // Attention / control
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(10)
    @MethodSource
    void startListening(String input) throws InterruptedException {
        assertRouted(input, WakeupCommand.ID);
    }

    static Stream<String> startListening() {
        return Stream.of("wach auf", "aufwachen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SleepCommand.ID);
    }

    static Stream<String> ignoreMe() {
        return Stream.of("ignoriere mich", "hör nicht zu", "schlaf");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, InterruptCommand.ID);
    }

    static Stream<String> interrupt() {
        return Stream.of("unterbrich", "unterbrechen", "hör auf zu reden", "sei still", "ruhe", "halt die klappe");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToCombatModeCommand.ID);
    }

    static Stream<String> combatMode() {
        return Stream.of("kampfmodus", "in den kampfmodus wechseln", "kampfmodus aktivieren", "kampf");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToAnalysisModeCommand.ID);
    }

    static Stream<String> analysisMode() {
        return Stream.of("analysemodus", "in den analysemodus wechseln", "erkundungsmodus",
                "analyse HUD", "analysemodus aktivieren");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, ResetHeadLookAheadCommand.ID);
    }

    static Stream<String> lookAhead() {
        return Stream.of("blick nach vorne", "blickrichtung zurücksetzen", "schau nach vorne", "ansicht zentrieren");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(16)
    @MethodSource
    void honkTheSystem(String input) throws InterruptedException {
        assertRouted(input, HonkCommand.ID);
    }

    static Stream<String> honkTheSystem() {
        return Stream.of("erkunde das system", "scanne das system", "system erkunden", "system abtasten");
    }

    // =========================================================================
    // Speed / throttle - highest collision risk group
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(20)
    @MethodSource
    void speedZero(String input) throws InterruptedException {
        assertRouted(input, SetSpeedZeroCommand.ID);
    }

    static Stream<String> speedZero() {
        return Stream.of("triebwerke stoppen", "anhalten", "voller stopp",
                "triebwerke aus", "schub auf null", "null schub", "schiff stoppen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SetSpeed25Command.ID);
    }

    static Stream<String> speed25() {
        return Stream.of("viertel schub", "25 prozent", "langsame fahrt");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SetSpeed50Command.ID);
    }

    static Stream<String> speed50() {
        return Stream.of("halber schub", "50 prozent", "halbe fahrt");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SetSpeed75Command.ID);
    }

    static Stream<String> speed75() {
        return Stream.of("drei viertel schub", "75 prozent", "drei viertel geschwindigkeit");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SetSpeed100Command.ID);
    }

    static Stream<String> speed100() {
        return Stream.of("voller schub", "100 prozent", "volle geschwindigkeit", "maximaler schub");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, IncreaseSpeedCommand.ID);
    }

    static Stream<String> speedPlus() {
        return Stream.of("erhöhe geschwindigkeit um 10", "erhöhe geschwindigkeit um 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DecreaseSpeedCommand.ID);
    }

    static Stream<String> speedMinus() {
        return Stream.of("reduziere geschwindigkeit um 10", "reduziere geschwindigkeit um 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SetOptimalSpeedCommand.ID);
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("optimale geschwindigkeit setzen", "optimale anfluggeschwindigkeit");
    }

    // =========================================================================
    // Navigation - second highest collision risk
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(30)
    @MethodSource
    void jumpToHyperspace(String input) throws InterruptedException {
        assertRouted(input, JumpToHyperspaceCommand.ID);
    }

    static Stream<String> jumpToHyperspace() {
        return Stream.of("sprung in den hyperraum", "sprung", "los gehts",
                "auf gehts", "sprung zum nächsten wegpunkt");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, EnterSuperCruiseCommand.ID);
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("in supercruise gehen", "supercruise einschalten", "supercruise", "lichtgeschwindigkeit", "supercruise aktivieren", "überlichtflug");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {
        assertRouted(input, DropFromSuperCruiseCommand.ID);
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("hier rausfallen", "raus hier", "supercruise verlassen", "drop");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NavigateToMissionTargetCommand.ID);
    }

    static Stream<String> navigateToMission() {
        return Stream.of("fliege zur aktiven mission", "route zur aktiven mission planen",
                "zur aktiven mission", "fliege zur mission", "navigation zur mission");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("fliege zum fleet carrier", "zurück zum carrier", "bring uns zum carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, CancelNavigationCommand.ID);
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("navigation abbrechen", "route abbrechen", "navigation ausschalten");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, NavigateToLandingZoneCommand.ID);
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("fliege zur landezone", "kurs zur landezone", "bring mich zurück zur landezone");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TargetDestinationCommand.ID);
    }

    static Stream<String> targetDestination() {
        return Stream.of("reiseziel auswählen", "sprungziel auswählen");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(38)
    @MethodSource
    void clearActiveMissions(String input) throws InterruptedException {
        assertRouted(input, ClearActiveMissionsCommand.ID);
    }

    static Stream<String> clearActiveMissions() {
        return Stream.of("lösche alle aufträge", "alle aufträge löschen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NavigateToTradeStopCommand.ID);
    }

    static Stream<String> nextTradeStop() {
        return Stream.of("fliege zum nächsten handelsstopp", "zum nächsten handelsstopp");
    }

    // =========================================================================
    // Flight / ship systems
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(40)
    @MethodSource
    void deployLandingGear(String input) throws InterruptedException {
        assertRouted(input, DeployLandingGearCommand.ID);
    }

    static Stream<String> deployLandingGear() {
        return Stream.of("fahrwerk", "fahrwerk ausfahren", "fahrwerk runter", "fahrwerk raus", "landefahrwerk ausfahren");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RetractLandingGearCommand.ID);
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("fahrwerk einfahren", "fahrwerk hoch", "fahrwerk rein", "landefahrwerk einfahren");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, RequestDockingCommand.ID);
    }

    static Stream<String> requestDocking() {
        return Stream.of("landeerlaubnis anfragen", "landeplatz anfragen", "andocken anfragen",
                "kontaktiere den tower für einen landeplatz",
                "landung anfragen", "docking anfragen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, ToggleCargoScoopCommand.ID);
    }

    static Stream<String> cargoScoop() {
        return Stream.of("frachtschaufel öffnen", "frachtschaufel ausfahren", "frachttor öffnen", "frachtluke öffnen", "fangschaufel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, ToggleNightVisionOnOffCommand.ID);
    }

    static Stream<String> nightVision() {
        return Stream.of("nachtsicht", "nachtsichtmodus", "nachtsicht einschalten", "nachtsicht ausschalten");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, ToggleLightsOnOffCommand.ID);
    }

    static Stream<String> lights() {
        return Stream.of("scheinwerfer", "licht einschalten", "licht ausschalten", "lichter", "licht an");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DismissShipToOrbitCommand.ID);
    }

    static Stream<String> dismissShip() {
        return Stream.of("schiff wegschicken", "schiff fortschicken", "schiff in den orbit", "schiff entlassen", "weggetreten");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TaxiToLandingPadCommand.ID);
    }

    static Stream<String> taxi() {
        return Stream.of("autopilot", "taxi", "automatisches andocken", "autolandung", "taxi", "auto taxi");
    }

    // =========================================================================
    // Combat / hardpoints
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(50)
    @MethodSource
    void deployHardpoints(String input) throws InterruptedException {
        assertRouted(input, DeployHardpointsCommand.ID);
    }

    static Stream<String> deployHardpoints() {
        return Stream.of("waffen ausfahren", "waffen bereit", "kampfbereit", "bewaffnen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RetractHardpointsCommand.ID);
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("waffen einfahren", "waffen weg", "waffen sichern", "entwarnung");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DeployHeatSinkCommand.ID);
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("wärmesenke abwerfen", "kühlkörper auslösen", "hitze abwerfen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, TargetHostileHighestThreatCommand.ID);
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("prioritätsziel", "gefährlichstes ziel", "nächster feind", "feind auswählen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DeployShieldCellCommand.ID);
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("schildzelle aktivieren", "schildzelle verwenden",
                "schildzelle einsetzen", "schildzellenbank", "energiezelle einsetzen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DeployChaffCommand.ID);
    }

    static Stream<String> deployChaff() {
        return Stream.of("täuschkörper einsetzen", "täuschkörper abwerfen", "düppel werfen", "chaff einsetzen", "täuschkörper auslösen", "düppel abwerfen");
    }

    // =========================================================================
    // Power management
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(60)
    @MethodSource
    void powerToShields(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToShieldsCommand.ID);
    }

    static Stream<String> powerToShields() {
        return Stream.of("energie auf schilde", "maximale schilde", "schilde verstärken");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToEnginesCommand.ID);
    }

    static Stream<String> powerToEngines() {
        return Stream.of("energie auf triebwerke", "maximale triebwerke", "triebwerke verstärken");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToWeaponsCommand.ID);
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("energie auf waffen", "maximale waffen", "waffen verstärken");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, EqualizePowerCommand.ID);
    }

    static Stream<String> resetPower() {
        return Stream.of("energie ausgleichen", "energie balancieren", "energie zurücksetzen", "energie gleichmäßig verteilen");
    }

    // =========================================================================
    // Science / exploration / mining
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(70)
    @MethodSource
    void openFss(String input) throws InterruptedException {
        assertRouted(input, OpenFssScanSystemCommand.ID);
    }

    static Stream<String> openFss() {
        return Stream.of("Öffne FSS und scanne.", "vollständigen Spektralscan durchführen",
                "vollständiger spektralscan", "systemscan");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NavigateToBioSampleCodexEntryCommand.ID);
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("zum nächsten bio sample navigieren", "zum nächsten organismus navigieren", "zum codex eintrag navigieren");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FindMiningSiteCommand.ID);
    }

    static Stream<String> findMiningSite() {
        return Stream.of("finde einen abbauort für alexandrit im umkreis von 300 lichtjahren",
                "finde einen abbauort für bromellit im umkreis von 1200 lichtjahren",
                "finde ein asteroidenfeld mit gold");
    }

    // =========================================================================
    // Fleet carrier
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(80)
    @MethodSource
    void enterCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, EnterFleetCarrierDestinationCommand.ID);
    }

    static Stream<String> enterCarrierDestination() {
        return Stream.of("carrier ziel eingeben", "carrier ziel setzen",
                "nächstes carrier ziel eingeben");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(82)
    @MethodSource
    void findNearestCarrier(String input) throws InterruptedException {
        assertRouted(input, FindNearestFleetCarrierCommand.ID);
    }

    static Stream<String> findNearestCarrier() {
        return Stream.of("nächsten fleet carrier finden", "nächster carrier");
    }

    // =========================================================================
    // Squadron carrier
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(85)
    @MethodSource
    void navigateToSquadronCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToSquadronCarrierCommand.ID);
    }

    static Stream<String> navigateToSquadronCarrier() {
        return Stream.of("fliege zum squadron carrier", "route zum squadron carrier planen", "kurs zum squadron carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierDataQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("squadron carrier status", "squadron carrier finanzen",
                "squadron carrier balance", "wie lange kann der squadron carrier betrieben werden",
                "squadron carrier tritium", "squadron carrier treibstoff",
                "squadron carrier treibstoffstand");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierRouteQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of("squadron carrier route", "wie viele sprünge auf der squadron carrier route",
                "squadron carrier sprungroute");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierFinalDestinationQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("wohin fliegt der squadron carrier", "endziel des squadron carriers", "squadron carrier kurs");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierETAQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("squadron carrier ankunftszeit", "wann kommt der squadron carrier an",
                "wie lange bis squadron carrier ankunft");
    }

    // =========================================================================
    // Disambiguation: bare "carrier" phrases must route to fleet, not squadron
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(250)
    @MethodSource
    void bareCarrierDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> bareCarrierDefaultsToFleet() {
        return Stream.of("fliege zum fleet carrier", "zurück zum carrier", "bring uns zum carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierDataQueryCommand.ID);
    }

    static Stream<String> bareCarrierStatusDefaultsToFleet() {
        return Stream.of("carrier status", "carrier balance", "carrier kontostand");
    }

    // =========================================================================
    // App settings / announcements
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(90)
    @MethodSource
    void disableAnnouncements(String input) throws InterruptedException {
        assertRouted(input, ToggleAllAnnouncementsCommand.ID);
    }

    static Stream<String> disableAnnouncements() {
        return Stream.of("alle ansagen ausschalten");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SetReminderCommand.ID);
    }

    static Stream<String> setReminder() {
        return Stream.of("erinnere mich am nächsten stopp zu tanken");
    }

    // =========================================================================
    // UI panels
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(100)
    @MethodSource
    void galaxyMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenGalaxyMapCommand.ID);
    }

    static Stream<String> galaxyMap() {
        return Stream.of("galaxiekarte öffnen", "galaxiekarte anzeigen", "galaxy map öffnen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenSystemMapCommand.ID);
    }

    static Stream<String> systemMap() {
        return Stream.of("systemkarte öffnen", "lokale karte öffnen", "systemkarte anzeigen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, ShowNavigationPanelCommand.ID);
    }

    static Stream<String> navigationPanel() {
        return Stream.of("navigationspanel anzeigen", "navigationspanel öffnen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, ShowModulesPanelCommand.ID);
    }

    static Stream<String> modulesPanel() {
        return Stream.of("modulpanel anzeigen", "module öffnen", "module anzeigen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, ShowStatusPanelCommand.ID);
    }

    static Stream<String> statusPanel() {
        return Stream.of("statuspanel anzeigen", "statuspanel öffnen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, ShowInventoryPanelCommand.ID);
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("inventar anzeigen", "inventar öffnen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, ExitCloseCommand.ID);
    }

    static Stream<String> closePanel() {
        return Stream.of("panel schließen", "panel verlassen");
    }

    // =========================================================================
    // Queries
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(200)
    @MethodSource
    void queryCurrentLocation(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCurrentLocationQueryCommand.ID);
    }

    static Stream<String> queryCurrentLocation() {
        return Stream.of("Wo sind wir gerade?", "wie lautet unsere position", "wo sind wir", "wie lange dauert ein tag hier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, AnalyzeShipLoadoutQueryCommand.ID);
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("schiffsausrüstung", "was fliege ich", "schiffsausstattung",
                "habe ich einen fuelscoop an bord", "habe ich waffen an bord",
                "welche waffen sind installiert", "habe ich einen refinery an bord");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCargoHoldQueryCommand.ID);
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("frachtraum", "was transportieren wir", "frachtinhalt");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeRouterQueryCommand.ID);
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("geplante route", "verbleibende sprünge", "wie viele sprünge bis zum ziel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStationsQueryCommand.ID);
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("stationen im system", "welche stationen", "nächste stationen",
                "gibt es hier stationen oder häfen", "gibt es häfen in diesem sternsystem");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarObjectsQueryCommand.ID);
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("Welche landbaren planeten oder monde gibt es in diesem system?",
                "Gibt es eisringe in diesem sternsystem");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarSignalsQueryCommand.ID);
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of("Welche signale gibt es in diesem system?", "Welche signale siehst du?",
                "Gibt es interessante signale?", "Systemsignale?", "Was ist in diesem system?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioScansStarSystemQueryCommand.ID);
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("Welche planeten müssen noch biologisch oder organisch gescannt werden?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioSamplesPlanetSurfaceQueryCommand.ID);
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of("Welche bioscans haben wir auf diesem planeten abgeschlossen?", "Welche organismen müssen noch auf diesem planeten gescannt werden?", "Welche organik oder biologie gibt es auf diesem planeten");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, AnalyzePlayerProfileQueryCommand.ID);
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("spielerprofil", "spielerprofil rangübersicht", "spielerprofil fortschrittsübersicht");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierDataQueryCommand.ID);
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of("Wie groß ist die reichweite unseres carriers?", "Wie ist der treibstoffstatus des fleet carriers",
                "Wie lange können wir mit dem aktuellen guthaben operieren?",
                "Wie weit kann der carrier mit dem aktuellen tritium springen?", "carrier tritium", "carrier treibstoff",
                "wie viel tritium auf dem carrier", "tritium stand", "treibstoffstand des carriers"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromFleetCarrierQueryCommand.ID);
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("Wie weit sind wir vom carrier entfernt?", "Entfernung zum fleet carrier?",
                "Wie weit ist der fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFsdTargetQueryCommand.ID);
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("fsd ziel", "info zum nächsten sprung");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, AnalyzeExplorationProfitsQueryCommand.ID);
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("Explorationsgewinn-Potenzial in diesem system.",
                "Wie hoch ist das Explorationsgewinn-Potenzial in diesem system?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TimeQueryCommand.ID);
    }

    static Stream<String> queryTime() {
        return Stream.of("aktuelle zeit", "wie spät ist es", "utc zeit");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSystemSecurityQueryCommand.ID);
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("systemsicherheit", "wer kontrolliert dieses system", "dominante fraktion");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, StationDataQueryCommand.ID);
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("stationsdetails", "welche services gibt es auf dieser station",
                "welche dienste hier", "stationsinfo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyseMaterialsQueryCommand.ID);
    }

    static Stream<String> queryMaterials() {
        return Stream.of("materialinventar eisen", "wie viel eisen haben wir", "wie viel vanadium haben wir");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMaterialsOnPlanetQueryCommand.ID);
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("Welche materialien sind auf diesem planeten verfügbar?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromTheBubbleQueryCommand.ID);
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("Wie weit sind wir von der Bubble entfernt?", "Entfernung zur Erde", "Wie weit ist die Erde",
                "wie weit zur zivilisation", "wie weit zur erde");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, AnalyzeLastScanQueryCommand.ID);
    }

    static Stream<String> queryLastScan() {
        return Stream.of("Analysiere den letzten scan");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, RemindTargetDestinationQueryCommand.ID);
    }

    static Stream<String> queryReminder() {
        return Stream.of("erinnerung", "was war die erinnerung", "gibt es erinnerungen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierETAQueryCommand.ID);
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("Wie ist die ankunftszeit unseres fleet carriers?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeGeologyInStarSystemQueryCommand.ID);
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("geosignale", "geologische signale", "vulkanische aktivität");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMarketsQueryCommand.ID);
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("lokale märkte", "märkte auf stationen und siedlungen", "märkte auf außenposten im system");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBountiesCollectedQueryCommand.ID);
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("kopfgelder", "gesamte kopfgelder", "wie viele kopfgelder");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, BiomeAnalyzerQueryCommand.ID);
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("Analysiere das biom dieses sternsystems", "Biom-Analyse für planet A 1");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromLastBioSampleQueryCommand.ID);
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("entfernung zur letzten bio probe", "Wie weit sind wir von der letzten bio probe entfernt");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierRouteQueryCommand.ID);
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of("Was ist auf der carrier route?", "Wie ist die route unseres fleet carriers?",
                "Wie viele sprünge auf der carrier route?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierFinalDestinationQueryCommand.ID);
    }

    static Stream<String> queryCarrierDestination() {
        return Stream.of("Wohin fliegt unser carrier?", "Was ist das endziel des carriers?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SetCarrierFuelReserveCommand.ID);
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("Setze die treibstoffreserve auf 5000", "Setze die treibstoffreserve auf 10000",
                "Treibstoffreserve 15000", "Setze die treibstoffreserve auf fünfzehntausend");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DisembarkCommand.ID);
    }

    static Stream<String> disembark() {
        return Stream.of("aussteigen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, ShowCommanderPanelCommand.ID);
    }

    static Stream<String> openCentralPanel() {
        return Stream.of("Commander panel öffnen", "zentrales panel öffnen",
                "rollen panel öffnen", "kneeboard öffnen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, ShowFighterPanelCommand.ID);
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("jäger panel anzeigen", "jäger panel öffnen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FighterFireAtWillCommand.ID);
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("feuer frei", "nach eigenem ermessen feuern", "feuer", "das feuer eröffnen");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FighterAttackTargetCommand.ID);
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("jäger greife mein ziel an", "greife mein ziel an", "fokus auf mein ziel");
    }
}
