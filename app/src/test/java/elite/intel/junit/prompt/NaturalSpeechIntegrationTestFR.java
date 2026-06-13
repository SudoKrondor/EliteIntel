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
 * French-language integration test for the NaturalSpeech routing system.
 * Mirrors {@link NaturalSpeechIntegrationTestEN} in structure and @Order numbers.
 * <p>
 * Intended as a starting point for a native French speaker to verify and extend.
 * All phrases are drawn from {@code FrenchAiActionAliases} and should be natural
 * spoken equivalents — not literal translations of the English test phrases.
 * <p>
 * NOTES FOR THE LOCALIZER:
 * - "honk" in French routes to OPEN_FSS (not HONK_THE_SYSTEM) — see openFss().
 * - NAVIGATE_TO_SQUADRON_CARRIER has no French alias yet; test is commented out.
 * - Phrases with accent variants (e.g. "écoute"/"ecoute") only need one tested here;
 * the alias file covers both spellings.
 * <p>
 * REQUIREMENTS
 * 1) Local LLM installed and configured with the supported model.
 * 2) App started at least once with game running for basic session data.
 * 3) Language set to FR in app settings before running.
 * >> private static final int LLM_WAIT_MS =
 * Pause between each test phrase. Increase if your LLM is slow.
 * 3000 typical
 * 1500 go faster
 * 250 you are pushing it
 * 150 bro I want your hardware
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NaturalSpeechIntegrationTestFR {

    private static final int LLM_WAIT_MS = 3000;

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
        Thread.sleep(LLM_WAIT_MS);

        HandlerDispatchedEvent event = capture.getLastEvent();
        assertNotNull(event,
                "No handler dispatched for input: \"" + input + "\"");
        assertEquals(expectedAction, event.getAction(),
                "Input: \"" + input + "\" → got \"" + event.getAction()
                        + "\" but expected \"" + expectedAction + "\"");
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
        return Stream.of("réveille-toi",
                "écoute",
                "active les commandes vocales");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(11)
    @MethodSource
    void ignoreMe(String input) throws InterruptedException {
        assertRouted(input, SLEEP.getAction());
    }

    static Stream<String> ignoreMe() {
        return Stream.of("dors",
                "va dormir",
                "ignore-moi",
                "mode veille",
                "tu peux disposer",
                "désactive les commandes vocales");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(12)
    @MethodSource
    void interrupt(String input) throws InterruptedException {
        assertRouted(input, INTERRUPT_TTS.getAction());
    }

    static Stream<String> interrupt() {
        return Stream.of("interromps",
                "arrête de parler",
                "stop voix");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(13)
    @MethodSource
    void combatMode(String input) throws InterruptedException {
        assertRouted(input, ACTIVATE_COMBAT_MODE.getAction());
    }

    static Stream<String> combatMode() {
        return Stream.of("mode combat",
                "passe en mode combat",
                "active le mode combat");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(14)
    @MethodSource
    void analysisMode(String input) throws InterruptedException {
        assertRouted(input, ACTIVATE_ANALYSIS_MODE.getAction());
    }

    static Stream<String> analysisMode() {
        return Stream.of("mode analyse",
                "passe en mode analyse",
                "active le mode analyse");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(15)
    @MethodSource
    void lookAhead(String input) throws InterruptedException {
        assertRouted(input, RESET_HEAD_LOOK.getAction());
    }

    static Stream<String> lookAhead() {
        return Stream.of("réinitialise la vue tête",
                "vue tête par défaut",
                "reset head look");
    }

    // NOTE: In French, "honk" routes to OPEN_FSS (not HONK_THE_SYSTEM).
    // There is no honkTheSystem() test for French — see openFss() at Order(70).

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
        return Stream.of("arrête les moteurs",
                "arrêt complet",
                "halte",
                "stop vaisseau");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(21)
    @MethodSource
    void speed25(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED25.getAction());
    }

    static Stream<String> speed25() {
        return Stream.of("quart de poussée",
                "moteur à 25 pour cent",
                "vitesse lente");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(22)
    @MethodSource
    void speed50(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED50.getAction());
    }

    static Stream<String> speed50() {
        return Stream.of("demi poussée",
                "moteur à 50 pour cent",
                "demi-vitesse");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(23)
    @MethodSource
    void speed75(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED75.getAction());
    }

    static Stream<String> speed75() {
        return Stream.of("trois quarts de poussée",
                "moteur à 75 pour cent");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(24)
    @MethodSource
    void speed100(String input) throws InterruptedException {
        assertRouted(input, SET_SPEED100.getAction());
    }

    static Stream<String> speed100() {
        return Stream.of("pleine poussée",
                "100 pour cent",
                "mets les gaz",
                "plein gaz");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(25)
    @MethodSource
    void speedPlus(String input) throws InterruptedException {
        assertRouted(input, INCREASE_SPEED_BY.getAction());
    }

    static Stream<String> speedPlus() {
        return Stream.of("augmente la vitesse de 10",
                "augmente la vitesse de 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(26)
    @MethodSource
    void speedMinus(String input) throws InterruptedException {
        assertRouted(input, DECREASE_SPEED_BY.getAction());
    }

    static Stream<String> speedMinus() {
        return Stream.of("réduis la vitesse de 10",
                "ralentis de 5");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(27)
    @MethodSource
    void optimalSpeed(String input) throws InterruptedException {
        assertRouted(input, SET_OPTIMAL_SPEED.getAction());
    }

    static Stream<String> optimalSpeed() {
        return Stream.of("vitesse optimale",
                "vitesse d'approche optimale");
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
        return Stream.of("saute en hyperespace",
                "on saute",
                "allons-y");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(31)
    @MethodSource
    void enterSupercruise(String input) throws InterruptedException {
        assertRouted(input, ENTER_SUPER_CRUISE.getAction());
    }

    static Stream<String> enterSupercruise() {
        return Stream.of("entre en super navigation",
                "super navigation",
                "active la super navigation");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(32)
    @MethodSource
    void dropFromSupercruise(String input) throws InterruptedException {
        assertRouted(input, DROP_FROM_SUPER_CRUISE.getAction());
    }

    static Stream<String> dropFromSupercruise() {
        return Stream.of("sors de la super navigation",
                "quitte la super navigation");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(33)
    @MethodSource
    void navigateToMission(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_NEXT_MISSION.getAction());
    }

    static Stream<String> navigateToMission() {
        return Stream.of("navigue vers la mission active",
                "trace l'itinéraire vers la mission active",
                "trace la route vers la mission active",
                "amène-moi à la mission");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(34)
    @MethodSource
    void navigateToCarrier(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_FLEET_CARRIER.getAction());
    }

    static Stream<String> navigateToCarrier() {
        return Stream.of("navigue vers le porte-vaisseau",
                "direction le porte-vaisseau",
                "itinéraire vers le porte-vaisseau",
                "retourne au porte-vaisseau");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(35)
    @MethodSource
    void cancelNavigation(String input) throws InterruptedException {
        assertRouted(input, NAVIGATION_OFF.getAction());
    }

    static Stream<String> cancelNavigation() {
        return Stream.of("annule la navigation",
                "abandonne la navigation",
                "stop navigation",
                "annule l'itinéraire");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(36)
    @MethodSource
    void navigateToLandingZone(String input) throws InterruptedException {
        assertRouted(input, GET_HEADING_TO_LZ.getAction());
    }

    static Stream<String> navigateToLandingZone() {
        return Stream.of("navigue vers la zone d'atterrissage",
                "direction zone d'atterrissage",
                "direction ZA");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(37)
    @MethodSource
    void targetDestination(String input) throws InterruptedException {
        assertRouted(input, TARGET_DESTINATION.getAction());
    }

    static Stream<String> targetDestination() {
        return Stream.of("cible ma prochaine destination",
                "sélectionne la prochaine destination",
                "sélectionne la prochaine étape du trajet FSD",
                "reprise de de l'itinéraire");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(39)
    @MethodSource
    void nextTradeStop(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_NEXT_TRADE_STOP.getAction());
    }

    static Stream<String> nextTradeStop() {
        return Stream.of("navigue vers le prochain arrêt commercial",
                "trajet vers prochain arrêt commercial");
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
        return Stream.of("trains d'atterrissage",
                "déploie les trains d'atterrissage");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(41)
    @MethodSource
    void retractLandingGear(String input) throws InterruptedException {
        assertRouted(input, RETRACT_LANDING_GEAR.getAction());
    }

    static Stream<String> retractLandingGear() {
        return Stream.of("rentre les trains d'atterrissage",
                "remonte les trains d'atterrissage");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(42)
    @MethodSource
    void requestDocking(String input) throws InterruptedException {
        assertRouted(input, REQUEST_DOCKING.getAction());
    }

    static Stream<String> requestDocking() {
        return Stream.of("demande l'autorisation d'amarrage",
                "demande docking",
                "demande l'atterrissage",
                "demande un pad");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(43)
    @MethodSource
    void cargoScoop(String input) throws InterruptedException {
        assertRouted(input, TOGGLE_CARGO_SCOOP.getAction());
    }

    static Stream<String> cargoScoop() {
        return Stream.of("cargo scoop",
                "ouvre cargo scoop",
                "trappe cargo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(44)
    @MethodSource
    void nightVision(String input) throws InterruptedException {
        assertRouted(input, NIGHT_VISION_ON_OFF.getAction());
    }

    static Stream<String> nightVision() {
        return Stream.of("vision nocturne",
                "active vision nocturne",
                "éteins vision nocturne");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(45)
    @MethodSource
    void lights(String input) throws InterruptedException {
        assertRouted(input, LIGHTS_ON_OFF.getAction());
    }

    static Stream<String> lights() {
        return Stream.of("phares", "lumières",
                "allume les lumières",
                "éteins les lumières");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(46)
    @MethodSource
    void dismissShip(String input) throws InterruptedException {
        assertRouted(input, DISMISS_SHIP.getAction());
    }

    static Stream<String> dismissShip() {
        return Stream.of("renvoie le vaisseau",
                "mise en orbite du vaisseau",
                "envoie le vaisseau en orbite");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(47)
    @MethodSource
    void taxi(String input) throws InterruptedException {
        assertRouted(input, TAXI.getAction());
    }

    static Stream<String> taxi() {
        return Stream.of("mode autopilote",
                "active le pilotage automatique",
                "prends les commandes",
                "pose le vaisseau");
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
        return Stream.of("déploie les points d'emport",
                "armes au clair",
                "armes prêtes");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(51)
    @MethodSource
    void retractHardpoints(String input) throws InterruptedException {
        assertRouted(input, RETRACT_HARDPOINTS.getAction());
    }

    static Stream<String> retractHardpoints() {
        return Stream.of("rentre les points d'emport",
                "range les armes");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(52)
    @MethodSource
    void deployHeatSink(String input) throws InterruptedException {
        assertRouted(input, DEPLOY_HEAT_SINK.getAction());
    }

    static Stream<String> deployHeatSink() {
        return Stream.of("lance dissipateur thermique",
                "heat sink",
                "évacue la chaleur",
                "refroidissement");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(53)
    @MethodSource
    void selectHighestThreat(String input) throws InterruptedException {
        assertRouted(input, SELECT_HIGHEST_THREAT.getAction());
    }

    static Stream<String> selectHighestThreat() {
        return Stream.of("cible prioritaire",
                "cible la menace principale",
                "prochain ennemi");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(54)
    @MethodSource
    void deployShieldPowerCell(String input) throws InterruptedException {
        assertRouted(input, DEPLOY_SHIELD_CELL.getAction());
    }

    static Stream<String> deployShieldPowerCell() {
        return Stream.of("utilise une cellule de bouclier",
                "cellule de bouclier",
                "shield cell",
                "déploie une cellule de bouclier");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(55)
    @MethodSource
    void deployChaff(String input) throws InterruptedException {
        assertRouted(input, DEPLOY_CHAFF.getAction());
    }

    static Stream<String> deployChaff() {
        return Stream.of("lance les paillettes",
                "lance chaff",
                "lance des leurres",
                "lance les paillettes");
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
        return Stream.of("energie dans les boucliers",
                "priorité aux boucliers");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(61)
    @MethodSource
    void powerToEngines(String input) throws InterruptedException {
        assertRouted(input, INCREASE_ENGINES_POWER.getAction());
    }

    static Stream<String> powerToEngines() {
        return Stream.of( "energie dans les moteurs",
                "priorité aux moteurs");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(62)
    @MethodSource
    void powerToWeapons(String input) throws InterruptedException {
        assertRouted(input, INCREASE_WEAPONS_POWER.getAction());
    }

    static Stream<String> powerToWeapons() {
        return Stream.of("energie dans les armes",
                "priorité aux armes");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(63)
    @MethodSource
    void resetPower(String input) throws InterruptedException {
        assertRouted(input, RESET_POWER.getAction());
    }

    static Stream<String> resetPower() {
        return Stream.of("équilibre la puissance",
                "puissance équilibré",
                "puissance par défaut");
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
        // NOTE: In French, "honk" maps to OPEN_FSS — not HONK_THE_SYSTEM.
        return Stream.of( "ouvre scanner système",
                "outils d'analyse du système",
                "analyseur de système", "ACS");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(71)
    @MethodSource
    void navigateToNextBioSample(String input) throws InterruptedException {
        assertRouted(input, NAVIGATE_TO_NEXT_BIO_SAMPLE.getAction());
    }

    static Stream<String> navigateToNextBioSample() {
        return Stream.of("naviguation vers prochain échantillon biologique",
                "prochain organique",
                "naviguation codex",
                "navigue vers entree codex");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(72)
    @MethodSource
    void findMiningSite(String input) throws InterruptedException {
        assertRouted(input, FIND_MINING_SITE.getAction());
    }

    static Stream<String> findMiningSite() {
        return Stream.of("trouve site de minage pour alexandrite dans 300 années-lumière",
                "trouve champ d'astéroïdes avec de l'or",
                "où miner de la bromélite");
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
        return Stream.of("entre destination carrier",
                "entre destination du porte-vaisseau",
                "définis destination porte-vaisseau");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(82)
    @MethodSource
    void findNearestCarrier(String input) throws InterruptedException {
        assertRouted(input, FIND_NEAREST_FLEET_CARRIER.getAction());
    }

    static Stream<String> findNearestCarrier() {
        return Stream.of("trouve le porte-vaisseau le plus proche",
                "cherche le porte-vaisseau le plus proche",
                "porte-vaisseau le plus proche");
    }

    // =========================================================================
    // Squadron carrier
    // =========================================================================

    /*
     * NAVIGATE_TO_SQUADRON_CARRIER has no French alias yet.
     * Add an alias to FrenchAiActionAliases.java first, then enable this test.
     *
     * @ParameterizedTest(name = "[{index}] \"{0}\"")
     * @Order(85)
     * @MethodSource
     * void navigateToSquadronCarrier(String input) throws InterruptedException {
     *     assertRouted(input, NAVIGATE_TO_SQUADRON_CARRIER.getAction());
     * }
     *
     * static Stream<String> navigateToSquadronCarrier() {
     *     return Stream.of("navigue vers le carrier d'escadron", "va au carrier escadron");
     * }
     */

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(86)
    @MethodSource
    void calculateNeutronRoute(String input) throws InterruptedException {
        assertRouted(input, CALCULATE_NEUTRON_STAR_ROUTE.getAction());
    }

    static Stream<String> calculateNeutronRoute() {
        return Stream.of("calculer la route des étoiles à neutrons avec efficacité 20",
                "calculer l'itinéraire' des étoiles à neutrons avec efficacité 20",
                "calculer la route des étoiles à neutrons",
                "calculer itinéraire des étoiles à neutrons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(87)
    @MethodSource
    void plotNextNeutronLeg(String input) throws InterruptedException {
        assertRouted(input, PLOT_ROUTE_TO_NEXT_NEUTRON_STAR.getAction());
    }

    static Stream<String> plotNextNeutronLeg() {
        return Stream.of("prochain saut d'étoile à neutrons",
                "route vers prochaine étoile à neutrons",
                "tracer l'itinéraire vers le prochain point de cheminement de l'étoile à neutrons");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(88)
    @MethodSource
    void clearNeutronStarRoute(String input) throws InterruptedException {
        assertRouted(input, CLEAR_NEUTRON_ROUTE.getAction());
    }

    static Stream<String> clearNeutronStarRoute() {
        return Stream.of("effacer la route des étoiles à neutrons",
                "effacer l'itinéraire des étoiles à neutrons",
                "supprimer la route des étoiles à neutrons",
                "supprimer l'itinéraire des étoiles à neutrons");
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
        return Stream.of("toutes les annonces false",
                "basculer toutes les annonces");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(92)
    @MethodSource
    void setReminder(String input) throws InterruptedException {
        assertRouted(input, SET_REMINDER.getAction());
    }

    static Stream<String> setReminder() {
        return Stream.of("définis rappel ravitailler au prochain arrêt");
    }

    // =========================================================================
    // UI panels
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(100)
    @MethodSource
    void galaxyMap(String input) throws InterruptedException {
        assertRouted(input, OPEN_GALAXY_MAP.getAction());
    }

    static Stream<String> galaxyMap() {
        return Stream.of("ouvre carte galactique",
                "affiche carte galactique",
                "carte galaxie");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(101)
    @MethodSource
    void systemMap(String input) throws InterruptedException {
        assertRouted(input, OPEN_SYSTEM_MAP.getAction());
    }

    static Stream<String> systemMap() {
        return Stream.of("ouvre carte du système",
                "carte locale",
                "system map");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(102)
    @MethodSource
    void navigationPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_NAVIGATION.getAction());
    }

    static Stream<String> navigationPanel() {
        return Stream.of("montre navigation",
                "ouvre navigation",
                "panneau navigation");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(103)
    @MethodSource
    void modulesPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_MODULES_PANEL.getAction());
    }

    static Stream<String> modulesPanel() {
        return Stream.of("montre le panneau modules",
                "ouvre le panneau modules",
                "affiche modules");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(104)
    @MethodSource
    void statusPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_STATUS_PANEL.getAction());
    }

    static Stream<String> statusPanel() {
        return Stream.of("montre statut",
                "ouvre statut",
                "panneau statut");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(105)
    @MethodSource
    void inventoryPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_INVENTORY_PANEL.getAction());
    }

    static Stream<String> inventoryPanel() {
        return Stream.of("montre l'inventaire",
                "ouvre l'inventaire",
                "panneau d'inventaire");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(106)
    @MethodSource
    void closePanel(String input) throws InterruptedException {
        assertRouted(input, EXIT_CLOSE.getAction());
    }

    static Stream<String> closePanel() {
        return Stream.of("ferme panneau",
                "quitte le panneau",
                "close panel");
    }

    // =========================================================================
    // Queries
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(200)
    @MethodSource
    void queryCurrentLocation(String input) throws InterruptedException {
        assertRouted(input, CURRENT_LOCATION.getAction());
    }

    static Stream<String> queryCurrentLocation() {
        return Stream.of("où sommes-nous ?",
                "position actuelle",
                "dans quel système sommes-nous",
                "quelle est notre position actuelle");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(202)
    @MethodSource
    void queryShipLoadout(String input) throws InterruptedException {
        assertRouted(input, SHIP_LOADOUT.getAction());
    }

    static Stream<String> queryShipLoadout() {
        return Stream.of("configuration vaisseau",
                "loadout vaisseau",
                "que pilotons-nous",
                "avons-nous un fuel scoop installé",
                "avec quoi sommes-nous équipés");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(203)
    @MethodSource
    void queryCargoHold(String input) throws InterruptedException {
        assertRouted(input, CARGO_HOLD_CONTENTS.getAction());
    }

    static Stream<String> queryCargoHold() {
        return Stream.of("contenu de la soute",
                "que transportons-nous",
                "contenu cargo");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(204)
    @MethodSource
    void queryPlottedRoute(String input) throws InterruptedException {
        assertRouted(input, PLOTTED_ROUTE_ANALYSIS.getAction());
    }

    static Stream<String> queryPlottedRoute() {
        return Stream.of("rapport d''itinéraire",
                "sauts restants",
                "combien de sauts il me reste",
                "sommes-nous arrivés",
                "quand est-ce qu'on arrive");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(205)
    @MethodSource
    void queryStationsInSystem(String input) throws InterruptedException {
        assertRouted(input, QUERY_STATIONS.getAction());
    }

    static Stream<String> queryStationsInSystem() {
        return Stream.of("stations dans le système",
                "quelles stations",
                "ports spatiaux",
                "amarrage disponible");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(206)
    @MethodSource
    void queryStellarObjects(String input) throws InterruptedException {
        assertRouted(input, QUERY_STELLAR_OBJETS.getAction());
    }

    static Stream<String> queryStellarObjects() {
        return Stream.of("planètes atterrissables dans ce système ?",
                "corps du système",
                "anneaux glacés");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(207)
    @MethodSource
    void queryStellarSignals(String input) throws InterruptedException {
        assertRouted(input, QUERY_STELLAR_SIGNALS.getAction());
    }

    static Stream<String> queryStellarSignals() {
        return Stream.of("signaux dans le système",
                "quels signaux ici", "analyse les signaux",
                "qu'est-ce qui est détecté dans le système");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(208)
    @MethodSource
    void queryBioScanProgress(String input) throws InterruptedException {
        assertRouted(input, BIO_SAMPLE_IN_STAR_SYSTEM.getAction());
    }

    static Stream<String> queryBioScanProgress() {
        return Stream.of("quelles planètes ont des signaux biologique ?",
                "progression biologique système");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(209)
    @MethodSource
    void queryExobiologySamples(String input) throws InterruptedException {
        assertRouted(input, EXOBIOLOGY_SAMPLES_ON_THIS_PLANET.getAction());
    }

    static Stream<String> queryExobiologySamples() {
        return Stream.of("échantillons exobiologie",
                "que reste-t-il à scanner",
                "organiques sur cette planète");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(210)
    @MethodSource
    void queryPlayerProfile(String input) throws InterruptedException {
        assertRouted(input, PLAYER_PROFILE_ANALYSIS.getAction());
    }

    static Stream<String> queryPlayerProfile() {
        return Stream.of("profil joueur",
                "profil commandant");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(211)
    @MethodSource
    void queryCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, FLEET_CARRIER_STATUS.getAction());
    }

    static Stream<String> queryCarrierStatus() {
        return Stream.of("statut du porte-vaisseau",
                "état du porte-vaisseau",
                "rapport du porte-vaisseau");
    }

//    @ParameterizedTest(name = "[{index}] \"{0}\"")
//    @Order(212)
//    @MethodSource
//    void queryCarrierFuel(String input) throws InterruptedException {
//        assertRouted(input, FLEET_CARRIER_TRITIUM_SUPPLY.getAction());
//    }
//
//    static Stream<String> queryCarrierFuel() {
//        return Stream.of("niveau de tritium du porte-vaisseau",
//                "combien de tritium dans le porte-vaisseau");
//    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(213)
    @MethodSource
    void queryDistanceToCarrier(String input) throws InterruptedException {
        assertRouted(input, DISTANCE_TO_CARRIER.getAction());
    }

    static Stream<String> queryDistanceToCarrier() {
        return Stream.of("proximité de mon porte-vaisseau",
                "à quelle distance se trouve le porte-vaisseau",
                "sommes-nous loin de notre porte-vaisseau");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(214)
    @MethodSource
    void queryFsdTarget(String input) throws InterruptedException {
        assertRouted(input, FSD_TARGET_ANALYSIS.getAction());
    }

    static Stream<String> queryFsdTarget() {
        return Stream.of("info cible FSD",
                "analyse de la prochaine destination",
                "info sur la prochaine étape",
                "quelle étoile ciblons-nous");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(215)
    @MethodSource
    void queryExplorationProfits(String input) throws InterruptedException {
        assertRouted(input, EXPLORATION_PROFITS.getAction());
    }

    static Stream<String> queryExplorationProfits() {
        return Stream.of("profits exploration",
                "combien vaut l'exploration",
                "valeur exploration");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(216)
    @MethodSource
    void queryTime(String input) throws InterruptedException {
        assertRouted(input, TIME_IN_ZONE.getAction());
    }

    static Stream<String> queryTime() {
        return Stream.of("donne l'heure",
                "quelle heure est-il",
                "heure UTC");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(217)
    @MethodSource
    void querySystemSecurity(String input) throws InterruptedException {
        assertRouted(input, SYSTEM_SECURITY_ANALYSIS.getAction());
    }

    static Stream<String> querySystemSecurity() {
        return Stream.of("sécurité système",
                "qui contrôle ce système",
                "faction dominante");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(218)
    @MethodSource
    void queryStationDetails(String input) throws InterruptedException {
        assertRouted(input, STATION_DETAILS.getAction());
    }

    static Stream<String> queryStationDetails() {
        return Stream.of("détails station",
                "quels services ici",
                "liste les services de la station",
                "info station");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(219)
    @MethodSource
    void queryMaterials(String input) throws InterruptedException {
        assertRouted(input, MATERIALS_INVENTORY.getAction());
    }

    static Stream<String> queryMaterials() {
        return Stream.of("inventaire matériaux fer",
                "combien de vanadium avons-nous",
                "avons-nous du manganèse");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(220)
    @MethodSource
    void queryPlanetMaterials(String input) throws InterruptedException {
        assertRouted(input, PLANET_MATERIALS.getAction());
    }

    static Stream<String> queryPlanetMaterials() {
        return Stream.of("matériaux planète",
                "quels matériaux sur cette planète",
                "matériaux de surface");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(221)
    @MethodSource
    void queryDistanceToBubble(String input) throws InterruptedException {
        assertRouted(input, DISTANCE_TO_BUBBLE.getAction());
    }

    static Stream<String> queryDistanceToBubble() {
        return Stream.of("distance de la Bulle",
                "distance de Sol",
                "distance de la Terre");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(224)
    @MethodSource
    void queryLastScan(String input) throws InterruptedException {
        assertRouted(input, LAST_SCAN_ANALYSIS.getAction());
    }

    static Stream<String> queryLastScan() {
        return Stream.of("affiche dernier scan",
                "affiche le scan le plus récent",
                "quelle est le dernier analyse");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(225)
    @MethodSource
    void queryReminder(String input) throws InterruptedException {
        assertRouted(input, REMINDER.getAction());
    }

    static Stream<String> queryReminder() {
        return Stream.of("rappel",
                "quel était le rappel",
                "liste des rappels");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(226)
    @MethodSource
    void queryCarrierEta(String input) throws InterruptedException {
        assertRouted(input, FLEET_CARRIER_ETA.getAction());
    }

    static Stream<String> queryCarrierEta() {
        return Stream.of("ETA porte-vaisseau",
                "quand arrive le porte-vaisseau",
                "heure d'arrivée du porte-vaisseau");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(227)
    @MethodSource
    void queryGeoSignals(String input) throws InterruptedException {
        assertRouted(input, QUERY_GEO_SIGNALS.getAction());
    }

    static Stream<String> queryGeoSignals() {
        return Stream.of("signaux géologiques",
                "activité géologique",
                "activité volcanique");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(228)
    @MethodSource
    void queryLocalStations(String input) throws InterruptedException {
        assertRouted(input, ANALYZE_MARKETS.getAction());
    }

    static Stream<String> queryLocalStations() {
        return Stream.of("marchés locaux",
                "marchés aux stations et colonies",
                "marchés aux avant-postes du système");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(229)
    @MethodSource
    void queryTotalBounties(String input) throws InterruptedException {
        assertRouted(input, TOTAL_BOUNTIES.getAction());
    }

    static Stream<String> queryTotalBounties() {
        return Stream.of("primes",
                "total primes",
                "combien en primes");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(230)
    @MethodSource
    void queryKeyBindings(String input) throws InterruptedException {
        assertRouted(input, KEY_BINDINGS_ANALYSIS.getAction());
    }

    static Stream<String> queryKeyBindings() {
        return Stream.of("vérifie raccourcis",
                "touches manquantes",
                "commandes", "commandes manquantes");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(231)
    @MethodSource
    void queryBiomeAnalysis(String input) throws InterruptedException {
        assertRouted(input, PLANET_BIOME_ANALYSIS.getAction());
    }

    static Stream<String> queryBiomeAnalysis() {
        return Stream.of("analyse de biome", "biome planétaire",
                "quel biome sur cette planète");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(232)
    @MethodSource
    void queryLastBioSample(String input) throws InterruptedException {
        assertRouted(input, DISTANCE_TO_LAST_BIO_SAMPLE.getAction());
    }

    static Stream<String> queryLastBioSample() {
        return Stream.of("distance du dernier échantillon biologique",
                "distance dernier organisme");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(233)
    @MethodSource
    void queryCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, FLEET_CARRIER_ROUTE_ANALYSIS.getAction());
    }

    static Stream<String> queryCarrierRoute() {
        return Stream.of("route porte-vaisseau",
                "combien de sauts sur la route de notre porte-vaisseau",
                "quelle est la route du porte-vaisseau",
                "rapport sur l'itinéraire du porte-vaisseau"
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(234)
    @MethodSource
    void querySetCarrierFuelReserve(String input) throws InterruptedException {
        assertRouted(input, SET_CARRIER_FUEL_RESERVE.getAction());
    }

    static Stream<String> querySetCarrierFuelReserve() {
        return Stream.of("définir réserve carburant porte-vaisseau à 5000",
                "réserve tritium porte-vaisseau 10000");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(235)
    @MethodSource
    void disembark(String input) throws InterruptedException {
        assertRouted(input, DISEMBARK.getAction());
    }

    static Stream<String> disembark() {
        return Stream.of("je débarquer",
                "je quitter le vaisseau",
                "je sors du vaisseau");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(236)
    @MethodSource
    void openCentralPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_COMMANDER_PANEL.getAction());
    }

    static Stream<String> openCentralPanel() {
        return Stream.of("montre le panneau commandant",
                "panneau central",
                "panneau commander",
                "ouvre le panneau central");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(237)
    @MethodSource
    void openFighterPanel(String input) throws InterruptedException {
        assertRouted(input, SHOW_FIGHTER_PANEL.getAction());
    }

    static Stream<String> openFighterPanel() {
        return Stream.of("montre le panneau chasseur",
                "ouvre le panneau chasseur",
                "panneau fighter");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(238)
    @MethodSource
    void fighterOpenOrders(String input) throws InterruptedException {
        assertRouted(input, FIGHTER_OPEN_ORDERS.getAction());
    }

    static Stream<String> fighterOpenOrders() {
        return Stream.of("chasseur feu à volonté",
                "feu à volonté",
                "fire at will");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(239)
    @MethodSource
    void fighterAttackTarget(String input) throws InterruptedException {
        assertRouted(input, FIGHTER_REQUEST_FOCUS_TARGET.getAction());
    }

    static Stream<String> fighterAttackTarget() {
        return Stream.of("chasseur attaque ma cible",
                "chasseur attaque",
                "focus ma cible");
    }

    // =========================================================================
    // Squadron carrier queries
    // =========================================================================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(240)
    @MethodSource
    void querySquadronCarrierStatus(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_STATUS.getAction());
    }

    static Stream<String> querySquadronCarrierStatus() {
        return Stream.of("statut porte-vaisseau d'escadron ",
                "finances porte-vaisseau d'escadron",
                "combien de temps peut fonctionner le porte-vaisseau de l'escadron");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(241)
    @MethodSource
    void querySquadronCarrierFuel(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_TRITIUM_SUPPLY.getAction());
    }

    static Stream<String> querySquadronCarrierFuel() {
        return Stream.of("tritium porte-vaisseau d'escadron ",
                "carburant porte-vaisseau d'escadron ",
                "réserve tritium escadron");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(242)
    @MethodSource
    void querySquadronCarrierRoute(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_ROUTE_ANALYSIS.getAction());
    }

    static Stream<String> querySquadronCarrierRoute() {
        return Stream.of("route porte-vaisseau d'escadron ",
                "combien de sauts vers le porte-vaisseau d'escadron ",
                "sauts restants porte-vaisseau d'escadron ",
                "rapport sur l'itinéraire du porte-vaisseau de l'escadron");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(243)
    @MethodSource
    void querySquadronCarrierDestination(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_ROUTE_FINAL_DESTINATION.getAction());
    }

    static Stream<String> querySquadronCarrierDestination() {
        return Stream.of("où va vers la destination du porte-vaisseau de l'escadron ",
                "destination porte-vaisseau de l'escadron ",
                "cible la direction de la destination du porte-vaisseau de l'escadron");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(244)
    @MethodSource
    void querySquadronCarrierEta(String input) throws InterruptedException {
        assertRouted(input, SQUADRON_CARRIER_ETA.getAction());
    }

    static Stream<String> querySquadronCarrierEta() {
        return Stream.of("ETA porte-vaisseau d'escadron ",
                "quand arrive le porte-vaisseau d'escadron ",
                "heure d'arrivée porte-vaisseau d'escadron ");
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
        return Stream.of("navigue vers le porte-vaisseau",
                "retourne au porte-vaisseau",
                "ramène-nous au porte-vaisseau");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @Order(251)
    @MethodSource
    void bareCarrierStatusDefaultsToFleet(String input) throws InterruptedException {
        assertRouted(input, FLEET_CARRIER_STATUS.getAction());
    }

    static Stream<String> bareCarrierStatusDefaultsToFleet() {
        return Stream.of("statut porte-vaisseau",
                "rapport du porte-vaisseau",
                "finances porte-vaisseau",
                "combien de temps peut fonctionner le porte-vaisseau",
                "portée porte-vaisseau avec tritium actuel");
    }

}
