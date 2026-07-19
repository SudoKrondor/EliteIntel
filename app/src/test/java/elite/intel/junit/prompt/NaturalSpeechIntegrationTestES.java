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
 * Spanish routing suite - the ES counterpart of {@link NaturalSpeechIntegrationTestPT}. Extrapolated from the
 * other NaturalSpeech suites: each parameterized case asserts a native Spanish phrase routes to the expected
 * action through the real companion path (reflex gate -> semantic reducer -> companion LLM). Phrases are drawn
 * from {@code ai_action_aliases_es.properties} and {@code SpanishPromptRules} so the deterministic layer can
 * resolve the unambiguous ones and the disambiguation carries the rest.
 * <p>
 * REQUIREMENTS: local LLM installed and configured; app started once with the game running for basic data.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NaturalSpeechIntegrationTestES {

    private final CompanionRoutingHarness harness = new CompanionRoutingHarness(Language.ES);

    @BeforeAll
    void bootstrap() throws Exception {
        harness.boot();
    }

    @AfterAll
    void teardown() {
        harness.shutdown();
    }

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
        return Stream.of("despierta", "despiértate");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SleepCommand.ID);
    }

    static Stream<String> ignoreMe() {
        return Stream.of("ignórame", "no me vigiles", "duerme");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, InterruptCommand.ID);
    }

    static Stream<String> interrupt() {
        return Stream.of("interrumpe", "cállate", "silencio");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToCombatModeCommand.ID);
    }

    static Stream<String> combatMode() {
        return Stream.of("modo combate", "cambiar a modo combate", "combate", "activar modo combate");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToAnalysisModeCommand.ID);
    }

    static Stream<String> analysisMode() {
        return Stream.of("modo análisis", "cambiar a modo análisis", "modo de exploración", "activar modo análisis");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, ResetHeadLookAheadCommand.ID);
    }

    static Stream<String> lookAhead() {
        return Stream.of("restablecer vista de cabeza", "mirar al frente", "centrar vista");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(16)
    @MethodSource
    void honkTheSystem(String input) throws InterruptedException {
        assertRouted(input, RunSystemScanCommand.ID);
    }

    static Stream<String> honkTheSystem() {
        return Stream.of("explora el sistema", "escanea el sistema", "sondea el sistema");
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
        return Stream.of("detener motores", "alto total", "detener todo", "apagar motores", "cortar acelerador",
                "acelerador a cero", "detener nave");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SetSpeed25Command.ID);
    }

    static Stream<String> speed25() {
        return Stream.of("un cuarto de acelerador", "25 por ciento", "velocidad lenta");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SetSpeed50Command.ID);
    }

    static Stream<String> speed50() {
        return Stream.of("medio acelerador", "50 por ciento", "media velocidad");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SetSpeed75Command.ID);
    }

    static Stream<String> speed75() {
        return Stream.of("tres cuartos de acelerador", "75 por ciento", "velocidad a tres cuartos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SetSpeed100Command.ID);
    }

    static Stream<String> speed100() {
        return Stream.of("acelerador al máximo", "100 por ciento", "velocidad máxima", "acelerador máximo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, IncreaseSpeedCommand.ID);
    }

    static Stream<String> speedPlus() {
        return Stream.of("aumenta velocidad en 10", "aumenta velocidad en 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DecreaseSpeedCommand.ID);
    }

    static Stream<String> speedMinus() {
        return Stream.of("reduce velocidad en 10", "reduce velocidad en 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SetOptimalSpeedCommand.ID);
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("velocidad óptima", "velocidad óptima de aproximación");
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
        return Stream.of("salto al hiperespacio", "salta", "vamos a saltar de aquí", "vamos",
                "saltar al siguiente punto de ruta");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, EnterSuperCruiseCommand.ID);
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("entrar en supercruise", "activar supercruise", "supercruise");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {
        assertRouted(input, DropFromSuperCruiseCommand.ID);
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("salir aquí", "caer aquí", "salir de supercruise");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NavigateToMissionTargetCommand.ID);
    }

    static Stream<String> navigateToMission() {
        return Stream.of("navega a la misión activa", "traza ruta a la misión activa", "ve a la misión activa",
                "navega a la misión", "ve a la misión");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("navega al fleet carrier", "regresa al carrier", "llévanos al carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, CancelNavigationCommand.ID);
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("cancelar navegación", "abortar navegación", "detener navegación");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, NavigateToLandingZoneCommand.ID);
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("navega a la zona de aterrizaje", "rumbo a la zona de aterrizaje", "volver a la lz");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TargetDestinationCommand.ID);
    }

    static Stream<String> targetDestination() {
        return Stream.of("fijar destino", "seleccionar destino");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(38)
    @MethodSource
    void clearActiveMissions(String input) throws InterruptedException {
        assertRouted(input, ClearActiveMissionsCommand.ID);
    }

    static Stream<String> clearActiveMissions() {
        return Stream.of("borrar misiones activas", "borrar todas las misiones activas", "eliminar misiones activas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NavigateToTradeStopCommand.ID);
    }

    static Stream<String> nextTradeStop() {
        return Stream.of("navega a la siguiente parada comercial", "ve a la siguiente parada comercial");
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
        return Stream.of("tren de aterrizaje", "bajar tren de aterrizaje", "desplegar tren de aterrizaje");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RetractLandingGearCommand.ID);
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("retraer tren de aterrizaje", "subir tren de aterrizaje", "guardar tren de aterrizaje");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, RequestDockingCommand.ID);
    }

    static Stream<String> requestDocking() {
        return Stream.of("solicitar atraque", "atracar en estación", "petición de atraque", "solicitar aterrizaje",
                "contacta la torre y consíguenos una plataforma", "solicitar plataforma");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, ToggleCargoScoopCommand.ID);
    }

    static Stream<String> cargoScoop() {
        return Stream.of("abrir cargo scoop", "desplegar cargo scoop", "abrir bodega de carga", "cerrar bodega de carga");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, ToggleNightVisionOnOffCommand.ID);
    }

    static Stream<String> nightVision() {
        return Stream.of("visión nocturna", "activar visión nocturna", "desactivar visión nocturna");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, ToggleLightsOnOffCommand.ID);
    }

    static Stream<String> lights() {
        return Stream.of("faros", "luces encendidas", "apagar luces", "luces", "encender las luces");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DismissShipToOrbitCommand.ID);
    }

    static Stream<String> dismissShip() {
        return Stream.of("despedir nave", "enviar nave lejos", "nave a órbita");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TaxiToLandingPadCommand.ID);
    }

    static Stream<String> taxi() {
        return Stream.of("taxi al punto de aterrizaje", "aterrizaje automático", "aterrizaje con piloto automático", "taxi");
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
        return Stream.of("desplegar anclajes", "armas libres", "listos para combate", "armar", "armas listas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RetractHardpointsCommand.ID);
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("retraer anclajes", "armas frías", "bajar armas", "guardar hardpoints", "guardar armas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DeployHeatSinkCommand.ID);
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("lanzar disipador térmico", "disipador térmico", "descargar calor");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, TargetHostileHighestThreatCommand.ID);
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("objetivo prioritario", "apuntar a la mayor amenaza", "siguiente enemigo", "seleccionar enemigo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DeployShieldCellCommand.ID);
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("usar célula de escudo", "activar célula de escudo", "banco de células de escudo",
                "usar energía de escudo", "disparar célula de escudo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DeployChaffCommand.ID);
    }

    static Stream<String> deployChaff() {
        return Stream.of("lanzar chaff", "usar chaff", "disparar chaff", "soltar contramedidas", "lanzar contramedidas", "chaff");
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
        return Stream.of("energía a escudos", "máximo escudos", "reforzar escudos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToEnginesCommand.ID);
    }

    static Stream<String> powerToEngines() {
        return Stream.of("energía a motores", "máximo motores", "reforzar motores");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToWeaponsCommand.ID);
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("energía a armas", "máximo armas", "reforzar armas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, EqualizePowerCommand.ID);
    }

    static Stream<String> resetPower() {
        return Stream.of("igualar energía", "balancear energía", "restablecer energía", "distribuir energía por igual");
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
        return Stream.of("Abrir FSS y escanear.", "escaneo de espectro completo", "escaneo completo", "abrir fss");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NavigateToBioSampleCodexEntryCommand.ID);
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("Navega a la próxima muestra biológica", "Navega al siguiente orgánico",
                "navega a la entrada del codex");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FindMiningSiteCommand.ID);
    }

    static Stream<String> findMiningSite() {
        return Stream.of("buscar ubicación de minería de alexandrita en hasta 300 años luz",
                "buscar ubicación de minería de bromelita en 1200 años luz", "buscar campo de asteroides con oro");
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
        return Stream.of("ingresar destino del carrier", "establecer destino del carrier", "ingresar próximo destino del carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(82)
    @MethodSource
    void findNearestCarrier(String input) throws InterruptedException {
        assertRouted(input, FindNearestFleetCarrierCommand.ID);
    }

    static Stream<String> findNearestCarrier() {
        return Stream.of("buscar el fleet carrier más cercano", "carrier más cercano");
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
        return Stream.of("navega al carrier del escuadrón", "ve al carrier del escuadrón", "sigue al carrier del escuadrón");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(86)
    @MethodSource
    void calculateNeutronRoute(String input) throws InterruptedException {
        assertRouted(input, CalculateNeutronStarRouteCommand.ID);
    }

    static Stream<String> calculateNeutronRoute() {
        return Stream.of("calcular ruta de estrella de neutrones con eficiencia 20", "calcular ruta de estrella de neutrones");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(87)
    @MethodSource
    void plotNextNeutronLeg(String input) throws InterruptedException {
        assertRouted(input, PlotRouteNextNeutronStarWaypointCommand.ID);
    }

    static Stream<String> plotNextNeutronLeg() {
        return Stream.of("próximo salto de estrella de neutrones", "traza la ruta al siguiente punto de estrella de neutrones",
                "siguiente estrella de neutrones");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(88)
    @MethodSource
    void clearNeutronStarRoute(String input) throws InterruptedException {
        assertRouted(input, ClearNeutronRouteCommand.ID);
    }

    static Stream<String> clearNeutronStarRoute() {
        return Stream.of("borrar ruta de estrella de neutrones");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("estado del carrier del escuadrón", "finanzas del carrier del escuadrón", "saldo del carrier del escuadrón",
                "cuánto tiempo podemos operar el carrier del escuadrón", "tritio del carrier del escuadrón",
                "combustible del carrier del escuadrón", "nivel de combustible del carrier del escuadrón");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of("ruta del carrier del escuadrón", "cuántos saltos en la ruta del carrier del escuadrón",
                "saltos restantes del carrier del escuadrón");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("a dónde va el carrier del escuadrón", "destino final del carrier del escuadrón",
                "rumbo del carrier del escuadrón");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierDepartureEtaQuery.ID);
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("eta del carrier del escuadrón", "cuándo llega el carrier del escuadrón",
                "cuánto falta para que llegue el carrier del escuadrón");
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
        return Stream.of("navega al fleet carrier", "regresa al carrier", "llévanos al carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusRoutesToStatusQuery(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> bareCarrierStatusRoutesToStatusQuery() {
        return Stream.of("estado del fleet carrier", "saldo del fleet carrier", "finanzas del fleet carrier");
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
        return Stream.of("desactivar todos los anuncios");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SetReminderCommand.ID);
    }

    static Stream<String> setReminder() {
        return Stream.of("establecer recordatorio repostar en la próxima parada");
    }

    // =========================================================================
    // UI panels - representative sample
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(100)
    @MethodSource
    void galaxyMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenGalaxyMapCommand.ID);
    }

    static Stream<String> galaxyMap() {
        return Stream.of("abrir mapa galáctico", "mostrar mapa galáctico", "visualizar mapa galáctico");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenSystemMapCommand.ID);
    }

    static Stream<String> systemMap() {
        return Stream.of("abrir mapa local", "mostrar mapa del sistema", "visualizar mapa del sistema");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, ShowNavigationPanelCommand.ID);
    }

    static Stream<String> navigationPanel() {
        return Stream.of("mostrar panel de navegación", "abrir panel de navegación");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, ShowModulesPanelCommand.ID);
    }

    static Stream<String> modulesPanel() {
        return Stream.of("mostrar panel de módulos", "abrir panel de módulos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, ShowStatusPanelCommand.ID);
    }

    static Stream<String> statusPanel() {
        return Stream.of("mostrar panel de estado", "abrir panel de estado");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, ShowInventoryPanelCommand.ID);
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("mostrar panel de inventario", "abrir panel de inventario");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, ExitCloseCommand.ID);
    }

    static Stream<String> closePanel() {
        return Stream.of("cerrar panel", "cierra el panel");
    }

    // =========================================================================
    // Queries
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(200)
    @MethodSource
    void queryCurrentLocation(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCurrentLocationQuery.ID);
    }

    static Stream<String> queryCurrentLocation() {
        return Stream.of("¿Dónde estamos ahora?", "cuál es nuestra ubicación", "dónde estamos",
                "cuánto dura el día en la ubicación actual");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, AnalyzeShipLoadoutQuery.ID);
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("equipamiento de la nave", "qué estoy pilotando", "con qué estamos equipados",
                "¿Tienes instalada una pala de combustible?", "tienes armas instaladas", "¿Tiene instalada una refinería?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCargoHoldQuery.ID);
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("qué hay en nuestra bodega", "qué estamos llevando", "contenido de carga");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeRouterQuery.ID);
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("ruta trazada", "saltos restantes", "cuántos saltos hasta el destino", "ya llegamos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStationsQuery.ID);
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("estaciones en el sistema", "qué estaciones", "estaciones cercanas",
                "hay alguna estación o puerto aquí", "algún puerto en este sistema estelar");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarObjectsQuery.ID);
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("¿Qué planetas o lunas aterrizables hay en este sistema?",
                "¿Hay algún anillo en este sistema de accionamiento?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarSignalsQuery.ID);
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of("¿Qué señales hay en este sistema?", "¿Qué señales ves?", "¿Alguna señal interesante?",
                "¿Señales del sistema?", "¿Qué hay en este sistema?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioScansStarSystemQuery.ID);
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("¿Qué planetas aún necesitan escaneo biológico u orgánico?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioSamplesPlanetSurfaceQuery.ID);
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of("¿Qué escaneos biológicos completamos?", "¿Qué orgánicos aún tenemos que escanear?",
                "Qué orgánicos o biología hay en este planeta");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, AnalyzePlayerProfileQuery.ID);
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("perfil del jugador", "perfil del jugador resumir rangos", "perfil del jugador resumir progreso");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of("¿Cuál es el alcance de nuestro fleet carrier?", "¿Cuál es el estado de combustible de mi fleet carrier?",
                "¿Cuánto tiempo podemos operar con los fondos actuales?", "¿Qué tan lejos puede saltar el carrier con el tritio actual?",
                "tritio del carrier", "combustible del carrier", "nivel de tritio");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromFleetCarrierQuery.ID);
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("¿Qué tan lejos estamos del carrier?", "¿Distancia del fleet carrier?",
                "¿Qué tan lejos está el fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFsdTargetQuery.ID);
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("objetivo fsd", "a qué estrella apuntamos", "información sobre el próximo salto");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, AnalyzeExplorationProfitsQuery.ID);
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("Potencial de ganancia de exploración en este sistema.",
                "¿Cuál es el potencial de ganancia de exploración en este sistema?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TimeQuery.ID);
    }

    static Stream<String> queryTime() {
        return Stream.of("hora actual", "qué hora es", "hora utc");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSystemSecurityQuery.ID);
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("seguridad del sistema", "quién controla este sistema", "facción dominante");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, StationDataQuery.ID);
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("detalles de la estación", "qué servicios tiene esta estación", "qué servicios hay aquí",
                "información de la estación");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyseMaterialsQuery.ID);
    }

    static Stream<String> queryMaterials() {
        return Stream.of("inventario de materiales hierro", "cuánto hierro tenemos", "cuánto vanadio tenemos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMaterialsOnPlanetQuery.ID);
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("¿Qué materiales hay disponibles en este planeta?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromTheBubbleQuery.ID);
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("¿Qué tan lejos estamos de la Burbuja?", "Distancia a la Tierra", "Qué tan lejos está la Tierra",
                "qué tan lejos de la civilización", "¿Qué tan lejos estamos de la Tierra?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, AnalyzeLastScanQuery.ID);
    }

    static Stream<String> queryLastScan() {
        return Stream.of("¿Analizar el escaneo más reciente?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, RemindTargetDestinationQuery.ID);
    }

    static Stream<String> queryReminder() {
        return Stream.of("recordatorio", "cuál era el recordatorio", "hay recordatorios");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierDepartureEtaQuery.ID);
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("¿Cuál es el ETA del salto de nuestro fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeGeologyInStarSystemQuery.ID);
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("señales geológicas", "actividad volcánica", "geología en el sistema");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMarketsQuery.ID);
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("mercados locales", "mercados en estaciones y asentamientos", "mercados en puestos avanzados del sistema");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBountiesCollectedQuery.ID);
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("recompensas", "recompensas totales", "cuánto en recompensas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, BiomeAnalyzerQuery.ID);
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("Analizar el bioma de este sistema estelar", "Análisis de bioma para el planeta a 1");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromLastBioSampleQuery.ID);
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("Ubicación y distancia de la última muestra biológica.",
                "¿Qué tan lejos estamos de la última muestra biológica?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of("¿Qué hay en la ruta del carrier?", "¿Cuál es la ruta de nuestro fleet carrier?",
                "¿Cuántos saltos en la ruta del carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void queryCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> queryCarrierDestination() {
        return Stream.of("¿A dónde va el fleet carrier?", "¿Cuál es el destino final del carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SetCarrierFuelReserveCommand.ID);
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("Establecer nivel de reserva de combustible en 5000", "Establecer reserva de combustible en 10000",
                "Reserva de combustible 15000", "Establecer reserva de combustible en quince mil");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DisembarkCommand.ID);
    }

    static Stream<String> disembark() {
        return Stream.of("desembarcar");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, ShowCentralPanelCommand.ID);
    }

    static Stream<String> openCentralPanel() {
        return Stream.of("Abrir panel del comandante", "abrir panel central", "abrir panel de rol", "abrir kneeboard");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(238)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, ShowFighterPanelCommand.ID);
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("mostrar panel del caza", "abrir panel del caza");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(239)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FighterFireAtWillCommand.ID);
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("caza fuego a discreción", "fuego a voluntad");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(241)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FighterAttackTargetCommand.ID);
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("caza ataca mi objetivo", "atacar", "enfocar mi objetivo");
    }
}
