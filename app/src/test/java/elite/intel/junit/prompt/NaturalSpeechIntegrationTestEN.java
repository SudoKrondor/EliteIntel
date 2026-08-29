package elite.intel.junit.prompt;

import elite.intel.ai.brain.actions.handlers.commands.builtin.*;
import elite.intel.ai.brain.actions.handlers.queries.*;
import elite.intel.ai.brain.vega.input.CompanionRoutingHarness;
import elite.intel.ai.brain.vega.tools.RequestInputFunction;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


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
public class NaturalSpeechIntegrationTestEN {

    private final CompanionRoutingHarness harness = new CompanionRoutingHarness(Language.EN);

    @BeforeAll
    void bootstrap() throws Exception {
        harness.boot();
    }

    @AfterAll
    void teardown() {
        harness.shutdown();
    }

    // -------------------------------------------------------------------------
    // Core tester
    // -------------------------------------------------------------------------

    private void assertRouted(String input, String expectedAction) throws InterruptedException {
        harness.assertRouted(input, expectedAction);
    }

    // =========================================================================
    // Attention / control
    // =========================================================================


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, InterruptCommand.ID);
    }

    static Stream<String> interrupt() {
        return Stream.of("interrupt");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToCombatModeCommand.ID);
    }

    static Stream<String> combatMode() {
        return Stream.of("combat mode", "change to combat mode", "combat", "switch to combat mode",
                "swap to combat mode");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToAnalysisModeCommand.ID);
    }

    static Stream<String> analysisMode() {
        return Stream.of("Analysis mode", "switch to analysis mode", "explorer mode", "analysis HUD",
                "Change to analysis mode", "swap to analysis mode");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, ResetHeadLookAheadCommand.ID);
    }

    static Stream<String> lookAhead() {
        return Stream.of("look ahead", "reset", "reset head look", "head look to neutral");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(16)
    @MethodSource
    void honkTheSystem(String input) throws InterruptedException {
        assertRouted(input, RunSystemScanCommand.ID);
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
        assertRouted(input, SetSpeedZeroCommand.ID);
    }

    static Stream<String> speedZero() {
        return Stream.of("stop engines", "full stop", "all stop", "kill engines", "cut throttle", "zero throttle",
                "stop ship");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SetSpeed25Command.ID);
    }

    static Stream<String> speed25() {
        return Stream.of("quarter throttle", "25 percent", "slow speed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SetSpeed50Command.ID);
    }

    static Stream<String> speed50() {
        return Stream.of("half throttle", "50 percent", "half speed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SetSpeed75Command.ID);
    }

    static Stream<String> speed75() {
        return Stream.of("three quarters throttle", "75 percent", "three quarter speed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SetSpeed100Command.ID);
    }

    static Stream<String> speed100() {
        return Stream.of("full throttle", "100 percent", "full speed", "max throttle");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, IncreaseSpeedCommand.ID);
    }

    static Stream<String> speedPlus() {
        return Stream.of("increase speed by 10", "increase speed by 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DecreaseSpeedCommand.ID);
    }

    static Stream<String> speedMinus() {
        return Stream.of("decrease speed by 10", "decrease speed by 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SetOptimalSpeedCommand.ID);
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("set optimal speed", "optimal approach speed");
    }

    // =========================================================================
    // Missing-parameter clarification / continuation
    // =========================================================================

    @Test
    @Order(28)
    void missingSpeedAmountIsAppliedFromNextTurn() throws Exception {
        harness.restart();
        List<String> firstTurn = harness.routeWithActionVisible("increase speed", IncreaseSpeedCommand.ID);

        assertAll(
                () -> assertFalse(firstTurn.contains(IncreaseSpeedCommand.ID),
                        () -> "Incomplete command was dispatched: " + firstTurn),
                () -> assertTrue(firstTurn.contains(RequestInputFunction.ID),
                        () -> "Missing request_input dispatch: " + firstTurn),
                () -> assertTrue(harness.lastTurnRequestedInput(IncreaseSpeedCommand.ID, "key"),
                        () -> "Expected request_input for increase_speed.key; speech: "
                                + harness.lastTurnSpeech()),
                () -> assertFalse(harness.lastTurnSpeech().isEmpty(),
                        "The commander was not asked for the missing amount")
        );

        List<String> secondTurn = harness.routeWithActionVisible("by 10", IncreaseSpeedCommand.ID);

        assertAll(
                () -> assertTrue(secondTurn.contains(IncreaseSpeedCommand.ID),
                        () -> "Clarification reply dispatched " + secondTurn
                                + " instead of " + IncreaseSpeedCommand.ID),
                () -> assertEquals("10", harness.lastArgument(IncreaseSpeedCommand.ID, "key").orElse("<missing>"),
                        "The clarification value was not applied to increase_speed.key")
        );
    }

    @Test
    @Order(29)
    void newCommandSupersedesPendingClarification() throws Exception {
        harness.restart();
        try {
            List<String> firstTurn = harness.routeWithActionVisible("increase speed", IncreaseSpeedCommand.ID);

            assertAll(
                    () -> assertFalse(firstTurn.contains(IncreaseSpeedCommand.ID),
                            () -> "Incomplete command was dispatched: " + firstTurn),
                    () -> assertTrue(firstTurn.contains(RequestInputFunction.ID),
                            () -> "Missing request_input dispatch: " + firstTurn),
                    () -> assertTrue(harness.lastTurnRequestedInput(IncreaseSpeedCommand.ID, "key"),
                            () -> "Expected request_input for increase_speed.key; speech: "
                                    + harness.lastTurnSpeech())
            );

            List<String> secondTurn = harness.routeWithActionVisible("full stop", SetSpeedZeroCommand.ID);

            assertAll(
                    () -> assertTrue(secondTurn.contains(SetSpeedZeroCommand.ID),
                            () -> "New command dispatched " + secondTurn + " instead of " + SetSpeedZeroCommand.ID),
                    () -> assertFalse(secondTurn.contains(IncreaseSpeedCommand.ID),
                            () -> "Pending command was resumed instead of superseded: " + secondTurn)
            );
        } finally {
            harness.restart();
        }
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
        return Stream.of("jump to hyperspace", "jump", "let's get out of here", "lets go", "jump to next way point");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, EnterSuperCruiseCommand.ID);
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("enter supercruise", "engage supercruise", "supercruise", "light speed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {

        assertRouted(input, DropFromSuperCruiseCommand.ID);
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("drop here", "drop in", "drop out");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NavigateToMissionTargetCommand.ID);
    }

    static Stream<String> navigateToMission() {
        return Stream.of("navigate to active mission", "plot route to active mission", "go to active mission",
                "navigate to mission", "go to mission");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("navigate to fleet carrier", "return to carrier", "take us to carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, CancelNavigationCommand.ID);
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("cancel navigation", "abort navigation", "stop navigation");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, NavigateToLandingZoneCommand.ID);
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("navigate to landing zone", "bearing to landing zone", "take me back to LZ");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TargetDestinationCommand.ID);
    }

    static Stream<String> targetDestination() {
        return Stream.of("target destination", "select destination");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(38)
    @MethodSource
    void clearActiveMissions(String input) throws InterruptedException {
        assertRouted(input, ClearActiveMissionsCommand.ID);
    }

    static Stream<String> clearActiveMissions() {
        return Stream.of("clear active missions", "clear all active missions", "delete active missions");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NavigateToTradeStopCommand.ID);
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
        assertRouted(input, DeployLandingGearCommand.ID);
    }

    static Stream<String> deployLandingGear() {
        return Stream.of("landing gear", "gear down", "lower landing gear", "extend landing gear");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RetractLandingGearCommand.ID);
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("retract landing gear", "gear up", "raise landing gear", "stow landing gear");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, RequestDockingCommand.ID);
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
        assertRouted(input, ToggleCargoScoopCommand.ID);
    }

    static Stream<String> cargoScoop() {
        return Stream.of("open cargo scoop", "deploy cargo scoop", "open cargo bay", "open cargo bay door");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, ToggleNightVisionOnOffCommand.ID);
    }

    static Stream<String> nightVision() {
        return Stream.of("night vision", "nightvision", "turn on night vision", "turn off night vision");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, ToggleLightsOnOffCommand.ID);
    }

    static Stream<String> lights() {
        return Stream.of("headlights", "lights on", "turn off lights", "lights", "turn on the lights");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DismissShipToOrbitCommand.ID);
    }

    static Stream<String> dismissShip() {
        return Stream.of("dismiss ship", "send ship away", "ship to orbit", "go play", "dismissed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TaxiToLandingPadCommand.ID);
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
        assertRouted(input, DeployHardpointsCommand.ID);
    }

    static Stream<String> deployHardpoints() {
        return Stream.of("deploy hardpoints", "weapons hot", "combat ready", "weapons free", "arm weapons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RetractHardpointsCommand.ID);
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("retract hardpoints", "weapons cold", "weapons away", "stand down", "holster weapons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DeployHeatSinkCommand.ID);
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("deploy heat sink", "launch heat sink", "dump heat");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, TargetHostileHighestThreatCommand.ID);
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("priority target", "target highest threat", "next enemy", "select enemy");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DeployShieldCellCommand.ID);
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("deploy shield cell", "use shield cell", "activate shield cell", "shield cell bank",
                "deploy power cell", "fire shield cell");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DeployChaffCommand.ID);
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
        assertRouted(input, TransferPowerToShieldsCommand.ID);
    }

    static Stream<String> powerToShields() {
        return Stream.of("power to shields", "max shields", "boost shields");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToEnginesCommand.ID);
    }

    static Stream<String> powerToEngines() {
        return Stream.of("power to engines", "max engines", "boost engines");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToWeaponsCommand.ID);
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("power to weapons", "max weapons", "boost weapons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, EqualizePowerCommand.ID);
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
        assertRouted(input, OpenFssScanSystemCommand.ID);
    }

    static Stream<String> openFss() {
        return Stream.of("Open FSS.", "open filtered spectrum scan", "full spectrum scan");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NavigateToBioSampleCodexEntryCommand.ID);
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("Navigate to next bio-sample", "Navigate to next organic", "navigate to codex entry");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FindMiningSiteCommand.ID);
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
        assertRouted(input, EnterFleetCarrierDestinationCommand.ID);
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
        assertRouted(input, FindNearestFleetCarrierCommand.ID);
    }

    static Stream<String> findNearestCarrier() {
        return Stream.of("find nearest fleet carrier", "nearest carrier");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(83)
    @MethodSource
    void openFleetCarrierPanel(String input) throws InterruptedException {
        assertRouted(input, DisplayFleetCarrierManagementPanelCommand.ID);
    }

    static Stream<String> openFleetCarrierPanel() {
        return Stream.of("display fleet carrier panel", "open fleet carrier panel", "fleet carrier panel",
                "open career management panel"); // STT acoustic confusion: career -> carrier
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
        return Stream.of("navigate to squadron carrier", "go to squadron carrier", "head to squadron carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(86)
    @MethodSource
    void calculateNeutronRoute(String input) throws InterruptedException {
        assertRouted(input, CalculateNeutronStarRouteCommand.ID);
    }

    static Stream<String> calculateNeutronRoute() {
        return Stream.of(
                "calculate neutron route with efficiency 20",
                "calculate neutron star route at 60 efficiency"
        );
    }

    /**
     * A bare "calculate neutron route" is a complete order, not an incomplete one: efficiency is optional and the
     * command plots at its default rather than asking the commander a question. Acting always outranks conversing,
     * so the only acceptable outcome is a dispatch. The cross-turn clarification mechanism itself is exercised
     * where a parameter genuinely has no default - see {@link #missingSpeedAmountIsAppliedFromNextTurn()}.
     */
    @Test
    @Order(86)
    void bareNeutronRouteDispatchesAtDefaultEfficiencyInsteadOfAsking() throws Exception {
        harness.restart();
        try {
            List<String> firstTurn = harness.routeWithActionVisible(
                    "calculate neutron route", CalculateNeutronStarRouteCommand.ID);

            assertAll(
                    () -> assertTrue(firstTurn.contains(CalculateNeutronStarRouteCommand.ID),
                            () -> "A bare neutron-route order must plot the route, but dispatched " + firstTurn
                                    + "; speech: " + harness.lastTurnSpeech()),
                    () -> assertFalse(firstTurn.contains(RequestInputFunction.ID),
                            () -> "The commander was asked for an efficiency that has a default; speech: "
                                    + harness.lastTurnSpeech())
            );
        } finally {
            harness.restart();
        }
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(87)
    @MethodSource
    void plotNextNeutronLeg(String input) throws InterruptedException {
        assertRouted(input, PlotRouteNextNeutronStarWaypointCommand.ID);
    }

    static Stream<String> plotNextNeutronLeg() {
        return Stream.of("take me to the next neutron star", "plot route to next neutron star waypoint", "next neutron star");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(88)
    @MethodSource
    void clearNeutronStarRoute(String input) throws InterruptedException {
        assertRouted(input, ClearNeutronRouteCommand.ID);
    }

    static Stream<String> clearNeutronStarRoute() {
        return Stream.of("clear neutron route");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("squadron carrier status", "squadron carrier finances", "squadron carrier balance",
                "how long can we operate the squadron carrier",
                "squadron carrier tritium", "squadron carrier fuel", "squadron carrier fuel level");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of(
                "squadron carrier route",
                "how many jumps on the squadron carrier route",
                "squadron carrier route"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("where is the squadron carrier going", "squadron carrier final destination",
                "squadron carrier heading");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierDepartureEtaQuery.ID);
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("squadron carrier ETA", "when does the squadron carrier arrive",
                "how long until the squadron carrier arrives");
    }

    // =========================================================================
    // Disambiguation: bare "carrier" phrases must reach the fleet-carrier COMMAND, not the squadron one.
    // Carrier QUERIES no longer split by owner - one tool each, owner resolved from the utterance
    // (see CarrierOwnershipTest), so a bare phrase and a "squadron" phrase share a tool by design.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(250)
    @MethodSource
    void bareCarrierDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> bareCarrierDefaultsToFleet() {
        return Stream.of("navigate to fleet carrier", "return to carrier", "take us to carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusRoutesToStatusQuery(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> bareCarrierStatusRoutesToStatusQuery() {
        return Stream.of("fleet carrier status", "fleet carrier balance", "fleet carrier funds");
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
        return Stream.of("disable all announcements");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SetReminderCommand.ID);
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
        assertRouted(input, DisplayOpenGalaxyMapCommand.ID);
    }

    static Stream<String> galaxyMap() {
        return Stream.of("open galaxy map", "show galaxy map", "display galaxy map");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenSystemMapCommand.ID);
    }

    static Stream<String> systemMap() {
        return Stream.of("open local map", "show system map", "display system map");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, ShowNavigationPanelCommand.ID);
    }

    static Stream<String> navigationPanel() {
        return Stream.of("show navigation panel", "open navigation panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, ShowModulesPanelCommand.ID);
    }

    static Stream<String> modulesPanel() {
        return Stream.of("show modules panel", "open modules panel", "display modules panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, ShowStatusPanelCommand.ID);
    }

    static Stream<String> statusPanel() {
        return Stream.of("show status panel", "open status panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, ShowInventoryPanelCommand.ID);
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("show inventory panel", "open inventory panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, ExitCloseCommand.ID);
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
        assertRouted(input, AnalyzeCurrentLocationQuery.ID);
    }

    static Stream<String> queryCurrentLocation() {
        return Stream.of("Where are we right now?", "what is our location", "where are we",
                "how long does the day last at current location");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, AnalyzeShipLoadoutQuery.ID);
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("ship loadout", "what am I flying", "ship equipment", "do you have fuel scoop equipped",
                "do you have weapons equipped", "what weapons do you have equipped", "do you have a refinery equipped");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCargoHoldQuery.ID);
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("cargo hold", "what are we carrying", "cargo contents");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeRouterQuery.ID);
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("plotted route", "jumps remaining", "how many jumps to destination", "are we there yet");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStationsQuery.ID);
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("stations in system", "what stations", "nearby stations",
                "are there any stations or ports here", "any ports in this star system");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarObjectsQuery.ID);
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("What landable planets or moons are in this system?",
                "Are there any ice rings this star system");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarSignalsQuery.ID);
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of(
                "What signals are in this system?",
                "What signals do you see?",
                "Are there resource sites in this system?",
                "System signals?",
                "What's in this system?"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioScansStarSystemQuery.ID);
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("Which planets still need bio or organic scans?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioSamplesPlanetSurfaceQuery.ID);
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of("What bio scans have we completed?", "What organics do we still have to scan?",
                "What organics or biology is we still need to scan on this planet");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, AnalyzePlayerProfileQuery.ID);
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("player profile", "player profile summarize ranks", "player profile summarize progress");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of(
                "What is our fleet carrier range?",
                "What's my fleet carrier fuel status",
                "How long can fleet carrier operate on current funds?",
                "How far can carrier we jump with current tritium?",
                "what is carrier tritium status",
                "what is carrier fuel status",
                "tritium level"
        );
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromFleetCarrierQuery.ID);
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("How far are we from the carrier?", "Distance from the fleet carrier?",
                "How far is the fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFsdTargetQuery.ID);
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("FSD target", "what star are we targeting", "info on next jump");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, AnalyzeExplorationProfitsQuery.ID);
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("Exploration profit potential in this system.",
                "What is the exploration profit potential in this system?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TimeQuery.ID);
    }

    static Stream<String> queryTime() {
        return Stream.of("current time", "what time is it");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSystemSecurityQuery.ID);
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("system security", "who controls this system", "dominant faction");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, StationDataQuery.ID);
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("station details", "what station services are at this station", "what services here",
                "station info");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyseMaterialsQuery.ID);
    }

    static Stream<String> queryMaterials() {
        return Stream.of("material inventory iron", "how many iron do we have", "how much vanadium do we have");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMaterialsOnPlanetQuery.ID);
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("What materials are available on this planet?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromTheBubbleQuery.ID);
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("How far are we from the Bubble?", "Distance to earth", "How far is earth",
                "how far to civilization", "how far to earth");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, AnalyzeLastScanQuery.ID);
    }

    static Stream<String> queryLastScan() {
        return Stream.of("Analyze the most recent scan?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, RemindTargetDestinationQuery.ID);
    }

    static Stream<String> queryReminder() {
        return Stream.of("reminder", "what was the reminder", "any reminders");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierDepartureEtaQuery.ID);
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("What's the ETA for our fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeGeologyInStarSystemQuery.ID);
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("geo signals", "geological signals", "volcanic activity");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMarketsQuery.ID);
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("local markets", "markets at stations and settlements", "markets at outposts in system");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBountiesCollectedQuery.ID);
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("bounties", "total bounties", "how much in bounties");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, BiomeAnalyzerQuery.ID);
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("Analyze the biome for this star system", "Biome analysis for planet a 1");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromLastBioSampleQuery.ID);
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("What is the distance to last bio sample?", "How far are we from the last bio-sample?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of(
                "Analyze carrier route",
                "What's the route for our fleet carrier?",
                "How many jump on the carrier route?"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> queryCarrierDestination() {
        return Stream.of("Where is the fleet carrier headed?", "What's the carrier's final destination?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SetCarrierFuelReserveCommand.ID);
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("Set fuel reserve level to 5000", "Set fuel reserve to 10000", "Fuel reserve 15000",
                "Set fuel reserve to fifteen thousand");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DisembarkCommand.ID);
    }

    static Stream<String> disembark() {
        return Stream.of("disembark");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, ShowCentralPanelCommand.ID);
    }

    static Stream<String> openCentralPanel() {
        return Stream.of("Open commander panel", "open central panel", "open knee board");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, ShowFighterPanelCommand.ID);
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("show fighter panel", "open fighter panel");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FighterFireAtWillCommand.ID);
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("fighter open orders", "fire at will");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FighterAttackTargetCommand.ID);
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("fighter attack my target", "attack", "focus my target");
    }

    // =========================================================================
    // Trade route
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(300)
    @MethodSource
    void cancelTradeRoute(String input) throws InterruptedException {
        assertRouted(input, CancelTradeRouteCommand.ID);
    }

    static Stream<String> cancelTradeRoute() {
        return Stream.of("cancel the trade route", "abort our trading run", "clear the trade route",
                "forget the trade route");
    }

    // =========================================================================
    // Navigation - home system, clipboard, surface coordinates
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(305)
    @MethodSource
    void setHomeSystem(String input) throws InterruptedException {
        assertRouted(input, SetHomeSystemCommand.ID);
    }

    static Stream<String> setHomeSystem() {
        return Stream.of("set home system", "make this system our home", "mark this system as home");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(306)
    @MethodSource
    void navigateToHomeSystem(String input) throws InterruptedException {
        assertRouted(input, NavigateToHomeSystemCommand.ID);
    }

    static Stream<String> navigateToHomeSystem() {
        return Stream.of("take us home", "navigate home", "plot a route to our home system", "head home");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(307)
    @MethodSource
    void navigateFromMemory(String input) throws InterruptedException {
        assertRouted(input, NavigateFromMemoryCommand.ID);
    }

    static Stream<String> navigateFromMemory() {
        return Stream.of("navigate from memory", "paste from memory",
                "plot a route to the system on the clipboard");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(308)
    @MethodSource
    void navigateToCoordinates(String input) throws InterruptedException {
        assertRouted(input, NavigateToCoordinatesCommand.ID);
    }

    static Stream<String> navigateToCoordinates() {
        return Stream.of("navigate to coordinates latitude 12.5 longitude 78.9",
                "take me to lat 45.2 lon 130.7",
                "plot a surface course to latitude minus 12.3 longitude minus 40.5");
    }

    /**
     * The surface waypoint is useless without both halves of the fix, so assert lat and lon actually arrive.
     */
    @Test
    @Order(309)
    void navigateToCoordinatesCarriesLatAndLon() throws Exception {
        List<String> tools = harness.routeWithActionVisible(
                "navigate to coordinates latitude 12.5 longitude 78.9", NavigateToCoordinatesCommand.ID);

        assertAll(
                () -> assertTrue(tools.contains(NavigateToCoordinatesCommand.ID),
                        () -> "Dispatched " + tools + " instead of " + NavigateToCoordinatesCommand.ID),
                () -> assertEquals("12.5",
                        harness.lastArgument(NavigateToCoordinatesCommand.ID, "lat").orElse("<missing>"),
                        "Latitude was not passed to navigate_to_coordinates.lat"),
                () -> assertEquals("78.9",
                        harness.lastArgument(NavigateToCoordinatesCommand.ID, "lon").orElse("<missing>"),
                        "Longitude was not passed to navigate_to_coordinates.lon")
        );
    }

    // =========================================================================
    // Undock - "launch" must mean the SHIP here, not a fighter / SRV / Nomad
    // (the command is visible only while docked; see LaunchShipDetachFromStationCommand)
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(310)
    @MethodSource
    void launchShipDetachFromStation(String input) throws InterruptedException {
        assertRouted(input, LaunchShipDetachFromStationCommand.ID);
    }

    static Stream<String> launchShipDetachFromStation() {
        return Stream.of("undock", "launch the ship", "take off", "detach from the station",
                "release the docking clamps", "get us off this pad");
    }

    // =========================================================================
    // Combat targeting - subsystems and wing
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(315)
    @MethodSource
    void targetSubsystem(String input) throws InterruptedException {
        assertRouted(input, TargetSubsystemCommand.ID);
    }

    static Stream<String> targetSubsystem() {
        return Stream.of("target their power plant", "target the FSD", "target enemy drives",
                "target the shield generator", "target life support");
    }

    /**
     * The subsystem name is the whole payload of this command, so assert it survives routing.
     */
    @Test
    @Order(316)
    void targetSubsystemCarriesTheSubsystemName() throws Exception {
        List<String> tools = harness.routeWithActionVisible("target the power distributor",
                TargetSubsystemCommand.ID);

        assertAll(
                () -> assertTrue(tools.contains(TargetSubsystemCommand.ID),
                        () -> "Dispatched " + tools + " instead of " + TargetSubsystemCommand.ID),
                () -> assertEquals("power distributor",
                        harness.lastArgument(TargetSubsystemCommand.ID, "key").orElse("<missing>"),
                        "The subsystem name was not passed to target_subsystem.key")
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(317)
    @MethodSource
    void targetWingman1(String input) throws InterruptedException {
        assertRouted(input, TargetWingman1Command.ID);
    }

    static Stream<String> targetWingman1() {
        return Stream.of("target wingman one", "target wingman alpha", "select wing-mate one");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(318)
    @MethodSource
    void targetWingman2(String input) throws InterruptedException {
        assertRouted(input, TargetWingman2Command.ID);
    }

    static Stream<String> targetWingman2() {
        return Stream.of("target wingman two", "target wingman bravo", "select wing-mate two");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(319)
    @MethodSource
    void targetWingman3(String input) throws InterruptedException {
        assertRouted(input, TargetWingman3Command.ID);
    }

    static Stream<String> targetWingman3() {
        return Stream.of("target wingman three", "target wingman charlie", "select wing-mate three");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(320)
    @MethodSource
    void wingNavLock(String input) throws InterruptedException {
        assertRouted(input, WingNavLockCommand.ID);
    }

    static Stream<String> wingNavLock() {
        return Stream.of("wing nav lock", "lock onto my wingman's frame shift drive",
                "follow my wingmate into supercruise", "engage wing nav lock");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(321)
    @MethodSource
    void selectFireGroupByNato(String input) throws InterruptedException {
        assertRouted(input, SelectFireGroupByNatoCommand.ID);
    }

    static Stream<String> selectFireGroupByNato() {
        return Stream.of("select fire group bravo", "switch to fire group alpha", "fire group charlie");
    }

    /**
     * The NATO word must reach the command verbatim and in lower case - it is the group selector.
     */
    @Test
    @Order(322)
    void selectFireGroupCarriesTheNatoWord() throws Exception {
        List<String> tools = harness.routeWithActionVisible("switch to fire group bravo",
                SelectFireGroupByNatoCommand.ID);

        assertAll(
                () -> assertTrue(tools.contains(SelectFireGroupByNatoCommand.ID),
                        () -> "Dispatched " + tools + " instead of " + SelectFireGroupByNatoCommand.ID),
                () -> assertEquals("bravo",
                        harness.lastArgument(SelectFireGroupByNatoCommand.ID, "key").orElse("<missing>"),
                        "The NATO word was not passed to select_fire_group_by_nato.key")
        );
    }

    // =========================================================================
    // Ship-launched fighter - deployment and orders
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(325)
    @MethodSource
    void deployFighter(String input) throws InterruptedException {
        assertRouted(input, DeployFighterCommand.ID);
    }

    static Stream<String> deployFighter() {
        return Stream.of("deploy fighter", "launch the fighter", "get the fighter out",
                "launch the ship launched fighter");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(326)
    @MethodSource
    void fighterDefend(String input) throws InterruptedException {
        assertRouted(input, FighterDefendCommand.ID);
    }

    static Stream<String> fighterDefend() {
        return Stream.of("fighter defend the ship", "order the fighter to defend me", "fighter defensive");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(327)
    @MethodSource
    void fighterHoldFire(String input) throws InterruptedException {
        assertRouted(input, FighterHoldFireCommand.ID);
    }

    static Stream<String> fighterHoldFire() {
        return Stream.of("fighter hold fire", "tell the fighter to cease fire", "fighter stand down");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(328)
    @MethodSource
    void fighterReturnToShip(String input) throws InterruptedException {
        assertRouted(input, FighterReturnToShipCommand.ID);
    }

    static Stream<String> fighterReturnToShip() {
        return Stream.of("recall the fighter", "fighter return to ship", "order the fighter to dock");
    }

    // =========================================================================
    // SRV / Nomad / on foot
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(330)
    @MethodSource
    void driveAssist(String input) throws InterruptedException {
        assertRouted(input, DriveAssistCommand.ID);
    }

    static Stream<String> driveAssist() {
        return Stream.of("drive assist", "toggle drive assist", "turn on drive assist",
                "turn off driving assist");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(331)
    @MethodSource
    void recoverSrv(String input) throws InterruptedException {
        assertRouted(input, RecoverSrvVehicleGetOnBoardShipCommand.ID);
    }

    static Stream<String> recoverSrv() {
        return Stream.of("recover the SRV", "board the ship", "put the buggy back in the hangar",
                "dock the SRV");
    }

    /**
     * Nomad is aerial but reports as an SRV; "launch" here must not reach the fighter or the ship undock.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(332)
    @MethodSource
    void launchNomad(String input) throws InterruptedException {
        assertRouted(input, LaunchNomadCommand.ID);
    }

    static Stream<String> launchNomad() {
        return Stream.of("launch the nomad", "deploy the nomad", "deploy the planetary scout",
                "launch scout");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(333)
    @MethodSource
    void returnToSurface(String input) throws InterruptedException {
        assertRouted(input, ReturnToSurfaceCommand.ID);
    }

    static Stream<String> returnToSurface() {
        return Stream.of("return to surface", "pick me up", "bring the ship down to me",
                "recall the ship to my position");
    }

    // =========================================================================
    // Traders
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(340)
    @MethodSource
    void findRawMaterialTrader(String input) throws InterruptedException {
        assertRouted(input, FindRawMaterialTraderCommand.ID);
    }

    static Stream<String> findRawMaterialTrader() {
        return Stream.of("find a raw material trader", "nearest raw trader",
                "where can we trade raw materials", "plot a route to a raw material trader");
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


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(10)
    @MethodSource
    void startListening(String input) throws InterruptedException {
        assertRouted(input, WakeupCommand.ID);
    }

    static Stream<String> startListening() {
        return Stream.of("wake up", "wake");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SleepCommand.ID);
    }

    static Stream<String> ignoreMe() {
        return Stream.of("ignore me", "do not monitor", "sleep");
    }
}
