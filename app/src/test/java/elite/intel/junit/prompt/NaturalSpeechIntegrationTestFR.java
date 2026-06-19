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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import elite.intel.ai.brain.actions.command.builtin.*;
import elite.intel.ai.brain.actions.handlers.query.*;


/**
 * French-language integration test for the NaturalSpeech routing system.
 * Mirrors {@link NaturalSpeechIntegrationTestEN} in structure and @Order numbers.
 * <p>
 * Intended as a routing checklist for the French voice localization. The phrases
 * should be natural spoken French, not literal translations of the English tests.
 * Keep this file aligned with {@code i18n/ai_action_aliases_fr.properties}.
 * <p>
 * INTENTION TYPES
 * - INFO: asks for information; should route to a query and speak an answer.
 * - ACTION_JEU: interacts with Elite Dangerous through bindings, panels, navigation, or typing.
 * - ACTION_RECHERCHE: searches data and may then prepare navigation, reminders, or route entry.
 * - ACTION_APP: changes Elite Intel state, settings, announcements, reminders, or voice behavior.
 * - DANGEREUX: clears, deletes, cancels, or otherwise removes state; phrases must be explicit.
 * <p>
 * NOTES FOR THE LOCALIZER:
 * - "honk" in French routes to OPEN_FSS (not HONK_THE_SYSTEM) — see openFss().
 * - Phrases with accent variants (e.g. "écoute"/"ecoute") only need one tested here;
 * the alias file covers both spellings.
 * <p>
 * REQUIREMENTS
 * 1) Local LLM installed and configured with the supported model.
 * 2) App started at least once with game running for basic session data.
 * 3) Language set to FR in app settings before running.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NaturalSpeechIntegrationTestFR {

    private static final int LLM_WAIT_MS = 8000;
    private static final int LLM_POLL_MS = 100;

    private HandlerCapture capture;

    @BeforeAll
    void bootstrap() throws InterruptedException {
        SystemSession systemSession = SystemSession.getInstance();
        systemSession.setConversationalMode(false);
        systemSession.setLanguage(Language.FR);
        HeadlessBootstrap.start();
        WebSocketBroadcaster.getInstance().start();
        capture = new HandlerCapture();
        Thread.sleep(2000);
        EventBusManager.publish(new SensorDataEvent("command_verify_connection", "Acknowledge connection"));
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
    // BLOC 01 — ÉCOUTE, VEILLE ET INTERRUPTION
    // Type: ACTION_APP
    // Rôle: contrôler l'état d'écoute et interrompre la voix, sans interaction directe avec le jeu.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(10)
    @MethodSource
    void startListening(String input) throws InterruptedException {
        assertRouted(input, WakeupCommand.ID);
    }

    static Stream<String> startListening() {
        return Stream.of("réveil",
                "écoute commande vocale",
                "réveille-toi"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SleepCommand.ID);
    }

    static Stream<String> ignoreMe() {
        return Stream.of("passe en mode veille",
                "tu peux disposer",
                "désactive les commandes vocales"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, InterruptCommand.ID);
    }

    static Stream<String> interrupt() {
        return Stream.of(
                "arrête de parler",
                "stop voix",
                "silence"
        );
    }

    // =========================================================================
    // BLOC 03 — PILOTAGE DIRECT DU VAISSEAU: MODES ET VUE
    // Type: ACTION_JEU
    // Rôle: vérifier les bascules de mode cockpit et la remise à zéro de la vue.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToCombatModeCommand.ID);
    }

    static Stream<String> combatMode() {
        return Stream.of("active le mode combat",
                "passe en mode combat"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, SwitchToAnalysisModeCommand.ID);
    }

    static Stream<String> analysisMode() {
        return Stream.of(
                "active le mode analyse",
                "passe en mode analyse"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, ResetHeadLookAheadCommand.ID);
    }

    static Stream<String> lookAhead() {
        return Stream.of(
                "réinitialise la vue tête",
                "reset head look"
        );
    }

    // NOTE: In French, "honk" routes to OPEN_FSS (not HONK_THE_SYSTEM).
    // There is no honkTheSystem() test for French — see openFss() at Order(70).   ok:')

    // =========================================================================
    // BLOC 03 — PILOTAGE DIRECT DU VAISSEAU: VITESSE
    // Type: ACTION_JEU
    // Rôle: vérifier les ordres de poussée et de vitesse; groupe à fort risque de collision.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(20)
    @MethodSource
    void speedZero(String input) throws InterruptedException {
        assertRouted(input, SetSpeedZeroCommand.ID);
    }

    static Stream<String> speedZero() {
        return Stream.of(
                "arrête le vaisseau",
                "coupe les gaz",
                "stop"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SetSpeed25Command.ID);
    }

    static Stream<String> speed25() {
        return Stream.of(
                "règle la vitesse à 25 pour cent",
                "propulseurs à 25 pour cent",
                "vitesse lente"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SetSpeed50Command.ID);
    }

    static Stream<String> speed50() {
        return Stream.of(
                "règle la vitesse à 50 pour cent",
                "propulseurs à 50 pour cent", 
                "vitesse moyenne" 
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SetSpeed75Command.ID);
    }

    static Stream<String> speed75() {
        return Stream.of("règle la vitesse à 75 pour cent", 
                "propulseurs à 75 pour cent",                       
                "vitesse rapide"                                    
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SetSpeed100Command.ID);
    }

    static Stream<String> speed100() {
        return Stream.of("règle la vitesse à 100 pour cent", 
                "propulseurs à 100 pour cent",
                "vitesse maximum",                                   
                "plein gaz"                                          
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, IncreaseSpeedCommand.ID);
    }

    static Stream<String> speedPlus() {
        return Stream.of(
                "augmente la vitesse de 10",
                "augmente la vitesse de 5"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DecreaseSpeedCommand.ID);
    }

    static Stream<String> speedMinus() {
        return Stream.of(
                "réduis la vitesse de 10",
                "ralentis de 5"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SetOptimalSpeedCommand.ID);
    }

    static Stream<String> optimalSpeed() {
        return Stream.of(
                "règle la vitesse optimale",        
                "vitesse d'approche optimale"               
        );
    }

    // =========================================================================
    // BLOC 02 — NAVIGATION ET ITINÉRAIRES IMMÉDIATS
    // Type: ACTION_JEU / DANGEREUX
    // Rôle: tracer, sélectionner ou annuler des destinations et routes.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(30)
    @MethodSource
    void jumpToHyperspace(String input) throws InterruptedException {
        assertRouted(input, JumpToHyperspaceCommand.ID);
    }

    static Stream<String> jumpToHyperspace() {
        return Stream.of(
                "saute en hyperespace",             
                "active le saut FSD",                       
                "lance le saut",
                "active le réacteur FSD"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, EnterSuperCruiseCommand.ID);
    }

    static Stream<String> enterSupercruise() {
        return Stream.of(
                "entre en super navigation",        
                "active la super navigation"                
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {
        assertRouted(input, DropFromSuperCruiseCommand.ID);
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of(
                "sors de la super navigation",          
                "quitte la super navigation"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NavigateToMissionTargetCommand.ID);
    }

    static Stream<String> navigateToMission() {
        return Stream.of(
                "navigue vers la mission active",               
                "trace l'itinéraire vers la mission",           
                "va à la mission active"                        
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of(
                "navigue vers mon porte-vaisseaux",  
                "trace l'itinéraire vers le porte-vaisseaux"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, CancelNavigationCommand.ID);
    }

    static Stream<String> cancelNavigation() {
        return Stream.of(
                "annule la navigation",             
                "arrête la navigation",             
                "annule l'itinéraire"               
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, NavigateToLandingZoneCommand.ID);
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of(
                "navigue vers la zone d'atterrissage", 
                "direction la zone d'atterrissage"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TargetDestinationCommand.ID);
    }

    static Stream<String> targetDestination() {
        return Stream.of(
                "cible la prochaine destination",
                "cible la destination suivante",
                "cible la prochaine étape de l'itinéraire",
                "sélectionne la prochaine destination",
                "sélectionne la prochaine étape d'itinéraire",
                "verrouille la destination suivante"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NavigateToTradeStopCommand.ID);
    }

    static Stream<String> nextTradeStop() {
        return Stream.of(
                "navigue vers le prochain arrêt commercial",
                "va au prochain point de commerce"
        );
    }

    // =========================================================================
    // BLOC 03 — PILOTAGE DIRECT DU VAISSEAU: SYSTÈMES
    // Type: ACTION_JEU
    // Rôle: vérifier les commandes immédiates de vol, docking, train, modes et systèmes.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(40)
    @MethodSource
    void deployLandingGear(String input) throws InterruptedException {
        assertRouted(input, DeployLandingGearCommand.ID);
    }

    static Stream<String> deployLandingGear() {
        return Stream.of(
                "déploie le train d'atterrissage",
                "sors le train d'atterrissage"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RetractLandingGearCommand.ID);
    }

    static Stream<String> retractLandingGear() {
        return Stream.of(
                "rentre le train d'atterrissage",
                "relève le train d'atterrissage"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, RequestDockingCommand.ID);
    }

    static Stream<String> requestDocking() {
        return Stream.of(
                "demande l'autorisation d'appontage",
                "demande d'appontage",
                "demande un quai"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, ToggleCargoScoopCommand.ID);
    }

    static Stream<String> cargoScoop() {
        return Stream.of(
                "trappe",
                "ouvre la trappe",
                "ferme la trappe",
                "déploie le récupérateur de cargaison",
                "rentre le récupérateur de cargaison",
                "ouvre la trappe du cargo",
                "ferme la trappe du cargo",
                "ouvre la trappe de la soute",
                "ferme la trappe de la soute"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, ToggleNightVisionOnOffCommand.ID);
    }

    static Stream<String> nightVision() {
        return Stream.of(
                "active la vision nocturne",
                "désactive la vision nocturne",
                "vision nocturne"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, ToggleLightsOnOffCommand.ID);
    }

    static Stream<String> lights() {
        return Stream.of(
                "allume les lumières",
                "éteins les lumières",
                "allume les phares",
                "éteins les phares",
                "allume les projecteurs",
                "éteins les projecteurs");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DismissShipToOrbitCommand.ID);
    }

    static Stream<String> dismissShip() {
        return Stream.of(
                "renvoie le vaisseau en orbite",
                "envoie le vaisseau se mettre en orbite",
                "envoie le vaisseau en orbite"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TaxiToLandingPadCommand.ID);
    }

    static Stream<String> taxi() {
        return Stream.of("mode pilote automatique",
                "active le pilotage automatique",
                "mode taxi",
                "prends les commandes");
    }

    // =========================================================================
    // BLOC 04 — COMBAT, ARMES, CIBLES ET CHASSEUR
    // Type: ACTION_JEU
    // Rôle: vérifier armements, ciblage, ordres au chasseur et gestion tactique.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(50)
    @MethodSource
    void deployHardpoints(String input) throws InterruptedException {
        assertRouted(input, DeployHardpointsCommand.ID);
    }

    static Stream<String> deployHardpoints() {
        return Stream.of(
                "déploie les points d'emport",
                "déploie les armes",
                "armes au clair"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RetractHardpointsCommand.ID);
    }

    static Stream<String> retractHardpoints() {
        return Stream.of(
                "rentre les points d'emport",
                "range les armes",
                "replie les points d'emport"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DeployHeatSinkCommand.ID);
    }

    static Stream<String> deployHeatSink() {
        return Stream.of(
                "lance un dissipateur thermique",
                "déploie un heat sink",
                "évacue la chaleur"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, TargetHostileHighestThreatCommand.ID);
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of(
                "cible la menace prioritaire",
                "sélectionne l'ennemi le plus dangereux",
                "verrouille la plus grande menace"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DeployShieldCellCommand.ID);
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of(
                "utilise une cellule de bouclier",
                "active une cellule de bouclier",
                "déploie une cellule de bouclier"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DeployChaffCommand.ID);
    }

    static Stream<String> deployChaff() {
        return Stream.of(
                "lance les paillettes",
                "déploie les leurres",
                "active le chaff"
        );
    }

    // =========================================================================
    // BLOC 05 — DISTRIBUTION DE PUISSANCE
    // Type: ACTION_JEU
    // Rôle: vérifier la répartition d'énergie du vaisseau.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(60)
    @MethodSource
    void powerToShields(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToShieldsCommand.ID);
    }

    static Stream<String> powerToShields() {
        return Stream.of(
                "redirige la puissance vers les boucliers",
                "priorité aux boucliers",
                "puissance dans les boucliers",
                "redirige la puissance vers les systèmes",
                "priorité aux systèmes",
                "puissance dans les systèmes"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToEnginesCommand.ID);
    }

    static Stream<String> powerToEngines() {
        return Stream.of(
                "redirige la puissance vers les moteurs",
                "priorité aux moteurs",
                "puissance dans les moteurs"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, TransferPowerToWeaponsCommand.ID);
    }

    static Stream<String> powerToWeapons() {
        return Stream.of(
                "redirige la puissance vers les armes",
                "priorité aux armes",
                "puissance dans les armes"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, EqualizePowerCommand.ID);
    }

    static Stream<String> resetPower() {
        return Stream.of(
                "réinitialise la puissance",
                "équilibre le distributeur",
                "remet la puissance par défaut"
        );
    }

    // =========================================================================
    // BLOC 12 — MINAGE, EXOBIOLOGIE ET CODEX
    // Type: INFO / ACTION_JEU / ACTION_RECHERCHE / DANGEREUX
    // Rôle: vérifier les scans, l'exobio, le minage et les actions de codex.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(70)
    @MethodSource
    void openFss(String input) throws InterruptedException {
        assertRouted(input, OpenFssScanSystemCommand.ID);
    }

    static Stream<String> openFss() {
        // NOTE: In French, "honk" maps to OPEN_FSS — not HONK_THE_SYSTEM.
        return Stream.of(
                "ouvre l'analyseur de système",
                "lance l'outil d'analyse complet de système",
                "affiche l'analyseur spectral",
                "ouvre le détecteur ACS"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NavigateToBioSampleCodexEntryCommand.ID);
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of(
                "navigue vers le prochain échantillon biologique",
                "va au prochain organique",
                "trace l'itinéraire vers l'entrée codex suivante"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FindMiningSiteCommand.ID);
    }

    static Stream<String> findMiningSite() {
        return Stream.of(
                "trouve un site de minage pour alexandrite dans 300 années-lumière",
                "cherche un champ d'astéroïdes avec de l'or",
                "où puis-je miner de la bromélite"
        );
    }

    // =========================================================================
    // BLOC 13 — PORTE-VAISSEAUX: ACTIONS PERSONNELLES
    // Type: ACTION_JEU / ACTION_APP / ACTION_RECHERCHE
    // Rôle: vérifier les actions liées au porte-vaisseau personnel.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(80)
    @MethodSource
    void enterCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, EnterFleetCarrierDestinationCommand.ID);
    }

    static Stream<String> enterCarrierDestination() {
        return Stream.of(
                "entre la destination du porte-vaisseaux",
                "définis la destination du porte-vaisseaux",
                "programme la prochaine destination du porte-vaisseaux"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(82)
    @MethodSource
    void findNearestCarrier(String input) throws InterruptedException {
        assertRouted(input, FindNearestFleetCarrierCommand.ID);
    }

    static Stream<String> findNearestCarrier() {
        return Stream.of(
                "trouve le porte-vaisseaux le plus proche",
                "cherche le porte-vaisseaux le plus proche",
                "où est le porte-vaisseaux le plus proche"
        );
    }

    // =========================================================================
    // BLOC 13 — PORTE-VAISSEAUX: ACTIONS D'ESCADRON
    // Type: ACTION_JEU / ACTION_RECHERCHE
    // Rôle: vérifier les actions où "d'escadron" doit être explicite.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(85)
    @MethodSource
    void navigateToSquadronCarrier(String input) throws InterruptedException {
        assertRouted(input, NavigateToSquadronCarrierCommand.ID);
    }

    static Stream<String> navigateToSquadronCarrier() {
        return Stream.of(
                "navigue vers le porte-vaisseau d'escadron",
                "trace l'itinéraire vers le porte-vaisseau d'escadron"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(86)
    @MethodSource
    void calculateNeutronRoute(String input) throws InterruptedException {
        assertRouted(input, CalculateNeutronStarRouteCommand.ID);
    }

    static Stream<String> calculateNeutronRoute() {
        return Stream.of(
                "calcule la route des étoiles à neutrons avec efficacité 20",
                "calcule l'itinéraire par étoiles à neutrons"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(87)
    @MethodSource
    void plotNextNeutronLeg(String input) throws InterruptedException {
        assertRouted(input, PlotRouteNextNeutronStarWaypointCommand.ID);
    }

    static Stream<String> plotNextNeutronLeg() {
        return Stream.of(
                "trace la route vers la prochaine étoile à neutrons",
                "planifie le prochain saut neutron",
                "va à la prochaine étoile à neutrons"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(88)
    @MethodSource
    void clearNeutronStarRoute(String input) throws InterruptedException {
        assertRouted(input, ClearNeutronRouteCommand.ID);
    }

    static Stream<String> clearNeutronStarRoute() {
        return Stream.of(
                "efface la route des étoiles à neutrons",
                "supprime l'itinéraire neutron",
                "annule le voyage par neutrons"
        );
    }

    // =========================================================================
    // BLOC 10 — ANNONCES, RADIO ET RAPPELS
    // Type: ACTION_APP / DANGEREUX
    // Rôle: vérifier les réglages internes, annonces vocales, radio et rappels.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(90)
    @MethodSource
    void disableAnnouncements(String input) throws InterruptedException {
        assertRouted(input, ToggleAllAnnouncementsCommand.ID);
    }

    static Stream<String> disableAnnouncements() {
        return Stream.of(
                "désactive toutes les annonces",
                "coupe toutes les notifications vocales",
                "plus d'annonces"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SetReminderCommand.ID);
    }

    static Stream<String> setReminder() {
        return Stream.of(
                "définis un rappel : acheter des drones avant de partir d'une station",
                "ajoute un rappel : vérifier le carburant"
        );
    }

    // =========================================================================
    // BLOC 06 — PANNEAUX ET INTERFACE DU JEU
    // Type: ACTION_JEU
    // Rôle: vérifier l'ouverture et la fermeture des panneaux et cartes du jeu.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(100)
    @MethodSource
    void galaxyMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenGalaxyMapCommand.ID);
    }

    static Stream<String> galaxyMap() {
        return Stream.of(
                "ouvre la carte galactique",
                "affiche la carte de la galaxie",
                "montre la galaxy map"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, DisplayOpenSystemMapCommand.ID);
    }

    static Stream<String> systemMap() {
        return Stream.of(
                "ouvre la carte du système",
                "affiche la carte locale",
                "montre la carte du système"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, ShowNavigationPanelCommand.ID);
    }

    static Stream<String> navigationPanel() {
        return Stream.of(
                "montre le panneau de navigation",
                "ouvre la navigation",
                "affiche le panneau nav"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, ShowModulesPanelCommand.ID);
    }

    static Stream<String> modulesPanel() {
        return Stream.of(
                "montre le panneau des modules",
                "ouvre les modules",
                "affiche le panneau d'équipement du vaisseau"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, ShowStatusPanelCommand.ID);
    }

    static Stream<String> statusPanel() {
        return Stream.of(
                "montre le panneau de statut",
                "affiche l'interface de statut du vaisseau",
                "ouvre l'état général du vaisseau"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, ShowInventoryPanelCommand.ID);
    }

    static Stream<String> inventoryPanel() {
        return Stream.of(
                "montre l'inventaire du vaisseau",
                "ouvre le panneau d'affichage du cargo",
                "ouvre l'inventaire des marchandises du vaisseau",
                "affiche le panneau de cargaison"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, ExitCloseCommand.ID);
    }

    static Stream<String> closePanel() {
        return Stream.of(
                "ferme le panneau",
                "quitte l'écran actuel",
                "close"
        );
    }

    // =========================================================================
    // BLOC 14/15 — REQUÊTES D'INFORMATION GÉNÉRALES
    // Type: INFO
    // Rôle: vérifier les demandes d'information qui doivent répondre sans déclencher d'action de jeu.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(200)
    @MethodSource
    void queryCurrentLocation(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCurrentLocationQueryCommand.ID);
    }

    static Stream<String> queryCurrentLocation() {
        return Stream.of(
                "où sommes-nous",
                "quelle est notre position actuelle",
                "dans quel système sommes-nous"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, AnalyzeShipLoadoutQueryCommand.ID);
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of(
                "quelle est la configuration du vaisseau",
                "que pilotons-nous",
                "avons-nous un collecteur de carburant"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, AnalyzeCargoHoldQueryCommand.ID);
    }

    static Stream<String> queryCargoHold() {
        return Stream.of(
                "que contient la soute",
                "quel est le contenu du cargo",
                "liste la cargaison"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeRouterQueryCommand.ID);
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of(
                "combien de sauts reste-t-il",
                "rapport sur notre itinéraire",
                "sommes-nous arrivés à destination",
                "rapport d'itinéraire"
                );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStationsQueryCommand.ID);
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of(
                "quelles stations y a-t-il dans ce système",
                "liste les ports spatiaux",
                "où puis-je me poser ici"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarObjectsQueryCommand.ID);
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of(
                "quels sont les corps du système",
                "y a-t-il des planètes ou lunes où l'on peut atterrir",
                "y a-t-il des anneaux autour de certains astres"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeStellarSignalsQueryCommand.ID);
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of(
                "quels signaux sont détectés",
                "y a-t-il des hotspots de minage",
                "analyse les sources inconnues"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioScansStarSystemQueryCommand.ID);
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of(
                "quelles planètes ont des signaux biologiques",
                "progression des scans biologiques dans le système",
                "combien d'espèces biologique reste-t-il à analyser dans le système"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBioSamplesPlanetSurfaceQueryCommand.ID);
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of(
                "quels organismes reste-t-il à scanner ici",
                "liste les échantillons exobiologiques",
                "que devons-nous encore prélever sur cette planète"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, AnalyzePlayerProfileQueryCommand.ID);
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of(
                "quel est mon profil de commandant",
                "affiche mes statistiques",
                "donne-moi le rapport sur le commandant"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierDataQueryCommand.ID);
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of(
                "quel est le statut de mon porte-vaisseaux",
                "rapport du porte-vaisseaux",
                "état du porte-vaisseaux"
        );
    }

//    @ParameterizedTest(name = "[{index}] \"{0}\"")
//    @Order(212)
//    @MethodSource
//    void queryCarrierFuel(String input) throws InterruptedException {
//        assertRouted(input, FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
//    }
//
//    static Stream<String> queryCarrierFuel() {
//        return Stream.of(
//               "combien de tritium reste-t-il dans le porte-vaisseaux",
//                       "niveau de carburant du porte-vaisseaux"
//                );
//    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromFleetCarrierQueryCommand.ID);
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of(
                "proximité de mon porte-vaisseaux",
                "à quelle distance se trouve le porte-vaisseaux",
                "sommes-nous loin de notre porte-vaisseaux"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFsdTargetQueryCommand.ID);
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of(
                "quelle est la cible FSD actuelle",
                "quelle est la prochaine destination",
                "informations sur la prochaine destination",
                "quel système est ciblé",
                "rapport sur la prochaine étape de l'itinéraire"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, AnalyzeExplorationProfitsQueryCommand.ID);
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of(
                "combien rapportent les scans d'exploration",
                "valeur des données exobiologiques",
                "estimation des profits d'exploration"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TimeQueryCommand.ID);
    }

    static Stream<String> queryTime() {
        return Stream.of(
                "quelle heure est-il",
                "donne-moi l'heure UTC",
                "heure actuelle"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSystemSecurityQueryCommand.ID);
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of(
                "quel est le niveau de sécurité du système",
                "qui contrôle ce système",
                "faction dominante ici"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, StationDataQueryCommand.ID);
    }

    static Stream<String> queryStationDetails() {
        return Stream.of(
                "quels services propose cette station",
                "détails de la station",
                "que puis-je faire ici",
                "rapport de cette station"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyseMaterialsQueryCommand.ID);
    }

    static Stream<String> queryMaterials() {
        return Stream.of(
                "combien de fer avons-nous",
                "inventaire des matériaux",
                "avons-nous du manganèse"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMaterialsOnPlanetQueryCommand.ID);
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of(
                "quels matériaux trouve-t-on sur cette planète",
                "ressources de surface",
                "minéraux disponibles ici"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromTheBubbleQueryCommand.ID);
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of(
                "à quelle distance de la Bulle sommes-nous",
                "distance depuis Sol",
                "combien d'années-lumière de la civilisation"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, AnalyzeLastScanQueryCommand.ID);
    }

    static Stream<String> queryLastScan() {
        return Stream.of(
                "quel a été le dernier scan",
                "affiche le résultat du dernier analyse",
                "qu'avons-nous scanné récemment"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, RemindTargetDestinationQueryCommand.ID);
    }

    static Stream<String> queryReminder() {
        return Stream.of(
                "quel est le rappel actif",
                "rappelle-moi ce que j'ai programmé",
                "liste les rappels"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierETAQueryCommand.ID);
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of(
                "quand arrive mon porte-vaisseaux",
                "ETA du porte-vaisseaux",
                "quelle est l'heure d'arrivée du porte-vaisseaux"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, AnalyzeGeologyInStarSystemQueryCommand.ID);
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of(
                "y a-t-il des signaux géologiques",
                "activité volcanique détectée",
                "où sont les sites géologiques"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMarketsQueryCommand.ID);
    }

    static Stream<String> queryLocalStations() {
        return Stream.of(
                "quels sont les marchés locaux",
                "liste les stations avec commerce",
                "trouve moi le marché le plus proche",
                "cherche une station avec commerce"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, AnalyzeBountiesCollectedQueryCommand.ID);
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of(
                "combien de primes avons-nous accumulées",
                "rapport total des primes",
                "rapport sur le montant des récompenses"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(230)
    @MethodSource
    void queryKeyBindings(String input) throws InterruptedException {
        assertRouted(input, AnalyzeMisingKeyBindingQueryCommand.ID);
    }

    static Stream<String> queryKeyBindings() {
        return Stream.of(
                "vérifie les raccourcis clavier",
                "quelles touches sont manquantes",
                "analyse les bindings"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, BiomeAnalyzerQueryCommand.ID);
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of(
                "quel est le biome de cette planète",
                "analyse de l'environnement",
                "rapport sur le type de terrain ici"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, AnalyzeDistanceFromLastBioSampleQueryCommand.ID);
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of(
                "distance depuis le dernier échantillon biologique",
                "à quelle distance se trouve le dernier organique scanné",
                "combien de mètres jusqu'au prochain échantillon"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierRouteQueryCommand.ID);
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of(
                "quel est l'itinéraire de mon porte-vaisseaux",
                "combien de sauts pour le carrier",
                "rapport de l'itinéraire du porte-vaisseaux"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierFinalDestinationQueryCommand.ID);
    }

    static Stream<String> queryCarrierDestination() {
        return Stream.of(
                "où va le porte-vaisseau",
                "quelle est la destination finale du porte-vaisseau"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SetCarrierFuelReserveCommand.ID);
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of(
                "règle la réserve de tritium à 5000",
                "définis la réserve de carburant du porte-vaisseaux à 10000",
                "mets la réserve de tritium à 2000"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DisembarkCommand.ID);
    }

    static Stream<String> disembark() {
        return Stream.of(
                "je débarquer",
                "je quitter le vaisseau",
                "je sors du vaisseau"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, ShowCommanderPanelCommand.ID);
    }

    static Stream<String> openCentralPanel() {
        return Stream.of(
                "montre le panneau du commandant",
                "ouvre le panneau central",
                "affiche l'interface du commandant"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, ShowFighterPanelCommand.ID);
    }

    static Stream<String> openFighterPanel() {
        return Stream.of(
                "montre le panneau des chasseur",
                "ouvre les commandes des chasseurs",
                "affiche le panneau des chasseur"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(238)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FighterFireAtWillCommand.ID);
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of(
                "ordonne feu à volonté",
                "chasseur feu à volonté",
                "active le tire à volonté"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(239)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FighterAttackTargetCommand.ID);
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of(
                "chasseur attaque ma cible",
                "concentre-toi sur ma cible",
                "focus sur la cible"
        );
    }

    // =========================================================================
    // BLOC 13 — PORTE-VAISSEAUX: REQUÊTES D'ESCADRON
    // Type: INFO
    // Rôle: vérifier les infos de carrier d'escadron; "d'escadron" doit rester explicite.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierDataQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of(
                "statut du porte-vaisseaux d'escadron",
                "rapport sur le porte-vaisseaux d'escadron",
                "combien de tritium dans le porte-vaisseaux d'escadron",
                "carburant du porte-vaisseaux d'escadron",
                "rapport sur le niveau de carburant du porte-vaisseaux d'escadron"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierRouteQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of(
                "itinéraire du porte-vaisseaux d'escadron",
                "rapport sur l'itinéraire du porte-vaisseaux d'escadron"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierFinalDestinationQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of(
                "où va le porte-vaisseau d'escadron",
                "destination finale du carrier d'escadron",
                "rapport sur la destination du porte-vaisseaux d'escadron"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, AnalyzeSquadronCarrierETAQueryCommand.ID);
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of(
                "ETA du porte-vaisseau d'escadron",
                "quand arrive le carrier d'escadron"
        );
    }

    // =========================================================================
    // BLOC 13 — DÉSAMBIGUÏSATION CARRIER
    // Type: INFO / ACTION_JEU
    // Rôle: vérifier que "porte-vaisseau" seul route vers le carrier personnel, pas l'escadron.
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(250)
    @MethodSource
    void bareCarrierDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, NavigateToFleetCarrierCommand.ID);
    }

    static Stream<String> bareCarrierDefaultsToFleet() {
        return Stream.of("navigue vers le porte-vaisseaux",
                "retourne au porte-vaisseaux",
                "ramène-nous au porte-vaisseaux");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, AnalyzeFleetCarrierDataQueryCommand.ID);
    }

    static Stream<String> bareCarrierStatusDefaultsToFleet() {
        return Stream.of("statut du porte-vaisseaux",
                "rapport du porte-vaisseaux",
                "finances porte-vaisseaux",
                "combien de temps peut fonctionner le porte-vaisseaux",
                "portée porte-vaisseaux avec tritium actuel");
    }

}
