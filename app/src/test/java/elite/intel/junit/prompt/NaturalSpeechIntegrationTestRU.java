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
public class NaturalSpeechIntegrationTestRU {

    private static final int LLM_WAIT_MS = 3000;
    private static final int LLM_POLL_MS = 100;

    private HandlerCapture capture;

    @BeforeAll
    void bootstrap() throws InterruptedException {
        SystemSession systemSession = SystemSession.getInstance();
        systemSession.setConversationalMode(false);
        systemSession.setLanguage(Language.RU);
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
        return Stream.of("проснись", "просыпайся");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SleepCommand.ID);
    }

    static Stream<String> ignoreMe() {
        return Stream.of("игнорируй меня", "не слушай", "спи");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, InterruptCommand.ID);
    }

    static Stream<String> interrupt() {
        return Stream.of("перебей", "прерви", "останови речь", "прекрати говорить", "прервать", "отставить", "заткнись");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToCombatModeCommand.ID);
    }

    static Stream<String> combatMode() {
        return Stream.of("боевой режим", "переключись в боевой режим", "включи боевой режим", "боевой");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToAnalysisModeCommand.ID);
    }

    static Stream<String> analysisMode() {
        return Stream.of("режим анализа", "переключись в режим анализа", "режим исследователя",
                "HUD анализа", "включи режим анализа");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, ResetHeadLookAheadCommand.ID);
    }

    static Stream<String> lookAhead() {
        return Stream.of("смотреть вперёд", "сброс", "смотри вперёд", "обзор по центру");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(16)
    @MethodSource
    void honkTheSystem(String input) throws InterruptedException {
        assertRouted(input, HonkCommand.ID);
    }

    static Stream<String> honkTheSystem() {
        return Stream.of("исследуй систему", "сканируй систему", "отсканируй систему", "сканируй");
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
        return Stream.of("стоп двигатели", "остановись", "все стоп",
                "заглуши двигатели", "сбрось тягу", "нулевая тяга", "остановить корабль");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SetSpeed25Command.ID);
    }

    static Stream<String> speed25() {
        return Stream.of("четверть тяги", "25 процентов", "малый ход");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SetSpeed50Command.ID);
    }

    static Stream<String> speed50() {
        return Stream.of("половина тяги", "50 процентов", "пол скорости");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SetSpeed75Command.ID);
    }

    static Stream<String> speed75() {
        return Stream.of("три четверти тяги", "75 процентов", "три четверти скорости");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SetSpeed100Command.ID);
    }

    static Stream<String> speed100() {
        return Stream.of("полная тяга", "100 процентов", "полная скорость", "максимальная тяга");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, IncreaseSpeedCommand.ID);
    }

    static Stream<String> speedPlus() {
        return Stream.of("увеличь скорость на 10", "увеличь скорость на 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DecreaseSpeedCommand.ID);
    }

    static Stream<String> speedMinus() {
        return Stream.of("уменьши скорость на 10", "уменьши скорость на 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SetOptimalSpeedCommand.ID);
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("установи оптимальную скорость", "оптимальная скорость захода");
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
        return Stream.of("прыжок в гиперпространство", "прыжок", "уходим",
                "поехали", "прыжок к следующей точке маршрута");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, EnterSuperCruiseCommand.ID);
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("войти в суперкруиз", "включить суперкруиз", "суперкруиз", "световая скорость", "активировать суперкруиз", "на форсаж");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {
        assertRouted(input, DropFromSuperCruiseCommand.ID);
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("выходи здесь", "выход", "выйти из суперкруиза", "дроп");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NavigateToMissionTargetCommand.ID);
    }

    static Stream<String> navigateToMission() {
        return Stream.of("лети к активной миссии", "проложи маршрут к активной миссии",
                "к активной миссии", "лети к миссии", "навигация к миссии");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("лети к флотскому авианосцу", "вернуться к авианосцу", "веди нас к авианосцу");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, CancelNavigationCommand.ID);
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("отмени навигацию", "прервать навигацию", "выключи навигацию");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, NavigateToLandingZoneCommand.ID);
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("лети к зоне посадки", "курс к зоне посадки", "веди меня обратно в зону посадки");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TargetDestinationCommand.ID);
    }

    static Stream<String> targetDestination() {
        return Stream.of("выбрать цель назначения", "выбрать пункт назначения");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(38)
    @MethodSource
    void clearActiveMissions(String input) throws InterruptedException {
        assertRouted(input, ClearActiveMissionsCommand.ID);
    }

    static Stream<String> clearActiveMissions() {
        return Stream.of("удали все задания", "удалить все задания");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NavigateToTradeStopCommand.ID);
    }

    static Stream<String> nextTradeStop() {
        return Stream.of("лети к следующей торговой остановке", "к следующей торговой остановке");
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
        return Stream.of("шасси", "выпустить шасси", "опустить шасси", "развернуть шасси", "приготовся к посадке");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RetractLandingGearCommand.ID);
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("убрать шасси", "шасси вверх", "поднять шасси", "сложить шасси");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, RequestDockingCommand.ID);
    }

    static Stream<String> requestDocking() {
        return Stream.of("запрос на стыковку", "запроси площадку", "запрос на посадку",
                "свяжись с башней и получи посадочную площадку",
                "запрос разрешения на посадку", "запросить посадочную площадку");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, ToggleCargoScoopCommand.ID);
    }

    static Stream<String> cargoScoop() {
        return Stream.of("открыть грузовой совок", "развернуть грузовой совок", "открыть грузовой отсек", "открыть дверь грузового отсека", "грузозаборник");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, ToggleNightVisionOnOffCommand.ID);
    }

    static Stream<String> nightVision() {
        return Stream.of("ночное видение", "ночное зрение", "включить ночное видение", "выключить ночное видение");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, ToggleLightsOnOffCommand.ID);
    }

    static Stream<String> lights() {
        return Stream.of("фары", "свет включить", "выключить свет", "огни", "включить свет");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DismissShipToOrbitCommand.ID);
    }

    static Stream<String> dismissShip() {
        return Stream.of("отправить корабль", "убери корабль", "корабль на орбиту", "свободен", "вольно");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TaxiToLandingPadCommand.ID);
    }

    static Stream<String> taxi() {
        return Stream.of("автопилот", "такси", "автоматическая стыковка", "автопосадка", "такси", "авто такси");
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
        return Stream.of("развернуть орудия", "оружие готово", "к бою", "вооружиться");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RetractHardpointsCommand.ID);
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("убрать орудия", "оружие убрать", "сверни оружие", "отбой");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DeployHeatSinkCommand.ID);
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("выбросить теплоотвод", "запустить теплоотвод", "сброс тепла");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, TargetHostileHighestThreatCommand.ID);
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("приоритетная цель", "цель наибольшая угроза", "следующий враг", "выбрать врага");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DeployShieldCellCommand.ID);
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("активировать ячейку щита", "использовать ячейку щита",
                "включить ячейку щита", "банк ячеек щита", "запустить энергоячейку");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DeployChaffCommand.ID);
    }

    static Stream<String> deployChaff() {
        return Stream.of("выбросить помехи", "запустить помехи", "применить деполи", "выстрелить помехами", "запустить ракеты-ловушки", "скинь ловушки");
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
        return Stream.of("питание на щиты", "максимум щитов", "усилить щиты");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToEnginesCommand.ID);
    }

    static Stream<String> powerToEngines() {
        return Stream.of("питание на двигатели", "максимум двигателей", "усилить двигатели");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToWeaponsCommand.ID);
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("питание на оружие", "максимум оружия", "усилить оружие");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, EqualizePowerCommand.ID);
    }

    static Stream<String> resetPower() {
        return Stream.of("уравновесить питание", "баланс питания", "сброс питания", "распределить питание равномерно");
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
        return Stream.of("Открой FSS и сканируй.", "Выполнить фильтрованное спектральное сканирование",
                "полное спектральное сканирование", "сканирование системы");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NavigateToBioSampleCodexEntryCommand.ID);
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("навигация к следующему биообразцу", "навигация к следующему органическому", "навигация к записи кодекса");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FindMiningSiteCommand.ID);
    }

    static Stream<String> findMiningSite() {
        return Stream.of("найди место добычи александрита в радиусе 300 световых лет",
                "найди место добычи бромелита в радиусе 1200 световых лет",
                "найди астероидное поле с золотом");
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
        return Stream.of("ввести пункт назначения авианосца", "установить пункт назначения авианосца",
                "ввести следующий пункт назначения авианосца");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(82)
    @MethodSource
    void findNearestCarrier(String input) throws InterruptedException {
        assertRouted(input, FindNearestFleetCarrierCommand.ID);
    }

    static Stream<String> findNearestCarrier() {
        return Stream.of("найти ближайший флотский авианосец", "ближайший авианосец");
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
        return Stream.of("лети к авианосцу эскадрильи", "проложи маршрут к авианосцу эскадрильи", "проложи курс на авианосец эскадрильи");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierDataQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("статус авианосца эскадрильи", "финансы авианосца эскадрильи",
                "баланс авианосца эскадрильи", "как долго мы можем эксплуатировать авианосец эскадрильи",
                "тритий авианосца эскадрильи", "топливо авианосца эскадрильи",
                "уровень топлива авианосца эскадрильи");
    }


    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierRouteQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of("маршрут авианосца эскадрильи", "сколько прыжков на маршруте авианосца эскадрильи",
                "маршрут прыжков авианосца эскадрильи");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierFinalDestinationQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("куда летит авианосец эскадрильи", "конечный пункт назначения авианосца эскадрильи", "курс авианосца эскадрильи");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierETAQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("время прибытия авианосца эскадрильи", "когда прибудет авианосец эскадрильи",
                "сколько ждать прибытия авианосца эскадрильи");
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
        return Stream.of("лети к флотскому авианосцу", "вернуться к авианосцу", "веди нас к авианосцу");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierDataQueryCommand.ID);
    }

    static Stream<String> bareCarrierStatusDefaultsToFleet() {
        return Stream.of("статус авианосца", "баланс авианосца", "средства авианосца");
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
        return Stream.of("отключи все объявления");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SetReminderCommand.ID);
    }

    static Stream<String> setReminder() {
        return Stream.of("установи напоминание дозаправиться на следующей остановке");
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
        return Stream.of("открыть карту галактики", "показать карту галактики", "отобразить карту галактики");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenSystemMapCommand.ID);
    }

    static Stream<String> systemMap() {
        return Stream.of("открыть локальную карту", "показать карту системы", "отобразить карту системы");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, ShowNavigationPanelCommand.ID);
    }

    static Stream<String> navigationPanel() {
        return Stream.of("показать панель навигации", "открыть панель навигации");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, ShowModulesPanelCommand.ID);
    }

    static Stream<String> modulesPanel() {
        return Stream.of("показать панель модулей", "открыть панель модулей", "отобразить панель модулей");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, ShowStatusPanelCommand.ID);
    }

    static Stream<String> statusPanel() {
        return Stream.of("показать панель статуса", "открыть панель статуса");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, ShowInventoryPanelCommand.ID);
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("показать панель инвентаря", "открыть панель инвентаря");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, ExitCloseCommand.ID);
    }

    static Stream<String> closePanel() {
        return Stream.of("закрыть панель", "выйти из панели");
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
        return Stream.of("Где мы сейчас?", "каково наше местоположение", "где мы", "как долго длится день здесь");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, AnalyzeShipLoadoutQueryCommand.ID);
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("снаряжение корабля", "на чём я лечу", "оборудование корабля",
                "есть ли на борту топливный захват", "есть ли на борту оружие",
                "какое оружие установлено", "есть ли на борту переработчик");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCargoHoldQueryCommand.ID);
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("грузовой отсек", "что мы везём", "содержимое груза");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeRouterQueryCommand.ID);
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("проложенный маршрут", "оставшиеся прыжки", "сколько прыжков до цели");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStationsQueryCommand.ID);
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("станции в системе", "какие станции", "ближайшие станции",
                "есть ли здесь станции или порты", "есть ли порты в этой звёздной системе");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarObjectsQueryCommand.ID);
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("Какие планеты или луны с возможностью посадки есть в этой системе?",
                "Есть ли ледяные кольца в этой звёздной системе");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarSignalsQueryCommand.ID);
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of("Какие сигналы есть в этой системе?", "Какие сигналы ты видишь?",
                "Есть ли интересные сигналы?", "Сигналы системы?", "Что есть в этой системе?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioScansStarSystemQueryCommand.ID);
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("Какие планеты ещё нуждаются в биологическом или органическом сканировании?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioSamplesPlanetSurfaceQueryCommand.ID);
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of("Какие биосканирования мы завершили на этой планете?", "Какие органические объекты ещё нужно сканировать на этой планете?", "Какая органика или биология есть на этой планете");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, AnalyzePlayerProfileQueryCommand.ID);
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("профиль игрока", "профиль игрока обзор рангов", "профиль игрока обзор прогресса");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierDataQueryCommand.ID);
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of("Какова дальность действия нашего авианосца?", "Каков статус топлива флотского авианосца",
                "Как долго мы можем работать на текущих средствах?",
                "Как далеко может прыгнуть авианосец с текущим тритием?", "тритий авианосца", "топливо авианосца",
                "сколько трития на авианосце", "уровень трития", "Уровень топлива авианосца"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromFleetCarrierQueryCommand.ID);
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("Как далеко мы от авианосца?", "Расстояние до флотского авианосца?",
                "Как далеко флотский авианосец?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFsdTargetQueryCommand.ID);
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("цель фсд", "информация о следующем прыжке");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, AnalyzeExplorationProfitsQueryCommand.ID);
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("Потенциал исследовательской прибыли в этой системе.",
                "Каков потенциал исследовательской прибыли в этой системе?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TimeQueryCommand.ID);
    }

    static Stream<String> queryTime() {
        return Stream.of("текущее время", "который час", "время UTC");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSystemSecurityQueryCommand.ID);
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("безопасность системы", "кто контролирует эту систему", "доминирующая фракция");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, StationDataQueryCommand.ID);
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("детали станции", "какие услуги есть на этой станции",
                "какие услуги здесь", "информация о станции");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyseMaterialsQueryCommand.ID);
    }

    static Stream<String> queryMaterials() {
        return Stream.of("инвентарь материалов железо", "сколько железа у нас", "сколько ванадия у нас");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMaterialsOnPlanetQueryCommand.ID);
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("Какие материалы доступны на этой планете?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromTheBubbleQueryCommand.ID);
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("Как далеко мы от Пузыря?", "Расстояние до Земли", "Как далеко Земля",
                "как далеко до цивилизации", "как далеко до Земли");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, AnalyzeLastScanQueryCommand.ID);
    }

    static Stream<String> queryLastScan() {
        return Stream.of("Проанализируй последнее сканирование");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, RemindTargetDestinationQueryCommand.ID);
    }

    static Stream<String> queryReminder() {
        return Stream.of("напоминание", "какое было напоминание", "есть напоминания");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierETAQueryCommand.ID);
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("Каково время прибытия нашего флотского авианосца?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeGeologyInStarSystemQueryCommand.ID);
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("геосигналы", "геологические сигналы", "вулканическая активность");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMarketsQueryCommand.ID);
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("местные рынки", "рынки на станциях и поселениях", "рынки на аванпостах в системе");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBountiesCollectedQueryCommand.ID);
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("награды", "общие награды", "сколько наград");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, BiomeAnalyzerQueryCommand.ID);
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("Проанализируй биом этой звёздной системы", "Анализ биома для планеты А 1");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromLastBioSampleQueryCommand.ID);
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("расстояние до последнего биообразца", "Как далеко мы от последнего биообразца");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierRouteQueryCommand.ID);
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of("Что на маршруте авианосца?", "Каков маршрут нашего флотского авианосца?",
                "Сколько прыжков на маршруте авианосца?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierFinalDestinationQueryCommand.ID);
    }

    static Stream<String> queryCarrierDestination() {
        return Stream.of("Куда летит наш авианосец?", "Какой конечный пункт назначения авианосца?");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SetCarrierFuelReserveCommand.ID);
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("Установи уровень резерва топлива на 5000", "Установи резерв топлива на 10000",
                "Резерв топлива 15000", "Установи резерв топлива на пятнадцать тысяч");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DisembarkCommand.ID);
    }

    static Stream<String> disembark() {
        return Stream.of("высадиться");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, ShowCommanderPanelCommand.ID);
    }

    static Stream<String> openCentralPanel() {
        return Stream.of("Открыть панель командира", "открыть центральную панель",
                "открыть панель роли", "открыть наколенный планшет");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, ShowFighterPanelCommand.ID);
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("показать панель истребителя", "открыть панель истребителя");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FighterFireAtWillCommand.ID);
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("свободный огонь", "огонь по своему усмотрению", "огонь", "открыть огонь");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FighterAttackTargetCommand.ID);
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("истребитель атакуй мою цель", "атакуй мою цель", "сосредоточиться на моей цели");
    }

}