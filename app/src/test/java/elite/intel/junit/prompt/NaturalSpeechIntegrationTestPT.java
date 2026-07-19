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
public class NaturalSpeechIntegrationTestPT {

    private final CompanionRoutingHarness harness = new CompanionRoutingHarness(Language.PT);

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
        return Stream.of("acorda", "acorde");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SleepCommand.ID);
    }

    static Stream<String> ignoreMe() {
        return Stream.of("me ignore", "não monitore", "dormir");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, InterruptCommand.ID);
    }

    static Stream<String> interrupt() {
        return Stream.of("interrompa", "interromper", "cala a boca");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToCombatModeCommand.ID);
    }

    static Stream<String> combatMode() {
        return Stream.of("modo de combate", "mudar para o modo de combate", "combate", "ativar modo de combate",
                "trocar para o modo de combate");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToAnalysisModeCommand.ID);
    }

    static Stream<String> analysisMode() {
        return Stream.of("modo de análise", "mudar para o modo de análise", "modo de exploração", "hud de análise",
                "ativar modo de análise", "trocar para o modo de análise");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, ResetHeadLookAheadCommand.ID);
    }

    static Stream<String> lookAhead() {
        return Stream.of("olhar para frente", "resetar", "resetar visão", "centralizar visão");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(16)
    @MethodSource
    void honkTheSystem(String input) throws InterruptedException {
        assertRouted(input, RunSystemScanCommand.ID);
    }

    static Stream<String> honkTheSystem() {
        return Stream.of("explore o sistema", "escaneie o sistema", "varrer o sistema");
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
        return Stream.of("parar motores", "parada total", "tudo parado", "desligar motores", "cortar aceleração",
                "aceleração zero", "parar a nave");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SetSpeed25Command.ID);
    }

    static Stream<String> speed25() {
        return Stream.of("um quarto da aceleração", "25 por cento", "velocidade baixa");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SetSpeed50Command.ID);
    }

    static Stream<String> speed50() {
        return Stream.of("metade da aceleração", "50 por cento", "meia velocidade");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SetSpeed75Command.ID);
    }

    static Stream<String> speed75() {
        return Stream.of("três quartos da aceleração", "75 por cento", "velocidade de três quartos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SetSpeed100Command.ID);
    }

    static Stream<String> speed100() {
        return Stream.of("aceleração total", "100 por cento", "velocidade máxima", "aceleração máxima");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, IncreaseSpeedCommand.ID);
    }

    static Stream<String> speedPlus() {
        return Stream.of("aumentar velocidade em 10", "aumentar velocidade em 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DecreaseSpeedCommand.ID);
    }

    static Stream<String> speedMinus() {
        return Stream.of("diminuir velocidade em 10", "diminuir velocidade em 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SetOptimalSpeedCommand.ID);
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("definir velocidade ótima", "velocidade ótima de aproximação");
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
        return Stream.of("salto para o hiperespaço", "salto", "vamos saltar daqui", "vamos lá",
                "saltar para o próximo ponto de rota");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, EnterSuperCruiseCommand.ID);
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("entrar em supercruise", "ativar supercruise", "supercruise", "velocidade da luz");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {
        assertRouted(input, DropFromSuperCruiseCommand.ID);
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("sair aqui", "cair aqui", "sair do supercruise");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NavigateToMissionTargetCommand.ID);
    }

    static Stream<String> navigateToMission() {
        return Stream.of("navegar até a missão ativa", "traçar rota até a missão ativa", "ir até a missão ativa",
                "navegar até a missão", "ir até a missão");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("navegar até o fleet carrier", "voltar ao carrier", "nos leve até o carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, CancelNavigationCommand.ID);
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("cancelar navegação", "abortar navegação", "parar navegação");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, NavigateToLandingZoneCommand.ID);
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("navegar até a zona de pouso", "rumo à zona de pouso", "me leve de volta à zona de pouso");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TargetDestinationCommand.ID);
    }

    static Stream<String> targetDestination() {
        return Stream.of("mirar destino", "selecionar destino");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(38)
    @MethodSource
    void clearActiveMissions(String input) throws InterruptedException {
        assertRouted(input, ClearActiveMissionsCommand.ID);
    }

    static Stream<String> clearActiveMissions() {
        return Stream.of("limpar missões ativas", "limpar todas as missões ativas", "apagar missões ativas");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NavigateToTradeStopCommand.ID);
    }

    static Stream<String> nextTradeStop() {
        return Stream.of("navegar até a próxima parada comercial", "ir até a próxima parada comercial");
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
        return Stream.of("trem de pouso", "baixar trem de pouso", "abaixar o trem de pouso", "estender trem de pouso");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RetractLandingGearCommand.ID);
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("recolher trem de pouso", "trem de pouso para cima", "subir trem de pouso", "guardar trem de pouso");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, RequestDockingCommand.ID);
    }

    static Stream<String> requestDocking() {
        return Stream.of("solicitar atracagem", "atracar na estação", "pedido de atracagem", "solicitar pouso",
                "contate a torre e nos consiga uma plataforma", "solicitar permissão de pouso", "solicitar plataforma de pouso",
                "solicitar autorização de pouso");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, ToggleCargoScoopCommand.ID);
    }

    static Stream<String> cargoScoop() {
        return Stream.of("abrir coletor de carga", "estender coletor de carga", "abrir compartimento de carga",
                "abrir a porta do compartimento de carga");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, ToggleNightVisionOnOffCommand.ID);
    }

    static Stream<String> nightVision() {
        return Stream.of("visão noturna", "ligar visão noturna", "desligar visão noturna");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, ToggleLightsOnOffCommand.ID);
    }

    static Stream<String> lights() {
        return Stream.of("faróis", "luzes ligadas", "desligar luzes", "luzes", "ligar as luzes");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DismissShipToOrbitCommand.ID);
    }

    static Stream<String> dismissShip() {
        return Stream.of("dispensar nave", "mandar a nave embora", "nave para a órbita");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TaxiToLandingPadCommand.ID);
    }

    static Stream<String> taxi() {
        return Stream.of("taxiar para o pouso", "atracagem automática", "piloto automático de pouso", "taxiar",
                "taxiar automaticamente");
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
        return Stream.of("preparar armamento", "armas livres", "prontos para o combate", "armar", "armas prontas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RetractHardpointsCommand.ID);
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("recolher armamento", "armas frias", "baixar armas", "guardar hardpoints", "guardar armas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DeployHeatSinkCommand.ID);
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("lançar dissipador de calor", "dissipador de calor", "dissipar calor");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, TargetHostileHighestThreatCommand.ID);
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("alvo prioritário", "mirar maior ameaça", "próximo inimigo", "selecionar inimigo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DeployShieldCellCommand.ID);
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("usar célula de escudo", "ativar célula de escudo", "banco de células de escudo",
                "usar energia de escudo", "disparar célula de escudo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DeployChaffCommand.ID);
    }

    static Stream<String> deployChaff() {
        return Stream.of("lançar chaff", "usar chaff", "disparar chaff", "soltar contramedidas", "lançar contramedidas", "chaff");
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
        return Stream.of("energia para os escudos", "máximo de escudos", "reforçar escudos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToEnginesCommand.ID);
    }

    static Stream<String> powerToEngines() {
        return Stream.of("energia para os motores", "máximo de motores", "reforçar motores");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToWeaponsCommand.ID);
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("energia para as armas", "máximo de armas", "reforçar armas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, EqualizePowerCommand.ID);
    }

    static Stream<String> resetPower() {
        return Stream.of("equalizar energia", "balancear energia", "redefinir energia", "distribuir energia igualmente");
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
        return Stream.of("Abrir FSS e escanear.", "Fazer varredura espectral filtrada", "varredura espectral completa",
                "abrir fss");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NavigateToBioSampleCodexEntryCommand.ID);
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("Navegar até a próxima amostra biológica", "Navegar até o próximo orgânico",
                "navegar até a entrada do códex");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FindMiningSiteCommand.ID);
    }

    static Stream<String> findMiningSite() {
        return Stream.of("encontrar local de mineração de alexandrita em até 300 anos-luz",
                "encontrar local de mineração de bromelita em 1200 anos-luz", "encontrar campo de asteroides com ouro");
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
        return Stream.of("inserir destino do carrier", "definir destino do carrier", "inserir próximo destino do carrier");
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
        return Stream.of("encontrar o fleet carrier mais próximo", "carrier mais próximo");
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
        return Stream.of("navegar até o carrier do esquadrão", "ir até o carrier do esquadrão",
                "seguir até o carrier do esquadrão");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(86)
    @MethodSource
    void calculateNeutronRoute(String input) throws InterruptedException {
        assertRouted(input, CalculateNeutronStarRouteCommand.ID);
    }

    static Stream<String> calculateNeutronRoute() {
        return Stream.of("calcular rota de nêutrons com eficiência 20", "calcular rota de nêutrons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(87)
    @MethodSource
    void plotNextNeutronLeg(String input) throws InterruptedException {
        assertRouted(input, PlotRouteNextNeutronStarWaypointCommand.ID);
    }

    static Stream<String> plotNextNeutronLeg() {
        return Stream.of("próximo salto de estrela de nêutrons", "traçar rota até o próximo ponto de estrela de nêutrons",
                "próxima estrela de nêutrons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(88)
    @MethodSource
    void clearNeutronStarRoute(String input) throws InterruptedException {
        assertRouted(input, ClearNeutronRouteCommand.ID);
    }

    static Stream<String> clearNeutronStarRoute() {
        return Stream.of("limpar rota de nêutrons");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("status do carrier do esquadrão", "finanças do carrier do esquadrão", "saldo do carrier do esquadrão",
                "quanto tempo podemos operar o carrier do esquadrão", "trítio do carrier do esquadrão",
                "combustível do carrier do esquadrão", "nível de combustível do carrier do esquadrão");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of("rota do carrier do esquadrão", "quantos saltos na rota do carrier do esquadrão",
                "rota do carrier do esquadrão");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("para onde o carrier do esquadrão está indo", "destino final do carrier do esquadrão",
                "rumo do carrier do esquadrão");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierDepartureEtaQuery.ID);
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("ETA do carrier do esquadrão", "quando o carrier do esquadrão chega",
                "quanto falta para o carrier do esquadrão chegar");
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
        return Stream.of("navegar até o fleet carrier", "voltar ao carrier", "nos leve até o carrier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusRoutesToStatusQuery(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> bareCarrierStatusRoutesToStatusQuery() {
        return Stream.of("status do fleet carrier", "saldo do fleet carrier", "finanças do fleet carrier");
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
        return Stream.of("desativar todos os anúncios");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SetReminderCommand.ID);
    }

    static Stream<String> setReminder() {
        return Stream.of("definir lembrete reabastecer na próxima parada");
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
        return Stream.of("abrir mapa da galáxia", "mostrar mapa da galáxia", "exibir mapa da galáxia");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenSystemMapCommand.ID);
    }

    static Stream<String> systemMap() {
        return Stream.of("abrir mapa local", "mostrar mapa do sistema", "exibir mapa do sistema");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, ShowNavigationPanelCommand.ID);
    }

    static Stream<String> navigationPanel() {
        return Stream.of("mostrar painel de navegação", "abrir painel de navegação");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, ShowModulesPanelCommand.ID);
    }

    static Stream<String> modulesPanel() {
        return Stream.of("mostrar painel de módulos", "abrir painel de módulos", "exibir painel de módulos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, ShowStatusPanelCommand.ID);
    }

    static Stream<String> statusPanel() {
        return Stream.of("mostrar painel de status", "abrir painel de status");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, ShowInventoryPanelCommand.ID);
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("mostrar painel de inventário", "abrir painel de inventário");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, ExitCloseCommand.ID);
    }

    static Stream<String> closePanel() {
        return Stream.of("fechar painel", "fechar o painel");
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
        return Stream.of("Onde estamos agora?", "qual é a nossa localização", "onde estamos",
                "quanto tempo dura o dia na localização atual");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, AnalyzeShipLoadoutQuery.ID);
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("equipamento da nave", "o que estou pilotando", "equipamentos da nave",
                "você tem coletor de combustível instalado", "você tem armas instaladas",
                "quais armas você tem instaladas", "você tem uma refinaria instalada");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCargoHoldQuery.ID);
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("porão de carga", "o que estamos carregando", "conteúdo da carga");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeRouterQuery.ID);
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("rota traçada", "saltos restantes", "quantos saltos até o destino", "já chegamos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStationsQuery.ID);
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("estações no sistema", "quais estações", "estações próximas",
                "há alguma estação ou porto aqui", "algum porto neste sistema estelar");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarObjectsQuery.ID);
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("Quais planetas ou luas pousáveis há neste sistema?",
                "Há anéis de gelo neste sistema estelar");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarSignalsQuery.ID);
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of("Quais sinais há neste sistema?", "Quais sinais você vê?", "Algum sinal interessante?",
                "Sinais do sistema?", "O que há neste sistema?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioScansStarSystemQuery.ID);
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("Quais planetas ainda precisam de escaneamento biológico ou orgânico?", "Quais escaneamentos biológicos concluímos?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioSamplesPlanetSurfaceQuery.ID);
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of("Quais orgânicos ainda temos que escanear?",
                "Quais orgânicos ou biologia há neste planeta");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, AnalyzePlayerProfileQuery.ID);
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("perfil do jogador", "perfil do jogador resumir patentes", "perfil do jogador resumir progresso");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierStatusQuery.ID);
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of("Qual é o alcance do nosso fleet carrier?", "Qual é o status de combustível do meu fleet carrier",
                "Quanto tempo podemos operar com os fundos atuais?", "Quão longe o carrier pode saltar com o trítio atual?",
                "trítio do carrier", "combustível do carrier", "nível de trítio");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromFleetCarrierQuery.ID);
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("Quão longe estamos do carrier?", "Distância do fleet carrier?",
                "Quão longe está o fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFsdTargetQuery.ID);
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("alvo fsd", "qual estrela estamos mirando", "informação sobre o próximo salto");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, AnalyzeExplorationProfitsQuery.ID);
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("Potencial de lucro de exploração neste sistema.",
                "Qual é o potencial de lucro de exploração neste sistema?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TimeQuery.ID);
    }

    static Stream<String> queryTime() {
        return Stream.of("hora atual", "que horas são", "hora utc");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSystemSecurityQuery.ID);
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("segurança do sistema", "quem controla este sistema", "facção dominante");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, StationDataQuery.ID);
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("detalhes da estação", "quais serviços de estação há nesta estação", "quais serviços aqui",
                "informação da estação");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyseMaterialsQuery.ID);
    }

    static Stream<String> queryMaterials() {
        return Stream.of("inventário de materiais ferro", "quanto ferro temos", "quanto vanádio temos");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMaterialsOnPlanetQuery.ID);
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("Quais materiais estão disponíveis neste planeta?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromTheBubbleQuery.ID);
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("Quão longe estamos da Bolha?", "Distância até a Terra", "Quão longe está a Terra",
                "quão longe da civilização", "quão longe da Terra");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, AnalyzeLastScanQuery.ID);
    }

    static Stream<String> queryLastScan() {
        return Stream.of("Analisar o escaneamento mais recente?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, RemindTargetDestinationQuery.ID);
    }

    static Stream<String> queryReminder() {
        return Stream.of("lembrete", "qual era o lembrete", "algum lembrete");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierDepartureEtaQuery.ID);
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("Qual é o ETA do salto do nosso fleet carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeGeologyInStarSystemQuery.ID);
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("sinais geo", "sinais geológicos", "atividade vulcânica");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMarketsQuery.ID);
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("mercados locais", "mercados em estações e assentamentos", "mercados em postos avançados no sistema");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBountiesCollectedQuery.ID);
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("recompensas", "total de recompensas", "quanto em recompensas");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, BiomeAnalyzerQuery.ID);
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("Analisar o bioma deste sistema estelar", "Análise do bioma para o planeta a 1");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromLastBioSampleQuery.ID);
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("Localização e distância da última amostra biológica.",
                "Quão longe estamos da última amostra biológica?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of("O que há na rota do carrier?", "Qual é a rota do nosso fleet carrier?",
                "Quantos saltos na rota do carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCarrierVoyageQuery.ID);
    }

    static Stream<String> queryCarrierDestination() {
        return Stream.of("Para onde o fleet carrier está indo?", "Qual é o destino final do carrier?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SetCarrierFuelReserveCommand.ID);
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("Definir nível de reserva de combustível para 5000", "Definir reserva de combustível para 10000",
                "Reserva de combustível 15000", "Definir reserva de combustível para quinze mil");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DisembarkCommand.ID);
    }

    static Stream<String> disembark() {
        return Stream.of("desembarcar");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, ShowCentralPanelCommand.ID);
    }

    static Stream<String> openCentralPanel() {
        return Stream.of("Abrir painel do comandante", "abrir painel central", "abrir painel de função",
                "abrir painel de bordo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, ShowFighterPanelCommand.ID);
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("mostrar painel do caça", "abrir painel do caça");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FighterFireAtWillCommand.ID);
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("caça fogo à vontade", "fogo à vontade");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FighterAttackTargetCommand.ID);
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("caça atacar meu alvo", "atacar", "focar meu alvo");
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
