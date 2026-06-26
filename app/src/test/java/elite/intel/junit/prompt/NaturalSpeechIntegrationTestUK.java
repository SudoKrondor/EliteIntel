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
public class NaturalSpeechIntegrationTestUK {


    private static final int LLM_WAIT_MS = 3000;
    private static final int LLM_POLL_MS = 100;

    private HandlerCapture capture;

    @BeforeAll
    void bootstrap() throws InterruptedException {
        SystemSession systemSession = SystemSession.getInstance();
        systemSession.setConversationalMode(false);
        systemSession.setLanguage(Language.UK);
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
        return Stream.of("прокинься", "прокидайся");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SleepCommand.ID);
    }

    static Stream<String> ignoreMe() {
        return Stream.of("ігноруй мене", "не слухай", "спи");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, InterruptCommand.ID);
    }

    static Stream<String> interrupt() {
        return Stream.of("перебий", "перерви", "зупини мову", "припини говорити", "перервати", "відставити", "замовкни");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToCombatModeCommand.ID);
    }

    static Stream<String> combatMode() {
        return Stream.of("бойовий режим", "перемкнись у бойовий режим", "увімкни бойовий режим", "бойовий");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToAnalysisModeCommand.ID);
    }

    static Stream<String> analysisMode() {
        return Stream.of("режим аналізу", "перемкнись у режим аналізу", "режим дослідника",
                "HUD аналізу", "увімкни режим аналізу");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, ResetHeadLookAheadCommand.ID);
    }

    static Stream<String> lookAhead() {
        return Stream.of("дивись вперед", "скинь погляд", "дивитись вперед", "огляд по центру");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(16)
    @MethodSource
    void honkTheSystem(String input) throws InterruptedException {
        assertRouted(input, HonkCommand.ID);
    }

    static Stream<String> honkTheSystem() {
        return Stream.of("досліджуй систему", "скануй систему", "відскануй систему", "обстеж систему");
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
        return Stream.of("стоп двигуни", "зупинись", "повний стоп",
                "заглуши двигуни", "скинь тягу", "нульова тяга", "зупини корабель");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SetSpeed25Command.ID);
    }

    static Stream<String> speed25() {
        return Stream.of("чверть тяги", "25 відсотків", "малий хід");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SetSpeed50Command.ID);
    }

    static Stream<String> speed50() {
        return Stream.of("половина тяги", "50 відсотків", "пів швидкості");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SetSpeed75Command.ID);
    }

    static Stream<String> speed75() {
        return Stream.of("три чверті тяги", "75 відсотків", "три чверті швидкості");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SetSpeed100Command.ID);
    }

    static Stream<String> speed100() {
        return Stream.of("повна тяга", "100 відсотків", "повна швидкість", "максимальна тяга");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, IncreaseSpeedCommand.ID);
    }

    static Stream<String> speedPlus() {
        return Stream.of("збільш швидкість на 10", "збільш швидкість на 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DecreaseSpeedCommand.ID);
    }

    static Stream<String> speedMinus() {
        return Stream.of("зменш швидкість на 10", "зменш швидкість на 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SetOptimalSpeedCommand.ID);
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("встанови оптимальну швидкість", "оптимальна швидкість підходу");
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
        return Stream.of("стрибок у гіперпростір", "стрибок", "відходимо",
                "поїхали", "стрибок до наступної точки маршруту");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, EnterSuperCruiseCommand.ID);
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("увійти в суперкруїз", "увімкнути суперкруїз", "суперкруїз", "світлова швидкість", "активувати суперкруїз", "на форсаж");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {
        assertRouted(input, DropFromSuperCruiseCommand.ID);
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("виходь тут", "вихід", "вийти із суперкруїзу", "дроп");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NavigateToMissionTargetCommand.ID);
    }

    static Stream<String> navigateToMission() {
        return Stream.of("лети до активної місії", "проклади маршрут до активної місії",
                "до активної місії", "лети до місії", "навігація до місії");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("лети до флотського авіаносця", "повернутись до авіаносця", "веди нас до авіаносця");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, CancelNavigationCommand.ID);
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("скасуй навігацію", "перерви навігацію", "вимкни навігацію");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, NavigateToLandingZoneCommand.ID);
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("лети до зони посадки", "курс до зони посадки", "веди мене назад до зони посадки");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TargetDestinationCommand.ID);
    }

    static Stream<String> targetDestination() {
        return Stream.of("обрати ціль призначення", "обрати пункт призначення");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(38)
    @MethodSource
    void clearActiveMissions(String input) throws InterruptedException {
        assertRouted(input, ClearActiveMissionsCommand.ID);
    }

    static Stream<String> clearActiveMissions() {
        return Stream.of("видали всі завдання", "видалити всі завдання");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NavigateToTradeStopCommand.ID);
    }

    static Stream<String> nextTradeStop() {
        return Stream.of("лети до наступної торгової зупинки", "до наступної торгової зупинки");
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
        return Stream.of("шасі", "випустити шасі", "опустити шасі", "розгорнути шасі", "готуйся до посадки");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RetractLandingGearCommand.ID);
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("прибрати шасі", "шасі вгору", "підняти шасі", "скласти шасі");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, RequestDockingCommand.ID);
    }

    static Stream<String> requestDocking() {
        return Stream.of("запит на стикування", "запроси майданчик", "запит на посадку",
                "зв'яжись із вежею та отримай посадковий майданчик",
                "запит дозволу на посадку", "запросити посадковий майданчик");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, ToggleCargoScoopCommand.ID);
    }

    static Stream<String> cargoScoop() {
        return Stream.of("відкрити вантажний ківш", "розгорнути вантажний ківш", "відкрити вантажний відсік", "відкрити двері вантажного відсіку", "вантажозабірник");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, ToggleNightVisionOnOffCommand.ID);
    }

    static Stream<String> nightVision() {
        return Stream.of("нічне бачення", "нічний зір", "увімкнути нічне бачення", "вимкнути нічне бачення");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, ToggleLightsOnOffCommand.ID);
    }

    static Stream<String> lights() {
        return Stream.of("фари", "увімкни світло", "вимкнути світло", "вогні", "увімкнути світло");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DismissShipToOrbitCommand.ID);
    }

    static Stream<String> dismissShip() {
        return Stream.of("відправити корабель", "прибери корабель", "корабель на орбіту", "відпусти корабель", "вільно");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TaxiToLandingPadCommand.ID);
    }

    static Stream<String> taxi() {
        return Stream.of("автопілот", "таксі", "автоматичне стикування", "автопосадка", "таксі", "авто таксі");
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
        return Stream.of("розгорнути зброю", "зброя готова", "до бою", "озброїтись");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RetractHardpointsCommand.ID);
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("прибрати зброю", "зброю прибрати", "сховай зброю", "відбій");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DeployHeatSinkCommand.ID);
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("випустити тепловідвід", "запустити тепловідвід", "скидання тепла");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, TargetHostileHighestThreatCommand.ID);
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("пріоритетна ціль", "ціль найбільша загроза", "наступний ворог", "вибрати ворога");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DeployShieldCellCommand.ID);
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("активувати щитову комірку", "використати щитову комірку",
                "увімкнути щитову комірку", "банк щитових комірок", "запустити енергокомірку");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DeployChaffCommand.ID);
    }

    static Stream<String> deployChaff() {
        return Stream.of("випусти пастки", "запусти пастки", "застосувати диполі", "вистрелити пастками", "запустити дипольні відбивачі", "скинь пастки");
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
        return Stream.of("енергію на щити", "максимум щитів", "посилити щити");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToEnginesCommand.ID);
    }

    static Stream<String> powerToEngines() {
        return Stream.of("енергію на двигуни", "максимум двигунів", "посилити двигуни");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToWeaponsCommand.ID);
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("енергію на зброю", "максимум зброї", "посилити зброю");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, EqualizePowerCommand.ID);
    }

    static Stream<String> resetPower() {
        return Stream.of("зрівноваж живлення", "баланс живлення", "скидання живлення", "розподілити живлення порівну");
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
        return Stream.of("Відкрий FSS і скануй.", "Виконати фільтроване спектральне сканування",
                "повне спектральне сканування", "сканування системи");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NavigateToBioSampleCodexEntryCommand.ID);
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("навігація до наступного біозразка", "навігація до наступної органіки", "навігація до запису кодексу");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FindMiningSiteCommand.ID);
    }

    static Stream<String> findMiningSite() {
        return Stream.of("знайди місце видобутку александриту в радіусі 300 світлових років",
                "знайди місце видобутку бромеліту в радіусі 1200 світлових років",
                "знайди астероїдне поле із золотом");
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
        return Stream.of("ввести пункт призначення авіаносця", "встановити пункт призначення авіаносця",
                "ввести наступний пункт призначення авіаносця");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(82)
    @MethodSource
    void findNearestCarrier(String input) throws InterruptedException {
        assertRouted(input, FindNearestFleetCarrierCommand.ID);
    }

    static Stream<String> findNearestCarrier() {
        return Stream.of("знайти найближчий флотський авіаносець", "найближчий авіаносець");
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
        return Stream.of("лети до авіаносця ескадрильї", "проклади маршрут до авіаносця ескадрильї", "проклади курс на авіаносець ескадрильї");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierDataQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("статус авіаносця ескадрильї", "фінанси авіаносця ескадрильї",
                "баланс авіаносця ескадрильї", "як довго ми можемо експлуатувати авіаносець ескадрильї",
                "тритій авіаносця ескадрильї", "паливо авіаносця ескадрильї",
                "рівень палива авіаносця ескадрильї");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierRouteQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of("маршрут авіаносця ескадрильї", "скільки стрибків на маршруті авіаносця ескадрильї",
                "маршрут стрибків авіаносця ескадрильї");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierFinalDestinationQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("куди летить авіаносець ескадрильї", "кінцевий пункт призначення авіаносця ескадрильї", "курс авіаносця ескадрильї");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierETAQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("час прибуття авіаносця ескадрильї", "коли прибуде авіаносець ескадрильї",
                "скільки чекати прибуття авіаносця ескадрильї");
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
        return Stream.of("лети до флотського авіаносця", "повернутись до авіаносця", "веди нас до авіаносця");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierDataQueryCommand.ID);
    }

    static Stream<String> bareCarrierStatusDefaultsToFleet() {
        return Stream.of("статус авіаносця", "баланс авіаносця", "кошти авіаносця");
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
        return Stream.of("вимкни всі оголошення");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SetReminderCommand.ID);
    }

    static Stream<String> setReminder() {
        return Stream.of("встанови нагадування дозаправитись на наступній зупинці");
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
        return Stream.of("відкрити карту галактики", "показати карту галактики", "відобразити карту галактики");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenSystemMapCommand.ID);
    }

    static Stream<String> systemMap() {
        return Stream.of("відкрити локальну карту", "показати карту системи", "відобразити карту системи");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, ShowNavigationPanelCommand.ID);
    }

    static Stream<String> navigationPanel() {
        return Stream.of("показати панель навігації", "відкрити панель навігації");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, ShowModulesPanelCommand.ID);
    }

    static Stream<String> modulesPanel() {
        return Stream.of("показати панель модулів", "відкрити панель модулів", "відобразити панель модулів");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, ShowStatusPanelCommand.ID);
    }

    static Stream<String> statusPanel() {
        return Stream.of("показати панель статусу", "відкрити панель статусу");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, ShowInventoryPanelCommand.ID);
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("показати панель інвентарю", "відкрити панель інвентарю");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, ExitCloseCommand.ID);
    }

    static Stream<String> closePanel() {
        return Stream.of("закрити панель", "вийти з панелі");
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
        return Stream.of("Де ми зараз?", "яке наше місцезнаходження", "де ми", "скільки триває день тут");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, AnalyzeShipLoadoutQueryCommand.ID);
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("спорядження корабля", "на чому я лечу", "обладнання корабля",
                "чи є на борту паливний ківш", "чи є на борту зброя",
                "яка зброя встановлена", "чи є на борту переробник");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCargoHoldQueryCommand.ID);
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("вантажний відсік", "що ми веземо", "вміст вантажу");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeRouterQueryCommand.ID);
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("прокладений маршрут", "залишок стрибків", "скільки стрибків до цілі");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStationsQueryCommand.ID);
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("станції в системі", "які станції", "найближчі станції",
                "чи є тут станції або порти", "чи є порти в цій зоряній системі");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarObjectsQueryCommand.ID);
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("Які планети чи місяці придатні для посадки в цій системі?",
                "Чи є крижані кільця в цій зоряній системі");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarSignalsQueryCommand.ID);
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of("Які сигнали є в цій системі?", "Які сигнали ти бачиш?",
                "Чи є цікаві сигнали?", "Сигнали системи?", "Що є в цій системі?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioScansStarSystemQueryCommand.ID);
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("Які планети ще потребують біологічного чи органічного сканування?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioSamplesPlanetSurfaceQueryCommand.ID);
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of("Які біосканування ми завершили на цій планеті?", "Які органічні об'єкти ще треба сканувати на цій планеті?", "Яка органіка чи біологія є на цій планеті");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, AnalyzePlayerProfileQueryCommand.ID);
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("профіль гравця", "профіль гравця огляд рангів", "профіль гравця огляд прогресу");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierDataQueryCommand.ID);
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of("Яка дальність дії нашого авіаносця?", "Який статус палива флотського авіаносця",
                "Як довго ми можемо працювати на поточних коштах?",
                "Як далеко може стрибнути авіаносець із поточним тритієм?", "тритій авіаносця", "паливо авіаносця",
                "скільки тритію на авіаносці", "рівень тритію", "Рівень палива авіаносця"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromFleetCarrierQueryCommand.ID);
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("Як далеко ми від авіаносця?", "Відстань до флотського авіаносця?",
                "Як далеко флотський авіаносець?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFsdTargetQueryCommand.ID);
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("ціль фсд", "інформація про наступний стрибок");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, AnalyzeExplorationProfitsQueryCommand.ID);
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("Потенціал дослідницького прибутку в цій системі.",
                "Який потенціал дослідницького прибутку в цій системі?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TimeQueryCommand.ID);
    }

    static Stream<String> queryTime() {
        return Stream.of("поточний час", "котра година", "час UTC");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSystemSecurityQueryCommand.ID);
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("безпека системи", "хто контролює цю систему", "домінуюча фракція");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, StationDataQueryCommand.ID);
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("деталі станції", "які послуги є на цій станції",
                "які послуги тут", "інформація про станцію");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyseMaterialsQueryCommand.ID);
    }

    static Stream<String> queryMaterials() {
        return Stream.of("інвентар матеріалів залізо", "скільки заліза в нас", "скільки ванадію в нас");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMaterialsOnPlanetQueryCommand.ID);
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("Які матеріали доступні на цій планеті?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromTheBubbleQueryCommand.ID);
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("Як далеко ми від Бульбашки?", "Відстань до Землі", "Як далеко Земля",
                "як далеко до цивілізації", "як далеко до Землі");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, AnalyzeLastScanQueryCommand.ID);
    }

    static Stream<String> queryLastScan() {
        return Stream.of("Проаналізуй останнє сканування");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, RemindTargetDestinationQueryCommand.ID);
    }

    static Stream<String> queryReminder() {
        return Stream.of("нагадування", "яке було нагадування", "чи є нагадування");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierETAQueryCommand.ID);
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("Який час прибуття нашого флотського авіаносця?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeGeologyInStarSystemQueryCommand.ID);
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("геосигнали", "геологічні сигнали", "вулканічна активність");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMarketsQueryCommand.ID);
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("локальні ринки", "ринки на станціях і поселеннях", "ринки на аванпостах у системі");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBountiesCollectedQueryCommand.ID);
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("нагороди", "загальні нагороди", "скільки нагород");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(230)
    @MethodSource
    void queryKeyBindings(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMisingKeyBindingQueryCommand.ID);
    }

    static Stream<String> queryKeyBindings() {
        return Stream.of("перевірити прив'язки клавіш", "відсутні прив'язки клавіш", "непризначені клавіші");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, BiomeAnalyzerQueryCommand.ID);
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("Проаналізуй біом цієї зоряної системи", "Аналіз біому для планети А 1");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromLastBioSampleQueryCommand.ID);
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("відстань до останнього біозразка", "Як далеко ми від останнього біозразка");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierRouteQueryCommand.ID);
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of("Що на маршруті авіаносця?", "Який маршрут нашого флотського авіаносця?",
                "Скільки стрибків на маршруті авіаносця?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierFinalDestinationQueryCommand.ID);
    }

    static Stream<String> queryCarrierDestination() {
        return Stream.of("Куди летить наш авіаносець?", "Який кінцевий пункт призначення авіаносця?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SetCarrierFuelReserveCommand.ID);
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("Встанови рівень резерву палива на 5000", "Встанови резерв палива на 10000",
                "Резерв палива 15000", "Встанови резерв палива на п'ятнадцять тисяч");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DisembarkCommand.ID);
    }

    static Stream<String> disembark() {
        return Stream.of("висадитися");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, ShowCommanderPanelCommand.ID);
    }

    static Stream<String> openCentralPanel() {
        return Stream.of("Відкрити панель командира", "відкрити центральну панель",
                "відкрити панель ролі", "відкрити наколінний планшет");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, ShowFighterPanelCommand.ID);
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("показати панель винищувача", "відкрити панель винищувача");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FighterFireAtWillCommand.ID);
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("вільний вогонь", "вогонь на власний розсуд", "вогонь", "відкрити вогонь");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FighterAttackTargetCommand.ID);
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("винищувач атакуй мою ціль", "атакуй мою ціль", "зосередься на моїй цілі");
    }
}
