package elite.intel.junit.prompt;


import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;
import elite.intel.companion.input.CompanionRoutingHarness;
import elite.intel.i18n.Language;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;



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
public class NaturalSpeechIntegrationTestIT {

    private final CompanionRoutingHarness harness = new CompanionRoutingHarness(Language.IT);

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
    @Order(10)
    @MethodSource
    void startListening(String input) throws InterruptedException {
        assertRouted(input, WakeupCommand.ID);
    }

    static Stream<String> startListening() {
        return Stream.of("riattivati", "svegliati", "ascoltami");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SleepCommand.ID);
    }

    static Stream<String> ignoreMe() {
        return Stream.of("sospenditi", "vai in standby", "ignorami", "non ascoltarmi più", "ignora i miei comandi", "ignora i comandi");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, InterruptCommand.ID);
    }

    static Stream<String> interrupt() {
        return Stream.of("stai zitto", "silenzio", "chiudi la bocca", "smettila di parlare");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToCombatModeCommand.ID);
    }

    static Stream<String> combatMode() {
        return Stream.of("passa in modalità combattimento", "cambia in modalità combattimento", "attiva la modalità combattimento", "modalità combattimento");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToAnalysisModeCommand.ID);
    }

    static Stream<String> analysisMode() {
        return Stream.of("passa in modalità analisi", "cambia in modalità analisi", "attiva la modalità analisi", "modalità analisi");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, ResetHeadLookAheadCommand.ID);
    }

    static Stream<String> lookAhead() {
        return Stream.of("guarda avanti", "guarda dritto", "ripristina la posizione della testa");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(16)
    @MethodSource
    void honkTheSystem(String input) throws InterruptedException {
        assertRouted(input, RunSystemScanCommand.ID);
    }

    static Stream<String> honkTheSystem() {
        return Stream.of("esplora il sistema", "esegui una scansione del sistema");
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
        return Stream.of("spegni i motori", "fermati", "arresto completo", "arresto totale", "alt", "zero velocità", "ferma la nave");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SetSpeed25Command.ID);
    }

    static Stream<String> speed25() {
        return Stream.of("velocità a un quarto", "25 per cento", "velocità bassa");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SetSpeed50Command.ID);
    }

    static Stream<String> speed50() {
        return Stream.of("velocità a metà", "50 per cento", "velocità media");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SetSpeed75Command.ID);
    }

    static Stream<String> speed75() {
        return Stream.of("velocità a tre quarti", "75 per cento", "velocità alta");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SetSpeed100Command.ID);
    }

    static Stream<String> speed100() {
        return Stream.of("velocità massima", "100 per cento");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, IncreaseSpeedCommand.ID);
    }

    static Stream<String> speedPlus() {
        return Stream.of("aumenta la velocità di 10", "aumenta la velocità di 5", "aumenta la velocità del 10 per cento", "aumenta la velocità del 5 per cento");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DecreaseSpeedCommand.ID);
    }

    static Stream<String> speedMinus() {
        return Stream.of("diminuisci la velocità di 10", "diminuisci la velocità di 5", "diminuisci la velocità del 10 per cento", "diminuisci la velocità del 5 per cento");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SetOptimalSpeedCommand.ID);
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("imposta velocità ottimale", "velocità di avvicinamento ottimale", "velocità ottimale");
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
        return Stream.of("salta nell'iperspazio", "salta", "entra nell'iperspazio", "attivare", "prossimo salto");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, EnterSuperCruiseCommand.ID);
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("entra in supercruise", "entra in supercrociera", "attiva supercruise", "attiva in supercrociera",
                "supercruise", "supercrociera", "velocità luce", "vai in supercruise", "vai in supercrociera");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {

        assertRouted(input, DropFromSuperCruiseCommand.ID);
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("uscire", "uscire qui", "uscire dalla supercrociera", "abbandonare la supercrociera", "uscire da supercruise",
                "uscire da velocità luce");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NavigateToMissionTargetCommand.ID);
    }

    static Stream<String> navigateToMission() {
        return Stream.of("vai alla missione attiva", "traccia la rotta verso la missione attiva", "traccia il percorso verso la missione", "portami alla missione", "vai alla missione 1");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("raggiungere la fleet carrier", "andare alla portanavi", "vai verso la fleet carrier",
                "tornare alla fleetcarrier", "portami alla portanavi", "portami alla fleet carrier",
                "dirigiti alla portanavi", "rotta verso la fleet carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, CancelNavigationCommand.ID);
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("cancella la navigazione", "cancella l'itinerario", "cancella il percorso", "annulla la navigazione", "annulla itinerario");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, NavigateToLandingZoneCommand.ID);
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("vai alla zona di atterraggio", "direzione zona di atterraggio", "direzione piazzola di atterraggio",
                "rotta verso la zona di atterraggio", "traccia la rotta verso la zona di atterraggio", "vai all'area di atterraggio");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TargetDestinationCommand.ID);
    }

    static Stream<String> targetDestination() {
        return Stream.of("imposta la prossima destinazione", "imposta la destinazione successiva", "imposta la tappa successiva dell'itinerario",
                "seleziona la prossima destinazione", "seleziona la tappa successiva dell'itinerario", "seleziona la destinazione FSD");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(38)
    @MethodSource
    void clearActiveMissions(String input) throws InterruptedException {
        assertRouted(input, ClearActiveMissionsCommand.ID);
    }

    static Stream<String> clearActiveMissions() {
        return Stream.of("elimina missioni attive", "cancella tutte le missioni attive", "rimuovi tutte le missioni attive");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NavigateToTradeStopCommand.ID);
    }

    static Stream<String> nextTradeStop() {
        return Stream.of("vai alla prossima fermata commerciale", "prossima tappa commerciale", "vai al prossimo scalo commerciale", "vai alla prossima sosta commerciale");
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
        return Stream.of("estrai il carrello", "giù il carrello", "schiera il carrello", "abbassa il carrello", "schierare il carrello");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RetractLandingGearCommand.ID);
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("ritira il carrello", "su il carrello", "retrai il carrello");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, RequestDockingCommand.ID);
    }

    static Stream<String> requestDocking() {
        return Stream.of("richiesta di attracco", "attracco alla stazione", "richiesta di atterraggio",
                "contattare la torre di controllo e richiedere una piattaforma di atterraggio",
                "richiesta di autorizzazione all'atterraggio", "richiesta di piattaforma di atterraggio");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, ToggleCargoScoopCommand.ID);
    }

    static Stream<String> cargoScoop() {
        return Stream.of("apri cargo scoop", "apri lo scoop", "apri scoop di carico", "apri vano di carico",
                "chiudi cargo scoop", "chiudi lo scoop", "chiudi scoop di carico", "chiudi vano di carico");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, ToggleNightVisionOnOffCommand.ID);
    }

    static Stream<String> nightVision() {
        return Stream.of("visione notturna", "attiva visione notturna", "disattiva visione notturna", "accendi visione notturna",
                "spegni visione notturna", "attiva night vision", "disattiva night vision", "accendi night vision", "spegni night vision");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, ToggleLightsOnOffCommand.ID);
    }

    static Stream<String> lights() {
        return Stream.of("fari", "luci","accendi i fari", "accendi le luci", "spegni i fari", "spegni le luci", "fari accesi",
                "fari spenti", "accendi fari esterni");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DismissShipToOrbitCommand.ID);
    }

    static Stream<String> dismissShip() {
        return Stream.of("allontana la nave", "invia la nave in orbita", "nave in orbita", "nave vai via", "nave torna in orbita",
                "nave allontanati", "nave vai in orbita");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TaxiToLandingPadCommand.ID);
    }

    static Stream<String> taxi() {
        return Stream.of("autoatterraggio", "autoattracco", "pilota automatico", "attracco automatico", "atterraggio automatico");
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
        return Stream.of("dispiega le armi", "attiva le armi", "fuori le armi", "armi pronte", "pronto al combattimento",
                "armi attive", "armi fuori", "armi schierate", "armi pronte al fuoco", "armi pronte al combattimento");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RetractHardpointsCommand.ID);
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("ritira le armi", "disattiva le armi", "via le armi", "armi ritirate", "armi disattivate", "armi in standby");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DeployHeatSinkCommand.ID);
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("utilizza l'heat sink", "lancia heat sink", "scarica calore", "dissipatore di calore",
                "attiva heat sink", "usa heat sink", "lancia dissipatore di calore", "utilizza dissipatore di calore");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, TargetHostileHighestThreatCommand.ID);
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("mira al bersaglio più pericoloso", "seleziona nemico", "seleziona minaccia più alta",
                "mira al nemico più pericoloso", "seleziona il nemico più pericoloso", "mira alla minaccia più alta",
                "nemico più pericoloso");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DeployShieldCellCommand.ID);
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("attiva celle scudo", "usa celle scudo", "attiva SCB", "usa SCB", "usa banco cella scudo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DeployChaffCommand.ID);
    }

    /// Countermeasures are not chaff, Countermeasures are radio jamming signal for misiles. a different command.
    static Stream<String> deployChaff() {
        return Stream.of("lancia chaff", "lancia contromisure", "usa chaff", "contromisure per i missili",
                "lancia chaff per i missili", "usa contromisure", "attiva chaff");
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
        return Stream.of("potenza agli scudi", "max scudi", "boosta scudi", "massimizza scudi", "scudi al massimo",
                "scudi al massimo livello");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToEnginesCommand.ID);
    }

    static Stream<String> powerToEngines() {
        return Stream.of("potenza ai motori", "potenza max motori", "boost motori", "massimizza motori",
                "potenza motori al massimo", "potenza motori al massimo livello");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToWeaponsCommand.ID);
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("potenza alle armi", "max armi", "boosta armi", "massimizza armi", "armi al massimo",
                "armi al massimo livello");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, EqualizePowerCommand.ID);
    }

    static Stream<String> resetPower() {
        return Stream.of("bilancia potenza", "reset potenza", "distribuisci potenza equamente", "riporta potenza a livelli normali");
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
        return Stream.of("apri FSS ed esegui scansione", "esegui scansione a spettro filtrato", "scansione a spettro completo",
                "scan completo", "fss");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NavigateToBioSampleCodexEntryCommand.ID);
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("vai al prossimo campione biologico", "vai al prossimo campione", "vai alla voce del codex",
                "cerca il prossimo campione biologico");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FindMiningSiteCommand.ID);
    }

    /// NOTE: The material is required for the query. "find mining site" will always fail that is by design.
    static Stream<String> findMiningSite() {
        return Stream.of("trova sito di estrazione per alessandrite entro 300 anni luce",
                "trova posizione di estrazione per bromelite entro 1200 anni luce", "trova campo di asteroidi con oro",
                "trova campo di asteroidi con platino", "trova campo di asteroidi con ferro");
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
        return Stream.of("immetti la destinazione della fleet carrier", "imposta la destinazione della fleet carrier",
                "imposta la prossima destinazione della fleet carrier");
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
        return Stream.of("trova fleet carrier più vicina", "fleet carrier più vicina", "trova la fleet carrier più vicina",
            "trova la portanavi più vicina", "trova la portanavi più vicina a me");

    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(83)
    @MethodSource
    void openFleetCarrierPanel(String input) throws InterruptedException {
        assertRouted(input, DisplayFleetCarrierManagementPanelCommand.ID);
    }

    static Stream<String> openFleetCarrierPanel() {
        return Stream.of("mostra il pannello di gestione della fleet carrier", "apri il pannello di gestione della fleet carrier",
                "visualizza il pannello di gestione della fleet carrier", "mostra il pannello di gestione della portanavi",
                "apri il pannello di gestione della portanavi", "visualizza il pannello di gestione della portanavi");
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
        return Stream.of("naviga alla squadron carrier", "vai alla squadron carrier", "dirigiti verso la squadron carrier",
                "portami alla squadron carrier", "naviga alla portanavi dello squadrone", "vai alla portanavi dello squadrone",
                "dirigiti verso la portanavi dello squadrone", "dirigiti verso la portanavi della squadriglia");

    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(86)
    @MethodSource
    void calculateNeutronRoute(String input) throws InterruptedException {
        assertRouted(input, CalculateNeutronStarRouteCommand.ID);
    }

    static Stream<String> calculateNeutronRoute() {
        return Stream.of("calcola la rotta verso la stella di neutroni con efficienza 20",
            "qual è la rotta verso la stella di neutroni con efficienza 60?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(87)
    @MethodSource
    void plotNextNeutronLeg(String input) throws InterruptedException {
        assertRouted(input, PlotRouteNextNeutronStarWaypointCommand.ID);
    }

    static Stream<String> plotNextNeutronLeg() {
        return Stream.of("prossimo salto verso la stella di neutroni", "rotta verso la prossima stella di neutroni",
                "prossima stella di neutroni");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(88)
    @MethodSource
    void clearNeutronStarRoute(String input) throws InterruptedException {
        assertRouted(input, ClearNeutronRouteCommand.ID);
    }

    static Stream<String> clearNeutronStarRoute() {
        return Stream.of("annulla rotta verso la stella di neutroni", "cancella rotta verso la stella di neutroni",
                "cancella rotta verso le stelle di neutroni");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("stato della squadron carrier", "quanto a lungo possiamo usare la squadron carrier",
                "finanze della squadron carrier", "bilancio della squadron carrier", "panoramica della squadron carrier",
                "fondi della squadron carrier", "stato del carburante della squadron carrier", "trizio della squadron carrier",
                "livello di trizio della squadron carrier", "livello del carburante della squadron carrier",
                "livello del carburante della portanavi dello squadrone", "livello di trizio della portanavi dello squadrone",
                "stato della portanavi dello squadrone", "stato della portanavi della squadriglia",
                "livello di trizio della portanavi della squadriglia", "livello del carburante della portanavi della squadriglia");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of("rotta della squadron carrier", "navigazione della squadron carrier",
                "rotta di salto della squadron carrier", "quanti salti rimangono sulla rotta della squadron carrier",
                "salti rimanenti sullo squadron carrier", "rotta della portanavi dello squadrone",
                "rotta della portanavi della squadriglia");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("dove sta andando lo squadron carrier", "direzione della squadron carrier", "arrivo della squadron carrier",
                "destinazione finale della squadron carrier", "dove sta andando la portanavi dello squadrone",
                "destinazione finale della portanavi dello squadrone", "dove sta andando la portanavi della squadriglia");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierDepartureEtaQuery.ID);
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("tempo stimato di arrivo della squadron carrier", "quando arriva lo squadron carrier",
            "quanto manca allo squadron carrier", "tempo di arrivo della squadron carrier",
            "tempo di salto della squadron carrier", "tempo di arrivo della portanavi dello squadrone",
            "tempo stimato di arrivo della portanavi dello squadrone", "quando arriva la portanavi dello squadrone",
            "quanto manca alla portanavi dello squadriglia", "tempo di arrivo della portanavi della squadriglia");
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
        return Stream.of("raggiungere la fleet carrier", "andare alla fleet carrier", "vai verso la fleet carrier",
                "tornare alla fleet carrier", "portami alla fleet carrier", "dirigiti alla fleet carrier", "rotta verso la fleet carrier",
                "vai verso la portanavi", "portami alla portanavi", "dirigiti alla portanavi");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusRoutesToStatusQuery(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> bareCarrierStatusRoutesToStatusQuery() {
        return Stream.of("stato della fleet carrier", "per quanto tempo possiamo usare la fleet carrier",
                "quanto può saltare la fleet carrier", "raggio di salto della fleet carrier con trizio attuale",
                "raggio di salto della fleet carrier", "finanze della fleet carrier", "panoramica della fleet carrier",
                "stato della fleet carrier", "stato del carburante della fleet carrier", "fondi della fleet carrier",
                "bilancio della fleet carrier", "finanze della fleet carrier", "finanze della portanavi",
                "bilancio della portanavi", "stato del carburante della portanavi");
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
        return Stream.of("disattiva tutte le comunicazioni", "attiva tutte le comunicazioni",
                "disattiva tutti gli annunci vocali", "silenzia tutti gli annunci", "commuta tutti gli annunci");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SetReminderCommand.ID);
    }

    static Stream<String> setReminder() {
        return Stream.of("imposta un promemoria per il rifornimento al prossimo stop",
                "ricordami di fare rifornimento alla prossima fermata",
                "attiva promemoria per il rifornimento al prossimo stop",
                "ricordami di eseguire rifornimento alla prossima fermata");
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
        return Stream.of("mostra la mappa della galassia", "apri la mappa della galassia", "visualizza la mappa della galassia",
                "apri la mappa galattica", "mostra la mappa galattica", "visualizza la mappa galattica",
                "apri la mappa stellare", "mostra la mappa stellare", "visualizza la mappa stellare");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenSystemMapCommand.ID);
    }

    static Stream<String> systemMap() {
        return Stream.of("mostra la mappa del sistema", "apri la mappa del sistema", "mostra la mappa del sistema",
                "visualizza la mappa del sistema", "apri la mappa del sistema solare", "mostra la mappa del sistema solare",
                "visualizza la mappa del sistema solare");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, ShowNavigationPanelCommand.ID);
    }

    static Stream<String> navigationPanel() {
        return Stream.of("mostra il pannello di navigazione", "apri il pannello di navigazione", "visualizza il pannello di navigazione");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, ShowModulesPanelCommand.ID);
    }

    static Stream<String> modulesPanel() {
        return Stream.of("mostra il pannello moduli", "apri il pannello moduli", "visualizza il pannello moduli");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, ShowStatusPanelCommand.ID);
    }

    static Stream<String> statusPanel() {
        return Stream.of("mostra il pannello dello stato", "apri il pannello dello stato", "visualizza il pannello dello stato");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, ShowInventoryPanelCommand.ID);
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("mostra il pannello inventario", "apri il pannello inventario", "visualizza il pannello inventario");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, ExitCloseCommand.ID);
    }

    static Stream<String> closePanel() {
        return Stream.of("esci dal pannello", "chiudi pannello", "chiudi mappa", "chiudi mappa della galassia", "chiudi mappa del sistema");
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
        return Stream.of("Dove siamo adesso?", "qual è la nostra posizione", "dove siamo",
                "quanto dura il giorno nella posizione attuale", "quanto dura il giorno in questa posizione", "quanto dura il giorno qui");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, AnalyzeShipLoadoutQuery.ID);
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("carico della nave", "rapporto danni", "moduli della nave", "rapporto efficienza al combattimento",
                "equipaggiamento della nave", "specifiche della nave", "su che cosa sto volando", "con cosa siamo equipaggiati",
                "è equipaggiato", "generatore di scudi", "rinforzo della carena", "sensori", "propulsori", "frameshift",
                "fuel scoop", "installate");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCargoHoldQuery.ID);
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("contenuto del cargo", "cosa stiamo trasportando", "contenuto del carico");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeRouterQuery.ID);
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("rotta impostata", "riforimento alla prossima fermata", "dispinibilità rifornimento sulla rotta",
                "analisi rotta", "rotta corrente", "navigatione rotta", "salti rimanenti", "salti rimasti", "quanti salti",
                "prossima stella scoopabile", "fermata per rifornimento");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStationsQuery.ID);
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("stazioni nel sistema", "quali stazioni", "stazioni vicine",
                "ci sono stazioni o porti qui", "ci sono porti in questo sistema stellare",
                "dove si può attraccare in questo sistema", "quali stazioni sono disponibili in questo sistema",
                "dove si può atterrare in questo sistema");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarObjectsQuery.ID);
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("che oggetti stellari ci sono nel sistema", "quali pianeti nel sistema", "ci sono pianeti atterrabili",
                "si può atterrare su un pianeta o una luna", "quali corpi sono presenti nel sistema",
                "ci sono anelli di ghiaccio", "ci sono anelli planetari", "il sistema sistema ha anelli");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarSignalsQuery.ID);
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of("quali segnali ci sono nel sistema", "cosa c'è in questo sistema", "cosa è stato rilevato nel sistema",
                "quali segnali ci sono qui", "quali segnali vedi", "quali segnali rilevi", "quali segnali puoi vedere",
                "quali segnali FSS trovi", "ci sono hotspot minerari", "rilevi siti di estrazione delle risorse",
                "vedi zone di conflitto", "ci sono emissioni", "hai trobvato segnali non identificati", "ci sono segnali rilevati",
                "ci sono segnali anomali");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioScansStarSystemQuery.ID);
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("quali segnali biologici sono già stati scoperti nel sistema stellare",
                "quanti campioni biologici ci sono nel sistema stellare", "quali segnali biologici trovi nel sistema",
                "vedi segnali biologici nel sistema stellare", "quali pianeti hanno segnali biologici",
                "quali pianeti necessitano ancora di scansioni biologiche", "quali pianeti necessitano di scansioni organiche",
                "quali pianeti necessitano ancora di scansioni", "ci sono pianeti con segnali biologici non scansionati",
                "dimmi il progresso della scansione biologica", "progresso della scansione biologica");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioSamplesPlanetSurfaceQuery.ID);
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of( "quali campioni di esobiologia ci sono", "c'e materiale biologico su questo pianeta", "quali materiali organici ci sono",
            "cosa resta da scansionare", "quali sono gli organici rimanenti", "ci sono campioni rimanenti",
            "ci sono materiali organici rimanenti", "dimmi il progresso dell'esobiologia",
            "cosa rimane da scansionare", "quali scansioni biologiche sono state completate",
            "quali scansioni biologiche abbiamo completato", "scansioni biologiche completate", "ci sono organici su questo pianeta",
            "qual è il materiale biologico su questo pianeta", "quali materiali organici ci sono qui",
            "quali materiali organici ci sono su questo pianeta", "qual è il progresso dei campioni biologici sul pianeta",
            "cosa è stato scansionato qui", "quali materiali organici sono ancora da scansionare",
            "materiali organici ancora da scansionare", "organici rimanenti da scansionare",
            "organici rimasti da scansionare", "quali organici rimangono",
            "campioni biologici ancora da scansionare", "quale materiale biologico rimane", "cosa dobbiamo ancora scansionare qui");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, AnalyzePlayerProfileQuery.ID);
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("profilo del giocatore", "analisi del profilo del giocatore", "progresso del profilo del giocatore");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of("Qual è l'autonomia della nostra portanavi", "Qual è lo stato del carburante della mia portanavi?",
                "Per quanto tempo possiamo operare con i fondi attuali?", "quanto lontano possiamo saltare con l'attuale trizio?",
                "stato trizio fleet carrier", "stato carburante fleet carrier", "livello di trizio", "per quanto tempo possiamo usare la fleet carrier?",
                "quanto può saltare la fleet carrier?", "raggio di salto della fleet carrier con trizio attuale");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromFleetCarrierQuery.ID);
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("distanza dalla fleet carrier", "dov'è la nostra fleet carrier?",
                "quanto è lontana la nostra fleet carrier", "prossimità della fleet carrier",
                "quanto è lontana la fleet carrier", "dov'è la nostra portanavi?", "quanto è lontana la portanavi?",
                "prossimità della portanavi", "quanto è lontana la nostra portanavi?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFsdTargetQuery.ID);
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("mi dai info sull'obbiettivo FSD?", "analizza la destinazione",
                "a che stella stiamo puntando?", "analizza obbiettivo fsd",
                "recupera informazioni su obbiettivo fsd", "informazioni su target fsd", "analizza target fsd");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, AnalyzeExplorationProfitsQuery.ID);
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("Guadagni potenziali da esplorazione in questo sistema.",
                "Qual è il potenziale di guadagno dall'esplorazione in questo sistema?",
                "Quanto vale questa esplorazione?", "Quanto vale la scansione di questo sistema?",
                "Quanto vale la scansione biologica di questo sistema?", "Quali scansioni sono più redditizie?",
                "Quali scansioni è meglio fare in questo sistema?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TimeQuery.ID);
    }

    static Stream<String> queryTime() {
        return Stream.of("ora corrente", "che ore sono", "ora sulla Terra", "ora galattica", "ora UTC", "che ora è", "ora reale");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSystemSecurityQuery.ID);
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("il sistema è sicuro?", "chi controlla questo sistema?", "lotta per il potere", "livello di sicurezza", "chi possiede questo sistema", "qual è la fazione dominante", "potere di controllo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, StationDataQuery.ID);
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("dammi i dettagli della stazione", "quali servizi sono disponibili su questa stazione?",
            "quali servizi ci sono sulla stazione?", "servizi disponibili", "quali servizi ha questa stazione?",
            "cosa offre la stazione?", "informazioni sulla stazione", "strutture della stazione", "cosa c'è in questa stazione",
            "servizi disponibili", "cosa posso trovare su questa stazione?", "quali servizi posso trovare su questa stazione?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyseMaterialsQuery.ID);
    }

    static Stream<String> queryMaterials() {
        return Stream.of("quanto ferro abbiamo?", "quanto stagno c'è?", "quanti propulsori Guardian abbiamo?",
                "che scorta di molibdeno abbiamo?", "abbiamo del tungsteno?", "ce l'abbiamo il materiale osmio?",
                "quanto osmio ci rimane?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMaterialsOnPlanetQuery.ID);
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("Quali materiali ci sono su questo pianeta?", "Quali materiali ci sono qui?", "Quali minerali ci sono su questo pianeta?",
                "Quali depositi di materiali ci sono qui?", "Quali materiali ci sono sulla superficie del pianeta?",
                "Quali materiali ci sono in questo posto?", "Quali minerali ci sono sul pianeta?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromTheBubbleQuery.ID);
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("a che distanza siamo dalla bolla?", "distanza dal Sole?", "mi dici la distanza dalla Terra?",
                "quanto siamo lontani dal Sole?", "quanto è lontana la bolla", "quanto siamo lontani dallo spazio abitato",
                "distanza dallo spazio abitato", "vorrei sapere quanto siamo lontani dalla civiltà");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, AnalyzeLastScanQuery.ID);
    }

    static Stream<String> queryLastScan() {
        return Stream.of("qual è l'ultima scansione fatta", "mi dici l'ultima scan", "cosa abbiamo scannato di recente",
            "qual è l'ultimo oggetto scansionato", "vorrei sapere la scansione più recente",
            "scansione recente del pianeta", "cosa ho scansionato per ultimo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, RemindTargetDestinationQuery.ID);
    }

    static Stream<String> queryReminder() {
        return Stream.of("promemoria", "quali erano i promemoria?", "mi dici il promemoria destinazione",
                "c'è qualche promemoria", "cosa abbiamo impostato come promemoria");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierDepartureEtaQuery.ID);
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("qual è il tempo previsto di arrivo della portanavi", "quando arriva la fleet carrier",
            "quanto manca all'arrivo della portanavi", "mi dici l'ora di arrivo della portanavi", "quando arriva la portanavi");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeGeologyInStarSystemQuery.ID);
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("quali sono i segnali geologici?", "ci sono segnali geologici?",
                "vedi dei segnali vulcanici?", "c'è attività geologica", "ci sono attività vulcaniche",
                "com'è la geologia nel sistema");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMarketsQuery.ID);
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("quali sono i mercati locali?", "ci sono mercati presso stazioni e insediamenti",
                "mi dici i mercati presso avamposti nel sistema", "quali sono i mercati nelle stazioni e insediamenti");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBountiesCollectedQuery.ID);
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("taglie", "bounty", "quali sono le taglie totali?", "quali taglie ho riscosso?",
                "quali taglie ho cacciato?", "ho preso delle taglie?", "quanto abbiamo in taglie", "qual è il guadagno da taglie",
                "crediti da taglie", "crediti da bounty");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, BiomeAnalyzerQuery.ID);
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("analizza bioma del pianeta", "mi fai l'analisi del bioma", "quale bioma ha il pianeta A1",
                "bioma planetario", "analisi dell'atmosfera", "quali tipi di vita ci sono qui?", "che tipi di bioma ci sono?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromLastBioSampleQuery.ID);
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("distanza all'ultimo campione biologico", "quanto lontano è il campione",
                "quanto lontano è l'ultimo materiale organico", "portata al campione biologico",
                "quanto lontano è il materiale organico precedente");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> queryCarrierRoute() {
        // carrier route, carrier navigation, how many jumps on carrier route, jumps remaining on carrier, carrier route jump count, jumps left on carrier, carrier trip, carrier journey, carrier travel plan, carrier next jump
        return Stream.of("qual è la rotta della fleet carrier", "mi dici la rotta della portanavi",
                "navigazione della fleet carrier", "quanti salti rimangono sulla rotta della della portanavi",
                "quanti sono i salti rimanenti sulla fleet carrier", "vorrei sapere ilnumero di salti nella rotta della fleet carrier",
                "quanti salti rimangono sulla fleet carrier", "mostra il piano di viaggio della fleet carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> queryCarrierDestination() {
        return Stream.of("dove sta andando la fleet carrier?", "qual è la destinazione finale della fleet carrier?",
                "dove è diretta la fleet carrier?", "dove è diretta la portanavi?", "direzione fleet carrier?", "destinazione finale fleet carrier?",
                "destinazione fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SetCarrierFuelReserveCommand.ID);
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("imposta la riserva di carburante della fleet carrier a 5000",
                "imposta la riserva di carburante della fleet carrier a 10000", "riserva di carburante 15000",
                "imposta la riserva di carburante della fleet carrier a quindicimila", "riserva di trizio della fleet carrier a 5000");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DisembarkCommand.ID);
    }

    static Stream<String> disembark() {
        return Stream.of("sbarcare", "sbarca", "sbarco", "sbarco dalla nave", "voglio scendere dalla nave");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, ShowCommanderPanelCommand.ID);
    }

    static Stream<String> openCentralPanel() {
        // pannello del comandante, pannello comandante, pannello centrale, mostra il pannello del comandante, apri il pannello del comandante
        return Stream.of("pannello comandante aperto", "apri pannello comandante", "puoi aprire il pannello comandante",
                "mostra il pannello comandate", "apri il pannello del comandante", "mostra il pannello del comandante");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, ShowFighterPanelCommand.ID);
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("pannello caccia", "mostra il pannello del caccia", "apri il pannello del caccia",
                "visualizza il pannello del caccia");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FighterFireAtWillCommand.ID);
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("caccia fuoco a volontà", "apri il fuoco caccia", "attacca liberamente");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FighterAttackTargetCommand.ID);
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("caccia attacca il mio bersaglio", "caccia attacca il bersaglio", "caccia sul bersaglio", "concentrati sul mio bersaglio");
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
