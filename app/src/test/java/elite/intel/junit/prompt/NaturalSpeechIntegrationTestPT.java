package elite.intel.junit.prompt;

import elite.intel.ai.brain.commons.HandlerDispatchedEvent;
import elite.intel.gameapi.EventBusManager;
import elite.intel.gameapi.SensorDataEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.ws.WebSocketBroadcaster;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static elite.intel.ai.brain.actions.Commands.*;
import static elite.intel.ai.brain.actions.Queries.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test class for verifying the interaction between the application
 * and the NaturalSpeech system.
 * This class contains a series of parameterized tests designed to validate the
 * proper routing of input commands
 * to corresponding actions in the system. It covers various scenarios such as
 * navigation, speed control,
 * operational modes, and other gameplay-related actions.
 * <p>
 * REQUIREMENTS
 * 1) Have local LLM installed and configured with the supported model.
 * 2) Start the app at least once and have the game running for some basic data
 * 3) Ensure that the LLM is responsive and capable of handling the test
 * scenarios within the allocated time.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NaturalSpeechIntegrationTestPT {

    /**
     * Pause between each test phrase. Increase if your LLM is slow.
     * 3000 simulates a typical interaction.
     * 1500 go faster.
     * 250 you are pushing it.
     * 150 bro I want your hardware.
     */
    private static final int LLM_WAIT_MS = 3000;
    private static final int LLM_POLL_MS = 100;

    private HandlerCapture capture;

    @BeforeAll
    void bootstrap() throws InterruptedException {
        SystemSession systemSession = SystemSession.getInstance();
        systemSession.setConversationalMode(false);
        systemSession.setLanguage(Language.EN); ///NOTE: Change this to Language.PT
        HeadlessBootstrap.start();
        WebSocketBroadcaster.getInstance().start();
        capture = new HandlerCapture();
        // Let any startup noise (connection check etc.) settle
        Thread.sleep(2000);
        /// this allows LLM to cache the prompt header / same request runs on app
        /// startup.
        EventBusManager.publish(new SensorDataEvent("ping - connection check", "Acknowledge connection"));
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
        EventBusManager.publish(new UserInputEvent(input));

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
        assertRouted(input, WAKEUP.getAction());
    }

    static Stream<String> startListening() {
        return Stream.of("wake up", "wake");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SLEEP.getAction());
    }

    static Stream<String> ignoreMe() {
        return Stream.of("ignore me", "do not monitor", "sleep");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, INTERRUPT_TTS.getAction());
    }

    static Stream<String> interrupt() {
        return Stream.of("interrupt");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, ACTIVATE_COMBAT_MODE.getAction());
    }

    static Stream<String> combatMode() {
        return Stream.of("combat mode", "change to combat mode", "combat", "switch to combat mode",
                "swap to combat mode");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, ACTIVATE_ANALYSIS_MODE.getAction());
    }

    static Stream<String> analysisMode() {
        return Stream.of("Analysis mode", "switch to analysis mode", "explorer mode", "analysis HUD",
                "Change to analysis mode", "swap to analysis mode");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, RESET_HEAD_LOOK.getAction());
    }

    static Stream<String> lookAhead() {
        return Stream.of("look ahead", "reset", "reset head look", "head look to neutral");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(16)
    @MethodSource
    void honkTheSystem(String input) throws InterruptedException {
        assertRouted(input, HONK_THE_SYSTEM.getAction());
    }

    static Stream<String> honkTheSystem() {
        return Stream.of("honk", "honk the system");
    }
    // =========================================================================
    // Speed / throttle - highest collision risk group
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(20)
    @MethodSource
    void speedZero(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED_ZERO.getAction());
    }

    static Stream<String> speedZero() {
        return Stream.of("stop engines", "full stop", "all stop", "kill engines", "cut throttle", "zero throttle",
                "stop ship");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED25.getAction());
    }

    static Stream<String> speed25() {
        return Stream.of("quarter throttle", "25 percent", "slow speed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED50.getAction());
    }

    static Stream<String> speed50() {
        return Stream.of("half throttle", "50 percent", "half speed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED75.getAction());
    }

    static Stream<String> speed75() {
        return Stream.of("three quarters throttle", "75 percent", "three quarter speed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED100.getAction());
    }

    static Stream<String> speed100() {
        return Stream.of("full throttle", "100 percent", "full speed", "max throttle");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, INCREASE_SPEED_BY.getAction());
    }

    static Stream<String> speedPlus() {
        return Stream.of("increase speed by 10", "increase speed by 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DECREASE_SPEED_BY.getAction());
    }

    static Stream<String> speedMinus() {
        return Stream.of("decrease speed by 10", "decrease speed by 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SET_OPTIMAL_SPEED.getAction());
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("set optimal speed", "optimal approach speed");
    }

    // =========================================================================
    // Navigation - second highest collision risk
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(30)
    @MethodSource
    void jumpToHyperspace(String input) throws InterruptedException {
        assertRouted(input, JUMP_TO_HYPERSPACE.getAction());
    }

    static Stream<String> jumpToHyperspace() {
        return Stream.of("jump to hyperspace", "jump", "let's get out of here", "lets go", "jump to next way point");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, ENTER_SUPER_CRUISE.getAction());
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("enter supercruise", "engage supercruise", "supercruise", "light speed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {

        assertRouted(input, DROP_FROM_SUPER_CRUISE.getAction());
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("drop here", "drop in", "drop out");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_NEXT_MISSION.getAction());
    }

    static Stream<String> navigateToMission() {
        return Stream.of("navigate to active mission", "plot route to active mission", "go to active mission",
                "navigate to mission", "go to mission");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_FLEET_CARRIER.getAction());
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("navigate to fleet carrier", "return to carrier", "take us to carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, NAVIGATION_OFF.getAction());
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("cancel navigation", "abort navigation", "stop navigation");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, GET_HEADING_TO_LZ.getAction());
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("navigate to landing zone", "bearing to landing zone", "take me back to LZ");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TARGET_DESTINATION.getAction());
    }

    static Stream<String> targetDestination() {
        return Stream.of("target destination", "select destination");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(38)
    @MethodSource
    void clearActiveMissions(String input) throws InterruptedException {
        assertRouted(input, CLEAR_ALL_ACTIVE_MISSIONS.getAction());
    }

    static Stream<String> clearActiveMissions() {
        return Stream.of("clear active missions", "clear all active missions", "delete active missions");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
    }

    static Stream<String> nextTradeStop() {
        return Stream.of("navigate to next trade stop", "go to next trade stop");
    }

    // =========================================================================
    // Flight / ship systems
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(40)
    @MethodSource
    void deployLandingGear(String input) throws InterruptedException {
        assertRouted(input, DEPLOY_LANDING_GEAR.getAction());
    }

    static Stream<String> deployLandingGear() {
        return Stream.of("landing gear", "gear down", "lower landing gear", "extend landing gear");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RETRACT_LANDING_GEAR.getAction());
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("retract landing gear", "gear up", "raise landing gear", "stow landing gear");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, REQUEST_DOCKING.getAction());
    }

    static Stream<String> requestDocking() {
        return Stream.of("request docking", "dock at station", "docking request", "request landing",
                "contact tower and get us landing pad", "request landing permission", "request landing pad",
                "request landing clearance");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, TOGGLE_CARGO_SCOOP.getAction());
    }

    static Stream<String> cargoScoop() {
        return Stream.of("open cargo scoop", "deploy cargo scoop", "open cargo bay", "open cargo bay door");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, NIGHT_VISION_ON_OFF.getAction());
    }

    static Stream<String> nightVision() {
        return Stream.of("night vision", "nightvision", "turn on night vision", "turn off night vision");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, LIGHTS_ON_OFF.getAction());
    }

    static Stream<String> lights() {
        return Stream.of("headlights", "lights on", "turn off lights", "lights", "turn on the lights");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DISMISS_SHIP.getAction());
    }

    static Stream<String> dismissShip() {
        return Stream.of("dismiss ship", "send ship away", "ship to orbit", "go play", "dismissed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TAXI.getAction());
    }

    static Stream<String> taxi() {
        return Stream.of("taxi to landing", "auto docking", "autopilot landing", "taxi", "auto taxi");
    }

    // =========================================================================
    // Combat / hardpoints
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(50)
    @MethodSource
    void deployHardpoints(String input) throws InterruptedException {
        assertRouted(input, DEPLOY_HARDPOINTS.getAction());
    }

    static Stream<String> deployHardpoints() {
        return Stream.of("deploy hardpoints", "weapons hot", "combat ready", "weapons free", "arm weapons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RETRACT_HARDPOINTS.getAction());
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("retract hardpoints", "weapons cold", "weapons away", "stand down", "holster weapons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DEPLOY_HEAT_SINK.getAction());
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("deploy heat sink", "launch heat sink", "dump heat");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, SELECT_HIGHEST_THREAT.getAction());
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("priority target", "target highest threat", "next enemy", "select enemy");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DEPLOY_SHIELD_CELL.getAction());
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("deploy shield cell", "use shield cell", "activate shield cell", "shield cell bank",
                "deploy power cell", "fire shield cell");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DEPLOY_CHAFF.getAction());
    }

    static Stream<String> deployChaff() {
        return Stream.of("deploy chaff", "launch chaff", "use chaff", "fire chaff", "launch flares", "deploy flares");
    }

    // =========================================================================
    // Power management
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(60)
    @MethodSource
    void powerToShields(String input) throws InterruptedException {
        assertRouted(input, INCREASE_SHIELDS_POWER.getAction());
    }

    static Stream<String> powerToShields() {
        return Stream.of("power to shields", "max shields", "boost shields");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, INCREASE_ENGINES_POWER.getAction());
    }

    static Stream<String> powerToEngines() {
        return Stream.of("power to engines", "max engines", "boost engines");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, INCREASE_WEAPONS_POWER.getAction());
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("power to weapons", "max weapons", "boost weapons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, RESET_POWER.getAction());
    }

    static Stream<String> resetPower() {
        return Stream.of("equalize power", "balance power", "reset power", "distribute power equally");
    }

    // =========================================================================
    // Science / exploration / mining
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(70)
    @MethodSource
    void openFss(String input) throws InterruptedException {
        assertRouted(input, OPEN_FSS.getAction());
    }

    static Stream<String> openFss() {
        return Stream.of("Open FSS and scan.", "Perform filtered spectrum scan", "full spectrum scan", "discovery scan");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("Navigate to next bio-sample", "Navigate to next organic", "navigate to codex entry");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FIND_MINING_SITE.getAction());
    }

    static Stream<String> findMiningSite() {
        return Stream.of("find mining site for alexandrite within 300 light years",
                "find mining location for bromelite with 1200 light years", "find asteroid field with gold");
    }

    // =========================================================================
    // Fleet carrier
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(80)
    @MethodSource
    void enterCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, ENTER_FLEET_CARRIER_DESTINATION.getAction());
    }

    static Stream<String> enterCarrierDestination() {
        return Stream.of("enter carrier destination", "set carrier destination", "enter next carrier destination");
    }

    /*
     * @ParameterizedTest(name = "[{index}] \"{0}\"")
     *
     * @Order(81)
     *
     * @MethodSource
     * void clearCarrierRoute(String input) throws InterruptedException {
     * assertRouted(input, CLEAR_FLEET_CARRIER_ROUTE.getAction());
     * }
     *
     * static Stream<String> clearCarrierRoute() {
     * return Stream.of("clear fleet carrier route", "cancel carrier route");
     * }
     */

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(82)
    @MethodSource
    void findNearestCarrier(String input) throws InterruptedException {
        assertRouted(input, FIND_NEAREST_FLEET_CARRIER.getAction());
    }

    static Stream<String> findNearestCarrier() {
        return Stream.of("find nearest fleet carrier", "nearest carrier");
    }

    // =========================================================================
    // Squadron carrier
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(85)
    @MethodSource
    void navigateToSquadronCarrier(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_SQUADRON_CARRIER.getAction());
    }

    static Stream<String> navigateToSquadronCarrier() {
        return Stream.of("navigate to squadron carrier", "go to squadron carrier", "head to squadron carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(86)
    @MethodSource
    void calculateNeutronRoute(String input) throws InterruptedException {
        assertRouted(input, CALCULATE_NEUTRON_STAR_ROUTE.getAction());
    }

    static Stream<String> calculateNeutronRoute() {
        return Stream.of("calculate neutron route with efficiency 20", "calculate neutron route");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(87)
    @MethodSource
    void plotNextNeutronLeg(String input) throws InterruptedException {
        assertRouted(input, PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
    }

    static Stream<String> plotNextNeutronLeg() {
        return Stream.of("next neutron star jump", "plot route to next neutron star waypoint", "next neutron star");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(88)
    @MethodSource
    void clearNeutronStarRoute(String input) throws InterruptedException {
        assertRouted(input, CLEAR_NEUTRON_ROUTE.getAction());
    }

    static Stream<String> clearNeutronStarRoute() {
        return Stream.of("clear neutron route");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_STATUS.getAction());
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("squadron carrier status", "squadron carrier finances", "squadron carrier balance",
                "how long can we operate the squadron carrier", "squadron carrier tritium", "squadron carrier fuel", "squadron carrier fuel level");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of("squadron carrier route", "how many jumps on the squadron carrier route",
                "squadron carrier route");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("where is the squadron carrier going", "squadron carrier final destination",
                "squadron carrier heading");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_ETA.getAction());
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("squadron carrier ETA", "when does the squadron carrier arrive",
                "how long until the squadron carrier arrives");
    }

    // =========================================================================
    // Disambiguation: bare "carrier" phrases must route to fleet, not squadron
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(250)
    @MethodSource
    void bareCarrierDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_FLEET_CARRIER.getAction());
    }

    static Stream<String> bareCarrierDefaultsToFleet() {
        return Stream.of("navigate to fleet carrier", "return to carrier", "take us to carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, FLEET_CARRIER_STATUS.getAction());
    }

    static Stream<String> bareCarrierStatusDefaultsToFleet() {
        return Stream.of("fleet carrier status", "fleet carrier balance", "fleet carrier funds");
    }

    // =========================================================================
    // App settings / announcements
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(90)
    @MethodSource
    void disableAnnouncements(String input) throws InterruptedException {
        assertRouted(input, TOGGLE_ALL_ANNOUNCEMENTS.getAction());
    }

    static Stream<String> disableAnnouncements() {
        return Stream.of("disable all announcements");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SET_REMINDER.getAction());
    }

    static Stream<String> setReminder() {
        return Stream.of("set reminder refuel at next stop");
    }

    // =========================================================================
    // UI panels - test a representative sample (they share similar vocabulary)
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(100)
    @MethodSource
    void galaxyMap(String input) throws InterruptedException {
        assertRouted(input, OPEN_GALAXY_MAP.getAction());
    }

    static Stream<String> galaxyMap() {
        return Stream.of("open galaxy map", "show galaxy map", "display galaxy map");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, OPEN_SYSTEM_MAP.getAction());
    }

    static Stream<String> systemMap() {
        return Stream.of("open local map", "show system map", "display system map");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_NAVIGATION.getAction());
    }

    static Stream<String> navigationPanel() {
        return Stream.of("show navigation panel", "open navigation panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_MODULES_PANEL.getAction());
    }

    static Stream<String> modulesPanel() {
        return Stream.of("show modules panel", "open modules panel", "display modules panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_STATUS_PANEL.getAction());
    }

    static Stream<String> statusPanel() {
        return Stream.of("show status panel", "open status panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_INVENTORY_PANEL.getAction());
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("show inventory panel", "open inventory panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, EXIT_CLOSE.getAction());
    }

    static Stream<String> closePanel() {
        return Stream.of("exit close panel", "close panel");
    }

    // =========================================================================
    // Queries - use primary phrase from each entry
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(200)
    @MethodSource
    void queryCurrentLocation(String input) throws InterruptedException {
        assertRouted(input, CURRENT_LOCATION.getAction());
    }

    static Stream<String> queryCurrentLocation() {
        return Stream.of("Where are we right now?", "what is our location", "where are we",
                "how long does the day last at current location");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, SHIP_LOADOUT.getAction());
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("ship loadout", "what am I flying", "ship equipment", "do you have fuel scoop equipped",
                "do you have weapons equipped", "what weapons do you have equipped", "do you have a refinery equipped");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, CARGO_HOLD_CONTENTS.getAction());
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("cargo hold", "what are we carrying", "cargo contents");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, PLOTTED_ROUTE_ANALYSIS.getAction());
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("plotted route", "jumps remaining", "how many jumps to destination", "are we there yet");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, QUERY_STATIONS.getAction());
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("stations in system", "what stations", "nearby stations",
                "are there any stations or ports here", "any ports in this star system");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, QUERY_STELLAR_OBJETS.getAction());
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("What landable planets or moons are in this system?",
                "Are there any ice rings this star system");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, QUERY_STELLAR_SIGNALS.getAction());
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of("What signals are in this system?", "What signals do you see?", "Any interesting signals?",
                "System signals?", "What's in this system?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("Which planets still need bio or organic scans?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of("What bio scans have we completed?", "What organics do we still have to scan?",
                "What organics or biology is on this planet");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, PLAYER_PROFILE_ANALYSIS.getAction());
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("player profile", "player profile summarize ranks", "player profile summarize progress");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, FLEET_CARRIER_STATUS.getAction());
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of("What is our fleet carrier range?", "What's my fleet carrier fuel status",
                "How long can we operate on current funds?", "How far can carrier we jump with current tritium?",
                "carrier tritium", "carrier fuel", "tritium level");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, DISTANCE_TO_CARRIER.getAction());
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("How far are we from the carrier?", "Distance from the fleet carrier?",
                "How far is the fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, FSD_TARGET_ANALYSIS.getAction());
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("FSD target", "what star are we targeting", "info on next jump");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, EXPLORATION_PROFITS.getAction());
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("Exploration profit potential in this system.",
                "What is the exploration profit potential in this system?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TIME_IN_ZONE.getAction());
    }

    static Stream<String> queryTime() {
        return Stream.of("current time", "what time is it", "utc time");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, SYSTEM_SECURITY_ANALYSIS.getAction());
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("system security", "who controls this system", "dominant faction");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, STATION_DETAILS.getAction());
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("station details", "what station services are at this station", "what services here",
                "station info");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, MATERIALS_INVENTORY.getAction());
    }

    static Stream<String> queryMaterials() {
        return Stream.of("material inventory iron", "how many iron do we have", "how much vanadium do we have");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, PLANET_MATERIALS.getAction());
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("What materials are available on this planet?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, DISTANCE_TO_BUBBLE.getAction());
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("How far are we from the Bubble?", "Distance to earth", "How far is earth",
                "how far to civilization", "how far to earth");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, LAST_SCAN_ANALYSIS.getAction());
    }

    static Stream<String> queryLastScan() {
        return Stream.of("Analyze the most recent scan?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, REMINDER.getAction());
    }

    static Stream<String> queryReminder() {
        return Stream.of("reminder", "what was the reminder", "any reminders");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, FLEET_CARRIER_ETA.getAction());
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("What's the ETA for our fleet carrier jump?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, QUERY_GEO_SIGNALS.getAction());
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("geo signals", "geological signals", "volcanic activity");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, ANALYZE_MARKETS.getAction());
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("local markets", "markets at stations and settlements", "markets at outposts in system");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, TOTAL_BOUNTIES.getAction());
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("bounties", "total bounties", "how much in bounties");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(230)
    @MethodSource
    void queryKeyBindings(String input) throws InterruptedException {
        assertRouted(input, KEY_BINDINGS_ANALYSIS.getAction());
    }

    static Stream<String> queryKeyBindings() {
        return Stream.of("check key bindings", "missing key bindings", "unbound keys");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, PLANET_BIOME_ANALYSIS.getAction());
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("Analyze the biome for this star system", "Biome analysis for planet a 1");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("Last bio-sample location and distance.", "How far are we from the last bio-sample?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of("What's on the carrier route?", "What's the route for our fleet carrier?",
                "How many jump on the carrier route?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SET_CARRIER_FUEL_RESERVE.getAction());
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("Set fuel reserve level to 5000", "Set fuel reserve to 10000", "Fuel reserve 15000",
                "Set fuel reserve to fifteen thousand");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DISEMBARK.getAction());
    }

    static Stream<String> disembark() {
        return Stream.of("disembark");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_COMMANDER_PANEL.getAction());
    }

    static Stream<String> openCentralPanel() {
        return Stream.of("Open commander panel", "open central panel", "open role panel", "open knee board");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_FIGHTER_PANEL.getAction());
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("show fighter panel", "open fighter panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FIGHTER_OPEN_ORDERS.getAction());
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("fighter open orders", "fire at will");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FIGHTER_REQUEST_FOCUS_TARGET.getAction());
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("fighter attack my target", "attack", "focus my target");
    }

    /*
     * @ParameterizedTest(name = "[{index}] \"{0}\"")
     *
     * @Order(236)
     *
     * @MethodSource
     * void nonsense(String input) throws InterruptedException {
     * assertRouted(input, IGNORE_NONSENSE.getAction());
     * }
     *
     * static Stream<String> nonsense() {
     * return Stream.of("youtube stream is at 5 tomorrow",
     * "what time should we meet",
     * "most to the time it should pay no attention to bogus data",
     * "the response time is fast", "what is the meaning of life",
     * "some other crap", "have to navigate though the potholes");
     * }
     */

}
